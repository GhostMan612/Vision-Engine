// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class TestDocsProvider : ContentProvider() {
    companion object {
        var root: File? = null
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val name = uri.lastPathSegment
            ?: throw FileNotFoundException("no document: $uri")
        val base = root ?: throw FileNotFoundException("no root")
        val file = File(base, name)
        if (!file.isFile) {
            throw FileNotFoundException("no such doc: $uri")
        }
        return ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

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
