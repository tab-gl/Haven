package com.tabgl.haven.data

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppRepository(private val packageManager: PackageManager) {
    @Suppress("DEPRECATION")
    fun loadApps(): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
        return resolved.asSequence()
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                LauncherApp(
                    label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifBlank { activity.packageName },
                    packageName = activity.packageName,
                    activityName = activity.name
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
