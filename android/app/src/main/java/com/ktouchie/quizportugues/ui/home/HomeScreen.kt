package com.ktouchie.quizportugues.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.navigation.MODULE_VERBS
import com.ktouchie.quizportugues.ui.navigation.MODULE_VOCABULARY

/**
 * App home: streak, mastered count, and module entry points. Placeholder for now — the real
 * streak/mastered-count display (wired to [com.ktouchie.quizportugues.data.GamificationRepository])
 * is a separate task; this just proves the navigation graph reaches both modules.
 */
@Composable
fun HomeScreen(onOpenModule: (moduleId: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Português Europeu")
        Button(onClick = { onOpenModule(MODULE_VERBS) }) {
            Text("Conjugação de Verbos")
        }
        Button(onClick = { onOpenModule(MODULE_VOCABULARY) }) {
            Text("Vocabulário")
        }
    }
}
