// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import android.media.MediaMetadataRetriever
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MetaField
import com.visionengine.core.Provenance
import com.visionengine.core.VideoMeta
import com.visionengine.core.videoStatus

val iso6709 = Regex("^([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)$")

fun emptyVideo(): VideoMeta = VideoMeta(
    durationMs = MetaField(null, Provenance.unknown),
    widthRaw = MetaField(null, Provenance.unknown),
    heightRaw = MetaField(null, Provenance.unknown),
    rotation = MetaField(null, Provenance.unknown),
    creationTime = MetaField(null, Provenance.unknown),
    locationLatitude = MetaField(null, Provenance.unknown),
    locationLongitude = MetaField(null, Provenance.unknown),
    codec = MetaField(null, Provenance.unknown),
    fileSizeBytes = MetaField(null, Provenance.unknown),
    mime = MetaField(null, Provenance.unknown),
)

object VideoReader {
    fun intField(raw: String?): Pair<MetaField<Long>, String?> {
        if (raw == null) {
            return MetaField<Long>(null, Provenance.unknown) to null
        }
        val parsed = raw.toLongOrNull()
        if (parsed == null) {
            return MetaField<Long>(null, Provenance.unknown) to
                "value ignored: $raw"
        }
        return MetaField(parsed, Provenance.container) to null
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
        return MetaField(parsed, Provenance.container) to null
    }

    fun locationFields(raw: String?): Triple<Double?, Double?, String?> {
        if (raw == null) {
            return Triple(null, null, null)
        }
        val match = iso6709.matchEntire(raw.trim())
        val latitude = match?.groupValues?.get(1)?.toDoubleOrNull()
        val longitude = match?.groupValues?.get(2)?.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            return Triple(null, null, "location value ignored: $raw")
        }
        return Triple(latitude, longitude, null)
    }

    fun readVideo(
        get: (Int) -> String?,
        fileSizeBytes: Long?,
        mime: String?,
    ): VideoResult {
        val warnings = mutableListOf<String>()
        val duration = intField(get(MediaMetadataRetriever.METADATA_KEY_DURATION))
        warnings.addNotNull(duration.second)
        val width = dimensionField(get(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        warnings.addNotNull(width.second)
        val height = dimensionField(get(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        warnings.addNotNull(height.second)
        val rotation = intField(get(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
        warnings.addNotNull(rotation.second)
        val location = locationFields(get(MediaMetadataRetriever.METADATA_KEY_LOCATION))
        if (location.third != null) {
            warnings.add(location.third!!)
        }
        val creation = get(MediaMetadataRetriever.METADATA_KEY_DATE)
        val codec = get(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val meta = VideoMeta(
            durationMs = duration.first,
            widthRaw = width.first,
            heightRaw = height.first,
            rotation = rotation.first,
            creationTime = if (creation == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(creation, Provenance.container)
            },
            locationLatitude = if (location.first == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(location.first, Provenance.container)
            },
            locationLongitude = if (location.second == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(location.second, Provenance.container)
            },
            codec = if (codec == null) {
                MetaField(null, Provenance.unknown)
            } else {
                MetaField(codec, Provenance.container)
            },
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
        return VideoResult(meta, videoStatus(meta), warnings.toList())
    }

    private fun MutableList<String>.addNotNull(warning: String?) {
        if (warning != null) {
            add(warning)
        }
    }
}
