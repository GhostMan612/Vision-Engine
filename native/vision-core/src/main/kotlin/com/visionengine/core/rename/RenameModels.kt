// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

/**
 * Pure rename models — port of `packages/rename_core/lib/src/model.dart`.
 *
 * No filesystem access, no Android types: these are plain values the
 * plan/manifest functions operate on. The enumerator (S3/S4) owns
 * building [RenameEntry]s from real directories.
 */
fun basenameOf(path: String): String {
    val slash = path.lastIndexOf('/')
    val backslash = path.lastIndexOf('\\')
    val cut = if (slash > backslash) slash else backslash
    return if (cut < 0) path else path.substring(cut + 1)
}

/**
 * One discovered filesystem entry, as supplied by the caller.
 * Mirrors the Dart form: `buildPlan` receives entries, it does not
 * enumerate a directory itself — that is what keeps it JVM-pure.
 */
data class RenameEntry(
    val path: String,
    val name: String,
    val isDir: Boolean = false,
    val isFile: Boolean = true,
)

/**
 * One planned rename: [srcPath] -> [dstPath].
 *
 * A data class on purpose: structural equality means a [RenameOp] can
 * be looked up in the set of actually-renamed ops when the manifest
 * writer stamps honest statuses (see [manifestToCsv]).
 */
data class RenameOp(
    val srcPath: String,
    val dstPath: String,
) {
    val oldName: String get() = basenameOf(srcPath)
    val newName: String get() = basenameOf(dstPath)
}

/**
 * Outcome of [buildPlan]. Mirrors the Dart report, plus a
 * [skippedSymlink] counter.
 *
 * Symlink note (spec S2 §6.2): `buildPlan` NEVER increments
 * [skippedSymlink] — it takes pre-built entries and cannot detect
 * symlinks. The counter exists so the enumerator layer (S3/S4) can
 * record symlinks it skipped before handing entries to the plan.
 * Any test claiming `buildPlan` skips symlinks would be testing a
 * fiction.
 */
class RenameReport {
    var scanned: Int = 0
    var planned: MutableList<RenameOp> = mutableListOf()
    var skippedNoPrefix: Int = 0
    var skippedIsDir: Int = 0
    var skippedEmptyResult: Int = 0
    var skippedSymlink: Int = 0
    var skippedCollision: MutableList<String> = mutableListOf()

    val toRename: Int get() = planned.size
    val collisionCount: Int get() = skippedCollision.size
}

/**
 * One row of a rename manifest CSV. Six columns — the Dart form
 * (spec S2 §6.4, reconciled with S4 §4.1): a SAF rename's identity
 * is the document URI, and the Python 5-column form cannot name one.
 * `contentUri` is empty when the rename was planned from plain
 * filesystem paths.
 */
data class ManifestRow(
    val originalName: String,
    val newName: String,
    val originalPath: String,
    val newPath: String,
    val contentUri: String,
    val status: String,
)
