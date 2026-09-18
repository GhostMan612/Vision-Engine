// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import androidx.exifinterface.media.ExifInterface
import com.visionengine.core.ExtractStatus
import com.visionengine.core.Provenance
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExifReaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var jpeg: File
    private lateinit var png: File

    @Before
    fun stage() {
        jpeg = File(temporaryFolder.root, "ve_exif.jpg")
        resourceBytes("fixtures/ve_exif.jpg").let(jpeg::writeBytes)
        png = File(temporaryFolder.root, "ve_plain.png")
        resourceBytes("fixtures/ve_plain.png").let(png::writeBytes)
    }

    @Test
    fun syntheticExifPhotoExtractsExactly() {
        val result = ExifReader.readPhoto(
            ExifInterface(jpeg.absolutePath),
            jpeg.length(),
            "image/jpeg",
        )
        assertEquals(ExtractStatus.ok, result.status)
        assertTrue(result.warnings.isEmpty())
        val meta = result.meta
        assertEquals("2026:09:14 11:05:23", meta.datetimeOriginal.value)
        assertEquals(Provenance.exif, meta.datetimeOriginal.provenance)
        assertEquals("SYNTHETIC", meta.make.value)
        assertEquals("SAMPLE CAM", meta.model.value)
        assertEquals(6L, meta.orientation.value)
        assertEquals("0.008333333333333333", meta.exposureTime.value)
        assertEquals(3000L, meta.widthRaw.value)
        assertEquals(4000L, meta.heightRaw.value)
        assertEquals(4000L, meta.width)
        assertEquals(3000L, meta.height)
        assertTrue(meta.gpsLatitude.value != null)
        assertTrue((meta.gpsLatitude.value!! - 44.9778) < 0.002)
        assertTrue((meta.gpsLongitude.value!! + 93.2650) < 0.002)
        assertEquals("image/jpeg", meta.mime.value)
        assertEquals(Provenance.filesystem, meta.mime.provenance)
        assertEquals(jpeg.length(), meta.fileSizeBytes.value)
    }

    @Test
    fun exifLessPngYieldsUnknowns() {
        val result = ExifReader.readPhoto(
            ExifInterface(png.absolutePath),
            png.length(),
            "image/png",
        )
        val meta = result.meta
        assertNull(meta.datetimeOriginal.value)
        assertEquals(Provenance.unknown, meta.datetimeOriginal.provenance)
        assertNull(meta.orientation.value)
        assertNull(meta.width)
        assertEquals("image/png", meta.mime.value)
        assertEquals(png.length(), meta.fileSizeBytes.value)
    }

    @Test
    fun orientationFieldRules() {
        assertEquals(null, ExifReader.orientationField(null).first.value)
        val junk = ExifReader.orientationField("sideways")
        assertNull(junk.first.value)
        assertEquals(1, junk.second?.let { listOf(it).size })
        val zero = ExifReader.orientationField("0")
        assertNull(zero.first.value)
        assertEquals(Provenance.unknown, zero.first.provenance)
        val nine = ExifReader.orientationField("9")
        assertEquals(9L, nine.first.value)
        assertEquals(Provenance.exif, nine.first.provenance)
    }

    @Test
    fun dimensionFieldRules() {
        assertNull(ExifReader.dimensionField(null).first.value)
        val junk = ExifReader.dimensionField("wide")
        assertNull(junk.first.value)
        assertTrue(junk.second != null)
        val zero = ExifReader.dimensionField("0")
        assertNull(zero.first.value)
        val good = ExifReader.dimensionField("3000")
        assertEquals(3000L, good.first.value)
        assertEquals(Provenance.exif, good.first.provenance)
    }

    private fun resourceBytes(name: String): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
            ?: throw IllegalStateException("missing fixture: $name")
        return stream.readBytes()
    }
}
