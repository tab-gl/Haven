package com.tabgl.haven.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

fun iconShape(name: String): Shape = when (name) {
    "circle" -> CircleShape
    "rounded" -> RoundedCornerShape(24)
    "square" -> CutCornerShape(22)
    else -> RoundedCornerShape(34)
}

/** Draws an app icon from the bounded cache; decoding happens off the main thread. */
@Composable
fun AppIcon(app: LauncherApp, size: Dp, shapeName: String, modifier: Modifier = Modifier) {
    val cache = LocalIconCache.current
    val density = LocalDensity.current
    val px = remember(size, density) { with(density) { size.roundToPx() }.coerceIn(32, 192) }
    val bitmap by produceState<ImageBitmap?>(initialValue = cache.peek(app.key, px), app.key, px) {
        if (value == null) value = cache.load(app, px)
    }
    val shape = remember(shapeName) { iconShape(shapeName) }
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(size)
            .clip(shape)
            .then(if (bitmap == null) Modifier.background(placeholder) else Modifier)
    ) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
