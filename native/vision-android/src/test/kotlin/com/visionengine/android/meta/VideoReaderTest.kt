// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import android.media.MediaMetadataRetriever
import com.visionengine.core.ExtractStatus
import com.visionengine.core.Provenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun scripted(vararg pairs: Pair<Int, String?>): (Int) -> String? {
    val fields = mapOf(*pairs)
    return { key -> fields[key] }
}

class VideoReaderTest {
    @Test
    fun fullPayloadDecodesWithProvenance() {
        val result = VideoReader.readVideo(
            scripted(
                MediaMetadataRetriever.METADATA_KEY_DURATION to "8340",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "1920",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "1080",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "90",
                MediaMetadataRetriever.METADATA_KEY_DATE to
                    "2026-09-14T11:05:23.000",
                MediaMetadataRetriever.METADATA_KEY_LOCATION to
                    "+44.9778-093.2650",
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "video/avc",
            ),
            11800211L,
            "video/mp4",
        )
        assertEquals(ExtractStatus.ok, result.status)
        assertTrue(result.warnings.isEmpty())
        val meta = result.meta
        assertEquals(8340L, meta.durationMs.value)
        assertEquals(Provenance.container, meta.durationMs.provenance)
        assertEquals(90L, meta.rotation.value)
        assertEquals(44.9778, meta.locationLatitude.value!!, 0.0)
        assertEquals(-93.265, meta.locationLongitude.value!!, 0.0)
        assertEquals(Provenance.container, meta.locationLatitude.provenance)
        assertEquals("video/avc", meta.codec.value)
        assertEquals("video/mp4", meta.mime.value)
        assertEquals(Provenance.filesystem, meta.mime.provenance)
        assertEquals(1080L, meta.width)
        assertEquals(1920L, meta.height)
    }

    @Test
    fun missingLocationStaysUnknownSilently() {
        val result = VideoReader.readVideo(
            scripted(MediaMetadataRetriever.METADATA_KEY_DURATION to "1000"),
            10L,
            "video/mp4",
        )
        assertEquals(ExtractStatus.ok, result.status)
        assertTrue(result.warnings.isEmpty())
        assertNull(result.meta.locationLatitude.value)
        assertEquals(
            Provenance.unknown,
            result.meta.locationLatitude.provenance,
        )
    }

    @Test
    fun malformedLocationWarns() {
        val result = VideoReader.readVideo(
            scripted(
                MediaMetadataRetriever.METADATA_KEY_DURATION to "1000",
                MediaMetadataRetriever.METADATA_KEY_LOCATION to "near the lake",
            ),
            10L,
            "video/mp4",
        )
        assertEquals(1, result.warnings.size)
        assertNull(result.meta.locationLatitude.value)
        assertEquals(
            Provenance.unknown,
            result.meta.locationLatitude.provenance,
        )
    }

    @Test
    fun junkDurationAndRotationWarn() {
        val result = VideoReader.readVideo(
            scripted(
                MediaMetadataRetriever.METADATA_KEY_DURATION to "long",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "up",
            ),
            10L,
            "video/mp4",
        )
        assertEquals(2, result.warnings.size)
        assertNull(result.meta.durationMs.value)
        assertNull(result.meta.rotation.value)
        assertEquals(ExtractStatus.partial, result.status)
    }

    @Test
    fun emptyPayloadStaysPartial() {
        val result = VideoReader.readVideo({ null }, 10L, "video/mp4")
        assertEquals(ExtractStatus.partial, result.status)
    }

    @Test
    fun nullSizeAndMimeStayUnknown() {
        val result = VideoReader.readVideo(
            scripted(MediaMetadataRetriever.METADATA_KEY_DURATION to "5"),
            null,
            null,
        )
        assertNull(result.meta.fileSizeBytes.value)
        assertEquals(Provenance.unknown, result.meta.fileSizeBytes.provenance)
        assertNull(result.meta.mime.value)
        assertEquals(Provenance.unknown, result.meta.mime.provenance)
        assertEquals(ExtractStatus.ok, result.status)
    }

    @Test
    fun zeroDurationStaysKnown() {
        val result = VideoReader.readVideo(
            scripted(MediaMetadataRetriever.METADATA_KEY_DURATION to "0"),
            150L,
            "video/mp4",
        )
        assertEquals(0L, result.meta.durationMs.value)
        assertEquals(Provenance.container, result.meta.durationMs.provenance)
    }
}
