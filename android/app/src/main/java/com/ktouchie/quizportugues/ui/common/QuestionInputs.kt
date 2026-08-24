package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Shared question-input widgets used by both the Verb Conjugation and Vocabulary Quick Practice
 * sessions (docs/MOBILE_APP_SPEC.md §9) — each question renders one or the other depending on
 * [com.ktouchie.quizportugues.content.QuestionModality], decided per item from its typing
 * readiness rather than fixed per module.
 */

/** One tappable option per choice; the caller supplies already-shuffled [options]. */
@Composable
fun MultipleChoiceOptions(
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Button(
                onClick = { onSelect(option) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(option)
            }
        }
    }
}

/**
 * Free-text input relying on the device keyboard's own accent support (long-press a base letter
 * for its accented variants) instead of a custom in-app accent bar — the standard Gboard/AOSP
 * keyboard already offers this for every letter used in Portuguese, so a bespoke picker was
 * redundant chrome.
 */
@Composable
fun TypedAnswerInput(enabled: Boolean, onSubmit: (String) -> Unit) {
    var answerText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = answerText,
            onValueChange = { answerText = it },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit(answerText) },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Mantém premida uma letra no teclado para ver os acentos",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSubmit(answerText) }, enabled = enabled) {
                Text("Responder")
            }
        }
    }
}
