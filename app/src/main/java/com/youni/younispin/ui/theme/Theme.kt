package com.youni.younispin.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun YouniSpinTheme(
    appearanceSettings: AppearanceSettings = AppearanceSettings(),
    fullscreen: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appearanceSettings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val youniColors = createYouniColors(
        isDark = isDark,
        accent = appearanceSettings.primaryAccent,
        secondaryAccent = appearanceSettings.secondaryAccent,
        dialStyle = appearanceSettings.dialStyle
    )

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = youniColors.accent,
            onPrimary = youniColors.background,
            secondary = youniColors.secondaryAccent,
            onSecondary = youniColors.background,
            background = youniColors.background,
            surface = youniColors.surface,
            onBackground = youniColors.textPrimary,
            onSurface = youniColors.textPrimary,
            surfaceVariant = youniColors.surfaceElevated,
            onSurfaceVariant = youniColors.textSecondary,
            outline = youniColors.border
        )
    } else {
        lightColorScheme(
            primary = youniColors.accent,
            onPrimary = youniColors.surface,
            secondary = youniColors.secondaryAccent,
            onSecondary = youniColors.surface,
            background = youniColors.background,
            surface = youniColors.surface,
            onBackground = youniColors.textPrimary,
            onSurface = youniColors.textPrimary,
            surfaceVariant = youniColors.surfaceElevated,
            onSurfaceVariant = youniColors.textSecondary,
            outline = youniColors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = youniColors.background.toArgb()
                window.navigationBarColor = youniColors.background.toArgb()

                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark

                if (fullscreen) {
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalYouniColors provides youniColors,
        LocalAppearanceSettings provides appearanceSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
