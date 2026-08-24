package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktouchie.quizportugues.ui.theme.OnAccent

/**
 * A module entry on the Home screen: icon, title, due count, mastery progress bar, CTA. The whole
 * card is one tap target (not a separate button nested inside it) — "Praticar" is a decorative
 * pill labeling that action, not an independent click target.
 */
@Composable
fun ModuleCard(
    icon: String,
    title: String,
    dueCount: Int,
    progressPct: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 22.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (dueCount > 0) "$dueCount por rever" else "Nada por rever",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            GradientProgressBar(
                progress = progressPct / 100f,
                modifier = Modifier.fillMaxWidth().height(7.dp),
            )
        }

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Praticar", style = MaterialTheme.typography.labelLarge, color = OnAccent)
        }
    }
}
