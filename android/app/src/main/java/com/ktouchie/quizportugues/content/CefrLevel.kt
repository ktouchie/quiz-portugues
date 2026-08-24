package com.ktouchie.quizportugues.content

/**
 * Common European Framework of Reference for Languages levels, used to gate which content a
 * session can draw from (docs/MOBILE_APP_SPEC.md §9). Ordinal order is the unlock order:
 * [ContentProgression.unlockedTiers] walks `entries` from A1 upward.
 */
enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
}
