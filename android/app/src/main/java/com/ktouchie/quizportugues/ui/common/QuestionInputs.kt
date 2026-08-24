package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme
import com.ktouchie.quizportugues.ui.theme.OnAccent

/**
 * Shared question-input widgets used by both the Verb Conjugation and Vocabulary Quick Practice
 * sessions (docs/MOBILE_APP_SPEC.md §9/§11) — each question renders one or the other depending on
 * [com.ktouchie.quizportugues.content.QuestionModality], decided per item from its typing
 * readiness rather than fixed per module. Styled for "Direction A — Warm Encourager": lettered
 * badge cards for multiple-choice, the correct one highlighted green once answered; a warm-bordered
 * text field with a full-width gradient submit button for typed input.
 */

/** One tappable, lettered option per choice; the caller supplies already-shuffled [options]. Once
 *  [revealAnswer] is true (an answer has been given), the option matching [correctAnswer] is
 *  highlighted — mirrors the mockup's "correct option turns green" state. */
@Composable
fun MultipleChoiceOptions(
    options: List<String>,
    correctAnswer: String,
    revealAnswer: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, option ->
            OptionRow(
                badge = ('A' + index).toString(),
                text = option,
                highlighted = revealAnswer && option == correctAnswer,
                enabled = enabled,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun OptionRow(
    badge: String,
    text: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val borderColor = if (highlighted) ExtendedTheme.colors.correct else MaterialTheme.colorScheme.outline
    val backgroundColor = if (highlighted) {
        ExtendedTheme.colors.correct.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (highlighted) ExtendedTheme.colors.correct else MaterialTheme.colorScheme.onSurface
    val badgeBg = if (highlighted) ExtendedTheme.colors.correct else MaterialTheme.colorScheme.surfaceVariant
    val badgeText = if (highlighted) OnAccent else ExtendedTheme.colors.textWarm

    var rowModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(backgroundColor)
        .border(2.dp, borderColor, shape)
    if (enabled) {
        rowModifier = rowModifier.clickable(onClick = onClick)
    }

    Row(
        modifier = rowModifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(badgeBg, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeText)
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor)
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = answerText,
            onValueChange = { answerText = it },
            enabled = enabled,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ExtendedTheme.colors.warmAccentEnd,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
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
            text = "💡 Mantém premida uma letra no teclado para ver os acentos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WarmGradientButton(
            text = "Responder",
            onClick = { onSubmit(answerText) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
