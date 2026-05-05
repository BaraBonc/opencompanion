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
 *   mel:       IN [1, N_samples] float32  →  OUT [1, N_frames, 1, 32]
 *   embedding: IN [1, 76, 32, 1] float32  →  OUT [1, 1, 1, 96]
 *   wakeword:  IN [1, 16, 96]   float32  →  OUT [1, 1]
 *
 * Timing at 16kHz:
 *   Audio chunk  = 1280 samples = 80ms
 *   Mel frames   = 8 per chunk
 *   Embedding    = every 76 frames → recomputed every 80ms after initial 760ms warmup
 *   Wake word    = every new embedding → score updated every 80ms
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        const val AUDIO_CHUNK_SAMPLES = 1280  // 80ms at 16kHz — required input size per pipeline step
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

    // Sliding window buffers (flat arrays for zero-copy slicing)
    private val melBuffer  = FloatArray(EMBEDDING_FRAMES * MEL_BINS)
    private val embBuffer  = FloatArray(WAKEWORD_CONTEXT * EMBEDDING_DIMS)
    private var melFilled  = 0
    private var embFilled  = 0

    // Accumulates short audio until we have a full 1280-sample chunk
    private val audioAccum  = ShortArray(AUDIO_CHUNK_SAMPLES)
    private var accumCount  = 0

    init { loadModels() }

    /**
     * Feed any number of samples. Returns true only when a wake word score
     * exceeds the threshold on a complete 1280-sample chunk.
     */
    fun process(samples: ShortArray, count: Int): Boolean {
        var i = 0
        while (i < count) {
            val space = AUDIO_CHUNK_SAMPLES - accumCount
            val copy  = minOf(space, count - i)
            System.arraycopy(samples, i, audioAccum, accumCount, copy)
            accumCount += copy
            i += copy

            if (accumCount == AUDIO_CHUNK_SAMPLES) {
                val triggered = processChunk(audioAccum)
                accumCount = 0
                if (triggered) return true
            }
        }
        return false
    }

    private fun processChunk(chunk: ShortArray): Boolean {
        val floatAudio = FloatArray(chunk.size) { chunk[it] / 32768f }

        // Stage 1 — mel spectrogram
        val melFrames = runMel(floatAudio) ?: return false  // [MEL_FRAMES_PER_CHUNK * MEL_BINS]

        // Slide mel buffer left, append new frames at the end
        System.arraycopy(melBuffer, MEL_FRAMES_PER_CHUNK * MEL_BINS, melBuffer, 0,
            (EMBEDDING_FRAMES - MEL_FRAMES_PER_CHUNK) * MEL_BINS)
        System.arraycopy(melFrames, 0, melBuffer,
            (EMBEDDING_FRAMES - MEL_FRAMES_PER_CHUNK) * MEL_BINS,
            MEL_FRAMES_PER_CHUNK * MEL_BINS)
        melFilled = minOf(melFilled + MEL_FRAMES_PER_CHUNK, EMBEDDING_FRAMES)
        if (melFilled < EMBEDDING_FRAMES) return false

        // Stage 2 — embedding
        val embedding = runEmbedding(melBuffer) ?: return false  // [EMBEDDING_DIMS]

        // Slide embedding buffer left, append new embedding at the end
        System.arraycopy(embBuffer, EMBEDDING_DIMS, embBuffer, 0,
            (WAKEWORD_CONTEXT - 1) * EMBEDDING_DIMS)
        System.arraycopy(embedding, 0, embBuffer,
            (WAKEWORD_CONTEXT - 1) * EMBEDDING_DIMS, EMBEDDING_DIMS)
        embFilled = minOf(embFilled + 1, WAKEWORD_CONTEXT)
        if (embFilled < WAKEWORD_CONTEXT) return false

        // Stage 3 — wake word classifier
        return runWakeWord(embBuffer) > threshold()
    }

    private fun runMel(audio: FloatArray): FloatArray? {
        val interp = melInterp ?: return null
        return try {
            interp.resizeInput(0, intArrayOf(1, audio.size))
            interp.allocateTensors()

            val inBuf = floatBuf(audio.size)
            audio.forEach { inBuf.putFloat(it) }
            inBuf.rewind()

            // Output: [1, MEL_FRAMES_PER_CHUNK, 1, MEL_BINS]
            val outSize = MEL_FRAMES_PER_CHUNK * MEL_BINS
            val outBuf = floatBuf(outSize)
            interp.run(inBuf, outBuf)

            outBuf.rewind()
            FloatArray(outSize) { outBuf.float }
        } catch (e: Exception) { null }
    }

    private fun runEmbedding(melFlat: FloatArray): FloatArray? {
        val interp = embInterp ?: return null
        return try {
            // Input shape: [1, 76, 32, 1]
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
            // Input shape: [1, 16, 96]
            val inBuf = floatBuf(WAKEWORD_CONTEXT * EMBEDDING_DIMS)
            embFlat.forEach { inBuf.putFloat(it) }
            inBuf.rewind()

            val outBuf = floatBuf(1)
            interp.run(inBuf, outBuf)
            outBuf.rewind()
            outBuf.float
        } catch (e: Exception) { 0f }
    }

    fun reload() {
        close()
        loadModels()
    }

    fun close() {
        melInterp?.close(); melInterp = null
        embInterp?.close(); embInterp = null
        wwInterp?.close();  wwInterp = null
    }

    private fun loadModels() {
        val modelName = prefs.getString(PREF_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        melInterp = loadInterp("wakewords/melspectrogram.tflite")
        embInterp = loadInterp("wakewords/embedding_model.tflite")
        wwInterp  = loadInterp(modelName)
            ?: loadInterp(DEFAULT_MODEL)  // fallback if custom model missing
    }

    private fun loadInterp(assetPath: String): Interpreter? = try {
        Interpreter(loadAsset(assetPath))
    } catch (e: Exception) { null }

    private fun loadAsset(path: String): MappedByteBuffer {
        val fd = context.assets.openFd(path)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    private fun floatBuf(floatCount: Int): ByteBuffer =
        ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder())

    private fun threshold(): Float {
        val sensitivity = prefs.getInt(PREF_SENSITIVITY, DEFAULT_SENSITIVITY) / 100f
        return 1f - sensitivity
    }
}
