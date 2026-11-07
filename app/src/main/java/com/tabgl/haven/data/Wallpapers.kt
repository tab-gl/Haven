package com.tabgl.haven.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Setting the wallpaper without leaving the launcher. */
object Wallpapers {

    private const val MAX_EDGE = 2560

    /**
     * Applies [uri] (from the photo picker) to the **home screen** only, so the lock screen is left
     * alone. Returns false when the image could not be read or the system refused it.
     */
    suspend fun setFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri) ?: return@runCatching false
            val manager = WallpaperManager.getInstance(context)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }

    /** Downscales while decoding: a 12 MP photo never needs to be fully inflated. */
    private fun decode(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
