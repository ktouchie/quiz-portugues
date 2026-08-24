package com.ktouchie.quizportugues.ui.verbs

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
 * Smoke test for the default (no SRS history yet) path: every item starts multiple-choice —
 * docs/MOBILE_APP_SPEC.md §9 — so a fresh session renders tappable options like Vocabulary's,
 * not a typed field. Coverage for the typed path (an item that's crossed its typing-readiness
 * threshold) needs a way to seed Room state ahead of screen launch, which the ViewModel doesn't
 * expose yet — see the Testing & CI epic for that follow-up.
 */
@RunWith(AndroidJUnit4::class)
class VerbSessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun answeringAQuestionShowsFeedbackAndAContinueButton() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                VerbSessionScreen(onDone = {})
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size >= 4
        }

        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        composeTestRule.onNodeWithText("Continuar").assertExists()
    }
}
