// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import android.content.ContentResolver
import android.content.Intent

fun persistedGrantUris(resolver: ContentResolver): Set<String> {
    return resolver.persistedUriPermissions
        .filter { it.isReadPermission && it.isWritePermission }
        .map { it.uri.toString() }
        .toSet()
}

fun grantFlags(): Int {
    return Intent.FLAG_GRANT_READ_URI_PERMISSION or
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}
