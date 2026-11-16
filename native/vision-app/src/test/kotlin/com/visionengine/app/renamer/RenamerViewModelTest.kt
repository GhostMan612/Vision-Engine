// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.saf.DisplayNameSource
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import com.visionengine.core.rename.parseManifestCsv
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RenamerViewModelTest {
    private lateinit var context: Context
    private lateinit var treeUri: Uri

    private fun docUri(name: String): String {
        return documentUriForPath(
            treeUri,
            "primary:Pictures/$name",
        )
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
                "primary:Pictures/keep.jpg",
                "keep.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/cloud.jpg",
                "cloud.jpg",
                "image/jpeg",
                DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT,
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

    private fun backendFor(vararg names: String): FakeRenameBackend {
        val backend = FakeRenameBackend(
            newUriOf = { _, newName ->
                destinationDocumentUri(treeUri, "primary:Pictures", newName)
            },
        )
        for (name in names) {
            backend.liveUris.add(docUri(name))
            backend.names[docUri(name)] = name
        }
        return backend
    }

    private fun viewModel(
        backend: FakeRenameBackend,
        rows: List<Map<String, Any?>> = folderRows(),
    ): RenamerViewModel {
        return RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backend },
            discoveryFactory = { scriptedDiscovery(rows) },
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
    }

    @Test
    fun selectHealthyGrantDiscoversCounts() = runBlocking {
        val model = viewModel(backendFor())
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        val counts = model.discovery.value
        assertNotNull(counts)
        assertEquals(4, counts!!.scanned)
        assertEquals(2, counts.photos)
        assertEquals(1, counts.videos)
        assertEquals(1, counts.unsupported)
        assertEquals(
            listOf("virtual document: cloud.jpg"),
            counts.warnings,
        )
        assertEquals("Pictures", model.selectedGrant.value?.displayName)
    }

    @Test
    fun previewBuildsPlanWithReconciliation() = runBlocking {
        val model = viewModel(backendFor())
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        model.preview()
        val report = model.plan.value
        assertNotNull(report)
        assertEquals(2, report!!.toRename)
        assertEquals(1, report.skippedNoPrefix)
        assertEquals(0, report.collisionCount)
        assertTrue(model.previewFresh.value)
        val skipped = model.skippedRows.value
        assertEquals(2, skipped.size)
        assertTrue(
            skipped.any {
                it.name == "keep.jpg" &&
                    it.reason == "no camera prefix"
            },
        )
        assertTrue(
            skipped.any {
                it.name == "cloud.jpg" &&
                    it.reason == "unsupported file type"
            },
        )
        assertTrue(
            countsReconcile(
                model.discovery.value!!,
                model.planCounts.value!!,
            ),
        )
    }

    @Test
    fun previewBlockedOnWarningsOnlyDiscovery() = runBlocking {
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backendFor() },
            discoveryFactory = {
                scriptedDiscovery(emptyList(), returnNull = true)
            },
        )
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        model.preview()
        assertNull(model.plan.value)
        assertTrue(
            (model.error.value ?: "").contains("Discovery failed"),
        )
    }

    @Test
    fun lostGrantBlocksPreview() = runBlocking {
        val model = viewModel(backendFor())
        model.selectGrant(
            FolderState(grantEntry(), GrantState.LOST, null, emptyList()),
            context.contentResolver,
        )
        assertTrue(model.grantLost.value)
        assertNull(model.discovery.value)
        model.preview()
        assertNull(model.plan.value)
        assertTrue(
            (model.error.value ?: "").contains("lost access"),
        )
    }

    @Test
    fun executeWritesHonestManifestAndReport() = runBlocking {
        val backend = backendFor("IMG_a.jpg", "VID_b.mp4")
        val model = viewModel(backend)
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        model.preview()
        model.execute(context.contentResolver)
        val result = model.executeResult.value
        assertNotNull(result)
        assertEquals(2, result!!.renamedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(0, result.unverifiedCount)
        val file = model.manifestFile.value
        assertNotNull(file)
        assertTrue(file!!.name.startsWith("renames_"))
        assertTrue(file.name.endsWith(".csv"))
        val rows = parseManifestCsv(file.readText())
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.status == "renamed" })
        assertTrue(rows.all { it.contentUri.isNotEmpty() })
        assertTrue(model.reportText.value.contains("renamed=2/2"))
        assertTrue(model.reportText.value.contains(file.name))
        assertEquals(2, model.undoRemaining.value.size)
    }

    @Test
    fun undoRestoresAfterExecute() = runBlocking {
        val backend = backendFor("IMG_a.jpg", "VID_b.mp4")
        val model = viewModel(backend)
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        model.preview()
        model.execute(context.contentResolver)
        model.undo(context.contentResolver)
        val undo = model.undoResult.value
        assertNotNull(undo)
        assertEquals(2, undo!!.restoredCount)
        assertEquals(0, undo.skippedCount)
        assertEquals(0, undo.errorCount)
        assertTrue(model.reportText.value.contains("restored=2"))
        assertTrue(model.undoRemaining.value.isEmpty())
    }

    @Test
    fun executeSkipsDestinationAppearingMidBatch() = runBlocking {
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.appearAfterCalls = 1
        backend.appearing = setOf(
            destinationDocumentUri(treeUri, "primary:Pictures", "b.jpg"),
        )
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backend },
            discoveryFactory = {
                scriptedDiscovery(
                    listOf(
                        safDocRow(
                            "primary:Pictures/IMG_a.jpg",
                            "IMG_a.jpg",
                            "image/jpeg",
                        ),
                        safDocRow(
                            "primary:Pictures/IMG_b.jpg",
                            "IMG_b.jpg",
                            "image/jpeg",
                        ),
                    ),
                )
            },
        )
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        model.preview()
        model.execute(context.contentResolver)
        val result = model.executeResult.value
        assertNotNull(result)
        assertEquals(1, result!!.renamedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        val rows = parseManifestCsv(model.manifestFile.value!!.readText())
        assertEquals(
            "renamed",
            rows.first { it.originalName == "IMG_a.jpg" }.status,
        )
        assertEquals(
            "skipped",
            rows.first { it.originalName == "IMG_b.jpg" }.status,
        )
    }
}
