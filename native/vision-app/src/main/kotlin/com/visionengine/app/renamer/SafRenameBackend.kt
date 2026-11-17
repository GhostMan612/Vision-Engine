// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

fun documentUriForPath(treeUri: Uri, path: String): String {
    return DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        path,
    ).toString()
}

class SafRenameBackend(
    private val resolver: ContentResolver,
) : RenameBackend {
    override fun rename(documentUri: String, newName: String): String? {
        return DocumentsContract.renameDocument(
            resolver,
            Uri.parse(documentUri),
            newName,
        )?.toString()
    }

    override fun displayName(documentUri: String): String? {
        return try {
            resolver.query(
                Uri.parse(documentUri),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    val index = cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    )
                    if (index < 0 || cursor.isNull(index)) {
                        null
                    } else {
                        cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun destinationExists(
        treeUri: String,
        treeDocumentId: String,
        name: String,
    ): Boolean {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            Uri.parse(treeUri),
            treeDocumentId,
        )
        val cursor = resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        ) ?: throw IllegalStateException(
            "could not enumerate folder",
        )
        cursor.use {
            val index = it.getColumnIndex(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
            if (index < 0) {
                throw IllegalStateException(
                    "provider returned no display name column",
                )
            }
            while (it.moveToNext()) {
                if (!it.isNull(index) && it.getString(index) == name) {
                    return true
                }
            }
            return false
        }
    }
}
