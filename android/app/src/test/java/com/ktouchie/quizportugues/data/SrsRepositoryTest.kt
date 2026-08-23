package com.ktouchie.quizportugues.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SrsRepositoryTest {

    @Test
    fun `getRecord returns null for an item never answered`() = runBlocking {
        val repo = SrsRepository(FakeSrsRecordDao())
        assertNull(repo.getRecord("comer|||presente|||0"))
    }

    @Test
    fun `recordAnswer creates a fresh record on first answer and persists it`() = runBlocking {
        val dao = FakeSrsRecordDao()
        val repo = SrsRepository(dao)
        val now = 1_700_000_000_000L

        val updated = repo.recordAnswer("comer|||presente|||0", "verbs", quality = 4, now = now)

        assertEquals(1, updated.repetitions)
        assertEquals(1, updated.interval)
        val persisted = dao.records.getValue("comer|||presente|||0")
        assertEquals("verbs", persisted.module)
        assertEquals(4, persisted.lastQuality)
        assertEquals(now, persisted.updatedAt)
    }

    @Test
    fun `recordAnswer builds on the previously persisted record`() = runBlocking {
        val dao = FakeSrsRecordDao()
        val repo = SrsRepository(dao)

        repo.recordAnswer("k", "verbs", quality = 4, now = 1_000L)
        val second = repo.recordAnswer("k", "verbs", quality = 4, now = 2_000L)

        assertEquals(2, second.repetitions)
        assertEquals(3, second.interval)
    }

    @Test
    fun `getDueItemIds only returns due items for the given module`() = runBlocking {
        val dao = FakeSrsRecordDao()
        val repo = SrsRepository(dao)
        val now = 1_700_000_000_000L
        dao.records["due-verb"] = SrsRecordEntity("due-verb", "verbs", 1, 2.5, 1, now - 1, 4, now - 1)
        dao.records["future-verb"] = SrsRecordEntity("future-verb", "verbs", 1, 2.5, 3, now + 1, 4, now)
        dao.records["due-vocab"] = SrsRecordEntity("due-vocab", "vocabulary", 1, 2.5, 1, now - 1, 4, now - 1)

        assertEquals(listOf("due-verb"), repo.getDueItemIds("verbs", now))
    }

    @Test
    fun `countMastered delegates to the dao`() = runBlocking {
        val dao = FakeSrsRecordDao()
        dao.records["a"] = SrsRecordEntity("a", "verbs", 1, 2.5, 1, 0, 4, 0)
        dao.records["b"] = SrsRecordEntity("b", "verbs", 0, 2.5, 0, 0, 0, 0)
        val repo = SrsRepository(dao)

        assertEquals(1, repo.countMastered())
    }
}
