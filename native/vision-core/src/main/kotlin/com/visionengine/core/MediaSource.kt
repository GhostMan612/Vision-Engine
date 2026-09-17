// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

data class MediaSource(
    val id: String,
    val displayName: String,
    val kind: MediaKind,
    val filePath: String? = null,
    val documentUri: String? = null,
    val mime: String? = null,
    val sizeBytes: Long? = null,
    val modifiedMs: Long? = null,
) {
    init {
        require(filePath != null || documentUri != null) {
            "MediaSource needs filePath or documentUri: $id"
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "kind" to kind.name,
        "filePath" to filePath,
        "documentUri" to documentUri,
        "mime" to mime,
        "sizeBytes" to sizeBytes,
        "modifiedMs" to modifiedMs,
    )

    companion object {
        fun fromMap(json: Map<String, Any?>): MediaSource {
            var kind = MediaKind.unsupported
            for (candidate in MediaKind.values()) {
                if (candidate.name == json["kind"]) {
                    kind = candidate
                }
            }
            return MediaSource(
                id = json["id"] as String,
                displayName = json["displayName"] as String,
                kind = kind,
                filePath = json["filePath"] as String?,
                documentUri = json["documentUri"] as String?,
                mime = json["mime"] as String?,
                sizeBytes = (json["sizeBytes"] as Number?)?.toLong(),
                modifiedMs = (json["modifiedMs"] as Number?)?.toLong(),
            )
        }
    }
}
