package com.smartattendance.app

import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.dao.ReportRecordTuple
import com.smartattendance.app.data.repository.ReportQuery
import com.smartattendance.app.data.repository.ReportType
import com.smartattendance.app.data.repository.ReportsRepository
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.export.ExcelExporter
import com.smartattendance.app.export.ExportMetadata
import com.smartattendance.app.export.FullRegisterExportData
import com.smartattendance.app.export.RegisterLectureColumn
import com.smartattendance.app.export.RegisterStudentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PerformanceScaleTest {

    @Test
    fun fiveHundredStudentsAndHundredLectures_reportsAndExcelCompleteQuickly() {
        val studentCount = 500
        val lectureCount = 100

        val rolls = (1..studentCount).map { "CS-${it.toString().padStart(3, '0')}" }.shuffled()
        val sortedRolls = rolls.sortedWith(NaturalRollComparator)
        assertEquals("CS-001", sortedRolls.first())
        assertEquals("CS-500", sortedRolls.last())

        val records = ArrayList<ReportRecordTuple>(studentCount * lectureCount)
        for (l in 1..lectureCount) {
            val date = "2026-10-${((l - 1) % 28 + 1).toString().padStart(2, '0')}"
            for (s in 1..studentCount) {
                val status = when ((s + l) % 10) {
                    0 -> AttendanceStatus.ABSENT
                    1 -> AttendanceStatus.LEAVE
                    else -> AttendanceStatus.PRESENT
                }
                records.add(
                    ReportRecordTuple(
                        sessionId = l.toLong(),
                        date = date,
                        lectureNumber = l,
                        programId = 1L,
                        programName = "BSCS",
                        semesterId = 1L,
                        semesterNumber = 3,
                        section = "A",
                        sessionName = "2024-2028",
                        subjectId = 1L,
                        subjectName = "Algorithms",
                        studentId = s.toLong(),
                        rollNumber = sortedRolls[s - 1],
                        studentName = "Student $s",
                        status = status
                    )
                )
            }
        }

        val startMs = System.currentTimeMillis()
        val report = ReportsRepository.buildReportSummary(
            allRecords = records,
            leaveRule = LeaveRule.INCLUDE_LEAVE,
            threshold = 75,
            query = ReportQuery(type = ReportType.STUDENT_WISE)
        )
        assertEquals(studentCount, report.rows.size)

        val cols = (1..lectureCount).map { RegisterLectureColumn(it.toLong(), "2026-10-01", it) }
        val registerRows = (1..studentCount).map { s ->
            RegisterStudentRow(
                rollNumber = sortedRolls[s - 1],
                studentName = "Student $s",
                isArchived = false,
                statuses = List(lectureCount) { AttendanceStatus.PRESENT },
                presentCount = lectureCount,
                absentCount = 0,
                leaveCount = 0,
                percentageLabel = "100.0%"
            )
        }
        val tempFile = File.createTempFile("perf_500x100", ".xlsx")
        try {
            ExcelExporter.exportFullRegister(
                FullRegisterExportData(
                    metadata = ExportMetadata(
                        instituteName = "Perf Test Institute",
                        logoPath = null,
                        programName = "BSCS",
                        semesterLabel = "Sem 3",
                        section = "A",
                        subjectName = "Algorithms",
                        teacherName = "Dr. Sara",
                        dateRangeLabel = "100 Lectures",
                        leaveRule = LeaveRule.INCLUDE_LEAVE,
                        generatedAtLabel = "26 Nov 2026"
                    ),
                    columns = cols,
                    rows = registerRows
                ),
                tempFile
            )
            assertTrue(tempFile.exists() && tempFile.length() > 0L)
        } finally {
            tempFile.delete()
        }
        val elapsedMs = System.currentTimeMillis() - startMs
        assertTrue("Expected 500x100 processing under 15s, took ${elapsedMs}ms", elapsedMs < 15_000L)
    }
}
