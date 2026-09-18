// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.visionengine.core.ExtractStatus
import com.visionengine.core.Provenance
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaMetadataRetriever

@RunWith(RobolectricTestRunner::class)
class ExtractorLifecycleTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun extractor(): AndroidMetadataExtractor =
        AndroidMetadataExtractor(
            ApplicationProvider.getApplicationContext<Context>()
                .contentResolver,
        )

    @Test
    fun missingPhotoFileIsUnreadableWithoutTouchingExif() {
        val result = extractor().probePhotoFile("/t/nope.jpg", "image/jpeg")
        assertEquals(ExtractStatus.unreadable, result.status)
        assertNull(result.meta.make.value)
    }

    @Test
    fun missingVideoFileIsUnreadableWithoutTouchingRetriever() {
        val result = extractor().probeVideoFile("/t/nope.mp4", "video/mp4")
        assertEquals(ExtractStatus.unreadable, result.status)
        assertNull(result.meta.durationMs.value)
    }

    @Test
    fun scriptedRetrieverMapsEndToEnd() {
        val file = File(temporaryFolder.root, "VID_x.mp4")
        file.writeBytes(ByteArray(150))
        ShadowMediaMetadataRetriever.addMetadata(
            file.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_DURATION,
            "8340",
        )
        ShadowMediaMetadataRetriever.addMetadata(
            file.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
            "1920",
        )
        ShadowMediaMetadataRetriever.addMetadata(
            file.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
            "1080",
        )
        ShadowMediaMetadataRetriever.addMetadata(
            file.absolutePath,
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
            "90",
        )
        val result = extractor().probeVideoFile(file.absolutePath, "video/mp4")
        assertEquals(ExtractStatus.ok, result.status)
        assertEquals(8340L, result.meta.durationMs.value)
        assertEquals(
            Provenance.container,
            result.meta.durationMs.provenance,
        )
        assertEquals(90L, result.meta.rotation.value)
        assertEquals(1080L, result.meta.width)
        assertEquals(1920L, result.meta.height)
        assertEquals(file.length(), result.meta.fileSizeBytes.value)
        assertEquals("video/mp4", result.meta.mime.value)
    }

    @Test
    fun unscriptedVideoFileStaysGraceful() {
        val file = File(temporaryFolder.root, "VID_y.mp4")
        file.writeBytes(ByteArray(64))
        val result = extractor().probeVideoFile(file.absolutePath, "video/mp4")
        assertEquals(0L, result.meta.rotation.value)
        assertEquals(Provenance.container, result.meta.rotation.provenance)
        assertNull(result.meta.durationMs.value)
        assertEquals(ExtractStatus.ok, result.status)
        assertEquals(file.length(), result.meta.fileSizeBytes.value)
    }
}
