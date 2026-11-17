// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantState
import com.visionengine.android.saf.GrantStore
import com.visionengine.android.saf.buildTreePickerIntent
import com.visionengine.android.saf.deriveFolderStates
import com.visionengine.android.saf.picturesTreeUri
import com.visionengine.app.renamer.RenamerScreen
import com.visionengine.app.renamer.RenamerViewModel
import com.visionengine.app.renamer.RenamerViewModelFactory
import com.visionengine.app.viewer.ViewerScreen
import com.visionengine.app.viewer.ViewerViewModel
import com.visionengine.app.viewer.ViewerViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun VisionTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun FolderRow(
    folder: FolderState,
    onRepick: () -> Unit,
    onForget: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = folder.entry.displayName)
        when (folder.state) {
            GrantState.HEALTHY -> Text(text = "Healthy")
            GrantState.LOST -> Text(
                text = "Vision lost access to " +
                    folder.entry.displayName +
                    ". Re-pick the folder to restore it.",
            )
            GrantState.NEVER_VERIFIED -> Text(text = "Never verified")
        }
        if (folder.itemCount == null) {
            Text(text = "Not counted yet")
        } else if (folder.itemCount == 0) {
            Text(text = "Empty folder")
        } else {
            Text(text = "${folder.itemCount} items")
        }
        if (folder.warnings.isNotEmpty()) {
            Text(text = folder.warnings.joinToString(separator = "\n"))
        }
        Row {
            Button(onClick = onRepick) {
                Text(text = "Re-pick")
            }
            Button(onClick = onForget) {
                Text(text = "Forget")
            }
        }
    }
}

@Composable
fun AppRoot(
    mainViewModel: MainViewModel? = null,
    renamerViewModel: RenamerViewModel? = null,
    viewerViewModel: ViewerViewModel? = null,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val store = remember { GrantStore(context.applicationContext) }
    val viewModel: MainViewModel = mainViewModel ?: viewModel(
        factory = MainViewModelFactory(store),
    )
    val renamer: RenamerViewModel = renamerViewModel ?: viewModel(
        factory = RenamerViewModelFactory(
            context.applicationContext.filesDir,
        ),
    )
    val viewer: ViewerViewModel = viewerViewModel ?: viewModel(
        factory = ViewerViewModelFactory(),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )
    val persistedUris by viewModel.persistedUris.collectAsStateWithLifecycle()
    val lastDiscovery by viewModel.lastDiscovery.collectAsStateWithLifecycle()
    val pickFailure by viewModel.pickFailure.collectAsStateWithLifecycle()
    val folders = deriveFolderStates(entries, persistedUris, lastDiscovery)
    var repickTarget by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        val target = repickTarget
        repickTarget = null
        if (uri != null) {
            scope.launch {
                if (target == null) {
                    viewModel.handlePickedFolder(uri, resolver)
                } else {
                    viewModel.handleRepickedFolder(target, uri, resolver)
                }
            }
        }
    }
    fun launchPicker() {
        try {
            pickerLauncher.launch(buildTreePickerIntent(picturesTreeUri()))
        } catch (e: Exception) {
            pickerLauncher.launch(buildTreePickerIntent(null))
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshHealth(resolver)
    }
    VisionTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = state.appName)
                Text(text = if (state.ready) "Native foundation ready" else "Starting")
                Row {
                    Button(onClick = { tab = 0 }) {
                        Text(text = "Folders")
                    }
                    Button(onClick = { tab = 1 }) {
                        Text(text = "Renamer")
                    }
                    Button(onClick = { tab = 2 }) {
                        Text(text = "Viewer")
                    }
                }
                if (tab == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (pickFailure != null) {
                        Text(text = pickFailure ?: "")
                        Button(onClick = {
                            viewModel.dismissPickFailure()
                            repickTarget = null
                            launchPicker()
                        }) {
                            Text(text = "Try again")
                        }
                    }
                    if (folders.isEmpty()) {
                        Text(text = "No folders yet — pick a folder to begin")
                    } else {
                        for (folder in folders) {
                            FolderRow(
                                folder = folder,
                                onRepick = {
                                    repickTarget = folder.entry.uri
                                    launchPicker()
                                },
                                onForget = {
                                    scope.launch {
                                        viewModel.forgetFolder(
                                            folder.entry.uri,
                                            resolver,
                                        )
                                    }
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        repickTarget = null
                        launchPicker()
                    }) {
                        Text(text = "Pick a folder")
                    }
                } else if (tab == 1) {
                    RenamerScreen(
                        folders = folders,
                        renamerViewModel = renamer,
                        resolver = resolver,
                        scope = scope,
                        onRepick = { uri ->
                            repickTarget = uri
                            launchPicker()
                        },
                    )
                } else {
                    ViewerScreen(
                        folders = folders,
                        viewerViewModel = viewer,
                        resolver = resolver,
                        scope = scope,
                        onRepick = { uri ->
                            repickTarget = uri
                            launchPicker()
                        },
                    )
                }
            }
        }
    }
}
