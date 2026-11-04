package com.tabgl.haven.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Where an icon sits on screen, as a 0..1 fraction of the window. */
data class ZoomOrigin(val x: Float, val y: Float) {
    companion object {
        val CENTER = ZoomOrigin(0.5f, 0.5f)

        /** Converts an icon centre in window pixels into a [ZoomOrigin]. */
        fun of(center: Offset, windowWidth: Int, windowHeight: Int): ZoomOrigin {
            if (windowWidth <= 0 || windowHeight <= 0) return CENTER
            return ZoomOrigin(
                x = (center.x / windowWidth).coerceIn(0f, 1f),
                y = (center.y / windowHeight).coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Drives the iOS-style "the app grows out of the icon you tapped" transition.
 *
 * A single 0..1 value describes how far the launcher has been pushed towards the launched app:
 * 0 is the resting home screen, 1 is fully zoomed in and faded out. Tapping an icon runs it
 * forward and fires the real `startActivity` part-way through, so the zoom is already in motion
 * when the app window arrives; returning from the app runs the same value back to 0, which reads
 * as the app collapsing into the icon it came from.
 *
 * The value is only ever read inside `graphicsLayer` lambdas, so the transition costs no
 * recomposition.
 */
@Stable
class LaunchZoom(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set

    var origin by mutableStateOf(ZoomOrigin.CENTER)
        private set

    /** The tile being launched, so it can lift while the rest of the surface zooms past it. */
    var launchingKey by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    /**
     * Runs the open transition, then calls [launch] exactly once. [launch] returns whether the app
     * actually started; if it did not, the launcher retreats instead of being left faded out.
     */
    fun open(key: String?, from: ZoomOrigin?, spec: MotionSpec, launch: () -> Boolean) {
        job?.cancel()
        from?.let { origin = it }
        launchingKey = key
        if (!spec.enabled) {
            progress = 0f
            launchingKey = null
            launch()
            return
        }
        var fired = false
        var started = true
        val fire = {
            if (!fired) {
                fired = true
                started = launch()
            }
        }
        job = scope.launch {
            animate(
                initialValue = progress,
                targetValue = 1f,
                // Opens a little stiffer than it closes: leaving must feel instant, arriving can breathe.
                animationSpec = spring(dampingRatio = spec.damping, stiffness = spec.stiffness * OPEN_STIFFNESS)
            ) { value, _ ->
                progress = value.coerceIn(0f, 1f)
                if (progress >= LAUNCH_AT) fire()
            }
            fire()
            if (!started) {
                animate(
                    initialValue = progress,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = spec.stiffness)
                ) { value, _ -> progress = value.coerceIn(0f, 1f) }
                progress = 0f
                launchingKey = null
            }
        }
    }

    /** Runs the close transition: the launcher settles back out of the app. */
    fun settleBack(spec: MotionSpec) {
        job?.cancel()
        if (!spec.enabled) {
            progress = 0f
            return
        }
        job = scope.launch {
            animate(
                initialValue = progress,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = CLOSE_DAMPING, stiffness = spec.stiffness * CLOSE_STIFFNESS)
            ) { value, _ -> progress = value.coerceIn(0f, 1f) }
            progress = 0f
            launchingKey = null
        }
    }

    fun reset() {
        job?.cancel()
        progress = 0f
        launchingKey = null
    }

    private companion object {
        /** Launch once the zoom is far enough along that the app window lands on top of it. */
        const val LAUNCH_AT = 0.55f
        const val OPEN_STIFFNESS = 1.25f
        const val CLOSE_STIFFNESS = 0.8f

        /** A touch of bounce on the way back keeps the return from feeling like a fade. */
        const val CLOSE_DAMPING = 0.78f
    }
}

/**
 * Applies the launch/return transform. Safe to stack with other `graphicsLayer` modifiers because
 * it is a no-op at rest.
 */
fun Modifier.surfaceZoom(zoom: LaunchZoom, spec: MotionSpec): Modifier = graphicsLayer {
    val p = zoom.progress
    if (p <= 0.001f) return@graphicsLayer
    transformOrigin = TransformOrigin(zoom.origin.x, zoom.origin.y)
    when (spec.mode) {
        MotionSpec.NONE -> Unit
        MotionSpec.FADE -> alpha = 1f - 0.78f * p
        else -> {
            val scale = 1f + ZOOM_GAIN * p
            scaleX = scale
            scaleY = scale
            // Not fully transparent: the app window covers this, and holding a little alpha makes
            // the return read as a settle instead of a hard cut from black.
            alpha = 1f - 0.72f * p
        }
    }
}

private const val ZOOM_GAIN = 0.18f
