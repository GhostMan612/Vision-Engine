// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun orientationSwapsAxes(orientation: Long?): Boolean =
    orientation != null && orientation in 5L..8L

data class PhotoMeta(
    val datetimeOriginal: MetaField<String>,
    val gpsLatitude: MetaField<Double>,
    val gpsLongitude: MetaField<Double>,
    val orientation: MetaField<Long>,
    val make: MetaField<String>,
    val model: MetaField<String>,
    val exposureTime: MetaField<String>,
    val widthRaw: MetaField<Long>,
    val heightRaw: MetaField<Long>,
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
            return if (orientationSwapsAxes(orientation.value)) rawHeight else rawWidth
        }

    val height: Long?
        get() {
            val rawWidth = widthRaw.value
            val rawHeight = heightRaw.value
            if (rawWidth == null || rawHeight == null) {
                return null
            }
            return if (orientationSwapsAxes(orientation.value)) rawWidth else rawHeight
        }
}
