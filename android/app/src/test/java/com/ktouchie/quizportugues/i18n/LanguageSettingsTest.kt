package com.ktouchie.quizportugues.i18n

import com.ktouchie.quizportugues.ui.i18n.EnglishStrings
import com.ktouchie.quizportugues.ui.i18n.PortugueseStrings
import com.ktouchie.quizportugues.ui.i18n.CATEGORY_NAMES_EN
import com.ktouchie.quizportugues.ui.i18n.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LanguageSettingsTest {

    private class FakeStore(var saved: String? = null) : LanguageStore {
        override fun read(): String? = saved
        override fun write(value: String) {
            saved = value
        }
    }

    @Test
    fun `English is the default`() {
        assertEquals(AppLanguage.EN, LanguageSettings(FakeStore()).language.value)
    }

    @Test
    fun `a saved choice is remembered`() {
        assertEquals(AppLanguage.PT, LanguageSettings(FakeStore("PT")).language.value)
    }

    @Test
    fun `an unreadable saved value falls back to English`() {
        assertEquals(AppLanguage.EN, LanguageSettings(FakeStore("klingon")).language.value)
    }

    @Test
    fun `switching saves the choice and updates the flow`() {
        val store = FakeStore()
        val settings = LanguageSettings(store)
        settings.set(AppLanguage.PT)
        assertEquals(AppLanguage.PT, settings.language.value)
        assertEquals("PT", store.saved)
    }

    @Test
    fun `each language gets its own strings`() {
        assertSame(EnglishStrings, stringsFor(AppLanguage.EN))
        assertSame(PortugueseStrings, stringsFor(AppLanguage.PT))
        assertEquals("Nothing to review", EnglishStrings.nothingDue)
        assertEquals("Nada por rever", PortugueseStrings.nothingDue)
        assertEquals("1 mistake", EnglishStrings.mistakeCount(1))
        assertEquals("2 erros", PortugueseStrings.mistakeCount(2))
    }

    @Test
    fun `every vocabulary category has an English name, shown in English mode only`() {
        val categories = org.json.JSONObject(java.io.File("../../vocabulary.json").readText()).keys().asSequence().toList()
        for (category in categories) {
            assert(CATEGORY_NAMES_EN.containsKey(category)) { "No English name for $category" }
        }
        assertEquals("Colours", EnglishStrings.categoryName("Cores"))
        assertEquals("Cores", PortugueseStrings.categoryName("Cores"))
    }
}
