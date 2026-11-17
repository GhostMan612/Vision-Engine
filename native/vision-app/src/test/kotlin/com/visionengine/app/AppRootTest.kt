// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.saf.GrantStore
import com.visionengine.app.renamer.RenamerViewModel
import com.visionengine.app.viewer.ViewerViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppRootTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun rootShowsFoldersTabAndRenamerTab() {
        val main = MainViewModel(GrantStore(context))
        val renamer = RenamerViewModel(context.filesDir)
        composeRule.setContent {
            AppRoot(mainViewModel = main, renamerViewModel = renamer)
        }
        composeRule.onNodeWithText("Vision Engine").assertIsDisplayed()
        composeRule
            .onNodeWithText("Native foundation ready")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Folders").assertIsDisplayed()
        composeRule
            .onNodeWithText("No folders yet — pick a folder to begin")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Pick a folder").assertIsDisplayed()
        composeRule.onNodeWithText("Renamer").performClick()
        composeRule
            .onNodeWithText(
                "No folders yet — pick a folder in the Folders tab.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun rootShowsViewerTab() {
        val main = MainViewModel(GrantStore(context))
        val renamer = RenamerViewModel(context.filesDir)
        val viewer = ViewerViewModel()
        composeRule.setContent {
            AppRoot(
                mainViewModel = main,
                renamerViewModel = renamer,
                viewerViewModel = viewer,
            )
        }
        composeRule.onNodeWithText("Viewer").performClick()
        composeRule
            .onNodeWithText(
                "No folders yet — pick a folder in the Folders tab.",
            )
            .assertIsDisplayed()
    }
}
