package com.ktouchie.quizportugues.ui.contractions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.ui.common.CheckboxRow
import com.ktouchie.quizportugues.ui.i18n.LocalStrings
import com.ktouchie.quizportugues.ui.navigation.MODULE_CONTRACTIONS
import com.ktouchie.quizportugues.ui.navigation.moduleDisplayName
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Advanced setup for Contractions (docs/MOBILE_APP_SPEC.md §8, GitHub #53): pick categories, then
 * start an uncapped session over just that selection — unlike Quick Practice, not restricted to
 * unlocked CEFR tiers.
 */
@Composable
fun ContractionsSetupScreen(
    onStart: (selectedCategories: List<String>) -> Unit,
    viewModel: ContractionsSetupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(LocalStrings.current.advancedTitle(moduleDisplayName(MODULE_CONTRACTIONS, LocalStrings.current)), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = LocalStrings.current.categoriesSetupHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = LocalStrings.current.selectAll,
                style = MaterialTheme.typography.labelLarge,
                color = ExtendedTheme.colors.textWarm,
                modifier = Modifier.clickable(onClick = viewModel::selectAllCategories),
            )
            Text(
                text = LocalStrings.current.clear,
                style = MaterialTheme.typography.labelLarge,
                color = ExtendedTheme.colors.textWarm,
                modifier = Modifier.clickable(onClick = viewModel::clearCategories),
            )
        }

        Column {
            state.categories.forEach { category ->
                CheckboxRow(
                    label = LocalStrings.current.categoryName(category),
                    checked = category in state.selectedCategories,
                    onToggle = { viewModel.toggleCategory(category) },
                )
            }
        }

        if (!state.loading) {
            Text(
                text = LocalStrings.current.matching(state.matchingCount, state.dueCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { onStart(state.selectedCategories.toList()) },
            enabled = state.selectedCategories.isNotEmpty(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(LocalStrings.current.startQuiz)
        }
    }
}
