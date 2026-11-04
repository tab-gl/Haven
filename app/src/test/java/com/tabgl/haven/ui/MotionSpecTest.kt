package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {

    @Test
    fun defaultsToThePixelFeel() {
        val spec = motionSpecFor(LauncherSettings())
        assertTrue(spec.enabled)
        assertEquals(MotionSpec.ZOOM, spec.mode)
        assertEquals(430f, spec.stiffness, 0.01f)
        assertEquals(0.90f, spec.lift, 0.001f)
    }

    @Test
    fun masterSwitchTurnsMotionOff() {
        val spec = motionSpecFor(LauncherSettings(animations = false))
        assertFalse(spec.enabled)
        assertEquals(MotionSpec.NONE, spec.mode)
        assertEquals(0f, spec.lift, 0.001f)
    }

    @Test
    fun styleNoneTurnsMotionOff() {
        assertFalse(motionSpecFor(LauncherSettings(motionStyle = "none")).enabled)
    }

    @Test
    fun fadeStyleIsKept() {
        val spec = motionSpecFor(LauncherSettings(motionStyle = "fade"))
        assertTrue(spec.enabled)
        assertEquals(MotionSpec.FADE, spec.mode)
    }

    @Test
    fun unknownStyleFallsBackToZoom() {
        assertEquals(MotionSpec.ZOOM, motionSpecFor(LauncherSettings(motionStyle = "wobble")).mode)
    }

    @Test
    fun speedChangesStiffness() {
        val fast = motionSpecFor(LauncherSettings(motionSpeed = "fast"))
        val normal = motionSpecFor(LauncherSettings(motionSpeed = "normal"))
        val calm = motionSpecFor(LauncherSettings(motionSpeed = "calm"))
        assertTrue(fast.stiffness > normal.stiffness)
        assertTrue(normal.stiffness > calm.stiffness)
    }

    @Test
    fun oneUiIsCrisperThanPixel() {
        val pixel = motionSpecFor(LauncherSettings(motionFeel = "pixel"))
        val oneUi = motionSpecFor(LauncherSettings(motionFeel = "oneui"))
        // Stiffer, less bounce, smaller scale, more fade, less icon growth.
        assertTrue(oneUi.stiffness > pixel.stiffness)
        assertTrue(oneUi.closeDamping > pixel.closeDamping)
        assertTrue(oneUi.zoomGain < pixel.zoomGain)
        assertTrue(oneUi.fade > pixel.fade)
        assertTrue(oneUi.lift < pixel.lift)
        assertTrue(oneUi.launchAt < pixel.launchAt)
    }

    @Test
    fun classicIsSofterThanPixel() {
        val pixel = motionSpecFor(LauncherSettings(motionFeel = "pixel"))
        val classic = motionSpecFor(LauncherSettings(motionFeel = "classic"))
        assertTrue(classic.stiffness < pixel.stiffness)
        assertTrue(classic.fade > pixel.fade)
    }

    @Test
    fun unknownFeelFallsBackToPixel() {
        assertEquals(
            motionSpecFor(LauncherSettings(motionFeel = "pixel")).stiffness,
            motionSpecFor(LauncherSettings(motionFeel = "nonsense")).stiffness,
            0.01f
        )
    }

    @Test
    fun fadeIsNeverTotalSoTheReturnDoesNotCutFromBlack() {
        LauncherSettings(motionFeel = "oneui").let { settings ->
            assertTrue(motionSpecFor(settings).fade < 1f)
        }
    }
}
