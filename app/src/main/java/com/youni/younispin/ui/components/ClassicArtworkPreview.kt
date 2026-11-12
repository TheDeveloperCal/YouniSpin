package com.youni.younispin.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackState
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun ClassicArtworkPreview(
    playbackState: PlaybackState,
    waveform: FloatArray,
    modifier: Modifier = Modifier
) {
    val colors = YouniTheme.colors
    val song = playbackState.currentSong
    val isRadio = song?.isStream == true
    val trackArtwork = if (isRadio) playbackState.radioTrackArtworkUrl else song?.artworkUri
    val artwork = trackArtwork ?: if (isRadio) playbackState.radioStation?.logoUrl else null

    if (song == null) {
        IdleBrandDisplay(modifier)
        return
    }

    val title = playbackState.streamMetadataTitle?.takeIf(String::isNotBlank) ?: song.displayTitle
    val artist = song.displayArtist.takeIf(String::isNotBlank)
    val album = song.album.takeIf { it.isNotBlank() && it != "<unknown>" }
    val format = if (isRadio) {
        listOfNotNull(
            playbackState.radioStation?.codec,
            playbackState.radioStation?.bitrate?.takeIf { it > 0 }?.let { "$it kbps" }
        ).joinToString(" · ").ifBlank { "LIVE RADIO" }
    } else {
        listOfNotNull(
            song.path.substringAfterLast('.', "").uppercase().takeIf(String::isNotBlank),
            playbackState.audioQuality?.sampleRateHz?.let { "${it / 1000} kHz" }
        ).joinToString(" · ")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(colors.displayGlass)
    ) {
        val imageSize = minOf(maxWidth * 0.82f, maxHeight * 0.78f)
        if (!artwork.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artwork)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(24.dp).alpha(0.18f)
            ) {
                if (painter.state is coil.compose.AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.displayGlass.copy(alpha = 0.58f),
                        colors.displayGlass.copy(alpha = 0.82f),
                        colors.displayGlass
                    )
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRadio) {
                    Text(
                        "LIVE",
                        color = colors.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else if (playbackState.isPlaying) {
                    Text(
                        "NOW PLAYING",
                        color = colors.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = artwork, label = "classicTrackArtwork") { uri ->
                    if (uri.isNullOrBlank()) {
                        ThemedAlbumPlaceholder(
                            modifier = Modifier.size(imageSize).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = if (isRadio && trackArtwork == null) {
                                "${playbackState.radioStation?.name ?: "Radio"} station artwork"
                            } else {
                                "$title album artwork"
                            },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(imageSize).clip(RoundedCornerShape(10.dp))
                        ) {
                            if (painter.state is coil.compose.AsyncImagePainter.State.Success) {
                                SubcomposeAsyncImageContent()
                            } else {
                                ThemedAlbumPlaceholder(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Text(
                title,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (artist != null) {
                Text(
                    artist,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
            if (album != null || format.isNotBlank()) {
                Text(
                    listOfNotNull(album, format.takeIf(String::isNotBlank)).joinToString(" · "),
                    color = colors.textMuted,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
            ActualMiniWaveform(
                samples = waveform,
                active = playbackState.isPlaying,
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun IdleBrandDisplay(modifier: Modifier) {
    val colors = YouniTheme.colors
    val animationsEnabled = YouniTheme.settings.animationsEnabled
    Box(
        modifier = modifier.fillMaxSize().clipToBounds().background(
            Brush.linearGradient(listOf(colors.previewBackground, colors.displayGlass, colors.previewBackground))
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val width = size.width * 0.66f
            val height = size.height * 0.32f
            drawOval(
                color = colors.border.copy(alpha = 0.30f),
                topLeft = androidx.compose.ui.geometry.Offset(centerX - width / 2, centerY - height / 2),
                size = androidx.compose.ui.geometry.Size(width, height),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
            if (animationsEnabled) {
                drawArc(
                    color = colors.accent.copy(alpha = 0.58f),
                    startAngle = -38f,
                    sweepAngle = 34f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - width / 2, centerY - height / 2),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("YOUNI", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text(" SPIN", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "YOUR MUSIC. YOUR WAY.",
                color = colors.textSecondary,
                fontSize = 7.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ActualMiniWaveform(
    samples: FloatArray,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = YouniTheme.colors.accent
    Canvas(modifier) {
        val count = 48
        val gap = 1.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
        val mid = size.height / 2f
        repeat(count) { index ->
            val sampleIndex = if (samples.isEmpty()) 0 else index * samples.size / count
            val amplitude = if (active && sampleIndex in samples.indices) {
                samples[sampleIndex].coerceIn(-1f, 1f).let { kotlin.math.abs(it) }
            } else {
                0.025f
            }
            val barHeight = (size.height * amplitude).coerceAtLeast(1.dp.toPx())
            drawLine(
                color = accent.copy(alpha = if (active) 0.88f else 0.35f),
                start = androidx.compose.ui.geometry.Offset(index * (barWidth + gap) + barWidth / 2, mid - barHeight / 2),
                end = androidx.compose.ui.geometry.Offset(index * (barWidth + gap) + barWidth / 2, mid + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
