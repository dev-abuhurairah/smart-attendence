package com.smartattendance.app.ui.screens.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.repository.ExportRepository
import com.smartattendance.app.data.repository.ExportWriteResult
import com.smartattendance.app.data.repository.GeneratedExportBundle
import com.smartattendance.app.data.repository.ReportQuery
import com.smartattendance.app.data.repository.ReportSummary
import com.smartattendance.app.data.repository.ReportType
import com.smartattendance.app.data.repository.ReportsRepository
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.export.ExportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportHistoryItemUi(
    val entity: ExportHistoryEntity,
    val isAccessible: Boolean
)

data class ReportsUiState(
    val query: ReportQuery = ReportQuery(),
    val summary: ReportSummary? = null,
    val exportHistory: List<ExportHistoryItemUi> = emptyList(),
    val isExporting: Boolean = false,
    val pendingSafBundle: GeneratedExportBundle? = null,
    val pendingShareBundle: GeneratedExportBundle? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepo: ReportsRepository,
    private val exportRepo: ExportRepository
) : ViewModel() {

    private val _query = MutableStateFlow(ReportQuery())
    private val _actionState = MutableStateFlow(ReportsActionState())

    private data class ReportsActionState(
        val isExporting: Boolean = false,
        val pendingSafBundle: GeneratedExportBundle? = null,
        val pendingShareBundle: GeneratedExportBundle? = null,
        val statusMessage: String? = null,
        val errorMessage: String? = null
    )

    val uiState: StateFlow<ReportsUiState> = combine(
        _query,
        reportsRepo.observeReport(_query),
        reportsRepo.observeExportHistory(),
        _actionState
    ) { query, summary, history, action ->
        val historyUi = withContext(Dispatchers.IO) {
            history.map { entity ->
                ExportHistoryItemUi(
                    entity = entity,
                    isAccessible = reportsRepo.isExportUriAccessible(entity.uriString, entity.isTreeUri)
                )
            }
        }
        ReportsUiState(
            query = query,
            summary = summary,
            exportHistory = historyUi,
            isExporting = action.isExporting,
            pendingSafBundle = action.pendingSafBundle,
            pendingShareBundle = action.pendingShareBundle,
            statusMessage = action.statusMessage,
            errorMessage = action.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun updateQuery(transform: (ReportQuery) -> ReportQuery) {
        _query.update(transform)
    }

    fun exportCurrentReport(format: ExportFormat) {
        val summary = uiState.value.summary ?: return
        viewModelScope.launch {
            _actionState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            reportsRepo.generateReportBundle(summary, format).fold(
                onSuccess = { bundle ->
                    _actionState.update { it.copy(isExporting = false, pendingSafBundle = bundle) }
                },
                onFailure = { err ->
                    _actionState.update {
                        it.copy(isExporting = false, errorMessage = err.message ?: "Report export failed.")
                    }
                }
            )
        }
    }

    fun recreateHistoryEntry(entry: ExportHistoryEntity) {
        val format = runCatching { ExportFormat.valueOf(entry.format) }.getOrDefault(ExportFormat.PDF)
        val mode = runCatching { ExportMode.valueOf(entry.mode) }.getOrDefault(ExportMode.FULL_REGISTER)
        if (entry.subjectId == null || entry.subjectId == 0L) {
            _actionState.update { it.copy(errorMessage = "Use the Analytics tab to re-export custom reports.") }
            return
        }
        viewModelScope.launch {
            _actionState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            exportRepo.generateToTemp(entry.sessionId, entry.subjectId, mode, format).fold(
                onSuccess = { bundle ->
                    _actionState.update { it.copy(isExporting = false, pendingSafBundle = bundle) }
                },
                onFailure = { err ->
                    _actionState.update {
                        it.copy(isExporting = false, errorMessage = err.message ?: "Recreate failed.")
                    }
                }
            )
        }
    }

    fun onSafTargetSelected(uri: Uri) {
        val bundle = _actionState.value.pendingSafBundle ?: return
        viewModelScope.launch {
            _actionState.update { it.copy(isExporting = true, pendingSafBundle = null) }
            val result = if (bundle.format == ExportFormat.PNG) {
                exportRepo.writePngBundleToTreeUri(bundle, uri)
            } else {
                exportRepo.writeBundleToFileUri(bundle, uri)
            }
            when (result) {
                is ExportWriteResult.Success -> _actionState.update {
                    it.copy(isExporting = false, statusMessage = "Saved: ${result.displayName}")
                }
                is ExportWriteResult.NeedsNewLocation -> _actionState.update {
                    it.copy(isExporting = false, errorMessage = result.reason, pendingSafBundle = result.bundle)
                }
                is ExportWriteResult.Failure -> _actionState.update {
                    it.copy(isExporting = false, errorMessage = result.error)
                }
            }
        }
    }

    fun clearPendingSaf() {
        _actionState.update { it.copy(pendingSafBundle = null) }
    }

    fun removeHistoryEntry(id: Long) {
        viewModelScope.launch {
            reportsRepo.removeExportHistoryEntry(id)
            _actionState.update { it.copy(statusMessage = "Removed from history (attendance data kept).") }
        }
    }

    fun openExportedFile(context: Context, entry: ExportHistoryEntity) {
        val uri = Uri.parse(entry.uriString)
        val mime = runCatching { ExportFormat.valueOf(entry.format).mimeType }.getOrDefault("*/*")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (entry.isTreeUri) "vnd.android.document/directory" else mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open File"))
        }.onFailure {
            _actionState.update { it.copy(errorMessage = "No app found to open this file.") }
        }
    }

    fun shareExportedFile(context: Context, entry: ExportHistoryEntity) {
        if (entry.isTreeUri) {
            _actionState.update { it.copy(errorMessage = "Multi-page PNG folders can be shared from Sheet Details or recreated.") }
            return
        }
        val uri = Uri.parse(entry.uriString)
        val mime = runCatching { ExportFormat.valueOf(entry.format).mimeType }.getOrDefault("*/*")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Exported File"))
    }

    fun clearMessages() {
        _actionState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
