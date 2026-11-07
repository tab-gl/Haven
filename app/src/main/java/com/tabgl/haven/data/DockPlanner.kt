package com.tabgl.haven.data

/**
 * Pure dock / Frequent-row planning, kept out of the composables so it can be unit tested and so
 * the home screen only ever has to remember the finished lists.
 */
object DockPlanner {

    /**
     * Resolves the dock. A custom dock is honoured exactly as the user arranged it; the default
     * dock is topped up with the most launched apps so it never renders half empty.
     *
     * If every pinned entry has since been uninstalled or hidden the default dock is used again
     * instead of leaving the user with an empty dock they can only fix by pinning something.
     */
    fun dock(
        visible: List<LauncherApp>,
        pinned: List<String>?,
        defaults: List<String>,
        slots: Int,
        usage: Map<String, Int> = emptyMap()
    ): List<LauncherApp> {
        val limit = slots.coerceIn(1, LauncherPreferences.MAX_PINNED)
        if (pinned != null) {
            val byKey = visible.associateBy { it.key }
            val resolved = pinned.asSequence()
                .mapNotNull { byKey[it] }
                .distinctBy { it.key }
                .take(limit)
                .toList()
            if (resolved.isNotEmpty() || pinned.isEmpty()) return resolved
        }

        val out = ArrayList<LauncherApp>(limit)
        for (packageName in defaults) {
            val match = visible.firstOrNull { it.packageName == packageName }
            if (match != null && out.none { it.key == match.key }) out.add(match)
            if (out.size == limit) return out
        }
        if (out.size < limit) {
            val taken = out.mapTo(HashSet()) { it.key }
            val topUp = visible.asSequence()
                .filter { it.key !in taken }
                .sortedWith(compareByDescending<LauncherApp> { usage[it.key] ?: 0 }.thenBy { it.label.lowercase() })
                .take(limit - out.size)
            out.addAll(topUp.toList())
        }
        return out
    }

    /** Most launched apps that are neither in the dock nor hidden. */
    fun frequent(
        visible: List<LauncherApp>,
        usage: Map<String, Int>,
        excluded: Set<String>,
        count: Int
    ): List<LauncherApp> {
        if (count <= 0) return emptyList()
        return visible.asSequence()
            .filter { it.key !in excluded && (usage[it.key] ?: 0) > 0 }
            .sortedWith(
                compareByDescending<LauncherApp> { usage[it.key] ?: 0 }
                    .thenBy { it.label.lowercase() }
            )
            .take(count)
            .toList()
    }
}
