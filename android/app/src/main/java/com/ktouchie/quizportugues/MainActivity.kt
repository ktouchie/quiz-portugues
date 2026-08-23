package com.ktouchie.quizportugues

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme

/**
 * App entry point. Just a placeholder scaffold for now — navigation between the home screen and
 * the Verb Conjugation / Vocabulary modules is set up in a separate task.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizPortuguesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        PlaceholderHome()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderHome() {
    Text(text = "Olá, Português!")
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomePreview() {
    QuizPortuguesTheme {
        Surface {
            PlaceholderHome()
        }
    }
}
