package com.youni.younispin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.youni.younispin.data.model.Playlist
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlists: List<Playlist>,
    playlistSongs: List<Song>,
    playbackController: PlaybackController,
    onRemoveTrack: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    val playlist = playlists.find { it.id == playlistId }
    val playlistName = playlist?.name ?: "Playlist"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = ErrorRed)
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${playlistSongs.size} tracks",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                if (playlistSongs.isNotEmpty()) {
                                    playbackController.playQueue(playlistSongs, 0)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (playlistSongs.isNotEmpty()) {
                                    playbackController.playQueue(playlistSongs.shuffled(), 0)
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = colors.accent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = colors.textPrimary)
                        }
                    }
                }
            }

            itemsIndexed(playlistSongs) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playbackController.playQueue(playlistSongs, index)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.displayTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.displayArtist,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onRemoveTrack(playlistId, song.path) }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.textMuted)
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Playlist") },
                text = { Text("Are you sure you want to delete \"$playlistName\"? Local music files will NOT be deleted.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeletePlaylist(playlistId)
                            showDeleteConfirm = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
