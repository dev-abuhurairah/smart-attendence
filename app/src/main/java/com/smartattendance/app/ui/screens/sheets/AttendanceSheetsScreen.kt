package com.smartattendance.app.ui.screens.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.data.local.db.dao.SavedSheetSummaryTuple
import com.smartattendance.app.data.repository.SheetsFilter
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusLeave
import com.smartattendance.app.ui.theme.StatusLeaveBg
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceSheetsScreen(
    onBack: () -> Unit,
    onOpenSheet: (Long) -> Unit,
    viewModel: AttendanceSheetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    if (showFilters) {
        SheetsFilterDialog(
            current = state.filter,
            options = state.options,
            onDismiss = { showFilters = false },
            onApply = { newFilter ->
                viewModel.updateFilter { newFilter }
                showFilters = false
            },
            onClear = {
                viewModel.clearFilters()
                showFilters = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Sheets (${state.totalCount})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.filter.isActive) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear Filters", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Filters", tint = Color.White)
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
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.groupedSheets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (state.filter.isActive) "No Matching Sheets" else "No Attendance Sheets Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (state.filter.isActive) {
                                    "Try clearing or adjusting your filters."
                                } else {
                                    "Take attendance from Programs to see saved sheets here."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.filter.isActive) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(onClick = { viewModel.clearFilters() }) {
                                    Text("Clear Filters")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.groupedSheets.forEach { (date, sheets) ->
                        item(key = "header_$date") {
                            Surface(
                                color = BlueLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            ) {
                                Text(
                                    text = DateUtils.formatDisplayDate(date),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(sheets, key = { it.sessionId }) { sheet ->
                            SheetSummaryCard(
                                sheet = sheet,
                                onClick = { onOpenSheet(sheet.sessionId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSummaryCard(
    sheet: SavedSheetSummaryTuple,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val codePrefix = if (sheet.subjectCode.isNullOrBlank()) "" else "${sheet.subjectCode} — "
                Text(
                    text = "$codePrefix${sheet.subjectName}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BlueLight
                ) {
                    Text(
                        text = "L${sheet.lectureNumber}",
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val sec = if (sheet.section.isBlank()) "" else " (${sheet.section})"
            Text(
                text = "${sheet.programName} • Sem ${sheet.semesterNumber}$sec • ${sheet.teacherNameSnapshot}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatusBadge("P ${sheet.presentCount}", StatusPresent, StatusPresentBg)
                MiniStatusBadge("A ${sheet.absentCount}", StatusAbsent, StatusAbsentBg)
                MiniStatusBadge("L ${sheet.leaveCount}", StatusLeave, StatusLeaveBg)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last edited: ${DateUtils.formatDateTime(sheet.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniStatusBadge(text: String, fg: Color, bg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SheetsFilterDialog(
    current: SheetsFilter,
    options: SheetsFilterOptions,
    onDismiss: () -> Unit,
    onApply: (SheetsFilter) -> Unit,
    onClear: () -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    if (pickingStartDate || pickingEndDate) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                pickingStartDate = false
                pickingEndDate = false
            },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        draft = if (pickingStartDate) draft.copy(startDate = iso) else draft.copy(endDate = iso)
                    }
                    pickingStartDate = false
                    pickingEndDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickingStartDate = false
                    pickingEndDate = false
                }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Sheets") },
        text = {
            Column(
                modifier = Modifier
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipSection("Program", options.programs, draft.programName) {
                    draft = draft.copy(programName = if (draft.programName == it) null else it)
                }
                FilterChipSection("Semester", options.semesters.map { "Sem $it" }, draft.semesterNumber?.let { "Sem $it" }) { label ->
                    val num = label.removePrefix("Sem ").toIntOrNull()
                    draft = draft.copy(semesterNumber = if (draft.semesterNumber == num) null else num)
                }
                if (options.sections.isNotEmpty()) {
                    FilterChipSection("Section", options.sections, draft.section) {
                        draft = draft.copy(section = if (draft.section == it) null else it)
                    }
                }
                FilterChipSection("Subject", options.subjects, draft.subjectName) {
                    draft = draft.copy(subjectName = if (draft.subjectName == it) null else it)
                }
                FilterChipSection("Teacher", options.teachers, draft.teacherName) {
                    draft = draft.copy(teacherName = if (draft.teacherName == it) null else it)
                }
                HorizontalDivider()
                Text("Date Range", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pickingStartDate = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(draft.startDate ?: "From")
                    }
                    OutlinedButton(
                        onClick = { pickingEndDate = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(draft.endDate ?: "To")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(draft) },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear All") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipSection(
    title: String,
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    if (items.isEmpty()) return
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                FilterChip(
                    selected = selected == item,
                    onClick = { onSelect(item) },
                    label = { Text(item) }
                )
            }
        }
    }
}
