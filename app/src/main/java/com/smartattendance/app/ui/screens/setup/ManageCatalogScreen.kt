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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.data.local.db.entity.ProgramEntity
import com.smartattendance.app.data.local.db.entity.StudentEntity
import com.smartattendance.app.data.local.db.entity.TeacherEntity
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg

private val catalogTabs = listOf("Programs", "Semesters", "Students", "Teachers", "Subjects")

private sealed interface PendingDelete {
    data class Program(val id: Long, val label: String) : PendingDelete
    data class Semester(val id: Long, val label: String, val step: Int = 1) : PendingDelete
    data class Student(val id: Long, val label: String, val step: Int = 1) : PendingDelete
    data class Teacher(val id: Long, val label: String) : PendingDelete
    data class Subject(val id: Long, val label: String, val step: Int = 1) : PendingDelete
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCatalogScreen(
    onBack: () -> Unit,
    viewModel: ManageCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    var editingProgram by remember { mutableStateOf<ProgramEntity?>(null) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var editingTeacher by remember { mutableStateOf<TeacherEntity?>(null) }

    // 1-step & 2-step Delete Confirmation Dialogs
    pendingDelete?.let { target ->
        val isSecondWarning = when (target) {
            is PendingDelete.Semester -> target.step == 2
            is PendingDelete.Student -> target.step == 2
            is PendingDelete.Subject -> target.step == 2
            else -> false
        }
        val label = when (target) {
            is PendingDelete.Program -> target.label
            is PendingDelete.Semester -> target.label
            is PendingDelete.Student -> target.label
            is PendingDelete.Teacher -> target.label
            is PendingDelete.Subject -> target.label
        }

        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(if (isSecondWarning) "Permanent Delete Warning" else "Confirm Remove")
            },
            text = {
                Text(
                    if (isSecondWarning) {
                        "Are you absolutely sure you want to permanently delete '$label' and its linked attendance records? This cannot be undone."
                    } else {
                        "Remove '$label'? Items with attendance history are safely archived automatically unless you choose Permanent Delete."
                    }
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSecondWarning) StatusAbsent else BluePrimary
                    ),
                    onClick = {
                        when (target) {
                            is PendingDelete.Program -> {
                                viewModel.deleteProgram(target.id)
                                pendingDelete = null
                            }
                            is PendingDelete.Teacher -> {
                                viewModel.deleteTeacher(target.id)
                                pendingDelete = null
                            }
                            is PendingDelete.Semester -> {
                                if (target.step == 1) {
                                    viewModel.deleteSemester(target.id, forcePermanent = false)
                                    pendingDelete = null
                                } else {
                                    viewModel.deleteSemester(target.id, forcePermanent = true)
                                    pendingDelete = null
                                }
                            }
                            is PendingDelete.Student -> {
                                if (target.step == 1) {
                                    viewModel.deleteStudent(target.id, forcePermanent = false)
                                    pendingDelete = null
                                } else {
                                    viewModel.deleteStudent(target.id, forcePermanent = true)
                                    pendingDelete = null
                                }
                            }
                            is PendingDelete.Subject -> {
                                if (target.step == 1) {
                                    viewModel.deleteSubject(target.id, forcePermanent = false)
                                    pendingDelete = null
                                } else {
                                    viewModel.deleteSubject(target.id, forcePermanent = true)
                                    pendingDelete = null
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isSecondWarning) "Delete Permanently" else "Archive / Remove")
                }
            },
            dismissButton = {
                Row {
                    if (!isSecondWarning && (target is PendingDelete.Student || target is PendingDelete.Subject)) {
                        TextButton(onClick = {
                            pendingDelete = when (target) {
                                is PendingDelete.Student -> target.copy(step = 2)
                                is PendingDelete.Subject -> target.copy(step = 2)
                                else -> null
                            }
                        }) {
                            Text("Permanent Delete", color = StatusAbsent)
                        }
                    }
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    editingProgram?.let { prog ->
        var name by remember(prog.id) { mutableStateOf(prog.name) }
        AlertDialog(
            onDismissRequest = { editingProgram = null },
            title = { Text("Edit Program") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Program Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameProgram(prog, name)
                    editingProgram = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingProgram = null }) { Text("Cancel") }
            }
        )
    }

    editingStudent?.let { st ->
        var roll by remember(st.id) { mutableStateOf(st.rollNumber) }
        var name by remember(st.id) { mutableStateOf(st.name) }
        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = { Text("Edit Student") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = roll, onValueChange = { roll = it }, label = { Text("Roll Number") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addOrUpdateStudent(st, st.semesterId, roll, name)
                    editingStudent = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingStudent = null }) { Text("Cancel") }
            }
        )
    }

