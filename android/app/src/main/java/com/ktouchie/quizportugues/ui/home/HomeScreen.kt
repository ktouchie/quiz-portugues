package com.ktouchie.quizportugues.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY

/**
 * App home: streak, mastered count, and module entry points (docs/MOBILE_APP_SPEC.md §10).
 * Verb Conjugation isn't built yet (still a placeholder further down the nav graph) — its button
 * is here so the module list is visibly complete, not because that flow works end-to-end yet.
 */
@Composable
fun HomeScreen(
    onOpenModule: (moduleId: String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Português Europeu")
        Text(text = "Sequência: ${state.currentStreak} dia${if (state.currentStreak == 1) "" else "s"}")
        Text(text = "Itens dominados: ${state.totalMastered}")
        Button(onClick = { onOpenModule(MODULE_VERBS) }) {
            Text("Conjugação de Verbos")
        }
        Button(onClick = { onOpenModule(MODULE_VOCABULARY) }) {
            Text("Vocabulário")
        }
    }
}
