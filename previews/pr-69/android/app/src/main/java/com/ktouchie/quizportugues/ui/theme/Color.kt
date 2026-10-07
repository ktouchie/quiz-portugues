package com.ktouchie.quizportugues.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ported 1:1 from styles.css' `:root` / `[data-theme="dark"]` custom properties
 * (docs/MOBILE_APP_SPEC.md §11). Keep these in sync if the web app's palette changes.
 */

// --- Light (:root) ---
val LightBg = Color(0xFFF4F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEEF0F7)
val LightBorder = Color(0xFFD0D5E8)
val LightAccent = Color(0xFF4A7CF7)
val LightAccentDim = Color(0xFF3A63CC)
val LightCorrect = Color(0xFF1E9E6B)
val LightIncorrect = Color(0xFFE03E52)
val LightText = Color(0xFF1A1E2E)
val LightTextMuted = Color(0xFF5A6382)

// --- Dark ([data-theme="dark"]) ---
val DarkBg = Color(0xFF0F1117)
val DarkSurface = Color(0xFF1A1D27)
val DarkSurface2 = Color(0xFF242736)
val DarkBorder = Color(0xFF2E3248)
val DarkAccent = Color(0xFF7FC1FF)
val DarkAccentDim = Color(0xFF5E9BFF)
val DarkCorrect = Color(0xFF3DDC97)
val DarkIncorrect = Color(0xFFFF5C6C)
val DarkText = Color(0xFFE2E8F0)
val DarkTextMuted = Color(0xFF7B8AB8)

// Buttons use white text on --accent/--accent-dim backgrounds in both themes
// (`button { background-color: var(--accent); color: #fff; }` in styles.css) — ported literally
// rather than adjusted for contrast, to stay visually consistent with the web app.
val OnAccent = Color(0xFFFFFFFF)
