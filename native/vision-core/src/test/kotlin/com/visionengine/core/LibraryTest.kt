// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun sourceOf(id: String, name: String): MediaSource = MediaSource(
    id = id,
    displayName = name,
    kind = MediaKind.photo,
    filePath = id,
)

class OrderingTest {
    @Test
    fun sortsCaseInsensitivelyWithDeterministicTiebreaks() {
        val planned = planDiscovery(
            listOf(
                sourceOf("/t/b.jpg", "b.jpg"),
                sourceOf("/t/A.jpg", "A.jpg"),
                sourceOf("/t/c.jpg", "C.jpg"),
                sourceOf("/t/a2.jpg", "a.jpg"),
            ),
        )
        assertEquals(
            listOf("A.jpg", "a.jpg", "b.jpg", "C.jpg"),
            planned.map { it.displayName },
        )
    }

    @Test
    fun deduplicatesByIdKeepingFirst() {
        val planned = planDiscovery(
            listOf(
                sourceOf("/t/b.jpg", "b.jpg"),
                sourceOf("/t/a.jpg", "a.jpg"),
                MediaSource(
                    id = "/t/a.jpg",
                    displayName = "a-renamed.jpg",
                    kind = MediaKind.photo,
                    filePath = "/t/a.jpg",
                ),
            ),
        )
        assertEquals(2, planned.size)
        assertEquals("a.jpg", planned[0].displayName)
        assertEquals("b.jpg", planned[1].displayName)
    }

    @Test
    fun emptyStaysEmptyAndTiesOrderById() {
        assertTrue(planDiscovery(emptyList()).isEmpty())
        val planned = planDiscovery(
            listOf(
                sourceOf("/t/z/same.jpg", "same.jpg"),
                sourceOf("/t/a/same.jpg", "same.jpg"),
            ),
        )
        assertEquals("/t/a/same.jpg", planned[0].id)
    }

    @Test
    fun pagesSliceDeterministicallyWithClampedEdges() {
        val all = listOf(0, 1, 2, 3, 4)
        assertEquals(listOf(1, 2), pageOf(all, offset = 1, limit = 2))
        assertEquals(listOf(4), pageOf(all, offset = 4, limit = 9))
        assertTrue(pageOf(all, offset = 5, limit = 2).isEmpty())
        assertTrue(pageOf(all, offset = -1, limit = 2).isEmpty())
        assertTrue(pageOf(all, offset = 0, limit = 0).isEmpty())
    }
}

class RecordTest {
    @Test
    fun photoFactoryComposesCoreModel() {
        val record = MediaRecord.fromPhoto(
            source = sourceOf("/t/IMG_a.jpg", "IMG_a.jpg"),
            sortIndex = 3,
            photo = photoMetaFromMap(
                mapOf(
                    "orientation" to mapOf(
                        "value" to 6L,
                        "provenance" to "exif",
                    ),
                    "widthRaw" to mapOf(
                        "value" to 3000L,
                        "provenance" to "exif",
                    ),
                    "heightRaw" to mapOf(
                        "value" to 4000L,
                        "provenance" to "exif",
                    ),
                ),
            ),
            status = ExtractStatus.ok,
            warnings = listOf("w"),
        )
        assertEquals(MediaKind.photo, record.kind)
        assertEquals(4000L, record.photo?.width)
        assertEquals(3, record.sortIndex)
        assertEquals(ThumbStatus.pending, record.thumb.status)
    }

    @Test
    fun unsupportedFactorySkipsThumbs() {
        val record = MediaRecord.unsupported(
            source = MediaSource(
                id = "/t/n.txt",
                displayName = "n.txt",
                kind = MediaKind.unsupported,
                filePath = "/t/n.txt",
            ),
            sortIndex = 0,
        )
        assertEquals(ExtractStatus.unsupported, record.status)
        assertEquals(ThumbStatus.unavailable, record.thumb.status)
        assertNull(record.photo)
        assertNull(record.video)
    }

    @Test
    fun roundTripsWithThumbState() {
        val record = MediaRecord.fromPhoto(
            source = sourceOf("/t/IMG_a.jpg", "IMG_a.jpg"),
            sortIndex = 1,
            photo = photoMetaFromMap(emptyMap()),
            status = ExtractStatus.partial,
        )
      val back = MediaRecord.fromMap(record.toMap())
        assertEquals("/t/IMG_a.jpg", back.source.id)
        assertEquals(ExtractStatus.partial, back.status)
        assertEquals(Provenance.unknown, back.photo?.orientation?.provenance)
        assertEquals(ThumbStatus.pending, back.thumb.status)
    }

    @Test
    fun unknownKindStringsStayUnsupported() {
        val json = sourceOf("/t/a.jpg", "a.jpg").toMap().toMutableMap()
        json["kind"] = "hologram"
        assertEquals(MediaKind.unsupported, MediaSource.fromMap(json).kind)
    }

    @Test
    fun sourceRequiresIdentityTransport() {
        var failures = 0
        try {
            MediaSource(id = "x", displayName = "x", kind = MediaKind.photo)
        } catch (expected: IllegalArgumentException) {
            failures += 1
        }
        assertEquals(1, failures)
    }
}

class StatusRulesTest {
    @Test
    fun substantiveFieldsMakeOk() {
        val photo = photoMetaFromMap(
            mapOf(
                "make" to mapOf("value" to "S", "provenance" to "exif"),
                "fileSizeBytes" to mapOf(
                    "value" to 10L,
                    "provenance" to "filesystem",
                ),
            ),
        )
        assertEquals(ExtractStatus.ok, photoStatus(photo))
    }

    @Test
    fun filesystemFactsAloneStayPartial() {
        val photo = photoMetaFromMap(
            mapOf(
                "fileSizeBytes" to mapOf(
                    "value" to 10L,
                    "provenance" to "filesystem",
                ),
            ),
        )
        assertEquals(ExtractStatus.partial, photoStatus(photo))
        val video = videoMetaFromMap(
            mapOf(
                "fileSizeBytes" to mapOf(
                    "value" to 10L,
                    "provenance" to "filesystem",
                ),
            ),
        )
        assertEquals(ExtractStatus.partial, videoStatus(video))
    }

    @Test
    fun emptyStaysPartial() {
        assertEquals(
            ExtractStatus.partial,
            photoStatus(photoMetaFromMap(emptyMap())),
        )
        assertEquals(
            ExtractStatus.partial,
            videoStatus(videoMetaFromMap(emptyMap())),
        )
    }
}
