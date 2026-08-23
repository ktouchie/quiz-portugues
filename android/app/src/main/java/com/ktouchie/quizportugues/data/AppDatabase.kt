package com.ktouchie.quizportugues.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SrsRecordEntity::class,
        BestScoreEntity::class,
        StreakDataEntity::class,
        SeenMilestoneEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun srsRecordDao(): SrsRecordDao
    abstract fun bestScoreDao(): BestScoreDao
    abstract fun streakDao(): StreakDao
    abstract fun seenMilestoneDao(): SeenMilestoneDao

    companion object {
        private const val DATABASE_NAME = "quiz_portugues.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
