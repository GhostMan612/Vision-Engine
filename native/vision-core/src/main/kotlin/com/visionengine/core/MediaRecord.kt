// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

data class MediaRecord(
    val source: MediaSource,
    val kind: MediaKind,
    val status: ExtractStatus,
    val sortIndex: Int,
    val photo: PhotoMeta? = null,
    val video: VideoMeta? = null,
    val warnings: List<String> = emptyList(),
    val thumb: ThumbInfo = ThumbInfo(ThumbStatus.pending),
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "source" to source.toMap(),
        "kind" to kind.name,
        "status" to status.name,
        "sortIndex" to sortIndex,
        "photo" to photo?.let(::photoMetaToMap),
        "video" to video?.let(::videoMetaToMap),
        "warnings" to warnings,
        "thumb" to thumb.toMap(),
    )

    companion object {
        fun fromPhoto(
            source: MediaSource,
            sortIndex: Int,
            photo: PhotoMeta,
            status: ExtractStatus,
            warnings: List<String> = emptyList(),
        ): MediaRecord = MediaRecord(
            source = source,
            kind = MediaKind.photo,
            status = status,
            sortIndex = sortIndex,
            photo = photo,
            warnings = warnings.toList(),
        )

        fun fromVideo(
            source: MediaSource,
            sortIndex: Int,
            video: VideoMeta,
            status: ExtractStatus,
            warnings: List<String> = emptyList(),
        ): MediaRecord = MediaRecord(
            source = source,
            kind = MediaKind.video,
            status = status,
            sortIndex = sortIndex,
            video = video,
            warnings = warnings.toList(),
        )

        fun unsupported(
            source: MediaSource,
            sortIndex: Int,
            warnings: List<String> = listOf("unsupported type"),
        ): MediaRecord = MediaRecord(
            source = source,
            kind = MediaKind.unsupported,
            status = ExtractStatus.unsupported,
            sortIndex = sortIndex,
            warnings = warnings.toList(),
            thumb = ThumbInfo(
                ThumbStatus.unavailable,
                reason = "unsupported type",
            ),
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(json: Map<String, Any?>): MediaRecord {
            var kind = MediaKind.unsupported
            for (candidate in MediaKind.values()) {
                if (candidate.name == json["kind"]) {
                    kind = candidate
                }
            }
            val photoNode = json["photo"] as Map<String, Any?>?
            val videoNode = json["video"] as Map<String, Any?>?
            val warningNodes = json["warnings"] as List<*>
            val thumbNode = json["thumb"] as Map<String, Any?>
            return MediaRecord(
                source = MediaSource.fromMap(
                    json["source"] as Map<String, Any?>,
                ),
                kind = kind,
                status = parseStatus(json["status"]),
                sortIndex = (json["sortIndex"] as Number).toInt(),
                photo = photoNode?.let(::photoMetaFromMap),
                video = videoNode?.let(::videoMetaFromMap),
                warnings = warningNodes.filterIsInstance<String>(),
                thumb = ThumbInfo.fromMap(thumbNode),
            )
        }

        private fun parseStatus(raw: Any?): ExtractStatus {
            for (candidate in ExtractStatus.values()) {
                if (candidate.name == raw) {
                    return candidate
                }
            }
            return ExtractStatus.partial
        }
    }
}
