// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.viewer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.saf.DisplayNameSource
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import com.visionengine.app.renamer.safDocRow
import com.visionengine.app.renamer.scriptedDiscovery
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBitmapFactory
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ViewerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var treeUri: Uri
    private lateinit var dir: File
    private lateinit var provider: FakeDocsProvider

    private class FakeDocsProvider : ContentProvider() {
        var files: MutableMap<String, File> = mutableMapOf()

        override fun onCreate(): Boolean = true

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            val documentId = DocumentsContract.getDocumentId(uri)
            val file = files[documentId]
                ?: throw FileNotFoundException("no such doc: $uri")
            return ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY,
            )
        }

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0
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
                "primary:Pictures/IMG_b.jpg",
                "IMG_b.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/VID_a.mp4",
                "VID_a.mp4",
                "video/mp4",
            ),
            safDocRow(
                "primary:Pictures/bad.jpg",
                "bad.jpg",
                "image/jpeg",
            ),
            safDocRow(
                "primary:Pictures/note.txt",
                "note.txt",
                "text/plain",
            ),
            safDocRow(
                "primary:Pictures/gone.jpg",
                "gone.jpg",
                "image/jpeg",
            ),
        )
    }

    private fun setScreen(
        model: ViewerViewModel,
        folders: List<FolderState> = emptyList(),
    ) {
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            ViewerScreen(
                folders = folders,
                viewerViewModel = model,
                resolver = context.contentResolver,
                scope = scope,
                onRepick = {},
            )
        }
    }

    private fun healthyFolder(): FolderState {
        return FolderState(grantEntry(), GrantState.HEALTHY, null, emptyList())
    }

    private fun openGrid(model: ViewerViewModel) {
        setScreen(model, listOf(healthyFolder()))
        composeRule.onNodeWithText("Select").performClick()
        composeRule.waitUntil(5_000) {
            model.records.value.size == 5 && !model.loading.value
        }
    }

    @Before
    fun setUp() {
        ShadowBitmapFactory.setAllowInvalidImageData(false)
        context = ApplicationProvider.getApplicationContext()
        treeUri = Uri.parse(
            "content://com.example.docs/tree/primary%3APictures",
        )
        dir = temporaryFolder.newFolder("VE_VIEW")
        stageAsset("ve_thumb.jpg", "IMG_b.jpg")
        stageAsset("ve_minimal.mp4", "VID_a.mp4")
        File(dir, "bad.jpg").writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        provider = FakeDocsProvider()
        provider.files["primary:Pictures/IMG_b.jpg"] = File(dir, "IMG_b.jpg")
        provider.files["primary:Pictures/VID_a.mp4"] = File(dir, "VID_a.mp4")
        provider.files["primary:Pictures/bad.jpg"] = File(dir, "bad.jpg")
        val info = ProviderInfo().apply {
            authority = "com.example.docs"
            grantUriPermissions = true
        }
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal(
            "com.example.docs",
            provider,
        )
    }

    @After
    fun tearDown() {
        ShadowBitmapFactory.setAllowInvalidImageData(true)
    }

    @Test
    fun emptyStateShowsWhenNoFolder() {
        val model = ViewerViewModel(
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
    fun grantLostStateShowsAndBlocksLoad() = runBlocking<Unit> {
        val model = ViewerViewModel(
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        model.openFolder(
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
        composeRule.onNodeWithText("Selected").assertIsDisplayed()
    }

    @Test
    fun gridShowsRecordsAndOpensDetail() = runBlocking<Unit> {
        val model = ViewerViewModel(
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        openGrid(model)
        composeRule.onNodeWithText("IMG_b.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("VID_a.mp4").assertIsDisplayed()
        composeRule.onNodeWithText("bad.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("note.txt").assertIsDisplayed()
        composeRule.onNodeWithText("gone.jpg").assertIsDisplayed()
        composeRule
            .onAllNodesWithText("Photo")
            .assertCountEquals(3)
        composeRule.onNodeWithText("Video").assertIsDisplayed()
        composeRule.onNodeWithText("Unsupported").assertIsDisplayed()
        composeRule.onNodeWithText("Thumb failed").assertIsDisplayed()
        composeRule
            .onNodeWithText("Could not read this file")
            .assertIsDisplayed()
        composeRule.onNodeWithText("IMG_b.jpg").performClick()
        composeRule.onNodeWithText("Width").assertIsDisplayed()
        composeRule.onNodeWithText("16").assertIsDisplayed()
        composeRule.onNodeWithText("Height").assertIsDisplayed()
        composeRule.onNodeWithText("24").assertIsDisplayed()
        composeRule.onNodeWithText("Orientation").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule
            .onNodeWithText("Raw 24×16 · orientation tag 6")
            .assertIsDisplayed()
        composeRule.onNodeWithText("File size").assertIsDisplayed()
        composeRule.onNodeWithText("194 B").assertIsDisplayed()
        composeRule
            .onAllNodesWithText("—")
            .assertCountEquals(6)
        composeRule
            .onAllNodesWithText("exif")
            .assertCountEquals(3)
        composeRule
            .onAllNodesWithText("filesystem")
            .assertCountEquals(2)
        composeRule
            .onNodeWithText("Strip GPS — Phase 3")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Strip GPS — Phase 3")
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("IMG_b.jpg").assertIsDisplayed()
    }

    @Test
    fun videoDetailOpens() = runBlocking<Unit> {
        val model = ViewerViewModel(
            discoveryFactory = { scriptedDiscovery(folderRows()) },
        )
        openGrid(model)
        composeRule.onNodeWithText("VID_a.mp4").performClick()
        composeRule.onNodeWithText("Duration").assertIsDisplayed()
        composeRule
            .onNodeWithText("rotation tag 0", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun emptyFolderShowsNoPhotosText() = runBlocking<Unit> {
        val model = ViewerViewModel(
            discoveryFactory = { scriptedDiscovery(emptyList()) },
        )
        setScreen(model, listOf(healthyFolder()))
        composeRule.onNodeWithText("Select").performClick()
        composeRule.waitUntil(5_000) {
            !model.loading.value && model.counts.value != null
        }
        composeRule
            .onNodeWithText("No photos or videos here")
            .assertIsDisplayed()
    }

    @Test
    fun allUnsupportedShowsCellsAndCounts() = runBlocking<Unit> {
        val model = ViewerViewModel(
            discoveryFactory = {
                scriptedDiscovery(
                    listOf(
                        safDocRow(
                            "primary:Pictures/note.txt",
                            "note.txt",
                            "text/plain",
                        ),
                    ),
                )
            },
        )
        setScreen(model, listOf(healthyFolder()))
        composeRule.onNodeWithText("Select").performClick()
        composeRule.waitUntil(5_000) {
            !model.loading.value && model.counts.value != null
        }
        composeRule.onNodeWithText("note.txt").assertIsDisplayed()
        composeRule.onNodeWithText("Unsupported").assertIsDisplayed()
        composeRule
            .onNodeWithText("Photos: 0  Videos: 0  Unsupported: 1")
            .assertIsDisplayed()
    }

    private fun stageAsset(asset: String, name: String) {
        val stream = javaClass.classLoader!!.getResourceAsStream(
            "fixtures/$asset",
        ) ?: throw IllegalStateException("missing fixture: $asset")
        File(dir, name).outputStream().use { output ->
            stream.copyTo(output)
        }
    }
}
