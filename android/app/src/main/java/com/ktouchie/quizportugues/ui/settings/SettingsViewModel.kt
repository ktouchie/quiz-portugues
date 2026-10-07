package com.ktouchie.quizportugues.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(val justReset: Boolean = false)

/**
 * Backs the Settings screen's "reset all progress" action — the only way to get the app back to
 * a from-scratch state for manual testing without uninstalling/reinstalling (there's no other
 * path to wipe SRS history, best scores, streak, or seen milestones).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Wipes every Room table — SRS records, best scores, streak, seen milestones — across every
     *  module. [RoomDatabase.clearAllTables] is a blocking call, so it's dispatched off the main
     *  thread explicitly (unlike the DAOs' own `suspend` functions, which already do this). */
    fun resetAllProgress() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
            _uiState.value = SettingsUiState(justReset = true)
        }
    }

    fun acknowledgeReset() {
        _uiState.value = SettingsUiState(justReset = false)
    }
}
