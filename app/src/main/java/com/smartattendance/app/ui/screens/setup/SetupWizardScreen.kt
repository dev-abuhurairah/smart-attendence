package com.smartattendance.app.ui.screens.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg

private val stepTitles = listOf("Program", "Students", "Import", "Teachers", "Subjects", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onBack: () -> Unit,
    onOpenManageCatalog: () -> Unit,
    onSetupSavedNavigateToPrograms: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val existingPrograms by viewModel.existingPrograms.collectAsStateWithLifecycle()
    val existingTeachers by viewModel.existingTeachers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    val openImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.parseImportFile(context, uri)
    }

    val saveCsvTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.writeSampleTemplate(context, uri, isXlsx = false)
    }

    val saveXlsxTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) viewModel.writeSampleTemplate(context, uri, isXlsx = true)
    }

    if (state.savedSemesterId != null) {
        AlertDialog(
            onDismissRequest = onSetupSavedNavigateToPrograms,
            title = { Text("Setup Saved") },
            text = { Text("Your program, semester, students, teachers, and subjects were saved successfully.") },
            confirmButton = {
                Button(onClick = onSetupSavedNavigateToPrograms) {
                    Text("Go to Programs")
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Back to Home")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Clear Setup Draft?") },
            text = { Text("This clears all unsaved fields in this setup wizard.") },
            confirmButton = {
                Button(onClick = {
                    showResetConfirm = false
                    viewModel.resetDraft()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    state.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = { Text("Import Preview (${preview.validCount} valid, ${preview.errorCount} issues)") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(preview.rows) { row ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (row.isValid) StatusPresentBg else StatusAbsentBg
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (row.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (row.isValid) StatusPresent else StatusAbsent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Row ${row.rowNumber}: ${row.rollNumber} — ${row.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (row.error != null) {
                                        Text(
                                            text = row.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusAbsent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmImport() },
                    enabled = preview.validCount > 0
                ) {
                    Text("Import ${preview.validCount} Valid")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPreview() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Wizard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenManageCatalog) {
                        Text("Manage", color = Color.White)
                    }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset Draft", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stepper Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stepTitles.forEachIndexed { index, title ->
                    val selected = state.draft.currentStep == index
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setStep(index) },
                        label = { Text("${index + 1}. $title") }
                    )
                }
            }
            HorizontalDivider()

            // Feedback Banners
            state.errorMessage?.let { msg ->
                Surface(
                    color = StatusAbsentBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StatusAbsent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = StatusAbsent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("Dismiss") }
                    }
                }
            }

            state.successMessage?.let { msg ->
                Surface(
                    color = StatusPresentBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StatusPresent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = StatusPresent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") }
                    }
                }
            }

            if (state.isBusy) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (state.draft.currentStep) {
                    0 -> StepProgram(
                        programName = state.draft.programName,
                        semesterNumber = state.draft.semesterNumber,
                        section = state.draft.section,
                        session = state.draft.session,
                        existingPrograms = existingPrograms.map { it.name },
                        onChanged = viewModel::updateProgramFields
                    )
                    1 -> StepStudents(
                        students = state.draft.students,
                        onAddStudent = viewModel::addStudent,
                        onBulkPaste = viewModel::bulkPasteStudents,
                        onRemoveStudent = viewModel::removeStudent
                    )
                    2 -> StepImport(
                        studentCount = state.draft.students.size,
                        onPickFile = {
                            openImportLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/csv"
                                )
                            )
                        },
                        onDownloadCsvTemplate = { saveCsvTemplateLauncher.launch("students_template.csv") },
                        onDownloadXlsxTemplate = { saveXlsxTemplateLauncher.launch("students_template.xlsx") }
                    )
                    3 -> StepTeachers(
                        teachers = state.draft.teachers,
                        existingTeachers = existingTeachers,
                        onAddTeacher = viewModel::addTeacher,
                        onRemoveTeacher = viewModel::removeTeacher
                    )
                    4 -> StepSubjects(
                        teachers = state.draft.teachers,
                        subjects = state.draft.subjects,
                        onAddSubject = viewModel::addSubject,
                        onRemoveSubject = viewModel::removeSubject
                    )
                    5 -> StepReview(
                        state = state,
                        onSaveAll = viewModel::saveAll
                    )
                }
            }

            // Bottom Navigation Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.prevStep() },
                        enabled = state.draft.currentStep > 0,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Previous")
                    }

                    if (state.draft.currentStep < 5) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text(if (state.draft.currentStep == 1) "Next (or Import)" else "Next")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveAll() },
                            enabled = !state.isBusy,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("Save Everything")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepProgram(
    programName: String,
    semesterNumber: String,
    section: String,
    session: String,
    existingPrograms: List<String>,
    onChanged: (String, String, String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Step 1: Program & Semester", style = MaterialTheme.typography.titleLarge)
        if (existingPrograms.isNotEmpty()) {
            Text(
                "Tap an existing program to add a new semester to it:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingPrograms.forEach { existing ->
                    AssistChip(
                        onClick = { onChanged(existing, semesterNumber, section, session) },
                        label = { Text(existing) }
                    )
                }
            }
        }
        OutlinedTextField(
            value = programName,
            onValueChange = { onChanged(it, semesterNumber, section, session) },
            label = { Text("Degree Program Name *") },
            placeholder = { Text("e.g. BS Computer Science") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = semesterNumber,
                onValueChange = { onChanged(programName, it.filter { c -> c.isDigit() }, section, session) },
                label = { Text("Semester # *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = section,
                onValueChange = { onChanged(programName, semesterNumber, it, session) },
                label = { Text("Section (Optional)") },
                placeholder = { Text("e.g. A") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = session,
            onValueChange = { onChanged(programName, semesterNumber, section, it) },
            label = { Text("Session *") },
            placeholder = { Text("e.g. 2024-2028") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepStudents(
    students: List<com.smartattendance.app.data.repository.DraftStudent>,
    onAddStudent: (String, String) -> Unit,
    onBulkPaste: (String) -> Unit,
    onRemoveStudent: (String) -> Unit
) {
    var roll by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showPasteDialog by rememberSaveable { mutableStateOf(false) }
    var pasteText by rememberSaveable { mutableStateOf("") }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Quick Multi-Line Paste") },
            text = {
                Column {
                    Text(
                        "Paste one student per line (Roll, Name):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text("CS-01, Ayesha Khan\nCS-02, Bilal Ahmed") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onBulkPaste(pasteText)
                    pasteText = ""
                    showPasteDialog = false
                }) { Text("Add All") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step 2: Students (${students.size})", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { showPasteDialog = true }) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bulk Paste")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roll,
                onValueChange = { roll = it },
                label = { Text("Roll No *") },
                singleLine = true,
                modifier = Modifier.weight(0.42f)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Student Name *") },
                singleLine = true,
                modifier = Modifier.weight(0.58f)
            )
            IconButton(
                onClick = {
                    onAddStudent(roll, name)
                    if (roll.isNotBlank() && name.isNotBlank()) {
                        roll = ""
                        name = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BluePrimary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Student", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (students.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BlueLight)
            ) {
                Text(
                    "No students added yet. Enter above, use Bulk Paste, or import from Excel/CSV in Step 3.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.rollNumber }) { st ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = st.rollNumber,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.widthIn(min = 72.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = st.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onRemoveStudent(st.rollNumber) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusAbsent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepImport(
    studentCount: Int,
    onPickFile: () -> Unit,
    onDownloadCsvTemplate: () -> Unit,
    onDownloadXlsxTemplate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Step 3: Import Students (Optional)", style = MaterialTheme.typography.titleLarge)
        Text(
            "Import from Excel (.xlsx) or CSV with columns: Roll Number, Student Name. Currently $studentCount student(s) in list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose .xlsx or .csv File")
        }
        HorizontalDivider()
        Text("Need a sample template?", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDownloadXlsxTemplate,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Excel (.xlsx)")
            }
            OutlinedButton(
                onClick = onDownloadCsvTemplate,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV (.csv)")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepTeachers(
    teachers: List<com.smartattendance.app.data.repository.DraftTeacher>,
    existingTeachers: List<com.smartattendance.app.data.local.db.entity.TeacherEntity>,
    onAddTeacher: (String, String, String, Long?) -> Unit,
    onRemoveTeacher: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Step 4: Teachers (${teachers.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (existingTeachers.isNotEmpty()) {
            Text(
                "Quick-add saved teachers:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingTeachers.forEach { t ->
                    AssistChip(
                        onClick = { onAddTeacher(t.name, t.phone.orEmpty(), t.email.orEmpty(), t.id) },
                        label = { Text(t.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Teacher Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onAddTeacher(name, phone, email, null)
                if (name.isNotBlank()) {
                    name = ""
                    phone = ""
                    email = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Add Teacher")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(teachers, key = { it.localId }) { t ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold)
                            val contact = listOfNotNull(
                                t.phone.takeIf { it.isNotBlank() },
                                t.email.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (contact.isNotEmpty()) {
                                Text(contact, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onRemoveTeacher(t.localId) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusAbsent)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepSubjects(
    teachers: List<com.smartattendance.app.data.repository.DraftTeacher>,
    subjects: List<com.smartattendance.app.data.repository.DraftSubject>,
    onAddSubject: (String, String, String) -> Unit,
    onRemoveSubject: (Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var selectedTeacherId by rememberSaveable { mutableStateOf(teachers.firstOrNull()?.localId.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }

    val selectedTeacherName = teachers.find { it.localId == selectedTeacherId }?.name ?: "Select Teacher"

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Step 5: Subjects (${subjects.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Subject Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(0.4f)
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(0.6f)
            ) {
                OutlinedTextField(
                    value = selectedTeacherName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Teacher *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    teachers.forEach { teacher ->
                        DropdownMenuItem(
                            text = { Text(teacher.name) },
                            onClick = {
                                selectedTeacherId = teacher.localId
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onAddSubject(name, code, selectedTeacherId)
                if (name.isNotBlank() && selectedTeacherId.isNotEmpty()) {
                    name = ""
                    code = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Add Subject")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(subjects) { idx, sub ->
                val teacherName = teachers.find { it.localId == sub.teacherLocalId }?.name ?: "Unknown"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val title = if (sub.code.isNotBlank()) "${sub.code} — ${sub.name}" else sub.name
                            Text(title, fontWeight = FontWeight.SemiBold)
                            Text("Teacher: $teacherName", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onRemoveSubject(idx) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusAbsent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepReview(
    state: SetupWizardUiState,
    onSaveAll: () -> Unit
) {
    val d = state.draft
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Step 6: Review & Save", style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Program: ${d.programName}", fontWeight = FontWeight.Bold)
                val sec = if (d.section.isBlank()) "None" else d.section
                Text("Semester: ${d.semesterNumber} | Section: $sec | Session: ${d.session}")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Students: ${d.students.size}", fontWeight = FontWeight.Bold)
                d.students.take(5).forEach { Text("• ${it.rollNumber} — ${it.name}") }
                if (d.students.size > 5) Text("...and ${d.students.size - 5} more")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Teachers (${d.teachers.size}) & Subjects (${d.subjects.size})", fontWeight = FontWeight.Bold)
                d.subjects.forEach { sub ->
                    val tName = d.teachers.find { it.localId == sub.teacherLocalId }?.name ?: ""
                    Text("• ${sub.name} (${tName})")
                }
            }
        }
        Button(
            onClick = onSaveAll,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Text("Confirm & Save All")
        }
    }
}
