package com.ktouchie.quizportugues.ui.navigation

/**
 * Top-level navigation graph (docs/MOBILE_APP_SPEC.md §4):
 * Home -> Module Home (Verbs/Vocabulary) -> Setup (Advanced) -> Session -> Results.
 *
 * `moduleId` matches the string module identifiers used throughout the content/srs/data layers
 * ("verbs", "vocabulary" — see content/QuizItem.kt's `QuizItem.module`).
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")

    data object ModuleHome : Destination("module/{moduleId}") {
        fun route(moduleId: String) = "module/$moduleId"
    }

    data object Setup : Destination("module/{moduleId}/setup") {
        fun route(moduleId: String) = "module/$moduleId/setup"
    }

    /**
     * `tenses`/`difficulty` (Verb Conjugation) and `categories` (Vocabulary) are optional query
     * args carrying an Advanced-mode selection from [Destination.Setup] — see
     * `VerbSessionViewModel`/`VocabularySessionViewModel`. Absent entirely for Quick Practice,
     * launched directly from Module Home via [route].
     */
    data object Session : Destination("module/{moduleId}/session?tenses={tenses}&difficulty={difficulty}&categories={categories}") {
        fun route(moduleId: String) = "module/$moduleId/session"
    }

    data object Results : Destination("module/{moduleId}/results") {
        fun route(moduleId: String) = "module/$moduleId/results"
    }
}

const val MODULE_VERBS = "verbs"
const val MODULE_VOCABULARY = "vocabulary"
const val MODULE_GENDER = "gender"
const val MODULE_SER_ESTAR_FICAR = "ser_estar_ficar"
const val MODULE_CONTRACTIONS = "contractions"

/** Display name shown on Home's module cards and each Module Home screen's title. */
fun moduleDisplayName(moduleId: String): String = when (moduleId) {
    MODULE_VERBS -> "Conjugação de Verbos"
    MODULE_VOCABULARY -> "Vocabulário"
    MODULE_GENDER -> "Género & Plural"
    MODULE_SER_ESTAR_FICAR -> "Ser, Estar & Ficar"
    MODULE_CONTRACTIONS -> "Contrações"
    else -> moduleId
}

/** Emoji used as the module's icon on Home's module cards. */
fun moduleIcon(moduleId: String): String = when (moduleId) {
    MODULE_VERBS -> "🗣️"
    MODULE_VOCABULARY -> "📚"
    MODULE_GENDER -> "👫"
    MODULE_SER_ESTAR_FICAR -> "🧩"
    MODULE_CONTRACTIONS -> "🔗"
    else -> "📘"
}
