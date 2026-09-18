// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.thumbs

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
class VideoFramesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun scriptedFrameScalesToBound() {
        val file = File(temporaryFolder.root, "VID_a.mp4")
        file.writeBytes(ByteArray(64))
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        ShadowMediaMetadataRetriever.addFrame(
            DataSource.toDataSource(file.absolutePath),
            0L,
            bitmap,
        )
        val frame = VideoFrames.grabFrame(file.absolutePath)
        assertTrue(frame != null)
        assertEquals(512, frame!!.width)
        assertEquals(384, frame.height)
        assertTrue(frame.bytes.isNotEmpty())
    }

    @Test
    fun unscriptedFileYieldsNull() {
        val file = File(temporaryFolder.root, "VID_b.mp4")
        file.writeBytes(ByteArray(64))
        assertNull(VideoFrames.grabFrame(file.absolutePath))
    }

    @Test
    fun missingFileYieldsNullWithoutTouchingRetriever() {
        assertNull(VideoFrames.grabFrame("/t/nope.mp4"))
    }
}
