package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings

/**
 * Haven's motion language: one spring, applied to transforms only.
 *
 * There is deliberately no app launch or close animation. A launcher that plays its own transition
 * before calling `startActivity` is adding latency to the one interaction that must feel instant —
 * the earlier build waited for a spring to reach 50% before launching, which is a 90–160 ms tax on
 * every app you open. Android already has a window transition; Haven's job is to get out of its way.
 *
 * What is left is the motion a launcher actually owns:
 *
 * - the drawer sheet, following the finger and settling on a spring
 * - the home receding behind it, by scale only
 * - the pager sliding sideways
 * - an icon dipping under a finger
 *
 * Every one of those is a transform or a solid-colour alpha. Nothing animates the alpha of a
 * full-screen layer, because that forces an offscreen buffer (`saveLayer`) for every frame of the
 * animation — the single most common reason a Compose launcher drops frames.
 */
data class MotionSpec(
    val enabled: Boolean,
    val stiffness: Float,
    val damping: Float
) {
    companion object {
        val OFF = MotionSpec(enabled = false, stiffness = 0f, damping = 1f)

        /** The press response, which is deliberately much snappier than a panel. */
        const val PRESS_STIFFNESS = 1300f
        const val PRESS_DAMPING = 0.62f
        const val PRESS_SCALE = 0.92f
    }
}

/** Maps the user's motion settings onto a spring. Pure, so it is unit tested. */
fun motionSpecFor(settings: LauncherSettings): MotionSpec {
    if (!settings.animations) return MotionSpec.OFF
    val (stiffness, damping) = when (settings.motionSpeed) {
        // Snappy: settles in about a third of a second, almost no overshoot.
        "fast" -> 620f to 0.92f
        // Relaxed: a longer glide, still no wobble.
        "calm" -> 300f to 0.84f
        // Balanced: the default — One UI's pace with Pixel's damping.
        else -> 430f to 0.88f
    }
    return MotionSpec(enabled = true, stiffness = stiffness, damping = damping)
}
