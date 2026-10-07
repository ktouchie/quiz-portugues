package com.ktouchie.quizportugues.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun resettingWipesProgressOnlyAfterConfirming() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                SettingsScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Repor tudo").performClick()

        // The dialog's own confirmation text — distinct from the screen's body copy — proves the
        // dialog actually opened rather than resetting immediately on the first tap.
        composeTestRule.onNodeWithText("Todo o histórico de prática em todos os módulos será apagado permanentemente.")
            .assertExists()

        composeTestRule.onNodeWithText("Repor", substring = false).performClick()

        composeTestRule.onNodeWithText("Progresso reposto.").assertExists()
    }
}
