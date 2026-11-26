package com.smartattendance.app.ui.screens.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.repository.ExportRepository
import com.smartattendance.app.data.repository.ExportWriteResult
import com.smartattendance.app.data.repository.GeneratedExportBundle
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.export.ExportMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportDialogUiState(
    val selectedMode: ExportMode = ExportMode.SINGLE_LECTURE,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val existingRegisterEntry: ExportHistoryEntity? = null,
    val isExporting: Boolean = false,
    val pendingBundleForSaf: GeneratedExportBundle? = null,
    val pendingShareBundle: GeneratedExportBundle? = null,
    val needsNewLocationMessage: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepo: ExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportDialogUiState())
    val uiState: StateFlow<ExportDialogUiState> = _uiState.asStateFlow()

    fun setModeAndFormat(subjectId: Long, mode: ExportMode, format: ExportFormat) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                selectedFormat = format,
                errorMessage = null,
                statusMessage = null
            )
        }
        if (mode == ExportMode.FULL_REGISTER) {
            viewModelScope.launch {
                val existing = exportRepo.findLatestRegisterExport(subjectId, format)
                _uiState.update { it.copy(existingRegisterEntry = existing) }
            }
        } else {
            _uiState.update { it.copy(existingRegisterEntry = null) }
        }
    }

    fun prepareExportForSaveAsNew(sessionId: Long?, subjectId: Long) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            exportRepo.generateToTemp(sessionId, subjectId, state.selectedMode, state.selectedFormat).fold(
                onSuccess = { bundle ->
                    _uiState.update { it.copy(isExporting = false, pendingBundleForSaf = bundle) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isExporting = false, errorMessage = err.message ?: "Export generation failed.")
                    }
                }
            )
        }
    }

    fun updateExistingRegister(sessionId: Long?, subjectId: Long) {
        val existing = _uiState.value.existingRegisterEntry ?: return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            exportRepo.generateToTemp(sessionId, subjectId, state.selectedMode, state.selectedFormat).fold(
                onSuccess = { bundle ->
                    val uri = Uri.parse(existing.uriString)
                    val writeResult = if (existing.isTreeUri) {
                        exportRepo.writePngBundleToTreeUri(bundle, uri, takePersistablePermission = false)
                    } else {
                        exportRepo.writeBundleToFileUri(bundle, uri, takePersistablePermission = false)
                    }
                    handleWriteResult(writeResult)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isExporting = false, errorMessage = err.message ?: "Export generation failed.")
                    }
                }
            )
        }
    }

    fun prepareQuickShare(sessionId: Long?, subjectId: Long) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            exportRepo.generateToTemp(sessionId, subjectId, state.selectedMode, state.selectedFormat).fold(
                onSuccess = { bundle ->
                    _uiState.update { it.copy(isExporting = false, pendingShareBundle = bundle) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isExporting = false, errorMessage = err.message ?: "Failed to prepare share.")
                    }
                }
            )
        }
    }

    fun onSafTargetSelected(uri: Uri) {
        val bundle = _uiState.value.pendingBundleForSaf ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, pendingBundleForSaf = null, needsNewLocationMessage = null) }
            val result = if (bundle.format == ExportFormat.PNG) {
                exportRepo.writePngBundleToTreeUri(bundle, uri)
            } else {
                exportRepo.writeBundleToFileUri(bundle, uri)
            }
            handleWriteResult(result)
        }
    }

    fun clearPendingSaf() {
        _uiState.update { it.copy(pendingBundleForSaf = null) }
    }

    fun launchShareIntent(context: Context, bundle: GeneratedExportBundle) {
        val intent = exportRepo.buildShareIntent(bundle)
        context.startActivity(android.content.Intent.createChooser(intent, "Share Attendance"))
        _uiState.update { it.copy(pendingShareBundle = null) }
    }

    private fun handleWriteResult(result: ExportWriteResult) {
        when (result) {
            is ExportWriteResult.Success -> _uiState.update {
                it.copy(isExporting = false, statusMessage = "Saved: ${result.displayName}")
            }
            is ExportWriteResult.NeedsNewLocation -> _uiState.update {
                it.copy(
                    isExporting = false,
                    needsNewLocationMessage = result.reason,
                    pendingBundleForSaf = result.bundle
                )
            }
            is ExportWriteResult.Failure -> _uiState.update {
                it.copy(
                    isExporting = false,
                    errorMessage = result.error,
                    pendingBundleForSaf = result.bundle
                )
            }
        }
    }
}
