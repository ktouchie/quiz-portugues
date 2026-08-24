package com.ktouchie.quizportugues.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.navigation.moduleDisplayName
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/**
 * Module home: "Quick Practice" primary CTA + "Advanced" entry point (docs/MOBILE_APP_SPEC.md
 * §8/§11). Placeholder for Setup — the real Advanced setup screen is a separate task; this proves
 * the navigation graph's Home -> Module Home -> Setup/Session hop works for both modules.
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
