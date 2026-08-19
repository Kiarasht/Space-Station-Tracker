package com.restart.spacestationtracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.shared.ui.SpaceDarkColorScheme
import com.restart.spacestationtracker.shared.ui.SpaceFontFamilies
import com.restart.spacestationtracker.shared.ui.SpaceLightColorScheme
import com.restart.spacestationtracker.shared.ui.SpaceStationTheme

private val AndroidSpaceFontFamilies = SpaceFontFamilies(
    display = FontFamily(
        Font(R.font.orbitron_variable, FontWeight.Normal),
        Font(R.font.orbitron_variable, FontWeight.Medium),
        Font(R.font.orbitron_variable, FontWeight.Bold)
    ),
    content = FontFamily(
        Font(R.font.exo_variable, FontWeight.Normal),
        Font(R.font.exo_variable, FontWeight.Medium),
        Font(R.font.exo_variable, FontWeight.Bold),
        Font(R.font.exo_italic_variable, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.exo_italic_variable, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.exo_italic_variable, FontWeight.Bold, FontStyle.Italic)
    )
)

@Composable
fun SpaceStationTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpaceDarkColorScheme else SpaceLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    SpaceStationTheme(
        darkTheme = darkTheme,
        fontFamilies = AndroidSpaceFontFamilies,
        content = content
    )
}
