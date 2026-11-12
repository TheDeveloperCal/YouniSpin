package com.youni.younispin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    allSongs: List<Song>,
    playbackController: PlaybackController,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    val albumSongs = remember(albumId, allSongs) {
        allSongs.filter { it.albumId == albumId }
            .sortedWith(compareBy({ it.discNumber }, { it.trackNumber }, { it.title }))
    }

    val first = albumSongs.firstOrNull()
    val albumTitle = first?.displayAlbum ?: "Album"
    val artistName = first?.displayArtist ?: "Artist"
    val artworkUri = first?.artworkUri

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
                    text = albumTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = albumTitle,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceElevated),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = "${albumSongs.size} tracks",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                if (albumSongs.isNotEmpty()) {
                                    playbackController.playQueue(albumSongs, 0)
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
                                if (albumSongs.isNotEmpty()) {
                                    playbackController.playQueue(albumSongs.shuffled(), 0)
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

            itemsIndexed(albumSongs) { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playbackController.playQueue(albumSongs, index)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (song.trackNumber > 0) "${song.trackNumber}" else "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        ),
                        modifier = Modifier.width(28.dp)
                    )

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

                    Text(
                        text = song.formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                    )
                }
            }
        }
    }
}
