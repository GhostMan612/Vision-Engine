// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun rotationSwapsAxes(rotation: Long?): Boolean =
    rotation == 90L || rotation == 270L

data class VideoMeta(
    val durationMs: MetaField<Long>,
    val widthRaw: MetaField<Long>,
    val heightRaw: MetaField<Long>,
    val rotation: MetaField<Long>,
    val creationTime: MetaField<String>,
    val locationLatitude: MetaField<Double>,
    val locationLongitude: MetaField<Double>,
    val codec: MetaField<String>,
    val fileSizeBytes: MetaField<Long>,
    val mime: MetaField<String>,
) {
    val width: Long?
        get() {
            val rawWidth = widthRaw.value
            val rawHeight = heightRaw.value
            if (rawWidth == null || rawHeight == null) {
                return null
            }
            return if (rotationSwapsAxes(rotation.value)) rawHeight else rawWidth
        }

    val height: Long?
        get() {
            val rawWidth = widthRaw.value
            val rawHeight = heightRaw.value
            if (rawWidth == null || rawHeight == null) {
                return null
            }
            return if (rotationSwapsAxes(rotation.value)) rawWidth else rawHeight
        }
}
