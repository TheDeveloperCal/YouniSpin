package com.youni.younispin.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DialStyle {
    AUTO,           // Matches theme: dark in dark mode, ceramic white in light mode
    DARK_GRAPHITE,  // Classic dark machined audio hardware
    SILVER_ALLOY,   // Anodized brushed metallic silver
    CERAMIC_WHITE   // Pristine minimalist white
}

enum class CornerRadiusStyle(val radiusDp: Dp, val label: String) {
    COMPACT_STUDIO(10.dp, "Compact Studio"),
    CLASSIC_CURVED(18.dp, "Classic Hardware"),
    MODERN_ROUNDED(24.dp, "Modern Rounded")
}

data class AccentPreset(
    val id: String,
    val name: String,
    val color: Color,
    val variantColor: Color
)

val BuiltInAccentPresets = listOf(
    AccentPreset("cyan", "Youni Cyan", Color(0xFF00D2FF), Color(0xFF0099CC)),
    AccentPreset("ocean_blue", "Ocean Blue", Color(0xFF1976D2), Color(0xFF115293)),
    AccentPreset("electric_blue", "Electric Blue", Color(0xFF2979FF), Color(0xFF1C54B2)),
    AccentPreset("emerald_green", "Emerald Green", Color(0xFF00C853), Color(0xFF008C3A)),
    AccentPreset("aurora_teal", "Aurora Teal", Color(0xFF00BFA5), Color(0xFF008573)),
    AccentPreset("royal_purple", "Royal Purple", Color(0xFF7C4DFF), Color(0xFF5635B2)),
    AccentPreset("sunset_orange", "Sunset Orange", Color(0xFFFF6D00), Color(0xFFB24C00)),
    AccentPreset("ruby_red", "Ruby Red", Color(0xFFD50000), Color(0xFF950000)),
    AccentPreset("rose_pink", "Rose Pink", Color(0xFFFF4081), Color(0xFFB22C5A)),
    AccentPreset("golden_amber", "Golden Amber", Color(0xFFFFAB00), Color(0xFFB27700)),
    AccentPreset("monochrome", "Monochrome", Color(0xFF78909C), Color(0xFF455A64))
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val primaryAccentHex: String = "#00D2FF",
    val secondaryAccentHex: String = "#FFAB00",
    val dialStyle: DialStyle = DialStyle.AUTO,
    val cornerRadiusStyle: CornerRadiusStyle = CornerRadiusStyle.CLASSIC_CURVED,
    val animationsEnabled: Boolean = true,
    val highContrastText: Boolean = false
) {
    val primaryAccent: Color
        get() = try {
            Color(android.graphics.Color.parseColor(primaryAccentHex))
        } catch (_: Exception) {
            Color(0xFF00D2FF)
        }

    val secondaryAccent: Color
        get() = try {
            Color(android.graphics.Color.parseColor(secondaryAccentHex))
        } catch (_: Exception) {
            Color(0xFFFFAB00)
        }
}

/**
 * Semantic color system supporting both rich dark hardware and pristine light mode
 */
data class YouniColors(
    val isDark: Boolean,
    val accent: Color,
    val accentVariant: Color,
    val secondaryAccent: Color,
    val background: Color,            // Main hardware chassis background
    val surface: Color,               // Cards, top bars, dialogs
    val surfaceElevated: Color,       // Elevated cards, text fields
    val border: Color,                // Card borders, subtle dividers
    val textPrimary: Color,           // Primary text
    val textSecondary: Color,         // Secondary text
    val textMuted: Color,             // Muted timestamps / labels

    // Classic Screen Glass Palette:
    val displayGlass: Color,          // Screen recessed glass
    val displayBorder: Color,         // Screen glass bezel
    val displayHeader: Color,         // Status bar header inside screen glass
    val menuBackground: Color,        // Left column menu background
    val menuDivider: Color,           // Row divider
    val selectedRowGradient: Brush,   // Highlight brush for selected row
    val selectedRowText: Color,       // Selected row text color
    val selectedRowSubtext: Color,    // Selected row subtext color
    val previewBackground: Color,     // Right column preview background

    // Orbit Dial Palette:
    val dialChassis: Color,           // Surrounding dial chassis
    val dialDisc: Brush,              // Outer rotary disc brush
    val dialDiscBorder: Color,        // Outer disc border
    val dialRingBorder: Color,        // Concentric inner ring
    val dialHub: Brush,               // Center action button brush
    val dialHubBorder: Color,         // Center button border
    val dialLabel: Color,             // "MENU", "▶❚❚", etc. text
    val dialLabelActive: Color        // Pressed label color
)

