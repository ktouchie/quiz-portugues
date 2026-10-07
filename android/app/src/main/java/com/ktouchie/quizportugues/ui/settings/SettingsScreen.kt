package com.ktouchie.quizportugues.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Settings screen. Today this is just the one thing manual testing actually needs: a way to wipe
 * all progress and start from zero without uninstalling the app (docs/MOBILE_APP_SPEC.md §9 —
 * the mastery-gated/checkpoint progression is hard to exercise repeatedly otherwise).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.justReset) {
        if (state.justReset) {
            showConfirmDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Definições", style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "Repor todo o progresso",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Apaga o histórico de repetição espaçada, as melhores pontuações, a sequência " +
                "de dias e os marcos de todos os módulos — útil para testar a app como se fosse a " +
                "primeira utilização. Esta ação não pode ser desfeita.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.justReset) {
            Text(
                text = "Progresso reposto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        OutlinedButton(
            onClick = { showConfirmDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Repor tudo")
        }

        Button(
            onClick = onBack,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Voltar")
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Repor todo o progresso?") },
            text = { Text("Todo o histórico de prática em todos os módulos será apagado permanentemente.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllProgress() }) {
                    Text("Repor", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
