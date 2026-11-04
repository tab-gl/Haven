package com.tabgl.haven.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * The shapes an app icon can be masked into.
 *
 * Every rounded shape is expressed as a **percentage of the icon**, not in dp. The 0.5.x build used
 * `RoundedCornerShape(34.dp)`, which is larger than the radius of a 42 dp icon — so "Squircle"
 * silently rendered as a circle on the home screen while looking correct only at drawer sizes.
 */
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

    private val ROUNDED: Shape = RoundedCornerShape(24)
    private val CUT: Shape = CutCornerShape(22)
    private val TEARDROP: CornerBasedShape = RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 50,
        bottomStartPercent = 0
    )
    private val PEBBLE: CornerBasedShape = RoundedCornerShape(
        topStartPercent = 46,
        topEndPercent = 56,
        bottomEndPercent = 42,
        bottomStartPercent = 58
    )
    private val ARCH: CornerBasedShape = RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomEndPercent = 16,
        bottomStartPercent = 16
    )
    private val SQUIRCLE: Shape = SuperellipseShape(4f)
    private val HEXAGON: Shape = HexagonShape()

    fun of(name: String): Shape = when (name) {
        "circle" -> CircleShape
        "rounded" -> ROUNDED
        "square" -> CUT
        "teardrop" -> TEARDROP
        "pebble" -> PEBBLE
        "arch" -> ARCH
        "hexagon" -> HEXAGON
        else -> SQUIRCLE
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
        if (size.width <= 0f || size.height <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
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
