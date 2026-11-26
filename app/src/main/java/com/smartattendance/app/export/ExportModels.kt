package com.smartattendance.app.export

import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (.xlsx)"),
    PDF("pdf", "application/pdf", "PDF (.pdf)"),
    PNG("png", "image/png", "Image (.png)")
}

enum class ExportMode(val label: String) {
    SINGLE_LECTURE("Single Lecture"),
    FULL_REGISTER("Full Register"),
    REPORT("Report")
}

data class ExportMetadata(
    val instituteName: String,
    val logoPath: String?,
    val programName: String,
    val semesterLabel: String,
    val section: String,
    val subjectName: String,
    val teacherName: String,
    val dateRangeLabel: String,
    val leaveRule: LeaveRule,
    val generatedAtLabel: String
)

data class SingleLectureRow(
    val rollNumber: String,
    val studentName: String,
    val status: AttendanceStatus
)

data class SingleLectureExportData(
    val metadata: ExportMetadata,
    val lectureNumber: Int,
    val rows: List<SingleLectureRow>,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int
)

data class RegisterLectureColumn(
    val sessionId: Long,
    val date: String,
    val lectureNumber: Int
) {
    val shortLabel: String get() = "${date.substring(5)} L$lectureNumber"
}

data class RegisterStudentRow(
    val rollNumber: String,
    val studentName: String,
    val isArchived: Boolean,
    val statuses: List<AttendanceStatus?>,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val percentageLabel: String
)

data class FullRegisterExportData(
    val metadata: ExportMetadata,
    val columns: List<RegisterLectureColumn>,
    val rows: List<RegisterStudentRow>
)
