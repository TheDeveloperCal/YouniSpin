package com.youni.younispin.visualizer

import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ln

/**
 * UI-facing view of PCM analysis performed in ExoPlayer's decoded audio pipeline.
 * No microphone or Android audio-session capture permission is required.
 */
class AudioVisualizerEngine {
    private var enabled: Boolean = true

    val waveform: StateFlow<FloatArray> = AudioPcmTap.frame.letState { it.waveform }
    val leftWaveform: StateFlow<FloatArray> = AudioPcmTap.frame.letState { it.leftWaveform }
    val rightWaveform: StateFlow<FloatArray> = AudioPcmTap.frame.letState { it.rightWaveform }
    val spectrum: StateFlow<FloatArray> = AudioPcmTap.frame.letState { it.spectrum }
    val available: StateFlow<Boolean> = AudioPcmTap.available
    val statusMessage: StateFlow<String?> = AudioPcmTap.statusMessage

    fun attach(audioSessionId: Int, captureRateHz: Int = 24) {
        enabled = audioSessionId > 0
        AudioPcmTap.captureRateHz = if (enabled) captureRateHz.coerceIn(15, 30) else 15
        if (!enabled) {
            AudioPcmTap.clear()
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        AudioPcmTap.captureRateHz = if (enabled) AudioPcmTap.captureRateHz.coerceIn(15, 30) else 15
        if (!enabled) AudioPcmTap.clear()
    }

    fun release() = Unit

    private fun <T> StateFlow<AudioPcmFrame>.letState(selector: (AudioPcmFrame) -> T): StateFlow<T> =
        MappedStateFlow(this, selector)

    companion object {
        fun bucketSpectrum(magnitudes: FloatArray, barCount: Int, sensitivity: Float): FloatArray {
            if (magnitudes.isEmpty() || barCount <= 0) return FloatArray(barCount.coerceAtLeast(0))
            val bars = FloatArray(barCount)
            val usable = magnitudes.size.coerceAtLeast(barCount)
            for (bar in 0 until barCount) {
                val startFraction = bar.toFloat() / barCount
                val endFraction = (bar + 1).toFloat() / barCount
                val start = (ln(1f + startFraction * 9f) / ln(10f) * usable).toInt()
                    .coerceIn(0, usable - 1)
                val end = (ln(1f + endFraction * 9f) / ln(10f) * usable).toInt()
                    .coerceIn(start + 1, usable)
                var peak = 0f
                for (i in start until end) peak = maxOf(peak, magnitudes[i])
                bars[bar] = (peak * sensitivity).coerceIn(0f, 1f)
            }
            return bars
        }
    }
}

private class MappedStateFlow<T>(
    private val source: StateFlow<AudioPcmFrame>,
    private val selector: (AudioPcmFrame) -> T
) : StateFlow<T> {
    override val value: T get() = selector(source.value)
    override val replayCache: List<T> get() = listOf(value)
    override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<T>): Nothing {
        source.collect { collector.emit(selector(it)) }
    }
}
