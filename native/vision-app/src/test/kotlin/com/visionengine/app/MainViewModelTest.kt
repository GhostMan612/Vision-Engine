// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.visionengine.android.saf.GrantStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {
    @Test
    fun initialState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MainViewModel(GrantStore(context))
        assertEquals("Vision Engine", viewModel.uiState.value.appName)
        assertTrue(viewModel.uiState.value.ready)
        assertNull(viewModel.pickFailure.value)
    }
}
