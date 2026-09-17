// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun fnv1a64(input: String): Long {
    var hash = 0xcbf29ce484222325uL
    for (index in input.indices) {
        hash = hash xor input[index].code.toULong()
        hash *= 0x100000001b3uL
    }
    return hash.toLong()
}

fun fnvHex(hash: Long): String {
    val rendered = if (hash < 0L) {
        if (hash == Long.MIN_VALUE) {
            "-8000000000000000"
        } else {
            "-" + java.lang.Long.toHexString(-hash)
        }
    } else {
        java.lang.Long.toHexString(hash)
    }
    return rendered.padStart(16, '0')
}

fun thumbKey(
    sourceId: String,
    sizeBytes: Long?,
    modifiedMs: Long?,
    maxDimension: Int,
): String {
    val canonical = "$sourceId|${sizeBytes ?: -1}|${modifiedMs ?: -1}|$maxDimension"
    return fnvHex(fnv1a64(canonical))
}
