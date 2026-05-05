package com.opencompanion.companion.wakeword

import android.content.Context
import androidx.preference.PreferenceManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * 3-stage openWakeWord inference pipeline:
 *   raw PCM → melspectrogram.tflite → embedding_model.tflite → <wakeword>.tflite → score
 *
 * Model I/O shapes (confirmed by inspection):
 *   mel:       IN [1, 1280] float32  →  OUT [1, 8, 1, 32]
 *   embedding: IN [1, 76, 32, 1]     →  OUT [1, 1, 1, 96]
 *   wakeword:  IN [1, 16, 96]        →  OUT [1, 1]
 *
 * Caller contract: process() must be called with exactly AUDIO_CHUNK_SAMPLES (1280)
 * normalised float samples. Audio accumulation is the caller's responsibility.
 * This class is NOT thread-safe — call only from the single detection coroutine.
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        const val AUDIO_CHUNK_SAMPLES = 1280  // 80ms at 16kHz
        private const val MEL_BINS = 32
        private const val MEL_FRAMES_PER_CHUNK = 8
        private const val EMBEDDING_FRAMES = 76
        private const val EMBEDDING_DIMS = 96
        private const val WAKEWORD_CONTEXT = 16

        private const val DEFAULT_MODEL = "wakewords/hello_zoli.tflite"
        private const val PREF_MODEL = "wake_word_model"
        private const val PREF_SENSITIVITY = "sensitivity"
        private const val DEFAULT_SENSITIVITY = 50
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var melInterp: Interpreter? = null
    private var embInterp: Interpreter? = null
    private var wwInterp: Interpreter? = null

    // Sliding window buffers — owned exclusively by the detection coroutine
    private val melBuffer = FloatArray(EMBEDDING_FRAMES * MEL_BINS)
    private val embBuffer = FloatArray(WAKEWORD_CONTEXT * EMBEDDING_DIMS)
    private var melFilled = 0
    private var embFilled = 0

    init { loadModels() }

    /**
     * Process exactly [AUDIO_CHUNK_SAMPLES] normalised float samples.
     * Returns true if a wake word is detected above the sensitivity threshold.
     */
    fun process(chunk: FloatArray): Boolean {
        // Stage 1 — mel spectrogram
        val melFrames = runMel(chunk) ?: return false

        // Slide mel window left, append new frames
        System.arraycopy(
            melBuffer, MEL_FRAMES_PER_CHUNK * MEL_BINS,
            melBuffer, 0,
            (EMBEDDING_FRAMES - MEL_FRAMES_PER_CHUNK) * MEL_BINS
        )
        System.arraycopy(
            melFrames, 0,
            melBuffer, (EMBEDDING_FRAMES - MEL_FRAMES_PER_CHUNK) * MEL_BINS,
            MEL_FRAMES_PER_CHUNK * MEL_BINS
        )
        melFilled = minOf(melFilled + MEL_FRAMES_PER_CHUNK, EMBEDDING_FRAMES)
        if (melFilled < EMBEDDING_FRAMES) return false

        // Stage 2 — embedding
        val embedding = runEmbedding(melBuffer) ?: return false

        // Slide embedding window left, append new embedding
        System.arraycopy(embBuffer, EMBEDDING_DIMS, embBuffer, 0, (WAKEWORD_CONTEXT - 1) * EMBEDDING_DIMS)
        System.arraycopy(embedding, 0, embBuffer, (WAKEWORD_CONTEXT - 1) * EMBEDDING_DIMS, EMBEDDING_DIMS)
        embFilled = minOf(embFilled + 1, WAKEWORD_CONTEXT)
        if (embFilled < WAKEWORD_CONTEXT) return false

        // Stage 3 — wake word classifier
        return runWakeWord(embBuffer) > threshold()
    }

    fun reload() { close(); loadModels() }

    fun close() {
        melInterp?.close(); melInterp = null
        embInterp?.close(); embInterp = null
        wwInterp?.close();  wwInterp = null
    }

    private fun runMel(audio: FloatArray): FloatArray? {
        val interp = melInterp ?: return null
        return try {
            interp.resizeInput(0, intArrayOf(1, audio.size))
            interp.allocateTensors()
            val inBuf = floatBuf(audio.size)
            audio.forEach { inBuf.putFloat(it) }
            inBuf.rewind()
            val outBuf = floatBuf(MEL_FRAMES_PER_CHUNK * MEL_BINS)
            interp.run(inBuf, outBuf)
            outBuf.rewind()
            FloatArray(MEL_FRAMES_PER_CHUNK * MEL_BINS) { outBuf.float }
        } catch (e: Exception) { null }
    }

    private fun runEmbedding(melFlat: FloatArray): FloatArray? {
        val interp = embInterp ?: return null
        return try {
            val inBuf = floatBuf(EMBEDDING_FRAMES * MEL_BINS)
            melFlat.forEach { inBuf.putFloat(it) }
            inBuf.rewind()
            val outBuf = floatBuf(EMBEDDING_DIMS)
            interp.run(inBuf, outBuf)
            outBuf.rewind()
            FloatArray(EMBEDDING_DIMS) { outBuf.float }
        } catch (e: Exception) { null }
    }

    private fun runWakeWord(embFlat: FloatArray): Float {
        val interp = wwInterp ?: return 0f
        return try {
            val inBuf = floatBuf(WAKEWORD_CONTEXT * EMBEDDING_DIMS)
            embFlat.forEach { inBuf.putFloat(it) }
            inBuf.rewind()
            val outBuf = floatBuf(1)
            interp.run(inBuf, outBuf)
            outBuf.rewind()
            outBuf.float
        } catch (e: Exception) { 0f }
    }

    private fun loadModels() {
        val modelName = prefs.getString(PREF_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        melInterp = loadInterp("wakewords/melspectrogram.tflite")
        embInterp = loadInterp("wakewords/embedding_model.tflite")
        wwInterp  = loadInterp(modelName) ?: loadInterp(DEFAULT_MODEL)
    }

    private fun loadInterp(path: String): Interpreter? = try {
        Interpreter(loadAsset(path))
    } catch (e: Exception) { null }

    private fun loadAsset(path: String): MappedByteBuffer {
        val fd = context.assets.openFd(path)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    private fun floatBuf(floatCount: Int): ByteBuffer =
        ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder())

    private fun threshold(): Float =
        1f - prefs.getInt(PREF_SENSITIVITY, DEFAULT_SENSITIVITY) / 100f
}
