package com.youni.younispin.ui.screens

import android.view.HapticFeedbackConstants
import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.youni.younispin.data.model.*
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.playback.RepeatMode
import com.youni.younispin.ui.components.ThemedAlbumPlaceholder
import com.youni.younispin.ui.components.ClassicArtworkPreview
import com.youni.younispin.ui.components.LiveAudioVisualizer
import com.youni.younispin.ui.components.VisualizerStage
import com.youni.younispin.ui.components.centerJoystick
import com.youni.younispin.ui.components.pressAndHold
import com.youni.younispin.ui.theme.*
import com.youni.younispin.visualizer.AudioVisualizerEngine
import com.youni.younispin.visualizer.GifBackgroundStore
import com.youni.younispin.visualizer.VisualizerPreferences
import com.youni.younispin.visualizer.VisualizerStyle
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import kotlin.math.PI
import kotlin.math.atan2

enum class MenuLevel {
    ROOT,
    MUSIC,
    SONGS,
    ALBUMS,
    ALBUM_DETAIL,
    ARTISTS,
    ARTIST_DETAIL,
    RADIO,
    PLAYLISTS,
    PLAYLIST_DETAIL,
    FAVORITES,
    FOLDERS,
    FOLDER_DETAIL,
    EQUALIZER,
    SETTINGS,
    APPEARANCE_STUDIO,
    NOW_PLAYING,
    VISUALIZER,
    VISUALIZER_STUDIO,
    GENRES,
    GENRE_DETAIL,
    SEARCH,
    RECENT
}

data class ClassicMenuItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val rightText: String? = null,
    val hasSubmenu: Boolean = true,
    val artworkUri: String? = null,
    val song: Song? = null,
    val album: Album? = null,
    val artist: Artist? = null,
    val playlist: Playlist? = null,
    val radioStation: RadioStation? = null,
    val action: (() -> Unit)? = null
)

