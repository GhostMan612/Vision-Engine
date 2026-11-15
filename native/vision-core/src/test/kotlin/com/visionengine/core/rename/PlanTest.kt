// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(
    path: String,
    isDir: Boolean = false,
    isFile: Boolean = true,
): RenameEntry {
    val name = basenameOf(path)
    return RenameEntry(path = path, name = name, isDir = isDir, isFile = isFile)
}

private fun planOf(vararg paths: String, recursive: Boolean = false): RenameReport {
    val entries = paths.map { entry(it) }
    val existing = paths.toSet()
    return buildPlan(entries = entries, existingPaths = existing, recursive = recursive)
}

class PlanTest {
    @Test
    fun directoriesAreSkipped() {
        val report = buildPlan(
            entries = listOf(
                entry("/t/sub", isDir = true, isFile = false),
                entry("/t/IMG_a.jpg"),
            ),
            existingPaths = setOf("/t/sub", "/t/IMG_a.jpg"),
        )
        assertEquals(2, report.scanned)
        assertEquals(1, report.skippedIsDir)
        assertEquals(1, report.toRename)
        assertEquals(listOf("/t/IMG_a.jpg" to "/t/a.jpg"), report.planned.map { it.srcPath to it.dstPath })
    }

    @Test
    fun nonFilesAreSkippedAsNoPrefix() {
        val report = buildPlan(
            entries = listOf(entry("/t/fifo", isDir = false, isFile = false)),
            existingPaths = setOf("/t/fifo"),
        )
        assertEquals(1, report.scanned)
        assertEquals(1, report.skippedNoPrefix)
        assertEquals(0, report.toRename)
    }

    @Test
    fun emptyResultsAreSkipped() {
        val report = planOf("/t/IMG_", "/t/VID_", "/t/IMG_ok.jpg")
        assertEquals(3, report.scanned)
        assertEquals(2, report.skippedEmptyResult)
        assertEquals(1, report.toRename)
    }

    @Test
    fun stayingPutCollisionSkips() {
        val report = planOf("/t/IMG_A.jpg", "/t/A.jpg")
        assertEquals(0, report.toRename)
        assertEquals(
            listOf("IMG_A.jpg -> A.jpg (target exists)"),
            report.skippedCollision,
        )
    }

    @Test
    fun existingTargetNotInBatchSkips() {
        val report = buildPlan(
            entries = listOf(entry("/t/IMG_D.jpg")),
            existingPaths = setOf("/t/IMG_D.jpg", "/t/D.jpg"),
        )
        assertEquals(0, report.toRename)
        assertEquals(
            listOf("IMG_D.jpg -> D.jpg (target exists)"),
            report.skippedCollision,
        )
    }

    @Test
    fun intraBatchCollisionKeepsFirst() {
        val report = planOf("/t/IMG_B.jpg", "/t/VID_B.jpg")
        assertEquals(listOf("/t/IMG_B.jpg" to "/t/B.jpg"), report.planned.map { it.srcPath to it.dstPath })
        assertEquals(
            listOf("VID_B.jpg -> B.jpg (duplicate target in batch)"),
            report.skippedCollision,
        )
    }

    @Test
    fun plannedOpsAreSortedByOldName() {
        val report = planOf("/t/VID_z.mp4", "/t/IMG_m.jpg", "/t/IMG_a.jpg")
        assertEquals(
            listOf("IMG_a.jpg", "IMG_m.jpg", "VID_z.mp4"),
            report.planned.map { it.oldName },
        )
    }

    @Test
    fun chainListsBothWithNoCollisions() {
        val report = planOf("/t/IMG_VID_a.jpg", "/t/VID_a.jpg")
        assertEquals(2, report.toRename)
        assertTrue(report.skippedCollision.isEmpty())
    }

    @Test
    fun caseGateKeepsLowercase() {
        val report = buildPlan(
            entries = listOf(entry("/t/img_lower.jpg"), entry("/t/IMG_upper.jpg")),
            existingPaths = setOf("/t/img_lower.jpg", "/t/IMG_upper.jpg"),
        )
        assertEquals(1, report.toRename)
        assertEquals(1, report.skippedNoPrefix)
        assertEquals("IMG_upper.jpg", report.planned[0].oldName)
    }

    @Test
    fun recursiveSortsByPath() {
        val report = planOf(
            "/t/z/IMG_deep.jpg",
            "/t/IMG_top.jpg",
            "/t/sub/keep.jpg",
            recursive = true,
        )
        assertEquals(3, report.scanned)
        assertEquals(2, report.toRename)
        assertEquals(1, report.skippedNoPrefix)
        assertEquals(
            listOf("/t/z/IMG_deep.jpg" to "/t/z/deep.jpg", "/t/IMG_top.jpg" to "/t/top.jpg"),
            report.planned.map { it.srcPath to it.dstPath },
        )
    }

    @Test
    fun buildPlanNeverClaimsSymlinkHandling() {
        // buildPlan takes pre-built entries and cannot detect symlinks.
        // The counter exists for the enumerator layer (S3/S4); the plan
        // itself must stay at 0. A test "proving" buildPlan skips
        // symlinks would be testing a fiction (spec S2 §6.2 note).
        val report = planOf("/t/IMG_a.jpg")
        assertEquals(0, report.skippedSymlink)
    }

    @Test
    fun astralPlaneFilenamesPinUtf16SortOrder() {
        // Spec S2 §9.1: Python sorts by code point, Kotlin/Dart
        // String.compareTo by UTF-16 code unit. For names containing
        // characters above U+FFFF the two orders differ, and this test
        // pins the Kotlin (UTF-16) behavior.
        //
        // U+1F600 (😀, surrogate pair D83D DE00) vs U+FFFF:
        // - code-point order: U+FFFF (65535) < U+1F600 (128512),
        //   so Python would order the U+FFFF file first.
        // - UTF-16 order: first surrogate 0xD83D < 0xFFFF, so Kotlin
        //   (and Dart) order the emoji file first.
        // Camera filenames are ASCII IMG_/VID_ in practice; this test
        // exists so the divergence is a documented rule, not a
        // surprise discovered during MP8 parity.
        val emojiName = "IMG_\uD83D\uDE00a.jpg"
        val ffffName = "IMG_\uFFFFb.jpg"
        val report = planOf("/t/$emojiName", "/t/$ffffName")
        assertEquals(2, report.toRename)
        assertEquals(
            listOf(emojiName, ffffName),
            report.planned.map { it.oldName },
        )
        assertEquals(
            "UTF-16 order must put the surrogate-pair name first; " +
                "code-point order would put U+FFFF first",
            "IMG_\uD83D\uDE00a.jpg",
            report.planned[0].oldName,
        )
    }
}
