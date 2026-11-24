// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.debugoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** MainActivity — the reusable smoke-checklist screen. */
class MainActivity : ComponentActivity() {

    private val store = ChecklistStore(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                },
            ) {
                SmokeChecklistScreen(store)
            }
        }
    }
}

@Composable
fun SmokeChecklistScreen(store: ChecklistStore) {
    var items by remember { mutableStateOf(store.load()) }
    val scope = rememberCoroutineScope()

    fun save(item: ChecklistItem) {
        store.save(item)
        items = items.map { if (it.id == item.id) item else it }
    }

    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Vision Engine — Smoke Checklist",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        save(item.copy(checked = checked))
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
                Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                    Text(item.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "status: ${item.status.name}" + if (item.auto) " (auto)" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = item.notes,
                        onValueChange = { save(item.copy(notes = it)) },
                        placeholder = { Text("notes (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    for (item in items.filter { it.auto }) {
                        val (status, detail) = when (item.id) {
                            "launch_offline" -> AutoChecks.launchOffline(
                                store.context,
                                OverlayConfig.MAIN_APP_PACKAGE,
                            )
                            "focus_offline" -> AutoChecks.focusOffline(
                                store.context,
                                OverlayConfig.MAIN_APP_PACKAGE,
                            )
                            else -> ItemStatus.OPEN to ""
                        }
                        val updated = item.copy(status = status)
                        store.save(updated)
                        if (detail.isNotBlank()) {
                            val appended = (updated.notes + if (updated.notes.isBlank()) "" else "; " + detail).trim()
                            store.setNotes(item.id, appended)
                        }
                    }
                    withContext(Dispatchers.Main.immediate) {
                        items = store.load()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text("Run auto checks")
        }
    }
}
