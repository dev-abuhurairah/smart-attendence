package com.smartattendance.app.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.db.dao.SubjectWithTeacherTuple
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProgramsFlowViewModel @Inject constructor(
    private val repo: AttendanceRepository
) : ViewModel() {

    val activePrograms: StateFlow<List<ProgramEntity>> = repo.observeActivePrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeSemesters(programId: Long, includeArchived: Boolean): Flow<List<SemesterEntity>> =
        repo.observeSemesters(programId, includeArchived)

    fun observeSubjects(semesterId: Long): Flow<List<SubjectWithTeacherTuple>> =
        repo.observeSubjectsWithTeacher(semesterId)

    suspend fun getProgramName(programId: Long): String =
        repo.getProgram(programId)?.name ?: "Program"

    suspend fun getSemesterLabel(semesterId: Long): String {
        val sem = repo.getSemester(semesterId) ?: return "Semester"
        val sec = if (sem.section.isBlank()) "" else " (${sem.section})"
        return "Sem ${sem.semesterNumber}$sec • ${sem.session}"
    }

    suspend fun hasExistingSession(subjectId: Long, date: String, lectureNumber: Int): Boolean =
        repo.hasExistingSession(subjectId, date, lectureNumber)
}
