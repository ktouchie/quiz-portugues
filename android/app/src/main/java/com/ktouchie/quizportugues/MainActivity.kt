package com.ktouchie.quizportugues

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ktouchie.quizportugues.i18n.LanguageSettings
import com.ktouchie.quizportugues.ui.i18n.LanguageSwitch
import com.ktouchie.quizportugues.ui.i18n.LocalAppLanguage
import com.ktouchie.quizportugues.ui.i18n.LocalStrings
import com.ktouchie.quizportugues.ui.i18n.stringsFor
import com.ktouchie.quizportugues.ui.navigation.AppNavigation
import com.ktouchie.quizportugues.ui.theme.QuizPortuguesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val languageSettings = LanguageSettings.get(this)
        setContent {
            val language by languageSettings.language.collectAsState()
            QuizPortuguesTheme {
                CompositionLocalProvider(
                    LocalAppLanguage provides language,
                    LocalStrings provides stringsFor(language),
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = { LanguageSwitch(language, onSelect = languageSettings::set) },
                    ) { innerPadding ->
                        Surface(modifier = Modifier.padding(innerPadding)) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
