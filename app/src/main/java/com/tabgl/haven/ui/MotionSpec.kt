package com.tabgl.haven.ui

import com.tabgl.haven.data.LauncherSettings

/**
 * The motion a launch/return animation should use, derived once per settings change so the
 * composables never re-derive springs per frame.
 *
 * [feel] presets are modelled on the two launchers people ask for by name:
 *
 * - **Pixel** (Material 3 Expressive): a springy, slightly slower scale where the tapped icon grows
 *   a long way towards the app and the surface fades only part way, so the transition reads as a
 *   shared element rather than a screen change.
 * - **One UI**: crisper and faster — a smaller scale, more fade, higher stiffness and almost no
 *   bounce on the way back, so it feels like a snap.
 * - **Classic**: Haven's original transition, kept because some people prefer the softer fade.
 */
data class MotionSpec(
    val enabled: Boolean,
    val mode: String,
    val stiffness: Float,
    val damping: Float,
    val closeDamping: Float,
    val zoomGain: Float,
    val fade: Float,
    val launchAt: Float,
    val lift: Float
) {
    companion object {
        const val ZOOM = "zoom"
        const val FADE = "fade"
        const val NONE = "none"

        val OFF = MotionSpec(
            enabled = false,
            mode = NONE,
            stiffness = 0f,
            damping = 1f,
            closeDamping = 1f,
            zoomGain = 0f,
            fade = 0f,
            launchAt = 0f,
            lift = 0f
        )
    }
}

private data class Feel(
    val stiffness: Float,
    val damping: Float,
    val closeDamping: Float,
    val zoomGain: Float,
    val fade: Float,
    val launchAt: Float,
    val lift: Float
)

private val PIXEL = Feel(
    stiffness = 430f,
    damping = 0.86f,
    closeDamping = 0.80f,
    zoomGain = 0.14f,
    fade = 0.42f,
    launchAt = 0.50f,
    lift = 0.90f
)

private val ONE_UI = Feel(
    stiffness = 560f,
    damping = 0.92f,
    closeDamping = 0.95f,
    zoomGain = 0.10f,
    fade = 0.55f,
    launchAt = 0.45f,
    lift = 0.50f
)

private val CLASSIC = Feel(
    stiffness = 380f,
    damping = 0.88f,
    closeDamping = 0.78f,
    zoomGain = 0.18f,
    fade = 0.72f,
    launchAt = 0.55f,
    lift = 0.34f
)

/** Maps the user's motion settings onto a spring. Pure, so it is unit tested. */
fun motionSpecFor(settings: LauncherSettings): MotionSpec {
    if (!settings.animations || settings.motionStyle == MotionSpec.NONE) return MotionSpec.OFF
    val mode = if (settings.motionStyle == MotionSpec.FADE) MotionSpec.FADE else MotionSpec.ZOOM
    val feel = when (settings.motionFeel) {
        "oneui" -> ONE_UI
        "classic" -> CLASSIC
        else -> PIXEL
    }
    val speed = when (settings.motionSpeed) {
        "fast" -> 1.35f
        "calm" -> 0.72f
        else -> 1f
    }
    return MotionSpec(
        enabled = true,
        mode = mode,
        stiffness = feel.stiffness * speed,
        damping = feel.damping,
        closeDamping = feel.closeDamping,
        zoomGain = feel.zoomGain,
        fade = feel.fade,
        launchAt = feel.launchAt,
        lift = feel.lift
    )
}
