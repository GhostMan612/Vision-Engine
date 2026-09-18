// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaSource
import com.visionengine.core.isDirectoryMime
import com.visionengine.core.kindForMime
import com.visionengine.core.mimeFor
import com.visionengine.core.planDiscovery

data class DiscoveryOutcome(
    val sources: List<MediaSource>,
    val skippedDirs: Int,
    val skippedRows: Int,
    val warnings: List<String>,
)

val discoveryProjection: Array<String> = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_SIZE,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    DocumentsContract.Document.COLUMN_FLAGS,
)

class SafDiscovery(
    private val queryChildren: (
        treeUri: Uri,
        childrenUri: Uri,
        projection: Array<String>,
    ) -> Cursor?,
) {
    fun discover(treeUri: Uri): DiscoveryOutcome {
        val rootId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: IllegalArgumentException) {
            return DiscoveryOutcome(
                emptyList(),
                0,
                0,
                listOf("invalid tree uri"),
            )
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            rootId,
        )
        val cursor = try {
            queryChildren(treeUri, childrenUri, discoveryProjection)
        } catch (e: Exception) {
            return DiscoveryOutcome(
                emptyList(),
                0,
                0,
                listOf("query failed"),
            )
        } ?: return DiscoveryOutcome(
            emptyList(),
            0,
            0,
            listOf("empty result"),
        )
        var skippedDirs = 0
        var skippedRows = 0
        val warnings = mutableListOf<String>()
        val found = mutableListOf<MediaSource>()
        cursor.use { active ->
            val idIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            )
            val nameIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
            val mimeIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            val sizeIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_SIZE,
            )
            val modifiedIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
            val flagsIndex = active.getColumnIndex(
                DocumentsContract.Document.COLUMN_FLAGS,
            )
            if (idIndex < 0 || nameIndex < 0 || mimeIndex < 0 ||
                sizeIndex < 0 || modifiedIndex < 0 || flagsIndex < 0
            ) {
                return DiscoveryOutcome(
                    emptyList(),
                    0,
                    0,
                    listOf("projection unsupported"),
                )
            }
            while (true) {
                val hasNext = try {
                    active.moveToNext()
                } catch (e: Exception) {
                    warnings.add("enumeration stopped")
                    break
                }
                if (!hasNext) {
                    break
                }
                try {
                    val documentId = active.getString(idIndex)
                    if (documentId.isNullOrBlank()) {
                        skippedRows += 1
                        continue
                    }
                    val rawName = if (active.isNull(nameIndex)) {
                        null
                    } else {
                        active.getString(nameIndex)
                    }
                    val name = if (rawName.isNullOrBlank()) {
                        documentId
                    } else {
                        rawName
                    }
                    val mimeRaw = if (active.isNull(mimeIndex)) {
                        null
                    } else {
                        active.getString(mimeIndex)
                    }
                    if (isDirectoryMime(mimeRaw)) {
                        skippedDirs += 1
                        continue
                    }
                    val flags = if (active.isNull(flagsIndex)) {
                        0
                    } else {
                        active.getInt(flagsIndex)
                    }
                    val size = if (active.isNull(sizeIndex)) {
                        null
                    } else {
                        active.getLong(sizeIndex)
                    }
                    val modified = if (active.isNull(modifiedIndex)) {
                        null
                    } else {
                        active.getLong(modifiedIndex)
                    }
                    val documentUri = DocumentsContract
                        .buildDocumentUriUsingTree(treeUri, documentId)
                        .toString()
                    var kind = kindForMime(mimeRaw, name)
                    var warning: String? = null
                    if (flags and DocumentsContract.Document
                            .FLAG_VIRTUAL_DOCUMENT != 0 &&
                        kind != MediaKind.unsupported
                    ) {
                        warning = "virtual document: $name"
                        kind = MediaKind.unsupported
                    }
                    if (warning != null) {
                        warnings.add(warning)
                    }
                    found.add(
                        MediaSource(
                            id = documentUri,
                            displayName = name,
                            kind = kind,
                            documentUri = documentUri,
                            mime = mimeFor(mimeRaw, name),
                            sizeBytes = size,
                            modifiedMs = modified,
                        ),
                    )
                } catch (e: Exception) {
                    skippedRows += 1
                }
            }
        }
        return DiscoveryOutcome(
            planDiscovery(found),
            skippedDirs,
            skippedRows,
            warnings.toList(),
        )
    }

    companion object {
        fun withResolver(resolver: ContentResolver): SafDiscovery =
            SafDiscovery { _, childrenUri, projection ->
                resolver.query(childrenUri, projection, null, null, null)
            }
    }
}
