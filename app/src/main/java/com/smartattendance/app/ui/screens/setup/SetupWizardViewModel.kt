package com.smartattendance.app.ui.screens.setup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.core.NaturalRollComparator
import com.smartattendance.app.core.normalizeRollNumber
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.data.repository.DraftStudent
import com.smartattendance.app.data.repository.DraftSubject
import com.smartattendance.app.data.repository.DraftTeacher
import com.smartattendance.app.data.repository.SetupRepository
import com.smartattendance.app.data.repository.SetupWizardDraft
import com.smartattendance.app.export.ImportPreviewResult
import com.smartattendance.app.export.StudentImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class SetupWizardUiState(
    val draft: SetupWizardDraft = SetupWizardDraft(),
    val importPreview: ImportPreviewResult? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val savedSemesterId: Long? = null
)

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val setupRepository: SetupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    val existingPrograms: StateFlow<List<ProgramEntity>> = setupRepository.observeAllPrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val existingTeachers: StateFlow<List<TeacherEntity>> = setupRepository.observeActiveTeachers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val saved = setupRepository.loadDraft()
            if (saved != null) {
                _uiState.update { it.copy(draft = saved) }
            }
        }
    }

    private fun updateDraft(transform: (SetupWizardDraft) -> SetupWizardDraft) {
        _uiState.update { state ->
            val updated = transform(state.draft)
            viewModelScope.launch { setupRepository.saveDraft(updated) }
            state.copy(draft = updated, errorMessage = null)
        }
    }

    fun setStep(step: Int) {
        updateDraft { it.copy(currentStep = step.coerceIn(0, 5)) }
    }

    fun nextStep() {
        val current = _uiState.value.draft
        val validationError = validateStep(current)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        updateDraft { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(5)) }
    }

    fun prevStep() {
        updateDraft { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    private fun validateStep(draft: SetupWizardDraft): String? = when (draft.currentStep) {
        0 -> when {
            draft.programName.trim().isEmpty() -> "Enter a degree program name."
            (draft.semesterNumber.trim().toIntOrNull() ?: 0) <= 0 -> "Enter a valid semester number."
            draft.session.trim().isEmpty() -> "Enter a session (e.g. 2024-2028)."
            else -> null
        }
        1 -> if (draft.students.isEmpty()) "Add at least one student (or import in the next step)." else null
        3 -> if (draft.teachers.isEmpty()) "Add at least one teacher." else null
        4 -> if (draft.subjects.isEmpty()) "Add at least one subject." else null
        else -> null
    }

    fun updateProgramFields(name: String, semester: String, section: String, session: String) {
        updateDraft {
            it.copy(
                programName = name,
                semesterNumber = semester,
                section = section,
                session = session
            )
        }
    }

    fun addStudent(roll: String, name: String) {
        val cleanRoll = roll.trim()
        val cleanName = name.trim()
        if (cleanRoll.isEmpty() || cleanName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Both roll number and student name are required.") }
            return
        }
        val norm = normalizeRollNumber(cleanRoll)
        if (_uiState.value.draft.students.any { normalizeRollNumber(it.rollNumber) == norm }) {
            _uiState.update { it.copy(errorMessage = "Roll number '$cleanRoll' is already in the list.") }
            return
        }
        updateDraft { d ->
            val updated = (d.students + DraftStudent(cleanRoll, cleanName))
                .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }
            d.copy(students = updated)
        }
    }

    fun bulkPasteStudents(rawText: String) {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        val existingNorms = _uiState.value.draft.students.map { normalizeRollNumber(it.rollNumber) }.toMutableSet()
        val added = mutableListOf<DraftStudent>()
        var skipped = 0

        for (line in lines) {
            val parts = when {
                line.contains("\t") -> line.split("\t", limit = 2)
                line.contains(",") -> line.split(",", limit = 2)
                else -> line.split(Regex("\\s+"), limit = 2)
            }
            val roll = parts.getOrNull(0)?.trim().orEmpty()
            val name = parts.getOrNull(1)?.trim().orEmpty()
            val norm = normalizeRollNumber(roll)
            if (roll.isNotEmpty() && name.isNotEmpty() && existingNorms.add(norm)) {
                added.add(DraftStudent(roll, name))
            } else {
                skipped++
            }
        }

        if (added.isNotEmpty()) {
            updateDraft { d ->
                val combined = (d.students + added)
                    .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }
                d.copy(students = combined)
            }
        }
        val msg = buildString {
            append("Added ${added.size} student(s).")
            if (skipped > 0) append(" Skipped $skipped invalid or duplicate line(s).")
        }
        _uiState.update { it.copy(successMessage = msg) }
    }

    fun removeStudent(rollNumber: String) {
        val norm = normalizeRollNumber(rollNumber)
        updateDraft { d ->
            d.copy(students = d.students.filterNot { normalizeRollNumber(it.rollNumber) == norm })
        }
    }

    fun parseImportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    val name = uri.lastPathSegment.orEmpty().lowercase()
                    val existingRolls = _uiState.value.draft.students
                        .map { normalizeRollNumber(it.rollNumber) }
                        .toSet()

                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (mime.contains("spreadsheet") || name.endsWith(".xlsx")) {
                            StudentImporter.parseXlsx(stream, existingRolls)
                        } else {
                            StudentImporter.parseCsv(stream, existingRolls)
                        }
                    } ?: throw IllegalStateException("Could not open selected file.")
                }
            }
            result.fold(
                onSuccess = { preview ->
                    _uiState.update { it.copy(isBusy = false, importPreview = preview) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = err.message ?: "Failed to read file."
                        )
                    }
                }
            )
        }
    }

    fun confirmImport() {
        val preview = _uiState.value.importPreview ?: return
        val valid = preview.rows.filter { it.isValid }.map { DraftStudent(it.rollNumber, it.name) }
        if (valid.isEmpty()) {
            _uiState.update { it.copy(importPreview = null, errorMessage = "No valid rows to import.") }
            return
        }
        updateDraft { d ->
            val combined = (d.students + valid)
                .sortedWith { a, b -> NaturalRollComparator.compare(a.rollNumber, b.rollNumber) }
            d.copy(students = combined)
        }
        _uiState.update {
            it.copy(
                importPreview = null,
                successMessage = "Imported ${valid.size} student(s)."
            )
        }
    }

    fun dismissImportPreview() {
        _uiState.update { it.copy(importPreview = null) }
    }

    fun writeSampleTemplate(context: Context, uri: Uri, isXlsx: Boolean) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        if (isXlsx) {
                            StudentImporter.writeSampleXlsxTemplate(out)
                        } else {
                            StudentImporter.writeSampleCsvTemplate(out)
                        }
                    } ?: throw IllegalStateException("Unable to write template.")
                }
            }
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Sample template saved.") }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Failed to save template.") }
                }
            )
        }
    }

    fun addTeacher(name: String, phone: String, email: String, existingId: Long? = null) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Teacher name is required.") }
            return
        }
        if (_uiState.value.draft.teachers.any { it.name.equals(cleanName, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Teacher '$cleanName' is already added.") }
            return
        }
        updateDraft { d ->
            d.copy(
                teachers = d.teachers + DraftTeacher(
                    localId = UUID.randomUUID().toString(),
                    name = cleanName,
                    phone = phone.trim(),
                    email = email.trim(),
                    existingTeacherId = existingId
                )
            )
        }
    }

    fun removeTeacher(localId: String) {
        updateDraft { d ->
            d.copy(
                teachers = d.teachers.filterNot { it.localId == localId },
                subjects = d.subjects.filterNot { it.teacherLocalId == localId }
            )
        }
    }

    fun addSubject(name: String, code: String, teacherLocalId: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Subject name is required.") }
            return
        }
        if (teacherLocalId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select an assigned teacher.") }
            return
        }
        updateDraft { d ->
            d.copy(
                subjects = d.subjects + DraftSubject(
                    name = cleanName,
                    code = code.trim(),
                    teacherLocalId = teacherLocalId
                )
            )
        }
    }

    fun removeSubject(index: Int) {
        updateDraft { d ->
            d.copy(subjects = d.subjects.filterIndexed { i, _ -> i != index })
        }
    }

    fun resetDraft() {
        viewModelScope.launch {
            setupRepository.clearDraft()
            _uiState.value = SetupWizardUiState()
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = setupRepository.saveSetupWizard(_uiState.value.draft)
            result.fold(
                onSuccess = { semId ->
                    _uiState.value = SetupWizardUiState(
                        savedSemesterId = semId,
                        successMessage = "Setup saved! Program is now available in Programs."
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = err.message ?: "Failed to save setup."
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
