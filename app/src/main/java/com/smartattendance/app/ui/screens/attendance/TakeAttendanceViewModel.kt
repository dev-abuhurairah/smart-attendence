package com.smartattendance.app.ui.screens.attendance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.dao.SessionHeaderTuple
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.repository.AttendanceRepository
import com.smartattendance.app.domain.model.AttendanceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TakeAttendanceUiState(
    val isLoading: Boolean = true,
    val header: SessionHeaderTuple? = null,
    val date: String = "",
    val lectureNumber: Int = 1,
    val students: List<StudentEntity> = emptyList(),
    val marks: Map<Long, AttendanceStatus> = emptyMap(),
    val searchQuery: String = "",
    val canUndo: Boolean = false,
    val isEditingExisting: Boolean = false,
    val loadedFromDraft: Boolean = false,
    val highlightUnmarked: Boolean = false,
    val scrollToStudentId: Long? = null,
    val showSummaryDialog: Boolean = false,
    val savedSessionId: Long? = null,
    val errorMessage: String? = null
) {
    val filteredStudents: List<StudentEntity>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return students
            return students.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.rollNumber.contains(q, ignoreCase = true)
            }
        }

    val totalCount: Int get() = students.size
    val presentCount: Int get() = students.count { marks[it.id] == AttendanceStatus.PRESENT }
    val absentCount: Int get() = students.count { marks[it.id] == AttendanceStatus.ABSENT }
    val leaveCount: Int get() = students.count { marks[it.id] == AttendanceStatus.LEAVE }
    val unmarkedCount: Int get() = students.count { marks[it.id] == null }
}

@HiltViewModel
class TakeAttendanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AttendanceRepository
) : ViewModel() {

    private val subjectId: Long = savedStateHandle["subjectId"] ?: 0L
    private val date: String = savedStateHandle["date"] ?: ""
    private val lectureNumber: Int = savedStateHandle["lectureNumber"] ?: 1

    private val undoStack = ArrayDeque<Map<Long, AttendanceStatus>>()

    private val _uiState = MutableStateFlow(
        TakeAttendanceUiState(date = date, lectureNumber = lectureNumber)
    )
    val uiState: StateFlow<TakeAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            repo.loadMarkingContext(subjectId, date, lectureNumber).fold(
                onSuccess = { ctx ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            header = ctx.header,
                            students = ctx.students,
                            marks = ctx.initialMarks,
                            isEditingExisting = ctx.isEditingExisting,
                            loadedFromDraft = ctx.loadedFromDraft
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Failed to load students."
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun markStudent(studentId: Long, status: AttendanceStatus) {
        val current = _uiState.value.marks
        if (current[studentId] == status) return

        undoStack.addLast(current)
        if (undoStack.size > 50) undoStack.removeFirst()

        val updated = current + (studentId to status)
        _uiState.update {
            it.copy(
                marks = updated,
                canUndo = undoStack.isNotEmpty(),
                errorMessage = null
            )
        }
        autoSaveDraft(updated)
    }

    fun markAllPresent() {
        val current = _uiState.value.marks
        val students = _uiState.value.students
        if (students.isEmpty()) return

        undoStack.addLast(current)
        if (undoStack.size > 50) undoStack.removeFirst()

        val updated = students.associate { it.id to AttendanceStatus.PRESENT }
        _uiState.update {
            it.copy(
                marks = updated,
                canUndo = undoStack.isNotEmpty(),
                highlightUnmarked = false,
                errorMessage = null
            )
        }
        autoSaveDraft(updated)
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _uiState.update {
            it.copy(
                marks = previous,
                canUndo = undoStack.isNotEmpty(),
                errorMessage = null
            )
        }
        autoSaveDraft(previous)
    }

    private fun autoSaveDraft(marks: Map<Long, AttendanceStatus>) {
        if (_uiState.value.isEditingExisting) return
        viewModelScope.launch {
            repo.saveDraft(subjectId, date, lectureNumber, marks)
        }
    }

    fun requestSave() {
        val state = _uiState.value
        val firstUnmarked = state.students.firstOrNull { state.marks[it.id] == null }
        if (firstUnmarked != null) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    highlightUnmarked = true,
                    scrollToStudentId = firstUnmarked.id,
                    errorMessage = "${state.unmarkedCount} student(s) still unmarked. Jumped to ${firstUnmarked.rollNumber}."
                )
            }
            return
        }
        _uiState.update { it.copy(showSummaryDialog = true, errorMessage = null) }
    }

    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToStudentId = null) }
    }

    fun dismissSummaryDialog() {
        _uiState.update { it.copy(showSummaryDialog = false) }
    }

    fun confirmSave() {
        val state = _uiState.value
        val teacherName = state.header?.teacherName ?: return
        viewModelScope.launch {
            repo.saveAttendanceSession(
                subjectId = subjectId,
                date = date,
                lectureNumber = lectureNumber,
                teacherNameSnapshot = teacherName,
                marks = state.marks
            ).fold(
                onSuccess = { sessionId ->
                    _uiState.update {
                        it.copy(
                            showSummaryDialog = false,
                            savedSessionId = sessionId
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            showSummaryDialog = false,
                            errorMessage = err.message ?: "Failed to save attendance."
                        )
                    }
                }
            )
        }
    }
}
