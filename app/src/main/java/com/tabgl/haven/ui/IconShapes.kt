package com.tabgl.haven.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * The corner radii of a rounded icon shape, as percentages of the icon.
 *
 * The four numbers are not free: two corners share every edge, so a pair may never add up to more
 * than 100% or the shape stops being a shape. Compose does not clamp this for you, and an
 * over-rounded outline can degenerate into a clip that swallows the icon whole — which is exactly
 * how 0.8.0 shipped a "Pebble" (46 / 56 / 42 / 58) that drew as a plain circle.
 */
data class IconCorners(
    val topStart: Int,
    val topEnd: Int,
    val bottomEnd: Int,
    val bottomStart: Int
) {
    /** True when every edge can hold both of its corners. */
    fun fits(): Boolean =
        listOf(topStart, topEnd, bottomEnd, bottomStart).all { it in 0..50 } &&
            topStart + topEnd <= 100 &&
            bottomStart + bottomEnd <= 100 &&
            topStart + bottomStart <= 100 &&
            topEnd + bottomEnd <= 100
}

object IconShapes {

    /** Draw order for the settings picker. */
    val NAMES = listOf(
        "squircle" to "Squircle",
        "circle" to "Circle",
        "rounded" to "Rounded",
        "square" to "Cut",
        "teardrop" to "Teardrop",
        "pebble" to "Pebble",
        "arch" to "Arch",
        "hexagon" to "Hexagon"
    )

    /**
     * Corner radii for every rounded shape, kept as data so [fits] can be asserted in a unit test.
     * Path-based shapes (squircle, hexagon) are not listed here.
     */
    val CORNERS: Map<String, IconCorners> = mapOf(
        "circle" to IconCorners(50, 50, 50, 50),
        "rounded" to IconCorners(24, 24, 24, 24),
        "teardrop" to IconCorners(50, 50, 50, 0),
        // Organic and asymmetric, but every edge still closes: 46+50 / 50+42 / 46+42 / 50+50.
        "pebble" to IconCorners(46, 50, 42, 50),
        "arch" to IconCorners(50, 50, 16, 16)
    )

    private val CUT: Shape = CutCornerShape(22)
    private val SQUIRCLE: Shape = SuperellipseShape(4f)
    private val HEXAGON: Shape = HexagonShape()

    private fun rounded(corners: IconCorners): Shape = RoundedCornerShape(
        topStartPercent = corners.topStart,
        topEndPercent = corners.topEnd,
        bottomEndPercent = corners.bottomEnd,
        bottomStartPercent = corners.bottomStart
    )

    fun of(name: String): Shape {
        CORNERS[name]?.let { return rounded(it) }
        return when (name) {
            "square" -> CUT
            "hexagon" -> HEXAGON
            else -> SQUIRCLE
        }
    }
}

/**
 * A true superellipse (|x/a|^n + |y/b|^n = 1), which is what iOS app icons and Android's own
 * squircle mask actually are. A rounded rectangle only approximates it: its corners meet the edges
 * tangentially in a way the eye reads as "boxy" at icon sizes.
 */
private class SuperellipseShape(private val exponent: Float) : Shape {
    private var cachedSize: Size = Size.Zero
    private var cachedPath: Path? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        if (size == cachedSize) cachedPath?.let { return Outline.Generic(it) }

        val a = size.width / 2f
        val b = size.height / 2f
        val power = 2f / exponent
        val path = Path()
        val steps = 96
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            val ct = cos(t)
            val st = sin(t)
            val x = a * sign(ct) * abs(ct).pow(power)
            val y = b * sign(st) * abs(st).pow(power)
            if (i == 0) path.moveTo(a + x, b + y) else path.lineTo(a + x, b + y)
        }
        path.close()
        cachedSize = size
        cachedPath = path
        return Outline.Generic(path)
    }
}

private class HexagonShape : Shape {
    private var cachedSize: Size = Size.Zero
    private var cachedPath: Path? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size == cachedSize) cachedPath?.let { return Outline.Generic(it) }
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        cachedSize = size
        cachedPath = path
        return Outline.Generic(path)
    }
}
