package com.ktouchie.quizportugues

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app and the website share one version number, the repo-root version.txt. Gradle unit tests
 * run with android/app as the working directory, so the repo root is two levels up.
 */
class AppVersionTest {

    private val versionTxt = File("../../version.txt").readText().trim()

    @Test
    fun `versionName is the shared version txt`() {
        assertEquals(versionTxt, BuildConfig.VERSION_NAME)
    }

    @Test
    fun `versionCode encodes major, minor and patch so newer versions install as updates`() {
        val (major, minor, patch) = versionTxt.split(".").map { it.toInt() }
        assertEquals(major * 1_000_000 + minor * 1_000 + patch, BuildConfig.VERSION_CODE)
    }
}
