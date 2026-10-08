package com.ktouchie.quizportugues.content

/**
 * A sentence with the answer blanked out ("O livro é ___ professor."), so an example shown with
 * the question doesn't give the answer away. Whole words only, ignoring case; same as the web
 * app's maskAnswer in practice.js.
 */
fun maskAnswer(sentence: String, answer: String): String =
    Regex("""(^|[^\p{L}])""" + Regex.escape(answer) + """(?!\p{L})""", RegexOption.IGNORE_CASE)
        .replace(sentence) { it.groupValues[1] + "___" }
