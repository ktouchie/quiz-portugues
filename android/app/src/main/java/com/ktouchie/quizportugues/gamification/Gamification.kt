package com.ktouchie.quizportugues.gamification

import com.ktouchie.quizportugues.srs.SrsRecord
import java.time.LocalDate

/**
 * Streak tracking and milestone detection — Kotlin port of `gamification.js`'s streak/milestone
 * logic, using it as the behavioral reference (see docs/MOBILE_APP_SPEC.md §10). Not a shared
 * module — Kotlin can't consume the web app's JS — so this is reimplemented directly, with tests
 * covering the same behavior since the web app itself has none for this logic today.
 *
 * The personal-goals feature in gamification.js (`loadGoal`/`saveGoal`/`getGoalProgress`) isn't
 * part of the v1 scope (docs/MOBILE_APP_SPEC.md §2/§10) and is intentionally not ported here.
 * `showMilestoneBanner`'s DOM manipulation is UI, not logic — its Android equivalent is a Compose
 * component, tracked under the milestone-banner UI task, not here.
 */

val MILESTONES = listOf(10, 25, 50, 100, 250, 500)

data class StreakData(
    val lastPracticeDate: LocalDate? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
)

/**
 * Record that the user practiced today and return the updated streak. Mirrors `updateStreak()`:
 * no-op if already counted today, +1 if the last practice was yesterday, reset to 1 otherwise.
 *
 * @param today injectable for deterministic tests; defaults to the real date.
 */
fun updateStreak(data: StreakData, today: LocalDate = LocalDate.now()): StreakData {
    if (data.lastPracticeDate == today) return data // already counted today

    val yesterday = today.minusDays(1)
    val newCurrentStreak = if (data.lastPracticeDate == yesterday) data.currentStreak + 1 else 1

    return data.copy(
        currentStreak = newCurrentStreak,
        longestStreak = maxOf(data.longestStreak, newCurrentStreak),
        lastPracticeDate = today,
    )
}

/**
 * Count of items that have been reviewed at least once, mirroring `getTotalMastered()`. Unlike
 * the web app — which sums across 7 separate per-module `localStorage` blobs via a hardcoded
 * `SRS_KEYS` list that has to be remembered on every new module — this operates on whatever
 * [SrsRecord] collection is passed in. In the Room-backed app (see the Room schema/DAO tasks),
 * that's naturally every row in the single `srs_records` table across all modules, no hardcoded
 * key list to maintain.
 */
fun countMastered(records: Collection<SrsRecord>): Int = records.count { it.repetitions > 0 }

/**
 * Mirrors `checkMilestone()`: the first milestone threshold that's been reached but not yet
 * marked seen, or null if none. The caller is responsible for persisting the returned milestone
 * into the seen-set (Room `seen_milestones` table) — this function itself has no side effects.
 */
fun checkMilestone(totalMastered: Int, seenMilestones: Set<Int>): Int? =
    MILESTONES.firstOrNull { totalMastered >= it && it !in seenMilestones }
