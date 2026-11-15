// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

private data class Candidate(val entry: RenameEntry, val stripped: String)

private fun parentOf(entry: RenameEntry): String =
    entry.path.substring(0, entry.path.length - entry.name.length)

/**
 * Build a validated rename plan over caller-supplied entries, changing
 * nothing. Port of `buildPlan` in `packages/rename_core/lib/src/plan.dart`,
 * which ports `build_plan` in `rename_camera_prefixes.py` (canonical,
 * post-PR #1 semantics).
 *
 * Pure: no filesystem access — [existingPaths] is the caller-supplied
 * set of full paths known to exist on disk (in Python this is a live
 * `Path.exists()` check; here it is a parameter, which is what makes
 * the function JVM-pure and testable).
 *
 * Behavior, in order (spec S2 §6.2):
 * 1. Sort entries first — by name, or by full path when [recursive].
 * 2. Classify each entry: directories -> `skippedIsDir`; non-files ->
 *    `skippedNoPrefix`; no prefix match -> stays (recorded in
 *    `stayingPaths`, counted `skippedNoPrefix`); empty strip result ->
 *    `skippedEmptyResult`; otherwise a candidate.
 * 3. Collision checks per candidate, in this order:
 *    - (1) destination is in [existingPaths] AND is not itself a
 *      candidate source -> `"<name> -> <stripped> (target exists)"`.
 *    - (2) destination is in `stayingPaths` (a file staying put —
 *      K2, the check naive ports miss) -> same `"(target exists)"`.
 *    - (3) destination already seen from an earlier candidate ->
 *      `"<name> -> <stripped> (duplicate target in batch)"`.
 * 4. Planned ops sorted by old name, tie-break by source path.
 *
 * Symlinks: NOT handled here. `buildPlan` takes entries and cannot
 * detect symlinks; the enumerator (S3/S4) owns skipping them and
 * records them in [RenameReport.skippedSymlink]. See
 * [RenameReport] — do not "fix" this by adding detection here.
 */
fun buildPlan(
    entries: List<RenameEntry>,
    existingPaths: Set<String>,
    prefixes: List<String> = defaultPrefixes,
    caseSensitive: Boolean = true,
    recursive: Boolean = false,
): RenameReport {
    val report = RenameReport()
    val ordered = if (recursive) {
        entries.sortedWith(compareBy { it.path })
    } else {
        entries.sortedWith(compareBy { it.name })
    }
    val stayingPaths = mutableSetOf<String>()
    val candidates = mutableListOf<Candidate>()
    for (entry in ordered) {
        report.scanned += 1
        if (entry.isDir) {
            report.skippedIsDir += 1
            continue
        }
        if (!entry.isFile) {
            report.skippedNoPrefix += 1
            continue
        }
        val stripped = stripLeadingPrefix(
            entry.name,
            prefixes = prefixes,
            caseSensitive = caseSensitive,
        )
        if (stripped == null) {
            stayingPaths.add(entry.path)
            report.skippedNoPrefix += 1
        } else if (stripped.isEmpty()) {
            report.skippedEmptyResult += 1
        } else {
            candidates.add(Candidate(entry, stripped))
        }
    }
    val candidateSrcs = candidates.map { it.entry.path }.toSet()
    val seenDestinations = mutableSetOf<String>()
    for (candidate in candidates) {
        val dst = parentOf(candidate.entry) + candidate.stripped
        if (dst in existingPaths && dst !in candidateSrcs) {
            report.skippedCollision.add(
                "${candidate.entry.name} -> ${candidate.stripped} (target exists)",
            )
            continue
        }
        if (dst in stayingPaths) {
            report.skippedCollision.add(
                "${candidate.entry.name} -> ${candidate.stripped} (target exists)",
            )
            continue
        }
        if (dst in seenDestinations) {
            report.skippedCollision.add(
                "${candidate.entry.name} -> ${candidate.stripped} (duplicate target in batch)",
            )
            continue
        }
        seenDestinations.add(dst)
        report.planned.add(RenameOp(srcPath = candidate.entry.path, dstPath = dst))
    }
    report.planned.sortWith(
        compareBy<RenameOp> { it.oldName }.thenBy { it.srcPath },
    )
    return report
}
