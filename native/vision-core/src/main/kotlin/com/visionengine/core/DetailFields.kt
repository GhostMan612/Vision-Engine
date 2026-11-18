// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

const val UNKNOWN_VALUE = "—"

data class DetailField(
    val label: String,
    val value: String,
    val provenance: Provenance,
)

fun formatDurationMs(durationMs: Long): String {
    if (durationMs < 1000L) {
        return "$durationMs ms"
    }
    val whole = durationMs / 1000L
    val tenth = (durationMs % 1000L) / 100L
    return if (tenth == 0L) {
        "$whole s"
    } else {
        "$whole.$tenth s"
    }
}

fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes < 1024L) {
        return "$sizeBytes B"
    }
    if (sizeBytes < 1024L * 1024L) {
        val whole = sizeBytes / 1024L
        val tenth = ((sizeBytes % 1024L) * 10L) / 1024L
        return if (tenth == 0L) {
            "$whole KB"
        } else {
            "$whole.$tenth KB"
        }
    }
    val whole = sizeBytes / (1024L * 1024L)
    val tenth = ((sizeBytes % (1024L * 1024L)) * 10L) / (1024L * 1024L)
    return if (tenth == 0L) {
        "$whole MB"
    } else {
        "$whole.$tenth MB"
    }
}

private fun <T> fieldText(
    field: MetaField<T>,
    format: (T) -> String = { it.toString() },
): DetailField {
    val value = field.value
    return if (value == null) {
        DetailField("", UNKNOWN_VALUE, field.provenance)
    } else {
        DetailField("", format(value), field.provenance)
    }
}

private fun labeled(
    label: String,
    field: DetailField,
): DetailField = DetailField(label, field.value, field.provenance)

fun photoOrientationBadge(photo: PhotoMeta): String? {
    val orientation = photo.orientation.value ?: return null
    val rawWidth = photo.widthRaw.value
    val rawHeight = photo.heightRaw.value
    val raw = if (rawWidth != null && rawHeight != null) {
        "$rawWidth×$rawHeight"
    } else {
        UNKNOWN_VALUE
    }
    return "Raw $raw · orientation tag $orientation"
}

fun videoRotationBadge(video: VideoMeta): String? {
    val rotation = video.rotation.value ?: return null
    val rawWidth = video.widthRaw.value
    val rawHeight = video.heightRaw.value
    val raw = if (rawWidth != null && rawHeight != null) {
        "$rawWidth×$rawHeight"
    } else {
        UNKNOWN_VALUE
    }
    return "Raw $raw · rotation tag $rotation"
}

fun photoDetailFields(photo: PhotoMeta): List<DetailField> {
    return listOf(
        labeled(
            "Date taken",
            fieldText(photo.datetimeOriginal),
        ),
        labeled(
            "GPS latitude",
            fieldText(photo.gpsLatitude),
        ),
        labeled(
            "GPS longitude",
            fieldText(photo.gpsLongitude),
        ),
        labeled(
            "Orientation",
            fieldText(photo.orientation),
        ),
        labeled(
            "Make",
            fieldText(photo.make),
        ),
        labeled(
            "Model",
            fieldText(photo.model),
        ),
        labeled(
            "Exposure time",
            fieldText(photo.exposureTime),
        ),
        labeled(
            "Width",
            fieldText(
                MetaField(photo.width, photo.widthRaw.provenance),
            ),
        ),
        labeled(
            "Height",
            fieldText(
                MetaField(photo.height, photo.heightRaw.provenance),
            ),
        ),
        labeled(
            "File size",
            fieldText(photo.fileSizeBytes, ::formatFileSize),
        ),
        labeled(
            "MIME type",
            fieldText(photo.mime),
        ),
    )
}

fun videoDetailFields(video: VideoMeta): List<DetailField> {
    return listOf(
        labeled(
            "Duration",
            fieldText(video.durationMs, ::formatDurationMs),
        ),
        labeled(
            "Width",
            fieldText(
                MetaField(video.width, video.widthRaw.provenance),
            ),
        ),
        labeled(
            "Height",
            fieldText(
                MetaField(video.height, video.heightRaw.provenance),
            ),
        ),
        labeled(
            "Rotation",
            fieldText(video.rotation),
        ),
        labeled(
            "Creation time",
            fieldText(video.creationTime),
        ),
        labeled(
            "Location latitude",
            fieldText(video.locationLatitude),
        ),
        labeled(
            "Location longitude",
            fieldText(video.locationLongitude),
        ),
        labeled(
            "Codec",
            fieldText(video.codec),
        ),
        labeled(
            "File size",
            fieldText(video.fileSizeBytes, ::formatFileSize),
        ),
        labeled(
            "MIME type",
            fieldText(video.mime),
        ),
    )
}
