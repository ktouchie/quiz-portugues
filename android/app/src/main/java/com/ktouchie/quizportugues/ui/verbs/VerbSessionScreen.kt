package com.ktouchie.quizportugues.ui.verbs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.content.tenseLabel
import com.ktouchie.quizportugues.ui.common.MultipleChoiceOptions
import com.ktouchie.quizportugues.ui.common.TypedAnswerInput
import com.ktouchie.quizportugues.ui.common.hapticCorrect
import com.ktouchie.quizportugues.ui.common.hapticWrong
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Verb Conjugation Quick Practice session (docs/MOBILE_APP_SPEC.md §8/§9): each question renders
 * multiple-choice or typed input depending on the item's own typing readiness — free-text input
 * relies on the device keyboard's own accent long-press, no in-app accent picker. Results shown
 * in place rather than as a separate route — same pattern as the Vocabulary session, matching how
 * quiz_base.js actually behaves.
 */
@Composable
fun VerbSessionScreen(
    onDone: () -> Unit,
    viewModel: VerbSessionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is VerbSessionUiState.Loading -> LoadingContent()
        is VerbSessionUiState.InProgress -> InProgressContent(
            state = s,
            onAnswerGiven = viewModel::onAnswerGiven,
            onContinue = viewModel::onContinue,
        )
        is VerbSessionUiState.Finished -> ResultsContent(state = s, onDone = onDone)
    }
}

@Composable
private fun LoadingContent() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("A carregar…")
    }
}

@Composable
private fun InProgressContent(
    state: VerbSessionUiState.InProgress,
    onAnswerGiven: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(state.feedback) {
        val feedback = state.feedback ?: return@LaunchedEffect
        if (feedback.wasCorrect) hapticCorrect(context) else hapticWrong(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { state.questionNumber.toFloat() / state.totalQuestions },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${state.correctCount} certas · ${state.errorCount} erradas")
        Text(
            text = "Conjugue o verbo ${state.item.verb} (${state.item.english}) no tempo " +
                "${tenseLabel(state.item.tense)} para ${state.item.person}:",
        )

        // Keyed on the item id so input state resets when a new question appears.
        key(state.item.id) {
            when (state.modality) {
                QuestionModality.TYPED -> TypedAnswerInput(
                    enabled = state.feedback == null,
                    onSubmit = onAnswerGiven,
                )
                QuestionModality.MULTIPLE_CHOICE -> MultipleChoiceOptions(
                    options = state.options,
                    enabled = state.feedback == null,
                    onSelect = onAnswerGiven,
                )
            }
        }

        state.feedback?.let { feedback ->
            val color = if (feedback.wasCorrect) ExtendedTheme.colors.correct else ExtendedTheme.colors.incorrect
            Text(
                text = if (feedback.wasCorrect) "Correto!" else "Resposta certa: ${feedback.correctAnswer}",
                color = color,
            )
            feedback.hint?.let { Text(it) }
            feedback.exampleSentence?.let { Text("Exemplo: $it") }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun ResultsContent(state: VerbSessionUiState.Finished, onDone: () -> Unit) {
    val total = state.correctCount + state.errorCount
    val accuracyPct = if (total > 0) (state.correctCount * 100) / total else 0
    val elapsedSeconds = state.elapsedMillis / 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Resultados")
        Text("Tempo: ${elapsedSeconds}s")
        Text("Precisão: $accuracyPct%")
        Text("${state.correctCount} certas · ${state.errorCount} erradas")

        if (state.newMilestone != null) {
            Text(
                text = "Parabéns! ${state.newMilestone} itens dominados! 🎉",
                color = ExtendedTheme.colors.correct,
            )
        }

        if (state.topMistakes.isNotEmpty()) {
            Text("Principais erros:")
            state.topMistakes.forEach { (label, count) ->
                Text("• $label ($count erro${if (count > 1) "s" else ""})")
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Concluído")
        }
    }
}
