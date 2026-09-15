// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import java.io.File

object MetadataBridge {
    fun probePhoto(path: String): Map<String, Map<String, Any?>> {
        val file = File(path)
        if (!file.isFile) {
            throw IllegalArgumentException("UNREADABLE: not a file: $path")
        }
        val exif = try {
            ExifInterface(path)
        } catch (e: java.io.IOException) {
            throw IllegalArgumentException("UNREADABLE: ${e.message}")
        }
        val out = LinkedHashMap<String, Map<String, Any?>>()
        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
            out["datetimeOriginal"] = exifField(it)
        }
        val latLong = FloatArray(2)
        if (exif.getLatLong(latLong)) {
            out["gpsLatitude"] = exifField(latLong[0].toDouble())
            out["gpsLongitude"] = exifField(latLong[1].toDouble())
        }
        exif.getAttribute(ExifInterface.TAG_ORIENTATION)?.let {
            out["orientation"] = exifField(it)
        }
        exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
            out["make"] = exifField(it)
        }
        exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
            out["model"] = exifField(it)
        }
        exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
            out["exposureTime"] = exifField(it)
        }
        exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let {
            out["widthRaw"] = exifField(it)
        }
        exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let {
            out["heightRaw"] = exifField(it)
        }
        out["fileSizeBytes"] = fsField(file.length())
        return out
    }

    fun probeVideo(path: String): Map<String, Map<String, Any?>> {
        val file = File(path)
        if (!file.isFile) {
            throw IllegalArgumentException("UNREADABLE: not a file: $path")
        }
        val retriever = MediaMetadataRetriever()
        try {
            try {
                retriever.setDataSource(path)
            } catch (e: Exception) {
                throw IllegalArgumentException("UNREADABLE: ${e.message}")
            }
            val out = LinkedHashMap<String, Map<String, Any?>>()
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION,
            )?.let {
                out["durationMs"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
            )?.let {
                out["widthRaw"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
            )?.let {
                out["heightRaw"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
            )?.let {
                out["rotation"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DATE,
            )?.let {
                out["creationTime"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_LOCATION,
            )?.let {
                out["location"] = containerField(it)
            }
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE,
            )?.let {
                out["codec"] = containerField(it)
            }
            out["fileSizeBytes"] = fsField(file.length())
            return out
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                android.util.Log.w("VisionEngine", "retriever release: ${e.message}")
            }
        }
    }

    private fun exifField(value: Any): Map<String, Any?> {
        return mapOf("value" to value, "provenance" to "exif")
    }

    private fun containerField(value: Any): Map<String, Any?> {
        return mapOf("value" to value, "provenance" to "container")
    }

    private fun fsField(value: Any): Map<String, Any?> {
        return mapOf("value" to value, "provenance" to "filesystem")
    }
}
