package com.ktouchie.quizportugues.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Mapping onto Material 3 ColorScheme roles (docs/MOBILE_APP_SPEC.md §11):
//   bg -> background, surface -> surface, surface2 -> surfaceVariant, border -> outline,
//   accent -> primary, accentDim -> secondary, incorrect -> error (semantic fit),
//   text -> onBackground/onSurface, textMuted -> onSurfaceVariant. `correct` and the warm
//   gradient accent have no ColorScheme equivalent — see ExtendedColors.kt.

private val LightColorScheme = lightColorScheme(
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightTextMuted,
    primary = LightAccent,
    onPrimary = OnAccent,
    secondary = LightAccentDim,
    onSecondary = OnAccent,
    error = LightIncorrect,
    onError = OnAccent,
    outline = LightBorder,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextMuted,
    primary = DarkAccent,
    onPrimary = OnAccent,
    secondary = DarkAccentDim,
    onSecondary = OnAccent,
    error = DarkIncorrect,
    onError = OnAccent,
    outline = DarkBorder,
)

/** Big, soft-rounded corners — "Direction A"'s defining shape language, well above Material 3's
 *  defaults (cards ~22-28dp, chips/pills fully rounded via [androidx.compose.foundation.shape.CircleShape]
 *  at usage sites rather than through this scale). */
private val WarmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val baseTypography = Typography()
private val WarmTypography = baseTypography.copy(
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
)

/**
 * App theme ("Direction A — Warm Encourager", see Color.kt). Respects the OS-level light/dark
 * setting by default, matching the web app's `initTheme` behavior in common.js. Dynamic color
 * (Android 12+) is deliberately not offered — the palette is a fixed, chosen brand identity, not
 * meant to shift with wallpaper.
 */
@Composable
fun QuizPortuguesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WarmTypography,
            shapes = WarmShapes,
            content = content,
        )
    }
}
