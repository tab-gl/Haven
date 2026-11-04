package com.tabgl.haven.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    @Suppress("DEPRECATION")
    fun loadApps(): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
        return resolved.asSequence()
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                if (activity.packageName == appContext.packageName) return@mapNotNull null
                val flags = activity.applicationInfo?.flags ?: 0
                val system = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                LauncherApp(
                    label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                        .ifBlank { activity.packageName },
                    packageName = activity.packageName,
                    activityName = activity.name,
                    isSystem = system
                )
            }
            .distinctBy { it.key }
            .sortedWith(Comparator { a, b -> a.label.compareTo(b.label, ignoreCase = true) })
            .toList()
    }

    /** Package names of the user's default dialer, messages, camera and browser (when resolvable). */
    fun defaultDockPackages(): List<String> = listOfNotNull(
        resolvePackage(Intent(Intent.ACTION_DIAL)),
        resolvePackage(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
        resolvePackage(Intent("android.media.action.STILL_IMAGE_CAMERA")),
        resolvePackage(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
        )
    ).distinct()

    @Suppress("DEPRECATION")
    private fun resolvePackage(intent: Intent): String? {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrNull()
        return info?.activityInfo?.packageName?.takeIf { it != "android" }
    }

    /** Emits whenever apps are installed, removed, updated, or the locale (labels) changes. */
    fun changes(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val locale = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        ContextCompat.registerReceiver(appContext, receiver, packages, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(appContext, receiver, locale, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.conflate()
}
