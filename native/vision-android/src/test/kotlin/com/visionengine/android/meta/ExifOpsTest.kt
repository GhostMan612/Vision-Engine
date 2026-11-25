// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExifOpsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var jpeg: File

    private fun stage(): File {
        jpeg = File(temporaryFolder.root, "ve_exif.jpg")
        resourceBytes("fixtures/ve_exif.jpg").let(jpeg::writeBytes)
        return jpeg
    }

    @Test
    fun shiftDateTimeShiftsOriginalAndPreservesOtherTags() {
        val file = stage()
        val exif = ExifInterface(file.absolutePath)
        val makeBefore = exif.getAttribute(ExifInterface.TAG_MAKE)
        val modelBefore = exif.getAttribute(ExifInterface.TAG_MODEL)
        val originalBefore = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val result = ExifOps.shiftDateTime(exif, 90)
        assertTrue(result.changed)
        assertTrue("read-back should verify: ${result.warnings}", result.verified)
        assertEquals(makeBefore, exif.getAttribute(ExifInterface.TAG_MAKE))
        assertEquals(modelBefore, exif.getAttribute(ExifInterface.TAG_MODEL))
        assertEquals(
            shiftExpected(originalBefore, 90),
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
        )
    }

    @Test
    fun stripGpsRemovesGpsOnReRead() {
        val file = stage()
        val exif = ExifInterface(file.absolutePath)
        assertTrue(exif.getLatLong(FloatArray(2)))
        val result = ExifOps.stripGps(exif)
        assertTrue(result.verified)
        val latLong = FloatArray(2)
        assertFalse(exif.getLatLong(latLong))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
    }

    @Test
    fun setOrientationWritesAndVerifies() {
        val file = stage()
        val exif = ExifInterface(file.absolutePath)
        val result = ExifOps.setOrientation(exif, 6)
        assertTrue(result.changed)
        assertTrue(result.verified)
        assertEquals("6", exif.getAttribute(ExifInterface.TAG_ORIENTATION))
    }

    private fun shiftExpected(raw: String?, minutes: Long): String {
        val date = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw!!)
        return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(
            java.util.Date(date.time + minutes * 60_000L),
        )
    }

    private fun resourceBytes(name: String): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
            ?: throw IllegalStateException("missing fixture: $name")
        return stream.readBytes()
    }
}
