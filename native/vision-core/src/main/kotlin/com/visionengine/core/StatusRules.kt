// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun photoStatus(meta: PhotoMeta): ExtractStatus {
    val known = listOf(
        meta.datetimeOriginal.isKnown,
        meta.gpsLatitude.isKnown,
        meta.gpsLongitude.isKnown,
        meta.orientation.isKnown,
        meta.make.isKnown,
        meta.model.isKnown,
        meta.exposureTime.isKnown,
        meta.widthRaw.isKnown,
        meta.heightRaw.isKnown,
    )
    return if (known.any { it }) ExtractStatus.ok else ExtractStatus.partial
}

fun videoStatus(meta: VideoMeta): ExtractStatus {
    val known = listOf(
        meta.durationMs.isKnown,
        meta.widthRaw.isKnown,
        meta.heightRaw.isKnown,
        meta.rotation.isKnown,
        meta.creationTime.isKnown,
        meta.locationLatitude.isKnown,
        meta.locationLongitude.isKnown,
        meta.codec.isKnown,
    )
    return if (known.any { it }) ExtractStatus.ok else ExtractStatus.partial
}
