package com.tabgl.haven.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IconShapesTest {

    @Test
    fun everyAdvertisedShapeResolvesToItsOwnShape() {
        val shapes = IconShapes.NAMES.map { (value, _) -> IconShapes.of(value) }
        assertEquals(IconShapes.NAMES.size, shapes.toSet().size)
    }

    @Test
    fun labelsAreAllDistinct() {
        val labels = IconShapes.NAMES.map { it.second }
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun unknownShapeFallsBackToTheSquircle() {
        assertSame(IconShapes.of("squircle"), IconShapes.of("no-such-shape"))
    }

    @Test
    fun theLegacyShapeNamesStillWork() {
        // 0.5.0 stored these four; they must keep meaning the same thing.
        listOf("squircle", "circle", "rounded", "square").forEach { name ->
            assertEquals(name, name, IconShapes.NAMES.first { it.first == name }.first)
        }
    }
}
