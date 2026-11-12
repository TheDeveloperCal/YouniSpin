package com.youni.younispin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youni.younispin.data.model.MusicFolder
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun FoldersScreen(
    folders: List<MusicFolder>,
    allSongs: List<Song>,
    playbackController: PlaybackController,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    var selectedFolder by remember { mutableStateOf<MusicFolder?>(null) }

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
                IconButton(
                    onClick = {
                        if (selectedFolder != null) selectedFolder = null else onBack()
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text(
                    text = selectedFolder?.name ?: "Folders (${folders.size})",
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
        val currentFolder = selectedFolder
        if (currentFolder == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolder = folder },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(colors.surfaceElevated, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = colors.secondaryAccent, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${folder.songCount} tracks • ${folder.path}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val folderSongs = remember(currentFolder, allSongs) {
                allSongs.filter { it.path.startsWith(currentFolder.path) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(folderSongs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val idx = folderSongs.indexOf(song)
                                playbackController.playQueue(folderSongs, idx)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.displayTitle,
                                style = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.displayArtist} • ${song.formattedDuration}",
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
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
