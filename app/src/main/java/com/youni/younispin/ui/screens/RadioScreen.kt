package com.youni.younispin.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.youni.younispin.data.model.RadioStation
import com.youni.younispin.data.repository.RadioDirectoryQuery
import com.youni.younispin.data.repository.RadioRepository
import com.youni.younispin.data.repository.RadioSortOrder
import com.youni.younispin.data.repository.SomaFmClient
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.playback.PlaybackState
import com.youni.younispin.ui.components.ThemedAlbumPlaceholder
import com.youni.younispin.ui.components.VisualizerView
import com.youni.younispin.ui.theme.YouniTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class RadioTab(val label: String) {
    DIRECTORY("Discover"),
    SOMAFM("SomaFM"),
    SAVED("Saved")
}

private val fallbackSomaFmStations = listOf(
    RadioStation(
        name = "Groove Salad",
        url = "https://api.somafm.com/groovesalad256.pls",
        genre = "Ambient · Downtempo",
        description = "A nicely chilled plate of ambient/downtempo beats.",
        homepage = "https://somafm.com/groovesalad/",
        codec = "MP3",
        bitrate = 256,
        source = SomaFmClient.SOURCE_SOMAFM,
        sourceId = "groovesalad",
        isCustom = false
    ),
    RadioStation(
        name = "Drone Zone",
        url = "https://api.somafm.com/dronezone256.pls",
        genre = "Ambient · Space",
        description = "Serving ambient and space music for comfortable flying.",
        homepage = "https://somafm.com/dronezone/",
        codec = "MP3",
        bitrate = 256,
        source = SomaFmClient.SOURCE_SOMAFM,
        sourceId = "dronezone",
        isCustom = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    stations: List<RadioStation>,
    playbackController: PlaybackController,
    radioRepository: RadioRepository,
    onPlayStation: (RadioStation) -> Unit,
    onAddStation: (String, String, String, String?, String?, String?) -> Unit,
    onUpdateStation: (RadioStation) -> Unit,
    onDeleteStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation, Boolean) -> Unit,
    onImportPlaylist: (String) -> Unit,
    onExportPlaylist: () -> Unit,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackState by playbackController.state.collectAsState()
    val isRadioPlaying = playbackState.currentSong?.isStream == true

    var tab by remember { mutableStateOf(RadioTab.DIRECTORY) }
    var search by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var codec by remember { mutableStateOf("") }
    var minimumBitrate by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RadioSortOrder.POPULARITY) }
    var showFilters by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<RadioStation?>(null) }
    var detailsStation by remember { mutableStateOf<RadioStation?>(null) }
    var directoryStations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var somaStations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read the selected playlist")
            }.onSuccess(onImportPlaylist)
                .onFailure { loadError = it.localizedMessage ?: "Could not import playlist" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val localStations = stations.filter { it.isCustom }
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("#EXTM3U\n")
                    localStations.forEach {
                        writer.write("#EXTINF:-1,${it.name.replace('\n', ' ')}\n${it.url}\n")
                    }
                } ?: error("Unable to write the selected file")
            }.onFailure { loadError = it.localizedMessage ?: "Could not export stations" }
        }
    }

    LaunchedEffect(tab, search, genre, country, language, codec, minimumBitrate, sortOrder) {
        when (tab) {
            RadioTab.DIRECTORY -> {
                isLoading = true
                loadError = null
                try {
                    delay(300)
                    radioRepository.discover(
                        RadioDirectoryQuery(
                            name = search,
                            genre = genre,
                            country = country,
                            language = language,
                            codec = codec,
                            minimumBitrate = minimumBitrate.toIntOrNull(),
                            order = sortOrder
                        )
                    )
                    .also { directoryStations = it }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    loadError = failure.localizedMessage ?: "Radio Browser is unavailable"
                } finally {
                    isLoading = false
                }
            }
            RadioTab.SOMAFM -> {
                if (somaStations.isEmpty()) {
                    isLoading = true
                    loadError = null
                    try {
                        somaStations = radioRepository.somaFmChannels()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        somaStations = fallbackSomaFmStations
                        loadError = "Showing two saved SomaFM stations. Connect to the internet to listen."
                    } finally {
                        isLoading = false
                    }
                }
            }
            RadioTab.SAVED -> Unit
        }
    }

    val visibleStations = when (tab) {
        RadioTab.DIRECTORY -> directoryStations
        RadioTab.SOMAFM -> somaStations.filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true) ||
                it.genre.contains(search, ignoreCase = true)
        }
        RadioTab.SAVED -> stations.filter {
            it.isCustom || it.isFavorite || it.lastPlayed > 0
        }.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) || it.genre.contains(search, true) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surface).statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Radio Live",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            "Verified directory · curated SomaFM",
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                        )
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Directory filters", tint = colors.accent)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add station", tint = colors.accent)
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort stations", tint = colors.textSecondary)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Most popular") },
                                onClick = { sortOrder = RadioSortOrder.POPULARITY; sortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Station name") },
                                onClick = { sortOrder = RadioSortOrder.NAME; sortMenuExpanded = false }
                            )
                        }
                    }
                }
                ScrollableTabRow(
                    selectedTabIndex = tab.ordinal,
                    edgePadding = 12.dp,
                    containerColor = colors.surface,
                    contentColor = colors.accent
                ) {
                    RadioTab.entries.forEach { item ->
                        Tab(
                            selected = tab == item,
                            onClick = { tab = item },
                            text = { Text(item.label) }
                        )
                    }
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isRadioPlaying) {
                item {
                    RadioNowPlayingCard(
                        state = playbackState,
                        onTogglePlayPause = playbackController::togglePlayPause,
                        onToggleFavorite = {
                            playbackState.radioStation?.let { onToggleFavorite(it, !it.isFavorite) }
                        }
                    )
                }
            }

            if (tab == RadioTab.SAVED) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain")) }) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import M3U")
                        }
                        OutlinedButton(onClick = { exportLauncher.launch("younispin-stations.m3u") }) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (tab == RadioTab.SAVED) "Search saved stations" else "Search station name") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Loading stations…", color = colors.textSecondary)
                    }
                }
            }
            loadError?.let { error ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(error, color = colors.textPrimary)
                            Text(
                                "Showing any cached results available. Playback is not stopped by directory refresh errors.",
                                color = colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = {
                                    if (tab == RadioTab.SOMAFM) somaStations = emptyList()
                                    else if (tab == RadioTab.DIRECTORY) {
                                        directoryStations = emptyList()
                                    }
                                }
                            ) { Text("Retry") }
                        }
                    }
                }
            }

            if (!isLoading && visibleStations.isEmpty() && loadError == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = colors.accent, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (tab == RadioTab.SAVED) "No saved stations yet" else "No stations found",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Try a different search or update the filters.",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(visibleStations.distinctBy { it.source to (it.sourceId ?: it.url) }, key = { "${it.source}:${it.sourceId ?: it.id}" }) { station ->
                val isPlayingThis = playbackState.radioStation?.let {
                    it.source == station.source && it.sourceId == station.sourceId
                } ?: (playbackState.currentSong?.path == station.url && playbackState.currentSong?.isStream == true)
                RadioStationCard(
                    station = station,
                    isPlaying = isPlayingThis,
                    isCurrentlyPlaying = isPlayingThis && playbackState.isPlaying,
                    onPlay = { onPlayStation(station) },
                    onToggleFavorite = { onToggleFavorite(station, !station.isFavorite) },
                    onDetails = { detailsStation = station },
                    onEdit = { editingStation = station },
                    onDelete = { onDeleteStation(station) }
                )
            }

            if (tab == RadioTab.DIRECTORY) {
                item {
                    Text(
                        "Radio Browser is a community directory; availability and station terms are set by each broadcaster.",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            if (tab == RadioTab.SOMAFM) {
                item {
                    Text(
                        "SomaFM channels and artwork are supplied by SomaFM. Free-to-listen broadcasts are not necessarily open-licensed music.",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (showFilters) {
        RadioFiltersDialog(
            genre = genre,
            country = country,
            language = language,
            codec = codec,
            minimumBitrate = minimumBitrate,
            onGenreChange = { genre = it },
            onCountryChange = { country = it },
            onLanguageChange = { language = it },
            onCodecChange = { codec = it },
            onBitrateChange = { minimumBitrate = it },
            onDismiss = { showFilters = false }
        )
    }

    if (showAddDialog) {
        StationEditorDialog(
            title = "Add radio station",
            onDismiss = { showAddDialog = false },
            onSave = { name, url, stationGenre, logo, description, homepage ->
                onAddStation(name, url, stationGenre, logo, description, homepage)
                showAddDialog = false
            }
        )
    }

    editingStation?.let { station ->
        StationEditorDialog(
            title = "Edit station",
            initialStation = station,
            onDismiss = { editingStation = null },
            onSave = { name, url, stationGenre, logo, description, homepage ->
                onUpdateStation(
                    station.copy(
                        name = name,
                        url = url,
                        genre = stationGenre,
                        logoUrl = logo,
                        description = description,
                        homepage = homepage
                    )
                )
                editingStation = null
            }
        )
    }

    detailsStation?.let { station ->
        AlertDialog(
            onDismissRequest = { detailsStation = null },
            title = { Text(station.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(station.description ?: station.genre, color = colors.textSecondary)
                    Text(
                        listOfNotNull(station.country, station.language).joinToString(" · ").ifBlank { "Country/language not listed" },
                        color = colors.textSecondary
                    )
                    Text(
                        listOfNotNull(station.codec, station.bitrate.takeIf { it > 0 }?.let { "$it kbps" })
                            .joinToString(" · ").ifBlank { "Stream quality not reported" },
                        color = colors.textSecondary
                    )
                    station.homepage?.let { Text(it, color = colors.accent) }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsStation = null; onPlayStation(station) }) { Text("Listen") }
            },
            dismissButton = { TextButton(onClick = { detailsStation = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun RadioNowPlayingCard(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = YouniTheme.colors
    val song = state.currentSong
    val station = state.radioStation
    val trackArtwork = state.radioTrackArtworkUrl
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Color(0xFFFF4B5C), CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    "ON AIR",
                    color = colors.accent,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (state.isBuffering) "BUFFERING" else station?.codec?.let { "$it${station.bitrate.takeIf { b -> b > 0 }?.let { b -> " · $b kbps" } ?: ""}" } ?: "LIVE RADIO",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = trackArtwork ?: station?.logoUrl, label = "radioNowPlayingArt") { image ->
                        if (image.isNullOrBlank()) {
                            ThemedAlbumPlaceholder(Modifier.fillMaxSize())
                        } else {
                            coil.compose.SubcomposeAsyncImage(
                                model = image,
                                contentDescription = "Radio artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (painter.state is coil.compose.AsyncImagePainter.State.Success) {
                                    SubcomposeAsyncImageContent()
                                } else {
                                    ThemedAlbumPlaceholder(Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song?.displayTitle ?: station?.name ?: "Radio",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(song?.displayArtist, station?.name).distinct().joinToString(" · "),
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (trackArtwork != null) "TRACK ART" else "STATION ART",
                        color = colors.textMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (station?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite station",
                        tint = if (station?.isFavorite == true) Color(0xFFFF4D4F) else colors.textMuted
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = colors.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            if (state.streamError != null) {
                Text("Stream error: ${state.streamError}", color = colors.secondaryAccent, style = MaterialTheme.typography.bodySmall)
            }
            if (state.isBuffering) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.surface
                )
            }
            VisualizerView(isPlaying = state.isPlaying, barCount = 34, modifier = Modifier.fillMaxWidth())
            if (state.radioTrackHistory.size > 1) {
                Text("RECENTLY PLAYED", color = colors.textMuted, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp))
                state.radioTrackHistory.drop(1).take(3).forEach { track ->
                    Text(
                        "${track.artist} — ${track.title}",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = YouniTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) colors.surfaceElevated else colors.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!station.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = "${station.name} station logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onDetails),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        station.name,
                        color = if (isPlaying) colors.accent else colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPlaying) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isCurrentlyPlaying) "LIVE" else "PAUSED",
                            color = colors.accent,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Text(
                    listOfNotNull(station.genre.takeIf(String::isNotBlank), station.country?.takeIf(String::isNotBlank))
                        .joinToString(" · "),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        station.codec,
                        station.bitrate.takeIf { it > 0 }?.let { "$it kbps" }
                    ).joinToString(" · ").ifBlank { station.source.replace('_', ' ') },
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (station.isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (station.isFavorite) Color(0xFFFF4D4F) else colors.textMuted
                )
            }
            if (station.isCustom) {
                Box {
                    var expanded by remember(station.id) { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Station actions", tint = colors.textSecondary)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
                    }
                }
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isCurrentlyPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isCurrentlyPlaying) "Pause station" else "Play station",
                    tint = colors.accent,
                    modifier = Modifier.size(31.dp)
                )
            }
        }
    }
}

@Composable
private fun RadioFiltersDialog(
    genre: String,
    country: String,
    language: String,
    codec: String,
    minimumBitrate: String,
    onGenreChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onCodecChange: (String) -> Unit,
    onBitrateChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Directory filters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(genre, onGenreChange, label = { Text("Genre / tag") }, singleLine = true)
                OutlinedTextField(country, onCountryChange, label = { Text("Country") }, singleLine = true)
                OutlinedTextField(language, onLanguageChange, label = { Text("Language") }, singleLine = true)
                OutlinedTextField(codec, onCodecChange, label = { Text("Codec (e.g. MP3)") }, singleLine = true)
                OutlinedTextField(
                    minimumBitrate,
                    { onBitrateChange(it.filter(Char::isDigit)) },
                    label = { Text("Minimum bitrate (kbps)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Apply", color = colors.accent) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onGenreChange("")
                    onCountryChange("")
                    onLanguageChange("")
                    onCodecChange("")
                    onBitrateChange("")
                }
            ) { Text("Clear") }
        }
    )
}

@Composable
private fun StationEditorDialog(
    title: String,
    initialStation: RadioStation? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?, String?) -> Unit
) {
    val colors = YouniTheme.colors
    var name by remember(initialStation?.id) { mutableStateOf(initialStation?.name.orEmpty()) }
    var url by remember(initialStation?.id) { mutableStateOf(initialStation?.url.orEmpty()) }
    var genre by remember(initialStation?.id) { mutableStateOf(initialStation?.genre.orEmpty()) }
    var logo by remember(initialStation?.id) { mutableStateOf(initialStation?.logoUrl.orEmpty()) }
    var description by remember(initialStation?.id) { mutableStateOf(initialStation?.description.orEmpty()) }
    var homepage by remember(initialStation?.id) { mutableStateOf(initialStation?.homepage.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Station name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("Stream URL (HTTP or HTTPS)") }, singleLine = true)
                OutlinedTextField(genre, { genre = it }, label = { Text("Genre") }, singleLine = true)
                OutlinedTextField(logo, { logo = it }, label = { Text("Station image URL (optional)") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, minLines = 2)
                OutlinedTextField(homepage, { homepage = it }, label = { Text("Homepage (optional)") }, singleLine = true)
                validationError?.let { Text(it, color = colors.secondaryAccent, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validScheme = url.trim().startsWith("https://", true) || url.trim().startsWith("http://", true)
                    if (name.isBlank() || !validScheme) {
                        validationError = "Enter a station name and a valid HTTP(S) stream URL."
                    } else {
                        onSave(
                            name.trim(), url.trim(), genre.trim().ifBlank { "Various" },
                            logo.trim().ifBlank { null },
                            description.trim().ifBlank { null },
                            homepage.trim().ifBlank { null }
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