@Composable
fun MiniVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniVis")
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(11.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val h1 = if (isPlaying) anim1 else 0.3f
        val h2 = if (isPlaying) anim2 else 0.5f
        val h3 = if (isPlaying) anim3 else 0.2f

        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h1)
                .background(accentColor, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h2)
                .background(accentColor, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h3)
                .background(accentColor, RoundedCornerShape(1.dp))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassicPlayerScreen(
    playbackController: PlaybackController,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    folders: List<MusicFolder>,
    radioStations: List<RadioStation>,
    onToggleFavorite: (Song) -> Unit,
    onSaveOverride: (Song, String, String, String, String?, String?, Int?, Int?, Int?, String?, String?) -> Unit,
    onRescanLibrary: () -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToRadio: () -> Unit = {},
    onScanCurrentTrack: (Song) -> Unit = {},
    onLoadPlaylistSongs: suspend (Long) -> List<Song> = { emptyList() },
    orbitDialEnabled: Boolean = true,
    onToggleOrbitDial: (Boolean) -> Unit = {},
    hapticEnabled: Boolean = true,
    onToggleHaptic: (Boolean) -> Unit = {},
    appearanceSettings: AppearanceSettings = AppearanceSettings(),
    onSaveAppearanceSettings: (AppearanceSettings) -> Unit = {},
    onResetAppearanceDefaults: () -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playbackState by playbackController.state.collectAsState()
    val currentSong = playbackState.currentSong
    val clockFormat = remember(context) { DateFormat.getTimeFormat(context) }
    var currentTime by remember { mutableStateOf(clockFormat.format(Date())) }

    LaunchedEffect(clockFormat) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            currentTime = clockFormat.format(Date())
        }
    }

    val visPrefs = remember { VisualizerPreferences(context) }
    var visSettings by remember { mutableStateOf(visPrefs.load()) }
    val gifStore = remember { GifBackgroundStore(context) }
    val visualizerEngine = remember { AudioVisualizerEngine() }
    val waveform by visualizerEngine.waveform.collectAsState()
    val leftWaveform by visualizerEngine.leftWaveform.collectAsState()
    val rightWaveform by visualizerEngine.rightWaveform.collectAsState()
    val spectrum by visualizerEngine.spectrum.collectAsState()
    val captureAvailable by visualizerEngine.available.collectAsState()
    val visStatus by visualizerEngine.statusMessage.collectAsState()
    val selectedGif = gifStore.find(visSettings.selectedGifId)

    fun persistVis(next: com.youni.younispin.visualizer.VisualizerSettings) {
        visSettings = next
        visPrefs.save(next)
    }

    fun ensureVisualizerCapture() {
        visualizerEngine.attach(playbackState.audioSessionId, visSettings.captureRateHz)
    }

    LaunchedEffect(visSettings.captureRateHz, playbackState.audioSessionId) {
        visualizerEngine.attach(playbackState.audioSessionId, visSettings.captureRateHz)
    }

    // Dynamic semantic colors based on active appearance settings
    val colors = YouniTheme.colors
    val animationsEnabled = appearanceSettings.animationsEnabled

    // Navigation Stack
    var menuStack by remember { mutableStateOf(listOf(MenuLevel.ROOT)) }
    val currentLevel = menuStack.last()

    // Context objects for detail screens
    var selectedAlbumId by remember { mutableLongStateOf(0L) }
    var selectedArtistId by remember { mutableLongStateOf(0L) }
    var selectedPlaylistId by remember { mutableLongStateOf(0L) }
    var selectedFolderPath by remember { mutableStateOf("") }
    var currentPlaylistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Selected item index in current menu
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    fun triggerHapticTick() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun triggerCenterSelect() {
        if (hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun navigateTo(level: MenuLevel) {
        menuStack = menuStack + level
        selectedIndex = 0
        coroutineScope.launch { listState.animateScrollToItem(0) }
    }

    fun navigateBack() {
        if (menuStack.size > 1) {
            menuStack = menuStack.dropLast(1)
            selectedIndex = 0
            coroutineScope.launch { listState.animateScrollToItem(0) }
        }
    }

    fun navigateToRoot() {
        menuStack = listOf(MenuLevel.ROOT)
        selectedIndex = 0
        coroutineScope.launch { listState.animateScrollToItem(0) }
    }

    // Load playlist songs when entering playlist detail
    LaunchedEffect(selectedPlaylistId, currentLevel) {
        if (currentLevel == MenuLevel.PLAYLIST_DETAIL && selectedPlaylistId != 0L) {
            currentPlaylistSongs = onLoadPlaylistSongs(selectedPlaylistId)
        }
    }

    // Prepare items based on currentLevel
    val currentMenuItems: List<ClassicMenuItem> = remember(
        currentLevel, songs, albums, artists, playlists, radioStations, folders,
        selectedAlbumId, selectedArtistId, selectedPlaylistId, selectedFolderPath, currentPlaylistSongs
    ) {
        when (currentLevel) {
            MenuLevel.ROOT -> listOf(
                ClassicMenuItem("music", "Library", subtitle = "${songs.size} tracks", hasSubmenu = true),
                ClassicMenuItem("playlists", "Playlists", subtitle = "${playlists.size} playlists", hasSubmenu = true),
                ClassicMenuItem("radio", "Radio Live", hasSubmenu = false, action = onNavigateToRadio),
                ClassicMenuItem("visualizer_studio", "Visualizer", hasSubmenu = true, action = {
                    ensureVisualizerCapture()
                    navigateTo(MenuLevel.VISUALIZER_STUDIO)
                }),
                ClassicMenuItem("settings", "Settings", hasSubmenu = true),
                ClassicMenuItem("now_playing", "Now Playing", hasSubmenu = true)
            )

            MenuLevel.MUSIC -> listOf(
                ClassicMenuItem("shuffle_songs", "Shuffle Songs", hasSubmenu = false, action = {
                    if (songs.isNotEmpty()) {
                        playbackController.playQueue(songs.shuffled(), 0)
                        navigateTo(MenuLevel.NOW_PLAYING)
                    }
                }),
                ClassicMenuItem("shuffle_albums", "Shuffle Albums", hasSubmenu = false, action = {
                    if (songs.isNotEmpty()) {
                        playbackController.playQueue(songs.shuffled(), 0)
                        navigateTo(MenuLevel.NOW_PLAYING)
                    }
                }),
                ClassicMenuItem("all_songs", "All Songs", subtitle = "${songs.size} tracks", hasSubmenu = true),
                ClassicMenuItem("albums", "Albums", subtitle = "${albums.size} albums", hasSubmenu = true),
                ClassicMenuItem("artists", "Artists", subtitle = "${artists.size} artists", hasSubmenu = true),
                ClassicMenuItem("folders", "Folders", subtitle = "${folders.size} folders", hasSubmenu = true),
                ClassicMenuItem("favorites", "Favorites", hasSubmenu = true)
            )

            MenuLevel.SONGS -> songs.map { s ->
                ClassicMenuItem(
                    id = s.path,
                    title = s.displayTitle,
                    subtitle = s.displayArtist,
                    rightText = s.formattedDuration,
                    hasSubmenu = false,
                    artworkUri = s.artworkUri,
                    song = s,
                    action = {
                        val idx = songs.indexOf(s)
                        playbackController.playQueue(songs, idx.coerceAtLeast(0))
                        navigateTo(MenuLevel.NOW_PLAYING)
                    }
                )
            }

            MenuLevel.ALBUMS -> albums.map { a ->
                ClassicMenuItem(
                    id = a.id.toString(),
                    title = a.title,
                    subtitle = "${a.artist} • ${a.songCount} tracks",
                    hasSubmenu = true,
                    artworkUri = a.artworkUri,
                    album = a,
                    action = {
                        selectedAlbumId = a.id
                        navigateTo(MenuLevel.ALBUM_DETAIL)
                    }
                )
            }

            MenuLevel.ALBUM_DETAIL -> {
                val albumSongs = songs.filter { it.albumId == selectedAlbumId }
                albumSongs.map { s ->
                    ClassicMenuItem(
                        id = s.path,
                        title = s.displayTitle,
                        subtitle = s.displayArtist,
                        rightText = s.formattedDuration,
                        hasSubmenu = false,
                        artworkUri = s.artworkUri,
                        song = s,
                        action = {
                            val idx = albumSongs.indexOf(s)
                            playbackController.playQueue(albumSongs, idx.coerceAtLeast(0))
                            navigateTo(MenuLevel.NOW_PLAYING)
                        }
                    )
                }
            }

            MenuLevel.ARTISTS -> artists.map { ar ->
                ClassicMenuItem(
                    id = ar.name,
                    title = ar.name,
                    subtitle = "${ar.songCount} songs",
                    hasSubmenu = true,
                    artist = ar,
                    action = {
                        selectedArtistId = ar.id
                        navigateTo(MenuLevel.ARTIST_DETAIL)
                    }
                )
            }

            MenuLevel.ARTIST_DETAIL -> {
                val artistSongs = songs.filter { it.artistId == selectedArtistId }
                artistSongs.map { s ->
                    ClassicMenuItem(
                        id = s.path,
                        title = s.displayTitle,
                        subtitle = s.displayAlbum,
                        rightText = s.formattedDuration,
                        hasSubmenu = false,
                        artworkUri = s.artworkUri,
                        song = s,
                        action = {
                            val idx = artistSongs.indexOf(s)
                            playbackController.playQueue(artistSongs, idx.coerceAtLeast(0))
                            navigateTo(MenuLevel.NOW_PLAYING)
                        }
                    )
                }
            }

            MenuLevel.RADIO -> radioStations.map { st ->
                ClassicMenuItem(
                    id = st.url,
                    title = st.name,
                    subtitle = st.genre,
                    rightText = "LIVE",
                    hasSubmenu = false,
                    artworkUri = st.logoUrl,
                    radioStation = st,
                    action = {
                        playbackController.playRadioStream(st)
                        navigateTo(MenuLevel.NOW_PLAYING)
                    }
                )
            }

            MenuLevel.PLAYLISTS -> playlists.map { pl ->
                ClassicMenuItem(
                    id = pl.id.toString(),
                    title = pl.name,
                    subtitle = "${pl.songCount} tracks",
                    hasSubmenu = true,
                    playlist = pl,
                    action = {
                        selectedPlaylistId = pl.id
                        navigateTo(MenuLevel.PLAYLIST_DETAIL)
                    }
                )
            }

            MenuLevel.PLAYLIST_DETAIL -> {
                currentPlaylistSongs.map { s ->
                    ClassicMenuItem(
                        id = s.path,
                        title = s.displayTitle,
                        subtitle = s.displayArtist,
                        rightText = s.formattedDuration,
                        hasSubmenu = false,
                        artworkUri = s.artworkUri,
                        song = s,
                        action = {
                            val idx = currentPlaylistSongs.indexOf(s)
                            playbackController.playQueue(currentPlaylistSongs, idx.coerceAtLeast(0))
                            navigateTo(MenuLevel.NOW_PLAYING)
                        }
                    )
                }
            }

            MenuLevel.FAVORITES -> {
                val favs = songs.filter { it.isFavorite }
                favs.map { s ->
                    ClassicMenuItem(
                        id = s.path,
                        title = s.displayTitle,
                        subtitle = s.displayArtist,
                        rightText = s.formattedDuration,
                        hasSubmenu = false,
                        artworkUri = s.artworkUri,
                        song = s,
                        action = {
                            val idx = favs.indexOf(s)
                            playbackController.playQueue(favs, idx.coerceAtLeast(0))
                            navigateTo(MenuLevel.NOW_PLAYING)
                        }
                    )
                }
            }

            MenuLevel.FOLDERS -> folders.map { f ->
                ClassicMenuItem(
                    id = f.path,
                    title = f.name,
                    subtitle = "${f.songCount} songs",
                    hasSubmenu = true,
                    action = {
                        selectedFolderPath = f.path
                        navigateTo(MenuLevel.FOLDER_DETAIL)
                    }
                )
            }

            MenuLevel.FOLDER_DETAIL -> {
                val folderSongs = songs.filter { File(it.path).parent == selectedFolderPath }
                folderSongs.map { s ->
                    ClassicMenuItem(
                        id = s.path,
                        title = s.displayTitle,
                        subtitle = s.displayArtist,
                        rightText = s.formattedDuration,
                        hasSubmenu = false,
                        artworkUri = s.artworkUri,
                        song = s,
                        action = {
                            val idx = folderSongs.indexOf(s)
                            playbackController.playQueue(folderSongs, idx.coerceAtLeast(0))
                            navigateTo(MenuLevel.NOW_PLAYING)
                        }
                    )
                }
            }

            else -> emptyList()
        }
    }

    val highlightedItem = currentMenuItems.getOrNull(selectedIndex)

    // Handlers for click wheel gestures
    fun handleWheelClockwise() {
        if (currentLevel == MenuLevel.NOW_PLAYING) {
            playbackController.seekTo(playbackState.currentPositionMs + 5000L)
            triggerHapticTick()
        } else if (currentMenuItems.isNotEmpty()) {
            if (selectedIndex < currentMenuItems.size - 1) {
                selectedIndex++
                triggerHapticTick()
                coroutineScope.launch {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }
    }

    fun handleWheelCounterClockwise() {
        if (currentLevel == MenuLevel.NOW_PLAYING) {
            playbackController.seekTo((playbackState.currentPositionMs - 5000L).coerceAtLeast(0L))
            triggerHapticTick()
        } else if (currentMenuItems.isNotEmpty()) {
            if (selectedIndex > 0) {
                selectedIndex--
                triggerHapticTick()
                coroutineScope.launch {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }
    }

    fun handleCenterNavigateUp() {
        if (currentLevel == MenuLevel.NOW_PLAYING || selectedIndex <= 0) return
        selectedIndex--
        triggerHapticTick()
        coroutineScope.launch { listState.animateScrollToItem(selectedIndex) }
    }

    fun handleCenterNavigateDown() {
        if (currentLevel == MenuLevel.NOW_PLAYING || selectedIndex >= currentMenuItems.lastIndex) return
        selectedIndex++
        triggerHapticTick()
        coroutineScope.launch { listState.animateScrollToItem(selectedIndex) }
    }

    fun activateMenuItem(item: ClassicMenuItem) {
        item.action?.invoke() ?: when (item.id) {
            "music" -> navigateTo(MenuLevel.MUSIC)
            "all_songs" -> navigateTo(MenuLevel.SONGS)
            "albums" -> navigateTo(MenuLevel.ALBUMS)
            "artists" -> navigateTo(MenuLevel.ARTISTS)
            "folders" -> navigateTo(MenuLevel.FOLDERS)
            "radio" -> navigateTo(MenuLevel.RADIO)
            "playlists" -> navigateTo(MenuLevel.PLAYLISTS)
            "favorites" -> navigateTo(MenuLevel.FAVORITES)
            "appearance" -> navigateTo(MenuLevel.APPEARANCE_STUDIO)
            "visualizer_studio" -> {
                ensureVisualizerCapture()
                navigateTo(MenuLevel.VISUALIZER_STUDIO)
            }
            "extras" -> navigateTo(MenuLevel.EQUALIZER)
            "settings" -> navigateTo(MenuLevel.SETTINGS)
            "now_playing" -> navigateTo(MenuLevel.NOW_PLAYING)
            else -> Unit
        }
    }

    fun handleCenterClick() {
        triggerCenterSelect()
        if (currentLevel == MenuLevel.NOW_PLAYING) {
            playbackController.togglePlayPause()
        } else {
            currentMenuItems.getOrNull(selectedIndex)?.let(::activateMenuItem)
        }
    }

    // MAIN HARDWARE CHASSIS (TRUE FULLSCREEN: fills screen edge-to-edge covering top pull down)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ==========================================
            // TOP HALF: RECESSED SCREEN DISPLAY GLASS
            // ==========================================
            val radius = appearanceSettings.cornerRadiusStyle.radiusDp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.08f)
                    .shadow(16.dp, RoundedCornerShape(radius))
                    .clip(RoundedCornerShape(radius))
                    .background(colors.displayGlass)
                    .border(2.5.dp, colors.displayBorder, RoundedCornerShape(radius))
                    .clipToBounds()
            ) {
                // Animated content transitions between menu levels
                AnimatedContent(
                    targetState = currentLevel,
                    transitionSpec = {
                        if (animationsEnabled) {
                            (fadeIn(animationSpec = tween(220)) +
                                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200)) +
                                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200))
                                )
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "screenTransition"
                ) { targetLevel ->
                    when (targetLevel) {
                        MenuLevel.NOW_PLAYING -> {
                            ClassicNowPlayingDisplay(
                                playbackState = playbackState,
                                playbackController = playbackController,
                                visualizerSettings = visSettings,
                                waveform = waveform,
                                spectrum = spectrum,
                                captureAvailable = captureAvailable,
                                onToggleFavorite = { if (currentSong != null) onToggleFavorite(currentSong) },
                                onScanCurrentTrack = { currentSong?.let(onScanCurrentTrack) },
                                onOpenVisualizerStudio = {
                                    ensureVisualizerCapture()
                                    navigateTo(MenuLevel.VISUALIZER_STUDIO)
                                },
                                onBackToMenu = { navigateBack() }
                            )
                        }

                        MenuLevel.APPEARANCE_STUDIO -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AppearanceStudioScreen(
                                    currentSettings = appearanceSettings,
                                    onSaveSettings = onSaveAppearanceSettings,
                                    onResetDefaults = onResetAppearanceDefaults,
                                    onBack = { navigateBack() }
                                )
                            }
                        }

                        MenuLevel.EQUALIZER -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                EqualizerScreen(
                                    equalizerManager = playbackController.equalizerManager,
                                    onBack = { navigateBack() }
                                )
                            }
                        }

                        MenuLevel.VISUALIZER, MenuLevel.VISUALIZER_STUDIO -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (targetLevel == MenuLevel.VISUALIZER) {
                                    VisualizerStage(
                                        playbackState = playbackState,
                                        settings = visSettings,
                                        gif = selectedGif,
                                        waveform = waveform,
                                        leftWaveform = leftWaveform,
                                        rightWaveform = rightWaveform,
                                        spectrum = spectrum,
                                        captureAvailable = captureAvailable,
                                        statusMessage = visStatus
                                    )
                                } else {
                                    VisualizerStudioScreen(
                                        playbackState = playbackState,
                                        settings = visSettings,
                                        onSettingsChange = { persistVis(it) },
                                        gifStore = gifStore,
                                        waveform = waveform,
                                        leftWaveform = leftWaveform,
                                        rightWaveform = rightWaveform,
                                        spectrum = spectrum,
                                        captureAvailable = captureAvailable,
                                        statusMessage = visStatus,
                                        onBack = { navigateBack() },
                                        onOpenLiveView = { navigateTo(MenuLevel.VISUALIZER) }
                                    )
                                }
                            }
                        }

                        MenuLevel.SETTINGS -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                SettingsScreen(
                                    orbitDialEnabled = orbitDialEnabled,
                                    onToggleOrbitDial = onToggleOrbitDial,
                                    hapticFeedbackEnabled = hapticEnabled,
                                    onToggleHaptic = onToggleHaptic,
                                    darkTheme = colors.isDark,
                                    onToggleDarkTheme = {
                                        val newMode = if (it) ThemeMode.DARK else ThemeMode.LIGHT
                                        onSaveAppearanceSettings(appearanceSettings.copy(themeMode = newMode))
                                    },
                                    onOpenAppearanceStudio = { navigateTo(MenuLevel.APPEARANCE_STUDIO) },
                                    onOpenVisualizerStudio = {
                                        ensureVisualizerCapture()
                                        navigateTo(MenuLevel.VISUALIZER_STUDIO)
                                    },
                                    onNavigateToEqualizer = { navigateTo(MenuLevel.EQUALIZER) },
                                    onNavigateToMusicFolders = { navigateTo(MenuLevel.FOLDERS) },
                                    onNavigateToPlaylists = { navigateTo(MenuLevel.PLAYLISTS) },
                                    onNavigateToRadio = onNavigateToRadio,
                                    onRescanLibrary = onRescanLibrary,
                                    onClearHistory = onClearHistory,
                                    onBack = { navigateBack() }
                                )
                            }
                        }

                        else -> {
                            // SPLIT VIEW (REFERENCE DESIGN)
                            Column(modifier = Modifier.fillMaxSize()) {

                                // 1. Screen Status Bar Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.displayHeader)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Screen Title / Back Navigation
                                    if (targetLevel == MenuLevel.ROOT) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "YOUNI",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = colors.textPrimary,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "SPIN",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = colors.accent,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(colors.accent, CircleShape)
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { navigateBack() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = colors.accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = when (targetLevel) {
                                                    MenuLevel.MUSIC -> "Library"
                                                    MenuLevel.SONGS -> "All Songs"
                                                    MenuLevel.ALBUMS -> "Albums"
                                                    MenuLevel.ALBUM_DETAIL -> "Album"
                                                    MenuLevel.ARTISTS -> "Artists"
                                                    MenuLevel.ARTIST_DETAIL -> "Artist"
                                                    MenuLevel.RADIO -> "Radio"
                                                    MenuLevel.PLAYLISTS -> "Playlists"
                                                    MenuLevel.PLAYLIST_DETAIL -> "Playlist"
                                                    MenuLevel.FAVORITES -> "Favorites"
                                                    MenuLevel.FOLDERS -> "Folders"
                                                    MenuLevel.FOLDER_DETAIL -> "Folder"
                                                    else -> "Menu"
                                                },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = colors.textPrimary
                                                )
                                            )
                                        }
                                    }

                                    // Status Indicators: Mini Visualizer, Shuffle, Repeat, Play, Battery
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        if (playbackState.isPlaying) {
                                            MiniVisualizer(
                                                isPlaying = true,
                                                accentColor = colors.accent
                                            )
                                        }

                                        if (playbackState.shuffleMode) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = colors.accent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        if (playbackState.repeatMode != RepeatMode.OFF) {
                                            Icon(
                                                imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                                contentDescription = "Repeat",
                                                tint = colors.accent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Status",
                                            tint = if (playbackState.isPlaying) colors.accent else colors.textSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )

                                        Text(
                                            text = currentTime,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textSecondary
                                            ),
                                            maxLines = 1
                                        )

                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = "Weather symbol",
                                            tint = colors.accent,
                                            modifier = Modifier.size(13.dp)
                                        )

                                        // Battery Icon Gauge
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(10.dp)
                                                .border(1.dp, colors.textSecondary, RoundedCornerShape(2.dp))
                                                .padding(1.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(0.85f)
                                                    .background(Color(0xFF00E676), RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                }

                                // 2. Split Screen: Left Menu (50%) | Right Album Art (50%)
                                Row(modifier = Modifier.fillMaxSize()) {

                                    // LEFT COLUMN: Classic Menu List
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(colors.menuBackground)
                                    ) {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(vertical = 2.dp)
                                        ) {
                                            itemsIndexed(currentMenuItems) { index, item ->
                                                val isSelected = (index == selectedIndex)

                                                val rowModifier = if (isSelected) {
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .background(colors.selectedRowGradient)
                                                } else {
                                                    Modifier.fillMaxWidth()
                                                }

                                                Row(
                                                    modifier = rowModifier
                                                        .clickable {
                                                            selectedIndex = index
                                                            triggerHapticTick()
                                                            activateMenuItem(item)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 13.sp,
                                                                color = if (isSelected) colors.selectedRowText else colors.textPrimary
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (!item.subtitle.isNullOrBlank()) {
                                                            Text(
                                                                text = item.subtitle,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 10.sp,
                                                                    color = if (isSelected) colors.selectedRowSubtext else colors.textMuted
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    if (item.rightText != null) {
                                                        Text(
                                                            text = item.rightText,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) colors.selectedRowText else colors.textMuted
                                                            ),
                                                            modifier = Modifier.padding(start = 4.dp)
                                                        )
                                                    } else if (item.hasSubmenu) {
                                                        Icon(
                                                            imageVector = Icons.Default.ChevronRight,
                                                            contentDescription = null,
                                                            tint = if (isSelected) colors.selectedRowText else colors.textSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    ClassicArtworkPreview(
                                        playbackState = playbackState,
                                        waveform = waveform,
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BOTTOM HALF: THE HARDWARE CLICK WHEEL
            // ==========================================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.92f),
                contentAlignment = Alignment.Center
            ) {
                val dialSize = min(maxWidth, maxHeight).coerceIn(168.dp, 270.dp)
                ClassicClickWheel(
                    dialSizeDp = dialSize.value.toInt(),
                    isPlaying = playbackState.isPlaying,
                    animationsEnabled = animationsEnabled,
                    colors = colors,
                    onRotateClockwise = { handleWheelClockwise() },
                    onRotateCounterClockwise = { handleWheelCounterClockwise() },
                    onCenterClick = { handleCenterClick() },
                    onCenterNavigateUp = { handleCenterNavigateUp() },
                    onCenterNavigateDown = { handleCenterNavigateDown() },
                    onMenuClick = {
                        triggerHapticTick()
                        navigateBack()
                    },
                    onMenuLongPress = {
                        triggerHapticTick()
                        navigateToRoot()
                    },
                    onPlayPauseClick = {
                        triggerHapticTick()
                        playbackController.togglePlayPause()
                    },
                    onPreviousClick = {
                        triggerHapticTick()
                        playbackController.skipToPrevious()
                    },
                    onPreviousHoldStart = {
                        triggerHapticTick()
                        playbackController.startSeeking(forward = false)
                    },
                    onPreviousHoldEnd = { playbackController.stopSeeking() },
                    onNextClick = {
                        triggerHapticTick()
                        playbackController.skipToNext()
                    },
                    onNextHoldStart = {
                        triggerHapticTick()
                        playbackController.startSeeking(forward = true)
                    },
                    onNextHoldEnd = { playbackController.stopSeeking() }
                )
            }
        }
    }
}

/**
 * Dedicated Now Playing display inside the screen glass
 */
@Composable
private fun ClassicNowPlayingDisplay(
    playbackState: com.youni.younispin.playback.PlaybackState,
    playbackController: PlaybackController,
    visualizerSettings: com.youni.younispin.visualizer.VisualizerSettings,
    waveform: FloatArray,
    spectrum: FloatArray,
    captureAvailable: Boolean,
    onToggleFavorite: () -> Unit,
    onScanCurrentTrack: () -> Unit,
    onOpenVisualizerStudio: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val currentSong = playbackState.currentSong
    val colors = YouniTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.displayGlass)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBackToMenu() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenVisualizerStudio,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Open Visualizer Studio",
                        tint = colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Shuffle Button
                IconButton(
                    onClick = { playbackController.toggleShuffle() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleMode) colors.accent else colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { playbackController.cycleRepeatMode() },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) colors.accent else colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (currentSong != null && !currentSong.isStream) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (currentSong.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentSong.isFavorite) Color(0xFFFF4D4F) else colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onScanCurrentTrack,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Scan track metadata online",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = if (playbackState.isPlaying) "PLAYING" else "PAUSED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (playbackState.isPlaying) colors.accent else colors.secondaryAccent,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Center: Album Art + Track Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artworkUri = if (currentSong?.isStream == true) {
                playbackState.radioTrackArtworkUrl ?: playbackState.radioStation?.logoUrl
            } else {
                currentSong?.artworkUri
            }
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
            ) {
                if (artworkUri.isNullOrBlank()) {
                    ThemedAlbumPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    SubcomposeAsyncImage(
                        model = artworkUri,
                        contentDescription = if (currentSong?.isStream == true) {
                            if (playbackState.radioTrackArtworkUrl != null) {
                                "${currentSong.displayTitle} track artwork"
                            } else {
                                "${playbackState.radioStation?.name ?: "Radio"} station artwork"
                            }
                        } else {
                            currentSong?.displayTitle
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        if (painter.state is coil.compose.AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            ThemedAlbumPlaceholder(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playbackState.streamMetadataTitle ?: currentSong?.displayTitle ?: "Ready",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.displayArtist ?: "Select music",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.displayAlbum ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Format & Bitrate pill
                val formatTag = currentSong?.path?.substringAfterLast('.', "")?.uppercase()
                if (!formatTag.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = formatTag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                if (captureAvailable && playbackState.isPlaying) {
                    LiveAudioVisualizer(
                        waveform = waveform,
                        spectrum = spectrum,
                        isPlaying = true,
                        settings = visualizerSettings.copy(
                            style = VisualizerStyle.CLASSIC_EQ,
                            barCount = 20,
                            waveformEnabled = false,
                            spectrumEnabled = true
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                }
            }
        }

        // Bottom: Progress bar & Timings
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { playbackState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.accent,
                trackColor = colors.border
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val curSec = playbackState.currentPositionMs / 1000
                val durSec = playbackState.durationMs / 1000
                Text(
                    text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = colors.textMuted)
                )
                Text(
                    text = String.format("%02d:%02d", durSec / 60, durSec % 60),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = colors.textMuted)
                )
            }
        }
    }
}

/**
 * Tactile Hardware Click Wheel with spring-press physics & theme adaptation
 */
@Composable
private fun ClassicClickWheel(
    dialSizeDp: Int = 270,
    isPlaying: Boolean = false,
    animationsEnabled: Boolean = true,
    colors: YouniColors = YouniTheme.colors,
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {},
    onCenterClick: () -> Unit = {},
    onCenterNavigateUp: () -> Unit = {},
    onCenterNavigateDown: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onMenuLongPress: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onPreviousHoldStart: () -> Unit = {},
    onPreviousHoldEnd: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNextHoldStart: () -> Unit = {},
    onNextHoldEnd: () -> Unit = {}
) {
    var accumulatedAngle by remember { mutableFloatStateOf(0f) }
    val angleThresholdDegrees = 12f
    var menuPressed by remember { mutableStateOf(false) }
    var menuLongPressed by remember { mutableStateOf(false) }
    var previousPressed by remember { mutableStateOf(false) }
    var previousHeld by remember { mutableStateOf(false) }
    var nextPressed by remember { mutableStateOf(false) }
    var nextHeld by remember { mutableStateOf(false) }
    var centerPressed by remember { mutableStateOf(false) }
    var joystickDirection by remember { mutableIntStateOf(0) }

    val playSource = remember { MutableInteractionSource() }
    val isPlayPressed by playSource.collectIsPressedAsState()
    val centerScale by animateFloatAsState(
        targetValue = if (centerPressed && animationsEnabled) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "centerScale"
    )
    val centerOffsetY by animateFloatAsState(
        targetValue = if (animationsEnabled) joystickDirection * 4f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "centerJoystickOffset"
    )
    val menuScale by animateFloatAsState(
        targetValue = if (menuPressed && animationsEnabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "menuScale"
    )
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed && animationsEnabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playScale"
    )
    val previousScale by animateFloatAsState(
        targetValue = if (previousPressed && animationsEnabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "previousScale"
    )
    val nextScale by animateFloatAsState(
        targetValue = if (nextPressed && animationsEnabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nextScale"
    )
    Box(
        modifier = Modifier.size(dialSizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Wheel Disc with circular touch tracking
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.dialDisc)
                .border(2.dp, colors.dialDiscBorder, CircleShape)
                .pointerInput(Unit) {
                    var lastAngleRad: Float? = null
                    var centerGesture = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            centerGesture = (offset - center).getDistance() <= minOf(size.width, size.height) * 0.24f
                            lastAngleRad = if (centerGesture) null
                            else atan2(offset.y - center.y, offset.x - center.x)
                        },
                        onDrag = { change, _ ->
                            if (!centerGesture) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val currentAngleRad = atan2(change.position.y - center.y, change.position.x - center.x)
                                val prev = lastAngleRad

                                if (prev != null) {
                                    var deltaRad = currentAngleRad - prev
                                    if (deltaRad > PI) deltaRad -= (2 * PI).toFloat()
                                    if (deltaRad < -PI) deltaRad += (2 * PI).toFloat()

                                    val deltaDeg = (deltaRad * 180f / PI).toFloat()
                                    accumulatedAngle += deltaDeg

                                    if (accumulatedAngle >= angleThresholdDegrees) {
                                        onRotateClockwise()
                                        accumulatedAngle = 0f
                                    } else if (accumulatedAngle <= -angleThresholdDegrees) {
                                        onRotateCounterClockwise()
                                        accumulatedAngle = 0f
                                    }
                                }
                                lastAngleRad = currentAngleRad
                            }
                        },
                        onDragEnd = {
                            lastAngleRad = null
                            accumulatedAngle = 0f
                            centerGesture = false
                        },
                        onDragCancel = {
                            lastAngleRad = null
                            accumulatedAngle = 0f
                            centerGesture = false
                        }
                    )
                }
        ) {
            // Concentric decorative machined ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .border(0.5.dp, colors.dialRingBorder, CircleShape)
            )

            // TOP: MENU
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .scale(menuScale)
                    .pressAndHold(
                        onTap = onMenuClick,
                        onHoldStart = {
                            menuLongPressed = true
                            onMenuLongPress()
                        },
                        onHoldEnd = { menuLongPressed = false },
                        onPressedChange = { menuPressed = it }
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                AnimatedContent(
                    targetState = menuLongPressed,
                    transitionSpec = {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(80))
                    },
                    label = "menuLabel"
                ) { pressed ->
                    Text(
                        text = if (pressed) "ROOT" else "MENU",
                        color = if (pressed) colors.accent else colors.dialLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // BOTTOM: PLAY / PAUSE (▶❚❚)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(CircleShape)
                    .scale(playScale)
                    .clickable(
                        interactionSource = playSource,
                        indication = ripple(bounded = true, color = colors.accent),
                        onClick = onPlayPauseClick
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(tween(110)) + scaleIn(initialScale = 0.75f, animationSpec = tween(110)))
                            .togetherWith(fadeOut(tween(80)) + scaleOut(targetScale = 0.75f, animationSpec = tween(80)))
                    },
                    label = "playPauseMorph"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = if (playing) colors.accent else colors.dialLabel,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // LEFT: PREVIOUS (◀◀)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clip(CircleShape)
                    .scale(previousScale)
                    .pressAndHold(
                        onTap = onPreviousClick,
                        onHoldStart = {
                            previousHeld = true
                            onPreviousHoldStart()
                        },
                        onHoldEnd = {
                            previousHeld = false
                            onPreviousHoldEnd()
                        },
                        onPressedChange = { previousPressed = it }
                    )
                    .padding(14.dp)
            ) {
                AnimatedContent(
                    targetState = previousHeld,
                    transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(70)) },
                    label = "rewindIndicator"
                ) { pressed ->
                    Text(
                        text = if (pressed) "REW" else "◀◀",
                        color = if (pressed) colors.accent else colors.dialLabel,
                        fontSize = if (pressed) 10.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // RIGHT: NEXT (▶▶)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .clip(CircleShape)
                    .scale(nextScale)
                    .pressAndHold(
                        onTap = onNextClick,
                        onHoldStart = {
                            nextHeld = true
                            onNextHoldStart()
                        },
                        onHoldEnd = {
                            nextHeld = false
                            onNextHoldEnd()
                        },
                        onPressedChange = { nextPressed = it }
                    )
                    .padding(14.dp)
            ) {
                AnimatedContent(
                    targetState = nextHeld,
                    transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(70)) },
                    label = "fastForwardIndicator"
                ) { pressed ->
                    Text(
                        text = if (pressed) "FWD" else "▶▶",
                        color = if (pressed) colors.accent else colors.dialLabel,
                        fontSize = if (pressed) 10.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center Circular Hub Action Button
        val centerHubSize = (dialSizeDp * 0.38f).dp
        Box(
            modifier = Modifier
                .size(centerHubSize)
                .scale(centerScale)
                .offset(y = centerOffsetY.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.dialHub)
                .border(1.5.dp, colors.dialHubBorder, CircleShape)
                .centerJoystick(
                    onTap = onCenterClick,
                    onMove = { direction ->
                        if (direction < 0) onCenterNavigateUp() else onCenterNavigateDown()
                    },
                    onDirectionChanged = { joystickDirection = it },
                    onPressedChanged = { centerPressed = it }
                )
                .semantics {
                    contentDescription = "Select. Drag up for previous item or down for next item."
                    onClick(label = "Select") {
                        onCenterClick()
                        true
                    }
                    customActions = listOf(
                        androidx.compose.ui.semantics.CustomAccessibilityAction("Previous item") {
                            onCenterNavigateUp()
                            true
                        },
                        androidx.compose.ui.semantics.CustomAccessibilityAction("Next item") {
                            onCenterNavigateDown()
                            true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (joystickDirection != 0) {
                Icon(
                    imageVector = if (joystickDirection < 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) colors.secondaryAccent else colors.dialRingBorder)
                )
            }
        }
    }
}
