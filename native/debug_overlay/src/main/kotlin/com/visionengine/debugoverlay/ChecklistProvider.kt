// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Exposes the checklist via adb (the "checked via adb" mechanism):
 *
 *   adb shell content query --uri content://<authority>.checklist/items
 *
 * Authority defaults to `${applicationId}.checklist` (see AndroidManifest).
 * Columns: `item_id`, `label`, `status` (OPEN/PASS/FAIL), `checked` (0/1),
 * `notes`.
 */
class ChecklistProvider : ContentProvider() {

    private lateinit var store: ChecklistStore

    override fun onCreate(): Boolean {
        store = ChecklistStore(context!!)
        return true
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/checklist"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uri.path != "/items") {
            throw IllegalArgumentException("unknown uri: $uri")
        }
        val cursor = MatrixCursor(arrayOf("item_id", "label", "status", "checked", "notes"))
        for (item in store.load()) {
            val row: Array<Any> = arrayOf(
                item.id,
                item.label,
                item.status.name,
                if (item.checked) 1 else 0,
                item.notes,
            )
            cursor.addRow(row)
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
