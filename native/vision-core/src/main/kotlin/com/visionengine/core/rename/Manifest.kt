// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

/**
 * Manifest CSV codec — port of `packages/rename_core/lib/src/manifest.dart`,
 * with the PR #1 honest statuses from `rename_camera_prefixes.py`
 * (`write_manifest` / `undo_from_manifest`).
 *
 * Six columns — the Dart form (spec S2 §6.4, reconciled with S4 §4.1):
 * a SAF rename's identity is the document URI, and the Python
 * 5-column form cannot name one. Parsing is header-driven, so a
 * 5-column Python manifest still reads (missing `content_uri`
 * defaults to `""`).
 */
val manifestFieldnames: List<String> = listOf(
    "original_name",
    "new_name",
    "original_path",
    "new_path",
    "content_uri",
    "status",
)

/** Manifest statuses — honest about what actually happened (PR #1). */
const val MANIFEST_STATUS_PLANNED: String = "planned"
const val MANIFEST_STATUS_RENAMED: String = "renamed"
const val MANIFEST_STATUS_SKIPPED: String = "skipped"

private fun escape(value: String): String {
    if (value.contains(',') ||
        value.contains('"') ||
        value.contains('\n') ||
        value.contains('\r')
    ) {
        return "\"${value.replace("\"", "\"\"")}\""
    }
    return value
}

/**
 * Write [ops] as a manifest CSV (header + one row per op).
 *
 * Statuses are honest (PR #1 — the pre-PR #1 behavior of stamping
 * `renamed` on every row must never come back):
 * - [executed] is false (dry run / unexecuted plan) -> every row is
 *   `planned`.
 * - [executed] is true -> only ops in [renamedOps] are `renamed`;
 *   planned ops that did not run are `skipped`.
 *
 * The caller passes [renamedOps] as the set of ops that really
 * executed; [RenameOp] is a data class, so set membership is by
 * src/dst path pair.
 */
fun manifestToCsv(
    ops: List<RenameOp>,
    contentUris: Map<String, String> = emptyMap(),
    executed: Boolean = false,
    renamedOps: Set<RenameOp> = emptySet(),
): String {
    val out = StringBuilder()
    out.append(manifestFieldnames.joinToString(",")).append('\n')
    for (op in ops) {
        val status = when {
            !executed -> MANIFEST_STATUS_PLANNED
            op in renamedOps -> MANIFEST_STATUS_RENAMED
            else -> MANIFEST_STATUS_SKIPPED
        }
        out.append(
            listOf(
                escape(op.oldName),
                escape(op.newName),
                escape(op.srcPath),
                escape(op.dstPath),
                escape(contentUris[op.srcPath] ?: ""),
                escape(status),
            ).joinToString(","),
        ).append('\n')
    }
    return out.toString()
}

/**
 * Split one CSV record into fields. Quote-aware, mirroring Dart's
 * `_splitCsvLine`: `""` inside a quoted field is one `"`, and a field
 * containing `,`, `"` or a newline is quoted on write.
 */
private fun splitCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (inQuotes) {
            if (ch == '"') {
                if (i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i += 2
                } else {
                    inQuotes = false
                    i += 1
                }
            } else {
                current.append(ch)
                i += 1
            }
        } else {
            if (ch == '"') {
                inQuotes = true
                i += 1
            } else if (ch == ',') {
                fields.add(current.toString())
                current.clear()
                i += 1
            } else {
                current.append(ch)
                i += 1
            }
        }
    }
    fields.add(current.toString())
    return fields
}

/**
 * Split raw CSV text into record strings, respecting quotes: a newline
 * (`\n`, `\r\n`, or lone `\r`) ends a record only OUTSIDE a quoted
 * field. A newline inside a quoted field is field content.
 *
 * This is a deliberate, spec-driven difference from Dart's
 * `parseManifestCsv`, which splits the text into lines first and
 * therefore cannot round-trip a name containing a newline (spec S2
 * §6.6 requires that round-trip). Quoting and field-splitting rules
 * are identical to Dart's; only the record split is quote-aware.
 * A `\r\n` inside a quoted field is preserved verbatim (Dart's
 * pre-split `replaceAll('\r\n', '\n')` would collapse it).
 */
private fun splitCsvRecords(text: String): List<String> {
    val records = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (inQuotes) {
            if (ch == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    current.append("\"\"")
                    i += 2
                } else {
                    current.append(ch)
                    inQuotes = false
                    i += 1
                }
            } else {
                current.append(ch)
                i += 1
            }
        } else {
            if (ch == '"') {
                current.append(ch)
                inQuotes = true
                i += 1
            } else if (ch == '\r') {
                // CRLF or lone CR: one record separator.
                if (i + 1 < text.length && text[i + 1] == '\n') {
                    i += 2
                } else {
                    i += 1
                }
                if (current.isNotEmpty()) {
                    records.add(current.toString())
                    current.clear()
                }
            } else if (ch == '\n') {
                i += 1
                if (current.isNotEmpty()) {
                    records.add(current.toString())
                    current.clear()
                }
            } else {
                current.append(ch)
                i += 1
            }
        }
    }
    if (current.isNotEmpty()) {
        records.add(current.toString())
    }
    return records
}

/**
 * Parse a manifest CSV. Header-driven (columns are mapped by field
 * name, not position), CRLF tolerant, missing fields default to `""`
 * — so a 5-column Python manifest parses with `contentUri == ""`,
 * and a reordered header still maps correctly.
 */
fun parseManifestCsv(csv: String): List<ManifestRow> {
    val records = splitCsvRecords(csv)
    if (records.isEmpty()) {
        return emptyList()
    }
    val header = splitCsvLine(records.first())
    val rows = mutableListOf<ManifestRow>()
    for (record in records.drop(1)) {
        val fields = splitCsvLine(record)
        val byName = mutableMapOf<String, String>()
        for (i in header.indices) {
            if (i < fields.size) {
                byName[header[i]] = fields[i]
            }
        }
        rows.add(
            ManifestRow(
                originalName = byName["original_name"] ?: "",
                newName = byName["new_name"] ?: "",
                originalPath = byName["original_path"] ?: "",
                newPath = byName["new_path"] ?: "",
                contentUri = byName["content_uri"] ?: "",
                status = byName["status"] ?: "",
            ),
        )
    }
    return rows
}

/**
 * Undo order: reverse the manifest rows, so later renames undo first
 * (safest if names overlapped). Port of Dart's `undoOrder` / Python's
 * `undo_from_manifest` reversal.
 */
fun undoOrder(rows: List<ManifestRow>): List<ManifestRow> = rows.reversed()

/**
 * The undo status gate (PR #1): a row is restorable ONLY if its
 * status is `renamed`. Rows with `planned`, `skipped`, or any other
 * status were never renamed — there is nothing to restore, and a
 * dry-run manifest must never undo renames that never happened.
 * The undo executor itself is S4; this slice supplies the codec and
 * this pure predicate.
 */
fun isRestorable(row: ManifestRow): Boolean = row.status == MANIFEST_STATUS_RENAMED
