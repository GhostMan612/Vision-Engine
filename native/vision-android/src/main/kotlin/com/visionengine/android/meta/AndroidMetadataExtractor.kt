// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.meta

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.visionengine.core.ExtractStatus
import java.io.File

class AndroidMetadataExtractor(private val resolver: ContentResolver) {
    fun probePhotoFile(path: String, mime: String?): PhotoResult {
        val file = File(path)
        if (!file.isFile) {
            return PhotoResult(
                emptyPhoto(),
                ExtractStatus.unreadable,
                listOf("UNREADABLE: not a file"),
            )
        }
        return try {
            val exif = ExifInterface(path)
            ExifReader.readPhoto(exif, file.length(), mime)
        } catch (e: Exception) {
            PhotoResult(
                emptyPhoto(),
                ExtractStatus.unreadable,
                listOf("UNREADABLE"),
            )
        }
    }

    fun probePhotoUri(uri: Uri, sizeBytes: Long?, mime: String?): PhotoResult {
        return try {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                ExifReader.readPhoto(exif, sizeBytes, mime)
            } ?: PhotoResult(
                emptyPhoto(),
                ExtractStatus.unreadable,
                listOf("UNREADABLE: no descriptor"),
            )
        } catch (e: Exception) {
            PhotoResult(
                emptyPhoto(),
                ExtractStatus.unreadable,
                listOf("UNREADABLE"),
            )
        }
    }

    fun probeVideoFile(path: String, mime: String?): VideoResult {
        val file = File(path)
        if (!file.isFile) {
            return VideoResult(
                emptyVideo(),
                ExtractStatus.unreadable,
                listOf("UNREADABLE: not a file"),
            )
        }
        val retriever = MediaMetadataRetriever()
        return try {
            try {
                retriever.setDataSource(path)
            } catch (e: Exception) {
                return VideoResult(
                    emptyVideo(),
                    ExtractStatus.unreadable,
                    listOf("UNREADABLE"),
                )
            }
            VideoReader.readVideo(
                { key -> retriever.extractMetadata(key) },
                file.length(),
                mime,
            )
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

    fun probeVideoUri(
        uri: Uri,
        sizeBytes: Long?,
        mime: String?,
    ): VideoResult {
        val descriptor = try {
            resolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            null
        } ?: return VideoResult(
            emptyVideo(),
            ExtractStatus.unreadable,
            listOf("UNREADABLE: no descriptor"),
        )
        val retriever = MediaMetadataRetriever()
        return try {
            try {
                retriever.setDataSource(descriptor.fileDescriptor)
            } catch (e: Exception) {
                return VideoResult(
                    emptyVideo(),
                    ExtractStatus.unreadable,
                    listOf("UNREADABLE"),
                )
            }
            VideoReader.readVideo(
                { key -> retriever.extractMetadata(key) },
                sizeBytes,
                mime,
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                android.util.Log.w(
                    "VisionEngine",
                    "retriever release: ${e.message}",
                )
            }
            try {
                descriptor.close()
            } catch (e: Exception) {
                android.util.Log.w(
                    "VisionEngine",
                    "descriptor close: ${e.message}",
                )
            }
        }
    }
}
