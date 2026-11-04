package com.tabgl.haven.data

/** Compact text encoding of per-app launch counts ("key|count" per line) with decay and a size cap. */
object UsageCodec {
    private const val DECAY_THRESHOLD = 400

    fun decode(raw: String?): Map<String, Int> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, Int>()
        for (line in raw.lineSequence()) {
            val i = line.lastIndexOf('|')
            if (i <= 0) continue
            val n = line.substring(i + 1).toIntOrNull() ?: continue
            if (n > 0) out[line.substring(0, i)] = n
        }
        return out
    }

    fun encode(map: Map<String, Int>): String =
        map.entries.joinToString("\n") { "${it.key}|${it.value}" }

    fun bump(map: Map<String, Int>, key: String, maxEntries: Int = 80): Map<String, Int> {
        val next = LinkedHashMap(map)
        next[key] = (next[key] ?: 0) + 1
        if (next.values.sum() > DECAY_THRESHOLD) {
            for (entry in next.entries.toList()) next[entry.key] = entry.value / 2
            next.values.removeAll { it <= 0 }
            if (key !in next) next[key] = 1
        }
        if (next.size > maxEntries) {
            val keep = next.entries.sortedByDescending { it.value }.take(maxEntries)
            next.clear()
            for (e in keep) next[e.key] = e.value
            if (key !in next) next[key] = 1
        }
        return next
    }
}
