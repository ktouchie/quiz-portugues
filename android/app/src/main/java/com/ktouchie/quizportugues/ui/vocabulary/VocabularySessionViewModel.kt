package com.ktouchie.quizportugues.ui.vocabulary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.VocabularyQuizItem
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.loadVocabularyEntries
import com.ktouchie.quizportugues.content.stringSimilarity
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.content.vocabularyQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One question: the item being tested, its modality, and (for multiple-choice) shuffled options. */
data class VocabQuestion(
    val item: VocabularyQuizItem,
    val modality: QuestionModality,
    val options: List<String> = emptyList(),
)

data class AnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String)

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class InProgress(
        val question: VocabQuestion,
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
 * Content breadth and input modality are gated the same way as
 * [com.ktouchie.quizportugues.ui.verbs.VerbSessionViewModel] (docs/MOBILE_APP_SPEC.md §9):
 * selection is restricted to unlocked CEFR tiers (favoring the newest unlocked tier), and each
 * item independently renders multiple-choice or typed based on its own typing readiness.
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
    private var records: Map<String, SrsRecord> = emptyMap()
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
        records = srsRepository.getAllRecords(MODULE_VOCABULARY)

        val itemsByLevel: Map<CefrLevel, List<String>> = allItems.groupBy({ cefrLevelOf(it) }, { it.id })
        val unlocked = unlockedTiers(itemsByLevel, records)
        val eligible = allItems.filter { cefrLevelOf(it) in unlocked }

        val dueIds = records.filterValues { it.nextReview in 1..now }.keys
        val due = eligible.filter { it.id in dueIds }.shuffled()
        val capped = due.take(QUICK_PRACTICE_CAP)

        val fillerNeeded = (QUICK_PRACTICE_CAP - capped.size).coerceAtLeast(0)
        val notDue = eligible.filterNot { it.id in dueIds }
        val frontier = unlocked.maxByOrNull { it.ordinal }
        val frontierFirst = notDue.filter { cefrLevelOf(it) == frontier }.shuffled()
        val restNotDue = notDue.filterNot { cefrLevelOf(it) == frontier }.shuffled()
        val filler = (frontierFirst + restNotDue).take(fillerNeeded)

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
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    private fun buildQuestion(item: VocabularyQuizItem): VocabQuestion {
        val modality = if (isReadyForTyping(records[item.id])) QuestionModality.TYPED else QuestionModality.MULTIPLE_CHOICE
        if (modality == QuestionModality.TYPED) return VocabQuestion(item, modality)

        val options = (confusableDistractors(item).map { it.english } + item.english).shuffled()
        return VocabQuestion(item, modality, options)
    }

    /**
     * Distractors are picked for genuine confusability, not random-from-category
     * (docs/MOBILE_APP_SPEC.md §9): each candidate is scored by the higher of two spelling
     * similarities against the correct item's Portuguese word — same-language look-alikes (e.g.
     * "irmã" as a distractor for "irmão") via [candidate.portuguese], and false-friend-style
     * cross-language look-alikes (e.g. "constipation" as a distractor for "constipação", which
     * actually means "a cold" in EP) via [candidate.english]. The top-scoring
     * [CONFUSABLE_SHORTLIST_SIZE] candidates are shuffled and sampled from, so a given word
     * doesn't show the identical distractor set on every attempt.
     */
    private fun confusableDistractors(item: VocabularyQuizItem): List<VocabularyQuizItem> {
        val ranked = allItems
            .filter { it.id != item.id }
            .sortedByDescending { candidate ->
                maxOf(
                    stringSimilarity(item.portuguese, candidate.portuguese),
                    stringSimilarity(item.portuguese, candidate.english),
                )
            }
        return ranked.take(CONFUSABLE_SHORTLIST_SIZE).shuffled().take(DISTRACTOR_COUNT)
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? SessionUiState.InProgress ?: return
        if (state.feedback != null) return // already answered this question, awaiting "continue"

        val item = state.question.item
        val wasCorrect = when (state.question.modality) {
            QuestionModality.TYPED -> answersMatch(answer, item.english)
            QuestionModality.MULTIPLE_CHOICE -> answer == item.english
        }
        val quality = when {
            wasCorrect && mistakeCounts.containsKey(item.id) -> 2 // correct after a mistake
            wasCorrect -> 4 // correct on the first try
            else -> 0
        }

        // Computed locally so the next question's modality decision never races the Room write —
        // see the matching comment in VerbSessionViewModel.
        val now = System.currentTimeMillis()
        val updatedRecord = sm2(records[item.id] ?: SrsRecord(), quality, now)
        records = records + (item.id to updatedRecord)
        viewModelScope.launch {
            srsRepository.recordAnswer(item.id, MODULE_VOCABULARY, quality, now)
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

    /**
     * Re-launches a session scoped to just the items the last round got wrong — see the matching
     * comment in VerbSessionViewModel for why this exists (SM-2 never schedules a same-day due
     * date, so "Nada por rever" right after a session with mistakes is expected, not a bug; this
     * is the actual same-day fix).
     */
    fun onRetryMistakes() {
        val mistakeIds = mistakeCounts.keys.toSet()
        if (mistakeIds.isEmpty()) return
        val mistakeItems = allItems.filter { it.id in mistakeIds }

        pool.clear()
        pool.addAll(mistakeItems.shuffled())
        totalQuestions = pool.size
        correctCount = 0
        errorCount = 0
        mistakeCounts.clear()
        startedAt = System.currentTimeMillis()

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
        private const val CONFUSABLE_SHORTLIST_SIZE = 8
    }
}
