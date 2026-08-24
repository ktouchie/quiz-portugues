package com.ktouchie.quizportugues.ui.verbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.content.Difficulty
import com.ktouchie.quizportugues.content.VERB_TENSES
import com.ktouchie.quizportugues.content.tenseLabel
import com.ktouchie.quizportugues.ui.common.CheckboxRow
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Advanced setup for Verb Conjugation (docs/MOBILE_APP_SPEC.md §8, GitHub #28): pick tenses and a
 * difficulty filter, then start an uncapped session over just that selection — unlike Quick
 * Practice, not restricted to unlocked CEFR tiers.
 */
@Composable
fun VerbSetupScreen(
    onStart: (selectedTenses: List<String>, difficulty: Difficulty?) -> Unit,
    viewModel: VerbSetupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Avançado · Verbos", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Selecione os tempos e o nível de dificuldade para praticar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "Selecionar tudo",
                style = MaterialTheme.typography.labelLarge,
                color = ExtendedTheme.colors.textWarm,
                modifier = Modifier.clickable(onClick = viewModel::selectAllTenses),
            )
            Text(
                text = "Limpar",
                style = MaterialTheme.typography.labelLarge,
                color = ExtendedTheme.colors.textWarm,
                modifier = Modifier.clickable(onClick = viewModel::clearTenses),
            )
        }

        Column {
            VERB_TENSES.forEach { tense ->
                CheckboxRow(
                    label = tenseLabel(tense).replaceFirstChar { it.uppercase() },
                    checked = tense in state.selectedTenses,
                    onToggle = { viewModel.toggleTense(tense) },
                )
            }
        }

        Text("Nível de dificuldade", style = MaterialTheme.typography.titleMedium)
        Column {
            DifficultyOption("Todos", state.difficulty == null) { viewModel.setDifficulty(null) }
            DifficultyOption("Iniciante", state.difficulty == Difficulty.BEGINNER) { viewModel.setDifficulty(Difficulty.BEGINNER) }
            DifficultyOption("Intermédio", state.difficulty == Difficulty.INTERMEDIATE) { viewModel.setDifficulty(Difficulty.INTERMEDIATE) }
            DifficultyOption("Avançado", state.difficulty == Difficulty.ADVANCED) { viewModel.setDifficulty(Difficulty.ADVANCED) }
        }

        if (!state.loading) {
            Text(
                text = "${state.matchingCount} itens · ${state.dueCount} por rever",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { onStart(state.selectedTenses.toList(), state.difficulty) },
            enabled = state.selectedTenses.isNotEmpty(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Iniciar Quiz")
        }
    }
}

@Composable
private fun DifficultyOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
