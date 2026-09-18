// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import androidx.exifinterface.media.ExifInterface
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MetaField
import com.visionengine.core.PhotoMeta
import com.visionengine.core.Provenance
import com.visionengine.core.VideoMeta
import com.visionengine.core.photoStatus

data class PhotoResult(
    val meta: PhotoMeta,
    val status: ExtractStatus,
    val warnings: List<String>,
)

data class VideoResult(
    val meta: VideoMeta,
    val status: ExtractStatus,
    val warnings: List<String>,
)

fun emptyPhoto(): PhotoMeta = PhotoMeta(
    datetimeOriginal = MetaField(null, Provenance.unknown),
    gpsLatitude = MetaField(null, Provenance.unknown),
    gpsLongitude = MetaField(null, Provenance.unknown),
    orientation = MetaField(null, Provenance.unknown),
    make = MetaField(null, Provenance.unknown),
    model = MetaField(null, Provenance.unknown),
    exposureTime = MetaField(null, Provenance.unknown),
    widthRaw = MetaField(null, Provenance.unknown),
    heightRaw = MetaField(null, Provenance.unknown),
    fileSizeBytes = MetaField(null, Provenance.unknown),
    mime = MetaField(null, Provenance.unknown),
)

object ExifReader {
    fun orientationField(raw: String?): Pair<MetaField<Long>, String?> {
        if (raw == null) {
            return MetaField<Long>(null, Provenance.unknown) to null
        }
        val parsed = raw.toLongOrNull()
        if (parsed == null) {
            return MetaField<Long>(null, Provenance.unknown) to
                "orientation value ignored: $raw"
        }
        if (parsed == 0L) {
            return MetaField<Long>(null, Provenance.unknown) to
                "orientation not legitimate: 0"
        }
        return MetaField(parsed, Provenance.exif) to null
    }

    fun dimensionField(raw: String?): Pair<MetaField<Long>, String?> {
        if (raw == null) {
            return MetaField<Long>(null, Provenance.unknown) to null
        }
        val parsed = raw.toLongOrNull()
        if (parsed == null || parsed <= 0L) {
            return MetaField<Long>(null, Provenance.unknown) to
                "dimension not legitimate: $raw"
        }
        return MetaField(parsed, Provenance.exif) to null
    }

    fun exifString(raw: String?): MetaField<String> {
        if (raw == null) {
            return MetaField(null, Provenance.unknown)
        }
        return MetaField(raw, Provenance.exif)
    }

    fun readPhoto(
        exif: ExifInterface,
        fileSizeBytes: Long?,
        mime: String?,
    ): PhotoResult {
        val warnings = mutableListOf<String>()
        val orientation = orientationField(
            exif.getAttribute(ExifInterface.TAG_ORIENTATION),
        )
        warnings.addNotNull(orientation.second)
        val width = dimensionField(
            exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH),
        )
        warnings.addNotNull(width.second)
        val height = dimensionField(
            exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH),
        )
        warnings.addNotNull(height.second)
        val latitude: Double?
        val longitude: Double?
        val latLong = FloatArray(2)
        if (exif.getLatLong(latLong)) {
            latitude = latLong[0].toDouble()
            longitude = latLong[1].toDouble()
        } else {
            latitude = null
            longitude = null
        }
        val meta = PhotoMeta(
            datetimeOriginal = exifString(
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            ),
            gpsLatitude = if (latitude == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(latitude, Provenance.exif)
            },
            gpsLongitude = if (longitude == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(longitude, Provenance.exif)
            },
            orientation = orientation.first,
            make = exifString(exif.getAttribute(ExifInterface.TAG_MAKE)),
            model = exifString(exif.getAttribute(ExifInterface.TAG_MODEL)),
            exposureTime = exifString(
                exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            ),
            widthRaw = width.first,
            heightRaw = height.first,
            fileSizeBytes = if (fileSizeBytes == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(fileSizeBytes, Provenance.filesystem)
            },
            mime = if (mime == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(mime, Provenance.filesystem)
            },
        )
        return PhotoResult(meta, photoStatus(meta), warnings.toList())
    }

    private fun MutableList<String>.addNotNull(warning: String?) {
        if (warning != null) {
            add(warning)
        }
    }
}
