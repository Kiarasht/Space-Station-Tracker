package com.restart.spacestationtracker.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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

// Brand constants kept for existing callers.
val SpaceDarkBlue = Color(0xFF000020)
val SpaceLightBlue = Color(0xFF0C1244)
val SpaceAccentYellow = Color(0xFFFFD84D)
val SpaceTextWhite = Color(0xFFFFFFFF)
val SpaceBackgroundLight = Color(0xFFF3F6FC)
val SpaceSurfaceLight = Color(0xFFFFFFFF)
val SpaceTextDark = Color(0xFF121633)
val SpacePrimaryBlue = Color(0xFF2457E6)
val SpaceLiveStreamRed = Color(0xFFD93A3F)
val SpaceOnLiveStreamRed = Color(0xFFFFFFFF)

// "Mission control" palette: navy and solar yellow, with orbit blue, teal and coral accents.
private val OrbitBlue = Color(0xFF2457E6)
private val Navy = Color(0xFF1B2A6B)
private val Teal = Color(0xFF0F766E)
private val TealBright = Color(0xFF2DD4BF)
private val Coral = Color(0xFFD93A3F)
private val CoralBright = Color(0xFFFF6B6B)

/** Every Material role is set so no component falls back to the default purple and grey tones. */
val SpaceLightColorScheme = lightColorScheme(
    primary = OrbitBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B2A7A),
    inversePrimary = Color(0xFFB5C8FF),
    secondary = Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E7FA),
    onSecondaryContainer = Color(0xFF121C4A),
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCF3EE),
    onTertiaryContainer = Color(0xFF003833),
    error = Coral,
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DF),
    onErrorContainer = Color(0xFF5C0A0D),
    background = SpaceBackgroundLight,
    onBackground = SpaceTextDark,
    surface = SpaceSurfaceLight,
    onSurface = SpaceTextDark,
    surfaceVariant = Color(0xFFE6EBF5),
    onSurfaceVariant = Color(0xFF4A5272),
    surfaceTint = OrbitBlue,
    inverseSurface = Color(0xFF1B2240),
    inverseOnSurface = Color(0xFFEEF1FA),
    outline = Color(0xFF8A93AE),
    outlineVariant = Color(0xFFD5DBE8),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCE2EE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FE),
    surfaceContainer = Color(0xFFEEF2FA),
    surfaceContainerHigh = Color(0xFFE8EDF7),
    surfaceContainerHighest = Color(0xFFE1E7F3)
)

val SpaceDarkColorScheme = darkColorScheme(
    primary = SpaceAccentYellow,
    onPrimary = SpaceDarkBlue,
    primaryContainer = Color(0xFF3D3300),
    onPrimaryContainer = Color(0xFFFFE58F),
    inversePrimary = Color(0xFF6B5A00),
    secondary = Color(0xFF9FB4FF),
    onSecondary = Color(0xFF0B1640),
    secondaryContainer = Navy,
    onSecondaryContainer = Color(0xFFDCE3FF),
    tertiary = TealBright,
    onTertiary = Color(0xFF00201C),
    tertiaryContainer = Color(0xFF004D45),
    onTertiaryContainer = Color(0xFFA7F3E8),
    error = CoralBright,
    onError = Color(0xFF3B0006),
    errorContainer = Color(0xFF5C1218),
    onErrorContainer = Color(0xFFFFDAD7),
    background = SpaceDarkBlue,
    onBackground = SpaceTextWhite,
    surface = SpaceLightBlue,
    onSurface = SpaceTextWhite,
    surfaceVariant = Color(0xFF18205A),
    onSurfaceVariant = Color(0xFFB9C1E6),
    surfaceTint = SpaceAccentYellow,
    inverseSurface = Color(0xFFE6E9F7),
    inverseOnSurface = Color(0xFF121633),
    outline = Color(0xFF6D76A8),
    outlineVariant = Color(0xFF2A3370),
    scrim = Color.Black,
    surfaceBright = Color(0xFF232B6B),
    surfaceDim = SpaceDarkBlue,
    surfaceContainerLowest = Color(0xFF000018),
    surfaceContainerLow = Color(0xFF080E38),
    surfaceContainer = SpaceLightBlue,
    surfaceContainerHigh = Color(0xFF141B55),
    surfaceContainerHighest = Color(0xFF1C2463)
)

/** Accent colors Material roles do not cover: status, pass brightness, gradients and the floating bar. */
@Immutable
data class SpaceColors(
    val live: Color,
    val onLive: Color,
    val success: Color,
    val warning: Color,
    val gold: Color,
    val onGold: Color,
    val visibilityVeryBright: Color,
    val visibilityBright: Color,
    val visibilityModerate: Color,
    val visibilityFaint: Color,
    val visibilityVeryFaint: Color,
    /** Navy-to-blue gradient for hero cards; reads as "space" in both modes. */
    val heroGradient: List<Color>,
    val onHero: Color,
    val navBarContainer: Color,
    val navBarBorder: Color,
    val navBarSelected: Color,
    val navBarUnselected: Color
)

val SpaceLightColors = SpaceColors(
    live = Coral,
    onLive = Color.White,
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    gold = Color(0xFFFFC83D),
    onGold = Color(0xFF2B1D00),
    visibilityVeryBright = Color(0xFFD97706),
    visibilityBright = Color(0xFFCA8A04),
    visibilityModerate = Teal,
    visibilityFaint = OrbitBlue,
    visibilityVeryFaint = Color(0xFF64748B),
    heroGradient = listOf(Color(0xFF0B1640), Color(0xFF1B2A6B), Color(0xFF2457E6)),
    onHero = Color.White,
    navBarContainer = Color.White.copy(alpha = 0.985f),
    navBarBorder = Color(0xFFD5DBE8),
    navBarSelected = OrbitBlue,
    navBarUnselected = Color(0xFF5B6382)
)

val SpaceDarkColors = SpaceColors(
    live = CoralBright,
    onLive = Color(0xFF2B0003),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    gold = Color(0xFFFFC83D),
    onGold = Color(0xFF2B1D00),
    visibilityVeryBright = Color(0xFFFBBF24),
    visibilityBright = Color(0xFFFDE047),
    visibilityModerate = TealBright,
    visibilityFaint = Color(0xFF9FB4FF),
    visibilityVeryFaint = Color(0xFF94A3B8),
    heroGradient = listOf(Color(0xFF0C1244), Color(0xFF1B2A6B), Color(0xFF2F4FB8)),
    onHero = Color.White,
    navBarContainer = Color(0xFF141B55).copy(alpha = 0.985f),
    navBarBorder = Color(0xFF2A3370),
    navBarSelected = SpaceAccentYellow,
    navBarUnselected = Color(0xFFB9C1E6)
)

val LocalSpaceColors = staticCompositionLocalOf { SpaceLightColors }

/** The app's extra accent colors for the current light or dark theme. */
val MaterialTheme.spaceColors: SpaceColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSpaceColors.current

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
    CompositionLocalProvider(
        LocalSpaceColors provides if (darkTheme) SpaceDarkColors else SpaceLightColors
    ) {
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
}
