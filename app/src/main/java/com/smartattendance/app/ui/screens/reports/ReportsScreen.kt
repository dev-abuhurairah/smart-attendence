package com.smartattendance.app.ui.screens.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.data.local.db.entity.ExportHistoryEntity
import com.smartattendance.app.data.repository.ReportType
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusLeave
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showExportMenu by remember { mutableStateOf(false) }
    var pendingRemoveEntry by remember { mutableStateOf<ExportHistoryEntity?>(null) }

    val createXlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.XLSX.mimeType)
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    LaunchedEffect(state.pendingSafBundle) {
        val bundle = state.pendingSafBundle ?: return@LaunchedEffect
        when (bundle.format) {
            ExportFormat.XLSX -> createXlsxLauncher.launch(bundle.suggestedName)
            ExportFormat.PDF -> createPdfLauncher.launch(bundle.suggestedName)
            ExportFormat.PNG -> openTreeLauncher.launch(null)
        }
    }

    if (showExportMenu) {
        AlertDialog(
            onDismissRequest = { showExportMenu = false },
            title = { Text("Export Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { fmt ->
                        Button(
                            onClick = {
                                showExportMenu = false
                                viewModel.exportCurrentReport(fmt)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("Export as ${fmt.label}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportMenu = false }) { Text("Cancel") }
            }
        )
    }

    pendingRemoveEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingRemoveEntry = null },
            title = { Text("Remove from History?") },
            text = { Text("Remove '${entry.fileName}' from the list? Your attendance data will NOT be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeHistoryEntry(entry.id)
                        pendingRemoveEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveEntry = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Analytics") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Export History (${state.exportHistory.size})") })
            }

            state.errorMessage?.let { msg ->
                Surface(color = StatusAbsentBg, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusAbsent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") }
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Surface(color = StatusPresentBg, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusPresent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") }
                    }
                }
            }

            if (state.isExporting) {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (selectedTab == 0) {
                AnalyticsTab(
                    state = state,
                    onQueryChange = viewModel::updateQuery,
                    onExportClick = { showExportMenu = true }
                )
            } else {
                ExportHistoryTab(
                    items = state.exportHistory,
                    onOpen = { viewModel.openExportedFile(context, it) },
                    onShare = { viewModel.shareExportedFile(context, it) },
                    onRecreate = { viewModel.recreateHistoryEntry(it) },
                    onRemove = { pendingRemoveEntry = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsTab(
    state: ReportsUiState,
    onQueryChange: ((com.smartattendance.app.data.repository.ReportQuery) -> com.smartattendance.app.data.repository.ReportQuery) -> Unit,
    onExportClick: () -> Unit
) {
    val summary = state.summary
    var pickingDaily by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    if (pickingDaily || pickingStart || pickingEnd) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = {
                pickingDaily = false
                pickingStart = false
                pickingEnd = false
            },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val iso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        onQueryChange { q ->
                            when {
                                pickingDaily -> q.copy(dailyDate = iso)
                                pickingStart -> q.copy(startDate = iso)
                                else -> q.copy(endDate = iso)
                            }
                        }
                    }
                    pickingDaily = false
                    pickingStart = false
                    pickingEnd = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pickingDaily = false
                    pickingStart = false
                    pickingEnd = false
                }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportType.entries.forEach { type ->
                    FilterChip(
                        selected = state.query.type == type,
                        onClick = { onQueryChange { it.copy(type = type) } },
                        label = { Text(type.label) }
                    )
                }
            }
        }

        if (state.query.type == ReportType.DAILY) {
            item {
                OutlinedButton(onClick = { pickingDaily = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date: ${DateUtils.formatDisplayDate(state.query.dailyDate)}")
                }
            }
        } else if (state.query.type == ReportType.CUSTOM_RANGE) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                        Text("From: ${state.query.startDate}")
                    }
                    OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                        Text("To: ${state.query.endDate}")
                    }
                }
            }
        }

        if (summary != null) {
            if (summary.availablePrograms.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = state.query.programName == null,
                            onClick = { onQueryChange { it.copy(programName = null) } },
                            label = { Text("All Programs") }
                        )
                        summary.availablePrograms.forEach { prog ->
                            FilterChip(
                                selected = state.query.programName == prog,
                                onClick = { onQueryChange { it.copy(programName = if (it.programName == prog) null else prog) } },
                                label = { Text(prog) }
                            )
                        }
                    }
                }
            }

            item {
                Surface(color = BlueLight, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.activeRuleLabel, style = MaterialTheme.typography.labelLarge, color = BluePrimary)
                                Text(
                                    "Low attendance highlighted below ${summary.threshold}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onExportClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Text("Export")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Overall: P=${summary.totalPresent}  A=${summary.totalAbsent}  L=${summary.totalLeave}  •  ${summary.overallPercentageLabel}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (summary.rows.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Saved Attendance", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No saved attendance matches this report filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(summary.rows, key = { "${it.primaryLabel}_${it.secondaryLabel}" }) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = if (row.isLowAttendance) BorderStroke(1.2.dp, StatusAbsent) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.primaryLabel, fontWeight = FontWeight.SemiBold)
                                Text(
                                    row.secondaryLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "P: ${row.presentCount}  A: ${row.absentCount}  L: ${row.leaveCount}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Surface(
                                color = if (row.isLowAttendance) StatusAbsentBg else StatusPresentBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = row.percentageLabel,
                                    color = if (row.isLowAttendance) StatusAbsent else StatusPresent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
private fun ExportHistoryTab(
    items: List<ExportHistoryItemUi>,
    onOpen: (ExportHistoryEntity) -> Unit,
    onShare: (ExportHistoryEntity) -> Unit,
    onRecreate: (ExportHistoryEntity) -> Unit,
    onRemove: (ExportHistoryEntity) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No Exported Files Yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Files you export as Excel, PDF, or PNG will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.entity.id }) { item ->
            val e = item.entity
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(e.fileName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${e.format} • ${e.mode} • ${e.programName} (${e.semesterLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                DateUtils.formatDateTime(e.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!item.isAccessible) {
                            Surface(color = StatusAbsentBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    "Missing",
                                    color = StatusAbsent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (item.isAccessible) {
                            OutlinedButton(onClick = { onOpen(e) }) {
                                Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open")
                            }
                            if (!e.isTreeUri) {
                                OutlinedButton(onClick = { onShare(e) }) {
                                    Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        } else {
                            Button(
                                onClick = { onRecreate(e) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusLeave)
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recreate")
                            }
                        }
                        IconButton(onClick = { onRemove(e) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove from list", tint = StatusAbsent)
                        }
                    }
                }
            }
        }
    }
}
