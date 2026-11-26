package com.smartattendance.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.smartattendance.app.core.AttendanceMath
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.ExportHistoryDao
import com.smartattendance.app.data.local.db.dao.ReportRecordTuple
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.export.ExcelExporter
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.export.ExportMode
import com.smartattendance.app.export.PdfExporter
import com.smartattendance.app.export.PngExporter
import com.smartattendance.app.export.ReportExportData
import com.smartattendance.app.export.ReportExportRow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ReportType(val label: String) {
    DAILY("Daily"),
    MONTHLY("Monthly"),
    CUSTOM_RANGE("Custom Range"),
    SUBJECT_WISE("Subject-wise"),
    STUDENT_WISE("Student-wise"),
    FULL_SEMESTER("Full Semester")
}

data class ReportQuery(
    val type: ReportType = ReportType.DAILY,
    val dailyDate: String = DateUtils.todayIso(),
    val monthPrefix: String = DateUtils.todayIso().substring(0, 7),
    val startDate: String = DateUtils.todayIso().substring(0, 7) + "-01",
    val endDate: String = DateUtils.todayIso(),
    val programName: String? = null,
    val semesterLabel: String? = null
)

data class ReportRow(
    val primaryLabel: String,
    val secondaryLabel: String,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val percentageValue: Double?,
    val percentageLabel: String,
    val isLowAttendance: Boolean
)

data class ReportSummary(
    val type: ReportType,
    val dateRangeLabel: String,
    val activeRuleLabel: String,
    val threshold: Int,
    val rows: List<ReportRow>,
    val totalPresent: Int,
    val totalAbsent: Int,
    val totalLeave: Int,
    val overallPercentageLabel: String,
    val availablePrograms: List<String>,
    val availableSemesters: List<String>
)

