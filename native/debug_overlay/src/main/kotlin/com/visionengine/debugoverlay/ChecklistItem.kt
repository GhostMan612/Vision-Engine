// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

/**
 * One reusable smoke-test checklist item.
 *
 * [auto] items run their check (see [AutoChecks]) and update their
 * [status]/[notes]; [checked] is the human's checkbox, persisted per
 * item id in app-private storage (see [ChecklistStore]).
 */
enum class ItemStatus {
    OPEN,
    PASS,
    FAIL,
}

data class ChecklistItem(
    val id: String,
    val label: String,
    val description: String,
    val auto: Boolean = false,
    val status: ItemStatus = ItemStatus.OPEN,
    val checked: Boolean = false,
    val notes: String = "",
)
