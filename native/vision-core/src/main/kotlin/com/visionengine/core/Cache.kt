// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

data class CachedThumb(
    val key: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    val byteSize: Int
        get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CachedThumb) {
            return false
        }
        return key == other.key &&
            bytes.contentEquals(other.bytes) &&
            width == other.width &&
            height == other.height
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

class ThumbnailCache(
    val maxEntries: Int = 100,
    val maxBytes: Long = 32L * 1024L * 1024L,
) {
    private val entries = HashMap<String, CachedThumb>()
    private val order = mutableListOf<String>()
    var hits: Int = 0
        private set
    var misses: Int = 0
        private set
    var evictions: Int = 0
        private set

    val length: Int
        get() = entries.size

    val currentBytes: Long
        get() {
            var total = 0L
            for (entry in entries.values) {
                total += entry.byteSize
            }
            return total
        }

    fun get(key: String): CachedThumb? {
        val hit = entries[key]
        if (hit == null) {
            misses += 1
            return null
        }
        hits += 1
        order.remove(key)
        order.add(key)
        return hit
    }

    fun put(thumb: CachedThumb) {
        order.remove(thumb.key)
        entries.remove(thumb.key)
        entries[thumb.key] = thumb
        order.add(thumb.key)
        while (entries.size > maxEntries || currentBytes > maxBytes) {
            val oldest = order.removeAt(0)
            entries.remove(oldest)
            evictions += 1
        }
    }

    fun clear() {
        entries.clear()
        order.clear()
    }
}
