// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

enum class Provenance {
    exif,
    container,
    filesystem,
    unknown,
}

fun parseProvenance(raw: Any?): Provenance {
    for (candidate in Provenance.values()) {
        if (candidate.name == raw) {
            return candidate
        }
    }
    return Provenance.unknown
}
