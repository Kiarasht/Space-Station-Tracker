package com.restart.spacestationtracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SpaceAccentYellow,
    secondary = SpaceLightBlue,
    background = SpaceDarkBlue,
    surface = SpaceLightBlue,
    onPrimary = SpaceDarkBlue,
    onSecondary = SpaceTextWhite,
    onBackground = SpaceTextWhite,
    onSurface = SpaceTextWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = SpacePrimaryBlue,
    secondary = SpaceLightBlue,
    background = SpaceBackgroundLight,
    surface = SpaceSurfaceLight,
    onPrimary = SpaceTextWhite,
    onSecondary = SpaceTextWhite,
    onBackground = SpaceTextDark,
    onSurface = SpaceTextDark,
)

@Composable
fun SpaceStationTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpaceTypography,
        content = content
    )
}
