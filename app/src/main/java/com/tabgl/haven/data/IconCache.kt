package com.tabgl.haven.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.content.ComponentName

/**
 * Memory-bounded icon cache. Icons are decoded off the main thread, at the exact pixel size they are
 * drawn at, and the whole thing shrinks on devices the system reports as short on memory.
 *
 * Adaptive icons are normally rendered as full-bleed layers so Haven's own icon shape (not the
 * system mask) is applied afterwards. The [style] decides how the artwork is laid down.
 */
class IconCache(context: Context, private val traits: PerfTraits) : ComponentCallbacks2 {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val gate = Semaphore(traits.decodePermits)
    private val cache: LruCache<String, ImageBitmap>

    init {
        cache = object : LruCache<String, ImageBitmap>(traits.iconCacheBytes) {
            override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
        }
        // The system tells us when it is under pressure; a launcher that ignores it gets killed.
        runCatching { appContext.registerComponentCallbacks(this) }
    }

    fun peek(key: String, sizePx: Int, style: String): ImageBitmap? =
        cache.get(cacheKey(key, clamp(sizePx), style))

    /**
     * Decodes one icon, or null when it could not be produced.
     *
     * The decode is bounded: a package manager that stalls would otherwise hold a permit for ever
     * and leave every other icon stuck behind it, which looks like an empty dock rather than a slow
     * one.
     */
    suspend fun load(app: LauncherApp, sizePx: Int, style: String): ImageBitmap? {
        val px = clamp(sizePx)
        val cacheKey = cacheKey(app.key, px, style)
        cache.get(cacheKey)?.let { return it }
        val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            gate.withPermit {
                cache.get(cacheKey) ?: withContext(Dispatchers.Default) {
                    render(app, px, style)?.also { cache.put(cacheKey, it) }
                }
            }
        }
        return loaded ?: cache.get(cacheKey)
    }

    fun clear() = cache.evictAll()

    /** Drops every cached size for one package — an update or reinstall changes its icon. */
    fun invalidatePackage(packageName: String) {
        val prefix = "$packageName/"
        for (key in cache.snapshot().keys) {
            if (key.startsWith(prefix)) cache.remove(key)
        }
    }

    // ---- ComponentCallbacks2 ------------------------------------------------

    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> cache.evictAll()
            // Hidden or merely low: the icons will be re-decoded when they are next drawn.
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(traits.iconCacheBytes / 2)
        }
    }

    override fun onLowMemory() {
        cache.evictAll()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    // ---- internals ----------------------------------------------------------

    private fun clamp(sizePx: Int): Int = sizePx.coerceIn(32, traits.maxIconPx)

    private fun cacheKey(key: String, sizePx: Int, style: String): String = "$key@$sizePx@$style"

    private fun render(app: LauncherApp, px: Int, style: String): ImageBitmap? = try {
        val drawable = packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        toBitmap(drawable, px, style).asImageBitmap()
    } catch (t: Throwable) {
        // A broken drawable must never take the launcher down with it.
        null
    }

    private fun toBitmap(drawable: Drawable, px: Int, style: String): Bitmap {
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        when (style) {
            // Exactly what the system would show: adaptive icons keep their OEM mask.
            "natural" -> {
                drawable.setBounds(0, 0, px, px)
                drawable.draw(canvas)
            }
            // Inset so the tinted plate behind the artwork is visible around it.
            "plate" -> drawInset(drawable, canvas, px, (px * 0.16f).toInt())
            else -> drawInset(drawable, canvas, px, 0)
        }
        return bitmap
    }

    private fun drawInset(drawable: Drawable, canvas: Canvas, px: Int, inset: Int) {
        val edge = px - inset * 2
        if (drawable is AdaptiveIconDrawable) {
            // Layers are 108dp for a 72dp visible window: extend bounds by a quarter on each side.
            val bleed = edge / 4
            val left = inset - bleed
            val right = inset + edge + bleed
            drawable.background?.let { it.setBounds(left, left, right, right); it.draw(canvas) }
            drawable.foreground?.let { it.setBounds(left, left, right, right); it.draw(canvas) }
        } else {
            val padding = inset + (edge * 0.1f).toInt()
            drawable.setBounds(padding, padding, px - padding, px - padding)
            drawable.draw(canvas)
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 4_000L
    }
}