    editingTeacher?.let { t ->
        var name by remember(t.id) { mutableStateOf(t.name) }
        var phone by remember(t.id) { mutableStateOf(t.phone.orEmpty()) }
        var email by remember(t.id) { mutableStateOf(t.email.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editingTeacher = null },
            title = { Text("Edit Teacher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveTeacher(t, name, phone, email)
                    editingTeacher = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingTeacher = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Saved Data") },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                catalogTabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title) }
                    )
                }
            }

            state.errorMessage?.let { msg ->
                Surface(color = StatusAbsentBg, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusAbsent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearFeedback() }) { Text("OK") }
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Surface(color = StatusPresentBg, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusPresent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearFeedback() }) { Text("OK") }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> items(state.programs, key = { it.id }) { p ->
                        CatalogItemCard(
                            title = p.name,
                            subtitle = if (p.isArchived) "Archived" else "Active",
                            isArchived = p.isArchived,
                            onEdit = { editingProgram = p },
                            onToggleArchive = { viewModel.setProgramArchived(p.id, !p.isArchived) },
                            onDelete = { pendingDelete = PendingDelete.Program(p.id, p.name) }
                        )
                    }
                    1 -> items(state.semesters, key = { it.id }) { sem ->
                        val progName = state.programs.find { it.id == sem.programId }?.name ?: "Program"
                        val sec = if (sem.section.isBlank()) "" else " (${sem.section})"
                        val label = "$progName — Sem ${sem.semesterNumber}$sec • ${sem.session}"
                        CatalogItemCard(
                            title = label,
                            subtitle = if (sem.isArchived) "Archived" else "Active",
                            isArchived = sem.isArchived,
                            onEdit = null,
                            onToggleArchive = { viewModel.setSemesterArchived(sem.id, !sem.isArchived) },
                            onDelete = { pendingDelete = PendingDelete.Semester(sem.id, label) }
                        )
                    }
                    2 -> items(state.students, key = { it.id }) { st ->
                        CatalogItemCard(
                            title = "${st.rollNumber} — ${st.name}",
                            subtitle = if (st.isArchived) "Archived" else "Active",
                            isArchived = st.isArchived,
                            onEdit = { editingStudent = st },
                            onToggleArchive = { viewModel.setStudentArchived(st.id, !st.isArchived) },
                            onDelete = { pendingDelete = PendingDelete.Student(st.id, "${st.rollNumber} ${st.name}") }
                        )
                    }
                    3 -> items(state.teachers, key = { it.id }) { t ->
                        val contact = listOfNotNull(t.phone, t.email).joinToString(" • ").ifEmpty { "No contact info" }
                        CatalogItemCard(
                            title = t.name,
                            subtitle = if (t.isArchived) "Archived • $contact" else contact,
                            isArchived = t.isArchived,
                            onEdit = { editingTeacher = t },
                            onToggleArchive = { viewModel.setTeacherArchived(t.id, !t.isArchived) },
                            onDelete = { pendingDelete = PendingDelete.Teacher(t.id, t.name) }
                        )
                    }
                    4 -> items(state.subjects, key = { it.id }) { sub ->
                        val tName = state.teachers.find { it.id == sub.teacherId }?.name ?: "Unknown"
                        val title = if (sub.code.isNullOrBlank()) sub.name else "${sub.code} — ${sub.name}"
                        CatalogItemCard(
                            title = title,
                            subtitle = "Teacher: $tName" + if (sub.isArchived) " (Archived)" else "",
                            isArchived = sub.isArchived,
                            onEdit = null,
                            onToggleArchive = { viewModel.setSubjectArchived(sub.id, !sub.isArchived) },
                            onDelete = { pendingDelete = PendingDelete.Subject(sub.id, title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogItemCard(
    title: String,
    subtitle: String,
    isArchived: Boolean,
    onEdit: (() -> Unit)?,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                }
            }
            IconButton(onClick = onToggleArchive) {
                Icon(
                    imageVector = if (isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    contentDescription = if (isArchived) "Restore" else "Archive"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = StatusAbsent)
            }
        }
    }
}
