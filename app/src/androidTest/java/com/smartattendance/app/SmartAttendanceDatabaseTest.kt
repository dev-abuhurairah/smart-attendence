package com.smartattendance.app

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.entity.AttendanceRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceSessionEntity
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.domain.model.AttendanceStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartAttendanceDatabaseTest {

    private lateinit var db: SmartAttendanceDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartAttendanceDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateProgramNormalizedName_throwsConstraintException() = runBlocking {
        db.programDao().insert(ProgramEntity(name = "BSCS", normalizedName = "bscs"))
        db.programDao().insert(ProgramEntity(name = "bscs", normalizedName = "bscs"))
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateAttendanceSession_throwsConstraintException() = runBlocking {
        val pId = db.programDao().insert(ProgramEntity(name = "BSCS", normalizedName = "bscs"))
        val semId = db.semesterDao().insert(SemesterEntity(programId = pId, semesterNumber = 1, section = "A", session = "2024-2028"))
        val tId = db.teacherDao().insert(TeacherEntity(name = "Dr. Sara"))
        val subId = db.subjectDao().insert(SubjectEntity(semesterId = semId, teacherId = tId, name = "Math"))

        db.attendanceDao().insertSession(
            AttendanceSessionEntity(subjectId = subId, date = "2026-11-26", lectureNumber = 1, teacherNameSnapshot = "Dr. Sara")
        )
        db.attendanceDao().insertSession(
            AttendanceSessionEntity(subjectId = subId, date = "2026-11-26", lectureNumber = 1, teacherNameSnapshot = "Dr. Sara")
        )
        Unit
    }

    @Test
    fun archivingStudent_preservesPastAttendanceRecords() = runBlocking {
        val pId = db.programDao().insert(ProgramEntity(name = "BSCS", normalizedName = "bscs"))
        val semId = db.semesterDao().insert(SemesterEntity(programId = pId, semesterNumber = 1, section = "A", session = "2024-2028"))
        val tId = db.teacherDao().insert(TeacherEntity(name = "Dr. Sara"))
        val subId = db.subjectDao().insert(SubjectEntity(semesterId = semId, teacherId = tId, name = "Math"))
        val stuId = db.studentDao().insert(StudentEntity(semesterId = semId, rollNumber = "CS-01", normalizedRoll = "cs-01", name = "Ayesha"))
        val sesId = db.attendanceDao().insertSession(
            AttendanceSessionEntity(subjectId = subId, date = "2026-11-26", lectureNumber = 1, teacherNameSnapshot = "Dr. Sara")
        )
        db.attendanceDao().insertRecords(listOf(AttendanceRecordEntity(sessionId = sesId, studentId = stuId, status = AttendanceStatus.PRESENT)))

        db.studentDao().setArchived(stuId, true)
        val records = db.attendanceDao().getRecordsForSession(sesId)
        assertEquals(1, records.size)
        assertEquals(AttendanceStatus.PRESENT, records.first().status)
    }
}
