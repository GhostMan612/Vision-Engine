// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import com.visionengine.core.rename.ManifestRow
import com.visionengine.core.rename.RenameEntry
import com.visionengine.core.rename.RenameOp
import com.visionengine.core.rename.RenameReport
import com.visionengine.core.rename.isRestorable
import com.visionengine.core.rename.stripLeadingPrefix
import com.visionengine.core.rename.undoOrder

enum class OpOutcome {
    RENAMED,
    SKIPPED,
    ERROR,
    UNVERIFIED,
}

data class OpResult(
    val op: RenameOp,
    val outcome: OpOutcome,
    val detail: String,
)

data class ExecuteResult(val results: List<OpResult>) {
    val renamedOps: Set<RenameOp> =
        results.filter { it.outcome == OpOutcome.RENAMED }
            .map { it.op }.toSet()
    val renamedCount: Int =
        results.count { it.outcome == OpOutcome.RENAMED }
    val skippedCount: Int =
        results.count { it.outcome == OpOutcome.SKIPPED }
    val errorCount: Int =
        results.count { it.outcome == OpOutcome.ERROR }
    val unverifiedCount: Int =
        results.count { it.outcome == OpOutcome.UNVERIFIED }
}

interface RenameBackend {
    fun rename(documentUri: String, newName: String): String?
    fun displayName(documentUri: String): String?
    fun destinationExists(documentUri: String): Boolean
}

private fun messageOf(e: Exception): String =
    e.message ?: e.javaClass.simpleName

fun executePlan(
    ordered: List<RenameOp>,
    cyclic: List<RenameOp>,
    sourceUriOf: (RenameOp) -> String,
    destinationUriOf: (RenameOp) -> String,
    backend: RenameBackend,
): ExecuteResult {
    val results = mutableListOf<OpResult>()
    for (op in ordered) {
        val destinationUri = destinationUriOf(op)
        val occupied = try {
            backend.destinationExists(destinationUri)
        } catch (e: Exception) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.SKIPPED,
                    "could not check destination: ${messageOf(e)}",
                ),
            )
            continue
        }
        if (occupied) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.SKIPPED,
                    "target already exists",
                ),
            )
            continue
        }
        val sourceUri = sourceUriOf(op)
        val newUri = try {
            backend.rename(sourceUri, op.newName)
        } catch (e: Exception) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.ERROR,
                    "rename failed: ${messageOf(e)}",
                ),
            )
            continue
        }
        if (newUri == null) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.SKIPPED,
                    "rename refused",
                ),
            )
            continue
        }
        val actual = try {
            backend.displayName(newUri)
        } catch (e: Exception) {
            null
        }
        if (actual == null) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.UNVERIFIED,
                    "rename reported success but the document " +
                        "could not be re-read",
                ),
            )
            continue
        }
        if (actual != op.newName) {
            results.add(
                OpResult(
                    op,
                    OpOutcome.UNVERIFIED,
                    "rename reported success but the document is " +
                        "still named $actual",
                ),
            )
            continue
        }
        results.add(
            OpResult(
                op,
                OpOutcome.RENAMED,
                "renamed",
            ),
        )
    }
    for (op in cyclic) {
        results.add(
            OpResult(
                op,
                OpOutcome.ERROR,
                "cyclic rename order",
            ),
        )
    }
    return ExecuteResult(results)
}

enum class UndoOutcome {
    RESTORED,
    SKIPPED,
    ERROR,
}

data class UndoRowResult(
    val row: ManifestRow,
    val outcome: UndoOutcome,
    val detail: String,
)

data class UndoResult(val results: List<UndoRowResult>) {
    val restoredCount: Int =
        results.count { it.outcome == UndoOutcome.RESTORED }
    val skippedCount: Int =
        results.count { it.outcome == UndoOutcome.SKIPPED }
    val errorCount: Int =
        results.count { it.outcome == UndoOutcome.ERROR }
}

fun undoRows(
    rows: List<ManifestRow>,
    documentUriOf: (ManifestRow) -> String,
    destinationUriOf: (ManifestRow) -> String,
    backend: RenameBackend,
): UndoResult {
    val results = mutableListOf<UndoRowResult>()
    for (row in undoOrder(rows)) {
        if (!isRestorable(row)) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.SKIPPED,
                    "not a completed rename",
                ),
            )
            continue
        }
        val destinationUri = destinationUriOf(row)
        val occupied = try {
            backend.destinationExists(destinationUri)
        } catch (e: Exception) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.ERROR,
                    "could not check destination: ${messageOf(e)}",
                ),
            )
            continue
        }
        if (occupied) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.ERROR,
                    "target already exists",
                ),
            )
            continue
        }
        val documentUri = documentUriOf(row)
        val newUri = try {
            backend.rename(documentUri, row.originalName)
        } catch (e: Exception) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.ERROR,
                    "rename failed: ${messageOf(e)}",
                ),
            )
            continue
        }
        if (newUri == null) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.ERROR,
                    "rename refused",
                ),
            )
            continue
        }
        val actual = try {
            backend.displayName(newUri)
        } catch (e: Exception) {
            null
        }
        if (actual != row.originalName) {
            results.add(
                UndoRowResult(
                    row,
                    UndoOutcome.ERROR,
                    "rename reported success but the document " +
                        "could not be verified",
                ),
            )
            continue
        }
        results.add(
            UndoRowResult(
                row,
                UndoOutcome.RESTORED,
                "restored",
            ),
        )
    }
    return UndoResult(results)
}

data class DiscoveryCounts(
    val scanned: Int,
    val photos: Int,
    val videos: Int,
    val unsupported: Int,
    val skippedDirs: Int,
    val skippedRows: Int,
    val warnings: List<String>,
)

