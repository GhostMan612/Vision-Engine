// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.pipeline

import android.content.ContentResolver
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
    resolver: ContentResolver,
    val cache: ThumbnailCache = ThumbnailCache(),
    val maxDimension: Int = 256,
    val maxSourceBytes: Long = 64L * 1024L * 1024L,
) {
    private val extractor = AndroidMetadataExtractor(resolver)

    fun loadPage(
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

    fun thumbnailFor(record: MediaRecord): ThumbFetch {
        if (record.kind == MediaKind.unsupported) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unsupported type"),
                null,
            )
        }
        val filePath = record.source.filePath
        if (filePath == null) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "no local path"),
                null,
            )
        }
        val key = thumbKey(
            sourceId = record.source.id,
            sizeBytes = record.source.sizeBytes,
            modifiedMs = record.source.modifiedMs,
            maxDimension = maxDimension,
        )
        return if (record.kind == MediaKind.video) {
            fetchVideoThumb(File(filePath), key)
        } else {
            fetchPhotoThumb(File(filePath), key, record)
        }
    }

    private fun recordFor(source: MediaSource, sortIndex: Int): MediaRecord {
        when (source.kind) {
            MediaKind.photo -> {
                val path = source.filePath
                if (path == null) {
                    return MediaRecord.fromPhoto(
                        source = source,
                        sortIndex = sortIndex,
                        photo = photoMetaFromMap(emptyMap()),
                        status = ExtractStatus.unreadable,
                        warnings = listOf("no local path"),
                    )
                }
                val probed = extractor.probePhotoFile(path, source.mime)
                return MediaRecord.fromPhoto(
                    source = source,
                    sortIndex = sortIndex,
                    photo = probed.meta,
                    status = probed.status,
                    warnings = probed.warnings,
                )
            }
            MediaKind.video -> {
                val path = source.filePath
                if (path == null) {
                    return MediaRecord.fromVideo(
                        source = source,
                        sortIndex = sortIndex,
                        video = videoMetaFromMap(emptyMap()),
                        status = ExtractStatus.unreadable,
                        warnings = listOf("no local path"),
                    )
                }
                val probed = extractor.probeVideoFile(path, source.mime)
                return MediaRecord.fromVideo(
                    source = source,
                    sortIndex = sortIndex,
                    video = probed.meta,
                    status = probed.status,
                    warnings = probed.warnings,
                )
            }
            MediaKind.unsupported -> return MediaRecord.unsupported(
                source = source,
                sortIndex = sortIndex,
            )
        }
    }

    private fun fetchPhotoThumb(
        file: File,
        key: String,
        record: MediaRecord,
    ): ThumbFetch {
        val hit = cache.get(key)
        if (hit != null) {
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
        val orientation = record.photo?.orientation?.value?.toInt()
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return ThumbFetch(
                ThumbInfo(ThumbStatus.unavailable, reason = "unreadable"),
                null,
            )
        }
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
        cache.put(
            CachedThumb(key, decoded.bytes, decoded.width, decoded.height),
        )
        return ThumbFetch(
            ThumbInfo(
                ThumbStatus.ready,
                key = key,
                width = decoded.width,
                height = decoded.height,
            ),
            decoded.bytes,
        )
    }

    private fun fetchVideoThumb(file: File, key: String): ThumbFetch {
        val hit = cache.get(key)
        if (hit != null) {
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
        val frame = try {
            VideoFrames.grabFrame(file.absolutePath)
        } catch (e: Exception) {
            null
        } ?: return ThumbFetch(
            ThumbInfo(ThumbStatus.unavailable, reason = "no frame"),
            null,
        )
        cache.put(
            CachedThumb(key, frame.bytes, frame.width, frame.height),
        )
        return ThumbFetch(
            ThumbInfo(
                ThumbStatus.ready,
                key = key,
                width = frame.width,
                height = frame.height,
            ),
            frame.bytes,
        )
    }
}
