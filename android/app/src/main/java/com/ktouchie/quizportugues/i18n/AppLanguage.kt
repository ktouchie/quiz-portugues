package com.ktouchie.quizportugues.i18n

/**
 * Interface language: instructions, labels and grammar explanations — never the Portuguese being
 * learnt. English is the default (issue #71); same setting as the web app's i18n.js.
 */
enum class AppLanguage { EN, PT }

/** A text shipped in both interface languages, e.g. a grammar hint. */
data class LocalizedText(val en: String, val pt: String) {
    fun get(language: AppLanguage): String = if (language == AppLanguage.PT) pt else en
}
