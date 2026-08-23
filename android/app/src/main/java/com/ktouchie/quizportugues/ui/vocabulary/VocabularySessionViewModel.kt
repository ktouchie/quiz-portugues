package com.ktouchie.quizportugues.ui.vocabulary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.VocabularyQuizItem
import com.ktouchie.quizportugues.content.loadVocabularyEntries
import com.ktouchie.quizportugues.content.vocabularyQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One multiple-choice question: the item being tested plus its shuffled answer options. */
data class VocabQuestion(val item: VocabularyQuizItem, val options: List<String>)

data class AnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String)

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class InProgress(
        val question: VocabQuestion,
        val questionNumber: Int,
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: AnswerFeedback? = null,
    ) : SessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // Portuguese word -> mistake count, worst first
        val newMilestone: Int?,
    ) : SessionUiState
}

/**
 * Drives a single Quick Practice session for the Vocabulary module (docs/MOBILE_APP_SPEC.md §8):
 * SRS-due items first, capped at [QUICK_PRACTICE_CAP], interleaved with new items, shuffled. A
 * wrong answer keeps the item in the pool (reinserted at a random later position) rather than
 * dropping it — mirrors `quiz_base.js`'s retry-in-pool behavior.
 *
 * No dependency-injection framework is set up yet (deliberately, to avoid scope creep before
 * there's a second consumer that would justify one) — [AndroidViewModel] gives just enough
 * [Application] context to build [AppDatabase] directly.
 */
class VocabularySessionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())
    private val gamificationRepository = GamificationRepository(
        db.bestScoreDao(),
        db.streakDao(),
        db.seenMilestoneDao(),
        srsRepository,
    )

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var allItems: List<VocabularyQuizItem> = emptyList()
    private val pool = ArrayDeque<VocabularyQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val entries = loadVocabularyEntries(application.assets)
            allItems = vocabularyQuizItems(entries)
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        val dueIds = srsRepository.getDueItemIds(MODULE_VOCABULARY, now).toSet()
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
        _uiState.value = SessionUiState.InProgress(
            question = buildQuestion(item),
            questionNumber = totalQuestions - pool.size + 1,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    private fun buildQuestion(item: VocabularyQuizItem): VocabQuestion {
        val sameCategoryDistractors = allItems
            .filter { it.category == item.category && it.id != item.id }
            .shuffled()
            .take(DISTRACTOR_COUNT)
        val distractors = if (sameCategoryDistractors.size == DISTRACTOR_COUNT) {
            sameCategoryDistractors
        } else {
            // Category too small — fall back to any other item in the module.
            allItems.filter { it.id != item.id }.shuffled().take(DISTRACTOR_COUNT)
        }
        val options = (distractors.map { it.english } + item.english).shuffled()
        return VocabQuestion(item, options)
    }

    fun onAnswerSelected(selected: String) {
        val state = _uiState.value as? SessionUiState.InProgress ?: return
        if (state.feedback != null) return // already answered this question, awaiting "continue"

        val item = state.question.item
        val wasCorrect = selected == item.english
        val quality = when {
            wasCorrect && mistakeCounts.containsKey(item.id) -> 2 // correct after a mistake
            wasCorrect -> 4 // correct on the first try
            else -> 0
        }

        viewModelScope.launch {
            srsRepository.recordAnswer(item.id, MODULE_VOCABULARY, quality)
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
            feedback = AnswerFeedback(wasCorrect, item.english),
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    fun onContinue() {
        showNextQuestion()
    }

    private fun finishSession() {
        viewModelScope.launch {
            gamificationRepository.recordScoreIfBest(MODULE_VOCABULARY, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) -> allItems.find { it.id == id }?.let { it.portuguese to count } }

            _uiState.value = SessionUiState.Finished(
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
        private const val DISTRACTOR_COUNT = 3
    }
}
