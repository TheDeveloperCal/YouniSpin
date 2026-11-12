package com.youni.younispin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youni.younispin.data.model.Song
import com.youni.younispin.metadata.ArtworkManager
import com.youni.younispin.metadata.MatchConfidence
import com.youni.younispin.metadata.MetadataMatch
import com.youni.younispin.metadata.MusicBrainzClient
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme
import kotlinx.coroutines.launch

@Composable
fun MetadataEditorScreen(
    song: Song,
    onSaveOverride: (Song, String, String, String, String?, String?, Int?, Int?, Int?, String?, String?) -> Unit,
    onBack: () -> Unit,
    scanOnOpen: Boolean = false
) {
    val colors = YouniTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf(song.albumArtist ?: "") }
    var genre by remember { mutableStateOf(song.genre ?: "") }
    var yearStr by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var trackNumStr by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var discNumStr by remember { mutableStateOf(song.discNumber.toString()) }
    var composer by remember { mutableStateOf(song.composer ?: "") }
    var customArtworkPath by remember { mutableStateOf(song.customArtworkPath) }

    // Online Search state
    var isSearchingOnline by remember { mutableStateOf(false) }
    var onlineMatches by remember { mutableStateOf<List<MetadataMatch>>(emptyList()) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var isDownloadingArtwork by remember { mutableStateOf(false) }

    fun startOnlineSearch() {
        scope.launch {
            isSearchingOnline = true
            searchError = null
            try {
                val fileTitle = song.path.substringAfterLast('/').substringBeforeLast('.')
                    .replace(Regex("[_-]+"), " ")
                    .trim()
                val queryTitle = title.trim().takeUnless {
                    it.isBlank() || it.equals("<unknown>", ignoreCase = true) ||
                        it.equals("Unknown Track", ignoreCase = true)
                } ?: fileTitle
                val queryArtist = artist.trim().takeUnless {
                    it.isBlank() || it.equals("<unknown>", ignoreCase = true) ||
                        it.equals("Unknown Artist", ignoreCase = true)
                }
                val queryAlbum = album.trim().takeUnless {
                    it.isBlank() || it.equals("<unknown>", ignoreCase = true) ||
                        it.equals("Unknown Album", ignoreCase = true)
                }
                val matches = MusicBrainzClient.searchTrack(queryTitle, queryArtist, queryAlbum)
                onlineMatches = matches
                if (matches.isNotEmpty()) {
                    showMatchDialog = true
                } else {
                    searchError = "No matches found. Check the title and artist, then scan again."
                }
            } catch (error: Exception) {
                searchError = error.localizedMessage ?: "Online metadata lookup failed."
            } finally {
                isSearchingOnline = false
            }
        }
    }

    LaunchedEffect(scanOnOpen, song.path) {
        if (scanOnOpen) startOnlineSearch()
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ArtworkManager.saveArtworkFromUri(context, uri, prefix = "custom_cover")
            if (savedPath != null) {
                customArtworkPath = savedPath
            }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.border
    )

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
                    text = "Edit Track Metadata",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onSaveOverride(
                            song,
                            title.trim(),
                            artist.trim(),
                            album.trim(),
                            albumArtist.trim().ifBlank { null },
                            genre.trim().ifBlank { null },
                            yearStr.toIntOrNull(),
                            trackNumStr.toIntOrNull(),
                            discNumStr.toIntOrNull() ?: 1,
                            composer.trim().ifBlank { null },
                            customArtworkPath
                        )
                        onBack()
                    },
                    enabled = !isDownloadingArtwork,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                ) {
                    Text(if (isDownloadingArtwork) "Loading cover..." else "Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Artwork section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = customArtworkPath ?: song.artworkUri,
                            contentDescription = "Cover Artwork",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.accent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Photo", color = colors.textPrimary)
                            }

                            if (customArtworkPath != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        ArtworkManager.deleteArtwork(customArtworkPath)
                                        customArtworkPath = null
                                    }
                                ) {
                                    Text("Restore Default", color = colors.secondaryAccent)
                                }
                            }
                        }
                    }
                }
            }

            // Online Scan Action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Online for Metadata",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.accent
                            )
                            Text(
                                text = "Look up MusicBrainz & Cover Art Archive",
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                            )
                        }

                        Button(
                            onClick = { startOnlineSearch() },
                            enabled = !isSearchingOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background)
                        ) {
                            if (isSearchingOnline) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.background, strokeWidth = 2.dp)
                            } else {
                                Text("Scan Online")
                            }
                        }
                    }
                    if (searchError != null) {
                        Text(
                            text = searchError ?: "",
                            color = colors.secondaryAccent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Editable Text Fields
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            item {
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            item {
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            item {
                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Album Artist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            item {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = trackNumStr,
                        onValueChange = { trackNumStr = it },
                        label = { Text("Track #") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = discNumStr,
                        onValueChange = { discNumStr = it },
                        label = { Text("Disc #") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    label = { Text("Composer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
            }
        }

        // Online Matches Selection Dialog
        if (showMatchDialog) {
            AlertDialog(
                onDismissRequest = { showMatchDialog = false },
                title = { Text("Select Online Match") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(onlineMatches) { match ->
                            val confidenceLabel = when (match.confidence) {
                                MatchConfidence.STRONG_MATCH -> "Strong Match (90%+)"
                                MatchConfidence.POSSIBLE_MATCH -> "Possible Match"
                                MatchConfidence.MULTIPLE_MATCHES -> "Multiple Candidates"
                                MatchConfidence.LOW_MATCH -> "Low Match"
                            }
                            val badgeColor = when (match.confidence) {
                                MatchConfidence.STRONG_MATCH -> SuccessGreen
                                MatchConfidence.POSSIBLE_MATCH -> colors.accent
                                else -> colors.secondaryAccent
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        title = match.title
                                        artist = match.artist
                                        if (!match.album.isNullOrBlank()) album = match.album
                                        if (match.trackNumber != null) trackNumStr = match.trackNumber.toString()
                                        if (!match.date.isNullOrBlank()) yearStr = match.date.take(4)

                                        // Download cover if available
                                        if (!match.coverArtUrl.isNullOrBlank()) {
                                            scope.launch {
                                                isDownloadingArtwork = true
                                                try {
                                                    val bytes = MusicBrainzClient.downloadArtwork(match.coverArtUrl)
                                                    if (bytes != null) {
                                                        val saved = ArtworkManager.saveArtworkBytes(context, bytes, "mb_art")
                                                        if (saved != null) customArtworkPath = saved
                                                        else searchError = "Cover art could not be saved."
                                                    } else {
                                                        searchError = "Cover art was unavailable for this release."
                                                    }
                                                } catch (error: Exception) {
                                                    searchError = error.localizedMessage ?: "Cover art download failed."
                                                } finally {
                                                    isDownloadingArtwork = false
                                                }
                                            }
                                        }

                                        showMatchDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = match.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = confidenceLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(color = badgeColor)
                                        )
                                    }
                                    Text(
                                        text = "${match.artist} • ${match.album ?: "No Album"}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                                    )
                                    if (!match.date.isNullOrBlank()) {
                                        Text(
                                            text = "Release: ${match.date}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMatchDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
