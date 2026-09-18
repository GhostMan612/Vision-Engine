// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.thumbs

import com.visionengine.core.ThumbnailCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoThumbsTest {
    private fun fixture(name: String): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream(
            "fixtures/$name",
        ) ?: throw IllegalStateException("missing fixture: $name")
        return stream.readBytes()
    }

    @Test
    fun orientationSixDecodesToDisplaySize() {
        val decoded = PhotoThumbs.decodeThumbnail(
            fixture("ve_thumb.jpg"),
            6,
        )
        assertTrue(decoded != null)
        assertEquals(256, decoded!!.width)
        assertEquals(384, decoded.height)
        assertTrue(decoded.bytes.isNotEmpty())
    }

    @Test
    fun orientationVectors() {
        val bytes = fixture("ve_thumb.jpg")
        val expectations = mapOf<Int?, Pair<Int, Int>>(
            null to (256 to 170),
            1 to (256 to 170),
            2 to (256 to 170),
            3 to (256 to 170),
            4 to (256 to 170),
            5 to (256 to 384),
            6 to (256 to 384),
            7 to (256 to 384),
            8 to (256 to 384),
            0 to (256 to 170),
            9 to (256 to 170),
        )
        for ((orientation, expected) in expectations) {
            val decoded = PhotoThumbs.decodeThumbnail(bytes, orientation)
            assertTrue(decoded != null)
            assertEquals(expected.first, decoded!!.width)
            assertEquals(expected.second, decoded.height)
        }
    }

    @Test
    fun smallPngScalesToTarget() {
        val decoded = PhotoThumbs.decodeThumbnail(
            fixture("ve_plain.png"),
            null,
        )
        assertTrue(decoded != null)
        assertEquals(256, decoded!!.width)
        assertEquals(256, decoded.height)
    }

    @Test
    fun scanLessJpegFailsGracefully() {
        val decoded = PhotoThumbs.decodeThumbnail(
            fixture("ve_exif.jpg"),
            6,
        )
        assertNull(decoded)
    }

    @Test
    fun oversizeSourcesSkipDeterministically() {
        val decoded = PhotoThumbs.decodeThumbnail(
            fixture("ve_thumb.jpg"),
            6,
            maxSourceBytes = 10L,
        )
        assertNull(decoded)
    }

    @Test
    fun sampleSizeVectors() {
        assertEquals(1, sampleSizeForBounds(100, 100, 1024))
        assertEquals(2, sampleSizeForBounds(3000, 4000, 1024))
        assertEquals(4, sampleSizeForBounds(4096, 4096, 1024))
        assertEquals(1, sampleSizeForBounds(2000, 100, 1024))
    }

    @Test
    fun rotatedSizeVectors() {
        assertEquals(16 to 24, rotatedSize(24, 16, 6))
        assertEquals(24 to 16, rotatedSize(24, 16, null))
        assertEquals(24 to 16, rotatedSize(24, 16, 3))
        assertEquals(16 to 24, rotatedSize(24, 16, 5))
    }

    @Test
    fun targetSizeVectors() {
        assertEquals(256 to 170, targetSize(24, 16, 256))
        assertEquals(256 to 384, targetSize(16, 24, 256))
    }

    @Test
    fun decodeCachesRoundTrip() {
        val decoded = PhotoThumbs.decodeThumbnail(
            fixture("ve_thumb.jpg"),
            6,
        )!!
        val cache = ThumbnailCache()
        val key = "k"
        cache.put(
            com.visionengine.core.CachedThumb(
                key,
                decoded.bytes,
                decoded.width,
                decoded.height,
            ),
        )
        val hit = cache.get(key)!!
        assertEquals(256, hit.width)
        assertEquals(384, hit.height)
        assertEquals(1, cache.hits)
    }
}
