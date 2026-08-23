package com.ktouchie.quizportugues.data

/** In-memory test doubles for the DAOs — keeps the repository tests fast, plain JVM unit tests. */

class FakeSrsRecordDao : SrsRecordDao {
    val records = mutableMapOf<String, SrsRecordEntity>()

    override suspend fun get(itemId: String): SrsRecordEntity? = records[itemId]

    override suspend fun getAllForModule(module: String): List<SrsRecordEntity> =
        records.values.filter { it.module == module }

    override suspend fun getDueForModule(module: String, now: Long): List<SrsRecordEntity> =
        records.values.filter { it.module == module && it.nextReviewAt > 0 && it.nextReviewAt <= now }

    override suspend fun countMastered(): Int = records.values.count { it.repetitions > 0 }

    override suspend fun upsert(record: SrsRecordEntity) {
        records[record.itemId] = record
    }
}

class FakeBestScoreDao : BestScoreDao {
    val scores = mutableMapOf<String, BestScoreEntity>()

    override suspend fun get(module: String): BestScoreEntity? = scores[module]

    override suspend fun upsert(entity: BestScoreEntity) {
        scores[entity.module] = entity
    }
}

class FakeStreakDao : StreakDao {
    var stored: StreakDataEntity? = null

    override suspend fun get(): StreakDataEntity? = stored

    override suspend fun upsert(entity: StreakDataEntity) {
        stored = entity
    }
}

class FakeSeenMilestoneDao : SeenMilestoneDao {
    val seen = mutableSetOf<Int>()

    override suspend fun getAll(): List<Int> = seen.toList()

    override suspend fun markSeen(entity: SeenMilestoneEntity) {
        seen.add(entity.milestone)
    }
}
