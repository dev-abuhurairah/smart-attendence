package com.smartattendance.app.data.repository

import androidx.room.withTransaction
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.dao.ProgramDao
import com.smartattendance.app.data.local.db.dao.SemesterDao
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.dao.TeacherDao
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class CopySubjectInput(
    val name: String,
    val code: String,
    val teacherId: Long
)

@Singleton
class CopySemesterRepository @Inject constructor(
    private val db: SmartAttendanceDatabase,
    private val programDao: ProgramDao,
    private val semesterDao: SemesterDao,
    private val studentDao: StudentDao,
    private val teacherDao: TeacherDao,
    private val subjectDao: SubjectDao
) {
    fun observeAllPrograms() = programDao.observeAllPrograms()
    fun observeAllSemesters() = semesterDao.observeAllSemesters()
    fun observeActiveTeachers(): Flow<List<TeacherEntity>> = teacherDao.observeActiveTeachers()

    suspend fun getStudentsForSemester(semesterId: Long): List<StudentEntity> =
        studentDao.getActiveBySemester(semesterId)
            .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }

    suspend fun copyToNewSemester(
        programId: Long,
        newSemesterNumber: Int,
        newSection: String,
        newSession: String,
        selectedStudents: List<StudentEntity>,
        subjects: List<CopySubjectInput>
    ): Result<Long> = runCatching {
        require(newSemesterNumber > 0) { "Semester number must be > 0." }
        val cleanSection = newSection.trim()
        val cleanSession = newSession.trim()
        require(cleanSession.isNotEmpty()) { "Session is required." }
        require(selectedStudents.isNotEmpty()) { "Select at least one student to copy." }
        require(subjects.isNotEmpty()) { "Add at least one subject for the new semester." }

        db.withTransaction {
            val dup = semesterDao.findDuplicate(programId, newSemesterNumber, cleanSection, cleanSession)
            if (dup != null) {
                throw IllegalStateException("Semester $newSemesterNumber ($cleanSection) for $cleanSession already exists.")
            }

            val newSemId = semesterDao.insert(
                SemesterEntity(
                    programId = programId,
                    semesterNumber = newSemesterNumber,
                    section = cleanSection,
                    session = cleanSession
                )
            )

            studentDao.insertAll(
                selectedStudents.map { st ->
                    StudentEntity(
                        semesterId = newSemId,
                        rollNumber = st.rollNumber,
                        normalizedRoll = st.normalizedRoll,
                        name = st.name
                    )
                }
            )

            subjects.forEach { sub ->
                subjectDao.insert(
                    SubjectEntity(
                        semesterId = newSemId,
                        teacherId = sub.teacherId,
                        name = sub.name.trim(),
                        code = sub.code.trim().ifEmpty { null }
                    )
                )
            }
            newSemId
        }
    }
}
