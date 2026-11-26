package com.smartattendance.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.DraftDao
import com.smartattendance.app.data.local.db.dao.ExportHistoryDao
import com.smartattendance.app.data.local.db.dao.HomeSummaryDao
import com.smartattendance.app.data.local.db.dao.ProgramDao
import com.smartattendance.app.data.local.db.dao.SemesterDao
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.dao.TeacherDao
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

@Database(
    entities = [
        ProgramEntity::class,
        SemesterEntity::class,
        StudentEntity::class,
        TeacherEntity::class,
        SubjectEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class,
        AttendanceDraftEntity::class,
        AttendanceDraftRecordEntity::class,
        SetupDraftEntity::class,
        ExportHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartAttendanceDatabase : RoomDatabase() {
    abstract fun homeSummaryDao(): HomeSummaryDao
    abstract fun programDao(): ProgramDao
    abstract fun semesterDao(): SemesterDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun draftDao(): DraftDao
    abstract fun exportHistoryDao(): ExportHistoryDao
}
