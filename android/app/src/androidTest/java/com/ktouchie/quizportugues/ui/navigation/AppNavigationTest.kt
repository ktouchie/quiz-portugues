package com.ktouchie.quizportugues.ui.navigation

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

        // The real VerbSetupScreen (docs/MOBILE_APP_SPEC.md §8, GitHub #28) replaced the
        // placeholder here — its own "Iniciar Quiz" CTA confirms real navigation happened.
        composeTestRule.onNodeWithText("Iniciar Quiz").assertExists()
    }

    @Test
    fun startingAnAdvancedVerbSessionNavigatesToSession() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Conjugação de Verbos").performClick()
        composeTestRule.onNodeWithText("Avançado").performClick()
        // All tenses are selected by default (VerbSetupViewModel), so "Iniciar Quiz" is already
        // enabled — starts an Advanced session scoped to that (default: every tense) selection.
        composeTestRule.onNodeWithText("Iniciar Quiz").performClick()

        // Confirms real navigation off Setup (not just the same screen re-rendering) — Setup's
        // own CTA is gone once the Session route takes over.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Iniciar Quiz").fetchSemanticsNodes().isEmpty()
        }
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