@Singleton
class ReportsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val exportHistoryDao: ExportHistoryDao,
    private val settingsDataStore: SettingsDataStore
) {
    fun observeReport(queryFlow: Flow<ReportQuery>): Flow<ReportSummary> = combine(
        attendanceDao.observeAllReportRecords(),
        settingsDataStore.settingsFlow,
        queryFlow
    ) { allRecords, settings, query ->
        buildReportSummary(allRecords, settings.leaveRule, settings.attendanceThreshold, query)
    }

    fun observeExportHistory(): Flow<List<ExportHistoryEntity>> = exportHistoryDao.observeAll()

    suspend fun removeExportHistoryEntry(id: Long) {
        exportHistoryDao.deleteById(id)
    }

    fun isExportUriAccessible(uriString: String, isTreeUri: Boolean): Boolean {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        return try {
            if (isTreeUri) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                context.contentResolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use {
                    it.moveToFirst()
                } ?: false
            } else {
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun generateReportBundle(
        summary: ReportSummary,
        format: ExportFormat
    ): Result<GeneratedExportBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsDataStore.settingsFlow.first()
            val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = System.currentTimeMillis()
            val baseName = AttendanceMath.sanitizeFileName("Report_${summary.type.name}_${summary.dateRangeLabel}")
            val exportData = ReportExportData(
                instituteName = settings.instituteName,
                logoPath = settings.logoPath,
                reportTitle = "${summary.type.label} Attendance Report",
                dateRangeLabel = summary.dateRangeLabel,
                activeRuleLabel = summary.activeRuleLabel,
                threshold = summary.threshold,
                totalPresent = summary.totalPresent,
                totalAbsent = summary.totalAbsent,
                totalLeave = summary.totalLeave,
                overallPercentageLabel = summary.overallPercentageLabel,
                rows = summary.rows.map {
                    ReportExportRow(
                        primaryLabel = it.primaryLabel,
                        secondaryLabel = it.secondaryLabel,
                        presentCount = it.presentCount,
                        absentCount = it.absentCount,
                        leaveCount = it.leaveCount,
                        percentageLabel = it.percentageLabel,
                        isLowAttendance = it.isLowAttendance
                    )
                }
            )
            val files = when (format) {
                ExportFormat.XLSX -> listOf(ExcelExporter.exportReport(exportData, File(tempDir, "$baseName.xlsx")))
                ExportFormat.PDF -> listOf(PdfExporter.exportReport(exportData, File(tempDir, "$baseName.pdf")))
                ExportFormat.PNG -> PngExporter.exportReportPages(exportData, File(tempDir, "${baseName}_$stamp"))
            }
            GeneratedExportBundle(
                suggestedName = if (format == ExportFormat.PNG) baseName else "$baseName.${format.extension}",
                format = format,
                mode = ExportMode.REPORT,
                files = files,
                programName = summary.type.label,
                semesterLabel = summary.dateRangeLabel,
                subjectName = "Report",
                subjectId = 0L,
                sessionId = null
            )
        }
    }

    companion object {
        fun buildReportSummary(
            allRecords: List<ReportRecordTuple>,
            leaveRule: LeaveRule,
            threshold: Int,
            query: ReportQuery
        ): ReportSummary {
            val availablePrograms = allRecords.map { it.programName }.distinct().sorted()
            val availableSemesters = allRecords.map { semesterLabel(it) }.distinct().sorted()

            val scoped = allRecords.filter { rec ->
                val progOk = query.programName == null || rec.programName == query.programName
                val semOk = query.semesterLabel == null || semesterLabel(rec) == query.semesterLabel
                val dateOk = when (query.type) {
                    ReportType.DAILY -> rec.date == query.dailyDate
                    ReportType.MONTHLY -> rec.date.startsWith(query.monthPrefix)
                    ReportType.CUSTOM_RANGE -> rec.date >= query.startDate && rec.date <= query.endDate
                    else -> true
                }
                progOk && semOk && dateOk
            }

            val rows: List<ReportRow> = when (query.type) {
                ReportType.DAILY, ReportType.MONTHLY, ReportType.CUSTOM_RANGE -> {
                    scoped.groupBy { "${it.subjectId}_${it.date}_${it.lectureNumber}" }
                        .values
                        .sortedByDescending { it.first().date }
                        .map { group ->
                            val first = group.first()
                            toReportRow(
                                primary = "${first.subjectName} (L${first.lectureNumber})",
                                secondary = "${first.programName} • Sem ${first.semesterNumber} • ${DateUtils.formatDisplayDate(first.date)}",
                                records = group,
                                leaveRule = leaveRule,
                                threshold = threshold
                            )
                        }
                }
                ReportType.SUBJECT_WISE -> {
                    scoped.groupBy { it.subjectId }
                        .values
                        .sortedBy { it.first().subjectName }
                        .map { group ->
                            val first = group.first()
                            toReportRow(
                                primary = first.subjectName,
                                secondary = "${first.programName} • ${semesterLabel(first)}",
                                records = group,
                                leaveRule = leaveRule,
                                threshold = threshold
                            )
                        }
                }
                ReportType.STUDENT_WISE -> {
                    scoped.groupBy { it.studentId }
                        .values
                        .sortedWith { a, b -> NaturalRollComparator.compare(a.first().rollNumber, b.first().rollNumber) }
                        .map { group ->
                            val first = group.first()
                            toReportRow(
                                primary = "${first.rollNumber} — ${first.studentName}",
                                secondary = "${first.programName} • ${semesterLabel(first)}",
                                records = group,
                                leaveRule = leaveRule,
                                threshold = threshold
                            )
                        }
                }
                ReportType.FULL_SEMESTER -> {
                    scoped.groupBy { it.semesterId }
                        .values
                        .sortedBy { it.first().semesterNumber }
                        .map { group ->
                            val first = group.first()
                            toReportRow(
                                primary = "${first.programName} — ${semesterLabel(first)}",
                                secondary = "Session ${first.sessionName}",
                                records = group,
                                leaveRule = leaveRule,
                                threshold = threshold
                            )
                        }
                }
            }

            val totalP = scoped.count { it.status == AttendanceStatus.PRESENT }
            val totalA = scoped.count { it.status == AttendanceStatus.ABSENT }
            val totalL = scoped.count { it.status == AttendanceStatus.LEAVE }
            val overallPct = AttendanceMath.formatPercentage(totalP, totalA, totalL, leaveRule)

            val dateLabel = when (query.type) {
                ReportType.DAILY -> DateUtils.formatDisplayDate(query.dailyDate)
                ReportType.MONTHLY -> query.monthPrefix
                ReportType.CUSTOM_RANGE -> "${DateUtils.formatDisplayDate(query.startDate)} to ${DateUtils.formatDisplayDate(query.endDate)}"
                else -> "All Saved Dates"
            }

            val ruleLabel = when (leaveRule) {
                LeaveRule.INCLUDE_LEAVE -> "Rule: P / (P + A + L)"
                LeaveRule.EXCLUDE_LEAVE -> "Rule: P / (P + A) [Leave Excluded]"
            }

            return ReportSummary(
                type = query.type,
                dateRangeLabel = dateLabel,
                activeRuleLabel = ruleLabel,
                threshold = threshold,
                rows = rows,
                totalPresent = totalP,
                totalAbsent = totalA,
                totalLeave = totalL,
                overallPercentageLabel = overallPct,
                availablePrograms = availablePrograms,
                availableSemesters = availableSemesters
            )
        }

        private fun semesterLabel(rec: ReportRecordTuple): String {
            val sec = if (rec.section.isBlank()) "" else " (${rec.section})"
            return "Sem ${rec.semesterNumber}$sec"
        }

        private fun toReportRow(
            primary: String,
            secondary: String,
            records: List<ReportRecordTuple>,
            leaveRule: LeaveRule,
            threshold: Int
        ): ReportRow {
            val p = records.count { it.status == AttendanceStatus.PRESENT }
            val a = records.count { it.status == AttendanceStatus.ABSENT }
            val l = records.count { it.status == AttendanceStatus.LEAVE }
            val pctVal = AttendanceMath.calculatePercentageValue(p, a, l, leaveRule)
            val pctLabel = AttendanceMath.formatPercentage(p, a, l, leaveRule)
            val isLow = pctVal != null && pctVal < threshold
            return ReportRow(
                primaryLabel = primary,
                secondaryLabel = secondary,
                presentCount = p,
                absentCount = a,
                leaveCount = l,
                percentageValue = pctVal,
                percentageLabel = pctLabel,
                isLowAttendance = isLow
            )
        }
    }
}
