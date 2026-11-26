package com.smartattendance.app.ui.screens.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun SheetDetailScreen(
    onBack: () -> Unit,
    onEditSheet: (subjectId: Long, date: String, lectureNumber: Int) -> Unit,
    viewModel: SheetDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportInfo by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Attendance Sheet?") },
            text = { Text("This permanently deletes this lecture's attendance sheet. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSheet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportInfo) {
        AlertDialog(
            onDismissRequest = { showExportInfo = false },
            title = { Text("Export & Share") },
            text = { Text("Excel, PDF, and PNG export & sharing will be available here in Phase 5.") },
            confirmButton = {
                Button(onClick = { showExportInfo = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sheet Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.summary?.let { s ->
                        IconButton(onClick = { onEditSheet(s.subjectId, s.date, s.lectureNumber) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                        IconButton(onClick = { showExportInfo = true }) {
                            Icon(Icons.Outlined.IosShare, contentDescription = "Export/Share", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White)
                        }
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

        val summary = state.summary
        if (summary == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Attendance sheet not found.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(color = BlueLight, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val sec = if (summary.section.isBlank()) "" else " (${summary.section})"
                    Text(
                        text = "${summary.subjectName} — Lecture #${summary.lectureNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = BluePrimary
                    )
                    Text("${summary.programName} • Sem ${summary.semesterNumber}$sec")
                    Text("Teacher: ${summary.teacherNameSnapshot} • ${DateUtils.formatDisplayDate(summary.date)}")
                    Text(
                        text = "Last edited: ${DateUtils.formatDateTime(summary.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.records, key = { it.studentId }) { rec ->
                    val (fg, bg) = when (rec.status) {
                        AttendanceStatus.PRESENT -> StatusPresent to StatusPresentBg
                        AttendanceStatus.ABSENT -> StatusAbsent to StatusAbsentBg
                        AttendanceStatus.LEAVE -> StatusLeave to StatusLeaveBg
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.rollNumber,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                text = rec.studentName + if (rec.isArchived) " (Archived)" else "",
                                modifier = Modifier.weight(1f)
                            )
                            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${rec.status.code} (${rec.status.label})",
                                    color = fg,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
