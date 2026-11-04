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
 * pixel size they are drawn at. Adaptive icons are rendered as full-bleed layers so Haven's own
 * icon shape (not the system mask) is applied afterwards.
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

    fun peek(key: String, sizePx: Int): ImageBitmap? = cache.get("$key@$sizePx")

    suspend fun load(app: LauncherApp, sizePx: Int): ImageBitmap? {
        val cacheKey = "${app.key}@$sizePx"
        cache.get(cacheKey)?.let { return it }
        return gate.withPermit {
            cache.get(cacheKey) ?: withContext(Dispatchers.Default) {
                render(app, sizePx)?.also { cache.put(cacheKey, it) }
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

    private fun render(app: LauncherApp, px: Int): ImageBitmap? = try {
        val drawable = packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        toBitmap(drawable, px).asImageBitmap()
    } catch (e: Exception) {
        null
    }

    private fun toBitmap(drawable: Drawable, px: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            // Layers are 108dp for a 72dp visible window: extend bounds by a quarter on each side.
            val o = px / 4
            drawable.background?.let { it.setBounds(-o, -o, px + o, px + o); it.draw(canvas) }
            drawable.foreground?.let { it.setBounds(-o, -o, px + o, px + o); it.draw(canvas) }
        } else {
            val inset = (px * 0.1f).toInt()
            drawable.setBounds(inset, inset, px - inset, px - inset)
            drawable.draw(canvas)
        }
        return bitmap
    }
}
