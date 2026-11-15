// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import com.visionengine.core.loadGolden
import org.junit.Assert.assertEquals
import org.junit.Test

private fun countsAsInt(counts: Map<String, Any?>, key: String): Int =
    (counts[key] as Long).toInt()

private fun stringList(raw: Any?): List<String> =
    (raw as List<Any?>).map { it as String }

/**
 * Golden replay for the rename port (spec S2 §6.5): the same 6 JSON
 * fixtures the Dart `golden_test.dart` runs, copied byte-identical
 * into `src/test/resources/golden/`. Exercises [buildPlan] through
 * the existing [loadGolden]/`parseJson` helpers — no new JSON code.
 * A failure here means the Kotlin port drifted from the Dart/Python
 * contract; the golden is never "fixed" to match the port.
 */
class RenameGoldenTest {
    @Test
    fun allSixGoldensReplay() {
        for (name in listOf(
            "leading_only",
            "case_gate",
            "empty_skip",
            "collision_trio",
            "sort_determinism",
            "recursive",
        )) {
            replay(name)
        }
    }

    private fun replay(name: String) {
        val golden = loadGolden(name)
        @Suppress("UNCHECKED_CAST")
        val rawEntries = golden["entries"] as List<Any?>
        val entries = rawEntries.map { raw ->
            @Suppress("UNCHECKED_CAST")
            val map = raw as Map<String, Any?>
            RenameEntry(
                path = map["path"] as String,
                name = map["name"] as String,
                isDir = (map["isDir"] as Boolean?) ?: false,
                isFile = (map["isFile"] as Boolean?) ?: true,
            )
        }
        val existing = stringList(golden["existing"]).toSet()
        val report = buildPlan(
            entries = entries,
            existingPaths = existing,
            prefixes = stringList(golden["prefixes"]),
            caseSensitive = (golden["caseSensitive"] as Boolean?) ?: true,
            recursive = (golden["recursive"] as Boolean?) ?: false,
        )
        @Suppress("UNCHECKED_CAST")
        val rawPlanned = golden["expectedPlanned"] as List<Any?>
        val expectedPlanned = rawPlanned.map { pair ->
            @Suppress("UNCHECKED_CAST")
            (pair as List<Any?>).map { it as String }
        }
        assertEquals(
            "$name: planned ops",
            expectedPlanned,
            report.planned.map { listOf(it.srcPath, it.dstPath) },
        )
        assertEquals(
            "$name: collisions",
            stringList(golden["expectedCollisions"]),
            report.skippedCollision,
        )
        @Suppress("UNCHECKED_CAST")
        val counts = golden["expectedCounts"] as Map<String, Any?>
        assertEquals("$name: scanned", countsAsInt(counts, "scanned"), report.scanned)
        assertEquals("$name: planned", countsAsInt(counts, "planned"), report.planned.size)
        assertEquals(
            "$name: skippedNoPrefix",
            countsAsInt(counts, "skippedNoPrefix"),
            report.skippedNoPrefix,
        )
        assertEquals(
            "$name: skippedIsDir",
            countsAsInt(counts, "skippedIsDir"),
            report.skippedIsDir,
        )
        assertEquals(
            "$name: skippedEmptyResult",
            countsAsInt(counts, "skippedEmptyResult"),
            report.skippedEmptyResult,
        )
        assertEquals(
            "$name: collisions",
            countsAsInt(counts, "collisions"),
            report.skippedCollision.size,
        )
        // buildPlan never claims symlink handling (spec S2 §6.2 note):
        // the counter stays 0 — the enumerator owns it.
        assertEquals("$name: skippedSymlink", 0, report.skippedSymlink)
    }
}
