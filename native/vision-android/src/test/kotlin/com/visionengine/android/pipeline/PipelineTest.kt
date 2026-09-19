// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaMetadataRetriever

@RunWith(RobolectricTestRunner::class)
class PipelineTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var pipeline: MediaPipeline

    @Before
    fun stage() {
        dir = temporaryFolder.newFolder("VE_6")
        stageAsset("ve_thumb.jpg", "IMG_b.jpg")
        stageAsset("ve_minimal.mp4", "VID_a.mp4")
        File(dir, "note.txt").writeText("hello")
        val sub = File(dir, "sub")
        sub.mkdirs()
        stageAsset("ve_thumb.jpg", "sub/nested.jpg")
        val context = ApplicationProvider.getApplicationContext<Context>()
        pipeline = MediaPipeline(context.contentResolver)
    }

    @Test
    fun loadPageBuildsRecordsWithSortPositions() {
        val sources = listOf(
            source("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            source("VID_a.mp4", MediaKind.video, "video/mp4"),
            source("note.txt", MediaKind.unsupported, null),
        )
        ShadowMediaMetadataRetriever.addMetadata(
            File(dir, "VID_a.mp4").absolutePath,
            MediaMetadataRetriever.METADATA_KEY_DURATION,
            "8340",
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(3, records.size)
        assertEquals(MediaKind.photo, records[0].kind)
        assertEquals(ExtractStatus.ok, records[0].status)
        assertEquals(6L, records[0].photo?.orientation?.value)
        assertEquals(16L, records[0].photo?.width)
        assertEquals(24L, records[0].photo?.height)
        assertEquals(0, records[0].sortIndex)
        assertEquals(MediaKind.video, records[1].kind)
        assertEquals(8340L, records[1].video?.durationMs?.value)
        assertEquals(MediaKind.unsupported, records[2].kind)
        assertEquals(ExtractStatus.unsupported, records[2].status)
    }

    @Test
    fun pagingSlicesDeterministically() {
        val sources = listOf(
            source("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            source("VID_a.mp4", MediaKind.video, "video/mp4"),
            source("note.txt", MediaKind.unsupported, null),
        )
        val page = pipeline.loadPage(sources, 1, 1)
        assertEquals(1, page.size)
        assertEquals("VID_a.mp4", page.single().source.displayName)
        assertEquals(1, page.single().sortIndex)
        assertTrue(pipeline.loadPage(sources, 9, 2).isEmpty())
    }

    @Test
    fun photoThumbnailDecodesAndCaches() {
        val sources = listOf(
            source("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        val first = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.ready, first.info.status)
        assertEquals(256, first.info.width)
        assertEquals(384, first.info.height)
        assertTrue(first.bytes != null)
        val second = pipeline.thumbnailFor(records.single())
        assertEquals(first.bytes, second.bytes)
        assertEquals(1, pipeline.cache.hits)
    }

    @Test
    fun videoAndUnsupportedThumbsStayUnavailable() {
        val sources = listOf(
            source("VID_a.mp4", MediaKind.video, "video/mp4"),
            source("note.txt", MediaKind.unsupported, null),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        val video = pipeline.thumbnailFor(records[0])
        assertEquals(ThumbStatus.unavailable, video.info.status)
        assertNull(video.bytes)
        assertEquals(ExtractStatus.ok, records[0].status)
        assertEquals(0L, records[0].video?.rotation?.value)
        val text = pipeline.thumbnailFor(records[1])
        assertEquals(ThumbStatus.unavailable, text.info.status)
    }

    @Test
    fun missingFileStaysUnreadableEndToEnd() {
        val sources = listOf(
            MediaSource(
                id = "/t/gone.jpg",
                displayName = "gone.jpg",
                kind = MediaKind.photo,
                filePath = "/t/gone.jpg",
                mime = "image/jpeg",
            ),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(ExtractStatus.unreadable, records.single().status)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
    }

    @Test
    fun uriOnlySourceStaysUnreadableWithoutLocalPath() {
        val sources = listOf(
            MediaSource(
                id = "content://com.example.docs/document/1",
                displayName = "a.jpg",
                kind = MediaKind.photo,
                documentUri = "content://com.example.docs/document/1",
                mime = "image/jpeg",
            ),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        assertEquals(ExtractStatus.unreadable, records.single().status)
        val thumb = pipeline.thumbnailFor(records.single())
        assertEquals(ThumbStatus.unavailable, thumb.info.status)
    }

    @Test
    fun pipelineLeavesSourcesUntouched() {
        val before = snapshot()
        val sources = listOf(
            source("IMG_b.jpg", MediaKind.photo, "image/jpeg"),
            source("VID_a.mp4", MediaKind.video, "video/mp4"),
        )
        val records = pipeline.loadPage(sources, 0, 10)
        for (record in records) {
            pipeline.thumbnailFor(record)
        }
        assertEquals(before, snapshot())
    }

    private fun source(name: String, kind: MediaKind, mime: String?): MediaSource {
        val file = File(dir, name)
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
        val stream = javaClass.classLoader!!.getResourceAsStream(
            "fixtures/$asset",
        ) ?: throw IllegalStateException("missing fixture: $asset")
        File(dir, name).outputStream().use { output ->
            stream.copyTo(output)
        }
    }

    private fun snapshot(): String {
        val parts = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            var checksum = file.length()
            for (byte in file.readBytes()) {
                checksum = (checksum * 31 + byte) and 0xFFFFFFFFL
            }
            parts.add("${file.name}:$checksum")
        }
        return parts.sorted().joinToString("|")
    }
}
