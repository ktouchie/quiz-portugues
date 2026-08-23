package com.ktouchie.quizportugues.content

import java.text.Normalizer

/**
 * Mirrors `quiz_base.js`'s `submitAnswer()` comparison exactly: trim + lowercase + NFC-normalize
 * both sides before comparing. Used for typed-answer modules (Verb Conjugation) — Vocabulary's
 * multiple-choice input compares selected option strings directly, no normalization needed.
 */
fun answersMatch(userAnswer: String, correctAnswer: String): Boolean {
    val normalizedUser = Normalizer.normalize(userAnswer.trim().lowercase(), Normalizer.Form.NFC)
    val normalizedCorrect = Normalizer.normalize(correctAnswer.lowercase(), Normalizer.Form.NFC)
    return normalizedUser == normalizedCorrect
}
