// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.viewer

import android.content.ContentResolver
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantState
import com.visionengine.core.ExtractStatus
import com.visionengine.core.MediaKind
import com.visionengine.core.ThumbStatus
import com.visionengine.core.photoDetailFields
import com.visionengine.core.videoDetailFields
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    folders: List<FolderState>,
    viewerViewModel: ViewerViewModel,
    resolver: ContentResolver,
    scope: CoroutineScope,
    onRepick: (String) -> Unit,
) {
    val selectedGrant by viewerViewModel.selectedGrant
        .collectAsStateWithLifecycle()
    val grantLost by viewerViewModel.grantLost
        .collectAsStateWithLifecycle()
    val loading by viewerViewModel.loading.collectAsStateWithLifecycle()
    val records by viewerViewModel.records.collectAsStateWithLifecycle()
    val counts by viewerViewModel.counts.collectAsStateWithLifecycle()
    val hasMore by viewerViewModel.hasMore.collectAsStateWithLifecycle()
    val error by viewerViewModel.error.collectAsStateWithLifecycle()
    val selected by viewerViewModel.selectedRecord
        .collectAsStateWithLifecycle()
    LaunchedEffect(folders) {
        viewerViewModel.onFoldersChanged(folders)
    }
    val selectedRecord = selected
    if (selectedRecord != null) {
        ViewerDetail(
            viewerRecord = selectedRecord,
            onBack = { viewerViewModel.clearSelection() },
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize()
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
                            viewerViewModel.openFolder(folder, resolver)
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
            val currentCounts = counts
            if (currentCounts != null) {
                Text(
                    text = "Photos: " + currentCounts.photos +
                        "  Videos: " + currentCounts.videos +
                        "  Unsupported: " + currentCounts.unsupported,
                )
            }
            if (loading && records.isEmpty()) {
                Text(text = "Loading...")
            }
            if (!loading && records.isEmpty() && error == null) {
                Text(text = "No photos or videos here")
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
            ) {
                items(
                    records,
                    key = { it.record.source.id },
                ) { viewerRecord ->
                    ViewerCell(
                        viewerRecord = viewerRecord,
                        onClick = {
                            viewerViewModel.selectRecord(viewerRecord)
                        },
                    )
                }
            }
            if (hasMore) {
                Button(onClick = {
                    scope.launch {
                        viewerViewModel.loadMore(resolver)
                    }
                }) {
                    Text(text = "Load more")
                }
            }
            val errorText = error
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = { viewerViewModel.dismissError() }) {
                    Text(text = "Dismiss")
                }
            }
        }
    }
}

@Composable
fun ViewerCell(
    viewerRecord: ViewerRecord,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        val bitmap = viewerRecord.thumbBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription =
                    viewerRecord.record.source.displayName,
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (viewerRecord.thumbStatus) {
                        ThumbStatus.pending -> "Loading..."
                        ThumbStatus.failed -> "Thumb failed"
                        else -> "Thumb unavailable"
                    },
                )
            }
        }
        Text(text = viewerRecord.record.source.displayName)
        Text(
            text = when (viewerRecord.record.kind) {
                MediaKind.photo -> "Photo"
                MediaKind.video -> "Video"
                MediaKind.unsupported -> "Unsupported"
            },
        )
        if (viewerRecord.record.status == ExtractStatus.unreadable) {
            Text(text = "Could not read this file")
        }
    }
}

@Composable
fun ViewerDetail(
    viewerRecord: ViewerRecord,
    onBack: () -> Unit,
) {
    val record = viewerRecord.record
    val photo = record.photo
    val video = record.video
    val fields = when {
        photo != null -> photoDetailFields(photo)
        video != null -> videoDetailFields(video)
        else -> emptyList()
    }
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Button(onClick = onBack) {
            Text(text = "Back")
        }
        val bitmap = viewerRecord.thumbBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = record.source.displayName,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Text(text = "Thumb unavailable")
        }
        Text(text = record.source.displayName)
        for (field in fields) {
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = field.label)
                    Text(text = field.value)
                }
                Text(text = field.provenance.name)
            }
        }
        if (record.photo != null) {
            Button(onClick = {}, enabled = false) {
                Text(text = "Strip GPS — Phase 3")
            }
        }
    }
}
