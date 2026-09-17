// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

data class MetaField<T>(
    val value: T?,
    val provenance: Provenance,
) {
    val isKnown: Boolean
        get() = value != null && provenance != Provenance.unknown
}
