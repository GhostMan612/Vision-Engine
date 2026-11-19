// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

/**
 * Order planned renames so chains resolve instead of half-failing.
 * Pure port of `_execution_order` in `rename_camera_prefixes.py`
 * (PR #1 chain fix).
 *
 * An op whose destination is another op's source runs AFTER that op:
 * `VID_a.jpg -> a.jpg` must execute before
 * `IMG_VID_a.jpg -> VID_a.jpg`, or the second rename hits the
 * "target exists" guard and is skipped.
 *
 * Returns `(ordered, cyclic)`: ops in execution order, plus any
 * leftovers. Stripped names are strictly shorter than sources, so no
 * cycles can exist from [buildPlan] output; anything left over is
 * reported as cyclic rather than hung on. [buildPlan] callers treat a
 * non-empty `cyclic` list as a defect to report, never as a reason
 * to loop forever.
 *
 * Pure logic only — tested here, wired to real renames in S4. This
 * function never touches the filesystem.
 */
fun executionOrder(ops: List<RenameOp>): Pair<List<RenameOp>, List<RenameOp>> {
    val pending = ops.toMutableList()
    val ordered = mutableListOf<RenameOp>()
    while (pending.isNotEmpty()) {
        val sources = pending.map { it.srcPath }.toSet()
        val ready = pending
            .filter { it.dstPath !in sources }
            .sortedBy { it.oldName }
        if (ready.isEmpty()) {
            break
        }
        ordered.addAll(ready)
        pending.removeAll(ready.toSet())
    }
    return ordered to pending
}
