package com.opencompanion.companion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.opencompanion.companion.R
import com.opencompanion.companion.audio.BluetoothScoManager
import com.opencompanion.companion.firebase.FirebaseLogger
import com.opencompanion.companion.wakeword.EmergencyPhraseDetector
import com.opencompanion.companion.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "wake_word_service"
        const val NOTIFICATION_ID = 1
        const val SAMPLE_RATE = 16000
        // Read in small chunks so the loop stays responsive to job cancellation.
        // Accumulation to AUDIO_CHUNK_SAMPLES happens in startDetectionLoop().
        private const val READ_CHUNK = 320  // 20ms
    }

    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var emergencyDetector: EmergencyPhraseDetector
    private lateinit var bluetoothScoManager: BluetoothScoManager
    private lateinit var firebaseLogger: FirebaseLogger

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        firebaseLogger = FirebaseLogger()
        wakeWordDetector = WakeWordDetector(this)
        emergencyDetector = EmergencyPhraseDetector(this, firebaseLogger)
        bluetoothScoManager = BluetoothScoManager(this, ::onAudioSourceChanged)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        bluetoothScoManager.register()
        startDetectionLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        detectionJob?.cancel()
        stopAudioRecord()
        bluetoothScoManager.unregister()
        wakeWordDetector.close()
        emergencyDetector.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDetectionLoop() {
        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            val audioSource = bluetoothScoManager.currentAudioSource()
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(READ_CHUNK * 4)

            val record = AudioRecord(
                audioSource, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )
            audioRecord = record
            record.startRecording()

            // Accumulation buffer owned entirely by this coroutine — no race possible.
            val readBuf   = ShortArray(READ_CHUNK)
            val accumBuf  = FloatArray(WakeWordDetector.AUDIO_CHUNK_SAMPLES)
            var accumCount = 0

            while (isActive) {
                val read = record.read(readBuf, 0, READ_CHUNK)
                if (read <= 0) continue

                emergencyDetector.process(readBuf, read)

                // Normalise and accumulate into float buffer
                var i = 0
                while (i < read) {
                    val space = accumBuf.size - accumCount
                    val copy  = minOf(space, read - i)
                    for (j in 0 until copy) {
                        accumBuf[accumCount + j] = readBuf[i + j] / 32768f
                    }
                    accumCount += copy
                    i += copy

                    if (accumCount == WakeWordDetector.AUDIO_CHUNK_SAMPLES) {
                        if (wakeWordDetector.process(accumBuf)) {
                            onWakeWordDetected()
                            // Job is now cancelled — exit loop cleanly
                            break
                        }
                        accumCount = 0
                    }
                }
            }

            record.stop()
            record.release()
        }
    }

    private fun onWakeWordDetected() {
        firebaseLogger.logWakeWord()
        startActivity(Intent(Intent.ACTION_ASSIST).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        // Cancel current job and restart after 500ms so the assistant app gets the mic
        detectionJob?.cancel()
        serviceScope.launch(Dispatchers.Main) {
            delay(500)
            startDetectionLoop()
        }
    }

    private fun onAudioSourceChanged() = startDetectionLoop()

    private fun stopAudioRecord() {
        detectionJob?.cancel()
        audioRecord?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) it.stop()
            it.release()
        }
        audioRecord = null
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Wake Word Listener", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Runs in background to listen for the wake word"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
