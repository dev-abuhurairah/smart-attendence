package com.smartattendance.app.ui.screens.attendance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.domain.model.AttendanceStatus
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusLeave
import com.smartattendance.app.ui.theme.StatusLeaveBg
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(
    onBack: () -> Unit,
    onSavedDone: () -> Unit,
    viewModel: TakeAttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.scrollToStudentId) {
        val targetId = state.scrollToStudentId ?: return@LaunchedEffect
        val idx = state.filteredStudents.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
        viewModel.clearScrollTarget()
    }

    if (state.showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSummaryDialog() },
            title = { Text("Confirm Attendance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total Students: ${state.totalCount}")
                    Text("Present (P): ${state.presentCount}", color = StatusPresent, fontWeight = FontWeight.SemiBold)
                    Text("Absent (A): ${state.absentCount}", color = StatusAbsent, fontWeight = FontWeight.SemiBold)
                    Text("Leave (L): ${state.leaveCount}", color = StatusLeave, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Save Attendance")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSummaryDialog() }) {
                    Text("Review")
                }
            }
        )
    }

    if (state.savedSessionId != null) {
        AlertDialog(
            onDismissRequest = onSavedDone,
            title = { Text("Attendance Saved") },
            text = {
                Text("Saved to database! Export options (Excel, PDF, PNG) will be wired here in Phase 5.")
            },
            confirmButton = {
                Button(
                    onClick = onSavedDone,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditingExisting) "Edit Attendance" else "Take Attendance",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Session Header Card
            state.header?.let { h ->
                Surface(
                    color = BlueLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        val sec = if (h.section.isBlank()) "" else " (${h.section})"
                        Text(
                            text = "${h.programName} • Sem ${h.semesterNumber}$sec",
                            style = MaterialTheme.typography.titleMedium,
                            color = BluePrimary
                        )
                        Text(
                            text = "${h.subjectName} • ${h.teacherName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${DateUtils.formatDisplayDate(state.date)} • Lecture #${state.lectureNumber}" +
                                if (state.loadedFromDraft) " (Draft restored)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Counts Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountChip("Total", state.totalCount, BluePrimary, Modifier.weight(1f))
                CountChip("P", state.presentCount, StatusPresent, Modifier.weight(1f))
                CountChip("A", state.absentCount, StatusAbsent, Modifier.weight(1f))
                CountChip("L", state.leaveCount, StatusLeave, Modifier.weight(1f))
                CountChip("Left", state.unmarkedCount, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }

            // Search & Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search roll or name") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.markAllPresent() },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark All Present")
                }
                Button(
                    onClick = { viewModel.requestSave() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Save (${state.totalCount - state.unmarkedCount}/${state.totalCount})")
                }
            }

            state.errorMessage?.let { msg ->
                Surface(
                    color = StatusAbsentBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        color = StatusAbsent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Student List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filteredStudents, key = { it.id }) { student ->
                    val status = state.marks[student.id]
                    val showUnmarkedHighlight = state.highlightUnmarked && status == null
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = if (showUnmarkedHighlight) BorderStroke(1.5.dp, StatusAbsent) else null,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.rollNumber,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusToggleButton(
                                    label = "P",
                                    selected = status == AttendanceStatus.PRESENT,
                                    activeColor = StatusPresent,
                                    activeBg = StatusPresentBg,
                                    onClick = { viewModel.markStudent(student.id, AttendanceStatus.PRESENT) }
                                )
                                StatusToggleButton(
                                    label = "A",
                                    selected = status == AttendanceStatus.ABSENT,
                                    activeColor = StatusAbsent,
                                    activeBg = StatusAbsentBg,
                                    onClick = { viewModel.markStudent(student.id, AttendanceStatus.ABSENT) }
                                )
                                StatusToggleButton(
                                    label = "L",
                                    selected = status == AttendanceStatus.LEAVE,
                                    activeColor = StatusLeave,
                                    activeBg = StatusLeaveBg,
                                    onClick = { viewModel.markStudent(student.id, AttendanceStatus.LEAVE) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusToggleButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor else activeBg.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (selected) activeColor else activeColor.copy(alpha = 0.35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else activeColor
            )
        }
    }
}
