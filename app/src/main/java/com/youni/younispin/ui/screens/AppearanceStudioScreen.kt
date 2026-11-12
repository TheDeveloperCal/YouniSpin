package com.youni.younispin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youni.younispin.R
import com.youni.younispin.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudioScreen(
    currentSettings: AppearanceSettings,
    onSaveSettings: (AppearanceSettings) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = YouniTheme.colors

    // Local mutable state for real-time live preview before saving/applying
    var draftSettings by remember(currentSettings) { mutableStateOf(currentSettings) }

    // Live preview colors derived from draftSettings
    val isDraftDark = when (draftSettings.themeMode) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val previewColors = remember(draftSettings, isDraftDark) {
        createYouniColors(
            isDark = isDraftDark,
            accent = draftSettings.primaryAccent,
            secondaryAccent = draftSettings.secondaryAccent,
            dialStyle = draftSettings.dialStyle
        )
    }

    // Custom Hex input state
    var hexInput by remember(draftSettings.primaryAccentHex) {
        mutableStateOf(draftSettings.primaryAccentHex.removePrefix("#").uppercase(Locale.ROOT))
    }
    var hexError by remember { mutableStateOf<String?>(null) }

    // HSV sliders state
    val hsv = remember(draftSettings.primaryAccent) {
        val floatArray = FloatArray(3)
        android.graphics.Color.colorToHSV(draftSettings.primaryAccent.toArgb(), floatArray)
        floatArray
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var brightness by remember { mutableFloatStateOf(hsv[2]) }

    fun updateColorFromHsv(h: Float, s: Float, v: Float) {
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val hexStr = String.format("#%06X", 0xFFFFFF and colorInt)
        hexInput = hexStr.removePrefix("#")
        hexError = null
        draftSettings = draftSettings.copy(primaryAccentHex = hexStr)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Appearance Studio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = "Live Hardware Theming & Custom Accents",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val defs = AppearanceSettings()
                            draftSettings = defs
                            hexInput = defs.primaryAccentHex.removePrefix("#")
                            onResetDefaults()
                            Toast.makeText(context, "Appearance reset to factory defaults", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary
                )
            )
        },
        containerColor = colors.background,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = colors.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val defs = AppearanceSettings()
                            draftSettings = defs
                            hexInput = defs.primaryAccentHex.removePrefix("#")
                            onResetDefaults()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                    ) {
                        Text("Reset")
                    }

                    Button(
                        onClick = {
                            onSaveSettings(draftSettings)
                            Toast.makeText(context, "Appearance applied successfully", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = previewColors.accent,
                            contentColor = if (isDraftDark) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Appearance", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ==========================================
            // 1. LIVE HARDWARE DISPLAY PREVIEW
            // ==========================================
            item {
                Text(
                    text = "REAL-TIME HARDWARE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LiveMiniPlayerPreview(
                    previewColors = previewColors,
                    cornerRadius = draftSettings.cornerRadiusStyle.radiusDp,
                    dialStyle = draftSettings.dialStyle,
                    isDark = isDraftDark
                )
            }

            // ==========================================
            // 2. THEME MODE SELECTOR
            // ==========================================
            item {
                Text(
                    text = "THEME MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeOption(
                            title = "System",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = draftSettings.themeMode == ThemeMode.SYSTEM,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { draftSettings = draftSettings.copy(themeMode = ThemeMode.SYSTEM) }
                        )

                        ThemeModeOption(
                            title = "Light",
                            icon = Icons.Default.LightMode,
                            isSelected = draftSettings.themeMode == ThemeMode.LIGHT,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { draftSettings = draftSettings.copy(themeMode = ThemeMode.LIGHT) }
                        )

                        ThemeModeOption(
                            title = "Dark",
                            icon = Icons.Default.DarkMode,
                            isSelected = draftSettings.themeMode == ThemeMode.DARK,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { draftSettings = draftSettings.copy(themeMode = ThemeMode.DARK) }
                        )
                    }
                }
            }

            // ==========================================
            // 3. ACCENT COLOR PALETTES
            // ==========================================
            item {
                Text(
                    text = "PRIMARY ACCENT COLOR PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(BuiltInAccentPresets) { preset ->
                                val isSelected = draftSettings.primaryAccentHex.equals(
                                    String.format("#%06X", 0xFFFFFF and preset.color.toArgb()),
                                    ignoreCase = true
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val hex = String.format("#%06X", 0xFFFFFF and preset.color.toArgb())
                                            draftSettings = draftSettings.copy(primaryAccentHex = hex)
                                            hexInput = hex.removePrefix("#")
                                            hexError = null
                                            // sync HSV
                                            val arr = FloatArray(3)
                                            android.graphics.Color.colorToHSV(preset.color.toArgb(), arr)
                                            hue = arr[0]
                                            saturation = arr[1]
                                            brightness = arr[2]
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(if (isSelected) 8.dp else 2.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(preset.color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.5.dp,
                                                color = if (isSelected) colors.textPrimary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. CUSTOM COLOR PICKER (HEX & HSV)
            // ==========================================
            item {
                Text(
                    text = "CUSTOM COLOR STUDIO (HEX & HSV)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        // Hex Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Color Preview Swatch
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(4.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(draftSettings.primaryAccent)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(10.dp))
                            )

                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isLetterOrDigit() }.take(6).uppercase(Locale.ROOT)
                                    hexInput = clean
                                    if (clean.length == 6) {
                                        try {
                                            val parsed = android.graphics.Color.parseColor("#$clean")
                                            draftSettings = draftSettings.copy(primaryAccentHex = "#$clean")
                                            hexError = null
                                            // sync HSV
                                            val arr = FloatArray(3)
                                            android.graphics.Color.colorToHSV(parsed, arr)
                                            hue = arr[0]
                                            saturation = arr[1]
                                            brightness = arr[2]
                                        } catch (_: Exception) {
                                            hexError = "Invalid color format"
                                        }
                                    } else {
                                        hexError = "Enter 6-digit hex"
                                    }
                                },
                                prefix = { Text("#", fontWeight = FontWeight.Bold, color = previewColors.accent) },
                                label = { Text("HEX Color Code") },
                                isError = hexError != null,
                                supportingText = {
                                    if (hexError != null) Text(hexError ?: "") else Text("e.g. 00D2FF, FF4081")
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = previewColors.accent,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                )
                            )
                        }

                        // HSV Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hue (Tint)", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                                Text("${hue.toInt()}°", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary))
                            }
                            Slider(
                                value = hue,
                                onValueChange = {
                                    hue = it
                                    updateColorFromHsv(hue, saturation, brightness)
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = previewColors.accent,
                                    activeTrackColor = previewColors.accent
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saturation", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                                Text("${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary))
                            }
                            Slider(
                                value = saturation,
                                onValueChange = {
                                    saturation = it
                                    updateColorFromHsv(hue, saturation, brightness)
                                },
                                valueRange = 0.1f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = previewColors.accent,
                                    activeTrackColor = previewColors.accent
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Brightness", style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary))
                                Text("${(brightness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary))
                            }
                            Slider(
                                value = brightness,
                                onValueChange = {
                                    brightness = it
                                    updateColorFromHsv(hue, saturation, brightness)
                                },
                                valueRange = 0.2f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = previewColors.accent,
                                    activeTrackColor = previewColors.accent
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 5. HARDWARE ORBIT DIAL FINISH
            // ==========================================
            item {
                Text(
                    text = "ORBIT DIAL HARDWARE FINISH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DialStyleRow(
                            title = "Automatic (Adaptive)",
                            subtitle = "Matches active theme: Dark Graphite in dark, Ceramic White in light",
                            isSelected = draftSettings.dialStyle == DialStyle.AUTO,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            subtextColor = colors.textSecondary,
                            onClick = { draftSettings = draftSettings.copy(dialStyle = DialStyle.AUTO) }
                        )

                        HorizontalDivider(color = colors.border)

                        DialStyleRow(
                            title = "Dark Graphite Hardware",
                            subtitle = "Authentic matte machined dark disc with tactile contrast",
                            isSelected = draftSettings.dialStyle == DialStyle.DARK_GRAPHITE,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            subtextColor = colors.textSecondary,
                            onClick = { draftSettings = draftSettings.copy(dialStyle = DialStyle.DARK_GRAPHITE) }
                        )

                        HorizontalDivider(color = colors.border)

                        DialStyleRow(
                            title = "Silver Metallic Alloy",
                            subtitle = "Anodized brushed aluminum hardware disc with obsidian labeling",
                            isSelected = draftSettings.dialStyle == DialStyle.SILVER_ALLOY,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            subtextColor = colors.textSecondary,
                            onClick = { draftSettings = draftSettings.copy(dialStyle = DialStyle.SILVER_ALLOY) }
                        )

                        HorizontalDivider(color = colors.border)

                        DialStyleRow(
                            title = "Ceramic White",
                            subtitle = "Minimalist pristine white finish with slate labeling",
                            isSelected = draftSettings.dialStyle == DialStyle.CERAMIC_WHITE,
                            accentColor = previewColors.accent,
                            textColor = colors.textPrimary,
                            subtextColor = colors.textSecondary,
                            onClick = { draftSettings = draftSettings.copy(dialStyle = DialStyle.CERAMIC_WHITE) }
                        )
                    }
                }
            }

            // ==========================================
            // 6. ANIMATIONS & FLUIDITY
            // ==========================================
            item {
                Text(
                    text = "MOTION & FLUIDITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fluid Hardware Animations",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = "Smooth screen transitions, spring press physics, and real-time visualizers",
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                            )
                        }
                        Switch(
                            checked = draftSettings.animationsEnabled,
                            onCheckedChange = { draftSettings = draftSettings.copy(animationsEnabled = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = previewColors.accent,
                                checkedTrackColor = previewColors.accent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, interactive live mini replica of the YouniSpin screen
 */
@Composable
private fun LiveMiniPlayerPreview(
    previewColors: YouniColors,
    cornerRadius: androidx.compose.ui.unit.Dp,
    dialStyle: DialStyle,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = previewColors.background),
        border = androidx.compose.foundation.BorderStroke(2.dp, previewColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Screen Glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewColors.displayGlass)
                    .border(1.5.dp, previewColors.displayBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(previewColors.displayHeader)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "YOUNI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = previewColors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "SPIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = previewColors.accent
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(previewColors.accent, CircleShape)
                            )
                            Text(
                                text = "▶",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = previewColors.accent
                                )
                            )
                        }
                    }

                    // Split preview
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Menu
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(previewColors.menuBackground)
                                .padding(vertical = 3.dp)
                        ) {
                            // Selected Item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(previewColors.selectedRowGradient)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Songs",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = previewColors.selectedRowText
                                    )
                                )
                                Text(
                                    text = "03:45",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = previewColors.selectedRowSubtext
                                    )
                                )
                            }

                            // Unselected item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = previewColors.textPrimary
                                    )
                                )
                            }
                        }

                        // Right Preview
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(previewColors.previewBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(previewColors.accent.copy(alpha = 0.3f), previewColors.previewBackground)
                                        )
                                    )
                            )

                            // Pill badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(previewColors.accent, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "● PLAYING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.Black else Color.White
                                    )
                                )
                            }

                            // Track title overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "Acoustic Breeze",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = previewColors.textPrimary
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Luna Bay [FLAC]",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.sp,
                                        color = previewColors.accent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mini Orbit Dial representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(previewColors.dialDisc)
                        .border(1.dp, previewColors.dialDiscBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(previewColors.dialHub)
                            .border(1.dp, previewColors.dialHubBorder, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else textColor
                )
            )
        }
    }
}

@Composable
private fun DialStyleRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    subtextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else textColor
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = subtextColor)
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
        )
    }
}
