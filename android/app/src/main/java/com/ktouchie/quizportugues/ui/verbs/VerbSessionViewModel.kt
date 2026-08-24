package com.ktouchie.quizportugues.ui.verbs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ktouchie.quizportugues.content.CefrLevel
import com.ktouchie.quizportugues.content.Difficulty
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.VerbQuizItem
import com.ktouchie.quizportugues.content.answersMatch
import com.ktouchie.quizportugues.content.cefrLevelOf
import com.ktouchie.quizportugues.content.getVerbHint
import com.ktouchie.quizportugues.content.loadVerbEntries
import com.ktouchie.quizportugues.content.unlockedTiers
import com.ktouchie.quizportugues.content.verbQuizItems
import com.ktouchie.quizportugues.data.AppDatabase
import com.ktouchie.quizportugues.data.GamificationRepository
import com.ktouchie.quizportugues.data.SrsRepository
import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping
import com.ktouchie.quizportugues.srs.sm2
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
        val modality: QuestionModality,
        /** Shuffled answer options; only populated when [modality] is [QuestionModality.MULTIPLE_CHOICE]. */
        val options: List<String> = emptyList(),
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
 * retry-in-pool design as [com.ktouchie.quizportugues.ui.vocabulary.VocabularySessionViewModel].
 *
 * Two gates decide what a session looks like (docs/MOBILE_APP_SPEC.md §9):
 *  - **Content breadth**: [unlockedTiers] restricts item selection to CEFR tiers the user has
 *    earned access to, favoring the newest unlocked ("frontier") tier so it crosses its own
 *    unlock threshold rather than always being crowded out by earlier, better-known tiers.
 *  - **Input modality**: each selected item independently renders multiple-choice or typed,
 *    decided by [isReadyForTyping] on that item's own SRS record — not a fixed per-module choice.
 *    A wrong typed answer demotes the item back to multiple-choice for free (see Production.kt).
 *
 * [savedStateHandle] optionally carries an Advanced-mode selection from [VerbSetupScreen] (nav
 * args "tenses"/"difficulty", comma-joined tense names + an optional [Difficulty] name) — when
 * present, the session pool is that selection, uncapped and *not* restricted to unlocked CEFR
 * tiers (docs/MOBILE_APP_SPEC.md §8); when absent (Quick Practice, launched directly from Module
 * Home), behavior is unchanged from before Advanced mode existed.
 */
class VerbSessionViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val selectedTenses: Set<String>? = savedStateHandle.get<String>("tenses")
        ?.takeIf { it.isNotBlank() }
        ?.split(",")
        ?.toSet()
    private val difficultyFilter: Difficulty? = savedStateHandle.get<String>("difficulty")
        ?.takeIf { it.isNotBlank() }
        ?.let { Difficulty.valueOf(it) }

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
    private var records: Map<String, SrsRecord> = emptyMap()
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
        records = srsRepository.getAllRecords(MODULE_VERBS)

        val items = if (selectedTenses != null) buildAdvancedPool(now) else buildQuickPracticePool(now)

        pool.clear()
        pool.addAll(items.shuffled())
        totalQuestions = pool.size
        correctCount = 0
        errorCount = 0
        mistakeCounts.clear()
        startedAt = now

        showNextQuestion()
    }

    private fun buildQuickPracticePool(now: Long): List<VerbQuizItem> {
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

    /** Advanced mode (docs/MOBILE_APP_SPEC.md §8): the user's chosen tenses/difficulty, not
     *  restricted to unlocked CEFR tiers, no session cap — due items still surface first. */
    private fun buildAdvancedPool(now: Long): List<VerbQuizItem> {
        val tenses = selectedTenses.orEmpty()
        val eligible = allItems.filter {
            it.tense in tenses && (difficultyFilter == null || it.difficulty == difficultyFilter)
        }
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
        _uiState.value = VerbSessionUiState.InProgress(
            item = item,
            modality = modality,
            options = if (modality == QuestionModality.MULTIPLE_CHOICE) buildOptions(item) else emptyList(),
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            errorCount = errorCount,
        )
    }

    /**
     * Distractors are ranked hardest-first (docs/MOBILE_APP_SPEC.md §9): the same verb's *own*
     * form in a different tense but the *same person* is the closest lexical neighbor — it shares
     * the verb's stem and differs only in the ending a learner actually needs to know (e.g. for
     * "eu ___" (fazer, presente) = "faço", offering "fiz"/"fazia"/"farei" rather than "fazes"/
     * "faz"). Falls back to other persons of the same verb+tense (still same-verb, still
     * plausible), then finally to other verbs' forms of the same tense/person — only reached when
     * a verb+tense genuinely lacks enough distinct forms (e.g. imperativo has no "eu" form).
     */
    private fun buildOptions(item: VerbQuizItem): List<String> {
        val sameVerbPerson = allItems
            .filter { it.verb == item.verb && it.personIndex == item.personIndex && it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val sameVerbTense = allItems
            .filter { it.verb == item.verb && it.tense == item.tense && it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val fallback = allItems
            .filter { it.tense == item.tense && it.answer != item.answer }
            .map { it.answer }
            .distinct()
            .shuffled()

        val distractors = (sameVerbPerson + sameVerbTense + fallback)
            .distinct()
            .take(DISTRACTOR_COUNT)

        return (distractors + item.answer).distinct().shuffled()
    }

    fun onAnswerGiven(answer: String) {
        val state = _uiState.value as? VerbSessionUiState.InProgress ?: return
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

        // Computed locally (not awaited from the repository) so the next question's modality
        // decision — including an immediate requeue of this same item after a wrong answer —
        // never races the Room write; recordAnswer() persists the identical result since sm2()
        // is a pure function of (record, quality, now).
        val now = System.currentTimeMillis()
        val updatedRecord = sm2(records[item.id] ?: SrsRecord(), quality, now)
        records = records + (item.id to updatedRecord)
        viewModelScope.launch {
            srsRepository.recordAnswer(item.id, MODULE_VERBS, quality, now)
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

    /**
     * Re-launches a session scoped to just the items the last round got wrong, bypassing the
     * due/tier filtering `startSession()` applies — these items were already shown and eligible
     * a moment ago, and the point is immediate practice, not waiting for SM-2's next-day-or-later
     * due date (a wrong or corrected-after-mistake answer always schedules its next review at
     * least a day out, by design — see srs/Srs.kt — so "Nada por rever" right after a session
     * with mistakes is expected, not a bug; this button is the actual answer to "let me redo what
     * I got wrong now").
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
        private const val DISTRACTOR_COUNT = 3
    }
}
