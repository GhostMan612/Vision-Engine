// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import com.visionengine.android.thumbs.PhotoThumbs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CorruptImageNativeGraphicsTest {
    @Test
    fun decodeThumbnailRejectsBadJpgBytes() {
        val decoded = PhotoThumbs.decodeThumbnail(
            byteArrayOf(1, 2, 3, 4, 5, 6),
            null,
            256,
            64L * 1024L * 1024L,
        )
        assertNull(decoded)
    }

    @Test
    fun decodeThumbnailDecodesGoodFixture() {
        val bytes = javaClass.classLoader!!
            .getResourceAsStream("fixtures/ve_thumb.jpg")!!
            .use { it.readBytes() }
        val decoded = PhotoThumbs.decodeThumbnail(
            bytes,
            6,
            256,
            64L * 1024L * 1024L,
        ) ?: throw AssertionError("good fixture did not decode")
        assertEquals(256, decoded.width)
        assertEquals(384, decoded.height)
    }
}
