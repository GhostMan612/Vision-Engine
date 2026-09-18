// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import com.visionengine.core.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

const val AUTHORITY = "com.example.docs"

fun docRow(
    id: Any?,
    name: Any?,
    mime: Any?,
    size: Any? = null,
    modified: Any? = null,
    flags: Any? = 0,
): Map<String, Any?> = mapOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID to id,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME to name,
    DocumentsContract.Document.COLUMN_MIME_TYPE to mime,
    DocumentsContract.Document.COLUMN_SIZE to size,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED to modified,
    DocumentsContract.Document.COLUMN_FLAGS to flags,
)

class ScriptedSource {
    var rows: List<Map<String, Any?>> = emptyList()
    var columnsOverride: Array<String>? = null
    var returnNull: Boolean = false
    var throwOnQuery: RuntimeException? = null
    var calls: Int = 0
    var lastChildrenUri: Uri? = null
    var lastProjection: Array<String>? = null

    fun query(
        treeUri: Uri,
        childrenUri: Uri,
        projection: Array<String>,
    ): Cursor? {
        calls += 1
        lastChildrenUri = childrenUri
        lastProjection = projection
        throwOnQuery?.let { throw it }
        if (returnNull) {
            return null
        }
        val cols = columnsOverride ?: projection
        val cursor = MatrixCursor(cols)
        for (row in rows) {
            cursor.addRow(cols.map { row[it] })
        }
        return cursor
    }

    fun discovery(): SafDiscovery = SafDiscovery(::query)
}

@RunWith(RobolectricTestRunner::class)
class DiscoveryTest {
    private lateinit var source: ScriptedSource
    private lateinit var discovery: SafDiscovery

    @Before
    fun setUp() {
        source = ScriptedSource()
        discovery = source.discovery()
    }

    private fun treeUri(): Uri =
        DocumentsContract.buildTreeDocumentUri(AUTHORITY, "root")

