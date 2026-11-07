package com.tabgl.haven.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One place for every size the icon-scale and clock-scale settings touch, so home, dock, Frequent
 * row and drawer always agree with each other.
 */
object Metrics {

    fun dockIcon(scale: String): Dp = when (scale) {
        "compact" -> 36.dp
        "large" -> 50.dp
        else -> 42.dp
    }

    fun frequentIcon(scale: String): Dp = when (scale) {
        "compact" -> 32.dp
        "large" -> 44.dp
        else -> 38.dp
    }

    fun drawerIcon(scale: String): Dp = when (scale) {
        "compact" -> 44.dp
        "large" -> 60.dp
        else -> 52.dp
    }

    /** Time text size in sp. */
    fun clockTime(scale: String): Float = when (scale) {
        "compact" -> 54f
        "large" -> 78f
        else -> 66f
    }

    /** Date text size in sp. */
    fun clockDate(scale: String): Float = when (scale) {
        "compact" -> 13f
        "large" -> 17f
        else -> 15f
    }

    fun clockHeight(scale: String): Dp = when (scale) {
        "compact" -> 94.dp
        "large" -> 132.dp
        else -> 112.dp
    }
}
