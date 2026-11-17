// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFieldsTest {

    private fun thumbLikePhoto(): PhotoMeta {
        return PhotoMeta(
            datetimeOriginal = MetaField(null, Provenance.unknown),
            gpsLatitude = MetaField(null, Provenance.unknown),
            gpsLongitude = MetaField(null, Provenance.unknown),
            orientation = MetaField(6L, Provenance.exif),
            make = MetaField(null, Provenance.unknown),
            model = MetaField(null, Provenance.unknown),
            exposureTime = MetaField(null, Provenance.unknown),
            widthRaw = MetaField(24L, Provenance.exif),
            heightRaw = MetaField(16L, Provenance.exif),
            fileSizeBytes = MetaField(194L, Provenance.filesystem),
            mime = MetaField("image/jpeg", Provenance.filesystem),
        )
    }

    private fun unknownPhoto(): PhotoMeta {
        return PhotoMeta(
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
    }

    @Test
    fun photoFieldsAreElevenInSpecOrderWithCorrectedDimensions() {
        val fields = photoDetailFields(thumbLikePhoto())
        assertEquals(11, fields.size)
        assertEquals(
            listOf(
                "Date taken",
                "GPS latitude",
                "GPS longitude",
                "Orientation",
                "Make",
                "Model",
                "Exposure time",
                "Width",
                "Height",
                "File size",
                "MIME type",
            ),
            fields.map { it.label },
        )
        assertEquals("16", fields[7].value)
        assertEquals("24", fields[8].value)
        assertEquals("6", fields[3].value)
        assertEquals("194 B", fields[9].value)
        assertEquals("image/jpeg", fields[10].value)
    }

    @Test
    fun photoFieldsShowUnknownForMissingValuesWithProvenance() {
        val fields = photoDetailFields(thumbLikePhoto())
        assertEquals(UNKNOWN_VALUE, fields[0].value)
        assertEquals(Provenance.unknown, fields[0].provenance)
        assertEquals(UNKNOWN_VALUE, fields[1].value)
        assertEquals(UNKNOWN_VALUE, fields[2].value)
        assertEquals(UNKNOWN_VALUE, fields[4].value)
        assertEquals(UNKNOWN_VALUE, fields[5].value)
        assertEquals(UNKNOWN_VALUE, fields[6].value)
        assertEquals(Provenance.exif, fields[3].provenance)
        assertEquals(Provenance.exif, fields[7].provenance)
        assertEquals(Provenance.exif, fields[8].provenance)
        assertEquals(Provenance.filesystem, fields[9].provenance)
        assertEquals(Provenance.filesystem, fields[10].provenance)
    }

    @Test
    fun photoFieldsWithNoMetaAreAllUnknown() {
        val fields = photoDetailFields(unknownPhoto())
        assertEquals(11, fields.size)
        assertTrue(fields.all { it.value == UNKNOWN_VALUE })
        assertTrue(fields.all { it.provenance == Provenance.unknown })
    }

    @Test
    fun videoFieldsAreTenInSpecOrderWithRotationCorrectedDimensions() {
        val video = VideoMeta(
            durationMs = MetaField(8340L, Provenance.container),
            widthRaw = MetaField(1920L, Provenance.container),
            heightRaw = MetaField(1080L, Provenance.container),
            rotation = MetaField(90L, Provenance.container),
            creationTime = MetaField(null, Provenance.unknown),
            locationLatitude = MetaField(null, Provenance.unknown),
            locationLongitude = MetaField(null, Provenance.unknown),
            codec = MetaField("video/mp4", Provenance.container),
            fileSizeBytes = MetaField(150L, Provenance.filesystem),
            mime = MetaField("video/mp4", Provenance.filesystem),
        )
        val fields = videoDetailFields(video)
        assertEquals(10, fields.size)
        assertEquals(
            listOf(
                "Duration",
                "Width",
                "Height",
                "Rotation",
                "Creation time",
                "Location latitude",
                "Location longitude",
                "Codec",
                "File size",
                "MIME type",
            ),
            fields.map { it.label },
        )
        assertEquals("8.3 s", fields[0].value)
        assertEquals("1080", fields[1].value)
        assertEquals("1920", fields[2].value)
        assertEquals("90", fields[3].value)
        assertEquals(UNKNOWN_VALUE, fields[4].value)
        assertEquals(UNKNOWN_VALUE, fields[5].value)
        assertEquals(UNKNOWN_VALUE, fields[6].value)
        assertEquals("video/mp4", fields[7].value)
        assertEquals("150 B", fields[8].value)
        assertEquals("video/mp4", fields[9].value)
    }

    @Test
    fun formatDurationMsVectors() {
        assertEquals("0 ms", formatDurationMs(0L))
        assertEquals("999 ms", formatDurationMs(999L))
        assertEquals("1 s", formatDurationMs(1000L))
        assertEquals("8.3 s", formatDurationMs(8340L))
        assertEquals("12 s", formatDurationMs(12000L))
        assertEquals("61.5 s", formatDurationMs(61500L))
    }

    @Test
    fun formatFileSizeVectors() {
        assertEquals("0 B", formatFileSize(0L))
        assertEquals("194 B", formatFileSize(194L))
        assertEquals("1023 B", formatFileSize(1023L))
        assertEquals("1 KB", formatFileSize(1024L))
        assertEquals("1.5 KB", formatFileSize(1536L))
        assertEquals("41.5 MB", formatFileSize(43515904L))
        assertEquals("1 MB", formatFileSize(1048576L))
    }
}
