package com.smartattendance.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartattendance.app.domain.model.AttendanceStatus

@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["normalized_name"], unique = true)
    ]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "semesters",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["program_id"]),
        Index(value = ["program_id", "semester_number", "section", "session"], unique = true)
    ]
)
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "program_id") val programId: Long,
    @ColumnInfo(name = "semester_number") val semesterNumber: Int,
    @ColumnInfo(name = "section") val section: String = "",
    @ColumnInfo(name = "session") val session: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["semester_id", "normalized_roll"], unique = true)
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "semester_id") val semesterId: Long,
    @ColumnInfo(name = "roll_number") val rollNumber: String,
    @ColumnInfo(name = "normalized_roll") val normalizedRoll: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["teacher_id"])
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "semester_id") val semesterId: Long,
    @ColumnInfo(name = "teacher_id") val teacherId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String? = null,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["subject_id"]),
        Index(value = ["date"]),
        Index(value = ["subject_id", "date", "lecture_number"], unique = true)
    ]
)
data class AttendanceSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "lecture_number") val lectureNumber: Int = 1,
    @ColumnInfo(name = "teacher_name_snapshot") val teacherNameSnapshot: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["student_id"]),
        Index(value = ["session_id", "student_id"], unique = true)
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "status") val status: AttendanceStatus
)

@Entity(
    tableName = "attendance_drafts",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subject_id", "date", "lecture_number"], unique = true)
    ]
)
data class AttendanceDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "lecture_number") val lectureNumber: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_draft_records",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceDraftEntity::class,
            parentColumns = ["id"],
            childColumns = ["draft_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["draft_id"]),
        Index(value = ["student_id"]),
        Index(value = ["draft_id", "student_id"], unique = true)
    ]
)
data class AttendanceDraftRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "draft_id") val draftId: Long,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "status") val status: AttendanceStatus
)

@Entity(tableName = "setup_drafts")
data class SetupDraftEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "export_history",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["semester_id", "subject_id"])
    ]
)
data class ExportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "uri_string") val uriString: String,
    @ColumnInfo(name = "is_tree_uri") val isTreeUri: Boolean = false,
    @ColumnInfo(name = "format") val format: String,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "program_name") val programName: String,
    @ColumnInfo(name = "semester_label") val semesterLabel: String,
    @ColumnInfo(name = "subject_name") val subjectName: String,
    @ColumnInfo(name = "program_id") val programId: Long? = null,
    @ColumnInfo(name = "semester_id") val semesterId: Long? = null,
    @ColumnInfo(name = "subject_id") val subjectId: Long? = null,
    @ColumnInfo(name = "session_id") val sessionId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
