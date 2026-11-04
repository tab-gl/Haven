package com.tabgl.haven.data

/** Lightweight, allocation-conscious app search: prefix, word-prefix, substring, initials, then fuzzy. */
object AppSearch {
    private val separators = charArrayOf(' ', '-', '_', '.')

    fun rank(apps: List<LauncherApp>, query: String): List<LauncherApp> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        return apps.mapNotNull { app -> score(app.label.lowercase(), q)?.let { app to it } }
            .sortedWith(compareBy({ it.second }, { it.first.label.lowercase() }))
            .map { it.first }
    }

    internal fun score(label: String, q: String): Int? {
        if (label.startsWith(q)) return 0
        val words = label.split(*separators).filter { it.isNotEmpty() }
        if (words.any { it.startsWith(q) }) return 1
        if (label.contains(q)) return 2
        val initials = words.joinToString("") { it.substring(0, 1) }
        if (initials.startsWith(q)) return 3
        if (q.length >= 3 && isSubsequence(q, label)) return 4
        return null
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var i = 0
        for (c in haystack) {
            if (i < needle.length && c == needle[i]) i++
        }
        return i == needle.length
    }
}
