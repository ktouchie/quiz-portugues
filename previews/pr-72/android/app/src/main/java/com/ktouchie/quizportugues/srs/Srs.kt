package com.ktouchie.quizportugues.srs

/**
 * SM-2 spaced repetition algorithm — Kotlin port of `srs.js`. This is a behavioral port, not
 * shared code (see docs/MOBILE_APP_SPEC.md §7): Kotlin can't consume the web app's JS, so this is
 * a deliberate line-for-line translation of the same algorithm, kept in sync by the tests in
 * SrsTest.kt mirroring `tests/srs.test.js`'s cases.
 *
 * Where `srs.js` mutates its item object in place, this port returns a new [SrsRecord] instead —
 * an idiomatic Kotlin adjustment that keeps the function pure, not a behavioral change.
 */

const val DEFAULT_EASE = 2.5
private const val DAY_MILLIS = 86_400_000L

/**
 * One item's spaced-repetition state. Mirrors `srs.js`'s `SRSItem` shape 1:1 — this is also the
 * shape persisted in the Room `srs_records` table (see the Room schema task).
 */
data class SrsRecord(
    val interval: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Double = DEFAULT_EASE,
    val nextReview: Long = 0L,
)

/**
 * Compute the next SRS state for an item after answering it.
 *
 * @param quality 0 (blackout) to 5 (perfect) — the app only ever produces 4 (first-try correct),
 *   2 (correct after a mistake), or 0 (wrong), matching `quiz_base.js`'s scoring today.
 * @param now injectable for deterministic tests; defaults to the real clock.
 */
fun sm2(item: SrsRecord, quality: Int, now: Long = System.currentTimeMillis()): SrsRecord {
    // Ease factor is always updated, even on failed recalls.
    val newEase = (item.easeFactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        .coerceAtLeast(1.3)

    val (newRepetitions, newInterval) = if (quality < 3) {
        // Failed or barely recalled — reset schedule, item comes back tomorrow.
        0 to 1
    } else {
        val interval = when (item.repetitions) {
            0 -> 1
            1 -> 3
            else -> Math.round(item.interval * item.easeFactor).toInt()
        }
        (item.repetitions + 1) to interval
    }

    return item.copy(
        easeFactor = newEase,
        repetitions = newRepetitions,
        interval = newInterval,
        nextReview = now + newInterval * DAY_MILLIS,
    )
}

/** Mirrors `isItemDue()` in srs.js. */
fun isDue(record: SrsRecord, now: Long = System.currentTimeMillis()): Boolean =
    record.nextReview > 0 && record.nextReview <= now

/**
 * Mirrors `getDueItems()` in srs.js — filters a map of item-id -> record down to the ids that are
 * due now. The Room repository layer (see the Room schema/DAO tasks) is expected to query for
 * this directly rather than loading everything into memory; this is kept as a plain utility over
 * a [Map] for unit testing and for any in-memory session use.
 */
fun dueItemIds(records: Map<String, SrsRecord>, now: Long = System.currentTimeMillis()): List<String> =
    records.filterValues { isDue(it, now) }.keys.toList()
