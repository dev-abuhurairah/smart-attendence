package com.smartattendance.app.ui.screens.sheets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.dao.SavedSheetSummaryTuple
import com.smartattendance.app.data.repository.SheetsFilter
import com.smartattendance.app.data.repository.SheetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SheetsFilterOptions(
    val programs: List<String> = emptyList(),
    val semesters: List<Int> = emptyList(),
    val sections: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val teachers: List<String> = emptyList()
)

data class AttendanceSheetsUiState(
    val groupedSheets: Map<String, List<SavedSheetSummaryTuple>> = emptyMap(),
    val filter: SheetsFilter = SheetsFilter(),
    val options: SheetsFilterOptions = SheetsFilterOptions(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AttendanceSheetsViewModel @Inject constructor(
    repo: SheetsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SheetsFilter())

    val uiState: StateFlow<AttendanceSheetsUiState> = combine(
        repo.observeAllSheets(),
        _filter
    ) { allSheets, filter ->
        val options = SheetsFilterOptions(
            programs = allSheets.map { it.programName }.distinct().sorted(),
            semesters = allSheets.map { it.semesterNumber }.distinct().sorted(),
            sections = allSheets.map { it.section }.filter { it.isNotBlank() }.distinct().sorted(),
            subjects = allSheets.map { it.subjectName }.distinct().sorted(),
            teachers = allSheets.map { it.teacherNameSnapshot }.distinct().sorted()
        )
        val filtered = allSheets.filter { filter.matches(it) }
        val grouped = filtered.groupBy { it.date }
        AttendanceSheetsUiState(
            groupedSheets = grouped,
            filter = filter,
            options = options,
            totalCount = filtered.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttendanceSheetsUiState())

    fun updateFilter(transform: (SheetsFilter) -> SheetsFilter) {
        _filter.update(transform)
    }

    fun clearFilters() {
        _filter.value = SheetsFilter()
    }
}
