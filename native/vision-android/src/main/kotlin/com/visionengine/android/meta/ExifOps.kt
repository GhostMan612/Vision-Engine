// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

data class ExifWriteResult(
    val changed: Boolean,
    val verified: Boolean,
    val warnings: List<String> = emptyList(),
)

object ExifOps {
    private const val DATE_FORMAT = "yyyy:MM:dd HH:mm:ss"
    private const val MINUTES_PER_HOUR = 60L

    private val gpsTags = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    )

    fun shiftDateTime(exif: ExifInterface, minutes: Long): ExifWriteResult {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
        if (raw == null) {
            return ExifWriteResult(
                changed = false,
                verified = false,
                warnings = listOf("no date-time tag to shift"),
            )
        }
        val shifted = shiftTimestamp(raw, minutes)
            ?: return ExifWriteResult(
                changed = false,
                verified = false,
                warnings = listOf("unparseable date-time value: $raw"),
            )
        val tag = if (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null) {
            ExifInterface.TAG_DATETIME_ORIGINAL
        } else {
            ExifInterface.TAG_DATETIME_DIGITIZED
        }
        exif.setAttribute(tag, shifted)
        val result = writeAndVerify(exif, tag, shifted)
        return result.copy(changed = true)
    }

    fun stripGps(exif: ExifInterface): ExifWriteResult {
        val latLongBefore = FloatArray(2)
        val hadGps = exif.getLatLong(latLongBefore)
        for (tag in gpsTags) {
            exif.setAttribute(tag, null)
        }
        val result = writeAndVerify(exif, ExifInterface.TAG_GPS_LATITUDE, null)
        val latLongAfter = FloatArray(2)
        val gpsGone = !exif.getLatLong(latLongAfter) &&
            !exif.hasAttribute(ExifInterface.TAG_GPS_LATITUDE) &&
            !exif.hasAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        return result.copy(
            changed = hadGps || result.changed,
            verified = result.verified && gpsGone,
            warnings = result.warnings + if (gpsGone) {
                emptyList()
            } else {
                listOf("GPS still readable after strip")
            },
        )
    }

    fun setOrientation(exif: ExifInterface, orientation: Int): ExifWriteResult {
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        return writeAndVerify(exif, ExifInterface.TAG_ORIENTATION, orientation.toString())
    }

    private fun shiftTimestamp(raw: String, minutes: Long): String? {
        val date = SimpleDateFormat(DATE_FORMAT, Locale.US).parse(raw) ?: return null
        val shifted = date.time + minutes * MINUTES_PER_HOUR * 1000L
        return SimpleDateFormat(DATE_FORMAT, Locale.US).format(java.util.Date(shifted))
    }

    private fun writeAndVerify(
        exif: ExifInterface,
        tag: String,
        expected: String?,
    ): ExifWriteResult {
        val warnings = mutableListOf<String>()
        try {
            exif.saveAttributes()
        } catch (e: IOException) {
            return ExifWriteResult(
                changed = expected != null,
                verified = false,
                warnings = listOf("save failed: ${e.message}"),
            )
        }
        val actual = exif.getAttribute(tag)
        val verified = if (expected == null) {
            actual == null
        } else {
            actual == expected
        }
        if (!verified) {
            warnings.add("read-back mismatch on $tag: expected=$expected actual=$actual")
        }
        return ExifWriteResult(changed = expected != null, verified = verified, warnings = warnings)
    }
}
