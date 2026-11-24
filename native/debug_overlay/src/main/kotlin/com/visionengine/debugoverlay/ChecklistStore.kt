// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

import android.content.Context
import android.content.SharedPreferences

/**
 * App-private checklist store (never touches DCIM or media storage).
 * State is keyed per item id: "<id>.status", "<id>.checked", "<id>.notes".
 */
class ChecklistStore(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("checklist", Context.MODE_PRIVATE)

    /** Load all items: persisted state for defaults, fresh defaults otherwise. */
    fun load(): List<ChecklistItem> =
        DefaultItems.map { default ->
            default.copy(
                status = ItemStatus.entries.getOrNull(prefs.getInt("${default.id}.status", -1))
                    ?: default.status,
                checked = prefs.getBoolean("${default.id}.checked", default.checked),
                notes = prefs.getString("${default.id}.notes", default.notes) ?: default.notes,
            )
        }

    fun save(item: ChecklistItem) {
        prefs.edit()
            .putInt("${item.id}.status", item.status.ordinal)
            .putBoolean("${item.id}.checked", item.checked)
            .putString("${item.id}.notes", item.notes)
            .apply()
    }

    fun setStatus(id: String, status: ItemStatus) {
        prefs.edit().putInt("$id.status", status.ordinal).apply()
    }

    fun setChecked(id: String, checked: Boolean) {
        prefs.edit().putBoolean("$id.checked", checked).apply()
    }

    fun setNotes(id: String, notes: String) {
        prefs.edit().putString("$id.notes", notes).apply()
    }
}
