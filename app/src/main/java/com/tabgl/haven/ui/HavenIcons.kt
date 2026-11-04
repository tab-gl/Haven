package com.tabgl.haven.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Tiny stroke icons so the launcher doesn't ship the multi-megabyte material-icons-extended set. */
object HavenIcons {
    val Search: ImageVector by lazy {
        stroke("Search", "M5,11a6,6 0 1,0 12,0a6,6 0 1,0 -12,0M15.5,15.5L20,20")
    }
    val Close: ImageVector by lazy { stroke("Close", "M6,6L18,18M18,6L6,18") }
    val Back: ImageVector by lazy { stroke("Back", "M19,12L5,12M11,6L5,12L11,18") }
    val Tune: ImageVector by lazy {
        stroke(
            "Tune",
            "M4,8L20,8M4,16L20,16M9,8m-2,0a2,2 0 1,0 4,0a2,2 0 1,0 -4,0M15,16m-2,0a2,2 0 1,0 4,0a2,2 0 1,0 -4,0"
        )
    }

    private fun stroke(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()
}