fun discoveryCounts(
    photos: Int,
    videos: Int,
    unsupported: Int,
    skippedDirs: Int,
    skippedRows: Int,
    warnings: List<String>,
): DiscoveryCounts {
    return DiscoveryCounts(
        scanned = photos + videos + unsupported + skippedDirs + skippedRows,
        photos = photos,
        videos = videos,
        unsupported = unsupported,
        skippedDirs = skippedDirs,
        skippedRows = skippedRows,
        warnings = warnings,
    )
}

data class PlanCounts(
    val toRename: Int,
    val clean: Int,
    val collisions: Int,
    val emptyResults: Int,
    val symlinks: Int,
    val scanned: Int,
)

fun planCounts(report: RenameReport): PlanCounts {
    return PlanCounts(
        toRename = report.toRename,
        clean = report.skippedNoPrefix,
        collisions = report.collisionCount,
        emptyResults = report.skippedEmptyResult,
        symlinks = report.skippedSymlink,
        scanned = report.scanned,
    )
}

fun countsReconcile(
    discovery: DiscoveryCounts,
    plan: PlanCounts,
): Boolean {
    val planAddsUp =
        plan.toRename + plan.clean + plan.emptyResults +
            plan.collisions + plan.symlinks == plan.scanned
    val discoveryAddsUp =
        plan.scanned + discovery.unsupported + discovery.skippedDirs +
            discovery.skippedRows == discovery.scanned
    return planAddsUp && discoveryAddsUp
}

data class SkippedRow(
    val name: String,
    val reason: String,
)

fun skippedRowsFor(
    report: RenameReport,
    entries: List<RenameEntry>,
    unsupportedNames: List<String>,
): List<SkippedRow> {
    val rows = mutableListOf<SkippedRow>()
    val plannedOld = report.planned.map { it.oldName }.toSet()
    val collisionByName = mutableMapOf<String, String>()
    for (line in report.skippedCollision) {
        val name = line.substringBefore(" -> ", "")
        val reason = line.substringAfterLast("(", "").removeSuffix(")")
        if (name.isNotEmpty() && reason.isNotEmpty()) {
            collisionByName[name] = reason
        }
    }
    for (entry in entries) {
        if (entry.name in plannedOld) {
            continue
        }
        val collisionReason = collisionByName[entry.name]
        if (collisionReason != null) {
            rows.add(SkippedRow(entry.name, collisionReason))
            continue
        }
        val stripped = stripLeadingPrefix(entry.name)
        when {
            stripped == null -> rows.add(
                SkippedRow(entry.name, "no camera prefix"),
            )
            stripped.isEmpty() -> rows.add(
                SkippedRow(entry.name, "empty result"),
            )
            else -> rows.add(
                SkippedRow(entry.name, "target already exists"),
            )
        }
    }
    for (name in unsupportedNames) {
        rows.add(SkippedRow(name, "unsupported file type"))
    }
    return rows.sortedBy { it.name }
}

fun buildExecuteReportText(
    folderName: String,
    discovery: DiscoveryCounts,
    plan: PlanCounts,
    result: ExecuteResult,
    manifestName: String?,
    undoAvailable: Boolean,
): String {
    val out = StringBuilder()
    out.append("Renamer report — ").append(folderName).append('\n')
    out.append("Scanned=").append(discovery.scanned)
        .append(" photos=").append(discovery.photos)
        .append(" videos=").append(discovery.videos)
        .append(" unsupported=").append(discovery.unsupported)
        .append(" dirs=").append(discovery.skippedDirs).append('\n')
    out.append("Plan to-rename=").append(plan.toRename)
        .append(" clean=").append(plan.clean)
        .append(" collisions=").append(plan.collisions)
        .append(" empty=").append(plan.emptyResults).append('\n')
    out.append("DONE renamed=").append(result.renamedCount)
        .append('/').append(result.results.size)
        .append(" skipped=").append(result.skippedCount)
        .append(" errors=").append(result.errorCount)
        .append(" unverified=").append(result.unverifiedCount).append('\n')
    for (entry in result.results) {
        if (entry.outcome == OpOutcome.RENAMED) {
            continue
        }
        out.append(entry.outcome.name).append(' ')
            .append(entry.op.oldName).append(" -> ")
            .append(entry.op.newName).append(": ")
            .append(entry.detail).append('\n')
    }
    if (manifestName != null) {
        out.append("Manifest: ").append(manifestName)
            .append(" (app-private storage; clearing app data ")
            .append("loses undo history)").append('\n')
    } else {
        out.append("Manifest: not written — undo is unavailable")
            .append('\n')
    }
    if (undoAvailable) {
        out.append("Undo available").append('\n')
    }
    return out.toString()
}

fun buildUndoReportText(
    folderName: String,
    result: UndoResult,
    manifestName: String?,
): String {
    val out = StringBuilder()
    out.append("Undo report — ").append(folderName).append('\n')
    out.append("UNDO restored=").append(result.restoredCount)
        .append(" skipped=").append(result.skippedCount)
        .append(" errors=").append(result.errorCount).append('\n')
    for (entry in result.results) {
        if (entry.outcome == UndoOutcome.RESTORED) {
            continue
        }
        out.append(entry.outcome.name).append(' ')
            .append(entry.row.newName).append(" -> ")
            .append(entry.row.originalName).append(": ")
            .append(entry.detail).append('\n')
    }
    if (manifestName != null) {
        out.append("Manifest: ").append(manifestName).append('\n')
    }
    return out.toString()
}
