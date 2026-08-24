package com.ktouchie.quizportugues.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.loadVerbEntries
import com.ktouchie.quizportugues.content.loadVocabularyEntries
import com.ktouchie.quizportugues.content.verbQuizItems
import com.ktouchie.quizportugues.content.vocabularyQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isDue
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A module's card stats on the Home screen: how many items are due right now, and how much of
 *  the module has been seen at least once (0-100). */
data class ModuleProgress(val dueCount: Int = 0, val seenPct: Int = 0)

data class HomeUiState(
    val currentStreak: Int = 0,
    val totalMastered: Int = 0,
    val verbProgress: ModuleProgress = ModuleProgress(),
    val vocabularyProgress: ModuleProgress = ModuleProgress(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())
    private val gamificationRepository = GamificationRepository(
        db.bestScoreDao(),
        db.streakDao(),
        db.seenMilestoneDao(),
        srsRepository,
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Called from [HomeScreen] each time it's (re-)entered, e.g. after a session finishes and
     *  navigates back here — streak/mastered count/per-module progress may have just changed. */
    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val application = getApplication<Application>()

            val verbItemIds = verbQuizItems(loadVerbEntries(application.assets)).map { it.id }
            val vocabItemIds = vocabularyQuizItems(loadVocabularyEntries(application.assets)).map { it.id }
            val verbRecords = srsRepository.getAllRecords(MODULE_VERBS)
            val vocabRecords = srsRepository.getAllRecords(MODULE_VOCABULARY)

            _uiState.value = HomeUiState(
                currentStreak = gamificationRepository.getStreak().currentStreak,
                totalMastered = gamificationRepository.getTotalMastered(),
                verbProgress = moduleProgress(verbItemIds, verbRecords, now),
                vocabularyProgress = moduleProgress(vocabItemIds, vocabRecords, now),
            )
        }
    }

    private fun moduleProgress(itemIds: List<String>, records: Map<String, SrsRecord>, now: Long): ModuleProgress {
        if (itemIds.isEmpty()) return ModuleProgress()
        val due = itemIds.count { id -> records[id]?.let { isDue(it, now) } ?: false }
        val seen = itemIds.count { id -> (records[id]?.repetitions ?: 0) > 0 }
        return ModuleProgress(dueCount = due, seenPct = (seen * 100) / itemIds.size)
    }
}
