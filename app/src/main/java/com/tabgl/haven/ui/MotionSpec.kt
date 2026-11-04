package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings

/**
 * The motion a launch/return animation should use, derived once per settings change so the
 * composables never re-derive springs per frame.
 */
data class MotionSpec(
    val enabled: Boolean,
    val mode: String,
    val stiffness: Float,
    val damping: Float
) {
    companion object {
        const val ZOOM = "zoom"
        const val FADE = "fade"
        const val NONE = "none"

        val OFF = MotionSpec(enabled = false, mode = NONE, stiffness = 0f, damping = 1f)
    }
}

/** Maps the user's motion settings onto a spring. Pure, so it is unit tested. */
fun motionSpecFor(settings: LauncherSettings): MotionSpec {
    if (!settings.animations || settings.motionStyle == MotionSpec.NONE) return MotionSpec.OFF
    val mode = when (settings.motionStyle) {
        MotionSpec.FADE -> MotionSpec.FADE
        else -> MotionSpec.ZOOM
    }
    val (stiffness, damping) = when (settings.motionSpeed) {
        "fast" -> 560f to 0.95f
        "calm" -> 250f to 0.80f
        else -> 380f to 0.88f
    }
    return MotionSpec(enabled = true, mode = mode, stiffness = stiffness, damping = damping)
}
