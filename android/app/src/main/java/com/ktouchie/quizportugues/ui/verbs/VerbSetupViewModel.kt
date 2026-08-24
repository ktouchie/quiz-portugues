package com.ktouchie.quizportugues.ui.verbs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.Difficulty
import com.ktouchie.quizportugues.content.VERB_TENSES
import com.ktouchie.quizportugues.content.VerbQuizItem
import com.ktouchie.quizportugues.content.loadVerbEntries
import com.ktouchie.quizportugues.content.verbQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isDue
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VerbSetupUiState(
    val loading: Boolean = true,
    val selectedTenses: Set<String> = VERB_TENSES.toSet(),
    val difficulty: Difficulty? = null,
    val matchingCount: Int = 0,
    val dueCount: Int = 0,
)

/**
 * Backs the Verb Conjugation Advanced setup screen (docs/MOBILE_APP_SPEC.md §8, GitHub #28):
 * pick specific tenses and a difficulty filter for a session that's uncapped and not restricted
 * to unlocked CEFR tiers — for a learner who wants to deliberately drill something specific
 * rather than let Quick Practice ease them into it. Defaults to every tense selected (all
 * difficulties) so the common "just drill everything, unrestricted" case needs no taps.
 */
class VerbSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())

    private val _uiState = MutableStateFlow(VerbSetupUiState())
    val uiState: StateFlow<VerbSetupUiState> = _uiState.asStateFlow()

    private var allItems: List<VerbQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()

    init {
        viewModelScope.launch {
            allItems = verbQuizItems(loadVerbEntries(application.assets))
            records = srsRepository.getAllRecords(MODULE_VERBS)
            recompute()
        }
    }

    fun toggleTense(tense: String) {
        val current = _uiState.value.selectedTenses
        _uiState.value = _uiState.value.copy(
            selectedTenses = if (tense in current) current - tense else current + tense,
        )
        recompute()
    }

    fun selectAllTenses() {
        _uiState.value = _uiState.value.copy(selectedTenses = VERB_TENSES.toSet())
        recompute()
    }

    fun clearTenses() {
        _uiState.value = _uiState.value.copy(selectedTenses = emptySet())
        recompute()
    }

    fun setDifficulty(difficulty: Difficulty?) {
        _uiState.value = _uiState.value.copy(difficulty = difficulty)
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val matching = allItems.filter {
            it.tense in state.selectedTenses && (state.difficulty == null || it.difficulty == state.difficulty)
        }
        val now = System.currentTimeMillis()
        val due = matching.count { item -> records[item.id]?.let { isDue(it, now) } ?: false }
        _uiState.value = state.copy(loading = false, matchingCount = matching.size, dueCount = due)
    }
}
