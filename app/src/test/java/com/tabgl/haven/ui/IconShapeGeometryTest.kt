package com.tabgl.haven.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the corner maths of the rounded icon shapes.
 *
 * Two corners share every edge of an icon, so their percentages may never add up to more than 100.
 * Compose does not clamp this, and an over-rounded outline degenerates — which is how 0.8.0 shipped
 * a "Pebble" of 46 / 56 / 42 / 58 that drew as a plain circle, and why the shape is now data that
 * can be checked instead of four numbers buried in a constructor call.
 */
class IconShapeGeometryTest {

    @Test
    fun everyRoundedShapeFitsInsideItsIcon() {
        for ((name, corners) in IconShapes.CORNERS) {
            assertTrue("$name corners do not fit: $corners", corners.fits())
        }
    }

    @Test
    fun thePebbleIsAsymmetricButValid() {
        val pebble = IconShapes.CORNERS.getValue("pebble")
        assertTrue("pebble should not be a circle", pebble.topStart != pebble.topEnd)
        assertTrue(pebble.fits())
    }

    @Test
    fun aCircleIsTheMaximumRounding() {
        val circle = IconShapes.CORNERS.getValue("circle")
        assertTrue(circle.fits())
        assertTrue(circle.topStart + circle.topEnd == 100)
    }

    @Test
    fun anOverRoundedShapeIsRejected() {
        // The exact numbers 0.8.0 shipped: 46 + 56 on the top edge.
        assertTrue(!IconCorners(46, 56, 42, 58).fits())
        // 46 + 60 on the left edge.
        assertTrue(!IconCorners(46, 50, 42, 60).fits())
        // A single corner past half the icon is never valid either.
        assertTrue(!IconCorners(60, 20, 20, 20).fits())
    }

    @Test
    fun everyAdvertisedShapeResolves() {
        val shapes = IconShapes.NAMES.map { (value, _) -> IconShapes.of(value) }
        assertTrue(shapes.size == IconShapes.NAMES.size)
    }
}
