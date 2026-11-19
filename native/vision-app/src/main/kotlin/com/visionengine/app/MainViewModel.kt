// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.visionengine.android.saf.GrantDiscovery
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantStore
import com.visionengine.android.saf.SafDiscovery
import com.visionengine.android.saf.grantFlags
import com.visionengine.android.saf.persistedGrantUris
import com.visionengine.android.saf.resolveFolderDisplayName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

data class AppUiState(
    val appName: String = "Vision Engine",
    val ready: Boolean = true,
)

class MainViewModel(private val store: GrantStore) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val entries: Flow<List<GrantEntry>> = store.entries

    private val _persistedUris = MutableStateFlow<Set<String>>(emptySet())
    val persistedUris: StateFlow<Set<String>> = _persistedUris.asStateFlow()

    private val _lastDiscovery =
        MutableStateFlow<Map<String, GrantDiscovery>>(emptyMap())
    val lastDiscovery: StateFlow<Map<String, GrantDiscovery>> =
        _lastDiscovery.asStateFlow()

    private val _pickFailure = MutableStateFlow<String?>(null)
    val pickFailure: StateFlow<String?> = _pickFailure.asStateFlow()

    fun refreshHealth(resolver: ContentResolver) {
        _persistedUris.value = persistedGrantUris(resolver)
    }

    suspend fun handlePickedFolder(treeUri: Uri, resolver: ContentResolver) {
        val nowMs = System.currentTimeMillis()
        try {
            resolver.takePersistableUriPermission(treeUri, grantFlags())
        } catch (e: SecurityException) {
            _pickFailure.value =
                "Vision could not keep access to this folder."
            return
        }
        val (displayName, source) = resolveFolderDisplayName(resolver, treeUri)
        val outcome = SafDiscovery.withResolver(resolver).discover(treeUri)
        val healthy = outcome.warnings.isEmpty()
        val entry = GrantEntry(
            uri = treeUri.toString(),
            displayName = displayName,
            displayNameSource = source,
            grantedMs = nowMs,
            lastSeenHealthyMs = if (healthy) nowMs else null,
        )
        try {
            store.save(entry)
        } catch (e: IOException) {
            _pickFailure.value = "Vision could not save this folder."
            return
        }
        _lastDiscovery.value = _lastDiscovery.value + (
            entry.uri to GrantDiscovery(outcome.sources.size, outcome.warnings)
            )
        _pickFailure.value = null
        refreshHealth(resolver)
    }

    suspend fun handleRepickedFolder(
        oldUri: String,
        newUri: Uri,
        resolver: ContentResolver,
    ) {
        val nowMs = System.currentTimeMillis()
        try {
            resolver.takePersistableUriPermission(newUri, grantFlags())
        } catch (e: SecurityException) {
            _pickFailure.value =
                "Vision could not keep access to this folder."
            return
        }
        val (displayName, source) = resolveFolderDisplayName(resolver, newUri)
        val outcome = SafDiscovery.withResolver(resolver).discover(newUri)
        val healthy = outcome.warnings.isEmpty()
        val entry = GrantEntry(
            uri = newUri.toString(),
            displayName = displayName,
            displayNameSource = source,
            grantedMs = nowMs,
            lastSeenHealthyMs = if (healthy) nowMs else null,
        )
        try {
            store.replaceUri(oldUri, entry)
        } catch (e: IOException) {
            _pickFailure.value = "Vision could not save this folder."
            return
        }
        if (oldUri != entry.uri) {
            try {
                resolver.releasePersistableUriPermission(
                    Uri.parse(oldUri),
                    grantFlags(),
                )
            } catch (e: Exception) {
            }
            _lastDiscovery.value = _lastDiscovery.value - oldUri
        }
        _lastDiscovery.value = _lastDiscovery.value + (
            entry.uri to GrantDiscovery(outcome.sources.size, outcome.warnings)
            )
        _pickFailure.value = null
        refreshHealth(resolver)
    }

    suspend fun forgetFolder(uri: String, resolver: ContentResolver) {
        try {
            store.forget(uri)
        } catch (e: IOException) {
            _pickFailure.value = "Vision could not forget this folder."
            return
        }
        try {
            resolver.releasePersistableUriPermission(
                Uri.parse(uri),
                grantFlags(),
            )
        } catch (e: Exception) {
        }
        _lastDiscovery.value = _lastDiscovery.value - uri
        refreshHealth(resolver)
    }

    fun dismissPickFailure() {
        _pickFailure.value = null
    }
}

class MainViewModelFactory(
    private val store: GrantStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(store) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
