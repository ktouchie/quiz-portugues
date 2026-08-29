package com.ktouchie.quizportugues.ui.indirectspeech

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.IndirectSpeechQuizItem
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.indirectSpeechQuizItems
import com.ktouchie.quizportugues.content.loadIndirectSpeechEntries
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
import com.ktouchie.quizportugues.ui.navigation.MODULE_INDIRECT_SPEECH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IndirectSpeechAnswerFeedback(val wasCorrect: Boolean, val correctAnswer: String, val hint: String?)

sealed interface IndirectSpeechSessionUiState {
    data object Loading : IndirectSpeechSessionUiState

    data class InProgress(
        val item: IndirectSpeechQuizItem,
        val modality: QuestionModality,
        /** Shuffled answer options; only populated when [modality] is [QuestionModality.MULTIPLE_CHOICE]. */
        val options: List<String> = emptyList(),
        val totalQuestions: Int,
        val correctCount: Int,
        val errorCount: Int,
        val feedback: IndirectSpeechAnswerFeedback? = null,
    ) : IndirectSpeechSessionUiState

    data class Finished(
        val correctCount: Int,
        val errorCount: Int,
        val elapsedMillis: Long,
        val topMistakes: List<Pair<String, Int>>, // "verb_direct → answer" -> mistake count
        val newMilestone: Int?,
    ) : IndirectSpeechSessionUiState
}

/**
 * Drives a Quick Practice session for the Indirect Speech module — same pool/due/interleave/
 * retry-in-pool design and per-item MC-until-typing-ready modality as the other modules
 * (docs/MOBILE_APP_SPEC.md §9). Unlike every other module, content breadth has nothing to
 * restrict by — indirect_speech.json is a flat 20-item list with no categories — so CEFR gating
 * here just decides *whether* the (single-tier) content is unlocked at all, not *which slice* of
 * it is, and Advanced mode ([IndirectSpeechSetupScreen]) only lifts the session cap/tier
 * restriction rather than letting the user pick anything.
 *
 * [savedStateHandle]'s "categories" nav arg (reused from the other modules' setup screens purely
 * as an "advanced mode requested" flag, since there's nothing to actually filter by here) being
 * present at all — regardless of its value — means Advanced mode.
 */
class IndirectSpeechSessionViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val advancedMode: Boolean = !savedStateHandle.get<String>("categories").isNullOrBlank()

    private val db = AppDatabase.getInstance(application)
    private val srsRepository = SrsRepository(db.srsRecordDao())
    private val gamificationRepository = GamificationRepository(
        db.bestScoreDao(),
        db.streakDao(),
        db.seenMilestoneDao(),
        srsRepository,
    )

    private val _uiState = MutableStateFlow<IndirectSpeechSessionUiState>(IndirectSpeechSessionUiState.Loading)
    val uiState: StateFlow<IndirectSpeechSessionUiState> = _uiState.asStateFlow()

    private var allItems: List<IndirectSpeechQuizItem> = emptyList()
    private var records: Map<String, SrsRecord> = emptyMap()
    private val pool = ArrayDeque<IndirectSpeechQuizItem>()
    private var totalQuestions = 0
    private var correctCount = 0
    private var errorCount = 0
    private val mistakeCounts = mutableMapOf<String, Int>() // item id -> times gotten wrong
    private var startedAt = 0L

    init {
        viewModelScope.launch {
            allItems = indirectSpeechQuizItems(loadIndirectSpeechEntries(application.assets))
            startSession()
        }
    }

    private suspend fun startSession() {
        val now = System.currentTimeMillis()
        records = srsRepository.getAllRecords(MODULE_INDIRECT_SPEECH)

        val items = if (advancedMode) buildAdvancedPool(now) else buildQuickPracticePool(now)

        pool.clear()
        pool.addAll(items.shuffled())
        totalQuestions = pool.size
        correctCount = 0
        errorCount = 0
        mistakeCounts.clear()
        startedAt = now

        showNextQuestion()
    }

    private fun buildQuickPracticePool(now: Long): List<IndirectSpeechQuizItem> {
        val itemsByLevel: Map<CefrLevel, List<String>> = allItems.groupBy({ cefrLevelOf(it) }, { it.id })
        val unlocked = unlockedTiers(itemsByLevel, records)
        val eligible = allItems.filter { cefrLevelOf(it) in unlocked }

        val dueIds = records.filterValues { it.nextReview in 1..now }.keys
        val due = eligible.filter { it.id in dueIds }.shuffled()
        val capped = due.take(QUICK_PRACTICE_CAP)

        val fillerNeeded = (QUICK_PRACTICE_CAP - capped.size).coerceAtLeast(0)
        val notDue = eligible.filterNot { it.id in dueIds }.shuffled()
        val filler = notDue.take(fillerNeeded)

        return capped + filler
    }

    /** Advanced mode (docs/MOBILE_APP_SPEC.md §8): every item, uncapped and not restricted to
     *  unlocked CEFR tiers — due items still surface first. There's nothing to filter by
     *  ([IndirectSpeechSetupScreen] has no picker), so this is just "drill everything, unrestricted". */
    private fun buildAdvancedPool(now: Long): List<IndirectSpeechQuizItem> {
        val dueIds = records.filterValues { it.nextReview in 1..now }.keys
        val due = allItems.filter { it.id in dueIds }
        val notDue = allItems.filterNot { it.id in dueIds }
        return due + notDue
    }

    private fun showNextQuestion() {
        val item = pool.firstOrNull()
        if (item == null) {
            finishSession()
            return
        }
        val modality = if (isReadyForTyping(records[item.id])) QuestionModality.TYPED else QuestionModality.MULTIPLE_CHOICE
        _uiState.value = IndirectSpeechSessionUiState.InProgress(
            item = item,
            modality = modality,
            options = if (modality == QuestionModality.MULTIPLE_CHOICE) buildOptions(item) else emptyList(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    /**
     * The original direct-speech verb form is the most natural distractor (docs/MOBILE_APP_SPEC.md
     * §9, GitHub #55) — it's the exact mistake a learner makes by forgetting to backshift the
     * tense. Falls back to a plain different-answer pick from the rest of the pool for the
     * remaining slots.
     */
    private fun buildOptions(item: IndirectSpeechQuizItem): List<String> {
        val original = listOf(item.verbDirect).filter { it != item.answer }

        val fallback = allItems
            .filter { it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val distractors = (original + fallback).distinct().take(DISTRACTOR_COUNT)
        return (distractors + item.answer).distinct().shuffled()
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? IndirectSpeechSessionUiState.InProgress ?: return
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
            srsRepository.recordAnswer(item.id, MODULE_INDIRECT_SPEECH, quality, now)
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
            feedback = IndirectSpeechAnswerFeedback(
                wasCorrect = wasCorrect,
                correctAnswer = item.answer,
                // Matches indirect_speech_quiz.js's actual getHint() — rule + example, not the
                // per-item "hint" field (which the web app parses but never actually displays).
                hint = if (wasCorrect) {
                    null
                } else {
                    listOfNotNull(item.rule, item.indirectFull?.let { "Ex: $it" }).joinToString(" — ")
                },
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
            gamificationRepository.recordScoreIfBest(MODULE_INDIRECT_SPEECH, correctCount)
            gamificationRepository.recordPracticeToday()
            val newMilestone = gamificationRepository.checkAndMarkNewMilestone()

            val topMistakes = mistakeCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (id, count) ->
                    allItems.find { it.id == id }?.let { "${it.verbDirect} → ${it.answer}" to count }
                }

            _uiState.value = IndirectSpeechSessionUiState.Finished(
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
