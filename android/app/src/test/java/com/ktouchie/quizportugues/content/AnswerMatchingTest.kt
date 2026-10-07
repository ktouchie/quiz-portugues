package com.ktouchie.quizportugues.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMatchingTest {

    @Test
    fun `matches ignoring case and surrounding whitespace`() {
        assertTrue(answersMatch("  Como  ".trim(), "como"))
        assertTrue(answersMatch("COMO", "como"))
    }

    @Test
    fun `matches regardless of NFC vs NFD accent encoding`() {
        // Built from explicit code points, not typed glyphs, so there's no ambiguity about which
        // encoding each string actually uses.
        val eAcutePrecomposed = 0x00E9.toChar().toString() // single codepoint: LATIN SMALL LETTER E WITH ACUTE
        val eAcuteDecomposed = "e" + 0x0301.toChar() // "e" + COMBINING ACUTE ACCENT

        val nfc = "com" + eAcutePrecomposed + "rcio"
        val nfd = "com" + eAcuteDecomposed + "rcio"

        assertTrue(answersMatch(nfd, nfc))
    }

    @Test
    fun `does not match a genuinely different answer`() {
        assertFalse(answersMatch("comi", "como"))
    }
}
