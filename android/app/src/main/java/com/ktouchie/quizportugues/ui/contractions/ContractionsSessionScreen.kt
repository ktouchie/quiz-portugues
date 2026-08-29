package com.ktouchie.quizportugues.ui.contractions

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
 * Contractions Quick Practice session (docs/MOBILE_APP_SPEC.md §8/§9/§11) — same shape as
 * SerEstarFicarSessionScreen. The example sentence (+ English) is shown alongside the prompt,
 * mirroring contractions_quiz.js's existing renderQuestion() behavior.
 */
@Composable
fun ContractionsSessionScreen(
    onDone: () -> Unit,
    viewModel: ContractionsSessionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is ContractionsSessionUiState.Loading -> LoadingContent()
        is ContractionsSessionUiState.InProgress -> InProgressContent(
            state = s,
            onAnswerGiven = viewModel::onAnswerGiven,
            onContinue = viewModel::onContinue,
        )
        is ContractionsSessionUiState.Finished -> ResultsContent(
            state = s,
            onDone = onDone,
            onRetryMistakes = viewModel::onRetryMistakes,
        )
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
    state: ContractionsSessionUiState.InProgress,
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

        Column {
            PromptCard(
                chipLabel = state.item.category,
                prompt = "Qual é a contração de ${state.item.prep} + ${state.item.article}?",
            )
            val caption = listOfNotNull(state.item.example, state.item.english).joinToString(" — ")
            if (caption.isNotEmpty()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }

        // Keyed on the item id so input state resets when a new question appears.
        key(state.item.id) {
            when (state.modality) {
                QuestionModality.TYPED -> TypedAnswerInput(
                    enabled = state.feedback == null,
                    onSubmit = onAnswerGiven,
                )
                QuestionModality.MULTIPLE_CHOICE -> MultipleChoiceOptions(
                    options = state.options,
                    correctAnswer = state.item.answer,
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
                feedback.hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun ResultsContent(
    state: ContractionsSessionUiState.Finished,
    onDone: () -> Unit,
    onRetryMistakes: () -> Unit,
) {
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
                state.topMistakes.forEach { (label, count) ->
                    Text("• $label ($count erro${if (count > 1) "s" else ""})", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.topMistakes.isNotEmpty()) {
                OutlinedButton(
                    onClick = onRetryMistakes,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExtendedTheme.colors.textWarm),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Praticar erros")
                }
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                Text("Concluído")
            }
        }
    }
}
