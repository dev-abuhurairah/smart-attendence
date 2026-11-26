package com.smartattendance.app.ui.screens.sheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.dao.SavedSheetSummaryTuple
import com.smartattendance.app.data.local.db.dao.SheetRecordTuple
import com.smartattendance.app.data.repository.SheetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SheetDetailUiState(
    val summary: SavedSheetSummaryTuple? = null,
    val records: List<SheetRecordTuple> = emptyList(),
    val isDeleted: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class SheetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SheetsRepository
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L
    private val deletedFlag = MutableStateFlow(false)

    val uiState: StateFlow<SheetDetailUiState> = combine(
        repo.observeSheetSummary(sessionId),
        repo.observeSheetRecordsSorted(sessionId),
        deletedFlag
    ) { summary, records, deleted ->
        SheetDetailUiState(
            summary = summary,
            records = records,
            isDeleted = deleted,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SheetDetailUiState())

    fun deleteSheet() {
        viewModelScope.launch {
            repo.deleteSession(sessionId)
            deletedFlag.value = true
        }
    }
}
