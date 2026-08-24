package com.ktouchie.quizportugues.ui.module

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.navigation.moduleDisplayName
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Module home: "Quick Practice" primary CTA + "Advanced" entry point (docs/MOBILE_APP_SPEC.md
 * §8/§11), plus an explicit way back to the app's Home screen — previously the only way back was
 * the phone's own back gesture/button, since finishing a session lands here (not on Home) and
 * nothing on this screen pointed further back. Placeholder for Setup — the real Advanced setup
 * screen is a separate task; this proves the navigation graph's Home -> Module Home -> Setup/
 * Session hop works for both modules.
 */
@Composable
fun ModuleHomeScreen(
    moduleId: String,
    onStartQuickPractice: () -> Unit,
    onOpenAdvancedSetup: () -> Unit,
    onBackToHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onBackToHome),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("←", style = MaterialTheme.typography.titleMedium, color = ExtendedTheme.colors.textWarm)
            Text("Início", style = MaterialTheme.typography.titleMedium, color = ExtendedTheme.colors.textWarm)
        }
        Text(text = moduleDisplayName(moduleId), style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = onStartQuickPractice,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Prática Rápida")
        }
        OutlinedButton(
            onClick = onOpenAdvancedSetup,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExtendedTheme.colors.textWarm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Avançado")
        }
    }
}
