// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.thumbs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.visionengine.core.ThumbnailCache
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThumbsDeviceTest {
    private lateinit var work: File

    @Before
    fun stage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = context.getDir("ve_mp5", Context.MODE_PRIVATE)
        work = File(base, "run-${System.currentTimeMillis()}")
        work.mkdirs()
        stageAsset("fixtures/ve_thumb.jpg", "IMG_t.jpg")
        stageAsset("fixtures/ve_plain.png", "shot.png")
        stageAsset("fixtures/ve_minimal.mp4", "VID_m.mp4")
        stageAsset("fixtures/ve_exif.jpg", "noscan.jpg")
        File(work, "junk.jpg").writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
    }

    @After
    fun cleanup() {
        work.deleteRecursively()
    }

    @Test
    fun orientationPhotoDecodesToDisplaySize() {
        val bytes = File(work, "IMG_t.jpg").readBytes()
        val decoded = PhotoThumbs.decodeThumbnail(bytes, 6)
        assertTrue(decoded != null)
        assertEquals(256, decoded!!.width)
        assertEquals(384, decoded.height)
        assertTrue(decoded.bytes.isNotEmpty())
    }

    @Test
    fun smallPngScalesToTarget() {
        val bytes = File(work, "shot.png").readBytes()
        val decoded = PhotoThumbs.decodeThumbnail(bytes, null)
        assertTrue(decoded != null)
        assertEquals(256, decoded!!.width)
        assertEquals(256, decoded.height)
    }

    @Test
    fun scanLessGarbageAndEmptyFailGracefully() {
        assertNull(
            PhotoThumbs.decodeThumbnail(
                File(work, "noscan.jpg").readBytes(),
                6,
            ),
        )
        assertNull(
            PhotoThumbs.decodeThumbnail(
                File(work, "junk.jpg").readBytes(),
                1,
            ),
        )
        assertNull(PhotoThumbs.decodeThumbnail(ByteArray(0), 1))
    }

    @Test
    fun videoFrameUnavailableOnTracklessFile() {
        val frame = VideoFrames.grabFrame(
            File(work, "VID_m.mp4").absolutePath,
        )
        assertNull(frame)
    }

    @Test
    fun missingVideoFileYieldsNull() {
        assertNull(
            VideoFrames.grabFrame(
                File(work, "nope.mp4").absolutePath,
            ),
        )
    }

    @Test
    fun cacheRoundTripOnDevice() {
        val cache = ThumbnailCache()
        val decoded = PhotoThumbs.decodeThumbnail(
            File(work, "IMG_t.jpg").readBytes(),
            6,
        )!!
        val before = snapshot()
        cache.put(
            com.visionengine.core.CachedThumb(
                "k",
                decoded.bytes,
                decoded.width,
                decoded.height,
            ),
        )
        assertEquals(256, cache.get("k")?.width)
        assertEquals(1, cache.hits)
        assertEquals(before, snapshot())
    }

    private fun stageAsset(asset: String, name: String) {
        val assets = InstrumentationRegistry.getInstrumentation()
            .context.assets
        assets.open(asset).use { input ->
            File(work, name).outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun snapshot(): String {
        val parts = mutableListOf<String>()
        work.walkTopDown().filter { it.isFile }.forEach { file ->
            var checksum = file.length()
            for (byte in file.readBytes()) {
                checksum = (checksum * 31 + byte) and 0xFFFFFFFFL
            }
            parts.add("${file.name}:$checksum")
        }
        return parts.sorted().joinToString("|")
    }
}
