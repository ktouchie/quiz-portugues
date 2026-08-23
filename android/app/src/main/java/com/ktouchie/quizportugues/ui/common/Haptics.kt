package com.ktouchie.quizportugues.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Minimal "game feel" per docs/MOBILE_APP_SPEC.md §10: a haptic tick on correct/wrong answers,
 * distinguishable but not elaborate — a short single pulse for correct, a two-pulse pattern for
 * wrong. No sound design, no combo/XP feedback — that's explicitly backlog (§13).
 */

fun hapticCorrect(context: Context) = vibrateOneShot(context, durationMs = 15)

fun hapticWrong(context: Context) = vibrateWaveform(context, pattern = longArrayOf(0, 30, 40, 30))

private fun getVibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun vibrateOneShot(context: Context, durationMs: Long) {
    getVibrator(context).vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}

private fun vibrateWaveform(context: Context, pattern: LongArray) {
    getVibrator(context).vibrate(VibrationEffect.createWaveform(pattern, -1))
}
