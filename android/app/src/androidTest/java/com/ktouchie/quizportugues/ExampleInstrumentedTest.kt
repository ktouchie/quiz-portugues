package com.ktouchie.quizportugues

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Placeholder to confirm the instrumented test source set is wired up correctly. Real Compose UI
 * tests (Quick Practice flow, results screen, etc.) land in a separate task.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ktouchie.quizportugues", appContext.packageName)
    }
}
