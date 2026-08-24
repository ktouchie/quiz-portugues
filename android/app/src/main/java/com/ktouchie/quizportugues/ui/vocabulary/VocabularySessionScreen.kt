package com.ktouchie.quizportugues.ui.vocabulary

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
import com.ktouchie.quizportugues.ui.common.MultipleChoiceOptions
import com.ktouchie.quizportugues.ui.common.TypedAnswerInput
import com.ktouchie.quizportugues.ui.common.hapticCorrect
import com.ktouchie.quizportugues.ui.common.hapticWrong
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Vocabulary Quick Practice session (docs/MOBILE_APP_SPEC.md §8/§9): each question renders
 * multiple-choice or typed input depending on the item's own typing readiness, results shown in
 * place when the session ends — mirrors quiz_base.js's actual behavior of transitioning within a
 * single page rather than navigating to a separate results URL.
 */
@Composable
fun VocabularySessionScreen(
    onDone: () -> Unit,
    viewModel: VocabularySessionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is SessionUiState.Loading -> LoadingContent()
        is SessionUiState.InProgress -> InProgressContent(
            state = s,
            onAnswerGiven = viewModel::onAnswerGiven,
            onContinue = viewModel::onContinue,
        )
        is SessionUiState.Finished -> ResultsContent(state = s, onDone = onDone)
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
    state: SessionUiState.InProgress,
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
        Text(text = "Qual é o significado de \"${state.question.item.portuguese}\"?")

        // Keyed on the item id so input state resets when a new question appears.
        key(state.question.item.id) {
            when (state.question.modality) {
                QuestionModality.TYPED -> TypedAnswerInput(
                    enabled = state.feedback == null,
                    onSubmit = onAnswerGiven,
                )
                QuestionModality.MULTIPLE_CHOICE -> MultipleChoiceOptions(
                    options = state.question.options,
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
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun ResultsContent(state: SessionUiState.Finished, onDone: () -> Unit) {
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
            state.topMistakes.forEach { (word, count) ->
                Text("• $word ($count erro${if (count > 1) "s" else ""})")
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Concluído")
        }
    }
}
