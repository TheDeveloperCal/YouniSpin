package com.youni.younispin.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.youni.younispin.BuildConfig
import com.youni.younispin.R
import com.youni.younispin.playback.AudioQualityInfo
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun SettingsScreen(
    orbitDialEnabled: Boolean,
    onToggleOrbitDial: (Boolean) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    audioQuality: AudioQualityInfo? = null,
    onOpenAppearanceStudio: () -> Unit = {},
    onOpenVisualizerStudio: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToMusicFolders: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onRescanLibrary: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    val colors = YouniTheme.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = remember { context.getSharedPreferences("younispin_prefs", Context.MODE_PRIVATE) }

    // Persistent playback & library preferences
    var crossfadeEnabled by remember { mutableStateOf(prefs.getBoolean("crossfade_enabled", false)) }
    var volumeNormEnabled by remember { mutableStateOf(prefs.getBoolean("volume_norm_enabled", true)) }
    var headsetControlsEnabled by remember { mutableStateOf(prefs.getBoolean("headset_controls_enabled", true)) }
    var highQualityStreaming by remember { mutableStateOf(prefs.getBoolean("high_quality_streaming", true)) }

    // Dialog states
    var showFormatMatrixDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showPlaybackPrefsDialog by remember { mutableStateOf(false) }
    var showStreamingPrefsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "Hardware, Audio & Preferences",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )
                }
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // 1. PLAYBACK
            // ==========================================
            item {
                SettingsSectionHeader(title = "PLAYBACK", color = colors.textSecondary)
                SettingsCardGroup {
                    SettingsNavigationRow(
                        icon = Icons.Default.Audiotrack,
                        title = "Audio Quality",
                        subtitle = audioQuality?.summary ?: "No track loaded",
                        onClick = { showAudioQualityDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.GraphicEq,
                        title = "Audio & Equalizer",
                        subtitle = "5-band parametric EQ, BassBoost, Virtualizer",
                        onClick = onNavigateToEqualizer
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.Tune,
                        title = "Playback Preferences",
                        subtitle = "Audio focus, crossfade, replay-gain tuning",
                        onClick = { showPlaybackPrefsDialog = true }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Speed,
                        title = "Smooth Crossfade",
                        subtitle = "2-second seamless gapless track blend",
                        checked = crossfadeEnabled,
                        onCheckedChange = {
                            crossfadeEnabled = it
                            prefs.edit().putBoolean("crossfade_enabled", it).apply()
                        }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Volume Normalization",
                        subtitle = "Consistent listening level across varied masters",
                        checked = volumeNormEnabled,
                        onCheckedChange = {
                            volumeNormEnabled = it
                            prefs.edit().putBoolean("volume_norm_enabled", it).apply()
                        }
                    )
                }
            }

            // ==========================================
            // 2. MUSIC LIBRARY
            // ==========================================
            item {
                SettingsSectionHeader(title = "MUSIC LIBRARY", color = colors.textSecondary)
                SettingsCardGroup {
                    SettingsNavigationRow(
                        icon = Icons.Default.Folder,
                        title = "Music Folders",
                        subtitle = "Browse filesystem directories & storage paths",
                        onClick = onNavigateToMusicFolders
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Default.Sync,
                        title = "Library Scanning",
                        subtitle = "Rescan internal storage & SD card for new tracks",
                        actionLabel = "Rescan",
                        onClick = onRescanLibrary
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.QueueMusic,
                        title = "Playlist Management",
                        subtitle = "Create, reorder, and export custom playlists",
                        onClick = onNavigateToPlaylists
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Metadata & Format Matrix",
                        subtitle = "Audio codecs, tag inspection, lossless decode status",
                        onClick = { showFormatMatrixDialog = true }
                    )
                }
            }

            // ==========================================
            // 3. STREAMING
            // ==========================================
            item {
                SettingsSectionHeader(title = "STREAMING", color = colors.textSecondary)
                SettingsCardGroup {
                    SettingsNavigationRow(
                        icon = Icons.Default.Radio,
                        title = "Radio & Internet Streams",
                        subtitle = "Live Shoutcast / Icecast streams & station browser",
                        onClick = onNavigateToRadio
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.Bookmarks,
                        title = "Saved Stations",
                        subtitle = "Manage curated & favorite radio frequencies",
                        onClick = onNavigateToRadio
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.CloudQueue,
                        title = "Network Playback Options",
                        subtitle = "Buffer depth, mobile data streaming rules",
                        onClick = { showStreamingPrefsDialog = true }
                    )
                }
            }

            // ==========================================
            // 4. APPEARANCE
            // ==========================================
            item {
                SettingsSectionHeader(title = "APPEARANCE", color = colors.textSecondary)
                SettingsCardGroup {
                    // Appearance Studio Entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.accent)
                            ) { onOpenAppearanceStudio() }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Appearance Studio",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.accent)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "STUDIO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.background,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Custom HEX colors, rotary dial finishes, live preview",
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    SettingsDivider()

                    SettingsNavigationRow(
                        icon = Icons.Default.GraphicEq,
                        title = "Visualizer Studio",
                        subtitle = "Waveforms, spectrum, animated GIF backgrounds",
                        onClick = onOpenVisualizerStudio
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        icon = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title = if (darkTheme) "Graphite Dark Theme" else "Pristine Light Theme",
                        subtitle = if (darkTheme) "Deep graphite hardware aesthetic" else "Clean porcelain & silver aesthetic",
                        checked = !darkTheme,
                        onCheckedChange = { lightActive ->
                            onToggleDarkTheme(!lightActive)
                        }
                    )

                    SettingsDivider()

                    SettingsNavigationRow(
                        icon = Icons.Default.RotateRight,
                        title = "Orbit Dial Appearance",
                        subtitle = "Graphite, Silver Alloy, Ceramic White finishes",
                        onClick = onOpenAppearanceStudio
                    )
                }
            }

            // ==========================================
            // 5. CONTROLS & ACCESSIBILITY
            // ==========================================
            item {
                SettingsSectionHeader(title = "CONTROLS & ACCESSIBILITY", color = colors.textSecondary)
                SettingsCardGroup {
                    SettingsSwitchRow(
                        icon = Icons.Default.RadioButtonChecked,
                        title = "Orbit Dial Controls",
                        subtitle = "Display rotary click wheel navigation on screens",
                        checked = orbitDialEnabled,
                        onCheckedChange = onToggleOrbitDial
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Tactile mechanical clock tick on rotary gestures",
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onToggleHaptic
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Headphones,
                        title = "Hardware & Headset Controls",
                        subtitle = "Respond to headset play/pause and volume keys",
                        checked = headsetControlsEnabled,
                        onCheckedChange = {
                            headsetControlsEnabled = it
                            prefs.edit().putBoolean("headset_controls_enabled", it).apply()
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.AccessibilityNew,
                        title = "Reduced Motion & Display",
                        subtitle = "Configure transitions, scaling, and contrast",
                        onClick = onOpenAppearanceStudio
                    )
                }
            }

            // ==========================================
            // 6. ABOUT & SUPPORT
            // ==========================================
            item {
                SettingsSectionHeader(title = "ABOUT & SUPPORT", color = colors.textSecondary)
                SettingsCardGroup {
                    SettingsNavigationRow(
                        icon = Icons.Default.Info,
                        title = "About YouniSpin",
                        subtitle = "Version ${BuildConfig.VERSION_NAME} • Your Music. Your Way.",
                        onClick = { showAboutDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.Description,
                        title = "Open Source Licenses",
                        subtitle = "ExoPlayer, Media3, Compose, Coil, Room",
                        onClick = { showLicensesDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.Shield,
                        title = "Privacy Policy",
                        subtitle = "Strict local-first philosophy, zero telemetry",
                        onClick = { showPrivacyDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // OFFICIAL LIBERAPAY DONATE BUTTON
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.accent)
                        ) {
                            try {
                                uriHandler.openUri("https://liberapay.com/DeveloperCal/")
                            } catch (_: Exception) {
                                Toast.makeText(context, "Opening Liberapay in browser...", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .semantics { contentDescription = "Donate via Liberapay to support continued development" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.accent.copy(alpha = if (colors.isDark) 0.12f else 0.08f)
                    ),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = colors.background,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Donate",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = "Support YouniSpin's continued development.",
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Liberapay",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom padding for scroll clearance
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // Format Matrix Dialog
    if (showFormatMatrixDialog) {
        FormatMatrixDialog(onDismiss = { showFormatMatrixDialog = false })
    }

    if (showAudioQualityDialog) {
        AudioQualityDialog(
            audioQuality = audioQuality,
            onDismiss = { showAudioQualityDialog = false }
        )
    }

    // Playback Preferences Dialog
    if (showPlaybackPrefsDialog) {
        PlaybackPreferencesDialog(
            crossfade = crossfadeEnabled,
            onCrossfadeChange = {
                crossfadeEnabled = it
                prefs.edit().putBoolean("crossfade_enabled", it).apply()
            },
            volumeNorm = volumeNormEnabled,
            onVolumeNormChange = {
                volumeNormEnabled = it
                prefs.edit().putBoolean("volume_norm_enabled", it).apply()
            },
            onClearHistory = onClearHistory,
            onDismiss = { showPlaybackPrefsDialog = false }
        )
    }

    // Streaming Preferences Dialog
    if (showStreamingPrefsDialog) {
        StreamingPreferencesDialog(
            highQuality = highQualityStreaming,
            onHighQualityChange = {
                highQualityStreaming = it
                prefs.edit().putBoolean("high_quality_streaming", it).apply()
            },
            onDismiss = { showStreamingPrefsDialog = false }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }

    // About YouniSpin Dialog (with Liberapay link)
    if (showAboutDialog) {
        AboutDialog(
            onOpenDonate = {
                try {
                    uriHandler.openUri("https://liberapay.com/DeveloperCal/")
                } catch (_: Exception) {
                    Toast.makeText(context, "Opening Liberapay in browser...", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAboutDialog = false }
        )
    }
}

// ==========================================
// REUSABLE MINIMALIST SETTINGS COMPONENTS
// ==========================================

@Composable
private fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.2.sp,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCardGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = YouniTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    val colors = YouniTheme.colors
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = YouniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.accent)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onClick: () -> Unit
) {
    val colors = YouniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.accent)
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = YouniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.accent)
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.accent,
                checkedTrackColor = colors.accent.copy(alpha = 0.4f),
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surfaceElevated
            )
        )
    }
}

// ==========================================
// SETTINGS DIALOGS
// ==========================================

@Composable
private fun AudioQualityDialog(
    audioQuality: AudioQualityInfo?,
    onDismiss: () -> Unit
) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Audio Quality", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (audioQuality == null) {
                    Text(
                        "No track is currently loaded. Start playback to inspect the active file format.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                    )
                } else {
                    listOf(
                        "Codec" to audioQuality.codec,
                        "Sample rate" to audioQuality.sampleRateLabel,
                        "Bit depth" to audioQuality.bitDepthLabel,
                        "Channels" to audioQuality.channelLabel,
                        "Bitrate" to audioQuality.bitrateLabel,
                        "MIME" to (audioQuality.mimeType ?: "N/A")
                    ).forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary))
                            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PlaybackPreferencesDialog(
    crossfade: Boolean,
    onCrossfadeChange: (Boolean) -> Unit,
    volumeNorm: Boolean,
    onVolumeNormChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Playback Preferences", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Tuning and playback behavior:",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("2-Second Crossfade", fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Blend tracks seamlessly", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                    }
                    Switch(
                        checked = crossfade,
                        onCheckedChange = onCrossfadeChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Volume Normalization", fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Prevent sudden loud tracks", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                    }
                    Switch(
                        checked = volumeNorm,
                        onCheckedChange = onVolumeNormChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        onClearHistory()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Listening History")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun StreamingPreferencesDialog(
    highQuality: Boolean,
    onHighQualityChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Network Playback", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Internet radio streaming options:",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High-Quality Audio", fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("Prefer 192k/320k streams over 64k AAC", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                    }
                    Switch(
                        checked = highQuality,
                        onCheckedChange = onHighQualityChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                    )
                }

                Text(
                    "Streams use active HTTP buffering for uninterrupted listening through weak mobile connections.",
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FormatMatrixDialog(onDismiss: () -> Unit) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Audio Compatibility", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "All formats decode natively through ExoPlayer / Media3 AudioTrack:",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
                val formats = listOf(
                    "FLAC" to "Lossless 24-bit / 96kHz bit-perfect decode",
                    "MP3" to "MPEG-1/2 Audio Layer III up to 320 kbps",
                    "WAV" to "Uncompressed Linear PCM (16/24/32-bit)",
                    "AAC / M4A" to "Advanced Audio Coding in MP4 container",
                    "OGG / Vorbis" to "Ogg Vorbis & Opus container streaming",
                    "Live Streams" to "HTTP, Shoutcast, Icecast ICY metadata streams"
                )
                formats.forEach { (fmt, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fmt,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.width(84.dp)
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Privacy Policy", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Text(
                text = "YouniSpin is engineered with a strict local-first philosophy:\n\n" +
                        "• No account registration or cloud sign-in.\n" +
                        "• Zero telemetry, advertising, or background profiling.\n" +
                        "• Audio files never leave your device storage.\n" +
                        "• Online metadata lookup via MusicBrainz is strictly user-initiated on demand.",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Open Source Licenses", fontWeight = FontWeight.Bold, color = colors.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "YouniSpin is built with open-source software libraries:",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
                val libs = listOf(
                    "AndroidX Media3 / ExoPlayer" to "Apache License 2.0",
                    "Jetpack Compose & Material 3" to "Apache License 2.0",
                    "Coil Image Loading" to "Apache License 2.0",
                    "Kotlin Coroutines & Flow" to "Apache License 2.0",
                    "AndroidX Room Database" to "Apache License 2.0"
                )
                libs.forEach { (name, lic) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontWeight = FontWeight.Medium, color = colors.textPrimary, fontSize = 13.sp)
                        Text(lic, style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AboutDialog(
    onOpenDonate: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YouniTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Official YouniSpin app icon in About dialog
                Image(
                    painter = painterResource(id = R.drawable.younispin_brand),
                    contentDescription = "YouniSpin logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "YouniSpin",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        "Your Music. Your Way. • By Youni.",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.accent)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                )

                Text(
                    text = "YouniSpin is an Android-first music player designed for people who love their music libraries. Enjoy local music, playlists, album artwork, radio streams, and a customizable classic-player experience.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Created & developed by DeveloperCal.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Donation Button inside About
                Button(
                    onClick = {
                        onOpenDonate()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Donate on Liberapay", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
