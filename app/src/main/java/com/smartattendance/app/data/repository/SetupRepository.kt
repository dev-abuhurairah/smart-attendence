package com.smartattendance.app.data.repository

import androidx.room.withTransaction
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.core.normalizeProgramName
import com.smartattendance.app.core.normalizeRollNumber
import com.smartattendance.app.data.local.db.SmartAttendanceDatabase
import com.smartattendance.app.data.local.db.dao.DraftDao
import com.smartattendance.app.data.local.db.dao.ProgramDao
import com.smartattendance.app.data.local.db.dao.SemesterDao
import com.smartattendance.app.data.local.db.dao.StudentDao
import com.smartattendance.app.data.local.db.dao.SubjectDao
import com.smartattendance.app.data.local.db.dao.TeacherDao
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.SemesterEntity
import com.smartattendance.app.data.local.db.entity.SetupDraftEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.SubjectEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class DraftStudent(
    val rollNumber: String,
    val name: String
)

data class DraftTeacher(
    val localId: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val existingTeacherId: Long? = null
)

data class DraftSubject(
    val name: String,
    val code: String = "",
    val teacherLocalId: String
)

data class SetupWizardDraft(
    val currentStep: Int = 0,
    val programName: String = "",
    val semesterNumber: String = "1",
    val section: String = "",
    val session: String = "",
    val students: List<DraftStudent> = emptyList(),
    val teachers: List<DraftTeacher> = emptyList(),
    val subjects: List<DraftSubject> = emptyList()
)

enum class DeleteOutcome {
    ARCHIVED_HAS_HISTORY,
    DELETED_PERMANENTLY
}

