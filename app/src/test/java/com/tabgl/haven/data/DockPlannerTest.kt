package com.tabgl.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockPlannerTest {

    private fun app(label: String, pkg: String = "pkg.${label.lowercase()}") =
        LauncherApp(label, pkg, "Main")

    private val dialer = app("Phone", "com.android.dialer")
    private val messages = app("Messages", "com.google.messages")
    private val camera = app("Camera", "com.google.camera")
    private val browser = app("Chrome", "com.android.chrome")
    private val notes = app("Notes", "com.example.notes")

    private val defaults = listOf(dialer.packageName, messages.packageName, camera.packageName, browser.packageName)

    @Test
    fun defaultDockFollowsTheDefaultsInOrder() {
        val dock = DockPlanner.dock(listOf(browser, camera, messages, dialer), null, defaults, 4)
        assertEquals(listOf("Phone", "Messages", "Camera", "Chrome"), dock.map { it.label })
    }

    @Test
    fun defaultDockIsCappedBySlots() {
        val dock = DockPlanner.dock(listOf(dialer, messages, camera, browser), null, defaults, 3)
        assertEquals(3, dock.size)
    }

    @Test
    fun defaultDockTopsUpWithMostUsedApps() {
        val usage = mapOf(notes.key to 9)
        val dock = DockPlanner.dock(listOf(dialer, notes), null, listOf(dialer.packageName), 4, usage)
        assertEquals(listOf("Phone", "Notes"), dock.map { it.label })
    }

    @Test
    fun customDockIsHonouredExactly() {
        val dock = DockPlanner.dock(
            listOf(dialer, messages, camera, browser, notes),
            listOf(notes.key, browser.key),
            defaults,
            4
        )
        assertEquals(listOf("Notes", "Chrome"), dock.map { it.label })
    }

    @Test
    fun anEmptyCustomDockStaysEmpty() {
        assertTrue(DockPlanner.dock(listOf(dialer), emptyList(), defaults, 4).isEmpty())
    }

    @Test
    fun aFullyVanishedCustomDockFallsBackToDefaults() {
        val dock = DockPlanner.dock(
            listOf(dialer, messages),
            listOf("gone/Activity", "also.gone/Activity"),
            defaults,
            4
        )
        assertEquals(listOf("Phone", "Messages"), dock.map { it.label })
    }

    @Test
    fun frequentSkipsDockAppsAndZeroUse() {
        val usage = mapOf(notes.key to 5, camera.key to 2, dialer.key to 40)
        val frequent = DockPlanner.frequent(
            visible = listOf(dialer, notes, camera),
            usage = usage,
            excluded = setOf(dialer.key),
            count = 4
        )
        assertEquals(listOf("Notes", "Camera"), frequent.map { it.label })
    }

    @Test
    fun frequentHonoursItsCount() {
        val usage = mapOf(notes.key to 5, camera.key to 2, messages.key to 1)
        val frequent = DockPlanner.frequent(
            visible = listOf(notes, camera, messages),
            usage = usage,
            excluded = emptySet(),
            count = 2
        )
        assertEquals(2, frequent.size)
    }

    @Test
    fun frequentIsEmptyWhenTurnedOff() {
        val usage = mapOf(notes.key to 5)
        assertTrue(DockPlanner.frequent(listOf(notes), usage, emptySet(), 0).isEmpty())
    }
}
