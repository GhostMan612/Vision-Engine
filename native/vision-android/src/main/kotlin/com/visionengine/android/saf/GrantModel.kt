// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

enum class DisplayNameSource {
    TREE_DOCUMENT_ID,
    FOLDER_DISPLAY_NAME,
}

enum class GrantState {
    HEALTHY,
    LOST,
    NEVER_VERIFIED,
}

data class GrantEntry(
    val uri: String,
    val displayName: String,
    val displayNameSource: DisplayNameSource,
    val grantedMs: Long,
    val lastSeenHealthyMs: Long?,
)

data class GrantDiscovery(
    val itemCount: Int,
    val warnings: List<String>,
)

data class FolderState(
    val entry: GrantEntry,
    val state: GrantState,
    val itemCount: Int?,
    val warnings: List<String>,
)

fun resolveDisplayName(
    treeDocumentId: String,
    folderDisplayName: String?,
): Pair<String, DisplayNameSource> {
    if (folderDisplayName.isNullOrBlank()) {
        return Pair(treeDocumentId, DisplayNameSource.TREE_DOCUMENT_ID)
    }
    return Pair(folderDisplayName, DisplayNameSource.FOLDER_DISPLAY_NAME)
}

fun upsertGrant(
    entries: List<GrantEntry>,
    entry: GrantEntry,
): List<GrantEntry> {
    val index = entries.indexOfFirst { it.uri == entry.uri }
    if (index < 0) {
        return entries + entry
    }
    return entries.toMutableList().also { it[index] = entry }
}

fun replaceGrantUri(
    entries: List<GrantEntry>,
    oldUri: String,
    entry: GrantEntry,
): List<GrantEntry> {
    val index = entries.indexOfFirst { it.uri == oldUri }
    if (index < 0) {
        return upsertGrant(entries, entry)
    }
    return entries.toMutableList().also { it[index] = entry }
}

fun removeGrant(
    entries: List<GrantEntry>,
    uri: String,
): List<GrantEntry> {
    return entries.filter { it.uri != uri }
}

fun withLastSeenHealthy(
    entries: List<GrantEntry>,
    uri: String,
    nowMs: Long,
): List<GrantEntry> {
    return entries.map {
        if (it.uri == uri) it.copy(lastSeenHealthyMs = nowMs) else it
    }
}

fun deriveFolderStates(
    entries: List<GrantEntry>,
    persistedUris: Set<String>,
    discoveries: Map<String, GrantDiscovery>,
): List<FolderState> {
    return entries.map { entry ->
        val discovery = discoveries[entry.uri]
        val state = if (entry.uri !in persistedUris) {
            GrantState.LOST
        } else if (entry.lastSeenHealthyMs != null) {
            GrantState.HEALTHY
        } else {
            GrantState.NEVER_VERIFIED
        }
        FolderState(
            entry = entry,
            state = state,
            itemCount = discovery?.itemCount,
            warnings = discovery?.warnings ?: emptyList(),
        )
    }
}
