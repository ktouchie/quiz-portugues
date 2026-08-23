package com.ktouchie.quizportugues.ui.navigation

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingAModuleOnHomeNavigatesToItsModuleHomeScreen() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Vocabulário").performClick()

        composeTestRule.onNodeWithText(MODULE_VOCABULARY).assertExists()
    }

    @Test
    fun tappingAdvancedOnModuleHomeNavigatesToSetup() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Conjugação de Verbos").performClick()
        composeTestRule.onNodeWithText("Avançado").performClick()

        composeTestRule.onNodeWithText("Setup ($MODULE_VERBS)").assertExists()
    }
}
