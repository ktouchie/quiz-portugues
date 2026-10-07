package com.ktouchie.quizportugues.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Mapping from styles.css custom properties to Material 3 ColorScheme roles
// (docs/MOBILE_APP_SPEC.md §11):
//   --bg -> background, --surface -> surface, --surface-2 -> surfaceVariant,
//   --border -> outline, --accent -> primary, --accent-dim -> secondary,
//   --incorrect -> error (semantic fit), --text -> onBackground/onSurface,
//   --text-muted -> onSurfaceVariant. --correct has no ColorScheme equivalent — see
//   ExtendedColors.kt.

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

/**
 * App theme. Respects the OS-level light/dark setting by default, matching the web app's
 * [initTheme] behavior in common.js. Dynamic color (Android 12+) is deliberately not offered —
 * the whole point of this theme is pixel-for-token parity with the web app's fixed palette.
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
            typography = Typography,
            content = content,
        )
    }
}
