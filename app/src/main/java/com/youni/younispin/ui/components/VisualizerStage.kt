package com.youni.younispin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackState
import com.youni.younispin.ui.theme.YouniTheme
import com.youni.younispin.visualizer.ImportedGif
import com.youni.younispin.visualizer.VisualizerSettings
import com.youni.younispin.visualizer.VisualizerStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerStage(
    playbackState: PlaybackState,
    settings: VisualizerSettings,
    gif: ImportedGif?,
    waveform: FloatArray,
    leftWaveform: FloatArray,
    rightWaveform: FloatArray,
    spectrum: FloatArray,
    captureAvailable: Boolean,
    statusMessage: String?,
    previewSong: Song? = playbackState.currentSong,
    previewWithoutTrack: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = YouniTheme.colors
    val song = previewSong
    if (song == null && !previewWithoutTrack) {
        IdlePlayerDisplay(modifier)
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(colors.displayGlass)
    ) {
        GifBackgroundLayer(
            gif = gif,
            settings = settings,
            isPlaying = playbackState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        if (settings.artworkOverlayEnabled && song?.artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = song.displayTitle,
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.fillMaxSize()
            )
        }

        val showWave = settings.waveformEnabled && settings.style in waveStyles
        val showSpec = settings.spectrumEnabled && settings.style in specStyles
        val showAny = showWave || showSpec ||
            (settings.style in setOf(
                VisualizerStyle.PARTICLE_FLOW,
                VisualizerStyle.AMBIENT,
                VisualizerStyle.AUDIO_RINGS,
                VisualizerStyle.ARTWORK_REACTIVE
            ) && (settings.waveformEnabled || settings.spectrumEnabled))

        if (showAny) {
            if (captureAvailable) {
                LiveAudioVisualizer(
                    waveform = waveform,
                    leftWaveform = leftWaveform,
                    rightWaveform = rightWaveform,
                    spectrum = spectrum,
                    isPlaying = playbackState.isPlaying,
                    settings = settings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            } else if (statusMessage != null && song != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        if (settings.trackInfoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = playbackState.streamMetadataTitle ?: song?.displayTitle ?: "Visualizer Studio",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = song?.displayArtist ?: "Your Music. Your Way.",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        if (settings.decorationsEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = settings.style.title,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IdlePlayerDisplay(modifier: Modifier = Modifier) {
    val colors = YouniTheme.colors
    val animationsEnabled = YouniTheme.settings.animationsEnabled
    val transition = rememberInfiniteTransition(label = "idle_orbit")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing)
        ),
        label = "idle_orbit_phase"
    )
    val phase = if (animationsEnabled) animatedPhase else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.previewBackground,
                        colors.displayGlass,
                        colors.previewBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val orbitWidth = size.width * 0.76f
            val orbitHeight = size.height * 0.42f
            val topLeft = androidx.compose.ui.geometry.Offset(
                center.x - orbitWidth / 2f,
                center.y - orbitHeight / 2f
            )
            val orbitSize = androidx.compose.ui.geometry.Size(orbitWidth, orbitHeight)
            drawOval(
                color = colors.border.copy(alpha = 0.42f),
                topLeft = topLeft,
                size = orbitSize,
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = colors.accent.copy(alpha = 0.55f),
                startAngle = -34f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = topLeft,
                size = orbitSize,
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
            val angle = phase - (Math.PI / 2f).toFloat()
            drawCircle(
                color = colors.accent.copy(alpha = 0.82f),
                radius = 2.4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    center.x + (orbitWidth / 2f) * cos(angle),
                    center.y + (orbitHeight / 2f) * sin(angle)
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "YOUNI",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "SPIN",
                    color = colors.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = "YOUR MUSIC. YOUR WAY.",
                color = colors.textSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
        }
    }
}

private val waveStyles = setOf(
    VisualizerStyle.OSCILLOSCOPE,
    VisualizerStyle.SMOOTH_WAVE,
    VisualizerStyle.STEREO_WAVE,
    VisualizerStyle.MINIMAL_LINES
)

private val specStyles = setOf(
    VisualizerStyle.CLASSIC_EQ,
    VisualizerStyle.CIRCULAR_SPECTRUM,
    VisualizerStyle.RADIAL_EQ,
    VisualizerStyle.MIRRORED_BARS
)
