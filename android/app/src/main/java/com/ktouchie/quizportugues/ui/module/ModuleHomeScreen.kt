package com.ktouchie.quizportugues.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Module home: "Quick Practice" primary CTA + "Advanced" entry point (docs/MOBILE_APP_SPEC.md
 * §8). Placeholder — the real Quick Practice session logic and per-module content are separate
 * tasks (Verb Conjugation / Vocabulary module epics); this just proves the navigation graph's
 * Home -> Module Home -> Setup/Session hop works for both modules.
 */
@Composable
fun ModuleHomeScreen(
    moduleId: String,
    onStartQuickPractice: () -> Unit,
    onOpenAdvancedSetup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = moduleId)
        Button(onClick = onStartQuickPractice) {
            Text("Prática Rápida")
        }
        Button(onClick = onOpenAdvancedSetup) {
            Text("Avançado")
        }
    }
}
