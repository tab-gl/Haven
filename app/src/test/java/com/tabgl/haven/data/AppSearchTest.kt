package com.tabgl.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    private fun app(label: String) = LauncherApp(label, "pkg.${label.replace(' ', '_').lowercase()}", "Main")

    private val apps = listOf(
        app("Calculator"), app("Calendar"), app("Chrome"), app("Google Maps"), app("YouTube Music")
    )

    @Test
    fun blankQueryReturnsEverythingUnchanged() {
        assertEquals(apps, AppSearch.rank(apps, "  "))
    }

    @Test
    fun prefixMatchesComeFirstAndAreAlphabetical() {
        assertEquals(listOf("Calculator", "Calendar"), AppSearch.rank(apps, "cal").map { it.label })
    }

    @Test
    fun wordPrefixBeatsSubstring() {
        val result = AppSearch.rank(apps, "maps").map { it.label }
        assertEquals(listOf("Google Maps"), result)
    }

    @Test
    fun initialsMatch() {
        assertEquals(listOf("Google Maps"), AppSearch.rank(apps, "gm").map { it.label })
        assertEquals(listOf("YouTube Music"), AppSearch.rank(apps, "ym").map { it.label })
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(AppSearch.rank(apps, "zzzz").isEmpty())
    }
}
