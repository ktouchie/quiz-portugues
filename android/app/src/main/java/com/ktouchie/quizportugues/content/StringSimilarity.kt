package com.ktouchie.quizportugues.content

/**
 * Classic edit-distance-based similarity, used to rank multiple-choice distractors by how
 * genuinely confusable they are with the correct answer (docs/MOBILE_APP_SPEC.md §9) — rather
 * than a same-category-random pick, which can land on options obviously unrelated to the correct
 * one. Case-insensitive; not accent-stripped (an accent is itself part of what makes two Portuguese
 * words distinguishable, so ignoring it would understate similarity between genuinely different
 * words, e.g. "e"/"é").
 *
 * @return 1.0 for identical strings (case-insensitively), trending toward 0.0 as they diverge.
 */
fun stringSimilarity(a: String, b: String): Double {
    val lowerA = a.lowercase()
    val lowerB = b.lowercase()
    if (lowerA.isEmpty() && lowerB.isEmpty()) return 1.0
    val maxLen = maxOf(lowerA.length, lowerB.length)
    if (maxLen == 0) return 1.0
    return 1.0 - levenshteinDistance(lowerA, lowerB).toDouble() / maxLen
}

/** Standard Wagner-Fischer dynamic-programming edit distance (insert/delete/substitute, unit cost). */
private fun levenshteinDistance(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
    }
    return dp[a.length][b.length]
}
