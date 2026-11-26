package com.smartattendance.app.export

import com.smartattendance.app.core.AttendanceMath
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream

object ExcelExporter {

    fun exportSingleLecture(data: SingleLectureExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { out ->
            Workbook(out, "SmartAttendance", "1.0").use { wb ->
                val ws = wb.newWorksheet("Lecture ${data.lectureNumber}")
                writeHeaderBlock(ws, data.metadata, "Single Lecture Attendance (L${data.lectureNumber})")

                val summaryRow = 4
                ws.value(summaryRow, 0, "Totals:")
                ws.value(summaryRow, 1, "Present: ${data.presentCount} | Absent: ${data.absentCount} | Leave: ${data.leaveCount}")

                val headerRow = 6
                ws.value(headerRow, 0, "Roll Number")
                ws.value(headerRow, 1, "Student Name")
                ws.value(headerRow, 2, "Status")
                ws.style(headerRow, 0).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()
                ws.style(headerRow, 1).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()
                ws.style(headerRow, 2).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()

                data.rows.forEachIndexed { idx, row ->
                    val r = headerRow + 1 + idx
                    ws.value(r, 0, AttendanceMath.sanitizeExcelCell(row.rollNumber))
                    ws.value(r, 1, AttendanceMath.sanitizeExcelCell(row.studentName))
                    ws.value(r, 2, row.status.code)
                }

                ws.width(0, 18.0)
                ws.width(1, 30.0)
                ws.width(2, 12.0)
                ws.freezePane(2, headerRow + 1)
            }
        }
        verifyNonEmpty(targetFile)
        return targetFile
    }

    fun exportFullRegister(data: FullRegisterExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { out ->
            Workbook(out, "SmartAttendance", "1.0").use { wb ->
                val ws = wb.newWorksheet("Full Register")
                writeHeaderBlock(ws, data.metadata, "Full Attendance Register")

                val headerRow = 5
                ws.value(headerRow, 0, "Roll Number")
                ws.value(headerRow, 1, "Student Name")
                ws.style(headerRow, 0).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()
                ws.style(headerRow, 1).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()

                data.columns.forEachIndexed { idx, col ->
                    val c = 2 + idx
                    ws.value(headerRow, c, col.shortLabel)
                    ws.style(headerRow, c).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()
                    ws.width(c, 11.0)
                }

                val summaryStartCol = 2 + data.columns.size
                val summaryHeaders = listOf("Present", "Absent", "Leave", "Attendance %")
                summaryHeaders.forEachIndexed { offset, title ->
                    val c = summaryStartCol + offset
                    ws.value(headerRow, c, title)
                    ws.style(headerRow, c).bold().fillColor("073572").fontColor("FFFFFF").set()
                    ws.width(c, 13.0)
                }

                data.rows.forEachIndexed { rIdx, row ->
                    val r = headerRow + 1 + rIdx
                    ws.value(r, 0, AttendanceMath.sanitizeExcelCell(row.rollNumber))
                    val displayName = if (row.isArchived) "${row.studentName} (Archived)" else row.studentName
                    ws.value(r, 1, AttendanceMath.sanitizeExcelCell(displayName))

                    row.statuses.forEachIndexed { cIdx, status ->
                        ws.value(r, 2 + cIdx, status?.code ?: "-")
                    }

                    ws.value(r, summaryStartCol, row.presentCount)
                    ws.value(r, summaryStartCol + 1, row.absentCount)
                    ws.value(r, summaryStartCol + 2, row.leaveCount)
                    ws.value(r, summaryStartCol + 3, row.percentageLabel)
                }

                ws.width(0, 18.0)
                ws.width(1, 30.0)
                ws.freezePane(2, headerRow + 1)
            }
        }
        verifyNonEmpty(targetFile)
        return targetFile
    }

    private fun writeHeaderBlock(
        ws: org.dhatim.fastexcel.Worksheet,
        meta: ExportMetadata,
        title: String
    ) {
        ws.value(0, 0, AttendanceMath.sanitizeExcelCell(meta.instituteName))
        ws.style(0, 0).bold().fontSize(14).set()
        ws.value(1, 0, title)
        ws.style(1, 0).bold().set()
        val sec = if (meta.section.isBlank()) "" else " (${meta.section})"
        ws.value(2, 0, AttendanceMath.sanitizeExcelCell("${meta.programName} • ${meta.semesterLabel}$sec • ${meta.subjectName}"))
        ws.value(3, 0, AttendanceMath.sanitizeExcelCell("Teacher: ${meta.teacherName} • ${meta.dateRangeLabel}"))
    }

    private fun verifyNonEmpty(file: File) {
        check(file.exists() && file.length() > 0L) {
            "Generated Excel file is empty or missing."
        }
    }

    fun exportReport(data: ReportExportData, targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { out ->
            Workbook(out, "SmartAttendance", "1.0").use { wb ->
                val ws = wb.newWorksheet("Report")
                ws.value(0, 0, AttendanceMath.sanitizeExcelCell(data.instituteName))
                ws.style(0, 0).bold().fontSize(14).set()
                ws.value(1, 0, AttendanceMath.sanitizeExcelCell(data.reportTitle))
                ws.style(1, 0).bold().set()
                ws.value(2, 0, "${data.dateRangeLabel} • ${data.activeRuleLabel} • Low < ${data.threshold}%")
                ws.value(3, 0, "Overall: P=${data.totalPresent}, A=${data.totalAbsent}, L=${data.totalLeave}, ${data.overallPercentageLabel}")

                val headerRow = 5
                listOf("Item", "Details", "Present", "Absent", "Leave", "Percentage", "Status").forEachIndexed { c, h ->
                    ws.value(headerRow, c, h)
                    ws.style(headerRow, c).bold().fillColor("0B4EA2").fontColor("FFFFFF").set()
                }

                data.rows.forEachIndexed { idx, row ->
                    val r = headerRow + 1 + idx
                    ws.value(r, 0, AttendanceMath.sanitizeExcelCell(row.primaryLabel))
                    ws.value(r, 1, AttendanceMath.sanitizeExcelCell(row.secondaryLabel))
                    ws.value(r, 2, row.presentCount)
                    ws.value(r, 3, row.absentCount)
                    ws.value(r, 4, row.leaveCount)
                    ws.value(r, 5, row.percentageLabel)
                    ws.value(r, 6, if (row.isLowAttendance) "LOW (<${data.threshold}%)" else "OK")
                }

                ws.width(0, 22.0)
                ws.width(1, 28.0)
                ws.width(2, 11.0)
                ws.width(3, 11.0)
                ws.width(4, 11.0)
                ws.width(5, 13.0)
                ws.width(6, 15.0)
                ws.freezePane(1, headerRow + 1)
            }
        }
        verifyNonEmpty(targetFile)
        return targetFile
    }
}
