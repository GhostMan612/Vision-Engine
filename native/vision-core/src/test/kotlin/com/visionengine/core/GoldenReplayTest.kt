// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun loadGolden(name: String): Map<String, Any?> {
    val stream = object {}.javaClass.getResourceAsStream("/golden/$name.json")
        ?: throw IllegalStateException("missing golden: $name")
    val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    @Suppress("UNCHECKED_CAST")
    val parsed = parseJson(text) as Map<String, Any?>
    return parsed
}

fun withoutExpected(golden: Map<String, Any?>): Map<String, Any?> {
    val copy = LinkedHashMap(golden)
    copy.remove("expectedWidth")
    copy.remove("expectedHeight")
    return copy
}

class GoldenReplayTest {
    @Test
    fun photoExifDecodesWithProvenanceAndCorrectedDims() {
        val golden = loadGolden("photo_exif")
        val meta = photoMetaFromMap(withoutExpected(golden))
        assertEquals("2026:09:14 11:05:23", meta.datetimeOriginal.value)
        assertEquals(Provenance.exif, meta.datetimeOriginal.provenance)
        assertEquals(44.9778, meta.gpsLatitude.value!!, 0.0)
        assertEquals(6L, meta.orientation.value)
        assertEquals(Provenance.filesystem, meta.fileSizeBytes.provenance)
        assertEquals(golden["expectedWidth"], meta.width)
        assertEquals(golden["expectedHeight"], meta.height)
        assertEquals(withoutExpected(golden), photoMetaToMap(meta))
    }

    @Test
    fun videoMetaDecodesWithUnknownLocationIntact() {
        val golden = loadGolden("video_meta")
        val meta = videoMetaFromMap(withoutExpected(golden))
        assertEquals(8340L, meta.durationMs.value)
        assertEquals(90L, meta.rotation.value)
        assertNull(meta.locationLatitude.value)
        assertEquals(Provenance.unknown, meta.locationLatitude.provenance)
        assertEquals("avc1", meta.codec.value)
        assertEquals(golden["expectedWidth"], meta.width)
        assertEquals(golden["expectedHeight"], meta.height)
        assertEquals(withoutExpected(golden), videoMetaToMap(meta))
    }

    @Test
    fun missingKeysDecodeToUnknown() {
        val photo = photoMetaFromMap(emptyMap())
        assertNull(photo.orientation.value)
        assertEquals(Provenance.unknown, photo.orientation.provenance)
        assertNull(photo.width)
        val video = videoMetaFromMap(emptyMap())
        assertNull(video.durationMs.value)
        assertEquals(Provenance.unknown, video.durationMs.provenance)
        assertNull(video.width)
    }

    @Test
    fun orientationAndRotationVectors() {
        val golden = loadGolden("orientation_vectors")
        @Suppress("UNCHECKED_CAST")
        val photoBase = golden["photoBase"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val photoVectors = golden["photoVectors"] as List<Any?>
        for (raw in photoVectors) {
            @Suppress("UNCHECKED_CAST")
            val vector = raw as Map<String, Any?>
            val input = LinkedHashMap(photoBase)
            input["orientation"] = mapOf(
                "value" to vector["orientation"],
                "provenance" to "exif",
            )
            val meta = photoMetaFromMap(input)
            assertEquals(vector["width"], meta.width)
            assertEquals(vector["height"], meta.height)
        }
        @Suppress("UNCHECKED_CAST")
        val videoBase = golden["videoBase"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val videoVectors = golden["videoVectors"] as List<Any?>
        for (raw in videoVectors) {
            @Suppress("UNCHECKED_CAST")
            val vector = raw as Map<String, Any?>
            val input = LinkedHashMap(videoBase)
            input["rotation"] = mapOf(
                "value" to vector["rotation"],
                "provenance" to "container",
            )
            val meta = videoMetaFromMap(input)
            assertEquals(vector["width"], meta.width)
            assertEquals(vector["height"], meta.height)
        }
    }

    @Test
    fun parserHandlesJsonShapes() {
        @Suppress("UNCHECKED_CAST")
        val parsed = parseJson(
            "{\"a\":[1,-2,3.5,true,false,null],\"s\":\"x\\u0041\\n\"}",
        ) as Map<String, Any?>
        assertEquals(listOf(1L, -2L, 3.5, true, false, null), parsed["a"])
        assertEquals("xA\n", parsed["s"])
    }

    @Test
    fun parserRejectsMalformed() {
        var failures = 0
        for (bad in listOf("{", "[1,", "{\"a\":}", "\"x", "nul", "{1:2}")) {
            try {
                parseJson(bad)
            } catch (expected: IllegalArgumentException) {
                failures += 1
            }
        }
        assertEquals(6, failures)
    }
}
