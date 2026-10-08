package com.ktouchie.quizportugues.ui.subjunctive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.SubjunctiveQuizItem
import com.ktouchie.quizportugues.content.loadSubjunctiveEntries
import com.ktouchie.quizportugues.content.subjunctiveQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isDue
import com.ktouchie.quizportugues.ui.navigation.MODULE_SUBJUNCTIVE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubjunctiveSetupUiState(
    val loading: Boolean = true,
    val categories: List<String> = emptyList(),
    val selectedCategories: Set<String> = emptySet(),
    val matchingCount: Int = 0,
    val dueCount: Int = 0,
)

/**
 * Backs the Subjunctive Advanced setup screen (docs/MOBILE_APP_SPEC.md §8, GitHub #54): pick
 * specific categories for a session that's uncapped and not restricted to unlocked CEFR tiers.
 * Defaults to every category selected, matching the other modules' Setup screens.
 */
class SubjunctiveSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())

    private val _uiState = MutableStateFlow(SubjunctiveSetupUiState())
    val uiState: StateFlow<SubjunctiveSetupUiState> = _uiState.asStateFlow()

    private var allItems: List<SubjunctiveQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()

    init {
        viewModelScope.launch {
            allItems = subjunctiveQuizItems(loadSubjunctiveEntries(application.assets))
            records = srsRepository.getAllRecords(MODULE_SUBJUNCTIVE)
            val categories = allItems.map { it.category }.distinct()
            _uiState.value = _uiState.value.copy(categories = categories, selectedCategories = categories.toSet())
            recompute()
        }
    }

    fun toggleCategory(category: String) {
        val current = _uiState.value.selectedCategories
        _uiState.value = _uiState.value.copy(
            selectedCategories = if (category in current) current - category else current + category,
        )
        recompute()
    }

    fun selectAllCategories() {
        _uiState.value = _uiState.value.copy(selectedCategories = _uiState.value.categories.toSet())
        recompute()
    }

    fun clearCategories() {
        _uiState.value = _uiState.value.copy(selectedCategories = emptySet())
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val matching = allItems.filter { it.category in state.selectedCategories }
        val now = System.currentTimeMillis()
        val due = matching.count { item -> records[item.id]?.let { isDue(it, now) } ?: false }
        _uiState.value = state.copy(loading = false, matchingCount = matching.size, dueCount = due)
    }
}
