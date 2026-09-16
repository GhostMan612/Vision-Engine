// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppRootTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rootShowsAppShell() {
        composeRule.setContent {
            AppRoot(viewModel = MainViewModel())
        }
        composeRule.onNodeWithText("Vision Engine").assertIsDisplayed()
        composeRule.onNodeWithText("Native foundation ready").assertIsDisplayed()
    }
}
