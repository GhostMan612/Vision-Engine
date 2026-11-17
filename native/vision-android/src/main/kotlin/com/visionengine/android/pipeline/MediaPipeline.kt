// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.visionengine.android.meta.AndroidMetadataExtractor
import com.visionengine.android.thumbs.PhotoThumbs
import com.visionengine.android.thumbs.VideoFrames
import com.visionengine.core.CachedThumb
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbInfo
import com.visionengine.core.ThumbStatus
import com.visionengine.core.ThumbnailCache
import com.visionengine.core.pageOf
import com.visionengine.core.thumbKey
import com.visionengine.core.videoMetaFromMap
import com.visionengine.core.photoMetaFromMap
import java.io.File

data class ThumbFetch(
    val info: ThumbInfo,
    val bytes: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ThumbFetch) {
            return false
        }
        return info == other.info &&
            (bytes == null && other.bytes == null ||
                bytes != null && other.bytes != null &&
                bytes.contentEquals(other.bytes))
    }

    override fun hashCode(): Int {
        var result = info.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

class MediaPipeline(
    private val resolver: ContentResolver,
    val cache: ThumbnailCache = ThumbnailCache(),
    val maxDimension: Int = 256,
    val maxSourceBytes: Long = 64L * 1024L * 1024L,
) : ViewerPipeline {
    private val extractor = AndroidMetadataExtractor(resolver)

    override fun loadPage(
        sources: List<MediaSource>,
        offset: Int,
        limit: Int,
    ): List<MediaRecord> {
        val page = pageOf(sources, offset = offset, limit = limit)
        val records = mutableListOf<MediaRecord>()
        for (index in page.indices) {
            records.add(recordFor(page[index], offset + index))
        }
        return records
    }

    override fun thumbnailFor(record: MediaRecord): ThumbFetch {
        if (record.kind == MediaKind.unsupported) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unsupported type"),
                null,
            )
        }
        val key = thumbKey(
            sourceId = record.source.id,
            sizeBytes = record.source.sizeBytes,
            modifiedMs = record.source.modifiedMs,
            maxDimension = maxDimension,
        )
        val filePath = record.source.filePath
        if (filePath != null) {
            return if (record.kind == MediaKind.video) {
                fetchVideoThumb(File(filePath), key)
            } else {
                fetchPhotoThumb(File(filePath), key, record)
            }
        }
        val uri = record.source.documentUri
        if (uri == null) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "no local path"),
                null,
            )
        }
        return if (record.kind == MediaKind.video) {
            fetchVideoThumbUri(Uri.parse(uri), key)
        } else {
            fetchPhotoThumbUri(Uri.parse(uri), key, record)
        }
    }

    private fun recordFor(source: MediaSource, sortIndex: Int): MediaRecord {
        when (source.kind) {
            MediaKind.photo -> {
                val path = source.filePath
                if (path != null) {
                    val probed = extractor.probePhotoFile(path, source.mime)
                    return MediaRecord.fromPhoto(
                        source = source,
                        sortIndex = sortIndex,
                        photo = probed.meta,
                        status = probed.status,
                        warnings = probed.warnings,
                    )
                }
                val uri = source.documentUri
                if (uri != null) {
                    val probed = extractor.probePhotoUri(
                        Uri.parse(uri),
                        source.sizeBytes,
                        source.mime,
                    )
                    return MediaRecord.fromPhoto(
                        source = source,
                        sortIndex = sortIndex,
                        photo = probed.meta,
                        status = probed.status,
                        warnings = probed.warnings,
                    )
                }
                return MediaRecord.fromPhoto(
                    source = source,
                    sortIndex = sortIndex,
                    photo = photoMetaFromMap(emptyMap()),
                    status = ExtractStatus.unreadable,
                    warnings = listOf("no local path"),
                )
            }
            MediaKind.video -> {
                val path = source.filePath
                if (path != null) {
                    val probed = extractor.probeVideoFile(path, source.mime)
                    return MediaRecord.fromVideo(
                        source = source,
                        sortIndex = sortIndex,
                        video = probed.meta,
                        status = probed.status,
                        warnings = probed.warnings,
                    )
                }
                val uri = source.documentUri
                if (uri != null) {
                    val probed = extractor.probeVideoUri(
                        Uri.parse(uri),
                        source.sizeBytes,
                        source.mime,
                    )
                    return MediaRecord.fromVideo(
                        source = source,
                        sortIndex = sortIndex,
                        video = probed.meta,
                        status = probed.status,
                        warnings = probed.warnings,
                    )
                }
                return MediaRecord.fromVideo(
                    source = source,
                    sortIndex = sortIndex,
                    video = videoMetaFromMap(emptyMap()),
                    status = ExtractStatus.unreadable,
                    warnings = listOf("no local path"),
                )
            }
            MediaKind.unsupported -> return MediaRecord.unsupported(
                source = source,
                sortIndex = sortIndex,
            )
        }
    }

    private fun cachedThumbFetch(key: String): ThumbFetch? {
        val hit = cache.get(key) ?: return null
        return ThumbFetch(
            ThumbInfo(
                ThumbStatus.ready,
                key = key,
                width = hit.width,
                height = hit.height,
            ),
            hit.bytes,
        )
    }

    private fun readyThumbFetch(
        key: String,
        bytes: ByteArray,
        width: Int,
        height: Int,
    ): ThumbFetch {
        cache.put(CachedThumb(key, bytes, width, height))
        return ThumbFetch(
            ThumbInfo(
                ThumbStatus.ready,
                key = key,
                width = width,
                height = height,
            ),
            bytes,
        )
    }

    private fun fetchPhotoThumb(
        file: File,
        key: String,
        record: MediaRecord,
    ): ThumbFetch {
        cachedThumbFetch(key)?.let { return it }
        val orientation = record.photo?.orientation?.value?.toInt()
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unreadable"),
                null,
            )
        }
        return decodePhotoThumb(bytes, orientation, key)
    }

    private fun fetchPhotoThumbUri(
        uri: Uri,
        key: String,
        record: MediaRecord,
    ): ThumbFetch {
        cachedThumbFetch(key)?.let { return it }
        val orientation = record.photo?.orientation?.value?.toInt()
        val bytes = readPhotoSourceBytes(uri)
            ?: return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unreadable"),
                null,
            )
        return decodePhotoThumb(bytes, orientation, key)
    }

    private fun readPhotoSourceBytes(uri: Uri): ByteArray? {
        val descriptor: ParcelFileDescriptor
        try {
            descriptor = resolver.openFileDescriptor(uri, "r") ?: return null
        } catch (e: Exception) {
            return null
        }
        try {
            val input = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                total += read
                if (total > maxSourceBytes) {
                    return null
                }
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        } catch (e: Exception) {
            return null
        } finally {
            try {
                descriptor.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun decodePhotoThumb(
        bytes: ByteArray,
        orientation: Int?,
        key: String,
    ): ThumbFetch {
        val decoded = try {
            PhotoThumbs.decodeThumbnail(
                bytes,
                orientation,
                maxDimension,
                maxSourceBytes,
            )
        } catch (e: Exception) {
            null
        } ?: return ThumbFetch(
            ThumbInfo(ThumbStatus.failed, reason = "decode failed"),
            null,
        )
        return readyThumbFetch(key, decoded.bytes, decoded.width, decoded.height)
    }

    private fun fetchVideoThumb(file: File, key: String): ThumbFetch {
        cachedThumbFetch(key)?.let { return it }
        val frame = try {
            VideoFrames.grabFrame(file.absolutePath)
        } catch (e: Exception) {
            null
        } ?: return ThumbFetch(
            ThumbInfo(ThumbStatus.unavailable, reason = "no frame"),
            null,
        )
        return readyThumbFetch(key, frame.bytes, frame.width, frame.height)
    }

    private fun fetchVideoThumbUri(uri: Uri, key: String): ThumbFetch {
        cachedThumbFetch(key)?.let { return it }
        val descriptor: ParcelFileDescriptor
        try {
            descriptor = resolver.openFileDescriptor(uri, "r") ?: return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unreadable"),
                null,
            )
        } catch (e: Exception) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unreadable"),
                null,
            )
        }
        try {
            val frame = VideoFrames.grabFrameDescriptor(descriptor.fileDescriptor)
                ?: return ThumbFetch(
                    ThumbInfo(ThumbStatus.unavailable, reason = "no frame"),
                    null,
                )
            return readyThumbFetch(key, frame.bytes, frame.width, frame.height)
        } catch (e: Exception) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "no frame"),
                null,
            )
        } finally {
            try {
                descriptor.close()
            } catch (e: Exception) {
            }
        }
    }
}
