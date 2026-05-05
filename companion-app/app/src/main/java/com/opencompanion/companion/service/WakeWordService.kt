package com.opencompanion.companion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "wake_word_service"
        const val NOTIFICATION_ID = 1
        const val SAMPLE_RATE = 16000
        // Record in 80ms chunks — matches the mel spectrogram model's expected input.
        // WakeWordDetector accumulates internally so smaller chunks are also safe.
        const val CHUNK_SAMPLES = WakeWordDetector.AUDIO_CHUNK_SAMPLES  // 1280 samples = 80ms
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
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(CHUNK_SAMPLES * 2)

            val record = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord = record
            record.startRecording()

            val buffer = ShortArray(CHUNK_SAMPLES)
            while (detectionJob?.isActive == true) {
                val read = record.read(buffer, 0, CHUNK_SAMPLES)
                if (read > 0) {
                    emergencyDetector.process(buffer, read)
                    if (wakeWordDetector.process(buffer, read)) onWakeWordDetected()
                }
            }
            record.stop()
            record.release()
        }
    }

    private fun onWakeWordDetected() {
        stopAudioRecord()
        firebaseLogger.logWakeWord()

        val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(assistIntent)

        // Resume detection after handing off to assistant app
        serviceScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(500)
            startDetectionLoop()
        }
    }

    private fun onAudioSourceChanged() {
        // Restart the detection loop with the new audio source (BT SCO or phone mic)
        startDetectionLoop()
    }

    private fun stopAudioRecord() {
        detectionJob?.cancel()
        audioRecord?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) it.stop()
            it.release()
        }
        audioRecord = null
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wake Word Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Runs in background to listen for the wake word"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
