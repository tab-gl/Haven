package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {

    @Test
    fun defaultsToABalancedSpring() {
        val spec = motionSpecFor(LauncherSettings())
        assertTrue(spec.enabled)
        assertEquals(430f, spec.stiffness, 0.01f)
        assertEquals(0.88f, spec.damping, 0.001f)
    }

    @Test
    fun masterSwitchTurnsMotionOff() {
        val spec = motionSpecFor(LauncherSettings(animations = false))
        assertFalse(spec.enabled)
        assertEquals(0f, spec.stiffness, 0.001f)
    }

    @Test
    fun speedChangesStiffness() {
        val snappy = motionSpecFor(LauncherSettings(motionSpeed = "fast"))
        val balanced = motionSpecFor(LauncherSettings(motionSpeed = "normal"))
        val relaxed = motionSpecFor(LauncherSettings(motionSpeed = "calm"))
        assertTrue(snappy.stiffness > balanced.stiffness)
        assertTrue(balanced.stiffness > relaxed.stiffness)
        // Faster springs need more damping to settle without overshoot.
        assertTrue(snappy.damping > relaxed.damping)
    }

    @Test
    fun unknownSpeedFallsBackToBalanced() {
        assertEquals(
            motionSpecFor(LauncherSettings(motionSpeed = "normal")).stiffness,
            motionSpecFor(LauncherSettings(motionSpeed = "warp")).stiffness,
            0.01f
        )
    }

    @Test
    fun everySpringIsDampedEnoughToNotWobble() {
        for (speed in listOf("fast", "normal", "calm")) {
            val spec = motionSpecFor(LauncherSettings(motionSpeed = speed))
            assertTrue("$speed should not overshoot", spec.damping >= 0.8f)
        }
    }

    @Test
    fun thePressIsMuchSnappierThanTheDrawer() {
        val panel = motionSpecFor(LauncherSettings())
        assertTrue(MotionSpec.PRESS_STIFFNESS > panel.stiffness * 2f)
        assertTrue(MotionSpec.PRESS_SCALE < 1f)
    }
}
