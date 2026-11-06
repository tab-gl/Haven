package com.tabgl.haven.data

import android.app.ActivityManager
import android.content.Context

/** Which performance profile the launcher is running in. */
enum class Profile { LEAN, STANDARD }

/**
 * Everything the rest of the app needs to know about the device it is on.
 *
 * The point is not to make the launcher look different on a cheap phone — it is to keep the same
 * design language while spending less memory on it. A 1 GB device gets smaller icon bitmaps, fewer
 * of them in flight and one less page composed; a flagship gets the full treatment.
 */
data class PerfTraits(
    val profile: Profile,
    /** Upper bound for the icon LRU, in bytes. */
    val iconCacheBytes: Int,
    /** How many icons may decode at once. */
    val decodePermits: Int,
    /** Largest icon bitmap edge we will ever hold. */
    val maxIconPx: Int,
    /** Whether to decode the dock and Frequent icons before the first frame. */
    val preloadDock: Boolean,
    /** How many neighbouring pages the pager may compose. */
    val beyondViewportPages: Int
) {
    val isLean: Boolean get() = profile == Profile.LEAN
}

object PerformanceProfiles {

    /** Device-reported heap class, in MB. Used for the cache budget. */
    fun memoryClass(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 128
        return runCatching { am.memoryClass }.getOrDefault(128)
    }

    /**
     * Android already tells us which devices are short on memory; trust it first and fall back to
     * the heap class for devices that do not set the flag.
     */
    fun detect(context: Context): Profile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val lowRam = runCatching { am?.isLowRamDevice == true }.getOrDefault(false)
        return if (lowRam || memoryClass(context) <= 96) Profile.LEAN else Profile.STANDARD
    }

    /** Resolves the profile from the user's override, falling back to detection. */
    fun resolve(context: Context, mode: String): Profile = when (mode) {
        "lean" -> Profile.LEAN
        "full" -> Profile.STANDARD
        else -> detect(context)
    }

    fun traits(context: Context, profile: Profile): PerfTraits {
        val heap = memoryClass(context)
        return when (profile) {
            Profile.LEAN -> PerfTraits(
                profile = profile,
                // A sixth of the heap would already be 16 MB on a 96 MB device; cap it hard.
                iconCacheBytes = 2 * 1024 * 1024,
                decodePermits = 1,
                maxIconPx = 144,
                preloadDock = false,
                beyondViewportPages = 0
            )
            Profile.STANDARD -> PerfTraits(
                profile = profile,
                iconCacheBytes = (heap.toLong() * 1024L * 1024L / 14L)
                    .coerceIn(4L * 1024 * 1024, 12L * 1024 * 1024).toInt(),
                decodePermits = 2,
                maxIconPx = 192,
                preloadDock = true,
                beyondViewportPages = 1
            )
        }
    }
}