fun createYouniColors(
    isDark: Boolean,
    accent: Color,
    secondaryAccent: Color,
    dialStyle: DialStyle
): YouniColors {
    val accentDarker = Color(
        red = (accent.red * 0.75f).coerceIn(0f, 1f),
        green = (accent.green * 0.75f).coerceIn(0f, 1f),
        blue = (accent.blue * 0.75f).coerceIn(0f, 1f),
        alpha = 1f
    )

    // Determine dial colors based on dialStyle and theme
    val effectiveDialStyle = when (dialStyle) {
        DialStyle.AUTO -> if (isDark) DialStyle.DARK_GRAPHITE else DialStyle.CERAMIC_WHITE
        else -> dialStyle
    }

    val (dialDisc, dialBorder, dialRing, dialHub, dialHubBorder, dialLabel) = when (effectiveDialStyle) {
        DialStyle.DARK_GRAPHITE -> {
            val disc = Brush.radialGradient(
                colors = listOf(Color(0xFF22242B), Color(0xFF111215)),
                radius = 500f
            )
            val hub = Brush.verticalGradient(
                colors = listOf(Color(0xFF292C36), Color(0xFF17181D))
            )
            Tuple6(disc, Color(0xFF323542), Color(0x22FFFFFF), hub, Color(0xFF3E4254), Color.White)
        }
        DialStyle.SILVER_ALLOY -> {
            val disc = Brush.radialGradient(
                colors = listOf(Color(0xFFE4E7ED), Color(0xFFBCC2CE)),
                radius = 500f
            )
            val hub = Brush.verticalGradient(
                colors = listOf(Color(0xFFF2F4F7), Color(0xFFC8CED9))
            )
            Tuple6(disc, Color(0xFFA6ADB9), Color(0x33000000), hub, Color(0xFF9EA6B5), Color(0xFF1E222B))
        }
        DialStyle.CERAMIC_WHITE -> {
            val disc = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE9EDF4)),
                radius = 500f
            )
            val hub = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E7F0))
            )
            Tuple6(disc, Color(0xFFD3D8E2), Color(0x18000000), hub, Color(0xFFC7CDD8), Color(0xFF1E222B))
        }
        DialStyle.AUTO -> Tuple6(
            Brush.radialGradient(listOf(Color(0xFF22242B), Color(0xFF111215))),
            Color(0xFF323542), Color(0x22FFFFFF),
            Brush.verticalGradient(listOf(Color(0xFF292C36), Color(0xFF17181D))),
            Color(0xFF3E4254), Color.White
        )
    }

    return if (isDark) {
        // DARK MODE (Graphite Hardware)
        YouniColors(
            isDark = true,
            accent = accent,
            accentVariant = accentDarker,
            secondaryAccent = secondaryAccent,
            background = Color(0xFF101216),
            surface = Color(0xFF191D24),
            surfaceElevated = Color(0xFF232933),
            border = Color(0xFF2E323D),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFFADB5C2),
            textMuted = Color(0xFF6B7280),

            displayGlass = Color(0xFF0D0E11),
            displayBorder = Color(0xFF282A33),
            displayHeader = Color(0xFF191D24),
            menuBackground = Color(0xFF131418),
            menuDivider = Color(0xFF1E2026),
            selectedRowGradient = Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.85f), accentDarker.copy(alpha = 0.95f))
            ),
            selectedRowText = Color.White,
            selectedRowSubtext = Color(0xFFE1E8F5),
            previewBackground = Color(0xFF1A1C22),

            dialChassis = Color(0xFF101216),
            dialDisc = dialDisc,
            dialDiscBorder = dialBorder,
            dialRingBorder = dialRing,
            dialHub = dialHub,
            dialHubBorder = dialHubBorder,
            dialLabel = dialLabel,
            dialLabelActive = accent
        )
    } else {
        // LIGHT MODE (Pristine Clean Audio Aesthetic)
        YouniColors(
            isDark = false,
            accent = accent,
            accentVariant = accentDarker,
            secondaryAccent = secondaryAccent,
            background = Color(0xFFF4F6F9),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFEEF1F5),
            border = Color(0xFFDDE2E9),
            textPrimary = Color(0xFF171B24),
            textSecondary = Color(0xFF596273),
            textMuted = Color(0xFF8C96A8),

            displayGlass = Color(0xFFFFFFFF),
            displayBorder = Color(0xFFDDE2E9),
            displayHeader = Color(0xFFEEF1F5),
            menuBackground = Color(0xFFFFFFFF),
            menuDivider = Color(0xFFF0F3F7),
            selectedRowGradient = Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.08f))
            ),
            selectedRowText = Color(0xFF12151C),
            selectedRowSubtext = Color(0xFF424A58),
            previewBackground = Color(0xFFF0F3F7),

            dialChassis = Color(0xFFF4F6F9),
            dialDisc = dialDisc,
            dialDiscBorder = dialBorder,
            dialRingBorder = dialRing,
            dialHub = dialHub,
            dialHubBorder = dialHubBorder,
            dialLabel = dialLabel,
            dialLabelActive = accent
        )
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)

