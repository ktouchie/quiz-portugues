package com.ktouchie.quizportugues.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SrsRecordDao {
    @Query("SELECT * FROM srs_records WHERE item_id = :itemId")
    suspend fun get(itemId: String): SrsRecordEntity?

    @Query("SELECT * FROM srs_records WHERE module = :module")
    suspend fun getAllForModule(module: String): List<SrsRecordEntity>

    @Query(
        "SELECT * FROM srs_records WHERE module = :module AND next_review_at > 0 " +
            "AND next_review_at <= :now",
    )
    suspend fun getDueForModule(module: String, now: Long): List<SrsRecordEntity>

    @Query("SELECT COUNT(*) FROM srs_records WHERE repetitions > 0")
    suspend fun countMastered(): Int

    @Upsert
    suspend fun upsert(record: SrsRecordEntity)
}

@Dao
interface BestScoreDao {
    @Query("SELECT * FROM best_scores WHERE module = :module")
    suspend fun get(module: String): BestScoreEntity?

    @Upsert
    suspend fun upsert(entity: BestScoreEntity)
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_data WHERE id = 1")
    suspend fun get(): StreakDataEntity?

    @Upsert
    suspend fun upsert(entity: StreakDataEntity)
}

@Dao
interface SeenMilestoneDao {
    @Query("SELECT milestone FROM seen_milestones")
    suspend fun getAll(): List<Int>

    // IGNORE, not REPLACE/upsert: a milestone is either seen or not — re-marking one that's
    // already there should be a silent no-op, never an error or a spurious "update".
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(entity: SeenMilestoneEntity)
}
