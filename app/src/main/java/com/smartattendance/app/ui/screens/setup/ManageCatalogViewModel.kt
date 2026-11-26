package com.smartattendance.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.data.repository.DeleteOutcome
import com.smartattendance.app.data.repository.SetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageCatalogUiState(
    val programs: List<ProgramEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val students: List<StudentEntity> = emptyList(),
    val teachers: List<TeacherEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ManageCatalogViewModel @Inject constructor(
    private val repo: SetupRepository
) : ViewModel() {

    private val feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<ManageCatalogUiState> = combine(
        repo.observeAllPrograms(),
        repo.observeAllSemesters(),
        repo.observeAllStudents(),
        repo.observeAllTeachers(),
        repo.observeAllSubjects(),
        feedback
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val programs = arr[0] as List<ProgramEntity>
        @Suppress("UNCHECKED_CAST")
        val semesters = arr[1] as List<SemesterEntity>
        @Suppress("UNCHECKED_CAST")
        val students = (arr[2] as List<StudentEntity>).sortedWith { a, b ->
            NaturalRollComparator.compare(a.rollNumber, b.rollNumber)
        }
        @Suppress("UNCHECKED_CAST")
        val teachers = arr[3] as List<TeacherEntity>
        @Suppress("UNCHECKED_CAST")
        val subjects = arr[4] as List<SubjectEntity>
        @Suppress("UNCHECKED_CAST")
        val msg = arr[5] as Pair<String?, String?>

        ManageCatalogUiState(
            programs = programs,
            semesters = semesters,
            students = students,
            teachers = teachers,
            subjects = subjects,
            statusMessage = msg.first,
            errorMessage = msg.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageCatalogUiState())

    fun clearFeedback() {
        feedback.update { null to null }
    }

    fun renameProgram(program: ProgramEntity, newName: String) = viewModelScope.launch {
        repo.updateProgram(program, newName).fold(
            onSuccess = { feedback.value = "Program updated." to null },
            onFailure = { feedback.value = null to (it.message ?: "Failed to update program.") }
        )
    }

    fun setProgramArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        repo.setProgramArchived(id, archived)
        feedback.value = (if (archived) "Program archived." else "Program restored.") to null
    }

    fun deleteProgram(id: Long) = viewModelScope.launch {
        val outcome = repo.deleteProgram(id)
        feedback.value = when (outcome) {
            DeleteOutcome.ARCHIVED_HAS_HISTORY -> "Program has semesters and was archived instead."
            DeleteOutcome.DELETED_PERMANENTLY -> "Program deleted."
        } to null
    }

    fun setSemesterArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        repo.setSemesterArchived(id, archived)
        feedback.value = (if (archived) "Semester archived." else "Semester restored.") to null
    }

    fun deleteSemester(id: Long, forcePermanent: Boolean) = viewModelScope.launch {
        val outcome = repo.deleteSemester(id, forcePermanent)
        feedback.value = when (outcome) {
            DeleteOutcome.ARCHIVED_HAS_HISTORY -> "Semester has attendance history and was archived to protect records."
            DeleteOutcome.DELETED_PERMANENTLY -> "Semester deleted permanently."
        } to null
    }

    fun addOrUpdateStudent(existing: StudentEntity?, semesterId: Long, roll: String, name: String) = viewModelScope.launch {
        val res = if (existing == null) {
            repo.addStudent(semesterId, roll, name)
        } else {
            repo.updateStudent(existing, roll, name)
        }
        res.fold(
            onSuccess = { feedback.value = "Student saved." to null },
            onFailure = { feedback.value = null to (it.message ?: "Failed to save student.") }
        )
    }

    fun setStudentArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        repo.setStudentArchived(id, archived)
        feedback.value = (if (archived) "Student archived." else "Student restored.") to null
    }

    fun deleteStudent(id: Long, forcePermanent: Boolean) = viewModelScope.launch {
        val outcome = repo.deleteStudent(id, forcePermanent)
        feedback.value = when (outcome) {
            DeleteOutcome.ARCHIVED_HAS_HISTORY -> "Student has attendance history and was archived."
            DeleteOutcome.DELETED_PERMANENTLY -> "Student deleted permanently."
        } to null
    }

    fun saveTeacher(existing: TeacherEntity?, name: String, phone: String, email: String) = viewModelScope.launch {
        repo.addOrUpdateTeacher(existing, name, phone, email).fold(
            onSuccess = { feedback.value = "Teacher saved." to null },
            onFailure = { feedback.value = null to (it.message ?: "Failed to save teacher.") }
        )
    }

    fun setTeacherArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        repo.setTeacherArchived(id, archived)
        feedback.value = (if (archived) "Teacher archived." else "Teacher restored.") to null
    }

    fun deleteTeacher(id: Long) = viewModelScope.launch {
        val outcome = repo.deleteTeacher(id)
        feedback.value = when (outcome) {
            DeleteOutcome.ARCHIVED_HAS_HISTORY -> "Teacher is assigned to subjects and was archived."
            DeleteOutcome.DELETED_PERMANENTLY -> "Teacher deleted."
        } to null
    }

    fun saveSubject(existing: SubjectEntity?, semesterId: Long, teacherId: Long, name: String, code: String) = viewModelScope.launch {
        repo.addOrUpdateSubject(existing, semesterId, teacherId, name, code).fold(
            onSuccess = { feedback.value = "Subject saved." to null },
            onFailure = { feedback.value = null to (it.message ?: "Failed to save subject.") }
        )
    }

    fun setSubjectArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        repo.setSubjectArchived(id, archived)
        feedback.value = (if (archived) "Subject archived." else "Subject restored.") to null
    }

    fun deleteSubject(id: Long, forcePermanent: Boolean) = viewModelScope.launch {
        val outcome = repo.deleteSubject(id, forcePermanent)
        feedback.value = when (outcome) {
            DeleteOutcome.ARCHIVED_HAS_HISTORY -> "Subject has attendance history and was archived."
            DeleteOutcome.DELETED_PERMANENTLY -> "Subject deleted permanently."
        } to null
    }
}
