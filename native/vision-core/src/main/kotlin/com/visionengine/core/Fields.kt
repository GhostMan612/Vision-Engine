// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun stringField(json: Map<String, Any?>, key: String): MetaField<String> {
    val node = json[key]
    if (node is Map<*, *>) {
        val value = node["value"]
        return MetaField(
            if (value is String) value else null,
            parseProvenance(node["provenance"]),
        )
    }
    return MetaField(null, Provenance.unknown)
}

fun intField(json: Map<String, Any?>, key: String): MetaField<Long> {
    val node = json[key]
    if (node is Map<*, *>) {
        val value = node["value"]
        val parsed = when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Short -> value.toLong()
            is Byte -> value.toLong()
            else -> null
        }
        return MetaField(
            parsed,
            parseProvenance(node["provenance"]),
        )
    }
    return MetaField(null, Provenance.unknown)
}

fun doubleField(json: Map<String, Any?>, key: String): MetaField<Double> {
    val node = json[key]
    if (node is Map<*, *>) {
        val value = node["value"]
        return MetaField(
            if (value is Number) value.toDouble() else null,
            parseProvenance(node["provenance"]),
        )
    }
    return MetaField(null, Provenance.unknown)
}

fun <T> fieldNode(field: MetaField<T>): Map<String, Any?> = mapOf(
    "value" to field.value,
    "provenance" to field.provenance.name,
)

fun photoMetaFromMap(json: Map<String, Any?>): PhotoMeta = PhotoMeta(
    datetimeOriginal = stringField(json, "datetimeOriginal"),
    gpsLatitude = doubleField(json, "gpsLatitude"),
    gpsLongitude = doubleField(json, "gpsLongitude"),
    orientation = intField(json, "orientation"),
    make = stringField(json, "make"),
    model = stringField(json, "model"),
    exposureTime = stringField(json, "exposureTime"),
    widthRaw = intField(json, "widthRaw"),
    heightRaw = intField(json, "heightRaw"),
    fileSizeBytes = intField(json, "fileSizeBytes"),
    mime = stringField(json, "mime"),
)

fun photoMetaToMap(meta: PhotoMeta): Map<String, Any?> = mapOf(
    "datetimeOriginal" to fieldNode(meta.datetimeOriginal),
    "gpsLatitude" to fieldNode(meta.gpsLatitude),
    "gpsLongitude" to fieldNode(meta.gpsLongitude),
    "orientation" to fieldNode(meta.orientation),
    "make" to fieldNode(meta.make),
    "model" to fieldNode(meta.model),
    "exposureTime" to fieldNode(meta.exposureTime),
    "widthRaw" to fieldNode(meta.widthRaw),
    "heightRaw" to fieldNode(meta.heightRaw),
    "fileSizeBytes" to fieldNode(meta.fileSizeBytes),
    "mime" to fieldNode(meta.mime),
)

fun videoMetaFromMap(json: Map<String, Any?>): VideoMeta = VideoMeta(
    durationMs = intField(json, "durationMs"),
    widthRaw = intField(json, "widthRaw"),
    heightRaw = intField(json, "heightRaw"),
    rotation = intField(json, "rotation"),
    creationTime = stringField(json, "creationTime"),
    locationLatitude = doubleField(json, "locationLatitude"),
    locationLongitude = doubleField(json, "locationLongitude"),
    codec = stringField(json, "codec"),
    fileSizeBytes = intField(json, "fileSizeBytes"),
    mime = stringField(json, "mime"),
)

fun videoMetaToMap(meta: VideoMeta): Map<String, Any?> = mapOf(
    "durationMs" to fieldNode(meta.durationMs),
    "widthRaw" to fieldNode(meta.widthRaw),
    "heightRaw" to fieldNode(meta.heightRaw),
    "rotation" to fieldNode(meta.rotation),
    "creationTime" to fieldNode(meta.creationTime),
    "locationLatitude" to fieldNode(meta.locationLatitude),
    "locationLongitude" to fieldNode(meta.locationLongitude),
    "codec" to fieldNode(meta.codec),
    "fileSizeBytes" to fieldNode(meta.fileSizeBytes),
    "mime" to fieldNode(meta.mime),
)
