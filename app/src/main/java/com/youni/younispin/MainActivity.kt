package com.youni.younispin

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.youni.younispin.data.model.Song
import com.youni.younispin.playback.PlaybackController
import com.youni.younispin.ui.Screen
import com.youni.younispin.ui.screens.*
import com.youni.younispin.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playbackController: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        lockPortrait()
        super.onCreate(savedInstanceState)

        // Make app true fullscreen covering the top pull down
        applyFullscreen()

        val app = application as YouniSpinApp
        val musicRepo = app.musicRepository
        val radioRepo = app.radioRepository

        playbackController = PlaybackController(this, radioRepo) { song ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                musicRepo.recordPlayed(song)
            }
        }

        setContent {
            val scope = rememberCoroutineScope()
            val appearanceManager = remember { AppearanceManager(this@MainActivity) }
            var appearanceSettings by remember { mutableStateOf(appearanceManager.loadSettings()) }

            val prefs = remember { getSharedPreferences("younispin_prefs", Context.MODE_PRIVATE) }
            var orbitDialEnabled by remember { mutableStateOf(prefs.getBoolean("orbit_dial_enabled", true)) }
            var hapticFeedbackEnabled by remember { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }

            // Collect Repository flows
            val songs by musicRepo.songsFlow.collectAsState()
            val albums by remember { musicRepo.getAlbumsFlow() }.collectAsState(initial = emptyList())
            val artists by remember { musicRepo.getArtistsFlow() }.collectAsState(initial = emptyList())
            val playlists by remember { musicRepo.getPlaylistsFlow() }.collectAsState(initial = emptyList())
            val folders by remember { musicRepo.getFoldersFlow() }.collectAsState(initial = emptyList())
            val radioStations by radioRepo.allStations.collectAsState(initial = emptyList())

            val playbackState by playbackController.state.collectAsState()
            val currentSong = playbackState.currentSong

            // Permission Request
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val audioGranted = perms[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                        perms[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                if (audioGranted) {
                    scope.launch { musicRepo.scanLocalMusic() }
                } else {
                    Toast.makeText(this, "Audio permission needed to browse local music", Toast.LENGTH_LONG).show()
                }
            }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                if (neededPermissions.isNotEmpty()) {
                    permissionLauncher.launch(neededPermissions.toTypedArray())
                } else {
                    musicRepo.scanLocalMusic()
                }
            }

            YouniSpinTheme(appearanceSettings = appearanceSettings, fullscreen = true) {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                val colors = YouniTheme.colors

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.HOME) {
                            ClassicPlayerScreen(
                                playbackController = playbackController,
                                songs = songs,
                                albums = albums,
                                artists = artists,
                                playlists = playlists,
                                folders = folders,
                                radioStations = radioStations,
                                onToggleFavorite = { song -> scope.launch { musicRepo.toggleFavorite(song) } },
                                onSaveOverride = { s, t, a, al, aa, g, y, tn, dn, c, art ->
                                    scope.launch {
                                        musicRepo.saveTrackOverride(s, t, a, al, aa, g, y, tn, dn, c, art)
                                    }
                                },
                                onRescanLibrary = {
                                    scope.launch {
                                        musicRepo.scanLocalMusic()
                                        Toast.makeText(this@MainActivity, "Library rescan complete", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClearHistory = {
                                    scope.launch {
                                        app.database.historyDao().clearHistory()
                                        Toast.makeText(this@MainActivity, "Listening history cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onNavigateToRadio = { navController.navigate(Screen.RADIO) },
                                onOpenPlaylistManager = { navController.navigate(Screen.PLAYLISTS) },
                                radioPanel = { onBack ->
                                    RadioScreen(
                                        stations = radioStations,
                                        playbackController = playbackController,
                                        radioRepository = radioRepo,
                                        onPlayStation = { station ->
                                            scope.launch {
                                                runCatching { radioRepo.resolveStream(station) }
                                                    .onSuccess {
                                                        radioRepo.recordStationPlayed(station)
                                                        playbackController.playRadioStream(it)
                                                    }
                                                    .onFailure {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            it.localizedMessage ?: "Unable to resolve radio stream",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                            }
                                        },
                                        onAddStation = { name, url, genre, logo, description, homepage ->
                                            scope.launch { radioRepo.addStation(name, url, genre, logo, description, homepage) }
                                        },
                                        onUpdateStation = { station -> scope.launch { radioRepo.updateStation(station) } },
                                        onDeleteStation = { station -> scope.launch { radioRepo.deleteStation(station) } },
                                        onToggleFavorite = { station, favorite ->
                                            scope.launch { radioRepo.setFavorite(station, favorite) }
                                        },
                                        onImportPlaylist = { content ->
                                            scope.launch {
                                                val imported = radioRepo.importM3U(content)
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Imported $imported stations",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onExportPlaylist = {},
                                        onBack = onBack
                                    )
                                },
                                onScanCurrentTrack = { song ->
                                    navController.navigate(
                                        Screen.metadataEditor(Uri.encode(song.path), scanOnline = true)
                                    )
                                },
                                onLoadPlaylistSongs = { playlistId ->
                                    musicRepo.getPlaylistSongs(playlistId)
                                },
                                orbitDialEnabled = orbitDialEnabled,
                                onToggleOrbitDial = {
                                    orbitDialEnabled = it
                                    prefs.edit().putBoolean("orbit_dial_enabled", it).apply()
                                },
                                hapticEnabled = hapticFeedbackEnabled,
                                onToggleHaptic = {
                                    hapticFeedbackEnabled = it
                                    prefs.edit().putBoolean("haptic_enabled", it).apply()
                                },
                                appearanceSettings = appearanceSettings,
                                onSaveAppearanceSettings = {
                                    appearanceSettings = it
                                    appearanceManager.saveSettings(it)
                                },
                                onResetAppearanceDefaults = {
                                    appearanceSettings = appearanceManager.resetToDefaults()
                                }
                            )
                        }

                        composable(Screen.APPEARANCE_STUDIO) {
                            AppearanceStudioScreen(
                                currentSettings = appearanceSettings,
                                onSaveSettings = {
                                    appearanceSettings = it
                                    appearanceManager.saveSettings(it)
                                },
                                onResetDefaults = {
                                    appearanceSettings = appearanceManager.resetToDefaults()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.SONGS) {
                            SongsScreen(
                                playbackController = playbackController,
                                songs = songs,
                                playlists = playlists,
                                onToggleFavorite = { song -> scope.launch { musicRepo.toggleFavorite(song) } },
                                onAddToPlaylist = { pId, path -> scope.launch { musicRepo.addTrackToPlaylist(pId, path) } },
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ALBUMS) {
                            AlbumsScreen(
                                albums = albums,
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.ALBUM_DETAIL,
                            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                        ) { backStack ->
                            val albumId = backStack.arguments?.getLong("albumId") ?: 0L
                            AlbumDetailScreen(
                                albumId = albumId,
                                allSongs = songs,
                                playbackController = playbackController,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ARTISTS) {
                            ArtistsScreen(
                                artists = artists,
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.ARTIST_DETAIL,
                            arguments = listOf(navArgument("artistId") { type = NavType.LongType })
                        ) { backStack ->
                            val artistId = backStack.arguments?.getLong("artistId") ?: 0L
                            ArtistDetailScreen(
                                artistId = artistId,
                                allSongs = songs,
                                playbackController = playbackController,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.PLAYLISTS) {
                            PlaylistsScreen(
                                playlists = playlists,
                                onCreatePlaylist = { name -> scope.launch { musicRepo.createPlaylist(name) } },
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.PLAYLIST_DETAIL,
                            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                        ) { backStack ->
                            val playlistId = backStack.arguments?.getLong("playlistId") ?: 0L
                            var playlistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
                            LaunchedEffect(playlistId, songs) {
                                playlistSongs = musicRepo.getPlaylistSongs(playlistId)
                            }
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlists = playlists,
                                playlistSongs = playlistSongs,
                                playbackController = playbackController,
                                onRemoveTrack = { plId, path ->
                                    scope.launch {
                                        musicRepo.removeTrackFromPlaylist(plId, path)
                                        playlistSongs = musicRepo.getPlaylistSongs(plId)
                                    }
                                },
                                onDeletePlaylist = { plId -> scope.launch { musicRepo.deletePlaylist(plId) } },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.FAVORITES) {
                            val favoriteSongs = remember(songs) { songs.filter { it.isFavorite } }
                            SongsScreen(
                                playbackController = playbackController,
                                songs = favoriteSongs,
                                playlists = playlists,
                                onToggleFavorite = { song -> scope.launch { musicRepo.toggleFavorite(song) } },
                                onAddToPlaylist = { pId, path -> scope.launch { musicRepo.addTrackToPlaylist(pId, path) } },
                                onNavigate = { navController.navigate(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.FOLDERS) {
                            FoldersScreen(
                                folders = folders,
                                allSongs = songs,
                                playbackController = playbackController,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.RADIO) {
                            RadioScreen(
                                stations = radioStations,
                                playbackController = playbackController,
                                radioRepository = radioRepo,
                                onPlayStation = { station ->
                                    scope.launch {
                                        runCatching { radioRepo.resolveStream(station) }
                                            .onSuccess {
                                                radioRepo.recordStationPlayed(station)
                                                playbackController.playRadioStream(it)
                                            }
                                            .onFailure {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    it.localizedMessage ?: "Unable to resolve radio stream",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                },
                                onAddStation = { name, url, genre, logo, description, homepage ->
                                    scope.launch { radioRepo.addStation(name, url, genre, logo, description, homepage) }
                                },
                                onUpdateStation = { station -> scope.launch { radioRepo.updateStation(station) } },
                                onDeleteStation = { st -> scope.launch { radioRepo.deleteStation(st) } },
                                onToggleFavorite = { station, isFav -> scope.launch { radioRepo.setFavorite(station, isFav) } },
                                onImportPlaylist = { content ->
                                    scope.launch {
                                        val imported = radioRepo.importM3U(content)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Imported $imported stations",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onExportPlaylist = {},
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.NOW_PLAYING) {
                            NowPlayingScreen(
                                playbackController = playbackController,
                                onToggleFavorite = { song -> scope.launch { musicRepo.toggleFavorite(song) } },
                                onNavigate = { route -> navController.navigate(route) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.EQUALIZER) {
                            EqualizerScreen(
                                equalizerManager = playbackController.equalizerManager,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.METADATA_EDITOR,
                            arguments = listOf(
                                navArgument("trackPath") { type = NavType.StringType },
                                navArgument("scan") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStack ->
                            val rawPath = backStack.arguments?.getString("trackPath") ?: ""
                            val trackPath = Uri.decode(rawPath)
                            val scanOnline = backStack.arguments?.getBoolean("scan") ?: false
                            val song = songs.find { it.path == trackPath }
                            if (song != null) {
                                MetadataEditorScreen(
                                    song = song,
                                    onSaveOverride = { s, t, a, al, aa, g, y, tn, dn, c, art ->
                                        scope.launch {
                                            musicRepo.saveTrackOverride(s, t, a, al, aa, g, y, tn, dn, c, art)
                                        }
                                    },
                                    scanOnOpen = scanOnline,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable(Screen.SETTINGS) {
                            SettingsScreen(
                                orbitDialEnabled = orbitDialEnabled,
                                onToggleOrbitDial = {
                                    orbitDialEnabled = it
                                    prefs.edit().putBoolean("orbit_dial_enabled", it).apply()
                                },
                                hapticFeedbackEnabled = hapticFeedbackEnabled,
                                onToggleHaptic = {
                                    hapticFeedbackEnabled = it
                                    prefs.edit().putBoolean("haptic_enabled", it).apply()
                                },
                                darkTheme = appearanceSettings.themeMode != ThemeMode.LIGHT,
                                onToggleDarkTheme = { isDark ->
                                    val newMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
                                    val updated = appearanceSettings.copy(themeMode = newMode)
                                    appearanceSettings = updated
                                    appearanceManager.saveSettings(updated)
                                },
                                audioQuality = playbackState.audioQuality,
                                onOpenAppearanceStudio = {
                                    navController.navigate(Screen.APPEARANCE_STUDIO)
                                },
                                onNavigateToEqualizer = { navController.navigate(Screen.EQUALIZER) },
                                onNavigateToMusicFolders = { navController.navigate(Screen.FOLDERS) },
                                onNavigateToPlaylists = { navController.navigate(Screen.PLAYLISTS) },
                                onNavigateToRadio = { navController.navigate(Screen.RADIO) },
                                onRescanLibrary = {
                                    scope.launch {
                                        musicRepo.scanLocalMusic()
                                        Toast.makeText(this@MainActivity, "Library rescan complete", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClearHistory = {
                                    scope.launch {
                                        app.database.historyDao().clearHistory()
                                        Toast.makeText(this@MainActivity, "Listening history cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Persistent Mini Player (visible when music is loaded and user is not on Now Playing screen or Home)
                    val showMiniPlayer = currentSong != null && currentRoute != Screen.NOW_PLAYING && currentRoute != Screen.HOME
                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { navController.navigate(Screen.NOW_PLAYING) }
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
                            color = colors.surfaceElevated,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = currentSong?.artworkUri ?: R.drawable.ic_placeholder_album,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playbackState.streamMetadataTitle ?: currentSong?.displayTitle ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentSong?.displayArtist ?: "",
                                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { playbackController.togglePlayPause() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(colors.accent, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        tint = if (colors.isDark) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { playbackController.skipToNext() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("SourceLockedOrientationActivity", "DEPRECATION")
    private fun lockPortrait() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val params = window.attributes
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.attributes = params
    }

    private fun applyFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lockPortrait()
        applyFullscreen()
    }

    override fun onResume() {
        super.onResume()
        lockPortrait()
        applyFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullscreen()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            playbackController.release()
        }
        super.onDestroy()
    }
}
