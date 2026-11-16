// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.android.saf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(
    uri: String,
    grantedMs: Long = 100L,
    lastSeenHealthyMs: Long? = null,
): GrantEntry {
    return GrantEntry(
        uri = uri,
        displayName = uri,
        displayNameSource = DisplayNameSource.TREE_DOCUMENT_ID,
        grantedMs = grantedMs,
        lastSeenHealthyMs = lastSeenHealthyMs,
    )
}

class GrantModelTest {
    @Test
    fun resolveDisplayNamePrefersFolderName() {
        val resolved = resolveDisplayName("tree/primary:Pictures", "Pictures")
        assertEquals("Pictures", resolved.first)
        assertEquals(
            DisplayNameSource.FOLDER_DISPLAY_NAME,
            resolved.second,
        )
    }

    @Test
    fun resolveDisplayNameFallsBackToTreeIdOnBlank() {
        val blank = resolveDisplayName("tree/primary:Pictures", "  ")
        assertEquals("tree/primary:Pictures", blank.first)
        assertEquals(DisplayNameSource.TREE_DOCUMENT_ID, blank.second)
        val missing = resolveDisplayName("tree/primary:Pictures", null)
        assertEquals("tree/primary:Pictures", missing.first)
        assertEquals(DisplayNameSource.TREE_DOCUMENT_ID, missing.second)
    }

    @Test
    fun resolveDisplayNameNeverReturnsEmpty() {
        val resolved = resolveDisplayName("tree/primary:Pictures", "")
        assertTrue(resolved.first.isNotEmpty())
    }

    @Test
    fun upsertAppendsNewEntry() {
        val entries = upsertGrant(listOf(entry("a")), entry("b", 200L))
        assertEquals(listOf("a", "b"), entries.map { it.uri })
    }

    @Test
    fun upsertReplacesSameUriInPlace() {
        val before = listOf(entry("a", 100L), entry("b", 200L))
        val after = upsertGrant(before, entry("a", 300L, 300L))
        assertEquals(listOf("a", "b"), after.map { it.uri })
        assertEquals(300L, after[0].grantedMs)
        assertEquals(300L, after[0].lastSeenHealthyMs)
    }

    @Test
    fun replaceUriMovesEntryInPlace() {
        val before = listOf(entry("a", 100L), entry("b", 200L))
        val after = replaceGrantUri(before, "a", entry("c", 300L))
        assertEquals(listOf("c", "b"), after.map { it.uri })
        assertEquals(300L, after[0].grantedMs)
    }

    @Test
    fun replaceUriFallsBackToUpsertWhenOldUriAbsent() {
        val before = listOf(entry("a", 100L))
        val after = replaceGrantUri(before, "missing", entry("b", 200L))
        assertEquals(listOf("a", "b"), after.map { it.uri })
    }

    @Test
    fun removeDropsOnlyMatchingUri() {
        val before = listOf(entry("a"), entry("b"), entry("a"))
        assertEquals(listOf("b"), removeGrant(before, "a").map { it.uri })
        assertEquals(3, removeGrant(before, "missing").size)
    }

    @Test
    fun withLastSeenHealthyUpdatesOnlyMatchingUri() {
        val before = listOf(entry("a"), entry("b"))
        val after = withLastSeenHealthy(before, "b", 500L)
        assertEquals(null, after[0].lastSeenHealthyMs)
        assertEquals(500L, after[1].lastSeenHealthyMs)
    }

    @Test
    fun deriveHealthyNeedsPersistedGrantAndHealthyMark() {
        val entries = listOf(entry("a", 100L, 150L))
        val states = deriveFolderStates(entries, setOf("a"), emptyMap())
        assertEquals(GrantState.HEALTHY, states[0].state)
    }

    @Test
    fun deriveLostWhenGrantMissingFromPersistedSet() {
        val entries = listOf(entry("a", 100L, 150L))
        val states = deriveFolderStates(entries, emptySet(), emptyMap())
        assertEquals(GrantState.LOST, states[0].state)
    }

    @Test
    fun deriveNeverVerifiedWhenGrantPresentButUnmarked() {
        val entries = listOf(entry("a", 100L, null))
        val states = deriveFolderStates(entries, setOf("a"), emptyMap())
        assertEquals(GrantState.NEVER_VERIFIED, states[0].state)
    }

    @Test
    fun deriveCarriesDiscoveryCountAndWarnings() {
        val entries = listOf(entry("a", 100L, 150L))
        val discoveries = mapOf(
            "a" to GrantDiscovery(7, listOf("virtual document: cloud.jpg")),
        )
        val states = deriveFolderStates(entries, setOf("a"), discoveries)
        assertEquals(7, states[0].itemCount)
        assertEquals(1, states[0].warnings.size)
        assertTrue(states[0].warnings[0].contains("cloud.jpg"))
    }

    @Test
    fun deriveKeepsEntryOrder() {
        val entries = listOf(entry("b", 200L), entry("a", 100L))
        val states = deriveFolderStates(entries, setOf("a", "b"), emptyMap())
        assertEquals(listOf("b", "a"), states.map { it.entry.uri })
    }
}
