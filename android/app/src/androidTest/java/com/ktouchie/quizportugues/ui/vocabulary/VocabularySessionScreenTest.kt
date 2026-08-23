package com.ktouchie.quizportugues.ui.vocabulary

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
 * Smoke test for the Quick Practice happy path: a question renders with 4 tappable options, and
 * tapping one shows feedback plus a "Continuar" button. Full session-completion coverage (results
 * screen, retry-mistakes) is broader than a single smoke test should carry — see the
 * Testing & CI epic for dedicated coverage.
 */
@RunWith(AndroidJUnit4::class)
class VocabularySessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun answeringAQuestionShowsFeedbackAndAContinueButton() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                VocabularySessionScreen(onDone = {})
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size >= 4
        }

        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        composeTestRule.onNodeWithText("Continuar").assertExists()
    }
}
