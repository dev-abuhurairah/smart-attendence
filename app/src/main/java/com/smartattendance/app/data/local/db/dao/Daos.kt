package com.smartattendance.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartattendance.app.data.local.db.entity.AttendanceDraftEntity
import com.smartattendance.app.data.local.db.entity.AttendanceDraftRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceSessionEntity
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.SetupDraftEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow

data class SubjectWithTeacherTuple(
    val id: Long,
    val semesterId: Long,
    val teacherId: Long,
    val name: String,
    val code: String?,
    val teacherName: String
)

data class SessionHeaderTuple(
    val programName: String,
    val semesterNumber: Int,
    val section: String,
    val session: String,
    val subjectName: String,
    val subjectCode: String?,
    val teacherName: String
)

data class SavedSheetSummaryTuple(
    val sessionId: Long,
    val subjectId: Long,
    val programId: Long,
    val programName: String,
    val semesterId: Long,
    val semesterNumber: Int,
    val section: String,
    val sessionName: String,
    val subjectName: String,
    val subjectCode: String?,
    val teacherNameSnapshot: String,
    val date: String,
    val lectureNumber: Int,
    val updatedAt: Long,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int
)

data class SheetRecordTuple(
    val studentId: Long,
    val rollNumber: String,
    val studentName: String,
    val isArchived: Boolean,
    val status: com.smartattendance.app.domain.model.AttendanceStatus
)

