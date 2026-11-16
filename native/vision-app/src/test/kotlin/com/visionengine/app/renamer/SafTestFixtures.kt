// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import com.visionengine.android.saf.SafDiscovery

fun safDocRow(
    id: String,
    name: String,
    mime: String,
    flags: Int = 0,
): Map<String, Any?> = mapOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID to id,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME to name,
    DocumentsContract.Document.COLUMN_MIME_TYPE to mime,
    DocumentsContract.Document.COLUMN_SIZE to null,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED to null,
    DocumentsContract.Document.COLUMN_FLAGS to flags,
)

fun scriptedDiscovery(
    rows: List<Map<String, Any?>>,
    returnNull: Boolean = false,
    throwOnQuery: RuntimeException? = null,
): SafDiscovery {
    val query: (
        Uri,
        Uri,
        Array<String>,
    ) -> Cursor? = { _, _, projection ->
        if (throwOnQuery != null) {
            throw throwOnQuery
        }
        if (returnNull) {
            null
        } else {
            val cursor = MatrixCursor(projection)
            for (row in rows) {
                cursor.addRow(projection.map { row[it] })
            }
            cursor
        }
    }
    return SafDiscovery(query)
}
