package com.youni.younispin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youni.younispin.playback.EqualizerManager
import com.youni.younispin.ui.theme.*
import com.youni.younispin.ui.theme.YouniTheme

@Composable
fun EqualizerScreen(
    equalizerManager: EqualizerManager,
    onBack: () -> Unit,
    embedded: Boolean = false
) {
    val colors = YouniTheme.colors

    var isEnabled by remember { mutableStateOf(equalizerManager.isEnabled) }
    var currentPreset by remember { mutableStateOf(equalizerManager.currentPresetIndex) }
    var bassStrength by remember { mutableStateOf(equalizerManager.bassBoostStrength.toFloat()) }
    var virtStrength by remember { mutableStateOf(equalizerManager.virtualizerStrength.toFloat()) }
    val bands = remember(isEnabled, currentPreset) { equalizerManager.getBands() }
    val presets = remember { equalizerManager.getPresetNames() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .then(if (embedded) Modifier else Modifier.statusBarsPadding())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                }
                Text(
                    text = "Equalizer & Audio DSP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        equalizerManager.isEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.accent.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Presets row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PRESET SELECTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.take(4).forEachIndexed { idx, name ->
                                val isSelected = (currentPreset.toInt() == idx)
                                OutlinedButton(
                                    onClick = {
                                        equalizerManager.currentPresetIndex = idx.toShort()
                                        currentPreset = idx.toShort()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) colors.accent else colors.surfaceElevated
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        color = if (isSelected) colors.background else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Bands Sliders
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FREQUENCY BANDS (5-BAND EQ)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (bands.isEmpty()) {
                            Text(
                                text = "Start playback to activate hardware audio effects session.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = colors.secondaryAccent)
                            )
                        } else {
                            bands.forEach { band ->
                                var level by remember(band) { mutableFloatStateOf(band.currentLevelMilliB.toFloat()) }
                                val freqText = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}kHz" else "${band.centerFreqHz}Hz"
                                val dbText = "%+.1f dB".format(level / 100f)

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = freqText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text(text = dbText, style = MaterialTheme.typography.labelSmall.copy(color = colors.accent))
                                    }
                                    Slider(
                                        value = level,
                                        onValueChange = {
                                            level = it
                                            equalizerManager.setBandLevel(band.index, it.toInt().toShort())
                                        },
                                        valueRange = band.minLevelMilliB.toFloat()..band.maxLevelMilliB.toFloat(),
                                        enabled = isEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = colors.accent,
                                            activeTrackColor = colors.accent,
                                            inactiveTrackColor = colors.border
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bass Boost & Virtualizer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DYNAMIC ENHANCEMENTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Bass Boost
                        Text(text = "Bass Boost (${(bassStrength / 10).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = bassStrength,
                            onValueChange = {
                                bassStrength = it
                                equalizerManager.bassBoostStrength = it.toInt().toShort()
                            },
                            valueRange = 0f..1000f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(thumbColor = colors.secondaryAccent, activeTrackColor = colors.secondaryAccent)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Virtualizer (3D Audio)
                        Text(text = "3D Virtualizer (${(virtStrength / 10).toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = virtStrength,
                            onValueChange = {
                                virtStrength = it
                                equalizerManager.virtualizerStrength = it.toInt().toShort()
                            },
                            valueRange = 0f..1000f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
                        )
                    }
                }
            }
        }
    }
}
