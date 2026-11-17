// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class SafRenameBackendTest {
    private lateinit var context: Context
    private lateinit var treeUri: Uri
    private lateinit var provider: FakeDocsProvider

    private class FakeDocsProvider : ContentProvider() {
        var rows: List<String>? = null
        var throwOnQuery: Exception? = null
        var includeDisplayName: Boolean = true
        var lastQueriedUri: Uri? = null

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? {
            lastQueriedUri = uri
            val toThrow = throwOnQuery
            if (toThrow != null) {
                throw toThrow
            }
            val currentRows = rows
            if (currentRows == null) {
                return null
            }
            val cursor = MatrixCursor(
                if (includeDisplayName) {
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    )
                } else {
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    )
                },
            )
            for (name in currentRows) {
                cursor.addRow(arrayOf<Any?>(name))
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0
    }

    private fun backendFor(
        rows: List<String>?,
        throwOnQuery: Exception? = null,
        includeDisplayName: Boolean = true,
    ): SafRenameBackend {
        provider.rows = rows
        provider.throwOnQuery = throwOnQuery
        provider.includeDisplayName = includeDisplayName
        provider.lastQueriedUri = null
        return SafRenameBackend(context.contentResolver)
    }

    private fun childrenUri(): Uri {
        return DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            "primary:Pictures",
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
        provider = FakeDocsProvider()
        ShadowContentResolver.registerProvider(
            "com.example.docs",
            provider,
        )
    }

    @Test
    fun absentNameReturnsFalseAndQueriesChildrenUri() {
        val backend = backendFor(listOf("IMG_a.jpg", "keep.jpg"))
        assertFalse(
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            ),
        )
        assertEquals(childrenUri(), provider.lastQueriedUri)
    }

    @Test
    fun presentNameReturnsTrue() {
        val backend = backendFor(listOf("IMG_a.jpg", "smoke_b.mp4"))
        assertTrue(
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            ),
        )
    }

    @Test
    fun nullCursorThrows() {
        val backend = backendFor(null)
        assertThrows(IllegalStateException::class.java) {
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            )
        }
    }

    @Test
    fun fileNotFoundExceptionPropagates() {
        val backend = backendFor(
            listOf("IMG_a.jpg"),
            FileNotFoundException(
                "Missing file for primary:Pictures/VE_TEST_S4/smoke_b.mp4",
            ),
        )
        assertThrows(FileNotFoundException::class.java) {
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            )
        }
    }

    @Test
    fun missingDisplayNameColumnThrows() {
        val backend = backendFor(
            listOf("IMG_a.jpg"),
            includeDisplayName = false,
        )
        assertThrows(IllegalStateException::class.java) {
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            )
        }
    }

    @Test
    fun securityExceptionPropagates() {
        val backend = backendFor(
            listOf("IMG_a.jpg"),
            SecurityException("permission denied"),
        )
        assertThrows(SecurityException::class.java) {
            backend.destinationExists(
                treeUri.toString(),
                "primary:Pictures",
                "smoke_b.mp4",
            )
        }
    }
}
