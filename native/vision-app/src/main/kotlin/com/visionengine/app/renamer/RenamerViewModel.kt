// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.app.renamer

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.visionengine.android.saf.DiscoveryOutcome
import com.visionengine.android.saf.FolderState
import com.visionengine.android.saf.GrantEntry
import com.visionengine.android.saf.GrantState
import com.visionengine.android.saf.SafDiscovery
import com.visionengine.core.MediaKind
import com.visionengine.core.rename.MANIFEST_STATUS_RENAMED
import com.visionengine.core.rename.ManifestRow
import com.visionengine.core.rename.RenameEntry
import com.visionengine.core.rename.RenameReport
import com.visionengine.core.rename.buildPlan
import com.visionengine.core.rename.executionOrder
import com.visionengine.core.rename.isRestorable
import com.visionengine.core.rename.manifestToCsv
import com.visionengine.core.rename.parseManifestCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class RenamerViewModel(
    private val filesDir: File,
    private val backendFactory: (ContentResolver) -> RenameBackend =
        { resolver -> SafRenameBackend(resolver) },
    private val discoveryFactory: (ContentResolver) -> SafDiscovery =
        { resolver -> SafDiscovery.withResolver(resolver) },
) : ViewModel() {
    private val _selectedGrant = MutableStateFlow<GrantEntry?>(null)
    val selectedGrant: StateFlow<GrantEntry?> = _selectedGrant.asStateFlow()

    private val _grantLost = MutableStateFlow(false)
    val grantLost: StateFlow<Boolean> = _grantLost.asStateFlow()

    private val _discovery = MutableStateFlow<DiscoveryCounts?>(null)
    val discovery: StateFlow<DiscoveryCounts?> = _discovery.asStateFlow()

    private val _plan = MutableStateFlow<RenameReport?>(null)
    val plan: StateFlow<RenameReport?> = _plan.asStateFlow()

    private val _planCounts = MutableStateFlow<PlanCounts?>(null)
    val planCounts: StateFlow<PlanCounts?> = _planCounts.asStateFlow()

    private val _skippedRows =
        MutableStateFlow<List<SkippedRow>>(emptyList())
    val skippedRows: StateFlow<List<SkippedRow>> =
        _skippedRows.asStateFlow()

    private val _previewFresh = MutableStateFlow(false)
    val previewFresh: StateFlow<Boolean> = _previewFresh.asStateFlow()

    private val _executing = MutableStateFlow(false)
    val executing: StateFlow<Boolean> = _executing.asStateFlow()

    private val _executeResult = MutableStateFlow<ExecuteResult?>(null)
    val executeResult: StateFlow<ExecuteResult?> =
        _executeResult.asStateFlow()

    private val _manifestFile = MutableStateFlow<File?>(null)
    val manifestFile: StateFlow<File?> = _manifestFile.asStateFlow()

    private val _undoResult = MutableStateFlow<UndoResult?>(null)
    val undoResult: StateFlow<UndoResult?> = _undoResult.asStateFlow()

    private val _undoRemaining =
        MutableStateFlow<List<ManifestRow>>(emptyList())
    val undoRemaining: StateFlow<List<ManifestRow>> =
        _undoRemaining.asStateFlow()

    private val _reportText = MutableStateFlow("")
    val reportText: StateFlow<String> = _reportText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var lastOutcome: DiscoveryOutcome? = null
    private var lastEntries: List<RenameEntry> = emptyList()
    private var lastExistingPaths: Set<String> = emptySet()
    private var lastContentUris: Map<String, String> = emptyMap()
    private var lastDiscoveryCounts: DiscoveryCounts? = null
    private var treeDocumentId: String? = null
    private var restoredNames: MutableSet<String> = mutableSetOf()

    fun onFoldersChanged(folders: List<FolderState>) {
        val selected = _selectedGrant.value ?: return
        val matching = folders.firstOrNull {
            it.entry.uri == selected.uri
        }
        if (matching == null) {
            resetRunState()
            _selectedGrant.value = null
            _grantLost.value = false
            _discovery.value = null
            lastOutcome = null
            lastDiscoveryCounts = null
            treeDocumentId = null
            return
        }
        if (matching.state == GrantState.LOST) {
            _grantLost.value = true
            _previewFresh.value = false
        } else {
            _grantLost.value = false
        }
    }

    suspend fun selectGrant(folder: FolderState, resolver: ContentResolver) {
        resetRunState()
        _selectedGrant.value = folder.entry
        if (folder.state == GrantState.LOST) {
            _grantLost.value = true
            _discovery.value = null
            lastOutcome = null
            lastDiscoveryCounts = null
            treeDocumentId = null
            return
        }
        _grantLost.value = false
        val treeUri = Uri.parse(folder.entry.uri)
        val docId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: IllegalArgumentException) {
            _discovery.value = null
            lastOutcome = null
            lastDiscoveryCounts = null
            treeDocumentId = null
            _error.value = "Invalid folder URI."
            return
        }
        treeDocumentId = docId
        val outcome = discoveryFactory(resolver).discover(treeUri)
        lastOutcome = outcome
        val photos = outcome.sources.count { it.kind == MediaKind.photo }
        val videos = outcome.sources.count { it.kind == MediaKind.video }
        val unsupported = outcome.sources.count {
            it.kind == MediaKind.unsupported
        }
        val counts = discoveryCounts(
            photos = photos,
            videos = videos,
            unsupported = unsupported,
            skippedDirs = outcome.skippedDirs,
            skippedRows = outcome.skippedRows,
            warnings = outcome.warnings,
        )
        lastDiscoveryCounts = counts
        _discovery.value = counts
        val entries = mutableListOf<RenameEntry>()
        val existing = mutableSetOf<String>()
        val contentUris = mutableMapOf<String, String>()
        for (source in outcome.sources) {
            val path = "$docId/${source.displayName}"
            existing.add(path)
            if (source.kind == MediaKind.photo ||
                source.kind == MediaKind.video
            ) {
                entries.add(
                    RenameEntry(
                        path = path,
                        name = source.displayName,
                        isDir = false,
                        isFile = true,
                    ),
                )
                contentUris[path] =
                    source.documentUri ?: source.id
            }
        }
        lastEntries = entries
        lastExistingPaths = existing
        lastContentUris = contentUris
        _error.value = null
    }

    suspend fun preview() {
        val outcome = lastOutcome
        if (_selectedGrant.value == null) {
            _error.value = "Select a folder first."
            return
        }
        if (_grantLost.value) {
            _error.value = "Vision lost access to this folder."
            return
        }
        if (outcome == null) {
            _error.value = "Discover a folder first."
            return
        }
        if (outcome.sources.isEmpty() && outcome.warnings.isNotEmpty()) {
            _error.value =
                "Discovery failed: " + outcome.warnings.joinToString("; ")
            return
        }
        val report = buildPlan(lastEntries, lastExistingPaths)
        _plan.value = report
        _planCounts.value = planCounts(report)
        _skippedRows.value = skippedRowsFor(
            report,
            lastEntries,
            outcome.sources
                .filter { it.kind == MediaKind.unsupported }
                .map { it.displayName },
        )
        _previewFresh.value = true
        _error.value = null
    }

    suspend fun execute(resolver: ContentResolver) {
        val report = _plan.value
        val grant = _selectedGrant.value
        val docId = treeDocumentId
        if (report == null || !_previewFresh.value || grant == null ||
            docId == null
        ) {
            _error.value = "Run Preview first."
            return
        }
        if (report.planned.isEmpty()) {
            _error.value = "Nothing to execute."
            return
        }
        if (_executing.value) {
            return
        }
        _executing.value = true
        _error.value = null
        try {
            executeLocked(resolver, report, grant, docId)
        } finally {
            _executing.value = false
        }
    }

    private suspend fun executeLocked(
        resolver: ContentResolver,
        report: RenameReport,
        grant: GrantEntry,
        docId: String,
    ) {
        val backend = backendFactory(resolver)
        val (ordered, cyclic) = executionOrder(report.planned)
        val result = withContext(Dispatchers.IO) {
            executePlan(
                ordered = ordered,
                cyclic = cyclic,
                sourceUriOf = { op -> lastContentUris[op.srcPath] ?: "" },
                treeUri = grant.uri,
                treeDocumentId = docId,
                backend = backend,
            )
        }
        _executeResult.value = result
        _previewFresh.value = false
        val epoch = System.currentTimeMillis()
        val file = File(filesDir, "renames_$epoch.csv")
        val manifestContentUris = lastContentUris.toMutableMap()
        for ((op, uri) in result.renamedUris) {
            manifestContentUris[op.srcPath] = uri
        }
        val csv = manifestToCsv(
            report.planned,
            manifestContentUris,
            executed = true,
            renamedOps = result.renamedOps,
        )
        var manifestName: String? = null
        try {
            withContext(Dispatchers.IO) {
                file.writeText(csv)
            }
            _manifestFile.value = file
            manifestName = file.name
            _undoRemaining.value =
                parseManifestCsv(csv).filter { isRestorable(it) }
        } catch (e: IOException) {
            _manifestFile.value = null
            _undoRemaining.value = emptyList()
            _error.value =
                "Renames ran, but the manifest could not be written " +
                    "— undo is unavailable."
        }
        _reportText.value = buildExecuteReportText(
            folderName = grant.displayName,
            discovery = lastDiscoveryCounts ?: discoveryCounts(
                photos = 0,
                videos = 0,
                unsupported = 0,
                skippedDirs = 0,
                skippedRows = 0,
                warnings = emptyList(),
            ),
            plan = _planCounts.value ?: planCounts(report),
            result = result,
            manifestName = manifestName,
            undoAvailable = _undoRemaining.value.isNotEmpty(),
        )
    }

    suspend fun undo(resolver: ContentResolver) {
        val file = _manifestFile.value
        val grant = _selectedGrant.value
        val docId = treeDocumentId
        if (file == null || grant == null || docId == null) {
            _error.value = "No manifest — nothing to undo."
            return
        }
        if (_executing.value) {
            return
        }
        val csv = try {
            withContext(Dispatchers.IO) {
                file.readText()
            }
        } catch (e: IOException) {
            _error.value = "Could not read the manifest."
            return
        }
        val rows = parseManifestCsv(csv)
        val remaining = rows.filter {
            it.status == MANIFEST_STATUS_RENAMED &&
                it.newName !in restoredNames
        }
        if (remaining.isEmpty()) {
            _error.value = "Nothing left to undo."
            return
        }
        _executing.value = true
        _error.value = null
        try {
            undoLocked(resolver, file, grant, docId, remaining)
        } finally {
            _executing.value = false
        }
    }

    private suspend fun undoLocked(
        resolver: ContentResolver,
        file: File,
        grant: GrantEntry,
        docId: String,
        rows: List<ManifestRow>,
    ) {
        val treeUri = Uri.parse(grant.uri)
        val backend = backendFactory(resolver)
        val result = withContext(Dispatchers.IO) {
            undoRows(
                rows = rows,
                documentUrisOf = { row ->
                    val candidates = mutableListOf<String>()
                    if (row.contentUri.isNotEmpty()) {
                        candidates.add(row.contentUri)
                    }
                    candidates.add(
                        documentUriForPath(treeUri, row.newPath),
                    )
                    candidates
                },
                treeUri = grant.uri,
                treeDocumentId = docId,
                backend = backend,
            )
        }
        _undoResult.value = result
        for (entry in result.results) {
            if (entry.outcome == UndoOutcome.RESTORED) {
                restoredNames.add(entry.row.newName)
            }
        }
        _undoRemaining.value = rows.filter {
            it.status == MANIFEST_STATUS_RENAMED &&
                it.newName !in restoredNames
        }
        _reportText.value = buildUndoReportText(
            folderName = grant.displayName,
            result = result,
            manifestName = file.name,
        )
    }

    suspend fun exportManifest(uri: Uri, resolver: ContentResolver) {
        val file = _manifestFile.value ?: return
        try {
            val text = withContext(Dispatchers.IO) {
                file.readText()
            }
            withContext(Dispatchers.IO) {
                val out = resolver.openOutputStream(uri)
                if (out == null) {
                    _error.value =
                        "Could not open the export destination."
                } else {
                    out.use { stream ->
                        stream.write(text.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        } catch (e: Exception) {
            _error.value =
                "Export failed: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun resetRunState() {
        _plan.value = null
        _planCounts.value = null
        _skippedRows.value = emptyList()
        _previewFresh.value = false
        _executing.value = false
        _executeResult.value = null
        _manifestFile.value = null
        _undoResult.value = null
        _undoRemaining.value = emptyList()
        _reportText.value = ""
        _error.value = null
        lastEntries = emptyList()
        lastExistingPaths = emptySet()
        lastContentUris = emptyMap()
        restoredNames = mutableSetOf()
    }
}

class RenamerViewModelFactory(
    private val filesDir: File,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RenamerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RenamerViewModel(filesDir) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
