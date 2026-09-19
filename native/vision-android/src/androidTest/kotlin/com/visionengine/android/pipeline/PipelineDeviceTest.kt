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
import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbStatus
import com.visionengine.core.planDiscovery
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PipelineDeviceTest {
    private lateinit var work: File
    private lateinit var pipeline: MediaPipeline

    @Before
    fun stage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        pipeline = MediaPipeline(context.contentResolver)
        val base = context.getDir("ve_mp6", Context.MODE_PRIVATE)
        work = File(base, "run-${System.currentTimeMillis()}")
        work.mkdirs()
        stageAsset("fixtures/ve_thumb.jpg", "IMG_b.jpg")
        stageAsset("fixtures/ve_plain.png", "shot.png")
        stageAsset("fixtures/ve_minimal.mp4", "VID_a.mp4")
        File(work, "note.txt").writeText("hello")
        val sub = File(work, "sub")
        sub.mkdirs()
        stageAsset("fixtures/ve_thumb.jpg", "sub/nested.jpg")
    }

    @After
    fun cleanup() {
        work.deleteRecursively()
    }

    @Test
    fun pipelineDiscoversExtractsThumbsAndCleansUp() {
        val before = snapshot()
        val sources = listOf(
            source("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            source("VID_a.mp4", MediaKind.video, "video/mp4"),
            source("note.txt", MediaKind.unsupported, null),
            source("shot.png", MediaKind.photo, "image/png"),
        )
        val ordered = planDiscovery(sources)
        assertEquals(
            listOf("IMG_b.jpg", "note.txt", "shot.png", "VID_a.mp4"),
            ordered.map { it.displayName },
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(4, records.size)
        assertEquals(6L, records[0].photo?.orientation?.value)
        assertEquals(16L, records[0].photo?.width)
        assertEquals(24L, records[0].photo?.height)
        assertEquals(0L, records[1].video?.durationMs?.value)
        assertEquals(ExtractStatus.unsupported, records[2].status)
        assertEquals(ExtractStatus.partial, records[3].status)
        val thumb = pipeline.thumbnailFor(records[0])
        assertEquals(ThumbStatus.ready, thumb.info.status)
        assertEquals(256, thumb.info.width)
        assertEquals(384, thumb.info.height)
        val again = pipeline.thumbnailFor(records[0])
        assertTrue(thumb.bytes!!.contentEquals(again.bytes!!))
        assertEquals(1, pipeline.cache.hits)
        val png = pipeline.thumbnailFor(records[3])
        assertEquals(ThumbStatus.ready, png.info.status)
        assertEquals(256, png.info.width)
        assertEquals(256, png.info.height)
        val video = pipeline.thumbnailFor(records[1])
        assertEquals(ThumbStatus.unavailable, video.info.status)
        assertNull(video.bytes)
        val text = pipeline.thumbnailFor(records[2])
        assertEquals(ThumbStatus.unavailable, text.info.status)
        assertEquals(before, snapshot())
        work.deleteRecursively()
        assertTrue(!work.exists())
    }

    private fun source(
        name: String,
        kind: MediaKind,
        mime: String?,
    ): MediaSource {
        val file = File(work, name)
        return MediaSource(
            id = file.absolutePath,
            displayName = name,
            kind = kind,
            filePath = file.absolutePath,
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