@Singleton
class SetupRepository @Inject constructor(
    private val db: SmartAttendanceDatabase,
    private val programDao: ProgramDao,
    private val semesterDao: SemesterDao,
    private val studentDao: StudentDao,
    private val teacherDao: TeacherDao,
    private val subjectDao: SubjectDao,
    private val draftDao: DraftDao
) {
    fun observeAllPrograms(): Flow<List<ProgramEntity>> = programDao.observeAllPrograms()
    fun observeAllSemesters(): Flow<List<SemesterEntity>> = semesterDao.observeAllSemesters()
    fun observeAllStudents(): Flow<List<StudentEntity>> = studentDao.observeAllStudents()
    fun observeAllTeachers(): Flow<List<TeacherEntity>> = teacherDao.observeAllTeachers()
    fun observeAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeAllSubjects()
    fun observeActiveTeachers(): Flow<List<TeacherEntity>> = teacherDao.observeActiveTeachers()

    suspend fun loadDraft(): SetupWizardDraft? {
        val entity = draftDao.getSetupDraft() ?: return null
        return runCatching { deserializeDraft(entity.payloadJson) }.getOrNull()
    }

    suspend fun saveDraft(draft: SetupWizardDraft) {
        draftDao.saveSetupDraft(
            SetupDraftEntity(
                id = 1,
                payloadJson = serializeDraft(draft),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearDraft() {
        draftDao.clearSetupDraft()
    }

    suspend fun saveSetupWizard(draft: SetupWizardDraft): Result<Long> = runCatching {
        val cleanProgramName = draft.programName.trim().replace(Regex("\\s+"), " ")
        require(cleanProgramName.isNotEmpty()) { "Program name is required." }

        val semesterNum = draft.semesterNumber.trim().toIntOrNull()
            ?: throw IllegalArgumentException("Semester number must be a valid number.")
        require(semesterNum > 0) { "Semester number must be greater than 0." }

        val cleanSection = draft.section.trim()
        val cleanSession = draft.session.trim()
        require(cleanSession.isNotEmpty()) { "Session is required (e.g. 2024-2028)." }
        require(draft.students.isNotEmpty()) { "Add at least one student." }
        require(draft.teachers.isNotEmpty()) { "Add at least one teacher." }
        require(draft.subjects.isNotEmpty()) { "Add at least one subject." }

        db.withTransaction {
            val normProgram = normalizeProgramName(cleanProgramName)
            val existingProgram = programDao.findByNormalizedName(normProgram)
            val programId = if (existingProgram != null) {
                if (existingProgram.isArchived) {
                    programDao.setArchived(existingProgram.id, false)
                }
                existingProgram.id
            } else {
                programDao.insert(
                    ProgramEntity(
                        name = cleanProgramName,
                        normalizedName = normProgram
                    )
                )
            }

            val duplicateSem = semesterDao.findDuplicate(
                programId = programId,
                semesterNumber = semesterNum,
                section = cleanSection,
                session = cleanSession
            )
            if (duplicateSem != null) {
                val secLabel = if (cleanSection.isBlank()) "" else " ($cleanSection)"
                throw IllegalStateException(
                    "Semester $semesterNum$secLabel for session $cleanSession already exists in $cleanProgramName."
                )
            }

            val semesterId = semesterDao.insert(
                SemesterEntity(
                    programId = programId,
                    semesterNumber = semesterNum,
                    section = cleanSection,
                    session = cleanSession
                )
            )

            // Resolve or insert teachers
            val teacherIdMap = mutableMapOf<String, Long>()
            for (t in draft.teachers) {
                val resolvedId = if (t.existingTeacherId != null) {
                    t.existingTeacherId
                } else {
                    val existing = teacherDao.findByNameIgnoreCase(t.name.trim())
                    if (existing != null) {
                        if (existing.isArchived) teacherDao.setArchived(existing.id, false)
                        existing.id
                    } else {
                        teacherDao.insert(
                            TeacherEntity(
                                name = t.name.trim(),
                                phone = t.phone.trim().ifEmpty { null },
                                email = t.email.trim().ifEmpty { null }
                            )
                        )
                    }
                }
                teacherIdMap[t.localId] = resolvedId
            }

            // Insert subjects
            for (sub in draft.subjects) {
                val teacherId = teacherIdMap[sub.teacherLocalId]
                    ?: throw IllegalStateException("Subject '${sub.name}' is missing an assigned teacher.")
                subjectDao.insert(
                    SubjectEntity(
                        semesterId = semesterId,
                        teacherId = teacherId,
                        name = sub.name.trim(),
                        code = sub.code.trim().ifEmpty { null }
                    )
                )
            }

            // Validate unique roll numbers & insert sorted students
            val seenRolls = mutableSetOf<String>()
            val studentEntities = draft.students
                .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }
                .map { st ->
                    val roll = st.rollNumber.trim()
                    val normRoll = normalizeRollNumber(roll)
                    if (!seenRolls.add(normRoll)) {
                        throw IllegalStateException("Duplicate roll number: $roll")
                    }
                    StudentEntity(
                        semesterId = semesterId,
                        rollNumber = roll,
                        normalizedRoll = normRoll,
                        name = st.name.trim()
                    )
                }
            studentDao.insertAll(studentEntities)

            draftDao.clearSetupDraft()
            semesterId
        }
    }

    // --- Catalog Management ---

    suspend fun updateProgram(program: ProgramEntity, newName: String): Result<Unit> = runCatching {
        val clean = newName.trim().replace(Regex("\\s+"), " ")
        require(clean.isNotEmpty()) { "Program name cannot be empty." }
        val norm = normalizeProgramName(clean)
        val existing = programDao.findByNormalizedName(norm)
        if (existing != null && existing.id != program.id) {
            throw IllegalStateException("Another program with this name already exists.")
        }
        programDao.update(program.copy(name = clean, normalizedName = norm))
    }

    suspend fun setProgramArchived(id: Long, archived: Boolean) {
        programDao.setArchived(id, archived)
    }

    suspend fun deleteProgram(id: Long, forcePermanent: Boolean = false): DeleteOutcome {
        val hasChildren = programDao.countSemesters(id) > 0
        return if (hasChildren && !forcePermanent) {
            programDao.setArchived(id, true)
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        } else {
            if (!hasChildren) {
                programDao.deleteById(id)
                DeleteOutcome.DELETED_PERMANENTLY
            } else {
                programDao.setArchived(id, true)
                DeleteOutcome.ARCHIVED_HAS_HISTORY
            }
        }
    }

    suspend fun setSemesterArchived(id: Long, archived: Boolean) {
        semesterDao.setArchived(id, archived)
    }

    suspend fun deleteSemester(id: Long, forcePermanent: Boolean): DeleteOutcome {
        val hasHistory = semesterDao.countAttendanceSessions(id) > 0
        return if (hasHistory && !forcePermanent) {
            semesterDao.setArchived(id, true)
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        } else if (!hasHistory || forcePermanent) {
            if (hasHistory) {
                // Keep foreign keys safe: archive instead of corrupting attendance history
                semesterDao.setArchived(id, true)
                DeleteOutcome.ARCHIVED_HAS_HISTORY
            } else {
                db.withTransaction {
                    semesterDao.deleteSubjectsBySemester(id)
                    semesterDao.deleteStudentsBySemester(id)
                    semesterDao.deleteById(id)
                }
                DeleteOutcome.DELETED_PERMANENTLY
            }
        } else {
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        }
    }

    suspend fun addStudent(semesterId: Long, rollNumber: String, name: String): Result<Unit> = runCatching {
        val cleanRoll = rollNumber.trim()
        val cleanName = name.trim()
        require(cleanRoll.isNotEmpty() && cleanName.isNotEmpty()) { "Roll number and name are required." }
        val normRoll = normalizeRollNumber(cleanRoll)
        if (studentDao.findByNormalizedRoll(semesterId, normRoll) != null) {
            throw IllegalStateException("Roll number '$cleanRoll' already exists in this semester.")
        }
        studentDao.insert(
            StudentEntity(
                semesterId = semesterId,
                rollNumber = cleanRoll,
                normalizedRoll = normRoll,
                name = cleanName
            )
        )
    }

    suspend fun updateStudent(student: StudentEntity, newRoll: String, newName: String): Result<Unit> = runCatching {
        val cleanRoll = newRoll.trim()
        val cleanName = newName.trim()
        require(cleanRoll.isNotEmpty() && cleanName.isNotEmpty()) { "Roll number and name are required." }
        val normRoll = normalizeRollNumber(cleanRoll)
        val existing = studentDao.findByNormalizedRoll(student.semesterId, normRoll)
        if (existing != null && existing.id != student.id) {
            throw IllegalStateException("Roll number '$cleanRoll' already exists in this semester.")
        }
        studentDao.update(
            student.copy(
                rollNumber = cleanRoll,
                normalizedRoll = normRoll,
                name = cleanName
            )
        )
    }

    suspend fun setStudentArchived(id: Long, archived: Boolean) {
        studentDao.setArchived(id, archived)
    }

    suspend fun deleteStudent(id: Long, forcePermanent: Boolean): DeleteOutcome {
        val hasHistory = studentDao.countAttendanceRecords(id) > 0
        return if (hasHistory && !forcePermanent) {
            studentDao.setArchived(id, true)
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        } else {
            db.withTransaction {
                if (hasHistory) {
                    studentDao.deleteAttendanceRecords(id)
                }
                studentDao.deleteById(id)
            }
            DeleteOutcome.DELETED_PERMANENTLY
        }
    }

    suspend fun addOrUpdateTeacher(existing: TeacherEntity?, name: String, phone: String, email: String): Result<Unit> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Teacher name is required." }
        if (existing == null) {
            teacherDao.insert(
                TeacherEntity(
                    name = cleanName,
                    phone = phone.trim().ifEmpty { null },
                    email = email.trim().ifEmpty { null }
                )
            )
        } else {
            teacherDao.update(
                existing.copy(
                    name = cleanName,
                    phone = phone.trim().ifEmpty { null },
                    email = email.trim().ifEmpty { null }
                )
            )
        }
    }

    suspend fun setTeacherArchived(id: Long, archived: Boolean) {
        teacherDao.setArchived(id, archived)
    }

    suspend fun deleteTeacher(id: Long): DeleteOutcome {
        val hasSubjects = teacherDao.countSubjects(id) > 0
        return if (hasSubjects) {
            teacherDao.setArchived(id, true)
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        } else {
            teacherDao.deleteById(id)
            DeleteOutcome.DELETED_PERMANENTLY
        }
    }

    suspend fun addOrUpdateSubject(
        existing: SubjectEntity?,
        semesterId: Long,
        teacherId: Long,
        name: String,
        code: String
    ): Result<Unit> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Subject name is required." }
        if (existing == null) {
            subjectDao.insert(
                SubjectEntity(
                    semesterId = semesterId,
                    teacherId = teacherId,
                    name = cleanName,
                    code = code.trim().ifEmpty { null }
                )
            )
        } else {
            subjectDao.update(
                existing.copy(
                    semesterId = semesterId,
                    teacherId = teacherId,
                    name = cleanName,
                    code = code.trim().ifEmpty { null }
                )
            )
        }
    }

    suspend fun setSubjectArchived(id: Long, archived: Boolean) {
        subjectDao.setArchived(id, archived)
    }

    suspend fun deleteSubject(id: Long, forcePermanent: Boolean): DeleteOutcome {
        val hasHistory = subjectDao.countAttendanceSessions(id) > 0
        return if (hasHistory && !forcePermanent) {
            subjectDao.setArchived(id, true)
            DeleteOutcome.ARCHIVED_HAS_HISTORY
        } else {
            db.withTransaction {
                if (hasHistory) {
                    subjectDao.deleteAttendanceSessions(id)
                }
                subjectDao.deleteById(id)
            }
            DeleteOutcome.DELETED_PERMANENTLY
        }
    }

    // --- Draft JSON Serialization ---

    private fun serializeDraft(d: SetupWizardDraft): String {
        val obj = JSONObject()
        obj.put("currentStep", d.currentStep)
        obj.put("programName", d.programName)
        obj.put("semesterNumber", d.semesterNumber)
        obj.put("section", d.section)
        obj.put("session", d.session)

        val studentsArr = JSONArray()
        d.students.forEach {
            studentsArr.put(JSONObject().apply {
                put("rollNumber", it.rollNumber)
                put("name", it.name)
            })
        }
        obj.put("students", studentsArr)

        val teachersArr = JSONArray()
        d.teachers.forEach {
            teachersArr.put(JSONObject().apply {
                put("localId", it.localId)
                put("name", it.name)
                put("phone", it.phone)
                put("email", it.email)
                if (it.existingTeacherId != null) put("existingTeacherId", it.existingTeacherId)
            })
        }
        obj.put("teachers", teachersArr)

        val subjectsArr = JSONArray()
        d.subjects.forEach {
            subjectsArr.put(JSONObject().apply {
                put("name", it.name)
                put("code", it.code)
                put("teacherLocalId", it.teacherLocalId)
            })
        }
        obj.put("subjects", subjectsArr)
        return obj.toString()
    }

    private fun deserializeDraft(json: String): SetupWizardDraft {
        val obj = JSONObject(json)
        val students = mutableListOf<DraftStudent>()
        val sArr = obj.optJSONArray("students") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val item = sArr.getJSONObject(i)
            students.add(DraftStudent(item.getString("rollNumber"), item.getString("name")))
        }

        val teachers = mutableListOf<DraftTeacher>()
        val tArr = obj.optJSONArray("teachers") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val item = tArr.getJSONObject(i)
            teachers.add(
                DraftTeacher(
                    localId = item.getString("localId"),
                    name = item.getString("name"),
                    phone = item.optString("phone", ""),
                    email = item.optString("email", ""),
                    existingTeacherId = if (item.has("existingTeacherId")) item.getLong("existingTeacherId") else null
                )
            )
        }

        val subjects = mutableListOf<DraftSubject>()
        val subArr = obj.optJSONArray("subjects") ?: JSONArray()
        for (i in 0 until subArr.length()) {
            val item = subArr.getJSONObject(i)
            subjects.add(
                DraftSubject(
                    name = item.getString("name"),
                    code = item.optString("code", ""),
                    teacherLocalId = item.getString("teacherLocalId")
                )
            )
        }

        return SetupWizardDraft(
            currentStep = obj.optInt("currentStep", 0),
            programName = obj.optString("programName", ""),
            semesterNumber = obj.optString("semesterNumber", "1"),
            section = obj.optString("section", ""),
            session = obj.optString("session", ""),
            students = students,
            teachers = teachers,
            subjects = subjects
        )
    }
}
