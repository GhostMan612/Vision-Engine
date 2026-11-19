// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

fun resolveFolderDisplayName(
    resolver: ContentResolver,
    treeUri: Uri,
): Pair<String, DisplayNameSource> {
    val treeDocumentId = try {
        DocumentsContract.getTreeDocumentId(treeUri)
    } catch (e: IllegalArgumentException) {
        return Pair(treeUri.toString(), DisplayNameSource.TREE_DOCUMENT_ID)
    }
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        treeDocumentId,
    )
    val folderDisplayName = try {
        resolver.query(
            documentUri,
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
    return resolveDisplayName(treeDocumentId, folderDisplayName)
}
