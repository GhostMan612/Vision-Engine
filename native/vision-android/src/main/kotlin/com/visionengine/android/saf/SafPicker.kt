// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

fun picturesTreeUri(): Uri {
    return DocumentsContract.buildTreeDocumentUri(
        "com.android.externalstorage.documents",
        "primary:Pictures",
    )
}

fun buildTreePickerIntent(initialUri: Uri?): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
        if (initialUri != null) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
    }
}
