// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

enum class ThumbStatus {
    pending,
    ready,
    unavailable,
    failed,
}

data class ThumbInfo(
    val status: ThumbStatus,
    val key: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val reason: String? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "status" to status.name,
        "key" to key,
        "width" to width,
        "height" to height,
        "reason" to reason,
    )

    companion object {
        fun fromMap(json: Map<String, Any?>): ThumbInfo {
            var status = ThumbStatus.pending
            for (candidate in ThumbStatus.values()) {
                if (candidate.name == json["status"]) {
                    status = candidate
                }
            }
            return ThumbInfo(
                status = status,
                key = json["key"] as String?,
                width = (json["width"] as Number?)?.toInt(),
                height = (json["height"] as Number?)?.toInt(),
                reason = json["reason"] as String?,
            )
        }
    }
}
