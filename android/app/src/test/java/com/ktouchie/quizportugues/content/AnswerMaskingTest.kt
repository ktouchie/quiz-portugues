package com.ktouchie.quizportugues.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class AnswerMaskingTest {

    @Test
    fun `blanks the answer as a whole word, ignoring case`() {
        assertEquals("O livro é ___ professor.", maskAnswer("O livro é do professor.", "do"))
        assertEquals("___ outro lado, dou-lhe isto.", maskAnswer("Do outro lado, dou-lhe isto.", "do"))
        assertEquals("Vou ___ café.", maskAnswer("Vou àquele café.", "àquele"))
    }

    @Test
    fun `no contraction example still shows its answer`() {
        for (entry in parseContractionEntries(File("../../contractions.json").readText())) {
            val example = entry.example ?: continue
            val masked = maskAnswer(example, entry.answer)
            assertFalse("\"$masked\" still shows ${entry.answer}", Regex("""(^|[^\p{L}])${Regex.escape(entry.answer)}(?!\p{L})""", RegexOption.IGNORE_CASE).containsMatchIn(masked))
        }
    }
}
