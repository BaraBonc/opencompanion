package com.opencompanion.companion.wakeword

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager
import androidx.preference.PreferenceManager
import com.opencompanion.companion.firebase.FirebaseLogger
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class EmergencyPhraseDetector(
    private val context: Context,
    private val firebaseLogger: FirebaseLogger
) {
    companion object {
        private const val MODEL_PATH = "wakewords/call_zoli.tflite"
        private const val PREF_ENABLED = "emergency_enabled"
        private const val PREF_PHRASE = "emergency_phrase"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var interpreter: Interpreter? = null

    init {
        if (prefs.getBoolean(PREF_ENABLED, true)) loadModel()
    }

    fun process(samples: ShortArray, sampleCount: Int): Boolean {
        if (!prefs.getBoolean(PREF_ENABLED, true)) return false
        val interp = interpreter ?: return false

        val floatInput = FloatArray(sampleCount) { samples[it] / 32768f }
        val inputArray = arrayOf(floatInput)
        val outputMap = HashMap<Int, Any>()
        val scores = FloatArray(1)
        outputMap[0] = scores

        return try {
            interp.runForMultipleInputsOutputs(inputArray, outputMap)
            if (scores[0] > 0.7f) {
                onEmergencyDetected()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun onEmergencyDetected() {
        playConfirmationTone()
        val phrase = prefs.getString(PREF_PHRASE, "Hívd Zolit") ?: "Hívd Zolit"
        firebaseLogger.logEmergency(phrase)
    }

    private fun playConfirmationTone() {
        val sampleRate = 16000
        val durationMs = 300
        val samples = sampleRate * durationMs / 1000
        val buffer = ShortArray(samples) { i ->
            (Short.MAX_VALUE * Math.sin(2.0 * Math.PI * 880.0 * i / sampleRate)).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            track.stop(); track.release()
        }, durationMs.toLong() + 100)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun loadModel() {
        interpreter = try {
            Interpreter(loadAsset(MODEL_PATH))
        } catch (e: Exception) { null }
    }

    private fun loadAsset(path: String): MappedByteBuffer {
        val fd = context.assets.openFd(path)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }
}
