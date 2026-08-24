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

        // ModuleHomeScreen shows the display name ("Vocabulário"), not the raw module id
        // ("vocabulary") — asserting the button used to get here still exists, now as this
        // screen's title, plus its own "Prática Rápida" CTA, confirms real navigation happened
        // rather than the same Home screen just still being on top.
        composeTestRule.onNodeWithText("Prática Rápida").assertExists()
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

    @Test
    fun tappingInicioOnModuleHomeReturnsToHome() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Conjugação de Verbos").performClick()
        composeTestRule.onNodeWithText("Início").performClick()

        // Home-only content (the greeting) confirms we're actually back, not just that the
        // ModuleHomeScreen's "Início" row itself still renders.
        composeTestRule.onNodeWithText("Olá! 👋").assertExists()
    }
}
