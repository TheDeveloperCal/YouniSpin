package com.youni.younispin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.youni.younispin.ui.theme.YouniTheme
import kotlin.math.sin

@Composable
fun VisualizerView(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    barCount: Int = 28
) {
    val colors = YouniTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "eq_visualizer")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.25f
        val barWidth = (width - totalSpacing) / barCount
        val barGap = totalSpacing / (barCount - 1)

        val gradient = Brush.verticalGradient(
            colors = listOf(colors.secondaryAccent, colors.accent, colors.accentVariant),
            startY = 0f,
            endY = height
        )

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            val animatedHeight = if (isPlaying) {
                val wave1 = (sin(phase + fraction * 6f) + 1f) / 2f
                val wave2 = (sin(phase * 1.5f + fraction * 12f) + 1f) / 2f
                val mixed = (wave1 * 0.6f + wave2 * 0.4f)
                (mixed * (height - 6.dp.toPx())).coerceAtLeast(4.dp.toPx())
            } else {
                3.dp.toPx()
            }

            val x = i * (barWidth + barGap)
            val y = height - animatedHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, animatedHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
