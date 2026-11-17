// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbStatus
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UriPipelineDeviceTest {
    private lateinit var work: File
    private lateinit var pipeline: MediaPipeline

    @Before
    fun stage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        pipeline = MediaPipeline(context.contentResolver)
        val base = context.getDir("ve_mp7", Context.MODE_PRIVATE)
        work = File(base, "run-${System.currentTimeMillis()}")
        work.mkdirs()
        TestDocsProvider.root = work
        stageAsset("fixtures/ve_thumb.jpg", "IMG_b.jpg")
        stageAsset("fixtures/ve_minimal.mp4", "VID_a.mp4")
        File(work, "bad.jpg").writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
    }

    @After
    fun cleanup() {
        TestDocsProvider.root = null
        work.deleteRecursively()
    }

    @Test
    fun uriPhotoProbesAndThumbsOnDevice() {
        val sources = listOf(
            uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(1, records.size)
        assertEquals(ExtractStatus.ok, records.single().status)
        assertEquals(6L, records.single().photo?.orientation?.value)
        assertEquals(16L, records.single().photo?.width)
        assertEquals(24L, records.single().photo?.height)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, thumb.info.status)
        assertEquals(256, thumb.info.width)
        assertEquals(384, thumb.info.height)
        val again = pipeline.thumbnailFor(records.single())
        assertTrue(thumb.bytes!!.contentEquals(again.bytes!!))
        assertEquals(1, pipeline.cache.hits)
    }

    @Test
    fun uriVideoProbesAndThumbUnavailableOnDevice() {
        val sources = listOf(
            uriSource("VID_a.mp4", MediaKind.video, "video/mp4"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(1, records.size)
        assertEquals(ExtractStatus.ok, records.single().status)
        assertEquals(0L, records.single().video?.durationMs?.value)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
        assertNull(thumb.bytes)
    }

    @Test
    fun uriCorruptPhotoUnreadableOnDevice() {
        val sources = listOf(
            uriSource("bad.jpg", MediaKind.photo, "image/jpeg"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(ExtractStatus.unreadable, records.single().status)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.failed, thumb.info.status)
        assertEquals("decode failed", thumb.info.reason)
    }

    @Test
    fun uriPipelineLeavesSourcesUntouched() {
        val before = snapshot()
        val sources = listOf(
            uriSource("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            uriSource("VID_a.mp4", MediaKind.video, "video/mp4"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        for (record in records) {
            pipeline.thumbnailFor(record)
        }
        assertEquals(before, snapshot())
    }

    private fun uriSource(
        name: String,
        kind: MediaKind,
        mime: String?,
    ): MediaSource {
        val file = File(work, name)
        val documentUri =
            "content://com.visionengine.test.docs/document/$name"
        return MediaSource(
            id = documentUri,
            displayName = name,
            kind = kind,
            documentUri = documentUri,
            mime = mime,
            sizeBytes = if (file.isFile) file.length() else null,
            modifiedMs = if (file.isFile) file.lastModified() else null,
        )
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
