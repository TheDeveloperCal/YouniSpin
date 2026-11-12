package com.youni.younispin.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youni.younispin.data.model.Playlist
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.Screen
import com.youni.younispin.ui.theme.YouniTheme

enum class SongSortMode {
    TITLE_ASC,
    ARTIST_ASC,
    DURATION_DESC,
    DATE_ADDED_DESC
}

@Composable
fun SongsScreen(
    playbackController: PlaybackController,
    songs: List<Song>,
    playlists: List<Playlist>,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Long, String) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SongSortMode.TITLE_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val playbackState by playbackController.state.collectAsState()
    val currentSongPath = playbackState.currentSong?.path

    val filteredSongs = remember(songs, searchQuery, sortMode) {
        val list = if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortMode) {
            SongSortMode.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SongSortMode.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
            SongSortMode.DURATION_DESC -> list.sortedByDescending { it.duration }
            SongSortMode.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAdded }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Text(
                        text = "All Songs (${filteredSongs.size})",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Shuffle All
                    IconButton(
                        onClick = {
                            if (filteredSongs.isNotEmpty()) {
                                playbackController.playQueue(filteredSongs.shuffled(), 0)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle All", tint = colors.accent)
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = colors.textSecondary)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Title (A-Z)") },
                                onClick = { sortMode = SongSortMode.TITLE_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Artist (A-Z)") },
                                onClick = { sortMode = SongSortMode.ARTIST_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Duration (Longest first)") },
                                onClick = { sortMode = SongSortMode.DURATION_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Recently Added") },
                                onClick = { sortMode = SongSortMode.DATE_ADDED_DESC; showSortMenu = false }
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Search songs, artists, albums...", color = colors.textMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching tracks found" else "No music found in storage",
                        style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(filteredSongs, key = { _, s -> s.path }) { index, song ->
                    val isPlayingThis = (song.path == currentSongPath)
                    var showItemMenu by remember { mutableStateOf(false) }
                    var showPlaylistPicker by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playbackController.playQueue(filteredSongs, index)
                            }
                            .background(
                                if (isPlayingThis) colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.12f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art
                        AsyncImage(
                            model = song.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.displayTitle,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPlayingThis) colors.accent else colors.textPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.displayArtist} • ${song.displayAlbum}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Duration
                        Text(
                            text = song.formattedDuration,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Favorite toggle
                        IconButton(
                            onClick = { onToggleFavorite(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) Color(0xFFFF4D4F) else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Context menu button
                        Box {
                            IconButton(
                                onClick = { showItemMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = colors.textMuted)
                            }

                            DropdownMenu(
                                expanded = showItemMenu,
                                onDismissRequest = { showItemMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scan Online for Metadata") },
                                    leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        val encoded = Uri.encode(song.path)
                                        onNavigate(Screen.metadataEditor(encoded, scanOnline = true))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Metadata & Cover") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        val encoded = Uri.encode(song.path)
                                        onNavigate(Screen.metadataEditor(encoded))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        showPlaylistPicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View Album") },
                                    leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        onNavigate(Screen.albumDetail(song.albumId))
                                    }
                                )
                            }

                            // Playlist Picker Dialog
                            if (showPlaylistPicker) {
                                AlertDialog(
                                    onDismissRequest = { showPlaylistPicker = false },
                                    title = { Text("Add to Playlist", color = colors.textPrimary) },
                                    text = {
                                        if (playlists.isEmpty()) {
                                            Text("No playlists yet. Create one in the Playlists section.", color = colors.textSecondary)
                                        } else {
                                            Column {
                                                playlists.forEach { pl ->
                                                    TextButton(
                                                        onClick = {
                                                            onAddToPlaylist(pl.id, song.path)
                                                            showPlaylistPicker = false
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(pl.name, modifier = Modifier.fillMaxWidth(), color = colors.accent)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showPlaylistPicker = false }) {
                                            Text("Cancel", color = colors.textSecondary)
                                        }
                                    },
                                    containerColor = colors.surface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
