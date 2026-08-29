package com.ktouchie.quizportugues.ui.indirectspeech

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.indirectSpeechQuizItems
import com.ktouchie.quizportugues.content.loadIndirectSpeechEntries
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.isDue
import com.ktouchie.quizportugues.ui.navigation.MODULE_INDIRECT_SPEECH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IndirectSpeechSetupUiState(
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val dueCount: Int = 0,
)

/**
 * Backs the Indirect Speech Advanced setup screen (docs/MOBILE_APP_SPEC.md §8, GitHub #55).
 * Unlike every other module's setup screen, there's no category/tense picker — indirect_speech.json
 * has no categories to choose from — so this just surfaces the item/due counts before starting an
 * uncapped, unrestricted session over all 20 items.
 */
class IndirectSpeechSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())

    private val _uiState = MutableStateFlow(IndirectSpeechSetupUiState())
    val uiState: StateFlow<IndirectSpeechSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allItems = indirectSpeechQuizItems(loadIndirectSpeechEntries(application.assets))
            val records = srsRepository.getAllRecords(MODULE_INDIRECT_SPEECH)
            val now = System.currentTimeMillis()
            val due = allItems.count { item -> records[item.id]?.let { isDue(it, now) } ?: false }
            _uiState.value = IndirectSpeechSetupUiState(loading = false, totalCount = allItems.size, dueCount = due)
        }
    }
}
