package com.youni.younispin.visualizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioPcmFrame(
    val waveform: FloatArray = FloatArray(0),
    val leftWaveform: FloatArray = FloatArray(0),
    val rightWaveform: FloatArray = FloatArray(0),
    val spectrum: FloatArray = FloatArray(0)
)

object AudioPcmTap {
    private val _frame = MutableStateFlow(AudioPcmFrame())
    val frame: StateFlow<AudioPcmFrame> = _frame.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Play a track to visualize decoded audio.")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    @Volatile
    var captureRateHz: Int = 24

    internal fun publish(frame: AudioPcmFrame) {
        _frame.value = frame
        _available.value = true
        _statusMessage.value = null
    }

    internal fun clear() {
        _frame.value = AudioPcmFrame()
        _available.value = false
        _statusMessage.value = "Play a track to visualize decoded audio."
    }
}

@OptIn(UnstableApi::class)
class PcmTapRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(PcmTapAudioProcessor()))
            .build()
    }
}

@OptIn(UnstableApi::class)
private class PcmTapAudioProcessor : BaseAudioProcessor() {
    private val analyzer = PcmFrameAnalyzer()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        analyzer.consume(inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN), inputAudioFormat)

        val output = replaceOutputBuffer(inputBuffer.remaining())
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        analyzer.reset()
        AudioPcmTap.clear()
    }

    override fun onReset() {
        analyzer.reset()
        AudioPcmTap.clear()
    }
}

@OptIn(UnstableApi::class)
private class PcmFrameAnalyzer {
    private val fftSize = 512
    private val displaySize = 256
    private val left = FloatArray(fftSize)
    private val right = FloatArray(fftSize)
    private var writeIndex = 0
    private var samplesWritten = 0
    private var lastPublishMs = 0L

    fun consume(buffer: ByteBuffer, format: AudioProcessor.AudioFormat) {
        val channels = format.channelCount.coerceAtLeast(1)
        val bytesPerSample = when (format.encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        val frameSize = channels * bytesPerSample
        if (frameSize <= 0) return

        while (buffer.remaining() >= frameSize) {
            val first = readSample(buffer, format.encoding)
            val second = if (channels > 1) readSample(buffer, format.encoding) else first
            repeat((channels - 2).coerceAtLeast(0)) {
                readSample(buffer, format.encoding)
            }
            left[writeIndex] = first.coerceIn(-1f, 1f)
            right[writeIndex] = second.coerceIn(-1f, 1f)
            writeIndex = (writeIndex + 1) % fftSize
            samplesWritten = (samplesWritten + 1).coerceAtMost(fftSize)
        }

        val now = SystemClock.elapsedRealtime()
        val frameIntervalMs = 1000L / AudioPcmTap.captureRateHz.coerceIn(15, 30)
        if (samplesWritten >= fftSize && now - lastPublishMs >= frameIntervalMs) {
            lastPublishMs = now
            publishFrame()
        }
    }

    private fun readSample(buffer: ByteBuffer, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_8BIT -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
        C.ENCODING_PCM_16BIT -> buffer.short / 32768f
        C.ENCODING_PCM_24BIT -> {
            val b0 = buffer.get().toInt() and 0xFF
            val b1 = buffer.get().toInt() and 0xFF
            val b2 = buffer.get().toInt()
            ((b2 shl 16) or (b1 shl 8) or b0) / 8_388_608f
        }
        C.ENCODING_PCM_32BIT -> buffer.int / 2_147_483_648f
        C.ENCODING_PCM_FLOAT -> buffer.float
        else -> 0f
    }

    private fun publishFrame() {
        val leftFrame = orderedWindow(left)
        val rightFrame = orderedWindow(right)
        val mono = FloatArray(fftSize) { (leftFrame[it] + rightFrame[it]) * 0.5f }
        AudioPcmTap.publish(
            AudioPcmFrame(
                waveform = resample(mono, displaySize),
                leftWaveform = resample(leftFrame, displaySize),
                rightWaveform = resample(rightFrame, displaySize),
                spectrum = calculateSpectrum(mono)
            )
        )
    }

    private fun orderedWindow(source: FloatArray): FloatArray =
        FloatArray(fftSize) { source[(writeIndex + it) % fftSize] }

    fun reset() {
        left.fill(0f)
        right.fill(0f)
        writeIndex = 0
        samplesWritten = 0
        lastPublishMs = 0L
    }

    private fun resample(source: FloatArray, size: Int): FloatArray =
        FloatArray(size) { i -> source[i * source.size / size] }

    private fun calculateSpectrum(samples: FloatArray): FloatArray {
        val real = samples.copyOf()
        val imaginary = FloatArray(fftSize)
        for (i in real.indices) {
            val window = 0.5f * (1f - cos((2.0 * PI * i / (fftSize - 1)).toFloat()))
            real[i] *= window
        }

        var j = 0
        for (i in 1 until fftSize) {
            var bit = fftSize shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val r = real[i]
                real[i] = real[j]
                real[j] = r
            }
        }

        var length = 2
        while (length <= fftSize) {
            val angle = (-2.0 * PI / length).toFloat()
            val stepReal = cos(angle)
            val stepImaginary = sin(angle)
            for (start in 0 until fftSize step length) {
                var twiddleReal = 1f
                var twiddleImaginary = 0f
                for (offset in 0 until length / 2) {
                    val even = start + offset
                    val odd = even + length / 2
                    val oddReal = real[odd] * twiddleReal - imaginary[odd] * twiddleImaginary
                    val oddImaginary = real[odd] * twiddleImaginary + imaginary[odd] * twiddleReal
                    real[odd] = real[even] - oddReal
                    imaginary[odd] = imaginary[even] - oddImaginary
                    real[even] += oddReal
                    imaginary[even] += oddImaginary
                    val nextTwiddleReal = twiddleReal * stepReal - twiddleImaginary * stepImaginary
                    twiddleImaginary = twiddleReal * stepImaginary + twiddleImaginary * stepReal
                    twiddleReal = nextTwiddleReal
                }
            }
            length = length shl 1
        }

        return FloatArray(fftSize / 2) { i ->
            val magnitude = sqrt(real[i] * real[i] + imaginary[i] * imaginary[i]) / fftSize
            (magnitude * 10f).coerceIn(0f, 1f)
        }
    }
}
