package com.smartattendance.app

import com.smartattendance.app.core.AttendanceMath
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.export.ExcelExporter
import com.smartattendance.app.export.ExportMetadata
import com.smartattendance.app.export.FullRegisterExportData
import com.smartattendance.app.export.RegisterLectureColumn
import com.smartattendance.app.export.RegisterStudentRow
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ExportMathAndExcelTest {

    @Test
    fun sanitizeExcelCell_escapesFormulaTriggers() {
        assertEquals("'=SUM(A1:A2)", AttendanceMath.sanitizeExcelCell("=SUM(A1:A2)"))
        assertEquals("'+123", AttendanceMath.sanitizeExcelCell("+123"))
        assertEquals("'-123", AttendanceMath.sanitizeExcelCell("-123"))
        assertEquals("'@cmd", AttendanceMath.sanitizeExcelCell("@cmd"))
        assertEquals("'\tTab", AttendanceMath.sanitizeExcelCell("\tTab"))
        assertEquals("'\nNewline", AttendanceMath.sanitizeExcelCell("\nNewline"))
        assertEquals("CS-01", AttendanceMath.sanitizeExcelCell("CS-01"))
    }

    @Test
    fun calculatePercentage_handlesBothLeaveRulesAndZeroDenominator() {
        assertEquals("N/A", AttendanceMath.formatPercentage(0, 0, 0, LeaveRule.INCLUDE_LEAVE))
        assertEquals("N/A", AttendanceMath.formatPercentage(0, 0, 2, LeaveRule.EXCLUDE_LEAVE))
        // 8 P, 1 A, 1 L -> 8/10 = 80.0% (include leave), 8/9 = 88.9% (exclude leave)
        assertEquals("80.0%", AttendanceMath.formatPercentage(8, 1, 1, LeaveRule.INCLUDE_LEAVE))
        assertEquals("88.9%", AttendanceMath.formatPercentage(8, 1, 1, LeaveRule.EXCLUDE_LEAVE))
    }

    @Test
    fun sanitizeFileName_removesUnsafeCharacters() {
        assertEquals(
            "BSCS_Sem3_Math_2026-11-26_L1.xlsx",
            AttendanceMath.sanitizeFileName("BSCS/Sem3:Math 2026-11-26*L1") + ".xlsx"
        )
    }

    @Test
    fun excelExporter_writesFullRegisterWithMissingEarlierLectureDash() {
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.woodstox.stax.WstxInputFactory")
        val tempFile = File.createTempFile("register_test", ".xlsx")
        try {
            val data = FullRegisterExportData(
                metadata = ExportMetadata(
                    instituteName = "Test Institute",
                    logoPath = null,
                    programName = "BSCS",
                    semesterLabel = "Sem 3",
                    section = "A",
                    subjectName = "Algorithms",
                    teacherName = "Dr. Sara",
                    dateRangeLabel = "25 Nov - 26 Nov",
                    leaveRule = LeaveRule.INCLUDE_LEAVE,
                    generatedAtLabel = "26 Nov 2026"
                ),
                columns = listOf(
                    RegisterLectureColumn(1L, "2026-11-25", 1),
                    RegisterLectureColumn(2L, "2026-11-26", 1)
                ),
                rows = listOf(
                    RegisterStudentRow(
                        rollNumber = "=CS-01",
                        studentName = "Ayesha",
                        isArchived = false,
                        statuses = listOf(null, AttendanceStatus.PRESENT),
                        presentCount = 1,
                        absentCount = 0,
                        leaveCount = 0,
                        percentageLabel = "100.0%"
                    )
                )
            )
            ExcelExporter.exportFullRegister(data, tempFile)
            assertTrue(tempFile.exists() && tempFile.length() > 0)

            FileInputStream(tempFile).use { fis ->
                ReadableWorkbook(fis).use { wb ->
                    val rows = wb.firstSheet.read()
                    val dataRow = rows[6]
                    assertEquals("'=CS-01", dataRow.getCellText(0))
                    assertEquals("Ayesha", dataRow.getCellText(1))
                    assertEquals("-", dataRow.getCellText(2))
                    assertEquals("P", dataRow.getCellText(3))
                }
            }
        } finally {
            tempFile.delete()
        }
    }
}