val LocalYouniColors = staticCompositionLocalOf {
    createYouniColors(
        isDark = true,
        accent = Color(0xFF00D2FF),
        secondaryAccent = Color(0xFFFFAB00),
        dialStyle = DialStyle.AUTO
    )
}

val LocalAppearanceSettings = compositionLocalOf {
    AppearanceSettings()
}

object YouniTheme {
    val colors: YouniColors
        @Composable
        get() = LocalYouniColors.current

    val settings: AppearanceSettings
        @Composable
        get() = LocalAppearanceSettings.current
}

/**
 * SharedPreferences storage manager for Appearance
 */
class AppearanceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("younispin_appearance", Context.MODE_PRIVATE)
    private val legacyPrefs: SharedPreferences = context.getSharedPreferences("younispin_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppearanceSettings {
        // Safe migration from legacy dark_theme preference if needed
        val legacyDark = legacyPrefs.getBoolean("dark_theme", true)
        val defaultMode = if (legacyDark) ThemeMode.DARK.name else ThemeMode.LIGHT.name

        val themeModeStr = prefs.getString("theme_mode", defaultMode) ?: defaultMode
        val themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (_: Exception) { ThemeMode.DARK }

        val primaryAccent = prefs.getString("primary_accent_hex", "#00D2FF") ?: "#00D2FF"
        val secondaryAccent = prefs.getString("secondary_accent_hex", "#FFAB00") ?: "#FFAB00"

        val dialStyleStr = prefs.getString("dial_style", DialStyle.AUTO.name) ?: DialStyle.AUTO.name
        val dialStyle = try { DialStyle.valueOf(dialStyleStr) } catch (_: Exception) { DialStyle.AUTO }

        val radiusStyleStr = prefs.getString("corner_radius_style", CornerRadiusStyle.CLASSIC_CURVED.name) ?: CornerRadiusStyle.CLASSIC_CURVED.name
        val radiusStyle = try { CornerRadiusStyle.valueOf(radiusStyleStr) } catch (_: Exception) { CornerRadiusStyle.CLASSIC_CURVED }

        val animEnabled = prefs.getBoolean("animations_enabled", true)
        val highContrast = prefs.getBoolean("high_contrast_text", false)

        return AppearanceSettings(
            themeMode = themeMode,
            primaryAccentHex = primaryAccent,
            secondaryAccentHex = secondaryAccent,
            dialStyle = dialStyle,
            cornerRadiusStyle = radiusStyle,
            animationsEnabled = animEnabled,
            highContrastText = highContrast
        )
    }

    fun saveSettings(settings: AppearanceSettings) {
        prefs.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("primary_accent_hex", settings.primaryAccentHex)
            .putString("secondary_accent_hex", settings.secondaryAccentHex)
            .putString("dial_style", settings.dialStyle.name)
            .putString("corner_radius_style", settings.cornerRadiusStyle.name)
            .putBoolean("animations_enabled", settings.animationsEnabled)
            .putBoolean("high_contrast_text", settings.highContrastText)
            .apply()

        // Also sync legacy boolean for backward compatibility
        legacyPrefs.edit()
            .putBoolean("dark_theme", settings.themeMode != ThemeMode.LIGHT)
            .apply()
    }

    fun resetToDefaults(): AppearanceSettings {
        val defaults = AppearanceSettings()
        saveSettings(defaults)
        return defaults
    }
}
