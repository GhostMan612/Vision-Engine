// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun compareSources(a: MediaSource, b: MediaSource): Int {
    val folded = a.displayName.lowercase().compareTo(b.displayName.lowercase())
    if (folded != 0) {
        return folded
    }
    val named = a.displayName.compareTo(b.displayName)
    if (named != 0) {
        return named
    }
    return a.id.compareTo(b.id)
}

fun dedupeSources(found: List<MediaSource>): List<MediaSource> {
    val seen = LinkedHashSet<String>()
    val out = mutableListOf<MediaSource>()
    for (source in found) {
        if (seen.add(source.id)) {
            out.add(source)
        }
    }
    return out
}

fun sortSources(found: List<MediaSource>): List<MediaSource> =
    found.sortedWith(::compareSources)

fun planDiscovery(found: List<MediaSource>): List<MediaSource> =
    sortSources(dedupeSources(found))

fun <T> pageOf(ordered: List<T>, offset: Int, limit: Int): List<T> {
    if (offset < 0 || limit <= 0 || offset >= ordered.size) {
        return emptyList()
    }
    val end = if (offset + limit > ordered.size) ordered.size else offset + limit
    return ordered.subList(offset, end)
}
