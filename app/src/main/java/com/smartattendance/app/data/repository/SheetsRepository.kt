package com.smartattendance.app.data.repository

import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.SavedSheetSummaryTuple
import com.smartattendance.app.data.local.db.dao.SheetRecordTuple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SheetsFilter(
    val programName: String? = null,
    val semesterNumber: Int? = null,
    val section: String? = null,
    val subjectName: String? = null,
    val teacherName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    val isActive: Boolean
        get() = programName != null ||
            semesterNumber != null ||
            section != null ||
            subjectName != null ||
            teacherName != null ||
            startDate != null ||
            endDate != null

    fun matches(sheet: SavedSheetSummaryTuple): Boolean {
        if (programName != null && sheet.programName != programName) return false
        if (semesterNumber != null && sheet.semesterNumber != semesterNumber) return false
        if (section != null && sheet.section != section) return false
        if (subjectName != null && sheet.subjectName != subjectName) return false
        if (teacherName != null && sheet.teacherNameSnapshot != teacherName) return false
        if (startDate != null && sheet.date < startDate) return false
        if (endDate != null && sheet.date > endDate) return false
        return true
    }
}

@Singleton
class SheetsRepository @Inject constructor(
    private val attendanceDao: AttendanceDao
) {
    fun observeAllSheets(): Flow<List<SavedSheetSummaryTuple>> =
        attendanceDao.observeAllSavedSheets()

    fun observeSheetSummary(sessionId: Long): Flow<SavedSheetSummaryTuple?> =
        attendanceDao.observeSheetSummary(sessionId)

    fun observeSheetRecordsSorted(sessionId: Long): Flow<List<SheetRecordTuple>> =
        attendanceDao.observeSheetRecords(sessionId).map { list ->
            list.sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }
        }

    suspend fun deleteSession(sessionId: Long) {
        attendanceDao.deleteSessionById(sessionId)
    }
}
