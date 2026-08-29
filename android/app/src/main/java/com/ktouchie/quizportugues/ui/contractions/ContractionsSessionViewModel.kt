package com.ktouchie.quizportugues.ui.contractions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.ContractionQuizItem
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.contractionQuizItems
import com.ktouchie.quizportugues.content.loadContractionEntries
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
import com.ktouchie.quizportugues.ui.navigation.MODULE_CONTRACTIONS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContractionAnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String, val hint: String?)

sealed interface ContractionsSessionUiState {
    data object Loading : ContractionsSessionUiState

    data class InProgress(
        val item: ContractionQuizItem,
        val modality: QuestionModality,
        /** Shuffled answer options; only populated when [modality] is [QuestionModality.MULTIPLE_CHOICE]. */
        val options: List<String> = emptyList(),
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: ContractionAnswerFeedback? = null,
    ) : ContractionsSessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // "prep + article = answer" -> mistake count
        val newMilestone: Int?,
    ) : ContractionsSessionUiState
}

/**
 * Drives a Quick Practice session for the Contractions module — same pool/due/interleave/
 * retry-in-pool design, CEFR-tier content gating, and per-item MC-until-typing-ready modality as
 * the other modules (docs/MOBILE_APP_SPEC.md §9).
 *
 * [savedStateHandle] optionally carries an Advanced-mode selection from
 * [ContractionsSetupScreen] (nav arg "categories", reusing the same query param name the other
 * modules' setup screens already established).
 */
class ContractionsSessionViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

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

    private val _uiState = MutableStateFlow<ContractionsSessionUiState>(ContractionsSessionUiState.Loading)
    val uiState: StateFlow<ContractionsSessionUiState> = _uiState.asStateFlow()

    private var allItems: List<ContractionQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()
    private val pool = ArrayDeque<ContractionQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val entries = loadContractionEntries(application.assets)
            allItems = contractionQuizItems(entries)
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        records = srsRepository.getAllRecords(MODULE_CONTRACTIONS)

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

    private fun buildQuickPracticePool(now: Long): List<ContractionQuizItem> {
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
    private fun buildAdvancedPool(now: Long): List<ContractionQuizItem> {
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
        _uiState.value = ContractionsSessionUiState.InProgress(
            item = item,
            modality = modality,
            options = if (modality == QuestionModality.MULTIPLE_CHOICE) buildOptions(item) else emptyList(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    /**
     * Distractors are other contractions sharing the *same preposition* or the *same article*
     * (docs/MOBILE_APP_SPEC.md §9, GitHub #53) — the two axes a learner actually confuses (e.g. for
     * "de" + "este" = "deste", "deste"/"desta"/"destes" share the preposition and "neste"/"àquele"
     * share the article/demonstrative) — ranked same-preposition first since that's the stronger,
     * more common confusion, falling back to a plain different-answer pick from the rest of the
     * pool when neither axis has enough candidates.
     */
    private fun buildOptions(item: ContractionQuizItem): List<String> {
        val samePrep = allItems
            .filter { it.prep == item.prep && it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val sameArticle = allItems
            .filter { it.article == item.article && it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val fallback = allItems
            .filter { it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val distractors = (samePrep + sameArticle + fallback).distinct().take(DISTRACTOR_COUNT)
        return (distractors + item.answer).distinct().shuffled()
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? ContractionsSessionUiState.InProgress ?: return
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
            srsRepository.recordAnswer(item.id, MODULE_CONTRACTIONS, quality, now)
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
            feedback = ContractionAnswerFeedback(
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
            gamificationRepository.recordScoreIfBest(MODULE_CONTRACTIONS, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) ->
                    allItems.find { it.id == id }?.let { "${it.prep} + ${it.article} = ${it.answer}" to count }
                }

            _uiState.value = ContractionsSessionUiState.Finished(
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
