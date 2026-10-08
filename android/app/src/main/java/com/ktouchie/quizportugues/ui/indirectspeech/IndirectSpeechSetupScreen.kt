package com.ktouchie.quizportugues.ui.indirectspeech

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ktouchie.quizportugues.ui.i18n.LocalStrings
import com.ktouchie.quizportugues.ui.navigation.MODULE_INDIRECT_SPEECH
import com.ktouchie.quizportugues.ui.navigation.moduleDisplayName

/**
 * Advanced setup for Indirect Speech (docs/MOBILE_APP_SPEC.md §8, GitHub #55). No category/tense
 * picker — indirect_speech.json has no categories to choose from — so this just shows the
 * item/due counts before starting an uncapped, unrestricted session over all 20 items.
 */
@Composable
fun IndirectSpeechSetupScreen(
    onStart: () -> Unit,
    viewModel: IndirectSpeechSetupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(LocalStrings.current.advancedTitle(moduleDisplayName(MODULE_INDIRECT_SPEECH, LocalStrings.current)), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = LocalStrings.current.allItemsSetupHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!state.loading) {
            Text(
                text = LocalStrings.current.matching(state.totalCount, state.dueCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onStart,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(LocalStrings.current.startQuiz)
        }
    }
}
