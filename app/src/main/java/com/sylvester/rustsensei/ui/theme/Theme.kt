package com.sylvester.rustsensei.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Theme-aware accent palette (Syltech: Signal Orange on Ink) ────────
// Field names kept for source compatibility; values now map to Syltech tokens.
@Immutable
data class AppColorPalette(
    val accent: Color,
    val cyan: Color,          // legacy slot — now the Amber node accent
    val amber: Color,
    val success: Color,
    val error: Color,
    val codeBackground: Color,
    val inlineCodeBg: Color,
    val inlineCodeText: Color,
    val userBubbleBg: Color,
    val aiBubbleBg: Color,
    val codeBg: Color,
    val codeHeader: Color,
    val codeText: Color,
    val pathAccentOrange: Color,
    val pathAccentPurple: Color
)

private val DarkAccentPalette = AppColorPalette(
    accent = SignalOrange,
    cyan = Amber,
    amber = Amber,
    success = Positive,
    error = Danger,
    codeBackground = Ink,
    inlineCodeBg = SlateHigh,
    inlineCodeText = Amber,
    userBubbleBg = Color(0xFF2A1608),   // warm ink, signal-tinted
    aiBubbleBg = Panel,
    codeBg = Ink,
    codeHeader = Slate,
    codeText = Mist,
    pathAccentOrange = SignalBright,
    pathAccentPurple = Amber            // purple retired -> amber
)

private val LightAccentPalette = AppColorPalette(
    accent = Color(0xFFC7481A),         // Signal, darkened for light surfaces
    cyan = Color(0xFFB4692A),
    amber = Color(0xFFB4692A),
    success = Color(0xFFC7481A),
    error = Color(0xFFB23A28),
    codeBackground = Ink,               // code stays dark on both themes
    inlineCodeBg = Color(0xFFEDE7DE),
    inlineCodeText = Color(0xFFB4551F),
    userBubbleBg = Color(0xFFF3E4D6),
    aiBubbleBg = Color(0xFFEDEBE6),
    codeBg = Ink,
    codeHeader = Slate,
    codeText = Mist,
    pathAccentOrange = Color(0xFFC7481A),
    pathAccentPurple = Color(0xFFB4692A)
)

val LocalAppColors = staticCompositionLocalOf { DarkAccentPalette }

object AppColors {
    val current: AppColorPalette
        @Composable get() = LocalAppColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = SignalOrange,
    onPrimary = Ink,
    primaryContainer = SignalDeep,
    onPrimaryContainer = Color(0xFFFFD9BE),
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A2410),
    onSecondaryContainer = Color(0xFFFFD9BE),
    tertiary = Amber,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF3A2410),
    onTertiaryContainer = Color(0xFFFFD9BE),
    error = Danger,
    onError = Color.White,
    errorContainer = Color(0xFF4A1A12),
    onErrorContainer = Color(0xFFFFD9D2),
    background = Ink,
    onBackground = Mist,
    surface = InkRaised,
    onSurface = Mist,
    surfaceVariant = Slate,
    onSurfaceVariant = Ash,
    outline = Line,
    outlineVariant = SlateHigh,
    inverseSurface = Mist,
    inverseOnSurface = Ink,
    surfaceContainerHighest = PanelHigh,
    surfaceContainerHigh = Panel,
    surfaceContainer = SlateHigh,
    surfaceContainerLow = Slate,
    surfaceContainerLowest = Ink
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC7481A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF7A2C00),
    secondary = Color(0xFF9A5A2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DFC9),
    onSecondaryContainer = Color(0xFF4A2A0E),
    tertiary = Color(0xFF9A5A2A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6DFC9),
    onTertiaryContainer = Color(0xFF4A2A0E),
    error = Color(0xFFB23A28),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF5A1B10),
    background = LightBackground,
    onBackground = Ink,
    surface = LightSurface,
    onSurface = Ink,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFD6D0C7),
    outlineVariant = Color(0xFFE6E2DB),
    inverseSurface = Ink,
    inverseOnSurface = LightBackground,
    surfaceContainerHighest = Color(0xFFE4E0D8),
    surfaceContainerHigh = Color(0xFFEDEBE6),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerLow = LightSurface,
    surfaceContainerLowest = Color.White
)

// Syltech geometry — flat, small radii (was 4 / 8 / 12 / 16 / 28)
val RustSenseiShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun RustSenseiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Off by design — Signal Orange is the Syltech identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accentPalette = if (darkTheme) DarkAccentPalette else LightAccentPalette

    CompositionLocalProvider(LocalAppColors provides accentPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RustSenseiTypography,
            shapes = RustSenseiShapes,
            content = content
        )
    }
}
