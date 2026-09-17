// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbKeyTest {
    @Test
    fun byteIdenticalVectorsFromDart() {
        assertEquals(
            "-8ce164ad488a8a9",
            thumbKey(
                sourceId = "/t/a.jpg",
                sizeBytes = 10L,
                modifiedMs = 20L,
                maxDimension = 256,
            ),
        )
        assertEquals(
            "-76eab53baa50c442",
            thumbKey(
                sourceId = "/t/a.jpg",
                sizeBytes = null,
                modifiedMs = null,
                maxDimension = 256,
            ),
        )
        assertEquals(
            "0ee8ed062d97c94e",
            thumbKey(
                sourceId = "/t/a.jpg",
                sizeBytes = 11L,
                modifiedMs = 20L,
                maxDimension = 256,
            ),
        )
        assertEquals(
            "-7210faf7907c9bbe",
            thumbKey(
                sourceId = "/t/IMG_20260905_110523079_HDR.jpg",
                sizeBytes = 2450011L,
                modifiedMs = 1723456789000L,
                maxDimension = 256,
            ),
        )
        assertEquals(
            "1218deb53af69797",
            thumbKey(
                sourceId = "/t/a.jpg",
                sizeBytes = 10L,
                modifiedMs = 20L,
                maxDimension = 128,
            ),
        )
    }

    @Test
    fun keysAreStableHex() {
        val first = thumbKey(
            sourceId = "/t/a.jpg",
            sizeBytes = 10L,
            modifiedMs = 20L,
            maxDimension = 256,
        )
        val second = thumbKey(
            sourceId = "/t/a.jpg",
            sizeBytes = 10L,
            modifiedMs = 20L,
            maxDimension = 256,
        )
        assertEquals(first, second)
        assertEquals(16, first.length)
    }
}

fun thumbOf(key: String, bytes: Int): CachedThumb = CachedThumb(
    key = key,
    bytes = ByteArray(bytes),
    width = 4,
    height = 4,
)

class CacheTest {
    @Test
    fun hitAndMissAccounting() {
        val cache = ThumbnailCache()
        assertNull(cache.get("a"))
        cache.put(thumbOf("a", 8))
        assertEquals(8, cache.get("a")?.byteSize)
        assertEquals(1, cache.hits)
        assertEquals(1, cache.misses)
        assertEquals(0, cache.evictions)
    }

    @Test
    fun evictsLeastRecentlyUsedPastMaxEntries() {
        val cache = ThumbnailCache(maxEntries = 2)
        cache.put(thumbOf("a", 8))
        cache.put(thumbOf("b", 8))
        assertEquals("a", cache.get("a")?.key)
        cache.put(thumbOf("c", 8))
        assertEquals(2, cache.length)
        assertNull(cache.get("b"))
        assertEquals("a", cache.get("a")?.key)
        assertEquals("c", cache.get("c")?.key)
        assertEquals(1, cache.evictions)
    }

    @Test
    fun evictsPastByteBudget() {
        val cache = ThumbnailCache(maxBytes = 16L)
        cache.put(thumbOf("a", 8))
        cache.put(thumbOf("b", 8))
        assertEquals(16L, cache.currentBytes)
        cache.put(thumbOf("c", 8))
        assertTrue(cache.currentBytes <= 16L)
        assertTrue(cache.evictions > 0)
    }

    @Test
    fun reputRefreshesWithoutDuplicating() {
        val cache = ThumbnailCache()
        cache.put(thumbOf("a", 8))
        cache.put(thumbOf("a", 8))
        assertEquals(1, cache.length)
        assertEquals(0, cache.evictions)
    }

    @Test
    fun repeatedAccessProtectsFromEviction() {
        val cache = ThumbnailCache(maxEntries = 2)
        cache.put(thumbOf("a", 8))
        cache.put(thumbOf("b", 8))
        cache.get("a")
        cache.get("a")
        cache.put(thumbOf("c", 8))
        assertEquals("a", cache.get("a")?.key)
        assertNull(cache.get("b"))
    }

    @Test
    fun clearEmpties() {
        val cache = ThumbnailCache()
        cache.put(thumbOf("a", 8))
        cache.clear()
        assertEquals(0, cache.length)
        assertEquals(0L, cache.currentBytes)
    }
}

class EdgeTest {
    @Test
    fun wrongTypesKeepProvenanceButDropValues() {
        val meta = photoMetaFromMap(
            mapOf(
                "orientation" to mapOf(
                    "value" to listOf(6L),
                    "provenance" to "exif",
                ),
                "widthRaw" to mapOf(
                    "value" to 3000.5,
                    "provenance" to "exif",
                ),
            ),
        )
        assertNull(meta.orientation.value)
        assertEquals(Provenance.exif, meta.orientation.provenance)
        assertNull(meta.widthRaw.value)
        assertNull(meta.width)
    }

    @Test
    fun doublesAcceptedForGps() {
        val meta = photoMetaFromMap(
            mapOf(
                "gpsLatitude" to mapOf("value" to 44, "provenance" to "exif"),
            ),
        )
        assertEquals(44.0, meta.gpsLatitude.value!!, 0.0)
    }

    @Test
    fun unknownStatusFallsBackToPartial() {
        val json = MediaRecord.unsupported(
            source = MediaSource(
                id = "/t/n.txt",
                displayName = "n.txt",
                kind = MediaKind.unsupported,
                filePath = "/t/n.txt",
            ),
            sortIndex = 0,
        ).toMap().toMutableMap()
        json["status"] = "future-state"
        assertEquals(
            ExtractStatus.partial,
            MediaRecord.fromMap(json).status,
        )
    }

    @Test
    fun unknownThumbStatusFallsBackToPending() {
        val node = mapOf("status" to "future-state")
        assertEquals(ThumbStatus.pending, ThumbInfo.fromMap(node).status)
    }

    @Test
    fun bigFileSizesSurvive() {
        val meta = photoMetaFromMap(
            mapOf(
                "fileSizeBytes" to mapOf(
                    "value" to 3000000000L,
                    "provenance" to "filesystem",
                ),
            ),
        )
        assertEquals(3000000000L, meta.fileSizeBytes.value)
        assertNotEquals(ExtractStatus.ok, photoStatus(meta))
    }
}
