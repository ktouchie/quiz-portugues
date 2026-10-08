package com.ktouchie.quizportugues.i18n

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the chosen language is kept; an interface so the settings can be tested on the JVM. */
interface LanguageStore {
    fun read(): String?
    fun write(value: String)
}

/**
 * The chosen interface language, English unless the learner picked Portuguese. Kept in
 * SharedPreferences rather than Room: it's one setting, not learning data.
 */
class LanguageSettings(private val store: LanguageStore) {
    private val _language = MutableStateFlow(
        store.read()?.let { saved -> AppLanguage.entries.firstOrNull { it.name == saved } } ?: AppLanguage.EN,
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun set(language: AppLanguage) {
        store.write(language.name)
        _language.value = language
    }

    companion object {
        @Volatile private var instance: LanguageSettings? = null

        fun get(context: Context): LanguageSettings = instance ?: synchronized(this) {
            instance ?: LanguageSettings(SharedPreferencesLanguageStore(context.applicationContext)).also { instance = it }
        }
    }
}

private class SharedPreferencesLanguageStore(context: Context) : LanguageStore {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY, null)

    override fun write(value: String) {
        prefs.edit().putString(KEY, value).apply()
    }

    private companion object {
        const val KEY = "language"
    }
}
