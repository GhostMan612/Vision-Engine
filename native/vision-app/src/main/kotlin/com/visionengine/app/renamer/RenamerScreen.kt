// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RenamerScreen(
    folders: List<FolderState>,
    renamerViewModel: RenamerViewModel,
    resolver: ContentResolver,
    scope: CoroutineScope,
    onRepick: (String) -> Unit,
) {
    val selectedGrant by renamerViewModel.selectedGrant
        .collectAsStateWithLifecycle()
    val grantLost by renamerViewModel.grantLost
        .collectAsStateWithLifecycle()
    val discovery by renamerViewModel.discovery
        .collectAsStateWithLifecycle()
    val plan by renamerViewModel.plan.collectAsStateWithLifecycle()
    val planCounts by renamerViewModel.planCounts
        .collectAsStateWithLifecycle()
    val skippedRows by renamerViewModel.skippedRows
        .collectAsStateWithLifecycle()
    val previewFresh by renamerViewModel.previewFresh
        .collectAsStateWithLifecycle()
    val executing by renamerViewModel.executing
        .collectAsStateWithLifecycle()
    val manifestFile by renamerViewModel.manifestFile
        .collectAsStateWithLifecycle()
    val undoResult by renamerViewModel.undoResult
        .collectAsStateWithLifecycle()
    val undoRemaining by renamerViewModel.undoRemaining
        .collectAsStateWithLifecycle()
    val reportText by renamerViewModel.reportText
        .collectAsStateWithLifecycle()
    val error by renamerViewModel.error.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                renamerViewModel.exportManifest(uri, resolver)
            }
        }
    }
    LaunchedEffect(folders) {
        renamerViewModel.onFoldersChanged(folders)
    }
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (folders.isEmpty()) {
            Text(
                text = "No folders yet — pick a folder in the " +
                    "Folders tab.",
            )
            return@Column
        }
        Text(text = "Folder")
        for (folder in folders) {
            val selected =
                selectedGrant?.uri == folder.entry.uri
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = folder.entry.displayName)
                    when (folder.state) {
                        GrantState.HEALTHY -> Text(text = "Healthy")
                        GrantState.LOST -> Text(
                            text = "Vision lost access to " +
                                folder.entry.displayName + ".",
                        )
                        GrantState.NEVER_VERIFIED -> Text(
                            text = "Never verified",
                        )
                    }
                }
                if (selected) {
                    Text(text = "Selected")
                } else {
                    Button(onClick = {
                        scope.launch {
                            renamerViewModel.selectGrant(folder, resolver)
                        }
                    }) {
                        Text(text = "Select")
                    }
                }
            }
        }
        val grant = selectedGrant
        if (grant != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Selected: " + grant.displayName)
            if (grantLost) {
                Text(
                    text = "Vision lost access to " +
                        grant.displayName +
                        ". Re-pick the folder to restore it.",
                )
                Button(onClick = { onRepick(grant.uri) }) {
                    Text(text = "Re-pick")
                }
            }
            val counts = discovery
            if (counts != null) {
                Text(
                    text = "Scanned=" + counts.scanned +
                        " photos=" + counts.photos +
                        " videos=" + counts.videos +
                        " unsupported=" + counts.unsupported +
                        " dirs=" + counts.skippedDirs +
                        " skippedRows=" + counts.skippedRows,
                )
                for (warning in counts.warnings) {
                    Text(text = warning)
                }
            }
            Button(
                onClick = {
                    scope.launch { renamerViewModel.preview() }
                },
                enabled = !grantLost && !executing,
            ) {
                Text(text = "Preview")
            }
            val report = plan
            val countsNow = planCounts
            if (report != null && countsNow != null) {
                Text(
                    text = "to-rename=" + countsNow.toRename +
                        " clean=" + countsNow.clean +
                        " collisions=" + countsNow.collisions +
                        " empty=" + countsNow.emptyResults +
                        " symlinks=" + countsNow.symlinks,
                )
                val discoveryNow = discovery
                if (discoveryNow != null &&
                    !countsReconcile(discoveryNow, countsNow)
                ) {
                    Text(
                        text = "Counts do not reconcile.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                for (op in report.planned) {
                    Text(text = op.oldName + " -> " + op.newName)
                }
                for (skipped in skippedRows) {
                    Text(
                        text = "SKIP " + skipped.name + ": " +
                            skipped.reason,
                        color = Color(0xFFFF8800),
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch { renamerViewModel.execute(resolver) }
                },
                enabled = previewFresh &&
                    (countsNow?.toRename ?: 0) > 0 && !executing,
            ) {
                Text(
                    text = "Execute " +
                        (countsNow?.toRename ?: 0) + " renames",
                )
            }
            if (executing) {
                Text(text = "Executing…")
            }
            if (reportText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SelectionContainer {
                    Text(text = reportText)
                }
                Button(onClick = {
                    clipboard.setText(AnnotatedString(reportText))
                }) {
                    Text(text = "Copy report")
                }
            }
            val manifest = manifestFile
            if (manifest != null) {
                Text(text = "Manifest: " + manifest.absolutePath)
                Text(
                    text = "Clearing app data loses undo history.",
                )
                Button(onClick = {
                    exportLauncher.launch(manifest.name)
                }) {
                    Text(text = "Export manifest")
                }
            }
            Button(
                onClick = {
                    scope.launch { renamerViewModel.undo(resolver) }
                },
                enabled = undoRemaining.isNotEmpty() && !executing,
            ) {
                Text(text = "Undo")
            }
            val undo = undoResult
            if (undo != null) {
                Text(
                    text = "Undo restored=" + undo.restoredCount +
                        " skipped=" + undo.skippedCount +
                        " errors=" + undo.errorCount,
                )
            }
            val failure = error
            if (failure != null) {
                Text(
                    text = failure,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = { renamerViewModel.dismissError() }) {
                    Text(text = "Dismiss")
                }
            }
        }
    }
}
