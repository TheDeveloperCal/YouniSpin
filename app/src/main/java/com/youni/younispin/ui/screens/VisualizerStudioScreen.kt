package com.youni.younispin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youni.younispin.playback.PlaybackState
import com.youni.younispin.ui.components.VisualizerStage
import com.youni.younispin.ui.theme.YouniTheme
import com.youni.younispin.visualizer.GifBackgroundStore
import com.youni.younispin.visualizer.GifFit
import com.youni.younispin.visualizer.VisualizerSettings
import com.youni.younispin.visualizer.VisualizerStyle
import kotlinx.coroutines.launch

private enum class StudioTab { VISUALIZERS, GIFS, CUSTOMIZE }

@Composable
fun VisualizerStudioScreen(
    playbackState: PlaybackState,
    settings: VisualizerSettings,
    onSettingsChange: (VisualizerSettings) -> Unit,
    gifStore: GifBackgroundStore,
    waveform: FloatArray,
    leftWaveform: FloatArray,
    rightWaveform: FloatArray,
    spectrum: FloatArray,
    captureAvailable: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onOpenLiveView: () -> Unit
) {
    val colors = YouniTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gifs by gifStore.items.collectAsState()
    var tab by remember { mutableStateOf(StudioTab.VISUALIZERS) }
    val selectedGif = gifStore.find(settings.selectedGifId)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = gifStore.importFromUri(uri)
            result.onSuccess { gif ->
                onSettingsChange(settings.copy(selectedGifId = gif.id, gifEnabled = true))
                gif.warning?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Could not import GIF", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(colors.displayGlass)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.displayHeader)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.accent, modifier = Modifier.size(16.dp))
            }
            Text(
                "Visualizer Studio",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                "LIVE",
                color = colors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenLiveView)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .semantics { contentDescription = "Open live visualizer" }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clipToBounds()
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
        ) {
            VisualizerStage(
                playbackState = playbackState,
                settings = settings,
                gif = selectedGif,
                waveform = waveform,
                leftWaveform = leftWaveform,
                rightWaveform = rightWaveform,
                spectrum = spectrum,
                captureAvailable = captureAvailable,
                statusMessage = statusMessage,
                previewWithoutTrack = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StudioTab.entries.forEach { item ->
                val selected = tab == item
                Text(
                    text = when (item) {
                        StudioTab.VISUALIZERS -> "Visualizers"
                        StudioTab.GIFS -> "GIF Backgrounds"
                        StudioTab.CUSTOMIZE -> "Customize"
                    },
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) colors.selectedRowText else colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) colors.accent else colors.surfaceElevated)
                        .clickable { tab = item }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        when (tab) {
            StudioTab.VISUALIZERS -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VisualizerStyle.entries.forEach { style ->
                        val selected = settings.style == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) colors.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSettingsChange(settings.copy(style = style)) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.title, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(style.subtitle, color = colors.textMuted, fontSize = 9.sp)
                            }
                            if (selected) Text("ON", color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            StudioTab.GIFS -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent)
                            .clickable { picker.launch(arrayOf("image/gif")) }
                            .padding(10.dp)
                            .semantics {
                                contentDescription = if (selectedGif == null) {
                                    "Add GIF Background"
                                } else {
                                    "Replace GIF Background"
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selectedGif == null) "+ Add GIF Background" else "Replace GIF Background",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        "GIFs play only inside the music display. Imports are copied into private app storage.",
                        color = colors.textMuted,
                        fontSize = 9.sp
                    )
                    Text("GIF frame-rate target", color = colors.textSecondary, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 24, 30).forEach { fps ->
                            val selected = settings.gifFps == fps
                            Text(
                                "$fps FPS",
                                color = if (selected) Color.Black else colors.textPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) colors.accent else colors.surfaceElevated)
                                    .clickable {
                                        onSettingsChange(settings.copy(gifFps = fps, captureRateHz = fps))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .semantics { contentDescription = "$fps frames per second" }
                            )
                        }
                    }

                    if (gifs.isEmpty()) {
                        Text("No imported backgrounds yet.", color = colors.textSecondary, fontSize = 11.sp)
                    }

                    gifs.forEach { gif ->
                        val selected = gif.id == settings.selectedGifId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) colors.accent.copy(alpha = 0.18f) else colors.surfaceElevated)
                                .clickable { onSettingsChange(settings.copy(selectedGifId = gif.id, gifEnabled = true)) }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = gif.file(context),
                                contentDescription = gif.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gif.displayName, color = colors.textPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${gif.width}×${gif.height} • ${gif.byteSize / 1024} KB",
                                    color = colors.textMuted,
                                    fontSize = 9.sp
                                )
                                if (gif.warning != null) {
                                    Text(gif.warning, color = colors.secondaryAccent, fontSize = 8.sp, maxLines = 2)
                                }
                            }
                            IconButton(onClick = { gifStore.toggleFavorite(gif.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    if (gif.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite GIF",
                                    tint = if (gif.favorite) Color(0xFFFF4D4F) else colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(onClick = {
                                gifStore.remove(gif.id)
                                if (settings.selectedGifId == gif.id) {
                                    onSettingsChange(settings.copy(selectedGifId = null))
                                }
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, "Remove GIF", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Text(
                        "Restore default background",
                        color = colors.accent,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onSettingsChange(settings.copy(selectedGifId = null, gifEnabled = true)) }
                            .padding(4.dp)
                    )
                }
            }

            StudioTab.CUSTOMIZE -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CompactSwitch("Animated background", settings.gifEnabled) {
                        onSettingsChange(settings.copy(gifEnabled = it))
                    }
                    CompactSwitch("Waveform / visualizer", settings.waveformEnabled) {
                        onSettingsChange(settings.copy(waveformEnabled = it))
                    }
                    CompactSwitch("Spectrum", settings.spectrumEnabled) {
                        onSettingsChange(settings.copy(spectrumEnabled = it))
                    }
                    CompactSwitch("Track information", settings.trackInfoEnabled) {
                        onSettingsChange(settings.copy(trackInfoEnabled = it))
                    }
                    CompactSwitch("Album artwork overlay", settings.artworkOverlayEnabled) {
                        onSettingsChange(settings.copy(artworkOverlayEnabled = it))
                    }
                    CompactSwitch("Stereo presentation", settings.stereo) {
                        onSettingsChange(settings.copy(stereo = it))
                    }
                    CompactSwitch("Animate only during playback", settings.animateOnlyDuringPlayback) {
                        onSettingsChange(settings.copy(animateOnlyDuringPlayback = it))
                    }
                    CompactSwitch("Pause GIF when music pauses", settings.pauseGifWhenPaused) {
                        onSettingsChange(settings.copy(pauseGifWhenPaused = it))
                    }
                    CompactSwitch("Reduced motion", settings.reducedMotion) {
                        onSettingsChange(settings.copy(reducedMotion = it))
                    }

                    MiniSlider("GIF opacity", settings.gifOpacity, 0.1f, 1f) {
                        onSettingsChange(settings.copy(gifOpacity = it))
                    }
                    MiniSlider("Dark overlay", settings.gifDarkOverlay, 0f, 0.8f) {
                        onSettingsChange(settings.copy(gifDarkOverlay = it))
                    }
                    MiniSlider("Brightness", settings.gifBrightness, -0.4f, 0.4f) {
                        onSettingsChange(settings.copy(gifBrightness = it))
                    }
                    MiniSlider("Saturation", settings.gifSaturation, 0f, 2f) {
                        onSettingsChange(settings.copy(gifSaturation = it))
                    }
                    MiniSlider("Blur", settings.gifBlur, 0f, 1f) {
                        onSettingsChange(settings.copy(gifBlur = it))
                    }
                    MiniSlider("Wave amplitude", settings.amplitude, 0.3f, 2f) {
                        onSettingsChange(settings.copy(amplitude = it))
                    }
                    MiniSlider("Line thickness", settings.lineThickness, 1f, 6f) {
                        onSettingsChange(settings.copy(lineThickness = it))
                    }
                    MiniSlider("Glow", settings.glowIntensity, 0f, 1f) {
                        onSettingsChange(settings.copy(glowIntensity = it))
                    }
                    MiniSlider("Sensitivity", settings.sensitivity, 0.4f, 2.2f) {
                        onSettingsChange(settings.copy(sensitivity = it))
                    }
                    MiniSlider("Smoothing", settings.smoothing, 0.1f, 0.9f) {
                        onSettingsChange(settings.copy(smoothing = it))
                    }
                    MiniSlider("Bars", settings.barCount.toFloat(), 8f, 48f) {
                        onSettingsChange(settings.copy(barCount = it.toInt()))
                    }

                    Text("Background fit", color = colors.textSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GifFit.entries.forEach { fit ->
                            val on = settings.gifFit == fit
                            Text(
                                fit.title,
                                fontSize = 10.sp,
                                color = if (on) Color.Black else colors.textPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (on) colors.accent else colors.surfaceElevated)
                                    .clickable { onSettingsChange(settings.copy(gifFit = fit)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("GIF frame-rate target", color = colors.textSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 24, 30).forEach { fps ->
                            val on = settings.gifFps == fps
                            Text(
                                "$fps FPS",
                                fontSize = 10.sp,
                                color = if (on) Color.Black else colors.textPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (on) colors.accent else colors.surfaceElevated)
                                    .clickable {
                                        onSettingsChange(settings.copy(gifFps = fps, captureRateHz = fps))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable { onSettingsChange(VisualizerSettings()) }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset settings", tint = colors.accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset visualizer settings", color = colors.accent, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = YouniTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
private fun MiniSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    val colors = YouniTheme.colors
    Text(label, color = colors.textSecondary, fontSize = 10.sp)
    Slider(
        value = value.coerceIn(min, max),
        onValueChange = onChange,
        valueRange = min..max,
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
        modifier = Modifier
            .height(22.dp)
            .semantics { contentDescription = label }
    )
}
