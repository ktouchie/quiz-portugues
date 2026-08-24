package com.ktouchie.quizportugues.srs

/** Minimum consecutive correct reviews before an item can graduate to typed production. */
const val PRODUCTION_MIN_REPETITIONS = 3

/** Minimum SM-2 interval (days) before an item can graduate to typed production. */
const val PRODUCTION_MIN_INTERVAL_DAYS = 6

/**
 * Whether an item has been reviewed solidly enough to move from multiple-choice to typed
 * production (docs/MOBILE_APP_SPEC.md §9). Deliberately a higher bar than gamification's
 * "mastered" count (`repetitions > 0` — Daos.kt's `countMastered()`, ported from
 * `getTotalMastered()` in gamification.js), which is a loose encouragement counter, not a gate on
 * the harder input mode.
 *
 * No separate demotion logic is needed: a wrong answer runs `sm2()`'s `quality < 3` branch, which
 * resets `repetitions` to 0 and `interval` to 1 — so a missed typed item fails this check on its
 * next review and renders as multiple-choice again until re-earned.
 */
fun isReadyForTyping(record: SrsRecord?): Boolean =
    record != null &&
        record.repetitions >= PRODUCTION_MIN_REPETITIONS &&
        record.interval >= PRODUCTION_MIN_INTERVAL_DAYS
