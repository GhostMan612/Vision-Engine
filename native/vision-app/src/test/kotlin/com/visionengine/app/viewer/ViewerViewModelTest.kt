// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.viewer

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.pipeline.ThumbFetch
import com.visionengine.android.pipeline.ViewerPipeline
import com.visionengine.android.saf.DisplayNameSource
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import com.visionengine.android.saf.SafDiscovery
import com.visionengine.app.renamer.safDocRow
import com.visionengine.app.renamer.scriptedDiscovery
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbInfo
import com.visionengine.core.ThumbStatus
import com.visionengine.core.photoMetaFromMap
import com.visionengine.core.videoMetaFromMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewerViewModelTest {
    private lateinit var context: Context
    private lateinit var treeUri: Uri
    private lateinit var thumbBytes: ByteArray

    private class FakeViewerPipeline(
        val records: List<MediaRecord>,
        val thumbBytes: ByteArray?,
        val thumbDelayMs: Long = 0L,
    ) : ViewerPipeline {
        override fun loadPage(
            sources: List<MediaSource>,
            offset: Int,
            limit: Int,
        ): List<MediaRecord> {
            return records.drop(offset).take(limit)
        }

        override fun thumbnailFor(record: MediaRecord): ThumbFetch {
            if (thumbDelayMs > 0L) {
                Thread.sleep(thumbDelayMs)
            }
            if (record.kind == MediaKind.unsupported) {
                return ThumbFetch(
                    ThumbInfo(
                        ThumbStatus.unavailable,
                        reason = "unsupported type",
                    ),
                    null,
                )
            }
            return ThumbFetch(
                ThumbInfo(ThumbStatus.ready),
                thumbBytes,
            )
        }
    }

    private fun fakeRecord(name: String, kind: MediaKind): MediaRecord {
        val source = MediaSource(
            id = "content://com.example.docs/document/$name",
            displayName = name,
            kind = kind,
            documentUri = "content://com.example.docs/document/$name",
            mime = if (kind == MediaKind.photo) {
                "image/jpeg"
            } else if (kind == MediaKind.video) {
                "video/mp4"
            } else {
                "text/plain"
            },
        )
        return when (kind) {
            MediaKind.photo -> MediaRecord.fromPhoto(
                source,
                0,
                photoMetaFromMap(emptyMap()),
                ExtractStatus.ok,
                emptyList(),
            )
            MediaKind.video -> MediaRecord.fromVideo(
                source,
                0,
                videoMetaFromMap(emptyMap()),
                ExtractStatus.ok,
                emptyList(),
            )
            MediaKind.unsupported -> MediaRecord.unsupported(source, 0)
        }
    }

    private fun folderRows(): List<Map<String, Any?>> {
        return listOf(
            safDocRow(
                "primary:Pictures/IMG_a.jpg",
                "IMG_a.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/VID_b.mp4",
                "VID_b.mp4",
                "video/mp4",
            ),
            safDocRow(
                "primary:Pictures/note.txt",
                "note.txt",
                "text/plain",
            ),
        )
    }

    private fun grantEntry(): GrantEntry {
        return GrantEntry(
            uri = treeUri.toString(),
            displayName = "Pictures",
            displayNameSource = DisplayNameSource.TREE_DOCUMENT_ID,
            grantedMs = 100L,
            lastSeenHealthyMs = 100L,
        )
    }

    private fun healthyFolder(): FolderState {
        return FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList())
    }

    private fun lostFolder(): FolderState {
        return FolderState(grantEntry(), GrantState.LOST, null, emptyList())
    }

    private fun viewModel(
        pipeline: ViewerPipeline,
        rows: List<Map<String, Any?>> = folderRows(),
        discoveryFactory: (android.content.ContentResolver) -> SafDiscovery =
            { scriptedDiscovery(rows) },
    ): ViewerViewModel {
        return ViewerViewModel(
            pipelineFactory = { pipeline },
            discoveryFactory = discoveryFactory,
        )
    }

    private fun mixedPipeline(thumbDelayMs: Long = 0L): FakeViewerPipeline {
        return FakeViewerPipeline(
            listOf(
                fakeRecord("IMG_a.jpg", MediaKind.photo),
                fakeRecord("VID_b.mp4", MediaKind.video),
                fakeRecord("note.txt", MediaKind.unsupported),
            ),
            thumbBytes,
            thumbDelayMs,
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
        thumbBytes = javaClass.classLoader!!
            .getResourceAsStream("fixtures/ve_thumb.jpg")!!
            .readBytes()
    }

    @Test
    fun openHealthyFolderLoadsRecordsWithCounts() = runBlocking {
        val model = viewModel(mixedPipeline())
        model.openFolder(healthyFolder(), context.contentResolver)
        assertFalse(model.loading.value)
        assertEquals(3, model.records.value.size)
        assertEquals(
            "IMG_a.jpg",
            model.records.value[0].record.source.displayName,
        )
        assertEquals(ThumbStatus.ready, model.records.value[0].thumbStatus)
        assertNotNull(model.records.value[0].thumbBitmap)
        assertEquals(
            ThumbStatus.unavailable,
            model.records.value[2].thumbStatus,
        )
        val counts = model.counts.value
        assertNotNull(counts)
        assertEquals(1, counts!!.photos)
        assertEquals(1, counts.videos)
        assertEquals(1, counts.unsupported)
        assertEquals("Pictures", model.selectedGrant.value?.displayName)
    }

    @Test
    fun openLostGrantBlocksLoad() = runBlocking {
        val model = viewModel(mixedPipeline())
        model.openFolder(lostFolder(), context.contentResolver)
        assertTrue(model.grantLost.value)
        assertTrue(model.records.value.isEmpty())
        assertNull(model.counts.value)
        assertFalse(model.loading.value)
    }

    @Test
    fun openEmptyFolderShowsEmpty() = runBlocking {
        val model = viewModel(
            FakeViewerPipeline(emptyList(), thumbBytes),
            rows = emptyList(),
        )
        model.openFolder(healthyFolder(), context.contentResolver)
        assertFalse(model.loading.value)
        assertTrue(model.records.value.isEmpty())
        val counts = model.counts.value
        assertNotNull(counts)
        assertEquals(0, counts!!.photos)
        assertEquals(0, counts.videos)
        assertEquals(0, counts.unsupported)
    }

    @Test
    fun discoveryFailureShowsError() = runBlocking {
        val model = viewModel(
            mixedPipeline(),
            discoveryFactory = {
                scriptedDiscovery(emptyList(), returnNull = true)
            },
        )
        model.openFolder(healthyFolder(), context.contentResolver)
        assertTrue(model.records.value.isEmpty())
        assertTrue(
            (model.error.value ?: "").contains("Discovery failed"),
        )
        assertFalse(model.loading.value)
    }

    @Test
    fun discoverySecurityExceptionMarksGrantLost() = runBlocking {
        val model = viewModel(
            mixedPipeline(),
            discoveryFactory = { throw SecurityException("revoked") },
        )
        model.openFolder(healthyFolder(), context.contentResolver)
        assertTrue(model.grantLost.value)
        assertTrue(
            (model.error.value ?: "").contains("lost access"),
        )
        assertFalse(model.loading.value)
    }

    @Test
    fun cancellingLoadStopsWithoutThumbs() = runBlocking {
        val model = viewModel(mixedPipeline(thumbDelayMs = 300L))
        val job = launch {
            model.openFolder(healthyFolder(), context.contentResolver)
        }
        delay(50)
        job.cancel()
        job.join()
        assertFalse(model.loading.value)
        assertEquals(3, model.records.value.size)
        assertTrue(
            model.records.value.all {
                it.thumbStatus == ThumbStatus.pending
            },
        )
    }

    @Test
    fun secondOpenSupersedesFirst() = runBlocking {
        val slow = FakeViewerPipeline(
            listOf(fakeRecord("slow_a.jpg", MediaKind.photo)),
            thumbBytes,
            300L,
        )
        val fast = FakeViewerPipeline(
            listOf(fakeRecord("fast_a.jpg", MediaKind.photo)),
            thumbBytes,
        )
        val factories = mutableListOf<ViewerPipeline>(slow, fast)
        val model = ViewerViewModel(
            pipelineFactory = { factories.removeAt(0) },
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        val jobA = launch {
            model.openFolder(healthyFolder(), context.contentResolver)
        }
        delay(50)
        model.openFolder(healthyFolder(), context.contentResolver)
        jobA.join()
        assertFalse(model.loading.value)
        assertEquals(1, model.records.value.size)
        assertEquals(
            "fast_a.jpg",
            model.records.value.single().record.source.displayName,
        )
    }

    @Test
    fun selectAndClearSelection() = runBlocking {
        val model = viewModel(mixedPipeline())
        model.openFolder(healthyFolder(), context.contentResolver)
        val first = model.records.value[0]
        model.selectRecord(first)
        assertEquals(first, model.selectedRecord.value)
        model.clearSelection()
        assertNull(model.selectedRecord.value)
    }

    @Test
    fun foldersChangedToLostPreservesRecords() = runBlocking {
        val model = viewModel(mixedPipeline())
        model.openFolder(healthyFolder(), context.contentResolver)
        assertEquals(3, model.records.value.size)
        model.onFoldersChanged(listOf(lostFolder()))
        assertTrue(model.grantLost.value)
        assertFalse(model.loading.value)
        assertEquals(3, model.records.value.size)
    }

    @Test
    fun foldersChangedToMissingResetsState() = runBlocking {
        val model = viewModel(mixedPipeline())
        model.openFolder(healthyFolder(), context.contentResolver)
        assertEquals(3, model.records.value.size)
        model.onFoldersChanged(emptyList())
        assertNull(model.selectedGrant.value)
        assertTrue(model.records.value.isEmpty())
        assertNull(model.counts.value)
    }
}
