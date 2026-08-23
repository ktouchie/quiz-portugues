package com.ktouchie.quizportugues.ui.verbs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.VerbQuizItem
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.getVerbHint
import com.ktouchie.quizportugues.content.loadVerbEntries
import com.ktouchie.quizportugues.content.verbQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VerbAnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String, val hint: String?, val exampleSentence: String?)

sealed interface VerbSessionUiState {
    data object Loading : VerbSessionUiState

    data class InProgress(
        val item: VerbQuizItem,
        val questionNumber: Int,
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: VerbAnswerFeedback? = null,
    ) : VerbSessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // "verb (tense, pessoa)" -> mistake count
        val newMilestone: Int?,
    ) : VerbSessionUiState
}

/**
 * Drives a Quick Practice session for the Verb Conjugation module — same pool/due/interleave/
 * retry-in-pool design as [com.ktouchie.quizportugues.ui.vocabulary.VocabularySessionViewModel],
 * but typed-answer input compared via [answersMatch] instead of multiple choice, and wrong
 * answers surface a grammar hint + example sentence (docs/MOBILE_APP_SPEC.md §9).
 */
class VerbSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())
    private val gamificationRepository = GamificationRepository(
        db.bestScoreDao(),
        db.streakDao(),
        db.seenMilestoneDao(),
        srsRepository,
    )

    private val _uiState = MutableStateFlow<VerbSessionUiState>(VerbSessionUiState.Loading)
    val uiState: StateFlow<VerbSessionUiState> = _uiState.asStateFlow()

    private var allItems: List<VerbQuizItem> = emptyList()
    private val pool = ArrayDeque<VerbQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val entries = loadVerbEntries(application.assets)
            allItems = verbQuizItems(entries)
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        val dueIds = srsRepository.getDueItemIds(MODULE_VERBS, now).toSet()
        val due = allItems.filter { it.id in dueIds }.shuffled()
        val capped = due.take(QUICK_PRACTICE_CAP)
        val fillerNeeded = (QUICK_PRACTICE_CAP - capped.size).coerceAtLeast(0)
        val filler = allItems.filterNot { it.id in dueIds }.shuffled().take(fillerNeeded)

        pool.clear()
        pool.addAll((capped + filler).shuffled())
        totalQuestions = pool.size
        correctCount = 0
        errorCount = 0
        mistakeCounts.clear()
        startedAt = now

        showNextQuestion()
    }

    private fun showNextQuestion() {
        val item = pool.firstOrNull()
        if (item == null) {
            finishSession()
            return
        }
        _uiState.value = VerbSessionUiState.InProgress(
            item = item,
            questionNumber = totalQuestions - pool.size + 1,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    fun onAnswerSubmitted(typedAnswer: String) {
        val state = _uiState.value as? VerbSessionUiState.InProgress ?: return
        if (state.feedback != null) return // already answered this question, awaiting "continue"
        if (typedAnswer.isBlank()) return

        val item = state.item
        val wasCorrect = answersMatch(typedAnswer, item.answer)
        val quality = when {
            wasCorrect && mistakeCounts.containsKey(item.id) -> 2
            wasCorrect -> 4
            else -> 0
        }

        viewModelScope.launch {
            srsRepository.recordAnswer(item.id, MODULE_VERBS, quality)
        }

        if (wasCorrect) {
            correctCount++
            pool.removeFirst()
        } else {
            errorCount++
            mistakeCounts[item.id] = (mistakeCounts[item.id] ?: 0) + 1
            pool.removeFirst()
            val reinsertAt = if (pool.isEmpty()) 0 else (1..pool.size).random()
            pool.add(reinsertAt, item)
        }

        _uiState.value = state.copy(
            feedback = VerbAnswerFeedback(
                wasCorrect = wasCorrect,
                correctAnswer = item.answer,
                hint = if (wasCorrect) null else getVerbHint(item.tense, item.personIndex),
                exampleSentence = if (wasCorrect) null else item.exampleSentence,
            ),
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    fun onContinue() {
        showNextQuestion()
    }

    private fun finishSession() {
        viewModelScope.launch {
            gamificationRepository.recordScoreIfBest(MODULE_VERBS, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) ->
                    allItems.find { it.id == id }?.let { "${it.verb} (${it.tense}, ${it.person})" to count }
                }

            _uiState.value = VerbSessionUiState.Finished(
                correctCount = correctCount,
                errorCount = errorCount,
                elapsedMillis = System.currentTimeMillis() - startedAt,
                topMistakes = topMistakes,
                newMilestone = newMilestone,
            )
        }
    }

    companion object {
        const val QUICK_PRACTICE_CAP = 12
    }
}
