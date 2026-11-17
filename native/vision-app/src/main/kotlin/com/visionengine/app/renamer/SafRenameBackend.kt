// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

fun destinationDocumentUri(
    treeUri: Uri,
    treeDocumentId: String,
    name: String,
): String {
    return DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        "$treeDocumentId/$name",
    ).toString()
}

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

    override fun destinationExists(documentUri: String): Boolean {
        return resolver.query(
            Uri.parse(documentUri),
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            cursor.moveToFirst()
        } ?: false
    }
}
