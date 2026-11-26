package com.smartattendance.app.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopySemesterScreen(
    onBack: () -> Unit,
    viewModel: CopySemesterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var semExpanded by remember { mutableStateOf(false) }
    var teacherExpanded by remember { mutableStateOf(false) }
    var subName by remember { mutableStateOf("") }
    var subCode by remember { mutableStateOf("") }
    var selectedTeacherId by remember(state.teachers) {
        mutableLongStateOf(state.teachers.firstOrNull()?.id ?: 0L)
    }

    LaunchedEffect(state.isDone) {
        if (state.isDone) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Copy to Next Semester") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.errorMessage?.let { msg ->
                item {
                    Surface(color = StatusAbsentBg, modifier = Modifier.fillMaxWidth()) {
                        Text(msg, color = StatusAbsent, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item {
                val srcLabel = state.sourceSemester?.let { sem ->
                    val prog = state.programs.find { it.id == sem.programId }?.name ?: "Program"
                    "$prog — Sem ${sem.semesterNumber} (${sem.session})"
                } ?: "Select Source Semester"

                ExposedDropdownMenuBox(expanded = semExpanded, onExpandedChange = { semExpanded = !semExpanded }) {
                    OutlinedTextField(
                        value = srcLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source Semester") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = semExpanded, onDismissRequest = { semExpanded = false }) {
                        state.semesters.forEach { sem ->
                            val prog = state.programs.find { it.id == sem.programId }?.name ?: "Program"
                            DropdownMenuItem(
                                text = { Text("$prog — Sem ${sem.semesterNumber} (${sem.session})") },
                                onClick = {
                                    viewModel.selectSourceSemester(sem)
                                    semExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.sourceSemester != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.newSemesterNumber,
                            onValueChange = { viewModel.updateTargetFields(it.filter { c -> c.isDigit() }, state.newSection, state.newSession) },
                            label = { Text("New Sem #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.newSection,
                            onValueChange = { viewModel.updateTargetFields(state.newSemesterNumber, it, state.newSession) },
                            label = { Text("Section") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.newSession,
                        onValueChange = { viewModel.updateTargetFields(state.newSemesterNumber, state.newSection, it) },
                        label = { Text("Session") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Students to Copy (${state.selectedStudentIds.size}/${state.sourceStudents.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.selectAllStudents(true) }) { Text("All") }
                            OutlinedButton(onClick = { viewModel.selectAllStudents(false) }) { Text("None") }
                        }
                    }
                }

                items(state.sourceStudents, key = { it.id }) { st ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = st.id in state.selectedStudentIds,
                            onCheckedChange = { viewModel.toggleStudent(st.id) }
                        )
                        Text("${st.rollNumber} — ${st.name}")
                    }
                }

                item {
                    HorizontalDivider()
                    Text("New Semester Subjects", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subCode,
                            onValueChange = { subCode = it },
                            label = { Text("Code") },
                            modifier = Modifier.weight(0.4f)
                        )
                        val tName = state.teachers.find { it.id == selectedTeacherId }?.name ?: "Teacher"
                        ExposedDropdownMenuBox(
                            expanded = teacherExpanded,
                            onExpandedChange = { teacherExpanded = !teacherExpanded },
                            modifier = Modifier.weight(0.6f)
                        ) {
                            OutlinedTextField(
                                value = tName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Teacher") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = teacherExpanded, onDismissRequest = { teacherExpanded = false }) {
                                state.teachers.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name) },
                                        onClick = {
                                            selectedTeacherId = t.id
                                            teacherExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            if (selectedTeacherId > 0L) {
                                viewModel.addSubject(subName, subCode, selectedTeacherId)
                                if (subName.isNotBlank()) {
                                    subName = ""
                                    subCode = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Subject") }
                }

                items(state.subjects.size) { idx ->
                    val s = state.subjects[idx]
                    val tName = state.teachers.find { it.id == s.teacherId }?.name ?: ""
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${s.name} ($tName)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            IconButton(onClick = { viewModel.removeSubject(idx) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusAbsent)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.copyNow() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Copy to New Semester")
                    }
                }
            }
        }
    }
}
