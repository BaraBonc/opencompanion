package com.opencompanion.companion.wakeword

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class WakeWordDetector(private val context: Context) {

    companion object {
        private const val DEFAULT_MODEL = "wakewords/hello_zoli.tflite"
        private const val PREF_MODEL = "wake_word_model"
        private const val PREF_SENSITIVITY = "sensitivity"
        private const val DEFAULT_SENSITIVITY = 50
    }

    private var interpreter: Interpreter? = null
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        loadModel()
    }

    fun process(samples: ShortArray, sampleCount: Int): Boolean {
        val interp = interpreter ?: return false
        val floatInput = FloatArray(sampleCount) { samples[it] / 32768f }
        val inputArray = arrayOf(floatInput)
        val outputArray = HashMap<Int, Any>()
        val scores = FloatArray(1)
        outputArray[0] = scores

        return try {
            interp.runForMultipleInputsOutputs(inputArray, outputArray)
            scores[0] > threshold()
        } catch (e: Exception) {
            false
        }
    }

    fun reload() {
        interpreter?.close()
        loadModel()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun loadModel() {
        val modelName = prefs.getString(PREF_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        interpreter = try {
            Interpreter(loadAsset(modelName))
        } catch (e: Exception) {
            // Fall back to default model if custom model fails
            try { Interpreter(loadAsset(DEFAULT_MODEL)) } catch (e2: Exception) { null }
        }
    }

    private fun loadAsset(assetPath: String): MappedByteBuffer {
        val fd = context.assets.openFd(assetPath)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    private fun threshold(): Float {
        val sensitivity = prefs.getInt(PREF_SENSITIVITY, DEFAULT_SENSITIVITY) / 100f
        return 1f - sensitivity
    }
}
