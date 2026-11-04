package com.tabgl.haven.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.tabgl.haven.data.IconCache
import com.tabgl.haven.data.LauncherApp

val LocalIconCache = staticCompositionLocalOf<IconCache> { error("IconCache not provided") }

/** Kept for callers that only have a shape name. */
fun iconShape(name: String): Shape = IconShapes.of(name)

/**
 * Draws an app icon from the bounded cache; decoding happens off the main thread.
 *
 * [style] decides the artwork treatment — see [com.tabgl.haven.data.IconCache]:
 * `masked` crops to Haven's shape, `natural` leaves the system's own mask alone, and `plate` sets
 * the artwork on a tinted tile so legacy (non-adaptive) icons match the rest of the grid.
 */
@Composable
fun AppIcon(
    app: LauncherApp,
    size: Dp,
    shapeName: String,
    style: String = "masked",
    modifier: Modifier = Modifier
) {
    val cache = LocalIconCache.current
    val density = LocalDensity.current
    val px = remember(size, density) { with(density) { size.roundToPx() }.coerceIn(32, 192) }
    val bitmap by produceState<ImageBitmap?>(initialValue = cache.peek(app.key, px, style), app.key, px, style) {
        if (value == null) value = cache.load(app, px, style)
    }
    val shape = remember(shapeName) { IconShapes.of(shapeName) }
    val colors = MaterialTheme.colorScheme
    val plate = style == "plate"

    Box(
        modifier
            .size(size)
            .then(if (style == "natural") Modifier else Modifier.clip(shape))
            .then(
                when {
                    plate -> Modifier.background(colors.primaryContainer.copy(alpha = 0.6f))
                    bitmap == null -> Modifier.background(colors.surfaceVariant)
                    else -> Modifier
                }
            )
    ) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
