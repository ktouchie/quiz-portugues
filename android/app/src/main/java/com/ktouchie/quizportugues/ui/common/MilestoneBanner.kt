package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/** Celebratory banner shown on a results screen when a new mastery milestone is reached. */
@Composable
fun MilestoneBanner(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        ExtendedTheme.colors.warmAccentStart.copy(alpha = 0.35f),
                        ExtendedTheme.colors.warmAccentEnd.copy(alpha = 0.35f),
                    ),
                ),
                MaterialTheme.shapes.large,
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "🏅 Novo marco: $count itens dominados!",
            style = MaterialTheme.typography.titleMedium,
            color = ExtendedTheme.colors.textWarm,
            textAlign = TextAlign.Center,
        )
    }
}
