package com.ktouchie.quizportugues.ui.navigation

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

        composeTestRule.onNodeWithText("Vocabulary").performClick()

        // ModuleHomeScreen shows the display name ("Vocabulary"), not the raw module id
        // ("vocabulary") — asserting the button used to get here still exists, now as this
        // screen's title, plus its own "Quick Practice" CTA, confirms real navigation happened
        // rather than the same Home screen just still being on top.
        composeTestRule.onNodeWithText("Quick Practice").assertExists()
    }

    @Test
    fun tappingAdvancedOnModuleHomeNavigatesToSetup() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Verb Conjugation").performClick()
        composeTestRule.onNodeWithText("Advanced").performClick()

        // The real VerbSetupScreen (docs/MOBILE_APP_SPEC.md §8, GitHub #28) replaced the
        // placeholder here — its own "Start quiz" CTA confirms real navigation happened.
        composeTestRule.onNodeWithText("Start quiz").assertExists()
    }

    @Test
    fun startingAnAdvancedVerbSessionNavigatesToSession() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Verb Conjugation").performClick()
        composeTestRule.onNodeWithText("Advanced").performClick()
        // All tenses are selected by default (VerbSetupViewModel), so "Start quiz" is already
        // enabled — starts an Advanced session scoped to that (default: every tense) selection.
        composeTestRule.onNodeWithText("Start quiz").performClick()

        // Confirms real navigation off Setup (not just the same screen re-rendering) — Setup's
        // own CTA is gone once the Session route takes over.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Start quiz").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun tappingInicioOnModuleHomeReturnsToHome() {
        composeTestRule.setContent {
            QuizPortuguesTheme {
                AppNavigation()
            }
        }

        composeTestRule.onNodeWithText("Verb Conjugation").performClick()
        composeTestRule.onNodeWithText("Home").performClick()

        // Home-only content (the greeting) confirms we're actually back, not just that the
        // ModuleHomeScreen's "Início" row itself still renders.
        composeTestRule.onNodeWithText("Hello! 👋").assertExists()
    }
}