    private fun expectedChildrenUri(): Uri =
        DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri(),
            "root",
        )

    @Test
    fun topLevelEnumerationMapsExactFields() {
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg", 10L, 20L),
            docRow("doc:2", "VID_b.mp4", "video/mp4", 30L, 40L),
            docRow("doc:3", "note.txt", "application/pdf", 5L, 6L),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, source.calls)
        assertEquals(expectedChildrenUri(), source.lastChildrenUri)
        assertEquals(
            discoveryProjection.toList(),
            source.lastProjection?.toList(),
        )
        assertEquals(3, outcome.sources.size)
        val photo = outcome.sources[0]
        assertEquals("IMG_a.jpg", photo.displayName)
        assertEquals(MediaKind.photo, photo.kind)
        assertEquals("image/jpeg", photo.mime)
        assertEquals(10L, photo.sizeBytes)
        assertEquals(20L, photo.modifiedMs)
        assertTrue(photo.id.contains("com.example.docs"))
        assertEquals(photo.id, photo.documentUri)
        assertEquals(0, outcome.skippedDirs)
        assertEquals(0, outcome.skippedRows)
        assertTrue(outcome.warnings.isEmpty())
        assertEquals(MediaKind.unsupported, outcome.sources[1].kind)
        assertNull(outcome.sources[1].mime)
        assertEquals(MediaKind.video, outcome.sources[2].kind)
    }

    @Test
    fun directoriesAreSkippedAndCounted() {
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg", 10L, 20L),
            docRow(
                "doc:dir",
                "sub",
                "vnd.android.document/directory",
                null,
                null,
            ),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals(1, outcome.skippedDirs)
        assertEquals("IMG_a.jpg", outcome.sources[0].displayName)
    }

    @Test
    fun photoClassifiesViaExtensionFallback() {
        source.rows = listOf(
            docRow("doc:1", "UPPER.HEIC", null, 10L, 20L),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals(MediaKind.photo, outcome.sources[0].kind)
        assertEquals("image/heic", outcome.sources[0].mime)
    }

    @Test
    fun virtualDocumentBecomesUnsupportedWithWarning() {
        source.rows = listOf(
            docRow(
                "doc:1",
                "cloud.jpg",
                "image/jpeg",
                10L,
                20L,
                DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT,
            ),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals(MediaKind.unsupported, outcome.sources[0].kind)
        assertEquals(1, outcome.warnings.size)
        assertTrue(outcome.warnings[0].contains("cloud.jpg"))
    }

    @Test
    fun foldedOrderingBeatsProviderOrder() {
        source.rows = listOf(
            docRow("doc:3", "VID_b.mp4", "video/mp4"),
            docRow("doc:2", "note.txt", "text/plain"),
            docRow("doc:1", "IMG_a.jpg", "image/jpeg"),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(
            listOf("IMG_a.jpg", "note.txt", "VID_b.mp4"),
            outcome.sources.map { it.displayName },
        )
    }

    @Test
    fun nameAndIdTiebreaksHold() {
        source.rows = listOf(
            docRow("doc:2", "a.jpg", "image/jpeg"),
            docRow("doc:1", "A.jpg", "image/jpeg"),
            docRow("doc:4", "same.jpg", "image/jpeg"),
            docRow("doc:3", "same.jpg", "image/jpeg"),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(
            listOf("A.jpg", "a.jpg", "same.jpg", "same.jpg"),
            outcome.sources.map { it.displayName },
        )
        assertEquals(
            DocumentsContract.buildDocumentUriUsingTree(treeUri(), "doc:3")
                .toString(),
            outcome.sources[2].id,
        )
        assertEquals(
            DocumentsContract.buildDocumentUriUsingTree(treeUri(), "doc:4")
                .toString(),
            outcome.sources[3].id,
        )
    }

    @Test
    fun duplicateIdKeepsFirst() {
        source.rows = listOf(
            docRow("doc:1", "first.jpg", "image/jpeg"),
            docRow("doc:1", "second.jpg", "image/jpeg"),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals("first.jpg", outcome.sources[0].displayName)
    }

    @Test
    fun missingOptionalMetadataStaysNull() {
        source.rows = listOf(
            docRow("doc:1", "bare", null),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertNull(outcome.sources[0].sizeBytes)
        assertNull(outcome.sources[0].modifiedMs)
        assertNull(outcome.sources[0].mime)
        assertEquals(MediaKind.unsupported, outcome.sources[0].kind)
    }

    @Test
    fun nullIdSkipsAndNullNameFallsBackToId() {
        source.rows = listOf(
            docRow(null, "noname.jpg", "image/jpeg"),
            docRow("doc:9", null, "image/jpeg"),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals("doc:9", outcome.sources[0].displayName)
        assertEquals(1, outcome.skippedRows)
    }

    @Test
    fun wrongTypeRowSkipsWithoutKillingDiscovery() {
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg"),
            docRow("doc:2", "bad.jpg", "image/jpeg", "not-a-number", 20L),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(1, outcome.sources.size)
        assertEquals("IMG_a.jpg", outcome.sources[0].displayName)
        assertEquals(1, outcome.skippedRows)
    }

    @Test
    fun emptyFolderYieldsEmptyOutcome() {
        source.rows = emptyList()
        val outcome = discovery.discover(treeUri())
        assertTrue(outcome.sources.isEmpty())
        assertTrue(outcome.warnings.isEmpty())
        assertEquals(0, outcome.skippedDirs)
        assertEquals(0, outcome.skippedRows)
    }

    @Test
    fun nullCursorYieldsWarning() {
        source.returnNull = true
        val outcome = discovery.discover(treeUri())
        assertTrue(outcome.sources.isEmpty())
        assertEquals(1, outcome.warnings.size)
    }

    @Test
    fun queryFailureYieldsWarning() {
        source.throwOnQuery = RuntimeException("provider down")
        val outcome = discovery.discover(treeUri())
        assertTrue(outcome.sources.isEmpty())
        assertEquals(1, outcome.warnings.size)
    }

    @Test
    fun missingColumnYieldsWarning() {
        source.columnsOverride = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg"),
        )
        val outcome = discovery.discover(treeUri())
        assertTrue(outcome.sources.isEmpty())
        assertEquals(1, outcome.warnings.size)
    }

    @Test
    fun invalidTreeUriYieldsWarning() {
        val outcome = discovery.discover(
            Uri.parse("content://com.example.docs/document/doc%3A1"),
        )
        assertTrue(outcome.sources.isEmpty())
        assertEquals(1, outcome.warnings.size)
        assertEquals(0, source.calls)
    }

    @Test
    fun authoritiesScopeIdentities() {
        val other = ScriptedSource()
        other.rows = listOf(
            docRow("doc:1", "other.jpg", "image/jpeg"),
        )
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg"),
        )
        val otherTree =
            DocumentsContract.buildTreeDocumentUri("com.example.other", "root")
        val first = discovery.discover(treeUri())
        val second = SafDiscovery { _, childrenUri, projection ->
            other.query(otherTree, childrenUri, projection)
        }.discover(otherTree)
        assertEquals(1, first.sources.size)
        assertEquals(1, second.sources.size)
        assertTrue(first.sources[0].id.contains("com.example.docs"))
        assertTrue(second.sources[0].id.contains("com.example.other"))
    }

    @Test
    fun singleQueryPerDiscovery() {
        source.rows = listOf(
            docRow("doc:1", "IMG_a.jpg", "image/jpeg", 10L, 20L),
            docRow("doc:2", "VID_b.mp4", "video/mp4", 30L, 40L),
            docRow("doc:3", "note.txt", "text/plain"),
        )
        val outcome = discovery.discover(treeUri())
        assertEquals(3, outcome.sources.size)
        assertEquals(1, source.calls)
    }
}
