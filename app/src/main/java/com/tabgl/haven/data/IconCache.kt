package com.tabgl.haven.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
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

/**
 * Memory-bounded icon cache. Icons are decoded off the main thread, two at a time, at the exact
 * pixel size they are drawn at.
 *
 * Adaptive icons are normally rendered as full-bleed layers so Haven's own icon shape (not the
 * system mask) is applied afterwards. The [IconStyle] decides how the artwork is laid down.
 */
class IconCache(context: Context) {
    private val packageManager: PackageManager = context.applicationContext.packageManager
    private val gate = Semaphore(2)
    private val cache: LruCache<String, ImageBitmap>

    init {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val bytes = (am.memoryClass.toLong() * 1024L * 1024L / 14L)
            .coerceIn(2L * 1024 * 1024, 12L * 1024 * 1024).toInt()
        cache = object : LruCache<String, ImageBitmap>(bytes) {
            override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
        }
    }

    fun peek(key: String, sizePx: Int, style: String): ImageBitmap? = cache.get(cacheKey(key, sizePx, style))

    suspend fun load(app: LauncherApp, sizePx: Int, style: String): ImageBitmap? {
        val cacheKey = cacheKey(app.key, sizePx, style)
        cache.get(cacheKey)?.let { return it }
        return gate.withPermit {
            cache.get(cacheKey) ?: withContext(Dispatchers.Default) {
                render(app, sizePx, style)?.also { cache.put(cacheKey, it) }
            }
        }
    }

    fun clear() = cache.evictAll()

    /** Drops every cached size for one package — an update or reinstall changes its icon. */
    fun invalidatePackage(packageName: String) {
        val prefix = "$packageName/"
        for (key in cache.snapshot().keys) {
            if (key.startsWith(prefix)) cache.remove(key)
        }
    }

    private fun cacheKey(key: String, sizePx: Int, style: String): String = "$key@$sizePx@$style"

    private fun render(app: LauncherApp, px: Int, style: String): ImageBitmap? = try {
        val drawable = packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        toBitmap(drawable, px, style).asImageBitmap()
    } catch (e: Exception) {
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
}
