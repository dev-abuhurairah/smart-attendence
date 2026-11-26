package com.smartattendance.app.data.repository

import androidx.room.withTransaction
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.DraftDao
import com.smartattendance.app.data.local.db.dao.ProgramDao
import com.smartattendance.app.data.local.db.dao.SemesterDao
import com.smartattendance.app.data.local.db.dao.SessionHeaderTuple
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.dao.SubjectWithTeacherTuple
import com.smartattendance.app.data.local.db.entity.AttendanceDraftEntity
import com.smartattendance.app.data.local.db.entity.AttendanceDraftRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceSessionEntity
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class MarkingContext(
    val header: SessionHeaderTuple,
    val students: List<StudentEntity>,
    val initialMarks: Map<Long, AttendanceStatus>,
    val isEditingExisting: Boolean,
    val loadedFromDraft: Boolean
)

@Singleton
class AttendanceRepository @Inject constructor(
    private val db: SmartAttendanceDatabase,
    private val programDao: ProgramDao,
    private val semesterDao: SemesterDao,
    private val subjectDao: SubjectDao,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val draftDao: DraftDao
) {
    fun observeActivePrograms(): Flow<List<ProgramEntity>> = programDao.observeActivePrograms()

    suspend fun getProgram(programId: Long): ProgramEntity? = programDao.getById(programId)

    fun observeSemesters(programId: Long, includeArchived: Boolean): Flow<List<SemesterEntity>> =
        if (includeArchived) semesterDao.observeAllByProgram(programId)
        else semesterDao.observeActiveByProgram(programId)

    suspend fun getSemester(semesterId: Long): SemesterEntity? = semesterDao.getById(semesterId)

    fun observeSubjectsWithTeacher(semesterId: Long): Flow<List<SubjectWithTeacherTuple>> =
        subjectDao.observeActiveWithTeacher(semesterId)

    suspend fun hasExistingSession(subjectId: Long, date: String, lectureNumber: Int): Boolean =
        attendanceDao.findSession(subjectId, date, lectureNumber) != null

    suspend fun loadMarkingContext(
        subjectId: Long,
        date: String,
        lectureNumber: Int
    ): Result<MarkingContext> = runCatching {
        val subject = subjectDao.getById(subjectId)
            ?: throw IllegalStateException("Subject not found.")
        val header = subjectDao.getSessionHeader(subjectId)
            ?: throw IllegalStateException("Subject details not found.")
        val students = studentDao.getActiveBySemester(subject.semesterId)
            .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }

        val existingSession = attendanceDao.findSession(subjectId, date, lectureNumber)
        if (existingSession != null) {
            val records = attendanceDao.getRecordsForSession(existingSession.id)
                .associate { it.studentId to it.status }
            MarkingContext(
                header = header.copy(teacherName = existingSession.teacherNameSnapshot),
                students = students,
                initialMarks = records,
                isEditingExisting = true,
                loadedFromDraft = false
            )
        } else {
            val draft = draftDao.findAttendanceDraft(subjectId, date, lectureNumber)
            val draftMarks = if (draft != null) {
                draftDao.getDraftRecords(draft.id).associate { it.studentId to it.status }
            } else {
                emptyMap()
            }
            MarkingContext(
                header = header,
                students = students,
                initialMarks = draftMarks,
                isEditingExisting = false,
                loadedFromDraft = draft != null && draftMarks.isNotEmpty()
            )
        }
    }

    suspend fun saveDraft(
        subjectId: Long,
        date: String,
        lectureNumber: Int,
        marks: Map<Long, AttendanceStatus>
    ) {
        if (marks.isEmpty()) {
            draftDao.deleteAttendanceDraft(subjectId, date, lectureNumber)
            return
        }
        db.withTransaction {
            val existing = draftDao.findAttendanceDraft(subjectId, date, lectureNumber)
            val draftId = if (existing != null) {
                draftDao.upsertAttendanceDraft(existing.copy(updatedAt = System.currentTimeMillis()))
                existing.id
            } else {
                draftDao.upsertAttendanceDraft(
                    AttendanceDraftEntity(
                        subjectId = subjectId,
                        date = date,
                        lectureNumber = lectureNumber
                    )
                )
            }
            draftDao.deleteDraftRecords(draftId)
            draftDao.upsertDraftRecords(
                marks.map { (studentId, status) ->
                    AttendanceDraftRecordEntity(
                        draftId = draftId,
                        studentId = studentId,
                        status = status
                    )
                }
            )
        }
    }

    suspend fun saveAttendanceSession(
        subjectId: Long,
        date: String,
        lectureNumber: Int,
        teacherNameSnapshot: String,
        marks: Map<Long, AttendanceStatus>
    ): Result<Long> = runCatching {
        require(!DateUtils.isFutureIsoDate(date)) { "Future dates are not allowed." }
        require(lectureNumber > 0) { "Lecture number must be at least 1." }

        db.withTransaction {
            val now = System.currentTimeMillis()
            val existing = attendanceDao.findSession(subjectId, date, lectureNumber)
            val sessionId = if (existing != null) {
                attendanceDao.updateSession(existing.copy(updatedAt = now))
                attendanceDao.deleteRecordsForSession(existing.id)
                existing.id
            } else {
                attendanceDao.insertSession(
                    AttendanceSessionEntity(
                        subjectId = subjectId,
                        date = date,
                        lectureNumber = lectureNumber,
                        teacherNameSnapshot = teacherNameSnapshot,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            attendanceDao.insertRecords(
                marks.map { (studentId, status) ->
                    AttendanceRecordEntity(
                        sessionId = sessionId,
                        studentId = studentId,
                        status = status
                    )
                }
            )

            draftDao.deleteAttendanceDraft(subjectId, date, lectureNumber)
            sessionId
        }
    }
}
