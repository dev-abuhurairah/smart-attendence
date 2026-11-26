package com.smartattendance.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM programs WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): ProgramEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(program: ProgramEntity): Long
}

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters WHERE program_id = :programId AND is_archived = 0 ORDER BY semester_number ASC, section ASC")
    fun observeActiveByProgram(programId: Long): Flow<List<SemesterEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(semester: SemesterEntity): Long
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE semester_id = :semesterId AND is_archived = 0")
    fun observeActiveBySemester(semesterId: Long): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(students: List<StudentEntity>): List<Long>
}

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers WHERE is_archived = 0 ORDER BY name ASC")
    fun observeActiveTeachers(): Flow<List<TeacherEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(teacher: TeacherEntity): Long
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE semester_id = :semesterId AND is_archived = 0 ORDER BY name ASC")
    fun observeActiveBySemester(semesterId: Long): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subject: SubjectEntity): Long
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_sessions WHERE subject_id = :subjectId AND date = :date AND lecture_number = :lectureNumber LIMIT 1")
    suspend fun findSession(subjectId: Long, date: String, lectureNumber: Int): AttendanceSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: AttendanceSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM setup_drafts WHERE id = 1 LIMIT 1")
    suspend fun getSetupDraft(): SetupDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetupDraft(draft: SetupDraftEntity)

    @Query("DELETE FROM setup_drafts WHERE id = 1")
    suspend fun clearSetupDraft()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceDraft(draft: AttendanceDraftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraftRecords(records: List<AttendanceDraftRecordEntity>)
}

@Dao
interface ExportHistoryDao {
    @Query("SELECT * FROM export_history ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExportHistoryEntity): Long
}
