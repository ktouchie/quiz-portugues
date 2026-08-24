package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme

/** A pill-shaped progress bar, amber-to-blue gradient fill — used for both a module's mastery
 *  progress (Home) and a session's question-count progress (session screens). */
@Composable
fun GradientProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(
                    Brush.horizontalGradient(
                        listOf(ExtendedTheme.colors.warmAccentStart, MaterialTheme.colorScheme.primary),
                    ),
                ),
        )
    }
}
