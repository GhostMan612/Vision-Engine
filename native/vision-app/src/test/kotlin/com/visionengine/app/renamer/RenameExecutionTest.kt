// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import com.visionengine.core.rename.ManifestRow
import com.visionengine.core.rename.RenameEntry
import com.visionengine.core.rename.RenameOp
import com.visionengine.core.rename.RenameReport
import com.visionengine.core.rename.buildPlan
import com.visionengine.core.rename.executionOrder
import com.visionengine.core.rename.manifestToCsv
import com.visionengine.core.rename.parseManifestCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenameExecutionTest {
    private fun planFor(vararg names: String): RenameReport {
        val entries = names.map {
            RenameEntry(path = "tree/$it", name = it)
        }
        return buildPlan(entries, entries.map { it.path }.toSet())
    }

    private fun backendFor(vararg names: String): FakeRenameBackend {
        val backend = FakeRenameBackend(
            newUriOf = { _, newName -> "doc://$newName" },
        )
        for (name in names) {
            backend.liveUris.add("doc://$name")
            backend.names["doc://$name"] = name
        }
        return backend
    }

    private fun runPlan(
        report: RenameReport,
        backend: FakeRenameBackend,
    ): ExecuteResult {
        val (ordered, cyclic) = executionOrder(report.planned)
        return executePlan(
            ordered = ordered,
            cyclic = cyclic,
            sourceUriOf = { op -> "doc://" + op.oldName },
            treeUri = "tree://Pictures",
            treeDocumentId = "primary:Pictures",
            backend = backend,
        )
    }

    private fun undoWith(
        rows: List<ManifestRow>,
        backend: FakeRenameBackend,
    ): UndoResult {
        return undoRows(
            rows = rows,
            documentUrisOf = { row ->
                listOf(row.contentUri, "doc://" + row.newName)
            },
            treeUri = "tree://Pictures",
            treeDocumentId = "primary:Pictures",
            backend = backend,
        )
    }

    private fun renamedRow(
        original: String,
        renamed: String,
    ): ManifestRow {
        return ManifestRow(
            originalName = original,
            newName = renamed,
            originalPath = "tree/$original",
            newPath = "tree/$renamed",
            contentUri = "doc://$renamed",
            status = "renamed",
        )
    }

    @Test
    fun chainOrdersAndRenamesBoth() {
        val report = planFor("IMG_VID_a.jpg", "VID_a.jpg")
        assertEquals(2, report.toRename)
        val backend = backendFor("IMG_VID_a.jpg", "VID_a.jpg")
        val result = runPlan(report, backend)
        assertEquals(2, result.renamedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(0, result.unverifiedCount)
        assertEquals(
            listOf(
                "doc://VID_a.jpg" to "a.jpg",
                "doc://IMG_VID_a.jpg" to "VID_a.jpg",
            ),
            backend.renameCalls,
        )
        assertEquals("a.jpg", backend.names["doc://a.jpg"])
        assertEquals("VID_a.jpg", backend.names["doc://VID_a.jpg"])
    }

    @Test
    fun destinationAppearingMidBatchSkipsOnlyThatOp() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.appearAfterCalls = 1
        backend.appearing = setOf("b.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(0, result.unverifiedCount)
        assertEquals(
            setOf(report.planned[0]),
            result.renamedOps,
        )
        assertTrue(
            result.results.first {
                it.op.oldName == "IMG_b.jpg"
            }.detail.contains("target already exists"),
        )
    }

    @Test
    fun refusedRenameIsSkippedNotError() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.refuse = setOf("doc://IMG_a.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals("rename refused", result.results.first {
            it.op.oldName == "IMG_a.jpg"
        }.detail)
    }

    @Test
    fun throwingRenameIsErrorAndBatchContinues() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.throwOn = setOf("doc://IMG_a.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(1, result.errorCount)
        assertTrue(
            result.results.first {
                it.op.oldName == "IMG_a.jpg"
            }.detail.contains("rename boom"),
        )
    }

    @Test
    fun unverifiableRenameIsNotCountedRenamed() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.unverifiable = setOf("doc://a.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(1, result.unverifiedCount)
        assertFalse(
            report.planned[0] in result.renamedOps,
        )
        val csv = manifestToCsv(
            report.planned,
            mapOf(
                "tree/IMG_a.jpg" to "doc://IMG_a.jpg",
                "tree/IMG_b.jpg" to "doc://IMG_b.jpg",
            ),
            executed = true,
            renamedOps = result.renamedOps,
        )
        val rows = parseManifestCsv(csv)
        assertEquals(
            "skipped",
            rows.first { it.originalName == "IMG_a.jpg" }.status,
        )
        assertEquals(
            "renamed",
            rows.first { it.originalName == "IMG_b.jpg" }.status,
        )
    }

    @Test
    fun misnamedRenameIsUnverified() {
        val report = planFor("IMG_a.jpg")
        val backend = backendFor("IMG_a.jpg")
        backend.misnamed = setOf("doc://a.jpg")
        val result = runPlan(report, backend)
        assertEquals(0, result.renamedCount)
        assertEquals(1, result.unverifiedCount)
        assertTrue(
            result.results[0].detail.contains("still named STALE"),
        )
    }

    @Test
    fun cyclicOpsAreErrors() {
        val ops = listOf(
            RenameOp("tree/a.jpg", "tree/b.jpg"),
            RenameOp("tree/b.jpg", "tree/a.jpg"),
        )
        val (ordered, cyclic) = executionOrder(ops)
        assertTrue(ordered.isEmpty())
        assertEquals(2, cyclic.size)
        val backend = backendFor("a.jpg", "b.jpg")
        val result = executePlan(
            ordered = ordered,
            cyclic = cyclic,
            sourceUriOf = { op -> "doc://" + op.oldName },
            treeUri = "tree://Pictures",
            treeDocumentId = "primary:Pictures",
            backend = backend,
        )
        assertEquals(0, result.renamedCount)
        assertEquals(2, result.errorCount)
        assertTrue(
            result.results.all {
                it.detail.contains("cyclic rename order")
            },
        )
    }

    @Test
    fun manifestHonestyAfterExecute() {
        val report = planFor("IMG_ok.jpg", "IMG_smoke_a.jpg", "smoke_a.jpg")
        assertEquals(1, report.toRename)
        assertEquals(1, report.collisionCount)
        val backend = backendFor("IMG_ok.jpg", "IMG_smoke_a.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        val csv = manifestToCsv(
            report.planned,
            mapOf("tree/IMG_ok.jpg" to "doc://IMG_ok.jpg"),
            executed = true,
            renamedOps = result.renamedOps,
        )
        val rows = parseManifestCsv(csv)
        assertEquals(1, rows.size)
        assertEquals("renamed", rows[0].status)
        assertEquals("doc://IMG_ok.jpg", rows[0].contentUri)
        val dryCsv = manifestToCsv(report.planned, executed = false)
        assertEquals(
            "planned",
            parseManifestCsv(dryCsv)[0].status,
        )
    }

    @Test
    fun undoRestoresInReverseOrder() {
        val rows = listOf(
            renamedRow("IMG_a.jpg", "a.jpg"),
            renamedRow("IMG_b.jpg", "b.jpg"),
        )
        val backend = backendFor("a.jpg", "b.jpg")
        val result = undoWith(rows, backend)
        assertEquals(2, result.restoredCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(
            listOf(
                "doc://b.jpg" to "IMG_b.jpg",
                "doc://a.jpg" to "IMG_a.jpg",
            ),
            backend.renameCalls,
        )
    }

    @Test
    fun undoRefusesNonRenamedRows() {
        val rows = listOf(
            renamedRow("IMG_a.jpg", "a.jpg"),
            ManifestRow(
                originalName = "IMG_b.jpg",
                newName = "b.jpg",
                originalPath = "tree/IMG_b.jpg",
                newPath = "tree/b.jpg",
                contentUri = "doc://IMG_b.jpg",
                status = "planned",
            ),
            ManifestRow(
                originalName = "IMG_c.jpg",
                newName = "c.jpg",
                originalPath = "tree/IMG_c.jpg",
                newPath = "tree/c.jpg",
                contentUri = "doc://IMG_c.jpg",
                status = "skipped",
            ),
        )
        val backend = backendFor("a.jpg")
        val result = undoWith(rows, backend)
        assertEquals(1, result.restoredCount)
        assertEquals(2, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(1, backend.renameCalls.size)
    }

    @Test
    fun undoDryRunManifestRestoresZero() {
        val rows = listOf(
            ManifestRow(
                originalName = "IMG_a.jpg",
                newName = "a.jpg",
                originalPath = "tree/IMG_a.jpg",
                newPath = "tree/a.jpg",
                contentUri = "doc://IMG_a.jpg",
                status = "planned",
            ),
        )
        val backend = backendFor("a.jpg")
        val result = undoWith(rows, backend)
        assertEquals(0, result.restoredCount)
        assertEquals(1, result.skippedCount)
        assertTrue(backend.renameCalls.isEmpty())
    }

    @Test
    fun undoPartialExecuteRestoresOnlyRenamed() {
        val rows = listOf(
            renamedRow("IMG_a.jpg", "a.jpg"),
            ManifestRow(
                originalName = "IMG_b.jpg",
                newName = "b.jpg",
                originalPath = "tree/IMG_b.jpg",
                newPath = "tree/b.jpg",
                contentUri = "doc://IMG_b.jpg",
                status = "skipped",
            ),
        )
        val backend = backendFor("a.jpg")
        val result = undoWith(rows, backend)
        assertEquals(1, result.restoredCount)
        assertEquals(1, result.skippedCount)
        assertEquals(
            listOf("doc://a.jpg" to "IMG_a.jpg"),
            backend.renameCalls,
        )
    }

    @Test
    fun undoDestinationOccupiedIsError() {
        val rows = listOf(renamedRow("IMG_a.jpg", "a.jpg"))
        val backend = backendFor("a.jpg", "IMG_a.jpg")
        val result = undoWith(rows, backend)
        assertEquals(0, result.restoredCount)
        assertEquals(1, result.errorCount)
        assertTrue(
            result.results[0].detail.contains("target already exists"),
        )
    }

    @Test
    fun countsReconcile() {
        val report = planFor(
            "IMG_a.jpg",
            "VID_b.mp4",
            "keep.jpg",
            "IMG_",
            "IMG_c.jpg",
            "c.jpg",
        )
        assertEquals(6, report.scanned)
        assertEquals(2, report.toRename)
        assertEquals(2, report.skippedNoPrefix)
        assertEquals(1, report.skippedEmptyResult)
        assertEquals(1, report.collisionCount)
        val discovery = discoveryCounts(
            photos = 5,
            videos = 1,
            unsupported = 0,
            skippedDirs = 0,
            skippedRows = 0,
            warnings = emptyList(),
        )
        assertEquals(6, discovery.scanned)
        assertTrue(countsReconcile(discovery, planCounts(report)))
        assertFalse(
            countsReconcile(
                discovery,
                planCounts(report).copy(scanned = 99),
            ),
        )
    }

    @Test
    fun skippedRowsExplainEveryNonPlannedEntry() {
        val report = planFor(
            "IMG_a.jpg",
            "IMG_smoke_a.jpg",
            "smoke_a.jpg",
            "IMG_",
        )
        assertEquals(1, report.toRename)
        assertEquals(1, report.collisionCount)
        assertEquals(1, report.skippedEmptyResult)
        assertEquals(1, report.skippedNoPrefix)
        val entries = listOf(
            RenameEntry("tree/IMG_a.jpg", "IMG_a.jpg"),
            RenameEntry("tree/IMG_smoke_a.jpg", "IMG_smoke_a.jpg"),
            RenameEntry("tree/smoke_a.jpg", "smoke_a.jpg"),
            RenameEntry("tree/IMG_", "IMG_"),
        )
        val rows = skippedRowsFor(report, entries, listOf("cloud.jpg"))
        assertEquals(
            listOf(
                SkippedRow("IMG_", "empty result"),
                SkippedRow("IMG_smoke_a.jpg", "target is a file staying put"),
                SkippedRow("cloud.jpg", "unsupported file type"),
                SkippedRow("smoke_a.jpg", "no camera prefix"),
            ),
            rows,
        )
    }

    @Test
    fun collisionClassesRenderDistinctly() {
        val entries = listOf(
            RenameEntry("tree/smoke_a.jpg", "smoke_a.jpg"),
            RenameEntry("tree/IMG_smoke_a.jpg", "IMG_smoke_a.jpg"),
            RenameEntry("tree/IMG_a.jpg", "IMG_a.jpg"),
            RenameEntry("tree/IMG_b.jpg", "IMG_b.jpg"),
            RenameEntry("tree/VID_b.jpg", "VID_b.jpg"),
        )
        val existing = entries.map { it.path }.toSet() + "tree/a.jpg"
        val report = buildPlan(entries, existing)
        assertEquals(1, report.toRename)
        assertEquals(3, report.collisionCount)
        assertEquals(1, report.skippedNoPrefix)
        val rows = skippedRowsFor(report, entries, emptyList())
        assertEquals(
            listOf(
                SkippedRow("IMG_a.jpg", "target already exists"),
                SkippedRow(
                    "IMG_smoke_a.jpg",
                    "target is a file staying put",
                ),
                SkippedRow(
                    "VID_b.jpg",
                    "duplicate target in this batch",
                ),
                SkippedRow("smoke_a.jpg", "no camera prefix"),
            ),
            rows,
        )
    }

    @Test
    fun reportTextContainsCounts() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.refuse = setOf("doc://IMG_a.jpg")
        val result = runPlan(report, backend)
        val text = buildExecuteReportText(
            folderName = "Pictures",
            discovery = discoveryCounts(
                photos = 2,
                videos = 0,
                unsupported = 0,
                skippedDirs = 0,
                skippedRows = 0,
                warnings = emptyList(),
            ),
            plan = planCounts(report),
            result = result,
            manifestName = "renames_1.csv",
            undoAvailable = true,
        )
        assertTrue(text.contains("renamed=1/2"))
        assertTrue(text.contains("skipped=1"))
        assertTrue(text.contains("Manifest: renames_1.csv"))
        assertTrue(text.contains("Undo available"))
        val undoText = buildUndoReportText(
            folderName = "Pictures",
            result = UndoResult(emptyList()),
            manifestName = "renames_1.csv",
        )
        assertTrue(undoText.contains("restored=0"))
    }

    @Test
    fun destinationCheckThrowingSkipsOpAndBatchContinues() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.throwOnCheck = setOf("a.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertEquals(0, result.unverifiedCount)
        assertTrue(
            result.results.first {
                it.op.oldName == "IMG_a.jpg"
            }.detail.contains("could not check destination"),
        )
        assertTrue(
            backend.renameCalls.none { it.first == "doc://IMG_a.jpg" },
        )
        assertTrue(
            backend.renameCalls.contains("doc://IMG_b.jpg" to "b.jpg"),
        )
    }

    @Test
    fun destinationPresentAtExecuteSkipsWithoutRename() {
        val report = planFor("IMG_a.jpg")
        val backend = backendFor("IMG_a.jpg")
        backend.liveUris.add("doc://a.jpg")
        backend.names["doc://a.jpg"] = "a.jpg"
        val result = runPlan(report, backend)
        assertEquals(0, result.renamedCount)
        assertEquals(1, result.skippedCount)
        assertTrue(
            result.results[0].detail.contains("target already exists"),
        )
        assertTrue(backend.renameCalls.isEmpty())
    }

    @Test
    fun partialBatchCheckFailureKeepsManifestHonest() {
        val report = planFor("IMG_a.jpg", "IMG_b.jpg")
        val backend = backendFor("IMG_a.jpg", "IMG_b.jpg")
        backend.throwOnCheck = setOf("b.jpg")
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(1, result.skippedCount)
        val csv = manifestToCsv(
            report.planned,
            mapOf(
                "tree/IMG_a.jpg" to "doc://IMG_a.jpg",
                "tree/IMG_b.jpg" to "doc://IMG_b.jpg",
            ),
            executed = true,
            renamedOps = result.renamedOps,
        )
        val rows = parseManifestCsv(csv)
        assertEquals(
            "renamed",
            rows.first { it.originalName == "IMG_a.jpg" }.status,
        )
        assertEquals(
            "skipped",
            rows.first { it.originalName == "IMG_b.jpg" }.status,
        )
    }

    @Test
    fun postRenameUriIsPreservedInResult() {
        val report = planFor("IMG_a.jpg")
        val backend = FakeRenameBackend(
            newUriOf = { _, newName -> "doc://renamed-$newName" },
        )
        backend.liveUris.add("doc://IMG_a.jpg")
        backend.names["doc://IMG_a.jpg"] = "IMG_a.jpg"
        val result = runPlan(report, backend)
        assertEquals(1, result.renamedCount)
        assertEquals(
            mapOf(report.planned[0] to "doc://renamed-a.jpg"),
            result.renamedUris,
        )
    }

    @Test
    fun manifestCarriesPostRenameContentUri() {
        val report = planFor("IMG_a.jpg")
        val backend = FakeRenameBackend(
            newUriOf = { _, newName -> "doc://renamed-$newName" },
        )
        backend.liveUris.add("doc://IMG_a.jpg")
        backend.names["doc://IMG_a.jpg"] = "IMG_a.jpg"
        val result = runPlan(report, backend)
        val manifestUris = mutableMapOf(
            "tree/IMG_a.jpg" to "doc://IMG_a.jpg",
        )
        for ((op, uri) in result.renamedUris) {
            manifestUris[op.srcPath] = uri
        }
        val rows = parseManifestCsv(
            manifestToCsv(
                report.planned,
                manifestUris,
                executed = true,
                renamedOps = result.renamedOps,
            ),
        )
        assertEquals(1, rows.size)
        assertEquals("renamed", rows[0].status)
        assertEquals("doc://renamed-a.jpg", rows[0].contentUri)
    }

    @Test
    fun undoPrefersManifestContentUri() {
        val rows = listOf(
            ManifestRow(
                originalName = "IMG_a.jpg",
                newName = "a.jpg",
                originalPath = "tree/IMG_a.jpg",
                newPath = "tree/a.jpg",
                contentUri = "doc://current-a.jpg",
                status = "renamed",
            ),
        )
        val backend = FakeRenameBackend()
        backend.liveUris.add("doc://current-a.jpg")
        backend.names["doc://current-a.jpg"] = "a.jpg"
        val result = undoWith(rows, backend)
        assertEquals(1, result.restoredCount)
        assertEquals(0, result.errorCount)
        assertEquals(
            listOf("doc://current-a.jpg" to "IMG_a.jpg"),
            backend.renameCalls,
        )
    }

    @Test
    fun undoFallsBackToReconstructedPath() {
        val rows = listOf(
            ManifestRow(
                originalName = "IMG_a.jpg",
                newName = "a.jpg",
                originalPath = "tree/IMG_a.jpg",
                newPath = "tree/a.jpg",
                contentUri = "doc://IMG_a.jpg",
                status = "renamed",
            ),
        )
        val backend = backendFor("a.jpg")
        val result = undoWith(rows, backend)
        assertEquals(1, result.restoredCount)
        assertEquals(0, result.errorCount)
        assertEquals(
            listOf(
                "doc://IMG_a.jpg" to "IMG_a.jpg",
                "doc://a.jpg" to "IMG_a.jpg",
            ),
            backend.renameCalls,
        )
    }

    @Test
    fun undoAllCandidatesFailIsError() {
        val rows = listOf(
            ManifestRow(
                originalName = "IMG_a.jpg",
                newName = "a.jpg",
                originalPath = "tree/IMG_a.jpg",
                newPath = "tree/a.jpg",
                contentUri = "doc://IMG_a.jpg",
                status = "renamed",
            ),
        )
        val backend = FakeRenameBackend()
        val result = undoWith(rows, backend)
        assertEquals(0, result.restoredCount)
        assertEquals(1, result.errorCount)
        assertTrue(
            result.results[0].detail.contains("unknown document"),
        )
        assertEquals(
            listOf(
                "doc://IMG_a.jpg" to "IMG_a.jpg",
                "doc://a.jpg" to "IMG_a.jpg",
            ),
            backend.renameCalls,
        )
    }
}
