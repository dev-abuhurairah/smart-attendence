package com.smartattendance.app

import com.smartattendance.app.data.local.db.dao.ReportRecordTuple
import com.smartattendance.app.data.repository.ReportQuery
import com.smartattendance.app.data.repository.ReportType
import com.smartattendance.app.data.repository.ReportsRepository
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsCalculationTest {

    private val sampleRecords = listOf(
        ReportRecordTuple(1L, "2026-11-26", 1, 1L, "BSCS", 1L, 3, "A", "2024-2028", 1L, "Math", 1L, "CS-01", "Ayesha", AttendanceStatus.PRESENT),
        ReportRecordTuple(2L, "2026-11-25", 1, 1L, "BSCS", 1L, 3, "A", "2024-2028", 1L, "Math", 1L, "CS-01", "Ayesha", AttendanceStatus.ABSENT),
        ReportRecordTuple(1L, "2026-11-26", 1, 1L, "BSCS", 1L, 3, "A", "2024-2028", 1L, "Math", 2L, "CS-02", "Bilal", AttendanceStatus.PRESENT),
        ReportRecordTuple(2L, "2026-11-25", 1, 1L, "BSCS", 1L, 3, "A", "2024-2028", 1L, "Math", 2L, "CS-02", "Bilal", AttendanceStatus.LEAVE)
    )

    @Test
    fun studentWiseReport_highlightsLowAttendanceAndAppliesLeaveRule() {
        val includeLeave = ReportsRepository.buildReportSummary(
            allRecords = sampleRecords,
            leaveRule = LeaveRule.INCLUDE_LEAVE,
            threshold = 75,
            query = ReportQuery(type = ReportType.STUDENT_WISE)
        )
        assertEquals(2, includeLeave.rows.size)
        // CS-01: 1P, 1A -> 50.0% (low)
        assertEquals("50.0%", includeLeave.rows[0].percentageLabel)
        assertTrue(includeLeave.rows[0].isLowAttendance)
        // CS-02: 1P, 1L -> 50.0% (low when Leave included)
        assertEquals("50.0%", includeLeave.rows[1].percentageLabel)
        assertTrue(includeLeave.rows[1].isLowAttendance)

        val excludeLeave = ReportsRepository.buildReportSummary(
            allRecords = sampleRecords,
            leaveRule = LeaveRule.EXCLUDE_LEAVE,
            threshold = 75,
            query = ReportQuery(type = ReportType.STUDENT_WISE)
        )
        // CS-02: 1P, 0A, 1L -> 100.0% (not low when Leave excluded)
        assertEquals("100.0%", excludeLeave.rows[1].percentageLabel)
        assertFalse(excludeLeave.rows[1].isLowAttendance)
    }
}
