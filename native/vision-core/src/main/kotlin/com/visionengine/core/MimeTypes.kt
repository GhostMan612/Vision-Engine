// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

import java.util.Locale

val photoMimes: Set<String> = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/heif",
    "image/x-adobe-dng",
)

val videoMimes: Set<String> = setOf(
    "video/mp4",
    "video/x-m4v",
    "video/quicktime",
    "video/3gpp",
    "video/x-matroska",
    "video/webm",
)

const val directoryMime = "vnd.android.document/directory"

fun normalizeMime(raw: String?): String? {
    if (raw == null) {
        return null
    }
    val base = raw.substringBefore(';').trim().lowercase(Locale.ROOT)
    return base.ifEmpty { null }
}

fun isDirectoryMime(mime: String?): Boolean =
    normalizeMime(mime) == directoryMime

fun kindForMime(mime: String?, displayName: String): MediaKind {
    when (normalizeMime(mime)) {
        in photoMimes -> return MediaKind.photo
        in videoMimes -> return MediaKind.video
    }
    return kindForExtension(displayName)
}

fun kindForExtension(displayName: String): MediaKind {
    val dot = displayName.lastIndexOf('.')
    if (dot < 0 || dot == displayName.length - 1) {
        return MediaKind.unsupported
    }
    return when (displayName.substring(dot + 1).lowercase(Locale.ROOT)) {
        "jpg", "jpeg", "png", "webp", "heic", "heif", "dng" -> MediaKind.photo
        "mp4", "m4v", "mov", "3gp", "mkv", "webm" -> MediaKind.video
        else -> MediaKind.unsupported
    }
}

fun mimeFor(mime: String?, displayName: String): String? {
    val normalized = normalizeMime(mime)
    if (normalized != null &&
        (normalized in photoMimes || normalized in videoMimes)
    ) {
        return normalized
    }
    val dot = displayName.lastIndexOf('.')
    if (dot < 0 || dot == displayName.length - 1) {
        return null
    }
    return when (displayName.substring(dot + 1).lowercase(Locale.ROOT)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "dng" -> "image/x-adobe-dng"
        "mp4" -> "video/mp4"
        "m4v" -> "video/x-m4v"
        "mov" -> "video/quicktime"
        "3gp" -> "video/3gpp"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        else -> null
    }
}
