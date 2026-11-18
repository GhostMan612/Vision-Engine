// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.viewer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.visionengine.android.pipeline.MediaPipeline
import com.visionengine.android.pipeline.ThumbFetch
import com.visionengine.android.pipeline.ViewerPipeline
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import com.visionengine.android.saf.SafDiscovery
import com.visionengine.core.MediaKind
import com.visionengine.core.MediaRecord
import com.visionengine.core.MediaSource
import com.visionengine.core.ThumbInfo
import com.visionengine.core.ThumbStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Collections

data class ViewerRecord(
    val record: MediaRecord,
    val thumbStatus: ThumbStatus,
    val thumbBitmap: Bitmap?,
    val thumbReason: String?,
)

data class ViewerCounts(
    val photos: Int,
    val videos: Int,
    val unsupported: Int,
    val skippedDirs: Int,
    val skippedRows: Int,
    val warnings: List<String>,
)

class ViewerViewModel(
    private val pipelineFactory: (ContentResolver) -> ViewerPipeline =
        { resolver -> MediaPipeline(resolver) },
    private val discoveryFactory: (ContentResolver) -> SafDiscovery =
        { resolver -> SafDiscovery.withResolver(resolver) },
) : ViewModel() {
    companion object {
        const val PAGE_SIZE = 50
        const val MAX_RETAINED_BITMAPS = 12
    }

    private val _selectedGrant = MutableStateFlow<GrantEntry?>(null)
    val selectedGrant: StateFlow<GrantEntry?> = _selectedGrant.asStateFlow()

    private val _grantLost = MutableStateFlow(false)
    val grantLost: StateFlow<Boolean> = _grantLost.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _records =
        MutableStateFlow<List<ViewerRecord>>(emptyList())
    val records: StateFlow<List<ViewerRecord>> = _records.asStateFlow()

    private val _counts = MutableStateFlow<ViewerCounts?>(null)
    val counts: StateFlow<ViewerCounts?> = _counts.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedRecord = MutableStateFlow<ViewerRecord?>(null)
    val selectedRecord: StateFlow<ViewerRecord?> =
        _selectedRecord.asStateFlow()

    private var loadJob: Job? = null
    @Volatile
    private var loadGeneration: Int = 0
    private var lastSources: List<MediaSource> = emptyList()
    private var loadedCount: Int = 0
    private var pipeline: ViewerPipeline? = null
    private val restoringThumbs: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    fun onFoldersChanged(folders: List<FolderState>) {
        val selected = _selectedGrant.value ?: return
        val matching = folders.firstOrNull {
            it.entry.uri == selected.uri
        }
        if (matching == null) {
            loadJob?.cancel()
            resetRunState()
            _selectedGrant.value = null
            _grantLost.value = false
            return
        }
        if (matching.state == GrantState.LOST) {
            loadJob?.cancel()
            _grantLost.value = true
            _loading.value = false
        } else {
            _grantLost.value = false
        }
    }

    suspend fun openFolder(folder: FolderState, resolver: ContentResolver) {
        val current = currentCoroutineContext()[Job]
        if (loadJob != null && loadJob !== current) {
            loadJob?.cancel()
        }
        loadJob = current
        val generation = ++loadGeneration
        resetRunState()
        _selectedGrant.value = folder.entry
        if (folder.state == GrantState.LOST) {
            _grantLost.value = true
            return
        }
        _grantLost.value = false
        _loading.value = true
        val treeUri = Uri.parse(folder.entry.uri)
        val outcome = try {
            withContext(Dispatchers.IO) {
                discoveryFactory(resolver).discover(treeUri)
            }
        } catch (e: SecurityException) {
            if (generation == loadGeneration) {
                _grantLost.value = true
                _loading.value = false
                _error.value =
                    "Vision lost access to this folder. " +
                        "Re-pick it to restore it."
            }
            return
        } catch (e: Exception) {
            if (generation == loadGeneration) {
                _loading.value = false
                _error.value = "Could not read this folder."
            }
            return
        }
        if (generation != loadGeneration) {
            return
        }
        if (outcome.sources.isEmpty() && outcome.warnings.isNotEmpty()) {
            _loading.value = false
            _error.value =
                "Discovery failed: " + outcome.warnings.joinToString("; ")
            return
        }
        lastSources = outcome.sources
        pipeline = pipelineFactory(resolver)
        _counts.value = ViewerCounts(
            photos = outcome.sources.count { it.kind == MediaKind.photo },
            videos = outcome.sources.count { it.kind == MediaKind.video },
            unsupported = outcome.sources.count {
                it.kind == MediaKind.unsupported
            },
            skippedDirs = outcome.skippedDirs,
            skippedRows = outcome.skippedRows,
            warnings = outcome.warnings,
        )
        loadNextPage(resolver, generation)
    }

    suspend fun loadMore(resolver: ContentResolver) {
        if (_loading.value) {
            return
        }
        if (_selectedGrant.value == null || _grantLost.value) {
            return
        }
        if (loadedCount >= lastSources.size) {
            return
        }
        val current = currentCoroutineContext()[Job]
        if (loadJob != null && loadJob !== current) {
            loadJob?.cancel()
        }
        loadJob = current
        _loading.value = true
        loadNextPage(resolver, loadGeneration)
    }

    private suspend fun loadNextPage(resolver: ContentResolver, generation: Int) {
        try {
            loadNextPageLocked(resolver, generation)
        } finally {
            if (generation == loadGeneration) {
                _loading.value = false
            }
        }
    }

    private suspend fun loadNextPageLocked(
        resolver: ContentResolver,
        generation: Int,
    ) {
        val activePipeline = pipeline ?: pipelineFactory(resolver).also {
            pipeline = it
        }
        var published = 0
        val page = try {
            withContext(Dispatchers.IO) {
                activePipeline.loadPage(
                    lastSources,
                    loadedCount,
                    PAGE_SIZE,
                ) { record ->
                    if (generation == loadGeneration) {
                        published++
                        _records.value = _records.value +
                            ViewerRecord(
                                record,
                                ThumbStatus.pending,
                                null,
                                null,
                            )
                    }
                }
            }
        } catch (e: Exception) {
            if (generation == loadGeneration) {
                _error.value = "Could not read this folder."
            }
            return
        }
        currentCoroutineContext()[Job]?.ensureActive()
        if (generation != loadGeneration) {
            return
        }
        loadedCount += published
        _hasMore.value = loadedCount < lastSources.size
        if (page.isEmpty()) {
            _hasMore.value = false
            return
        }
        for (index in page.indices) {
            val fetched = try {
                withContext(Dispatchers.IO) {
                    activePipeline.thumbnailFor(page[index])
                }
            } catch (e: Exception) {
                ThumbFetch(
                    ThumbInfo(
                        ThumbStatus.failed,
                        reason = "decode failed",
                    ),
                    null,
                )
            }
            currentCoroutineContext()[Job]?.ensureActive()
            if (generation != loadGeneration) {
                return
            }
            val bytes = fetched.bytes
            val bitmap = if (bytes != null) {
                withContext(Dispatchers.IO) {
                    decodeThumbBytes(bytes)
                }
            } else {
                null
            }
            currentCoroutineContext()[Job]?.ensureActive()
            if (generation != loadGeneration) {
                return
            }
            val status = if (bytes != null && bitmap == null) {
                ThumbStatus.failed
            } else {
                fetched.info.status
            }
            val reason = if (bytes != null && bitmap == null) {
                "decode failed"
            } else {
                fetched.info.reason
            }
            updateRecord(page[index].source.id, status, bitmap, reason)
        }
    }

    fun selectRecord(viewerRecord: ViewerRecord) {
        _selectedRecord.value = viewerRecord
    }

    fun clearSelection() {
        _selectedRecord.value = null
    }

    fun onViewerHidden() {
        loadJob?.cancel()
        loadGeneration++
        _loading.value = false
    }

    suspend fun restoreThumb(viewerRecord: ViewerRecord) {
        val id = viewerRecord.record.source.id
        if (!restoringThumbs.add(id)) {
            return
        }
        try {
            val activePipeline = pipeline ?: return
            val fetched = withContext(Dispatchers.IO) {
                activePipeline.thumbnailFor(viewerRecord.record)
            }
            currentCoroutineContext()[Job]?.ensureActive()
            val bytes = fetched.bytes
            val bitmap = if (bytes != null) {
                withContext(Dispatchers.IO) {
                    decodeThumbBytes(bytes)
                }
            } else {
                null
            }
            if (bitmap != null) {
                updateRecord(
                    id,
                    fetched.info.status,
                    bitmap,
                    fetched.info.reason,
                )
            }
        } finally {
            restoringThumbs.remove(id)
        }
    }

    fun dismissError() {
        _error.value = null
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }

    private fun updateRecord(
        sourceId: String,
        status: ThumbStatus,
        bitmap: Bitmap?,
        reason: String?,
    ) {
        _records.value = _records.value.map { viewerRecord ->
            if (viewerRecord.record.source.id == sourceId) {
                viewerRecord.copy(
                    thumbStatus = status,
                    thumbBitmap = bitmap,
                    thumbReason = reason,
                )
            } else {
                viewerRecord
            }
        }
        val selected = _selectedRecord.value
        if (selected != null && selected.record.source.id == sourceId) {
            _selectedRecord.value = selected.copy(
                thumbStatus = status,
                thumbBitmap = bitmap,
                thumbReason = reason,
            )
        }
        if (bitmap != null) {
            enforceBitmapCap(sourceId)
        }
    }

    private fun enforceBitmapCap(keepId: String) {
        val selectedId = _selectedRecord.value?.record?.source?.id
        var retained = _records.value.count { it.thumbBitmap != null }
        if (retained <= MAX_RETAINED_BITMAPS) {
            return
        }
        for (viewerRecord in _records.value) {
            if (retained <= MAX_RETAINED_BITMAPS) {
                return
            }
            val id = viewerRecord.record.source.id
            if (viewerRecord.thumbBitmap == null ||
                id == keepId ||
                id == selectedId
            ) {
                continue
            }
            _records.value = _records.value.map {
                if (it.record.source.id == id) {
                    it.copy(thumbBitmap = null)
                } else {
                    it
                }
            }
            retained--
        }
    }

    private fun decodeThumbBytes(bytes: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun resetRunState() {
        _loading.value = false
        _records.value = emptyList()
        _counts.value = null
        _hasMore.value = false
        _error.value = null
        _selectedRecord.value = null
        lastSources = emptyList()
        loadedCount = 0
        pipeline = null
    }
}

class ViewerViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViewerViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