@Dao
interface HomeSummaryDao {
    @Query("SELECT COUNT(*) FROM programs WHERE is_archived = 0")
    fun observeActiveProgramCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM students s
        INNER JOIN semesters sem ON sem.id = s.semester_id
        INNER JOIN programs p ON p.id = sem.program_id
        WHERE s.is_archived = 0 AND sem.is_archived = 0 AND p.is_archived = 0
        """
    )
    fun observeActiveStudentCount(): Flow<Int>
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActivePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs ORDER BY is_archived ASC, name ASC")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProgramEntity?

    @Query("SELECT * FROM programs WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): ProgramEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(program: ProgramEntity): Long

    @Update
    suspend fun update(program: ProgramEntity)

    @Query("UPDATE programs SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM semesters WHERE program_id = :programId")
    suspend fun countSemesters(programId: Long): Int

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters WHERE program_id = :programId AND is_archived = 0 ORDER BY semester_number ASC, section ASC")
    fun observeActiveByProgram(programId: Long): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE program_id = :programId ORDER BY is_archived ASC, semester_number ASC, section ASC")
    fun observeAllByProgram(programId: Long): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY is_archived ASC, semester_number ASC, section ASC")
    fun observeAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SemesterEntity?

    @Query(
        """
        SELECT * FROM semesters
        WHERE program_id = :programId
          AND semester_number = :semesterNumber
          AND LOWER(TRIM(section)) = LOWER(TRIM(:section))
          AND LOWER(TRIM(session)) = LOWER(TRIM(:session))
        LIMIT 1
        """
    )
    suspend fun findDuplicate(
        programId: Long,
        semesterNumber: Int,
        section: String,
        session: String
    ): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(semester: SemesterEntity): Long

    @Update
    suspend fun update(semester: SemesterEntity)

    @Query("UPDATE semesters SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query(
        """
        SELECT COUNT(*) FROM attendance_sessions ses
        INNER JOIN subjects sub ON sub.id = ses.subject_id
        WHERE sub.semester_id = :semesterId
        """
    )
    suspend fun countAttendanceSessions(semesterId: Long): Int

    @Query("DELETE FROM students WHERE semester_id = :semesterId")
    suspend fun deleteStudentsBySemester(semesterId: Long)

    @Query("DELETE FROM subjects WHERE semester_id = :semesterId")
    suspend fun deleteSubjectsBySemester(semesterId: Long)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE semester_id = :semesterId AND is_archived = 0")
    fun observeActiveBySemester(semesterId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE semester_id = :semesterId AND is_archived = 0")
    suspend fun getActiveBySemester(semesterId: Long): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY is_archived ASC, semester_id ASC")
    fun observeAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE semester_id = :semesterId AND normalized_roll = :normalizedRoll LIMIT 1")
    suspend fun findByNormalizedRoll(semesterId: Long, normalizedRoll: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(students: List<StudentEntity>): List<Long>

    @Update
    suspend fun update(student: StudentEntity)

    @Query("UPDATE students SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE student_id = :studentId")
    suspend fun countAttendanceRecords(studentId: Long): Int

    @Query("DELETE FROM attendance_records WHERE student_id = :studentId")
    suspend fun deleteAttendanceRecords(studentId: Long)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActiveTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers ORDER BY is_archived ASC, name ASC")
    fun observeAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(teacher: TeacherEntity): Long

    @Update
    suspend fun update(teacher: TeacherEntity)

    @Query("UPDATE teachers SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM subjects WHERE teacher_id = :teacherId")
    suspend fun countSubjects(teacherId: Long): Int

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE semester_id = :semesterId AND is_archived = 0 ORDER BY name ASC")
    fun observeActiveBySemester(semesterId: Long): Flow<List<SubjectEntity>>

    @Query(
        """
        SELECT sub.id, sub.semester_id AS semesterId, sub.teacher_id AS teacherId,
               sub.name, sub.code, t.name AS teacherName
        FROM subjects sub
        INNER JOIN teachers t ON t.id = sub.teacher_id
        WHERE sub.semester_id = :semesterId AND sub.is_archived = 0
        ORDER BY sub.name ASC
        """
    )
    fun observeActiveWithTeacher(semesterId: Long): Flow<List<SubjectWithTeacherTuple>>

    @Query(
        """
        SELECT p.name AS programName, sem.semester_number AS semesterNumber,
               sem.section AS section, sem.session AS session,
               sub.name AS subjectName, sub.code AS subjectCode, t.name AS teacherName
        FROM subjects sub
        INNER JOIN semesters sem ON sem.id = sub.semester_id
        INNER JOIN programs p ON p.id = sem.program_id
        INNER JOIN teachers t ON t.id = sub.teacher_id
        WHERE sub.id = :subjectId
        LIMIT 1
        """
    )
    suspend fun getSessionHeader(subjectId: Long): SessionHeaderTuple?

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY is_archived ASC, name ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Query("UPDATE subjects SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM attendance_sessions WHERE subject_id = :subjectId")
    suspend fun countAttendanceSessions(subjectId: Long): Int

    @Query("DELETE FROM attendance_sessions WHERE subject_id = :subjectId")
    suspend fun deleteAttendanceSessions(subjectId: Long)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_sessions WHERE subject_id = :subjectId AND date = :date AND lecture_number = :lectureNumber LIMIT 1")
    suspend fun findSession(subjectId: Long, date: String, lectureNumber: Int): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_records WHERE session_id = :sessionId")
    suspend fun getRecordsForSession(sessionId: Long): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: AttendanceSessionEntity): Long

    @Update
    suspend fun updateSession(session: AttendanceSessionEntity)

    @Query("DELETE FROM attendance_records WHERE session_id = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query(
        """
        SELECT ses.id AS sessionId,
               ses.subject_id AS subjectId,
               p.id AS programId,
               p.name AS programName,
               sem.id AS semesterId,
               sem.semester_number AS semesterNumber,
               sem.section AS section,
               sem.session AS sessionName,
               sub.name AS subjectName,
               sub.code AS subjectCode,
               ses.teacher_name_snapshot AS teacherNameSnapshot,
               ses.date AS date,
               ses.lecture_number AS lectureNumber,
               ses.updated_at AS updatedAt,
               COALESCE(SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS presentCount,
               COALESCE(SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absentCount,
               COALESCE(SUM(CASE WHEN r.status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveCount
        FROM attendance_sessions ses
        INNER JOIN subjects sub ON sub.id = ses.subject_id
        INNER JOIN semesters sem ON sem.id = sub.semester_id
        INNER JOIN programs p ON p.id = sem.program_id
        LEFT JOIN attendance_records r ON r.session_id = ses.id
        GROUP BY ses.id
        ORDER BY ses.date DESC, ses.lecture_number DESC, ses.updated_at DESC
        """
    )
    fun observeAllSavedSheets(): Flow<List<SavedSheetSummaryTuple>>

    @Query(
        """
        SELECT ses.id AS sessionId,
               ses.subject_id AS subjectId,
               p.id AS programId,
               p.name AS programName,
               sem.id AS semesterId,
               sem.semester_number AS semesterNumber,
               sem.section AS section,
               sem.session AS sessionName,
               sub.name AS subjectName,
               sub.code AS subjectCode,
               ses.teacher_name_snapshot AS teacherNameSnapshot,
               ses.date AS date,
               ses.lecture_number AS lectureNumber,
               ses.updated_at AS updatedAt,
               COALESCE(SUM(CASE WHEN r.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS presentCount,
               COALESCE(SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absentCount,
               COALESCE(SUM(CASE WHEN r.status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS leaveCount
        FROM attendance_sessions ses
        INNER JOIN subjects sub ON sub.id = ses.subject_id
        INNER JOIN semesters sem ON sem.id = sub.semester_id
        INNER JOIN programs p ON p.id = sem.program_id
        LEFT JOIN attendance_records r ON r.session_id = ses.id
        WHERE ses.id = :sessionId
        GROUP BY ses.id
        LIMIT 1
        """
    )
    fun observeSheetSummary(sessionId: Long): Flow<SavedSheetSummaryTuple?>

    @Query(
        """
        SELECT st.id AS studentId,
               st.roll_number AS rollNumber,
               st.name AS studentName,
               st.is_archived AS isArchived,
               r.status AS status
        FROM attendance_records r
        INNER JOIN students st ON st.id = r.student_id
        WHERE r.session_id = :sessionId
        """
    )
    fun observeSheetRecords(sessionId: Long): Flow<List<SheetRecordTuple>>

    @Query("DELETE FROM attendance_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM setup_drafts WHERE id = 1 LIMIT 1")
    suspend fun getSetupDraft(): SetupDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetupDraft(draft: SetupDraftEntity)

    @Query("DELETE FROM setup_drafts WHERE id = 1")
    suspend fun clearSetupDraft()

    @Query("SELECT * FROM attendance_drafts WHERE subject_id = :subjectId AND date = :date AND lecture_number = :lectureNumber LIMIT 1")
    suspend fun findAttendanceDraft(subjectId: Long, date: String, lectureNumber: Int): AttendanceDraftEntity?

    @Query("SELECT * FROM attendance_draft_records WHERE draft_id = :draftId")
    suspend fun getDraftRecords(draftId: Long): List<AttendanceDraftRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceDraft(draft: AttendanceDraftEntity): Long

    @Query("DELETE FROM attendance_draft_records WHERE draft_id = :draftId")
    suspend fun deleteDraftRecords(draftId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraftRecords(records: List<AttendanceDraftRecordEntity>)

    @Query("DELETE FROM attendance_drafts WHERE subject_id = :subjectId AND date = :date AND lecture_number = :lectureNumber")
    suspend fun deleteAttendanceDraft(subjectId: Long, date: String, lectureNumber: Int)
}

@Dao
interface ExportHistoryDao {
    @Query("SELECT * FROM export_history ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExportHistoryEntity): Long
}
