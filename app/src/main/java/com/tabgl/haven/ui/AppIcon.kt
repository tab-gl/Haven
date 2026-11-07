package com.tabgl.haven.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tabgl.haven.data.IconCache
import com.tabgl.haven.data.LauncherApp
import kotlinx.coroutines.delay

val LocalIconCache = staticCompositionLocalOf<IconCache> { error("IconCache not provided") }

/** Kept for callers that only have a shape name. */
fun iconShape(name: String): Shape = IconShapes.of(name)

/**
 * Draws an app icon from the bounded cache; decoding happens off the main thread.
 *
 * [style] decides the artwork treatment — see [com.tabgl.haven.data.IconCache]:
 * `masked` crops to Haven's shape, `natural` leaves the system's own mask alone, and `plate` sets
 * the artwork on a tinted tile so legacy (non-adaptive) icons match the rest of the grid.
 *
 * While a bitmap is missing the tile shows the app's initial. That matters: the old placeholder was
 * a `surfaceVariant` square, which is the exact colour of the dock pill, so an icon that was merely
 * still decoding made the whole dock look empty.
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
    // Bucket the pixel size so a one-step change in icon size reuses the same cached decode.
    val px = remember(size, density) {
        val raw = with(density) { size.roundToPx() }.coerceIn(32, 192)
        ((raw + 7) / 8) * 8
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = cache.peek(app.key, px, style), app.key, px, style) {
        if (value == null) value = cache.load(app, px, style)
        if (value == null) {
            // One retry: the first attempt can lose a race with a package manager that is busy.
            delay(160)
            value = cache.load(app, px, style)
        }
    }
    val shape = remember(shapeName) { IconShapes.of(shapeName) }
    val colors = MaterialTheme.colorScheme
    val plate = style == "plate"
    val loaded = bitmap

    Box(
        modifier
            .size(size)
            .then(if (style == "natural") Modifier else Modifier.clip(shape))
    ) {
        if (loaded != null) {
            Image(bitmap = loaded, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (plate) colors.primaryContainer.copy(alpha = 0.6f) else colors.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = (size.value * 0.34f).sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
