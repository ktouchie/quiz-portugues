package com.ktouchie.quizportugues.ui.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.content.QuestionModality
import com.ktouchie.quizportugues.ui.common.AccuracyRing
import com.ktouchie.quizportugues.ui.common.GradientProgressBar
import com.ktouchie.quizportugues.ui.common.MilestoneBanner
import com.ktouchie.quizportugues.ui.common.MultipleChoiceOptions
import com.ktouchie.quizportugues.ui.common.PromptCard
import com.ktouchie.quizportugues.ui.common.StatChip
import com.ktouchie.quizportugues.ui.common.TypedAnswerInput
import com.ktouchie.quizportugues.ui.common.hapticCorrect
import com.ktouchie.quizportugues.ui.common.hapticWrong
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Vocabulary Quick Practice session (docs/MOBILE_APP_SPEC.md §8/§9/§11): each question renders
 * multiple-choice or typed input depending on the item's own typing readiness, results shown in
 * place when the session ends — mirrors quiz_base.js's actual behavior of transitioning within a
 * single page rather than navigating to a separate results URL. Styled for "Direction A — Warm
 * Encourager".
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Reflects items permanently cleared (correctCount), not which question is on screen —
        // see the matching comment in VerbSessionScreen.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientProgressBar(
                progress = state.correctCount.toFloat() / state.totalQuestions,
                modifier = Modifier.weight(1f).height(10.dp),
            )
            Text(
                text = "${state.correctCount}/${state.totalQuestions}",
                style = MaterialTheme.typography.labelLarge,
                color = ExtendedTheme.colors.textWarm,
            )
        }
        Text(
            text = "${state.correctCount} certas · ${state.errorCount} erradas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PromptCard(
            chipLabel = state.question.item.category,
            prompt = state.question.item.portuguese,
        )

        // Keyed on the item id so input state resets when a new question appears.
        key(state.question.item.id) {
            when (state.question.modality) {
                QuestionModality.TYPED -> TypedAnswerInput(
                    enabled = state.feedback == null,
                    onSubmit = onAnswerGiven,
                )
                QuestionModality.MULTIPLE_CHOICE -> MultipleChoiceOptions(
                    options = state.question.options,
                    correctAnswer = state.question.item.english,
                    revealAnswer = state.feedback != null,
                    enabled = state.feedback == null,
                    onSelect = onAnswerGiven,
                )
            }
        }

        state.feedback?.let { feedback ->
            val tint = if (feedback.wasCorrect) ExtendedTheme.colors.correct else ExtendedTheme.colors.incorrect
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tint.copy(alpha = 0.12f), MaterialTheme.shapes.large)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (feedback.wasCorrect) "Correto!" else "Resposta certa: ${feedback.correctAnswer}",
                    color = tint,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sessão concluída! 🎉", style = MaterialTheme.typography.headlineSmall)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            AccuracyRing(percent = accuracyPct)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatChip("${elapsedSeconds}s", "Tempo", modifier = Modifier.weight(1f))
            StatChip("${state.correctCount}", "Corretas", modifier = Modifier.weight(1f))
            StatChip("${state.errorCount}", "Erros", modifier = Modifier.weight(1f))
        }

        state.newMilestone?.let { MilestoneBanner(count = it) }

        if (state.topMistakes.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                    .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("PARA REVER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.topMistakes.forEach { (word, count) ->
                    Text("• $word ($count erro${if (count > 1) "s" else ""})", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Text("Concluído")
        }
    }
}
