package com.tabgl.haven.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCodecTest {
    @Test
    fun roundTrips() {
        val map = mapOf("a.b/c.D" to 3, "x.y/z.W" to 7)
        assertEquals(map, UsageCodec.decode(UsageCodec.encode(map)))
    }

    @Test
    fun decodeIgnoresGarbage() {
        val decoded = UsageCodec.decode("ok/pkg|2\nnonsense\n|5\nbad|x\nzero/pkg|0")
        assertEquals(mapOf("ok/pkg" to 2), decoded)
        assertTrue(UsageCodec.decode(null).isEmpty())
        assertTrue(UsageCodec.decode("").isEmpty())
    }

    @Test
    fun bumpIncrements() {
        val once = UsageCodec.bump(emptyMap(), "k")
        val twice = UsageCodec.bump(once, "k")
        assertEquals(2, twice["k"])
    }

    @Test
    fun bumpCapsEntriesAndKeepsTheNewestKey() {
        var map: Map<String, Int> = emptyMap()
        for (i in 1..200) map = UsageCodec.bump(map, "app$i", maxEntries = 20)
        assertTrue(map.size <= 20)
        assertTrue("app200" in map)
    }

    @Test
    fun decayKeepsCountsBoundedAndPositive() {
        var map: Map<String, Int> = mapOf("big" to 399)
        map = UsageCodec.bump(map, "big")
        map = UsageCodec.bump(map, "big")
        assertTrue(map.getValue("big") in 1..400)
    }
}
