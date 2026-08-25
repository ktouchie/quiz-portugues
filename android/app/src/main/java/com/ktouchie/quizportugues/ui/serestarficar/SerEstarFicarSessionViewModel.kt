package com.ktouchie.quizportugues.ui.serestarficar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.SerEstarFicarQuizItem
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.loadSerEstarFicarEntries
import com.ktouchie.quizportugues.content.serEstarFicarQuizItems
import com.ktouchie.quizportugues.content.serEstarFicarVerbDistractors
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
import com.ktouchie.quizportugues.ui.navigation.MODULE_SER_ESTAR_FICAR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SerEstarFicarAnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String, val hint: String?)

sealed interface SerEstarFicarSessionUiState {
    data object Loading : SerEstarFicarSessionUiState

    data class InProgress(
        val item: SerEstarFicarQuizItem,
        val modality: QuestionModality,
        /** Shuffled answer options; only populated when [modality] is [QuestionModality.MULTIPLE_CHOICE]. */
        val options: List<String> = emptyList(),
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: SerEstarFicarAnswerFeedback? = null,
    ) : SerEstarFicarSessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // sentence-with-answer-filled-in -> mistake count
        val newMilestone: Int?,
    ) : SerEstarFicarSessionUiState
}

/**
 * Drives a Quick Practice session for the Ser/Estar/Ficar module — same pool/due/interleave/
 * retry-in-pool design, CEFR-tier content gating, and per-item MC-until-typing-ready modality as
 * the other modules (docs/MOBILE_APP_SPEC.md §9).
 *
 * [savedStateHandle] optionally carries an Advanced-mode selection from [SerEstarFicarSetupScreen]
 * (nav arg "categories", reusing the same query param name Vocabulary/Gender's setup screens
 * already established).
 */
class SerEstarFicarSessionViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val selectedCategories: Set<String>? = savedStateHandle.get<String>("categories")
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.toSet()

    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())
    private val gamificationRepository = GamificationRepository(
        db.bestScoreDao(),
        db.streakDao(),
        db.seenMilestoneDao(),
        srsRepository,
    )

    private val _uiState = MutableStateFlow<SerEstarFicarSessionUiState>(SerEstarFicarSessionUiState.Loading)
    val uiState: StateFlow<SerEstarFicarSessionUiState> = _uiState.asStateFlow()

    private var allItems: List<SerEstarFicarQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()
    private val pool = ArrayDeque<SerEstarFicarQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val entries = loadSerEstarFicarEntries(application.assets)
            allItems = serEstarFicarQuizItems(entries)
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        records = srsRepository.getAllRecords(MODULE_SER_ESTAR_FICAR)

        val items = if (selectedCategories != null) buildAdvancedPool(now) else buildQuickPracticePool(now)

        pool.clear()
        pool.addAll(items.shuffled())
        totalQuestions = pool.size
        correctCount = 0
        errorCount = 0
        mistakeCounts.clear()
        startedAt = now

        showNextQuestion()
    }

    private fun buildQuickPracticePool(now: Long): List<SerEstarFicarQuizItem> {
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

        return capped + filler
    }

    /** Advanced mode (docs/MOBILE_APP_SPEC.md §8): the user's chosen categories, not restricted to
     *  unlocked CEFR tiers, no session cap — due items still surface first. */
    private fun buildAdvancedPool(now: Long): List<SerEstarFicarQuizItem> {
        val categories = selectedCategories.orEmpty()
        val eligible = allItems.filter { it.category in categories }
        val dueIds = records.filterValues { it.nextReview in 1..now }.keys
        val due = eligible.filter { it.id in dueIds }
        val notDue = eligible.filterNot { it.id in dueIds }
        return due + notDue
    }

    private fun showNextQuestion() {
        val item = pool.firstOrNull()
        if (item == null) {
            finishSession()
            return
        }
        val modality = if (isReadyForTyping(records[item.id])) QuestionModality.TYPED else QuestionModality.MULTIPLE_CHOICE
        _uiState.value = SerEstarFicarSessionUiState.InProgress(
            item = item,
            modality = modality,
            options = if (modality == QuestionModality.MULTIPLE_CHOICE) buildOptions(item) else emptyList(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    /**
     * Distractors are the *other two verbs'* conjugated form for the same grammatical person
     * (docs/MOBILE_APP_SPEC.md §9, GitHub #52) — the actual skill this module tests is picking the
     * right verb, not conjugating it correctly, so "é"/"está" are genuinely confusable distractors
     * for "ficou" in a way an unrelated word wouldn't be. Falls back to a plain different-answer
     * pick from the rest of the pool when [serEstarFicarVerbDistractors] has nothing for this
     * answer (shouldn't happen for the current content, but keeps this from crashing on future
     * content added without updating that lookup table).
     */
    private fun buildOptions(item: SerEstarFicarQuizItem): List<String> {
        val verbDistractors = serEstarFicarVerbDistractors(item.answer).filter { it != item.answer }

        val fallback = allItems
            .filter { it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val distractors = (verbDistractors + fallback).distinct().take(DISTRACTOR_COUNT)
        return (distractors + item.answer).distinct().shuffled()
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? SerEstarFicarSessionUiState.InProgress ?: return
        if (state.feedback != null) return // already answered this question, awaiting "continue"
        if (state.modality == QuestionModality.TYPED && answer.isBlank()) return

        val item = state.item
        val wasCorrect = when (state.modality) {
            QuestionModality.TYPED -> answersMatch(answer, item.answer)
            QuestionModality.MULTIPLE_CHOICE -> answer == item.answer
        }
        val quality = when {
            wasCorrect && mistakeCounts.containsKey(item.id) -> 2
            wasCorrect -> 4
            else -> 0
        }

        // Computed locally so the next question's modality decision never races the Room write —
        // see the matching comment in VerbSessionViewModel.
        val now = System.currentTimeMillis()
        val updatedRecord = sm2(records[item.id] ?: SrsRecord(), quality, now)
        records = records + (item.id to updatedRecord)
        viewModelScope.launch {
            srsRepository.recordAnswer(item.id, MODULE_SER_ESTAR_FICAR, quality, now)
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
            feedback = SerEstarFicarAnswerFeedback(
                wasCorrect = wasCorrect,
                correctAnswer = item.answer,
                hint = if (wasCorrect) null else item.hint,
            ),
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    fun onContinue() {
        showNextQuestion()
    }

    /** Re-launches a session scoped to just the items the last round got wrong — see the matching
     *  comment in VerbSessionViewModel for why this exists. */
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
            gamificationRepository.recordScoreIfBest(MODULE_SER_ESTAR_FICAR, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) ->
                    allItems.find { it.id == id }?.let { it.sentence.replace("___", "[${it.answer}]") to count }
                }

            _uiState.value = SerEstarFicarSessionUiState.Finished(
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
