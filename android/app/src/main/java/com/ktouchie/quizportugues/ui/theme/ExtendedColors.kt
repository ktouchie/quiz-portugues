package com.ktouchie.quizportugues.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * `correct`/`incorrect` from styles.css (used directly as text-feedback colors, e.g.
 * `.correct { color: var(--correct); }`) aren't a standard Material 3 [androidx.compose.material3.ColorScheme]
 * role — `incorrect` maps cleanly onto `error`, but `correct` has no equivalent slot, so both are
 * exposed here as an app-specific extension instead, alongside `MaterialTheme.colorScheme`.
 */
data class ExtendedColors(
    val correct: Color,
    val incorrect: Color,
)

val LightExtendedColors = ExtendedColors(correct = LightCorrect, incorrect = LightIncorrect)
val DarkExtendedColors = ExtendedColors(correct = DarkCorrect, incorrect = DarkIncorrect)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Usage: `ExtendedTheme.colors.correct` — mirrors `MaterialTheme.colorScheme`'s access pattern. */
object ExtendedTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
