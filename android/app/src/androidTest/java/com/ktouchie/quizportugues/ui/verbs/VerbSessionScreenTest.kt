package com.ktouchie.quizportugues.ui.verbs

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: typing a (deliberately wrong) answer and submitting shows feedback with a
 * "Continuar" button. Mirrors VocabularySessionScreenTest's coverage for the typed-input module.
 */
@RunWith(AndroidJUnit4::class)
class VerbSessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun submittingAnAnswerShowsFeedbackAndAContinueButton() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                VerbSessionScreen(onDone = {})
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Responder").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("xyzxyz")
        composeTestRule.onNodeWithText("Responder").performClick()

        composeTestRule.onNodeWithText("Continuar").assertExists()
    }
}
