// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.thumbs

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File

data class VideoFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is VideoFrame) {
            return false
        }
        return width == other.width &&
            height == other.height &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

object VideoFrames {
    fun grabFrame(
        path: String,
        positionUs: Long = 0L,
        maxSide: Int = 512,
    ): VideoFrame? {
        val file = File(path)
        if (!file.isFile) {
            return null
        }
        val retriever = MediaMetadataRetriever()
        try {
            try {
                retriever.setDataSource(path)
            } catch (e: Exception) {
                return null
            }
            val frame = retriever.getFrameAtTime(
                positionUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: return null
            try {
                val scale = minOf(
                    maxSide.toFloat() / frame.width,
                    maxSide.toFloat() / frame.height,
                    1f,
                )
                val target = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        frame,
                        (frame.width * scale).toInt(),
                        (frame.height * scale).toInt(),
                        true,
                    )
                } else {
                    frame
                }
                if (target !== frame) {
                    frame.recycle()
                }
                val out = ByteArrayOutputStream()
                try {
                    if (!target.compress(
                            Bitmap.CompressFormat.JPEG,
                            85,
                            out,
                        )
                    ) {
                        target.recycle()
                        return null
                    }
                    val encoded = out.toByteArray()
                    val result = VideoFrame(encoded, target.width, target.height)
                    target.recycle()
                    return result
                } finally {
                    out.close()
                }
            } catch (e: Exception) {
                return null
            }
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                android.util.Log.w(
                    "VisionEngine",
                    "retriever release: ${e.message}",
                )
            }
        }
    }
}
