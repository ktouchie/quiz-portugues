package com.ktouchie.quizportugues.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Direction A — Warm Encourager": cream/toast palette, warm amber gradient accents alongside the
 * existing blue, picked by the product owner from three mockup directions (docs/MOBILE_APP_SPEC.md
 * §11). This is a deliberate divergence from the web app's `styles.css` tokens the theme originally
 * ported 1:1 — the Android app's visual language is now its own, not required to track the web
 * app's palette. `--correct`/`--incorrect` semantics and the blue accent role carry over; the rest
 * (background, surface, border, text, and the new warm-gradient accent) do not.
 */

// --- Light ---
val LightBg = Color(0xFFFBF7F1)
val LightSurface = Color(0xFFFFF9F0)
val LightSurface2 = Color(0xFFFFF1DC)
val LightBorder = Color(0xFFF3E6D2)
val LightAccent = Color(0xFF4A7CF7)
val LightAccentDim = Color(0xFF3A63CC)
val LightWarmAccentStart = Color(0xFFFFB25E)
val LightWarmAccentEnd = Color(0xFFFF8A3D)
val LightCorrect = Color(0xFF1E9E6B)
val LightIncorrect = Color(0xFFE03E52)
val LightText = Color(0xFF3A2E1F)
val LightTextMuted = Color(0xFF9B8A6F)
val LightTextWarm = Color(0xFFB8874A)

// --- Dark ---
val DarkBg = Color(0xFF1F1710)
val DarkSurface = Color(0xFF2A2018)
val DarkSurface2 = Color(0xFF3A2C1C)
val DarkBorder = Color(0xFF4A3826)
val DarkAccent = Color(0xFF7FC1FF)
val DarkAccentDim = Color(0xFF5E9BFF)
val DarkWarmAccentStart = Color(0xFFFFB25E)
val DarkWarmAccentEnd = Color(0xFFFF8A3D)
val DarkCorrect = Color(0xFF3DDC97)
val DarkIncorrect = Color(0xFFFF5C6C)
val DarkText = Color(0xFFF5E9DA)
val DarkTextMuted = Color(0xFFB8A88C)
val DarkTextWarm = Color(0xFFFFC98C)

// Buttons use white text on solid/gradient accent backgrounds in both themes.
val OnAccent = Color(0xFFFFFFFF)
