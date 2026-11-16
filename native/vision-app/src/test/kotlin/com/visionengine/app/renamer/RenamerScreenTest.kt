// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.saf.DisplayNameSource
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RenamerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var treeUri: Uri

    private fun docUri(name: String): String {
        return documentUriForPath(treeUri, "primary:Pictures/$name")
    }

    private fun grantEntry(): GrantEntry {
        return GrantEntry(
            uri = treeUri.toString(),
            displayName = "Pictures",
            displayNameSource = DisplayNameSource.TREE_DOCUMENT_ID,
            grantedMs = 100L,
            lastSeenHealthyMs = 100L,
        )
    }

    private fun folderRows(): List<Map<String, Any?>> {
        return listOf(
            safDocRow(
                "primary:Pictures/IMG_a.jpg",
                "IMG_a.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/IMG_smoke_a.jpg",
                "IMG_smoke_a.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/smoke_a.jpg",
                "smoke_a.jpg",
                "image/jpeg",
            ),
        )
    }

    private fun backendFor(vararg names: String): FakeRenameBackend {
        val backend = FakeRenameBackend(
            newUriOf = { _, newName ->
                destinationDocumentUri(treeUri, "primary:Pictures", newName)
            },
        )
        for (name in names) {
            backend.liveUris.add(docUri(name))
            backend.names[docUri(name)] = name
        }
        return backend
    }

    private fun setScreen(
        model: RenamerViewModel,
        folders: List<FolderState> = emptyList(),
    ) {
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            RenamerScreen(
                folders = folders,
                renamerViewModel = model,
                resolver = context.contentResolver,
                scope = scope,
                onRepick = {},
            )
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
    }

    @Test
    fun emptyStateShowsWhenNoFolder() {
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backendFor() },
            discoveryFactory = { scriptedDiscovery(emptyList()) },
        )
        setScreen(model)
        composeRule
            .onNodeWithText(
                "No folders yet — pick a folder in the Folders tab.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun grantLostStateShowsAndBlocksPreview() = runBlocking<Unit> {
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backendFor() },
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        model.selectGrant(
            FolderState(grantEntry(), GrantState.LOST, null, emptyList()),
            context.contentResolver,
        )
        val folders = listOf(
            FolderState(grantEntry(), GrantState.LOST, null, emptyList()),
        )
        setScreen(model, folders)
        composeRule
            .onNodeWithText("Vision lost access to Pictures.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Re-pick").assertIsDisplayed()
        composeRule.onNodeWithText("Preview").assertIsNotEnabled()
    }

    @Test
    fun previewShowsPlannedAndSkippedRows() = runBlocking<Unit> {
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backendFor() },
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        val folders = listOf(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
        )
        setScreen(model, folders)
        composeRule.onNodeWithText("Preview").performClick()
        composeRule.waitUntil(5_000) {
            model.previewFresh.value
        }
        composeRule.onNodeWithText("IMG_a.jpg -> a.jpg").assertIsDisplayed()
        composeRule
            .onNodeWithText("SKIP IMG_smoke_a.jpg: target is a file staying put")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("SKIP smoke_a.jpg: no camera prefix")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Execute 1 renames").assertIsDisplayed()
    }

    @Test
    fun executeShowsPostRunReportAndUndo() = runBlocking<Unit> {
        val backend = backendFor(
            "IMG_a.jpg",
            "IMG_smoke_a.jpg",
            "smoke_a.jpg",
        )
        val model = RenamerViewModel(
            filesDir = context.filesDir,
            backendFactory = { backend },
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        model.selectGrant(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
            context.contentResolver,
        )
        val folders = listOf(
            FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList()),
        )
        setScreen(model, folders)
        composeRule.onNodeWithText("Preview").performClick()
        composeRule.waitUntil(5_000) {
            model.previewFresh.value
        }
        composeRule.onNodeWithText("Execute 1 renames").performScrollTo()
        composeRule.onNodeWithText("Execute 1 renames").performClick()
        composeRule.waitUntil(5_000) {
            model.executeResult.value != null
        }
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        composeRule.onNodeWithText("Copy report").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitUntil(5_000) {
            model.undoResult.value != null
        }
        composeRule.onNodeWithText("Copy report").assertIsDisplayed()
    }
}
