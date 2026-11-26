package com.smartattendance.app.backup

import android.content.Context
import androidx.room.withTransaction
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.entity.AttendanceRecordEntity
import com.smartattendance.app.data.local.db.entity.AttendanceSessionEntity
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SmartAttendanceDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        const val BACKUP_VERSION = 1
        private const val ENTRY_MANIFEST = "manifest.json"
        private const val ENTRY_DATABASE = "database.json"
        private const val ENTRY_SETTINGS = "settings.json"
        private const val ENTRY_LOGO = "logo.png"
    }

    suspend fun createBackupZip(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsDataStore.settingsFlow.first()
            val manifest = JSONObject().apply {
                put("backupVersion", BACKUP_VERSION)
                put("createdAt", System.currentTimeMillis())
            }
            val dbJson = exportDatabaseToJson()
            val settingsJson = JSONObject().apply {
                put("instituteName", settings.instituteName)
                put("themeMode", settings.themeMode.name)
                put("attendanceThreshold", settings.attendanceThreshold)
                put("leaveRule", settings.leaveRule.name)
                // PIN hash/salt intentionally excluded from backups
            }

            ZipOutputStream(outputStream).use { zos ->
                writeZipEntry(zos, ENTRY_MANIFEST, manifest.toString().toByteArray(StandardCharsets.UTF_8))
                writeZipEntry(zos, ENTRY_DATABASE, dbJson.toString().toByteArray(StandardCharsets.UTF_8))
                writeZipEntry(zos, ENTRY_SETTINGS, settingsJson.toString().toByteArray(StandardCharsets.UTF_8))

                val logoFile = settings.logoPath?.let { File(it) }?.takeIf { it.exists() }
                if (logoFile != null) {
                    writeZipEntry(zos, ENTRY_LOGO, logoFile.readBytes())
                }
            }
            settingsDataStore.updateLastBackupTimestamp()
        }
    }

    suspend fun restoreBackupZip(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val incomingBytes = inputStream.readBytes()
            val parsed = parseAndValidateZip(ByteArrayInputStream(incomingBytes))

            // Create automatic local safety backup before touching anything
            val safetyFile = File(context.filesDir, "backups/safety_backup.zip").apply {
                parentFile?.mkdirs()
            }
            FileOutputStream(safetyFile).use { fos ->
                createBackupZip(fos).getOrThrow()
            }

            try {
                applyParsedBackup(parsed)
            } catch (restoreErr: Exception) {
                // Roll back to safety backup on any failure
                runCatching {
                    FileInputStream(safetyFile).use { fis ->
                        val safetyParsed = parseAndValidateZip(fis)
                        applyParsedBackup(safetyParsed)
                    }
                }
                throw restoreErr
            }
        }
    }

    private data class ParsedBackup(
        val dbJson: JSONObject,
        val settingsJson: JSONObject,
        val logoBytes: ByteArray?
    )

    private fun parseAndValidateZip(inputStream: InputStream): ParsedBackup {
        var manifestRaw: String? = null
        var dbRaw: String? = null
        var settingsRaw: String? = null
        var logoBytes: ByteArray? = null

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val bytes = zis.readBytes()
                when (entry.name) {
                    ENTRY_MANIFEST -> manifestRaw = String(bytes, StandardCharsets.UTF_8)
                    ENTRY_DATABASE -> dbRaw = String(bytes, StandardCharsets.UTF_8)
                    ENTRY_SETTINGS -> settingsRaw = String(bytes, StandardCharsets.UTF_8)
                    ENTRY_LOGO -> logoBytes = bytes
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val manifest = JSONObject(manifestRaw ?: throw IllegalArgumentException("Invalid backup: missing manifest.json"))
        val version = manifest.optInt("backupVersion", -1)
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }

        val dbJson = JSONObject(dbRaw ?: throw IllegalArgumentException("Invalid backup: missing database.json"))
        val requiredTables = listOf("programs", "semesters", "students", "teachers", "subjects", "attendance_sessions", "attendance_records")
        requiredTables.forEach { table ->
            require(dbJson.has(table)) { "Corrupted backup: missing table '$table'." }
        }
        val settingsJson = JSONObject(settingsRaw ?: "{}")
        return ParsedBackup(dbJson, settingsJson, logoBytes)
    }

    private suspend fun applyParsedBackup(parsed: ParsedBackup) {
        val dbJson = parsed.dbJson
        db.withTransaction {
            db.clearAllTables()
            val writable = db.openHelper.writableDatabase

            insertArray(dbJson.getJSONArray("programs")) { o ->
                writable.execSQL(
                    "INSERT INTO programs (id, name, normalized_name, is_archived, created_at) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getString("name"), o.getString("normalized_name"), o.getInt("is_archived"), o.getLong("created_at"))
                )
            }
            insertArray(dbJson.getJSONArray("semesters")) { o ->
                writable.execSQL(
                    "INSERT INTO semesters (id, program_id, semester_number, section, session, is_archived, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("program_id"), o.getInt("semester_number"), o.getString("section"), o.getString("session"), o.getInt("is_archived"), o.getLong("created_at"))
                )
            }
            insertArray(dbJson.getJSONArray("students")) { o ->
                writable.execSQL(
                    "INSERT INTO students (id, semester_id, roll_number, normalized_roll, name, is_archived, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("semester_id"), o.getString("roll_number"), o.getString("normalized_roll"), o.getString("name"), o.getInt("is_archived"), o.getLong("created_at"))
                )
            }
            insertArray(dbJson.getJSONArray("teachers")) { o ->
                writable.execSQL(
                    "INSERT INTO teachers (id, name, phone, email, is_archived, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getString("name"), o.optString("phone").ifEmpty { null }, o.optString("email").ifEmpty { null }, o.getInt("is_archived"), o.getLong("created_at"))
                )
            }
            insertArray(dbJson.getJSONArray("subjects")) { o ->
                writable.execSQL(
                    "INSERT INTO subjects (id, semester_id, teacher_id, name, code, is_archived, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("semester_id"), o.getLong("teacher_id"), o.getString("name"), o.optString("code").ifEmpty { null }, o.getInt("is_archived"), o.getLong("created_at"))
                )
            }
            insertArray(dbJson.getJSONArray("attendance_sessions")) { o ->
                writable.execSQL(
                    "INSERT INTO attendance_sessions (id, subject_id, date, lecture_number, teacher_name_snapshot, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("subject_id"), o.getString("date"), o.getInt("lecture_number"), o.getString("teacher_name_snapshot"), o.getLong("created_at"), o.getLong("updated_at"))
                )
            }
            insertArray(dbJson.getJSONArray("attendance_records")) { o ->
                writable.execSQL(
                    "INSERT INTO attendance_records (id, session_id, student_id, status) VALUES (?, ?, ?, ?)",
                    arrayOf(o.getLong("id"), o.getLong("session_id"), o.getLong("student_id"), o.getString("status"))
                )
            }
        }

        val s = parsed.settingsJson
        if (s.has("instituteName")) settingsDataStore.updateInstituteName(s.getString("instituteName"))
        if (s.has("themeMode")) runCatching { ThemeMode.valueOf(s.getString("themeMode")) }.getOrNull()?.let { settingsDataStore.updateThemeMode(it) }
        if (s.has("attendanceThreshold")) settingsDataStore.updateAttendanceThreshold(s.getInt("attendanceThreshold"))
        if (s.has("leaveRule")) runCatching { LeaveRule.valueOf(s.getString("leaveRule")) }.getOrNull()?.let { settingsDataStore.updateLeaveRule(it) }

        val logoTarget = File(context.filesDir, "institute_logo.png")
        if (parsed.logoBytes != null) {
            logoTarget.writeBytes(parsed.logoBytes)
            settingsDataStore.updateLogoPath(logoTarget.absolutePath)
        } else {
            if (logoTarget.exists()) logoTarget.delete()
            settingsDataStore.updateLogoPath(null)
        }
    }

    private fun exportDatabaseToJson(): JSONObject {
        val readable = db.openHelper.readableDatabase
        val root = JSONObject()
        val tables = listOf(
            "programs",
            "semesters",
            "students",
            "teachers",
            "subjects",
            "attendance_sessions",
            "attendance_records"
        )
        tables.forEach { table ->
            val arr = JSONArray()
            readable.query("SELECT * FROM $table").use { cursor ->
                val cols = cursor.columnNames
                while (cursor.moveToNext()) {
                    val obj = JSONObject()
                    cols.forEachIndexed { idx, col ->
                        when (cursor.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> obj.put(col, JSONObject.NULL)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> obj.put(col, cursor.getLong(idx))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> obj.put(col, cursor.getDouble(idx))
                            android.database.Cursor.FIELD_TYPE_STRING -> obj.put(col, cursor.getString(idx))
                        }
                    }
                    arr.put(obj)
                }
            }
            root.put(table, arr)
        }
        return root
    }

    private inline fun insertArray(arr: JSONArray, block: (JSONObject) -> Unit) {
        for (i in 0 until arr.length()) {
            block(arr.getJSONObject(i))
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(data)
        zos.closeEntry()
    }
}
