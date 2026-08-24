package com.ktouchie.quizportugues.ui.common

import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ktouchie.quizportugues.ui.theme.ExtendedTheme
import com.ktouchie.quizportugues.ui.theme.OnAccent

/**
 * Primary CTA styled with the warm amber gradient (Color.kt's "Direction A") instead of a flat
 * Material color — [ButtonDefaults] only takes a single container color, so this layers the
 * gradient on as a background behind a transparent [Button] instead of replacing it outright,
 * keeping Button's built-in disabled/ripple/touch-target handling.
 */
@Composable
fun WarmGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(ExtendedTheme.colors.warmGradient)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Text(text, color = OnAccent)
    }
}
