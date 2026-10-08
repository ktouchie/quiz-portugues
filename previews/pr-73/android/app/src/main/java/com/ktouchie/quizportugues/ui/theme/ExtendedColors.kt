package com.ktouchie.quizportugues.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Tokens with no standard Material 3 [androidx.compose.material3.ColorScheme] role: `correct` has
 * no equivalent slot (`incorrect` maps cleanly onto `error`, exposed there instead), and the warm
 * amber gradient (streak pill, primary CTAs, submit button — "Direction A" in Color.kt) is a
 * two-stop brush, not a single color.
 */
data class ExtendedColors(
    val correct: Color,
    val incorrect: Color,
    val warmAccentStart: Color,
    val warmAccentEnd: Color,
    /** Muted warm text used on chip labels over [warmAccentStart]-tinted surfaces (e.g.
     *  `surfaceVariant`) — a warmer alternative to `colorScheme.onSurfaceVariant`. */
    val textWarm: Color,
) {
    val warmGradient: Brush get() = Brush.linearGradient(listOf(warmAccentStart, warmAccentEnd))
}

val LightExtendedColors = ExtendedColors(
    correct = LightCorrect,
    incorrect = LightIncorrect,
    warmAccentStart = LightWarmAccentStart,
    warmAccentEnd = LightWarmAccentEnd,
    textWarm = LightTextWarm,
)

val DarkExtendedColors = ExtendedColors(
    correct = DarkCorrect,
    incorrect = DarkIncorrect,
    warmAccentStart = DarkWarmAccentStart,
    warmAccentEnd = DarkWarmAccentEnd,
    textWarm = DarkTextWarm,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Usage: `ExtendedTheme.colors.correct` — mirrors `MaterialTheme.colorScheme`'s access pattern. */
object ExtendedTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
