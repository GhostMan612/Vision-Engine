// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun photoOf(
    orientation: Long? = null,
    width: Long? = null,
    height: Long? = null,
): PhotoMeta = PhotoMeta(
    datetimeOriginal = MetaField(null, Provenance.unknown),
    gpsLatitude = MetaField(null, Provenance.unknown),
    gpsLongitude = MetaField(null, Provenance.unknown),
    orientation = MetaField(orientation, Provenance.exif),
    make = MetaField(null, Provenance.unknown),
    model = MetaField(null, Provenance.unknown),
    exposureTime = MetaField(null, Provenance.unknown),
    widthRaw = MetaField(width, Provenance.exif),
    heightRaw = MetaField(height, Provenance.exif),
    fileSizeBytes = MetaField(null, Provenance.unknown),
    mime = MetaField(null, Provenance.unknown),
)

class OrientationTest {
    @Test
    fun oneToFourKeepAxes() {
        for (orientation in listOf(1L, 2L, 3L, 4L)) {
            assertFalse(orientationSwapsAxes(orientation))
        }
    }

    @Test
    fun fiveToEightSwapAxes() {
        for (orientation in listOf(5L, 6L, 7L, 8L)) {
            assertTrue(orientationSwapsAxes(orientation))
        }
    }

    @Test
    fun nullAndOutOfRangeNeverSwap() {
        assertFalse(orientationSwapsAxes(null))
        assertFalse(orientationSwapsAxes(0L))
        assertFalse(orientationSwapsAxes(9L))
    }

    @Test
    fun orientationSixReportsDisplaySize() {
        val meta = photoOf(orientation = 6L, width = 3000L, height = 4000L)
        assertEquals(4000L, meta.width)
        assertEquals(3000L, meta.height)
    }

    @Test
    fun orientationOneKeepsStoredSize() {
        val meta = photoOf(orientation = 1L, width = 3000L, height = 4000L)
        assertEquals(3000L, meta.width)
        assertEquals(4000L, meta.height)
    }

    @Test
    fun missingRawDimensionsYieldNull() {
        val meta = photoOf(orientation = 6L, width = null, height = 4000L)
        assertNull(meta.width)
        assertNull(meta.height)
    }
}

fun videoOf(
    rotation: Long? = null,
    width: Long? = null,
    height: Long? = null,
): VideoMeta = VideoMeta(
    durationMs = MetaField(null, Provenance.unknown),
    widthRaw = MetaField(width, Provenance.container),
    heightRaw = MetaField(height, Provenance.container),
    rotation = MetaField(rotation, Provenance.container),
    creationTime = MetaField(null, Provenance.unknown),
    locationLatitude = MetaField(null, Provenance.unknown),
    locationLongitude = MetaField(null, Provenance.unknown),
    codec = MetaField(null, Provenance.unknown),
    fileSizeBytes = MetaField(null, Provenance.unknown),
    mime = MetaField(null, Provenance.unknown),
)

class RotationTest {
    @Test
    fun ninetyAndTwoSeventySwapAxes() {
        assertTrue(rotationSwapsAxes(90L))
        assertTrue(rotationSwapsAxes(270L))
    }

    @Test
    fun zeroOneEightyNullAndOthersKeepAxes() {
        assertFalse(rotationSwapsAxes(0L))
        assertFalse(rotationSwapsAxes(180L))
        assertFalse(rotationSwapsAxes(null))
        assertFalse(rotationSwapsAxes(360L))
    }

    @Test
    fun rotationNinetyReportsDisplaySize() {
        val meta = videoOf(rotation = 90L, width = 1920L, height = 1080L)
        assertEquals(1080L, meta.width)
        assertEquals(1920L, meta.height)
    }

    @Test
    fun rotationZeroKeepsStoredSize() {
        val meta = videoOf(rotation = 0L, width = 1920L, height = 1080L)
        assertEquals(1920L, meta.width)
        assertEquals(1080L, meta.height)
    }

    @Test
    fun missingRawDimensionsYieldNull() {
        val meta = videoOf(rotation = 90L, width = null, height = 1080L)
        assertNull(meta.width)
        assertNull(meta.height)
    }
}
