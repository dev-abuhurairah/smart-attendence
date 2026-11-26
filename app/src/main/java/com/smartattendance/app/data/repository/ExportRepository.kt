package com.smartattendance.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.smartattendance.app.core.AttendanceMath
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.data.local.db.dao.AttendanceDao
import com.smartattendance.app.data.local.db.dao.ExportHistoryDao
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.export.ExcelExporter
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.export.ExportMetadata
import com.smartattendance.app.export.ExportMode
import com.smartattendance.app.export.FullRegisterExportData
import com.smartattendance.app.export.PdfExporter
import com.smartattendance.app.export.PngExporter
import com.smartattendance.app.export.RegisterLectureColumn
import com.smartattendance.app.export.RegisterStudentRow
import com.smartattendance.app.export.SingleLectureExportData
import com.smartattendance.app.export.SingleLectureRow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedExportBundle(
    val suggestedName: String,
    val format: ExportFormat,
    val mode: ExportMode,
    val files: List<File>,
    val programName: String,
    val semesterLabel: String,
    val subjectName: String,
    val subjectId: Long,
    val sessionId: Long?
)

sealed interface ExportWriteResult {
    data class Success(val displayName: String, val uri: Uri) : ExportWriteResult
    data class NeedsNewLocation(val reason: String, val bundle: GeneratedExportBundle) : ExportWriteResult
    data class Failure(val error: String, val bundle: GeneratedExportBundle?) : ExportWriteResult
}

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao,
    private val subjectDao: SubjectDao,
    private val exportHistoryDao: ExportHistoryDao,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun findLatestRegisterExport(subjectId: Long, format: ExportFormat): ExportHistoryEntity? =
        exportHistoryDao.findLatestForSubject(subjectId, ExportMode.FULL_REGISTER.name, format.name)

    suspend fun generateToTemp(
        sessionId: Long?,
        subjectId: Long,
        mode: ExportMode,
        format: ExportFormat
    ): Result<GeneratedExportBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsDataStore.settingsFlow.first()
            val header = subjectDao.getSessionHeader(subjectId)
                ?: throw IllegalStateException("Subject not found.")
            val subject = subjectDao.getById(subjectId)
                ?: throw IllegalStateException("Subject record missing.")

            val tempDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = System.currentTimeMillis()
            val semShort = "Sem${header.semesterNumber}" + if (header.section.isNotBlank()) "_${header.section}" else ""

            when (mode) {
                ExportMode.SINGLE_LECTURE -> {
                    val sid = sessionId ?: throw IllegalStateException("Session ID required for single lecture export.")
                    val summary = attendanceDao.observeSheetSummary(sid).first()
                        ?: throw IllegalStateException("Attendance sheet not found.")
                    val records = attendanceDao.observeSheetRecords(sid).first()
                        .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }

                    val meta = ExportMetadata(
                        instituteName = settings.instituteName,
                        logoPath = settings.logoPath,
                        programName = header.programName,
                        semesterLabel = "Sem ${header.semesterNumber}",
                        section = header.section,
                        subjectName = header.subjectName,
                        teacherName = summary.teacherNameSnapshot,
                        dateRangeLabel = "${DateUtils.formatDisplayDate(summary.date)} (L${summary.lectureNumber})",
                        leaveRule = settings.leaveRule,
                        generatedAtLabel = DateUtils.formatDateTime(stamp)
                    )
                    val data = SingleLectureExportData(
                        metadata = meta,
                        lectureNumber = summary.lectureNumber,
                        rows = records.map { SingleLectureRow(it.rollNumber, it.studentName, it.status) },
                        presentCount = summary.presentCount,
                        absentCount = summary.absentCount,
                        leaveCount = summary.leaveCount
                    )
                    val baseName = AttendanceMath.sanitizeFileName(
                        "${header.programName}_${semShort}_${header.subjectName}_${summary.date}_L${summary.lectureNumber}"
                    )
                    val files = when (format) {
                        ExportFormat.XLSX -> listOf(ExcelExporter.exportSingleLecture(data, File(tempDir, "$baseName.xlsx")))
                        ExportFormat.PDF -> listOf(PdfExporter.exportSingleLecture(data, File(tempDir, "$baseName.pdf")))
                        ExportFormat.PNG -> PngExporter.exportSingleLecturePages(data, File(tempDir, "${baseName}_$stamp"))
                    }
                    GeneratedExportBundle(
                        suggestedName = if (format == ExportFormat.PNG) baseName else "$baseName.${format.extension}",
                        format = format,
                        mode = mode,
                        files = files,
                        programName = header.programName,
                        semesterLabel = "Sem ${header.semesterNumber}",
                        subjectName = header.subjectName,
                        subjectId = subjectId,
                        sessionId = sid
                    )
                }

                ExportMode.FULL_REGISTER, ExportMode.REPORT -> {
                    val sessions = attendanceDao.getSessionsForSubjectChronological(subjectId)
                    val allRecords = attendanceDao.getAllRecordsForSubject(subjectId)
                    val recordLookup = allRecords.associateBy { it.sessionId to it.studentId }
                    val recordedStudentIds = allRecords.map { it.studentId }.toSet()

                    val students = studentDao.getAllBySemesterIncludingArchived(subject.semesterId)
                        .filter { !it.isArchived || it.id in recordedStudentIds }
                        .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }

                    val dateRange = if (sessions.isEmpty()) {
                        "No lectures yet"
                    } else {
                        "${DateUtils.formatDisplayDate(sessions.first().date)} - ${DateUtils.formatDisplayDate(sessions.last().date)}"
                    }

                    val meta = ExportMetadata(
                        instituteName = settings.instituteName,
                        logoPath = settings.logoPath,
                        programName = header.programName,
                        semesterLabel = "Sem ${header.semesterNumber}",
                        section = header.section,
                        subjectName = header.subjectName,
                        teacherName = header.teacherName,
                        dateRangeLabel = dateRange,
                        leaveRule = settings.leaveRule,
                        generatedAtLabel = DateUtils.formatDateTime(stamp)
                    )
                    val columns = sessions.map { RegisterLectureColumn(it.id, it.date, it.lectureNumber) }
                    val rows = students.map { st ->
                        val statuses = columns.map { col -> recordLookup[col.sessionId to st.id]?.status }
                        val p = statuses.count { it == AttendanceStatus.PRESENT }
                        val a = statuses.count { it == AttendanceStatus.ABSENT }
                        val l = statuses.count { it == AttendanceStatus.LEAVE }
                        val pct = AttendanceMath.formatPercentage(p, a, l, settings.leaveRule)
                        RegisterStudentRow(
                            rollNumber = st.rollNumber,
                            studentName = st.name,
                            isArchived = st.isArchived,
                            statuses = statuses,
                            presentCount = p,
                            absentCount = a,
                            leaveCount = l,
                            percentageLabel = pct
                        )
                    }
                    val data = FullRegisterExportData(metadata = meta, columns = columns, rows = rows)
                    val baseName = AttendanceMath.sanitizeFileName(
                        "${header.programName}_${semShort}_${header.subjectName}_Register"
                    )
                    val files = when (format) {
                        ExportFormat.XLSX -> listOf(ExcelExporter.exportFullRegister(data, File(tempDir, "$baseName.xlsx")))
                        ExportFormat.PDF -> listOf(PdfExporter.exportFullRegister(data, File(tempDir, "$baseName.pdf")))
                        ExportFormat.PNG -> PngExporter.exportFullRegisterPages(data, File(tempDir, "${baseName}_$stamp"))
                    }
                    GeneratedExportBundle(
                        suggestedName = if (format == ExportFormat.PNG) baseName else "$baseName.${format.extension}",
                        format = format,
                        mode = mode,
                        files = files,
                        programName = header.programName,
                        semesterLabel = "Sem ${header.semesterNumber}",
                        subjectName = header.subjectName,
                        subjectId = subjectId,
                        sessionId = sessionId
                    )
                }
            }
        }
    }

    suspend fun writeBundleToFileUri(
        bundle: GeneratedExportBundle,
        targetUri: Uri,
        takePersistablePermission: Boolean = true
    ): ExportWriteResult = withContext(Dispatchers.IO) {
        try {
            if (takePersistablePermission) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        targetUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            val sourceFile = bundle.files.firstOrNull()
                ?: return@withContext ExportWriteResult.Failure("No generated file found.", bundle)

            val out = context.contentResolver.openOutputStream(targetUri, "wt")
                ?: return@withContext ExportWriteResult.NeedsNewLocation(
                    "File was moved, deleted, or lost permission. Please choose a new location.",
                    bundle
                )
            out.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }

            exportHistoryDao.insert(
                ExportHistoryEntity(
                    fileName = bundle.suggestedName,
                    uriString = targetUri.toString(),
                    isTreeUri = false,
                    format = bundle.format.name,
                    mode = bundle.mode.name,
                    programName = bundle.programName,
                    semesterLabel = bundle.semesterLabel,
                    subjectName = bundle.subjectName,
                    subjectId = bundle.subjectId,
                    sessionId = bundle.sessionId
                )
            )
            ExportWriteResult.Success(bundle.suggestedName, targetUri)
        } catch (e: SecurityException) {
            ExportWriteResult.NeedsNewLocation(
                "Permission to update this file was lost. Please pick a new location.",
                bundle
            )
        } catch (e: Exception) {
            ExportWriteResult.Failure(
                "Save failed: ${e.message ?: "Unknown error"}. Temp copy preserved.",
                bundle
            )
        }
    }

    suspend fun writePngBundleToTreeUri(
        bundle: GeneratedExportBundle,
        treeUri: Uri,
        takePersistablePermission: Boolean = true
    ): ExportWriteResult = withContext(Dispatchers.IO) {
        try {
            if (takePersistablePermission) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }

            val resolver = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)

            // Clean up any stale page_*.png files from older exports in this folder
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    if (name.startsWith("page_") && name.endsWith(".png")) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        runCatching { DocumentsContract.deleteDocument(resolver, docUri) }
                    }
                }
            }

            bundle.files.forEach { file ->
                val createdUri = DocumentsContract.createDocument(
                    resolver,
                    parentDocUri,
                    "image/png",
                    file.name
                ) ?: throw IllegalStateException("Could not create ${file.name} in selected folder.")

                resolver.openOutputStream(createdUri, "wt")?.use { out ->
                    FileInputStream(file).use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Could not write ${file.name}.")
            }

            exportHistoryDao.insert(
                ExportHistoryEntity(
                    fileName = "${bundle.suggestedName} (${bundle.files.size} PNG pages)",
                    uriString = treeUri.toString(),
                    isTreeUri = true,
                    format = ExportFormat.PNG.name,
                    mode = bundle.mode.name,
                    programName = bundle.programName,
                    semesterLabel = bundle.semesterLabel,
                    subjectName = bundle.subjectName,
                    subjectId = bundle.subjectId,
                    sessionId = bundle.sessionId
                )
            )
            ExportWriteResult.Success("${bundle.files.size} PNG page(s)", treeUri)
        } catch (e: SecurityException) {
            ExportWriteResult.NeedsNewLocation(
                "Folder permission was lost. Please choose a folder again.",
                bundle
            )
        } catch (e: Exception) {
            ExportWriteResult.Failure(
                "PNG export failed: ${e.message ?: "Unknown error"}. Temp files preserved.",
                bundle
            )
        }
    }

    fun buildShareIntent(bundle: GeneratedExportBundle): Intent {
        val uris = ArrayList(
            bundle.files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        )
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = bundle.format.mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = bundle.format.mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
