package com.youni.younispin.ui.screens

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.youni.younispin.R
import com.youni.younispin.data.model.Song
import com.youni.younispin.metadata.MetadataReader
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.playback.RepeatMode
import com.youni.younispin.ui.Screen
import com.youni.younispin.ui.components.OrbitDial
import com.youni.younispin.ui.components.ThemedAlbumPlaceholder
import com.youni.younispin.ui.components.VisualizerView
import com.youni.younispin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackController: PlaybackController,
    onToggleFavorite: (Song) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    val playbackState by playbackController.state.collectAsState()
    val currentSong = playbackState.currentSong
    val artworkUri = if (currentSong?.isStream == true) {
        playbackState.radioTrackArtworkUrl ?: playbackState.radioStation?.logoUrl
    } else {
        currentSong?.artworkUri
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var useOrbitDialInNowPlaying by remember { mutableStateOf(true) }

    val remainingSleepMs by playbackController.sleepTimer.remainingMs.collectAsState()
    val isTimerActive by playbackController.sleepTimer.isTimerActive.collectAsState()

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isSeeking) {
        (seekFraction * playbackState.durationMs).toLong()
    } else {
        playbackState.currentPositionMs
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            letterSpacing = 2.sp
                        )
                    )
                    if (isTimerActive) {
                        val min = remainingSleepMs / 1000 / 60
                        val sec = (remainingSleepMs / 1000) % 60
                        Text(
                            text = "Sleep: %02d:%02d".format(min, sec),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.secondaryAccent)
                        )
                    }
                }

                Row {
                    // Equalizer shortcut
                    IconButton(onClick = { onNavigate(Screen.EQUALIZER) }) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = colors.textSecondary)
                    }
                    // More options / Track info
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Track Info", tint = colors.textSecondary)
                    }
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Large Album Artwork
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = artworkUri,
                    label = "nowPlayingArtwork"
                ) { artworkUri ->
                    if (artworkUri.isNullOrBlank()) {
                        ThemedAlbumPlaceholder(modifier = Modifier.fillMaxSize())
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentSong?.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        ) {
                            if (painter.state is AsyncImagePainter.State.Success) {
                                SubcomposeAsyncImageContent()
                            } else {
                                ThemedAlbumPlaceholder(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
                if (currentSong?.isStream == true) {
                    Text(
                        if (playbackState.radioTrackArtworkUrl != null) "TRACK ART" else "STATION ART",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Title & Artist Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playbackState.streamMetadataTitle ?: currentSong?.displayTitle ?: "Nothing Playing",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.displayArtist
                            ?: playbackState.radioStation?.name
                            ?: "Select music to play",
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentSong != null) {
                        Text(
                            text = "${currentSong.displayAlbum} • ${currentSong.path.substringAfterLast('.', "").uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textMuted,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (currentSong != null && !currentSong.isStream) {
                    IconButton(
                        onClick = { onToggleFavorite(currentSong) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentSong.isFavorite) Color(0xFFFF4D4F) else colors.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Real-Time Audio Visualizer
            VisualizerView(
                isPlaying = playbackState.isPlaying,
                barCount = 36,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Seek Bar & Timing
            if (playbackState.durationMs > 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (isSeeking) seekFraction else playbackState.progressFraction,
                        onValueChange = {
                            isSeeking = true
                            seekFraction = it
                        },
                        onValueChangeFinished = {
                            playbackController.seekTo((seekFraction * playbackState.durationMs).toLong())
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val curSec = currentPosition / 1000
                        val durSec = playbackState.durationMs / 1000
                        Text(
                            text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                        )
                        Text(
                            text = String.format("%02d:%02d", durSec / 60, durSec % 60),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                        )
                    }
                }
            }

            // Controls View: Orbit Dial vs Modern Linear
            if (useOrbitDialInNowPlaying) {
                Box(
                    modifier = Modifier.padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OrbitDial(
                        dialSizeDp = 180,
                        isPlaying = playbackState.isPlaying,
                        onRotateClockwise = {
                            // Rotate forward seek 5s
                            playbackController.seekTo(playbackState.currentPositionMs + 5000L)
                        },
                        onRotateCounterClockwise = {
                            // Rotate rewind 5s
                            playbackController.seekTo((playbackState.currentPositionMs - 5000L).coerceAtLeast(0L))
                        },
                        onCenterClick = {
                            showQueueSheet = true
                        },
                        onTopClick = {
                            useOrbitDialInNowPlaying = false
                        },
                        onBottomClick = {
                            playbackController.togglePlayPause()
                        },
                        onLeftClick = {
                            playbackController.skipToPrevious()
                        },
                        onRightClick = {
                            playbackController.skipToNext()
                        }
                    )
                }
            } else {
                // Modern Linear Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { playbackController.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode) colors.accent else colors.textMuted
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = { playbackController.skipToPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = colors.textPrimary, modifier = Modifier.size(32.dp))
                    }

                    // Play / Pause Hub
                    IconButton(
                        onClick = { playbackController.togglePlayPause() },
                        modifier = Modifier
                            .size(68.dp)
                            .background(colors.accent, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = colors.background,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = { playbackController.skipToNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = colors.textPrimary, modifier = Modifier.size(32.dp))
                    }

                    // Repeat
                    IconButton(onClick = { playbackController.cycleRepeatMode() }) {
                        val (icon, tint) = when (playbackState.repeatMode) {
                            RepeatMode.OFF -> Pair(Icons.Default.Repeat, colors.textMuted)
                            RepeatMode.ALL -> Pair(Icons.Default.Repeat, colors.accent)
                            RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, colors.secondaryAccent)
                        }
                        Icon(icon, contentDescription = "Repeat", tint = tint)
                    }
                }
            }

            // Bottom Listening Utilities
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dial Switcher
                IconButton(onClick = { useOrbitDialInNowPlaying = !useOrbitDialInNowPlaying }) {
                    Icon(
                        Icons.Default.RotateRight,
                        contentDescription = "Toggle Dial Mode",
                        tint = if (useOrbitDialInNowPlaying) colors.accent else colors.textSecondary
                    )
                }

                // Lyrics
                IconButton(onClick = { showLyricsDialog = true }) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Lyrics", tint = colors.textSecondary)
                }

                // Playback Speed
                TextButton(onClick = { showSpeedDialog = true }) {
                    Text("${playbackState.playbackSpeed}x", color = colors.accent, fontWeight = FontWeight.Bold)
                }

                // Sleep Timer
                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = if (isTimerActive) colors.secondaryAccent else colors.textSecondary
                    )
                }

                // Queue
                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = colors.accent)
                }
            }
        }

        // Queue Bottom Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Current Queue (${playbackState.queue.size} tracks)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playbackState.queue) { idx, item ->
                            val isCurrent = (idx == playbackState.queueIndex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        playbackController.jumpToQueueItem(idx)
                                        showQueueSheet = false
                                    }
                                    .background(if (isCurrent) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrent) colors.accent else colors.textMuted,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isCurrent) colors.accent else colors.textPrimary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.displayArtist,
                                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                                        maxLines = 1
                                    )
                                }
                                IconButton(onClick = { playbackController.removeFromQueue(idx) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lyrics Dialog
        if (showLyricsDialog) {
            AlertDialog(
                onDismissRequest = { showLyricsDialog = false },
                title = { Text("Lyrics — ${currentSong?.displayTitle}") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        Text(
                            text = "No synchronized LRC lyrics found for this track.\n\nPlace a '.lrc' file with the same name in the same folder as the song to see synchronized lyrics.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLyricsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Sleep Timer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                            Button(
                                onClick = {
                                    playbackController.sleepTimer.startTimer(mins)
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated)
                            ) {
                                Text("$mins minutes", color = colors.textPrimary)
                            }
                        }
                        if (isTimerActive) {
                            OutlinedButton(
                                onClick = {
                                    playbackController.sleepTimer.cancelTimer()
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Active Timer", color = ErrorRed)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Playback Speed Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Playback Speed") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isCur = (playbackState.playbackSpeed == speed)
                            Button(
                                onClick = {
                                    playbackController.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCur) colors.accent else colors.surfaceElevated
                                )
                            ) {
                                Text("${speed}x", color = if (isCur) colors.background else colors.textPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Track Info Dialog
        if (showInfoDialog && currentSong != null) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Track Details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Title: ${currentSong.displayTitle}", style = MaterialTheme.typography.bodyMedium)
                        Text("Artist: ${currentSong.displayArtist}", style = MaterialTheme.typography.bodyMedium)
                        Text("Album: ${currentSong.displayAlbum}", style = MaterialTheme.typography.bodyMedium)
                        Text("Format / MIME: ${currentSong.mimeType}", style = MaterialTheme.typography.bodyMedium)
                        Text("Duration: ${currentSong.formattedDuration}", style = MaterialTheme.typography.bodyMedium)
                        Text("Path: ${currentSong.path}", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showInfoDialog = false
                            val encoded = Uri.encode(currentSong.path)
                            onNavigate(Screen.metadataEditor(encoded))
                        }
                    ) {
                        Text("Edit Tags", color = colors.accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
