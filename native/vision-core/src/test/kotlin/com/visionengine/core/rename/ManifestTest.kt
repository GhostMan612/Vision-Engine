// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestTest {
    private val opA = RenameOp("/t/IMG_a.jpg", "/t/a.jpg")
    private val opB = RenameOp("/t/VID_b.mp4", "/t/b.mp4")

    @Test
    fun headerIsSixColumnDartForm() {
        val csv = manifestToCsv(listOf(opA))
        assertEquals(
            "original_name,new_name,original_path,new_path,content_uri,status",
            csv.lines().first(),
        )
    }

    @Test
    fun dryRunStampsPlanned() {
        val rows = parseManifestCsv(manifestToCsv(listOf(opA, opB), executed = false))
        assertEquals(listOf("planned", "planned"), rows.map { it.status })
    }

    @Test
    fun executedStampsRenamedOnlyForOpsThatReallyRan() {
        // The pre-PR #1 lie was stamping "renamed" on every row.
        // Only ops in renamedOps are "renamed"; the rest are "skipped".
        val rows = parseManifestCsv(
            manifestToCsv(listOf(opA, opB), executed = true, renamedOps = setOf(opA)),
        )
        assertEquals(listOf("renamed", "skipped"), rows.map { it.status })
    }

    @Test
    fun contentUriColumnIsWrittenFromMapAndEmptyOtherwise() {
        val csv = manifestToCsv(
            listOf(opA, opB),
            contentUris = mapOf("/t/IMG_a.jpg" to "content://media/a"),
        )
        val rows = parseManifestCsv(csv)
        assertEquals("content://media/a", rows[0].contentUri)
        assertEquals("", rows[1].contentUri)
    }

    @Test
    fun roundTripPreservesCommasQuotesAndNewlines() {
        val tricky = listOf(
            RenameOp("/t/IMG_a,b.jpg", "/t/a,b.jpg"),
            RenameOp("/t/IMG_say \"hi\".jpg", "/t/say \"hi\".jpg"),
            RenameOp("/t/IMG_line1\nline2.jpg", "/t/line1\nline2.jpg"),
        )
        val rows = parseManifestCsv(manifestToCsv(tricky, executed = false))
        assertEquals(3, rows.size)
        assertEquals("a,b.jpg", rows[0].newName)
        assertEquals("say \"hi\".jpg", rows[1].newName)
        assertEquals("line1\nline2.jpg", rows[2].newName)
        assertEquals("/t/IMG_line1\nline2.jpg", rows[2].originalPath)
    }

    @Test
    fun crlfLineEndingsParse() {
        val lf = manifestToCsv(listOf(opA, opB), executed = false)
        val crlf = lf.replace("\n", "\r\n")
        val rows = parseManifestCsv(crlf)
        assertEquals(2, rows.size)
        assertEquals("a.jpg", rows[0].newName)
        assertEquals("b.mp4", rows[1].newName)
        assertEquals("planned", rows[0].status)
    }

    @Test
    fun fiveColumnPythonFormParsesWithEmptyContentUri() {
        val pythonCsv =
            "original_name,new_name,original_path,new_path,status\n" +
                "IMG_a.jpg,a.jpg,/t/IMG_a.jpg,/t/a.jpg,renamed\n"
        val rows = parseManifestCsv(pythonCsv)
        assertEquals(1, rows.size)
        assertEquals("", rows[0].contentUri)
        assertEquals("renamed", rows[0].status)
        assertEquals("/t/a.jpg", rows[0].newPath)
    }

    @Test
    fun reorderedHeaderStillMapsByName() {
        val csv =
            "status,new_path,original_name,content_uri,new_name,original_path\n" +
                "renamed,/t/a.jpg,IMG_a.jpg,,a.jpg,/t/IMG_a.jpg\n"
        val rows = parseManifestCsv(csv)
        assertEquals(1, rows.size)
        assertEquals("IMG_a.jpg", rows[0].originalName)
        assertEquals("a.jpg", rows[0].newName)
        assertEquals("renamed", rows[0].status)
    }

    @Test
    fun missingFieldsDefaultToEmpty() {
        val csv =
            "original_name,new_name,original_path,new_path,content_uri,status\n" +
                "IMG_a.jpg,a.jpg\n"
        val rows = parseManifestCsv(csv)
        assertEquals(1, rows.size)
        assertEquals("IMG_a.jpg", rows[0].originalName)
        assertEquals("a.jpg", rows[0].newName)
        assertEquals("", rows[0].originalPath)
        assertEquals("", rows[0].status)
    }

    @Test
    fun emptyCsvParsesToNoRows() {
        assertTrue(parseManifestCsv("").isEmpty())
        assertTrue(
            parseManifestCsv("original_name,new_name,original_path,new_path,content_uri,status\n")
                .isEmpty(),
        )
    }

    @Test
    fun undoOrderReversesRows() {
        val rows = listOf(
            ManifestRow("IMG_a.jpg", "a.jpg", "/t/IMG_a.jpg", "/t/a.jpg", "", "renamed"),
            ManifestRow("VID_b.mp4", "b.mp4", "/t/VID_b.mp4", "/t/b.mp4", "", "renamed"),
            ManifestRow("IMG_c.jpg", "c.jpg", "/t/IMG_c.jpg", "/t/c.jpg", "", "renamed"),
        )
        assertEquals(
            listOf("c.jpg", "b.mp4", "a.jpg"),
            undoOrder(rows).map { it.newName },
        )
    }

    @Test
    fun undoGateRefusesNonRenamedRows() {
        val planned = ManifestRow("IMG_a.jpg", "a.jpg", "/t/IMG_a.jpg", "/t/a.jpg", "", "planned")
        val skipped = ManifestRow("VID_b.mp4", "b.mp4", "/t/VID_b.mp4", "/t/b.mp4", "", "skipped")
        val renamed = ManifestRow("IMG_c.jpg", "c.jpg", "/t/IMG_c.jpg", "/t/c.jpg", "", "renamed")
        val blank = ManifestRow("IMG_d.jpg", "d.jpg", "/t/IMG_d.jpg", "/t/d.jpg", "", "")
        assertFalse(isRestorable(planned))
        assertFalse(isRestorable(skipped))
        assertFalse(isRestorable(blank))
        assertTrue(isRestorable(renamed))
    }

    @Test
    fun dryRunManifestRestoresNothing() {
        // The PR #1 undo bug: a dry-run manifest must never restore
        // renames that never happened.
        val rows = parseManifestCsv(manifestToCsv(listOf(opA, opB), executed = false))
        assertEquals(2, rows.size)
        assertTrue(undoOrder(rows).none { isRestorable(it) })
    }
}
