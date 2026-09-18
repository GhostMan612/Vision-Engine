// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.thumbs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

data class DecodedThumb(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DecodedThumb) {
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

fun sampleSizeForBounds(
    width: Int,
    height: Int,
    bound: Int,
): Int {
    var sample = 1
    while (width / (sample * 2) >= bound && height / (sample * 2) >= bound) {
        sample *= 2
    }
    return sample
}

fun rotatedSize(width: Int, height: Int, orientation: Int?): Pair<Int, Int> {
    return if (orientation == 5 || orientation == 6 ||
        orientation == 7 || orientation == 8
    ) {
        height to width
    } else {
        width to height
    }
}

fun targetSize(width: Int, height: Int, targetWidth: Int): Pair<Int, Int> {
    val scaledHeight = (targetWidth.toLong() * height / width).toInt()
    return targetWidth to scaledHeight
}

object PhotoThumbs {
    fun decodeThumbnail(
        bytes: ByteArray,
        orientation: Int?,
        targetWidth: Int = 256,
        maxSourceBytes: Long = 64L * 1024L * 1024L,
        boundSide: Int = 1024,
    ): DecodedThumb? {
        if (bytes.size > maxSourceBytes) {
            return null
        }
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        } catch (e: Exception) {
            return null
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSizeForBounds(
            bounds.outWidth,
            bounds.outHeight,
            boundSide,
        )
        val decoded = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        } ?: return null
        try {
            val rotated = applyOrientation(decoded, orientation)
            if (rotated !== decoded) {
                decoded.recycle()
            }
            val (targetW, targetH) = targetSize(
                rotated.width,
                rotated.height,
                targetWidth,
            )
            val scaled = Bitmap.createScaledBitmap(
                rotated,
                targetW,
                targetH,
                true,
            )
            if (scaled !== rotated) {
                rotated.recycle()
            }
            val out = ByteArrayOutputStream()
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                scaled.recycle()
                return null
            }
            val encoded = out.toByteArray()
            val result = DecodedThumb(encoded, scaled.width, scaled.height)
            scaled.recycle()
            return result
        } catch (e: Exception) {
            return null
        }
    }

    fun applyOrientation(source: Bitmap, orientation: Int?): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.postScale(-1f, 1f)
            3 -> matrix.postRotate(180f)
            4 -> matrix.postScale(1f, -1f)
            5 -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            6 -> matrix.postRotate(90f)
            7 -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            8 -> matrix.postRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true,
        )
    }
}
