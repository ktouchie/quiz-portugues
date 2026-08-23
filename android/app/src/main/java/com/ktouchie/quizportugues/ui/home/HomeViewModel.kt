package com.ktouchie.quizportugues.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(val currentStreak: Int = 0, val totalMastered: Int = 0)

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
     *  navigates back here — streak/mastered count may have just changed. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(
                currentStreak = gamificationRepository.getStreak().currentStreak,
                totalMastered = gamificationRepository.getTotalMastered(),
            )
        }
    }
}
