package com.smartattendance.app.ui.screens.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.data.local.db.dao.SubjectWithTeacherTuple
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusLeave
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    onBack: () -> Unit,
    onProgramSelected: (Long) -> Unit,
    onGoToSetup: () -> Unit,
    viewModel: ProgramsFlowViewModel = hiltViewModel()
) {
    val programs by viewModel.activePrograms.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        if (programs.isEmpty()) {
            EmptyStateCard(
                title = "No Active Programs",
                message = "Create your first degree program and semester in Setup.",
                actionLabel = "Open Setup",
                onAction = onGoToSetup,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(programs, key = { it.id }) { program ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onProgramSelected(program.id) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.School, contentDescription = null, tint = BluePrimary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = program.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemestersScreen(
    programId: Long,
    onBack: () -> Unit,
    onSemesterSelected: (Long) -> Unit,
    viewModel: ProgramsFlowViewModel = hiltViewModel()
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val programName by produceState(initialValue = "Semesters", programId) {
        value = viewModel.getProgramName(programId)
    }
    val semesters by viewModel.observeSemesters(programId, showArchived)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Semester", style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = !showArchived },
                    label = { Text(if (showArchived) "Showing Archived" else "Show Archived") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (semesters.isEmpty()) {
                EmptyStateCard(
                    title = "No Semesters Found",
                    message = if (showArchived) "No semesters exist for this program." else "No active semesters. Toggle 'Show Archived' if needed.",
                    actionLabel = null,
                    onAction = {}
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(semesters, key = { it.id }) { sem ->
                        val sec = if (sem.section.isBlank()) "" else " • Section ${sem.section}"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSemesterSelected(sem.id) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(BlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Class, contentDescription = null, tint = BluePrimary)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Semester ${sem.semesterNumber}$sec",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Session ${sem.session}" + if (sem.isArchived) " (Archived)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    semesterId: Long,
    onBack: () -> Unit,
    onStartAttendance: (subjectId: Long, date: String, lectureNumber: Int) -> Unit,
    viewModel: ProgramsFlowViewModel = hiltViewModel()
) {
    val semesterLabel by produceState(initialValue = "Subjects", semesterId) {
        value = viewModel.getSemesterLabel(semesterId)
    }
    val subjects by viewModel.observeSubjects(semesterId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedSubject by remember { mutableStateOf<SubjectWithTeacherTuple?>(null) }

    selectedSubject?.let { subject ->
        SessionSetupDialog(
            subject = subject,
            checkExists = { date, lecture ->
                viewModel.hasExistingSession(subject.id, date, lecture)
            },
            onDismiss = { selectedSubject = null },
            onConfirm = { date, lecture ->
                selectedSubject = null
                onStartAttendance(subject.id, date, lecture)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(semesterLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        if (subjects.isEmpty()) {
            EmptyStateCard(
                title = "No Subjects Found",
                message = "Add subjects to this semester in Manage Saved Data.",
                actionLabel = null,
                onAction = {},
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects, key = { it.id }) { subject ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedSubject = subject },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Book, contentDescription = null, tint = BluePrimary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val title = if (subject.code.isNullOrBlank()) subject.name else "${subject.code} — ${subject.name}"
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = subject.teacherName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSetupDialog(
    subject: SubjectWithTeacherTuple,
    checkExists: suspend (String, Int) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedDate by rememberSaveable { mutableStateOf(DateUtils.todayIso()) }
    var lectureText by rememberSaveable { mutableStateOf("1") }
    var showDatePicker by remember { mutableStateOf(false) }
    var existsWarning by remember { mutableStateOf(false) }

    val lectureNum = lectureText.toIntOrNull() ?: 0

    LaunchedEffect(selectedDate, lectureNum) {
        existsWarning = if (lectureNum > 0) checkExists(selectedDate, lectureNum) else false
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initialMillis = runCatching {
            LocalDate.parse(selectedDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrDefault(todayMillis)

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val local = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        selectedDate = local.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(subject.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Teacher: ${subject.teacherName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: ${DateUtils.formatDisplayDate(selectedDate)}")
                }
                OutlinedTextField(
                    value = lectureText,
                    onValueChange = { lectureText = it.filter { c -> c.isDigit() } },
                    label = { Text("Lecture Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (existsWarning) {
                    Text(
                        "Attendance already exists for this date & lecture. Opening it will edit the existing sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusLeave,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, lectureNum) },
                enabled = lectureNum > 0 && !DateUtils.isFutureIsoDate(selectedDate),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(if (existsWarning) "Edit Attendance" else "Start Marking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (actionLabel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
