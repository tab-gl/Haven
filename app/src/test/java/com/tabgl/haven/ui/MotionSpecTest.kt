package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {

    @Test
    fun defaultsToAReadyZoom() {
        val spec = motionSpecFor(LauncherSettings())
        assertTrue(spec.enabled)
        assertEquals(MotionSpec.ZOOM, spec.mode)
        assertEquals(380f, spec.stiffness, 0.01f)
    }

    @Test
    fun masterSwitchTurnsMotionOff() {
        val spec = motionSpecFor(LauncherSettings(animations = false))
        assertFalse(spec.enabled)
        assertEquals(MotionSpec.NONE, spec.mode)
    }

    @Test
    fun styleNoneTurnsMotionOff() {
        val spec = motionSpecFor(LauncherSettings(motionStyle = "none"))
        assertFalse(spec.enabled)
    }

    @Test
    fun fadeStyleIsKept() {
        val spec = motionSpecFor(LauncherSettings(motionStyle = "fade"))
        assertTrue(spec.enabled)
        assertEquals(MotionSpec.FADE, spec.mode)
    }

    @Test
    fun unknownStyleFallsBackToZoom() {
        val spec = motionSpecFor(LauncherSettings(motionStyle = "wobble"))
        assertEquals(MotionSpec.ZOOM, spec.mode)
    }

    @Test
    fun speedChangesStiffness() {
        val fast = motionSpecFor(LauncherSettings(motionSpeed = "fast"))
        val normal = motionSpecFor(LauncherSettings(motionSpeed = "normal"))
        val calm = motionSpecFor(LauncherSettings(motionSpeed = "calm"))
        assertTrue(fast.stiffness > normal.stiffness)
        assertTrue(normal.stiffness > calm.stiffness)
        assertTrue(calm.damping < fast.damping)
    }
}
