package com.tabgl.haven.data

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isSystem: Boolean = false
) {
    /** Stable identity of one launcher entry (a package can expose several). */
    val key: String get() = "$packageName/$activityName"
}
