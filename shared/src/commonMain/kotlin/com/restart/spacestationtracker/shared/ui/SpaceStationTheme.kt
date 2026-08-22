package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.restart.spacestationtracker.shared.resources.Res
import com.restart.spacestationtracker.shared.resources.exo_italic_variable
import com.restart.spacestationtracker.shared.resources.exo_variable
import com.restart.spacestationtracker.shared.resources.orbitron_variable
import org.jetbrains.compose.resources.Font

val SpaceDarkBlue = Color(0xFF000020)
val SpaceLightBlue = Color(0xFF0C1244)
val SpaceAccentYellow = Color(0xFFFFEB3B)
val SpaceTextWhite = Color(0xFFFFFFFF)
val SpaceBackgroundLight = Color(0xFFF0F2F5)
val SpaceSurfaceLight = Color(0xFFFFFFFF)
val SpaceTextDark = Color(0xFF1C1C1E)
val SpacePrimaryBlue = Color(0xFF005792)
val SpaceLiveStreamRed = Color(0xFF8C1D18)
val SpaceOnLiveStreamRed = Color(0xFFF9DEDC)

val SpaceDarkColorScheme = darkColorScheme(
    primary = SpaceAccentYellow,
    secondary = SpaceLightBlue,
    background = SpaceDarkBlue,
    surface = SpaceLightBlue,
    onPrimary = SpaceDarkBlue,
    onSecondary = SpaceTextWhite,
    onBackground = SpaceTextWhite,
    onSurface = SpaceTextWhite
)

val SpaceLightColorScheme = lightColorScheme(
    primary = SpacePrimaryBlue,
    secondary = SpaceLightBlue,
    background = SpaceBackgroundLight,
    surface = SpaceSurfaceLight,
    onPrimary = SpaceTextWhite,
    onSecondary = SpaceTextWhite,
    onBackground = SpaceTextDark,
    onSurface = SpaceTextDark
)

data class SpaceFontFamilies(
    val display: FontFamily,
    val content: FontFamily
)

@Composable
fun rememberSpaceFontFamilies(): SpaceFontFamilies {
    val orbitron = FontFamily(
        Font(Res.font.orbitron_variable, FontWeight.Normal),
        Font(Res.font.orbitron_variable, FontWeight.Medium),
        Font(Res.font.orbitron_variable, FontWeight.Bold)
    )
    val exo = FontFamily(
        Font(Res.font.exo_variable, FontWeight.Normal),
        Font(Res.font.exo_variable, FontWeight.Medium),
        Font(Res.font.exo_variable, FontWeight.Bold),
        Font(Res.font.exo_italic_variable, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.exo_italic_variable, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.exo_italic_variable, FontWeight.Bold, FontStyle.Italic)
    )
    return SpaceFontFamilies(display = orbitron, content = exo)
}

@Composable
fun rememberSpaceTypography(
    fontFamilies: SpaceFontFamilies = rememberSpaceFontFamilies()
): Typography {
    val orbitron = fontFamilies.display
    val exo = fontFamilies.content
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = orbitron),
        displayMedium = defaults.displayMedium.copy(fontFamily = orbitron),
        displaySmall = defaults.displaySmall.copy(fontFamily = orbitron),
        headlineLarge = defaults.headlineLarge.copy(
            fontFamily = orbitron,
            fontWeight = FontWeight.Bold
        ),
        headlineMedium = defaults.headlineMedium.copy(
            fontFamily = orbitron,
            fontWeight = FontWeight.Bold
        ),
        headlineSmall = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        ),
        titleMedium = defaults.titleMedium.copy(
            fontFamily = exo,
            fontWeight = FontWeight.Medium
        ),
        titleSmall = defaults.titleSmall.copy(
            fontFamily = exo,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = TextStyle(
            fontFamily = exo,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = exo,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = defaults.bodySmall.copy(fontFamily = exo),
        labelLarge = defaults.labelLarge.copy(
            fontFamily = exo,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = TextStyle(
            fontFamily = exo,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = defaults.labelSmall.copy(
            fontFamily = exo,
            fontWeight = FontWeight.Medium
        )
    )
}

@Composable
fun SpaceStationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamilies: SpaceFontFamilies = rememberSpaceFontFamilies(),
    content: @Composable () -> Unit
) {
    val typography = rememberSpaceTypography(fontFamilies)
    MaterialTheme(
        colorScheme = if (darkTheme) SpaceDarkColorScheme else SpaceLightColorScheme,
        typography = typography
    ) {
        // Plain Text composables do not automatically inherit MaterialTheme.typography.
        // Supplying Exo 2 here prevents platform defaults (Roboto/San Francisco) from
        // leaking into shared screens while Material components can still provide
        // their own semantic text styles.
        ProvideTextStyle(
            value = typography.bodyMedium,
            content = content
        )
    }
}
