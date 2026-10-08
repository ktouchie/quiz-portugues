package com.ktouchie.quizportugues.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room schema — see docs/MOBILE_APP_SPEC.md §6.2. Replaces the web app's flat `localStorage` JSON
 * blobs (`srs_verbs`/`srs_vocab`, `bestScore_*`, `streak_data`, `seen_milestones`) with real
 * tables keyed by the stable content-item IDs from the content package.
 *
 * All entity fields are plain primitives (String/Int/Long/Double) so no Room [androidx.room.TypeConverters]
 * are needed — richer domain types (e.g. `java.time.LocalDate` for dates) live in the srs/
 * gamification packages' pure models, converted at the repository boundary.
 */

@Entity(tableName = "srs_records")
data class SrsRecordEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "module") val module: String,
    @ColumnInfo(name = "repetitions") val repetitions: Int,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "next_review_at") val nextReviewAt: Long,
    @ColumnInfo(name = "last_quality") val lastQuality: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "best_scores")
data class BestScoreEntity(
    @PrimaryKey @ColumnInfo(name = "module") val module: String,
    @ColumnInfo(name = "best_correct_count") val bestCorrectCount: Int,
)

/** Single global row (id is always 1, enforced by the repository, not a DB-level CHECK constraint
 *  — Room's schema DSL doesn't expose table-level CHECK, so this is enforced at the call site). */
@Entity(tableName = "streak_data")
data class StreakDataEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "current_streak") val currentStreak: Int,
    @ColumnInfo(name = "longest_streak") val longestStreak: Int,
    /** ISO-8601 (YYYY-MM-DD), or null if the user has never practiced. */
    @ColumnInfo(name = "last_completed_date") val lastCompletedDate: String?,
)

/**
 * Milestones are global (based on total mastered items across every module), not per-module —
 * matches `gamification.js`'s single `SEEN_MILESTONES_KEY`, not the module-scoped table this
 * spec originally (incorrectly) described.
 */
@Entity(tableName = "seen_milestones")
data class SeenMilestoneEntity(
    @PrimaryKey @ColumnInfo(name = "milestone") val milestone: Int,
)
