// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core.rename

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionOrderTest {
    @Test
    fun twoOpChainOrdersDependentFirst() {
        val report = buildPlan(
            entries = listOf(
                RenameEntry(path = "/t/IMG_VID_a.jpg", name = "IMG_VID_a.jpg"),
                RenameEntry(path = "/t/VID_a.jpg", name = "VID_a.jpg"),
            ),
            existingPaths = setOf("/t/IMG_VID_a.jpg", "/t/VID_a.jpg"),
        )
        assertEquals(2, report.toRename)
        val (ordered, cyclic) = executionOrder(report.planned)
        assertTrue(cyclic.isEmpty())
        // VID_a.jpg -> a.jpg runs first, so IMG_VID_a.jpg can then
        // move into the freed VID_a.jpg name. The un-ordered plan
        // would half-fail (the PR #1 bug).
        assertEquals(
            listOf("VID_a.jpg", "IMG_VID_a.jpg"),
            ordered.map { it.oldName },
        )
        assertEquals(listOf("a.jpg", "VID_a.jpg"), ordered.map { it.newName })
    }

    @Test
    fun threeOpChainOrdersLongestDependencyLast() {
        val ops = listOf(
            RenameOp("/t/IMG_VID_VID_a.jpg", "/t/VID_VID_a.jpg"),
            RenameOp("/t/VID_VID_a.jpg", "/t/VID_a.jpg"),
            RenameOp("/t/VID_a.jpg", "/t/a.jpg"),
        )
        val (ordered, cyclic) = executionOrder(ops)
        assertTrue(cyclic.isEmpty())
        assertEquals(
            listOf("VID_a.jpg", "VID_VID_a.jpg", "IMG_VID_VID_a.jpg"),
            ordered.map { it.oldName },
        )
    }

    @Test
    fun independentOpsOrderByOldNameInOneWave() {
        val ops = listOf(
            RenameOp("/t/IMG_b.jpg", "/t/b.jpg"),
            RenameOp("/t/IMG_a.jpg", "/t/a.jpg"),
        )
        val (ordered, cyclic) = executionOrder(ops)
        assertTrue(cyclic.isEmpty())
        assertEquals(listOf("IMG_a.jpg", "IMG_b.jpg"), ordered.map { it.oldName })
    }

    @Test
    fun genuineCycleIsReportedNotHungOn() {
        // Cycles cannot arise from buildPlan (stripped names are
        // strictly shorter than sources), but executionOrder takes
        // arbitrary ops — a crafted cycle must be reported, never
        // looped on.
        val ops = listOf(
            RenameOp("/t/A.jpg", "/t/B.jpg"),
            RenameOp("/t/B.jpg", "/t/A.jpg"),
        )
        val (ordered, cyclic) = executionOrder(ops)
        assertTrue(ordered.isEmpty())
        assertEquals(2, cyclic.size)
    }

    @Test
    fun emptyPlanOrdersToEmpty() {
        val (ordered, cyclic) = executionOrder(emptyList())
        assertTrue(ordered.isEmpty())
        assertTrue(cyclic.isEmpty())
    }
}
