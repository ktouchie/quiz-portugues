package com.ktouchie.quizportugues.ui.i18n

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.i18n.AppLanguage
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/** The small English / Português switch shown at the bottom of every screen. */
@Composable
fun LanguageSwitch(language: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringsFor(language).languageLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguageOption("English", language == AppLanguage.EN) { onSelect(AppLanguage.EN) }
        LanguageOption("Português", language == AppLanguage.PT) { onSelect(AppLanguage.PT) }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) ExtendedTheme.colors.textWarm else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
