package com.tabgl.haven.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drives the app drawer as a single 0..1 progress value. The value is only ever read inside
 * graphicsLayer lambdas, so dragging and settling never trigger recomposition.
 */
@Stable
class DrawerController(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set

    var autoFocusSearch by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun dragBy(delta: Float) {
        job?.cancel()
        progress = (progress + delta).coerceIn(0f, 1f)
    }

    fun open(focusSearch: Boolean = false) {
        autoFocusSearch = focusSearch
        animateTo(1f, 0f)
    }

    fun close() {
        autoFocusSearch = false
        animateTo(0f, 0f)
    }

    /** [velocity] is in progress-units per second (positive opens). */
    fun settle(velocity: Float) {
        val target = when {
            velocity > FLING_VELOCITY -> 1f
            velocity < -FLING_VELOCITY -> 0f
            progress > 0.5f -> 1f
            else -> 0f
        }
        if (target == 0f) autoFocusSearch = false
        animateTo(target, velocity)
    }

    private fun animateTo(target: Float, velocity: Float) {
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
            ) { value, _ -> progress = value.coerceIn(0f, 1f) }
        }
    }

    /** Lets a scrolling list inside the drawer hand over-scroll to the drawer (pull down to close). */
    fun connection(heightPx: () -> Float): NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && progress in 0.001f..0.999f) {
                dragBy(-available.y / heightPx())
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y > 0f && source == NestedScrollSource.UserInput) {
                dragBy(-available.y / heightPx())
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (progress in 0.001f..0.999f) {
                settle(-available.y / heightPx())
                return available
            }
            return Velocity.Zero
        }
    }

    private companion object {
        const val FLING_VELOCITY = 0.6f
    }
}
