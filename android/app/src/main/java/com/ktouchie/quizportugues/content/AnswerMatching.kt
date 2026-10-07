package com.ktouchie.quizportugues.content

import java.text.Normalizer

/**
 * Mirrors `answerMatches()` in the web app's practice.js: trim + lowercase + NFC-normalize both
 * sides before comparing. A bracketed note at the end of the correct answer is optional: it tells
 * two words apart when they're the prompt ("short (height)" is baixo, "short (length)" is curto),
 * but "short" alone is a right answer when translating either into English. Multiple-choice input
 * compares the selected option string directly, no normalization needed.
 */
fun answersMatch(userAnswer: String, correctAnswer: String): Boolean {
    val normalizedUser = normalize(userAnswer)
    return normalizedUser == normalize(correctAnswer) ||
        normalizedUser == normalize(correctAnswer.replace(TRAILING_NOTE, ""))
}

private val TRAILING_NOTE = Regex("""\s*\([^)]*\)\s*$""")

private fun normalize(text: String): String = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFC)
