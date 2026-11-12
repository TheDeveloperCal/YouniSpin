package com.youni.younispin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.youni.younispin.visualizer.AudioVisualizerEngine
import com.youni.younispin.visualizer.VisualizerSettings
import com.youni.younispin.visualizer.VisualizerStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun LiveAudioVisualizer(
    waveform: FloatArray,
    spectrum: FloatArray,
    isPlaying: Boolean,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 1f,
    leftWaveform: FloatArray = waveform,
    rightWaveform: FloatArray = FloatArray(0)
) {
    var smoothedBars by remember { mutableStateOf(FloatArray(0)) }
    var peaks by remember { mutableStateOf(FloatArray(0)) }
    var peakAge by remember { mutableStateOf(LongArray(0)) }
    var particles by remember { mutableStateOf(List(28) { Particle() }) }

    val barCount = settings.barCount.coerceIn(8, 48)
    val rawBars = remember(spectrum, barCount, settings.sensitivity) {
        AudioVisualizerEngine.bucketSpectrum(spectrum, barCount, settings.sensitivity)
    }

    LaunchedEffect(rawBars, isPlaying, settings.smoothing) {
        val smooth = settings.smoothing.coerceIn(0.05f, 0.95f)
        val next = if (smoothedBars.size != barCount) FloatArray(barCount) else smoothedBars.copyOf()
        val nextPeaks = if (peaks.size != barCount) FloatArray(barCount) else peaks.copyOf()
        val nextAge = if (peakAge.size != barCount) LongArray(barCount) else peakAge.copyOf()
        val now = System.currentTimeMillis()
        for (i in 0 until barCount) {
            val target = if (isPlaying) rawBars.getOrElse(i) { 0f } else next[i] * 0.86f
            next[i] = next[i] * smooth + target * (1f - smooth)
            if (next[i] >= nextPeaks[i]) {
                nextPeaks[i] = next[i]
                nextAge[i] = now
            } else if (now - nextAge[i] > settings.peakHoldMs) {
                nextPeaks[i] *= 0.94f
            }
        }
        smoothedBars = next
        peaks = nextPeaks
        peakAge = nextAge
    }

    val waveColor = settings.waveformColor.copy(alpha = overlayAlpha)
    val accent = settings.accentColor.copy(alpha = overlayAlpha)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val amp = settings.amplitude.coerceIn(0.2f, 2.2f)
        val glow = settings.glowIntensity.coerceIn(0f, 1f)
        val stroke = settings.lineThickness.dp.toPx().coerceIn(1f, 8f)

        when (settings.style) {
            VisualizerStyle.CLASSIC_EQ, VisualizerStyle.MIRRORED_BARS -> {
                if (settings.spectrumEnabled) drawBars(
                    bars = smoothedBars,
                    peaks = peaks,
                    color = waveColor,
                    accent = accent,
                    mirrored = settings.style == VisualizerStyle.MIRRORED_BARS,
                    barWidthFraction = settings.barWidth,
                    glow = glow
                )
            }
            VisualizerStyle.OSCILLOSCOPE, VisualizerStyle.SMOOTH_WAVE,
            VisualizerStyle.STEREO_WAVE, VisualizerStyle.MINIMAL_LINES -> {
                if (settings.waveformEnabled) {
                    if (settings.style == VisualizerStyle.STEREO_WAVE &&
                        leftWaveform.isNotEmpty() && rightWaveform.isNotEmpty()
                    ) {
                        drawWave(
                            leftWaveform, waveColor, false, false, false, amp, stroke, glow,
                            centerY = h * 0.27f, maxHeight = h * 0.42f
                        )
                        drawWave(
                            rightWaveform, waveColor.copy(alpha = 0.78f), false, false, false, amp, stroke, glow,
                            centerY = h * 0.73f, maxHeight = h * 0.42f
                        )
                    } else {
                        drawWave(
                            samples = waveform,
                            color = waveColor,
                            filled = settings.style == VisualizerStyle.SMOOTH_WAVE,
                            mirrored = settings.stereo,
                            thin = settings.style == VisualizerStyle.MINIMAL_LINES,
                            amp = amp,
                            stroke = if (settings.style == VisualizerStyle.MINIMAL_LINES) stroke * 0.6f else stroke,
                            glow = glow
                        )
                    }
                }
                if (settings.spectrumEnabled) drawBars(
                    bars = smoothedBars,
                    peaks = FloatArray(0),
                    color = accent.copy(alpha = 0.45f),
                    accent = accent,
                    mirrored = false,
                    barWidthFraction = settings.barWidth * 0.45f,
                    glow = 0f
                )
            }
            VisualizerStyle.CIRCULAR_SPECTRUM, VisualizerStyle.RADIAL_EQ, VisualizerStyle.AUDIO_RINGS -> {
                if (settings.spectrumEnabled) drawCircular(
                    bars = smoothedBars,
                    color = waveColor,
                    accent = accent,
                    rings = settings.style == VisualizerStyle.AUDIO_RINGS,
                    radial = settings.style == VisualizerStyle.RADIAL_EQ,
                    amp = amp,
                    stroke = stroke,
                    glow = glow
                )
                if (settings.waveformEnabled) drawWave(
                    waveform, accent.copy(alpha = 0.75f), false, false, true, amp * 0.5f, stroke, 0f
                )
            }
            VisualizerStyle.PARTICLE_FLOW -> {
                if (settings.spectrumEnabled && isPlaying && !settings.reducedMotion) {
                    particles = particles.map { p -> p.step(smoothedBars.average().toFloat(), w, h) }
                }
                if (settings.spectrumEnabled) {
                    particles.forEach { p ->
                        drawCircle(
                            color = waveColor.copy(alpha = p.alpha * overlayAlpha),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
                if (settings.waveformEnabled) drawWave(
                    waveform, waveColor.copy(alpha = 0.5f), false, false, true, amp * 0.5f, 1.5f, 0f
                )
            }
            VisualizerStyle.ARTWORK_REACTIVE, VisualizerStyle.AMBIENT -> {
                val energy = if (settings.spectrumEnabled) smoothedBars.average().toFloat().coerceIn(0f, 1f) else 0f
                val radius = (minOf(w, h) * 0.22f) * (0.8f + energy * amp)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            waveColor.copy(alpha = 0.55f * glow + energy * 0.3f),
                            accent.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    radius = radius * 2.2f,
                    center = Offset(w / 2f, h / 2f)
                )
                if (settings.spectrumEnabled) drawBars(
                    bars = smoothedBars,
                    peaks = peaks,
                    color = waveColor.copy(alpha = 0.85f),
                    accent = accent,
                    mirrored = settings.style == VisualizerStyle.ARTWORK_REACTIVE,
                    barWidthFraction = settings.barWidth,
                    glow = glow * 0.4f
                )
                if (settings.waveformEnabled) drawWave(
                    waveform, waveColor.copy(alpha = 0.65f), false, false, true, amp * 0.4f, stroke, 0f
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    bars: FloatArray,
    peaks: FloatArray,
    color: Color,
    accent: Color,
    mirrored: Boolean,
    barWidthFraction: Float,
    glow: Float
) {
    if (bars.isEmpty()) return
    val w = size.width
    val h = size.height
    val n = bars.size
    val slot = w / n
    val barW = slot * barWidthFraction.coerceIn(0.25f, 0.95f)
    val mid = h / 2f
    for (i in 0 until n) {
        val mag = bars[i].coerceIn(0f, 1f)
        val x = i * slot + (slot - barW) / 2f
        if (mirrored) {
            val bh = mag * (h * 0.46f)
            if (glow > 0f) {
                drawRect(color.copy(alpha = 0.18f * glow), Offset(x - 2, mid - bh - 4), Size(barW + 4, bh * 2 + 8))
            }
            drawRect(color, Offset(x, mid - bh), Size(barW, bh * 2))
            val ph = peaks.getOrElse(i) { mag }.coerceIn(0f, 1f) * (h * 0.46f)
            drawRect(accent, Offset(x, mid - ph - 2), Size(barW, 2f))
            drawRect(accent, Offset(x, mid + ph), Size(barW, 2f))
        } else {
            val bh = mag * (h * 0.92f)
            val y = h - bh
            if (glow > 0f) {
                drawRect(color.copy(alpha = 0.2f * glow), Offset(x - 1, y - 3), Size(barW + 2, bh + 3))
            }
            drawRect(color, Offset(x, y), Size(barW, bh))
            val py = h - peaks.getOrElse(i) { mag }.coerceIn(0f, 1f) * (h * 0.92f)
            drawRect(accent, Offset(x, py), Size(barW, 2.5f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    samples: FloatArray,
    color: Color,
    filled: Boolean,
    mirrored: Boolean,
    thin: Boolean,
    amp: Float,
    stroke: Float,
    glow: Float,
    centerY: Float = size.height / 2f,
    maxHeight: Float = size.height
) {
    if (samples.isEmpty()) return
    val w = size.width
    val h = size.height
    val mid = centerY
    val step = max(1, samples.size / (w.toInt().coerceAtLeast(2)))
    val path = Path()
    var started = false
    var x = 0
    var i = 0
    while (i < samples.size) {
        val px = (x.toFloat() / ((samples.size / step).coerceAtLeast(1))) * w
        val py = mid - samples[i] * maxHeight * 0.45f * amp
        if (!started) {
            path.moveTo(px, py)
            started = true
        } else {
            path.lineTo(px, py)
        }
        x++
        i += step
    }
    if (filled) {
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        drawPath(path, color.copy(alpha = 0.35f))
    } else {
        if (glow > 0f) {
            drawPath(path, color.copy(alpha = 0.25f * glow), style = Stroke(width = stroke * 3f, cap = StrokeCap.Round))
        }
        drawPath(path, color, style = Stroke(width = if (thin) stroke else stroke, cap = StrokeCap.Round))
        if (mirrored) {
            val mirror = Path()
            started = false
            x = 0
            i = 0
            while (i < samples.size) {
                val px = (x.toFloat() / ((samples.size / step).coerceAtLeast(1))) * w
                val py = mid + samples[i] * maxHeight * 0.45f * amp
                if (!started) {
                    mirror.moveTo(px, py)
                    started = true
                } else {
                    mirror.lineTo(px, py)
                }
                x++
                i += step
            }
            drawPath(mirror, color.copy(alpha = 0.7f), style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircular(
    bars: FloatArray,
    color: Color,
    accent: Color,
    rings: Boolean,
    radial: Boolean,
    amp: Float,
    stroke: Float,
    glow: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val minR = minOf(w, h) * 0.16f
    val maxR = minOf(w, h) * 0.46f
    val energy = bars.average().toFloat()
    if (rings) {
        for (i in 1..5) {
            val r = minR + (maxR - minR) * (i / 5f) * (0.55f + energy * amp)
            drawCircle(
                color = color.copy(alpha = (0.5f - i * 0.07f) * (0.4f + glow)),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = stroke)
            )
        }
        return
    }
    val n = bars.size.coerceAtLeast(1)
    for (i in 0 until n) {
        val mag = bars[i].coerceIn(0f, 1f) * amp
        val ang = (i.toFloat() / n) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val inner = minR
        val outer = inner + mag * (maxR - minR)
        val x1 = cx + cos(ang) * inner
        val y1 = cy + sin(ang) * inner
        val x2 = cx + cos(ang) * outer
        val y2 = cy + sin(ang) * outer
        if (radial) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = stroke + 1f, cap = StrokeCap.Round)
        } else {
            val nextAng = ((i + 1).toFloat() / n) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val x3 = cx + cos(nextAng) * outer
            val y3 = cy + sin(nextAng) * outer
            drawLine(color, Offset(x2, y2), Offset(x3, y3), strokeWidth = stroke, cap = StrokeCap.Round)
            if (glow > 0.2f) {
                drawCircle(accent.copy(alpha = 0.35f * mag), 2.5f, Offset(x2, y2))
            }
        }
    }
}

private data class Particle(
    var x: Float = (Math.random() * 400).toFloat(),
    var y: Float = (Math.random() * 300).toFloat(),
    var vx: Float = (Math.random() * 2 - 1).toFloat(),
    var vy: Float = (Math.random() * -1.5).toFloat(),
    var size: Float = 2f + Math.random().toFloat() * 3f,
    var alpha: Float = 0.3f + Math.random().toFloat() * 0.5f
) {
    fun step(energy: Float, w: Float, h: Float): Particle {
        x += vx * (0.8f + energy * 4f)
        y += vy * (0.8f + energy * 3f)
        if (x < 0) x = w
        if (x > w) x = 0f
        if (y < 0) y = h
        if (y > h) y = 0f
        alpha = (0.25f + energy * 0.7f).coerceIn(0.1f, 0.9f)
        return copy()
    }
}
