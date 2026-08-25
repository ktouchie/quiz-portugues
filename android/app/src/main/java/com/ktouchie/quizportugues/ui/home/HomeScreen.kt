package com.ktouchie.quizportugues.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.ui.common.ModuleCard
import com.ktouchie.quizportugues.ui.common.StatChip
import com.ktouchie.quizportugues.ui.navigation.MODULE_GENDER
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY
import com.ktouchie.quizportugues.ui.navigation.moduleDisplayName
import com.ktouchie.quizportugues.ui.navigation.moduleIcon
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme
import com.ktouchie.quizportugues.ui.theme.OnAccent

/**
 * App home: streak, mastered count, and module entry points (docs/MOBILE_APP_SPEC.md §10/§11).
 * Styled for "Direction A — Warm Encourager": a gradient streak pill, stat chips, and per-module
 * cards showing due count + a mastery progress bar.
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Olá! 👋", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Continua a praticar hoje!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(ExtendedTheme.colors.warmGradient, MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "🔥 ${state.currentStreak}",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnAccent,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip(value = "${state.totalMastered}", label = "Dominadas", modifier = Modifier.weight(1f))
            StatChip(value = "${state.currentStreak}", label = "Dias seguidos", modifier = Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModuleCard(
                icon = moduleIcon(MODULE_VERBS),
                title = moduleDisplayName(MODULE_VERBS),
                dueCount = state.verbProgress.dueCount,
                progressPct = state.verbProgress.seenPct,
                onClick = { onOpenModule(MODULE_VERBS) },
            )
            ModuleCard(
                icon = moduleIcon(MODULE_VOCABULARY),
                title = moduleDisplayName(MODULE_VOCABULARY),
                dueCount = state.vocabularyProgress.dueCount,
                progressPct = state.vocabularyProgress.seenPct,
                onClick = { onOpenModule(MODULE_VOCABULARY) },
            )
            ModuleCard(
                icon = moduleIcon(MODULE_GENDER),
                title = moduleDisplayName(MODULE_GENDER),
                dueCount = state.genderProgress.dueCount,
                progressPct = state.genderProgress.seenPct,
                onClick = { onOpenModule(MODULE_GENDER) },
            )
        }
    }
}
