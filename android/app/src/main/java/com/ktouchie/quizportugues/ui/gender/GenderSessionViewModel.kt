package com.ktouchie.quizportugues.ui.gender

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.GenderQuizItem
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.genderQuizItems
import com.ktouchie.quizportugues.content.getGenderHint
import com.ktouchie.quizportugues.content.loadGenderEntries
import com.ktouchie.quizportugues.content.stringSimilarity
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
import com.ktouchie.quizportugues.ui.navigation.MODULE_GENDER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GenderAnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String, val hint: String?)

sealed interface GenderSessionUiState {
    data object Loading : GenderSessionUiState

    data class InProgress(
        val item: GenderQuizItem,
        val modality: QuestionModality,
        /** Shuffled answer options; only populated when [modality] is [QuestionModality.MULTIPLE_CHOICE]. */
        val options: List<String> = emptyList(),
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: GenderAnswerFeedback? = null,
    ) : GenderSessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // "masculine (label)" -> mistake count
        val newMilestone: Int?,
    ) : GenderSessionUiState
}

/**
 * Drives a Quick Practice session for the Gender & Plural module — same pool/due/interleave/
 * retry-in-pool design, CEFR-tier content gating, and per-item MC-until-typing-ready modality as
 * [com.ktouchie.quizportugues.ui.verbs.VerbSessionViewModel]/
 * [com.ktouchie.quizportugues.ui.vocabulary.VocabularySessionViewModel] (docs/MOBILE_APP_SPEC.md §9).
 *
 * [savedStateHandle] optionally carries an Advanced-mode selection from [GenderSetupScreen] (nav
 * arg "categories", comma-joined category names, reusing the same query param name Vocabulary's
 * setup screen already established) — when present, the session pool is that selection, uncapped
 * and not restricted to unlocked CEFR tiers; when absent (Quick Practice), behavior matches Verb/
 * Vocabulary's Quick Practice.
 */
class GenderSessionViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

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

    private val _uiState = MutableStateFlow<GenderSessionUiState>(GenderSessionUiState.Loading)
    val uiState: StateFlow<GenderSessionUiState> = _uiState.asStateFlow()

    private var allItems: List<GenderQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()
    private val pool = ArrayDeque<GenderQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            val entries = loadGenderEntries(application.assets)
            allItems = genderQuizItems(entries)
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        records = srsRepository.getAllRecords(MODULE_GENDER)

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

    private fun buildQuickPracticePool(now: Long): List<GenderQuizItem> {
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
    private fun buildAdvancedPool(now: Long): List<GenderQuizItem> {
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
        _uiState.value = GenderSessionUiState.InProgress(
            item = item,
            modality = modality,
            options = if (modality == QuestionModality.MULTIPLE_CHOICE) buildOptions(item) else emptyList(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    /**
     * Distractors are ranked by [stringSimilarity] against the correct answer (docs/MOBILE_APP_SPEC.md
     * §9) — the same approach Vocabulary uses, since these answers are also short PT words —
     * rather than a random pick from the same category, which can land on options that don't even
     * share the pattern being tested.
     */
    private fun buildOptions(item: GenderQuizItem): List<String> {
        val ranked = allItems
            .filter { it.id != item.id && it.answer != item.answer }
            .distinctBy { it.answer }
            .sortedByDescending { candidate -> stringSimilarity(item.answer, candidate.answer) }

        val distractors = ranked.take(CONFUSABLE_SHORTLIST_SIZE).shuffled().take(DISTRACTOR_COUNT).map { it.answer }
        return (distractors + item.answer).shuffled()
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? GenderSessionUiState.InProgress ?: return
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
            srsRepository.recordAnswer(item.id, MODULE_GENDER, quality, now)
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
            feedback = GenderAnswerFeedback(
                wasCorrect = wasCorrect,
                correctAnswer = item.answer,
                hint = if (wasCorrect) null else getGenderHint(item.category),
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
            gamificationRepository.recordScoreIfBest(MODULE_GENDER, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) ->
                    allItems.find { it.id == id }?.let { "${it.masculine} (${it.label})" to count }
                }

            _uiState.value = GenderSessionUiState.Finished(
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
