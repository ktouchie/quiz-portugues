package com.ktouchie.quizportugues.ui.gender

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test mirroring VerbSessionScreenTest: every item starts multiple-choice (no SRS history
 * yet), so a fresh session renders tappable options.
 */
@RunWith(AndroidJUnit4::class)
class GenderSessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun answeringAQuestionShowsFeedbackAndAContinueButton() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                GenderSessionScreen(onDone = {})
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size >= 4
        }

        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        composeTestRule.onNodeWithText("Continuar").assertExists()
    }
}
