package com.tabgl.haven.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactHit(val name: String, val number: String?)

data class FileHit(val name: String, val uri: Uri, val mimeType: String, val sizeBytes: Long)

/**
 * On-device lookups behind the search box. Everything here is read-only, permission-guarded and
 * returns an empty list rather than throwing, so a denied permission just means no section.
 */
object DeviceSearch {

    private const val CONTACT_LIMIT = 4
    private const val FILE_LIMIT = 4

    // ---- permissions --------------------------------------------------------

    fun contactsGranted(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    /** The permissions a file search needs on this API level. */
    fun filePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun filesGranted(context: Context): Boolean = filePermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    // ---- contacts -----------------------------------------------------------

    suspend fun contacts(context: Context, query: String): List<ContactHit> =
        withContext(Dispatchers.IO) {
            if (query.isBlank() || !contactsGranted(context)) return@withContext emptyList()
            runCatching {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
                val args = arrayOf("%${escapeLike(query.trim())}%")
                val sort = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                val out = ArrayList<ContactHit>(CONTACT_LIMIT)
                val seen = HashSet<String>()
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    args,
                    sort
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && out.size < CONTACT_LIMIT) {
                        val name = cursor.getString(nameIndex)?.trim().orEmpty()
                        if (name.isBlank()) continue
                        val number = cursor.getString(numberIndex)?.trim()
                        // One row per contact: the phone table repeats a person per number.
                        if (!seen.add(name.lowercase())) continue
                        out.add(ContactHit(name, number?.takeIf { it.isNotBlank() }))
                    }
                }
                out
            }.getOrDefault(emptyList())
        }

    // ---- files --------------------------------------------------------------

    /**
     * Searches the media collections rather than `MediaStore.Files`: on API 33+ the Files collection
     * is restricted to the app's own files, while images/video/audio are readable with the
     * `READ_MEDIA_*` permissions the user has actually granted.
     */
    suspend fun files(context: Context, query: String): List<FileHit> =
        withContext(Dispatchers.IO) {
            if (query.isBlank() || !filesGranted(context)) return@withContext emptyList()
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI to Manifest.permission.READ_MEDIA_IMAGES,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI to Manifest.permission.READ_MEDIA_VIDEO,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to Manifest.permission.READ_MEDIA_AUDIO
            )
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            )
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
            val args = arrayOf("%${escapeLike(query.trim())}%")

            val out = ArrayList<FileHit>(FILE_LIMIT)
            for ((collection, permission) in collections) {
                if (out.size >= FILE_LIMIT) break
                if (Build.VERSION.SDK_INT >= 33 && !granted(context, permission)) continue
                runCatching {
                    context.contentResolver.query(
                        collection,
                        projection,
                        selection,
                        args,
                        "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                    )?.use { cursor ->
                        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        while (cursor.moveToNext() && out.size < FILE_LIMIT) {
                            val name = cursor.getString(nameIndex) ?: continue
                            val id = cursor.getLong(idIndex)
                            out.add(
                                FileHit(
                                    name = name,
                                    uri = Uri.withAppendedPath(collection, id.toString()),
                                    mimeType = cursor.getString(mimeIndex) ?: "*/*",
                                    sizeBytes = cursor.getLong(sizeIndex)
                                )
                            )
                        }
                    }
                }
            }
            out
        }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Escapes the LIKE wildcards so a literal `%` in the query does not match everything. */
    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
