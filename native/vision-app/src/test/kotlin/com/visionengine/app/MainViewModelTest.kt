// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun initialState() {
        val viewModel = MainViewModel()
        assertEquals("Vision Engine", viewModel.uiState.value.appName)
        assertTrue(viewModel.uiState.value.ready)
    }
}
