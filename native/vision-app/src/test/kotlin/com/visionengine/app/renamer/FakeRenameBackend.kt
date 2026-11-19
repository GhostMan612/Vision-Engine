// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

class FakeRenameBackend(
    val names: MutableMap<String, String> = mutableMapOf(),
    val liveUris: MutableSet<String> = mutableSetOf(),
    var newUriOf: (String, String) -> String = { uri, _ -> uri },
    var refuse: Set<String> = emptySet(),
    var throwOn: Set<String> = emptySet(),
    var unverifiable: Set<String> = emptySet(),
    var misnamed: Set<String> = emptySet(),
    var appearAfterCalls: Int = -1,
    var appearing: Set<String> = emptySet(),
    var throwOnCheck: Set<String> = emptySet(),
) : RenameBackend {
    val renameCalls: MutableList<Pair<String, String>> = mutableListOf()
    var destinationChecks: Int = 0

    override fun rename(documentUri: String, newName: String): String? {
        renameCalls.add(documentUri to newName)
        if (documentUri in throwOn) {
            throw RuntimeException("rename boom")
        }
        if (documentUri !in liveUris) {
            throw IllegalArgumentException("unknown document")
        }
        if (documentUri in refuse) {
            return null
        }
        liveUris.remove(documentUri)
        val newUri = newUriOf(documentUri, newName)
        liveUris.add(newUri)
        names[newUri] = newName
        return newUri
    }

    override fun displayName(documentUri: String): String? {
        if (documentUri in unverifiable) {
            return null
        }
        if (documentUri in misnamed) {
            return "STALE"
        }
        return names[documentUri]
    }

    override fun destinationExists(
        treeUri: String,
        treeDocumentId: String,
        name: String,
    ): Boolean {
        destinationChecks += 1
        if (name in throwOnCheck) {
            throw RuntimeException("check boom")
        }
        if (appearAfterCalls >= 0 &&
            destinationChecks > appearAfterCalls &&
            name in appearing
        ) {
            return true
        }
        return names.any { (uri, n) -> uri in liveUris && n == name }
    }
}
