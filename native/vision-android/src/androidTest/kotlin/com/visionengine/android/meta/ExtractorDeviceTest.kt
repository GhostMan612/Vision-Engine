// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.visionengine.core.ExtractStatus
import com.visionengine.core.Provenance
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtractorDeviceTest {
    private lateinit var work: File
    private lateinit var extractor: AndroidMetadataExtractor

    @Before
    fun stage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        extractor = AndroidMetadataExtractor(context.contentResolver)
        val base = context.getDir("ve_mp4", Context.MODE_PRIVATE)
        work = File(base, "run-${System.currentTimeMillis()}")
        work.mkdirs()
        stageAsset("fixtures/ve_exif.jpg", "IMG_probe.jpg")
        stageAsset("fixtures/ve_plain.png", "shot.png")
        stageAsset("fixtures/ve_minimal.mp4", "VID_probe.mp4")
        File(work, "empty.jpg").writeBytes(ByteArray(0))
    }

    @After
    fun cleanup() {
        work.deleteRecursively()
    }

    @Test
    fun syntheticExifPhotoExtractsExactly() {
        val file = File(work, "IMG_probe.jpg")
        val result = extractor.probePhotoFile(file.absolutePath, "image/jpeg")
        assertEquals(ExtractStatus.ok, result.status)
        assertTrue(result.warnings.isEmpty())
        val meta = result.meta
        assertEquals("2026:09:14 11:05:23", meta.datetimeOriginal.value)
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
        assertEquals(file.length(), meta.fileSizeBytes.value)
    }

    @Test
    fun exifLessPngYieldsUnknowns() {
        val file = File(work, "shot.png")
        val result = extractor.probePhotoFile(file.absolutePath, "image/png")
        assertNull(result.meta.datetimeOriginal.value)
        assertEquals(Provenance.unknown, result.meta.datetimeOriginal.provenance)
        assertNull(result.meta.orientation.value)
        assertNull(result.meta.width)
        assertEquals("image/png", result.meta.mime.value)
        assertEquals(file.length(), result.meta.fileSizeBytes.value)
    }

    @Test
    fun missingFileIsUnreadable() {
        val result = extractor.probePhotoFile(
            File(work, "nope.jpg").absolutePath,
            "image/jpeg",
        )
        assertEquals(ExtractStatus.unreadable, result.status)
        assertNull(result.meta.make.value)
    }

    @Test
    fun corruptFileCompletesGracefully() {
        val file = File(work, "empty.jpg")
        val result = extractor.probePhotoFile(file.absolutePath, "image/jpeg")
        assertTrue(
            result.status == ExtractStatus.partial ||
                result.status == ExtractStatus.unreadable,
        )
    }

    @Test
    fun minimalContainerCompletesWithDeterministicShape() {
        val file = File(work, "VID_probe.mp4")
        val result = extractor.probeVideoFile(file.absolutePath, "video/mp4")
        assertEquals(file.length(), result.meta.fileSizeBytes.value)
        assertEquals("video/mp4", result.meta.mime.value)
        assertTrue(
            result.meta.codec.value == null ||
                result.meta.codec.provenance == Provenance.container,
        )
    }

    @Test
    fun sourcesRemainUntouched() {
        val before = snapshot()
        extractor.probePhotoFile(
            File(work, "IMG_probe.jpg").absolutePath,
            "image/jpeg",
        )
        extractor.probePhotoFile(
            File(work, "shot.png").absolutePath,
            "image/png",
        )
        extractor.probeVideoFile(
            File(work, "VID_probe.mp4").absolutePath,
            "video/mp4",
        )
        assertEquals(before, snapshot())
    }

    private fun stageAsset(asset: String, name: String) {
        val assets = InstrumentationRegistry.getInstrumentation()
            .context.assets
        assets.open(asset).use { input ->
            File(work, name).outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun snapshot(): String {
        val parts = mutableListOf<String>()
        work.walkTopDown().filter { it.isFile }.forEach { file ->
            var checksum = file.length()
            for (byte in file.readBytes()) {
                checksum = (checksum * 31 + byte) and 0xFFFFFFFFL
            }
            parts.add("${file.name}:$checksum")
        }
        return parts.sorted().joinToString("|")
    }
}
