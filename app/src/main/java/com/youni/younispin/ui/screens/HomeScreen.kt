package com.youni.younispin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youni.younispin.R
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.Screen
import com.youni.younispin.ui.components.OrbitDial
import com.youni.younispin.ui.components.VisualizerView
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun HomeScreen(
    playbackController: PlaybackController,
    allSongs: List<Song>,
    onNavigate: (String) -> Unit,
    orbitDialEnabled: Boolean = true
) {
    val colors = YouniTheme.colors
    val playbackState by playbackController.state.collectAsState()
    val currentSong = playbackState.currentSong

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Header with Branding
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Youni",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = "Spin",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        )
                    }
                    Text(
                        text = "Your Music. Your Way.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onNavigate(Screen.SETTINGS) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surface, CircleShape)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.textSecondary)
                    }
                }
            }
        }

        // Dedicated Music Display (Hardware Aesthetic Display Window)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        if (currentSong != null) onNavigate(Screen.NOW_PLAYING)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.border, colors.surfaceElevated)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art Thumbnail
                        AsyncImage(
                            model = currentSong?.artworkUri ?: R.drawable.ic_placeholder_album,
                            contentDescription = "Current Album Art",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceElevated),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Track details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playbackState.streamMetadataTitle ?: currentSong?.displayTitle ?: "Ready to Play",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSong?.displayArtist ?: "Select music or radio stream",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentSong != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentSong.displayAlbum} • ${currentSong.formattedDuration}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Play/Pause icon button
                        IconButton(
                            onClick = { playbackController.togglePlayPause() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (playbackState.isPlaying) colors.accent else colors.surfaceElevated, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = if (playbackState.isPlaying) colors.background else colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio visualizer bars
                    VisualizerView(
                        isPlaying = playbackState.isPlaying,
                        barCount = 32,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Tactile Orbit Dial Section (if enabled)
        if (orbitDialEnabled) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OrbitDial(
                        dialSizeDp = 220,
                        isPlaying = playbackState.isPlaying,
                        onRotateClockwise = {
                            if (allSongs.isNotEmpty()) {
                                playbackController.skipToNext()
                            }
                        },
                        onRotateCounterClockwise = {
                            if (allSongs.isNotEmpty()) {
                                playbackController.skipToPrevious()
                            }
                        },
                        onCenterClick = {
                            if (currentSong != null) {
                                onNavigate(Screen.NOW_PLAYING)
                            } else if (allSongs.isNotEmpty()) {
                                playbackController.playQueue(allSongs, 0)
                            }
                        },
                        onTopClick = {
                            onNavigate(Screen.SONGS)
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
            }
        }

        // Quick Navigation Menu Grid
        item {
            Text(
                text = "LIBRARY NAVIGATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        item {
            val navItems = listOf(
                NavigationTileData("All Songs", "${allSongs.size} tracks", Icons.Default.MusicNote, colors.accent, Screen.SONGS),
                NavigationTileData("Albums", "By album release", Icons.Default.Album, colors.secondaryAccent, Screen.ALBUMS),
                NavigationTileData("Artists", "Discography", Icons.Default.Person, colors.accent, Screen.ARTISTS),
                NavigationTileData("Playlists", "Custom collections", Icons.Default.QueueMusic, colors.secondaryAccent, Screen.PLAYLISTS),
                NavigationTileData("Favorites", "Hearted tracks", Icons.Default.Favorite, Color(0xFFFF4D4F), Screen.FAVORITES),
                NavigationTileData("Streams & Radio", "Online live audio", Icons.Default.Radio, colors.accent, Screen.RADIO),
                NavigationTileData("Folders", "Device file tree", Icons.Default.Folder, colors.secondaryAccent, Screen.FOLDERS),
                NavigationTileData("Equalizer", "Hardware DSP FX", Icons.Default.GraphicEq, colors.accent, Screen.EQUALIZER),
                NavigationTileData("Settings", "App preferences", Icons.Default.Settings, colors.secondaryAccent, Screen.SETTINGS),
                NavigationTileData("About", "YouniSpin v1.0", Icons.Default.Info, colors.accent, Screen.SETTINGS)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in navItems.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (item in row) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigate(item.route) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.border, colors.surfaceElevated)))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(colors.surfaceElevated, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.tint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Favorites / Quick Play Row
        if (allSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK PLAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.5.sp
                        )
                    )
                    TextButton(onClick = { onNavigate(Screen.SONGS) }) {
                        Text("View All", color = colors.accent, fontSize = 12.sp)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allSongs.take(8)) { song ->
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    val idx = allSongs.indexOf(song)
                                    playbackController.playQueue(allSongs, idx)
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                AsyncImage(
                                    model = song.artworkUri,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = song.displayTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.displayArtist,
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTileData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val route: String
)
