package com.smartattendance.app.di

import android.content.Context
import androidx.room.Room
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.DraftDao
import com.smartattendance.app.data.local.db.dao.ExportHistoryDao
import com.smartattendance.app.data.local.db.dao.HomeSummaryDao
import com.smartattendance.app.data.local.db.dao.ProgramDao
import com.smartattendance.app.data.local.db.dao.SemesterDao
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.dao.TeacherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartAttendanceDatabase {
        return Room.databaseBuilder(
            context,
            SmartAttendanceDatabase::class.java,
            "smart_attendance.db"
        ).build()
    }

    @Provides fun provideHomeSummaryDao(db: SmartAttendanceDatabase): HomeSummaryDao = db.homeSummaryDao()
    @Provides fun provideProgramDao(db: SmartAttendanceDatabase): ProgramDao = db.programDao()
    @Provides fun provideSemesterDao(db: SmartAttendanceDatabase): SemesterDao = db.semesterDao()
    @Provides fun provideStudentDao(db: SmartAttendanceDatabase): StudentDao = db.studentDao()
    @Provides fun provideTeacherDao(db: SmartAttendanceDatabase): TeacherDao = db.teacherDao()
    @Provides fun provideSubjectDao(db: SmartAttendanceDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideAttendanceDao(db: SmartAttendanceDatabase): AttendanceDao = db.attendanceDao()
    @Provides fun provideDraftDao(db: SmartAttendanceDatabase): DraftDao = db.draftDao()
    @Provides fun provideExportHistoryDao(db: SmartAttendanceDatabase): ExportHistoryDao = db.exportHistoryDao()
}
