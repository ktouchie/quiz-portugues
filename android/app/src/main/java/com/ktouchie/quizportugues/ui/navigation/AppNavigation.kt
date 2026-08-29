package com.ktouchie.quizportugues.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ktouchie.quizportugues.ui.contractions.ContractionsSessionScreen
import com.ktouchie.quizportugues.ui.contractions.ContractionsSetupScreen
import com.ktouchie.quizportugues.ui.gender.GenderSessionScreen
import com.ktouchie.quizportugues.ui.gender.GenderSetupScreen
import com.ktouchie.quizportugues.ui.home.HomeScreen
import com.ktouchie.quizportugues.ui.module.ModuleHomeScreen
import com.ktouchie.quizportugues.ui.serestarficar.SerEstarFicarSessionScreen
import com.ktouchie.quizportugues.ui.serestarficar.SerEstarFicarSetupScreen
import com.ktouchie.quizportugues.ui.verbs.VerbSessionScreen
import com.ktouchie.quizportugues.ui.verbs.VerbSetupScreen
import com.ktouchie.quizportugues.ui.vocabulary.VocabularySessionScreen
import com.ktouchie.quizportugues.ui.vocabulary.VocabularySetupScreen

private val moduleIdArg: List<NamedNavArgument> = listOf(
    navArgument("moduleId") { type = NavType.StringType },
)

private val sessionArgs: List<NamedNavArgument> = moduleIdArg + listOf(
    navArgument("tenses") { type = NavType.StringType; nullable = true; defaultValue = null },
    navArgument("difficulty") { type = NavType.StringType; nullable = true; defaultValue = null },
    navArgument("categories") { type = NavType.StringType; nullable = true; defaultValue = null },
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destination.Home.route) {
        composable(Destination.Home.route) {
            HomeScreen(
                onOpenModule = { moduleId ->
                    navController.navigate(Destination.ModuleHome.route(moduleId))
                },
            )
        }

        composable(Destination.ModuleHome.route, arguments = moduleIdArg) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId").orEmpty()
            ModuleHomeScreen(
                moduleId = moduleId,
                onStartQuickPractice = {
                    navController.navigate(Destination.Session.route(moduleId))
                },
                onOpenAdvancedSetup = {
                    navController.navigate(Destination.Setup.route(moduleId))
                },
                onBackToHome = {
                    navController.popBackStack(Destination.Home.route, inclusive = false)
                },
            )
        }

        composable(Destination.Setup.route, arguments = moduleIdArg) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId").orEmpty()
            when (moduleId) {
                MODULE_VERBS -> VerbSetupScreen(
                    onStart = { tenses, difficulty ->
                        val query = "tenses=${Uri.encode(tenses.joinToString(","))}" +
                            "&difficulty=${difficulty?.name.orEmpty()}"
                        navController.navigate("module/$moduleId/session?$query")
                    },
                )
                MODULE_VOCABULARY -> VocabularySetupScreen(
                    onStart = { categories ->
                        val query = "categories=${Uri.encode(categories.joinToString(","))}"
                        navController.navigate("module/$moduleId/session?$query")
                    },
                )
                MODULE_GENDER -> GenderSetupScreen(
                    onStart = { categories ->
                        val query = "categories=${Uri.encode(categories.joinToString(","))}"
                        navController.navigate("module/$moduleId/session?$query")
                    },
                )
                MODULE_SER_ESTAR_FICAR -> SerEstarFicarSetupScreen(
                    onStart = { categories ->
                        val query = "categories=${Uri.encode(categories.joinToString(","))}"
                        navController.navigate("module/$moduleId/session?$query")
                    },
                )
                MODULE_CONTRACTIONS -> ContractionsSetupScreen(
                    onStart = { categories ->
                        val query = "categories=${Uri.encode(categories.joinToString(","))}"
                        navController.navigate("module/$moduleId/session?$query")
                    },
                )
                else -> PlaceholderScreen(label = "Setup ($moduleId)")
            }
        }

        composable(Destination.Session.route, arguments = sessionArgs) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId").orEmpty()
            val onDone = {
                navController.popBackStack(Destination.ModuleHome.route(moduleId), inclusive = false)
                Unit
            }
            when (moduleId) {
                MODULE_VOCABULARY -> VocabularySessionScreen(onDone = onDone)
                MODULE_VERBS -> VerbSessionScreen(onDone = onDone)
                MODULE_GENDER -> GenderSessionScreen(onDone = onDone)
                MODULE_SER_ESTAR_FICAR -> SerEstarFicarSessionScreen(onDone = onDone)
                MODULE_CONTRACTIONS -> ContractionsSessionScreen(onDone = onDone)
                else -> PlaceholderScreen(label = "Session ($moduleId)")
            }
        }

        composable(Destination.Results.route, arguments = moduleIdArg) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId").orEmpty()
            PlaceholderScreen(label = "Results ($moduleId)")
        }
    }
}

/**
 * Stands in for Setup/Session/Results until the per-module screens (typed input for verbs,
 * multiple-choice for vocabulary — see docs/MOBILE_APP_SPEC.md §9) are built.
 */
@Composable
private fun PlaceholderScreen(label: String) {
    Text(text = label, modifier = Modifier.fillMaxSize().padding(24.dp))
}
