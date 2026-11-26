package com.smartattendance.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.data.repository.CopySemesterRepository
import com.smartattendance.app.data.repository.CopySubjectInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CopySemesterUiState(
    val programs: List<ProgramEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val teachers: List<TeacherEntity> = emptyList(),
    val sourceSemester: SemesterEntity? = null,
    val sourceStudents: List<StudentEntity> = emptyList(),
    val selectedStudentIds: Set<Long> = emptySet(),
    val newSemesterNumber: String = "",
    val newSection: String = "",
    val newSession: String = "",
    val subjects: List<CopySubjectInput> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isDone: Boolean = false
)

@HiltViewModel
class CopySemesterViewModel @Inject constructor(
    private val repo: CopySemesterRepository
) : ViewModel() {

    private val _local = MutableStateFlow(CopySemesterUiState())

    val uiState: StateFlow<CopySemesterUiState> = combine(
        repo.observeAllPrograms(),
        repo.observeAllSemesters(),
        repo.observeActiveTeachers(),
        _local
    ) { progs, sems, teachers, local ->
        local.copy(programs = progs, semesters = sems, teachers = teachers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CopySemesterUiState())

    fun selectSourceSemester(sem: SemesterEntity) {
        viewModelScope.launch {
            val students = repo.getStudentsForSemester(sem.id)
            _local.update {
                it.copy(
                    sourceSemester = sem,
                    sourceStudents = students,
                    selectedStudentIds = students.map { s -> s.id }.toSet(),
                    newSemesterNumber = (sem.semesterNumber + 1).toString(),
                    newSection = sem.section,
                    newSession = sem.session,
                    errorMessage = null
                )
            }
        }
    }

    fun toggleStudent(studentId: Long) {
        _local.update { s ->
            val next = if (studentId in s.selectedStudentIds) s.selectedStudentIds - studentId else s.selectedStudentIds + studentId
            s.copy(selectedStudentIds = next)
        }
    }

    fun selectAllStudents(selectAll: Boolean) {
        _local.update { s ->
            s.copy(selectedStudentIds = if (selectAll) s.sourceStudents.map { it.id }.toSet() else emptySet())
        }
    }

    fun updateTargetFields(semNum: String, section: String, session: String) {
        _local.update { it.copy(newSemesterNumber = semNum, newSection = section, newSession = session, errorMessage = null) }
    }

    fun addSubject(name: String, code: String, teacherId: Long) {
        if (name.isBlank()) {
            _local.update { it.copy(errorMessage = "Subject name is required.") }
            return
        }
        _local.update { it.copy(subjects = it.subjects + CopySubjectInput(name, code, teacherId), errorMessage = null) }
    }

    fun removeSubject(idx: Int) {
        _local.update { it.copy(subjects = it.subjects.filterIndexed { i, _ -> i != idx }) }
    }

    fun copyNow() {
        val state = _local.value
        val src = state.sourceSemester ?: return
        val semNum = state.newSemesterNumber.toIntOrNull() ?: 0
        val chosenStudents = state.sourceStudents.filter { it.id in state.selectedStudentIds }

        viewModelScope.launch {
            repo.copyToNewSemester(
                programId = src.programId,
                newSemesterNumber = semNum,
                newSection = state.newSection,
                newSession = state.newSession,
                selectedStudents = chosenStudents,
                subjects = state.subjects
            ).fold(
                onSuccess = {
                    _local.update { it.copy(statusMessage = "Semester copied successfully!", isDone = true) }
                },
                onFailure = { err ->
                    _local.update { it.copy(errorMessage = err.message ?: "Failed to copy semester.") }
                }
            )
        }
    }
}
