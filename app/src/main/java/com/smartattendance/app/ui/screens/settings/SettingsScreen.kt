package com.smartattendance.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.core.DateUtils
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.domain.model.ThemeMode
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCopySemester: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    var instituteInput by remember(settings.instituteName) { mutableStateOf(settings.instituteName) }
    var thresholdInput by remember(settings.attendanceThreshold) { mutableStateOf(settings.attendanceThreshold.toString()) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showBackupWarning by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.saveLogoFromUri(uri)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.createBackupToUri(uri)
    }

    val openRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set App Lock PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Important: Because Smart Attendance is 100% offline, a forgotten PIN cannot be recovered. If forgotten, you must unlock with fingerprint (if enabled) or clear app data and restore from a backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAbsent
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("4–6 Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPin(pin)
                        showPinDialog = false
                    },
                    enabled = pin.length in 4..6,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBackupWarning) {
        AlertDialog(
            onDismissRequest = { showBackupWarning = false },
            title = { Text("Private Data Notice") },
            text = { Text("Backup .zip files contain private student names, roll numbers, and attendance records. Store them securely.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupWarning = false
                        createBackupLauncher.launch("smart_attendance_backup_${DateUtils.todayIso()}.zip")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) { Text("Create Backup") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupWarning = false }) { Text("Cancel") }
            }
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore Backup?") },
            text = {
                Text("Restoring replaces ALL current programs, students, and attendance data. An automatic local safety backup will be created first in case anything fails.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.restoreBackupFromUri(uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent)
                ) { Text("Restore Now") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.errorMessage?.let { msg ->
                Surface(color = StatusAbsentBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusAbsent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") }
                    }
                }
            }
            state.statusMessage?.let { msg ->
                Surface(color = StatusPresentBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = StatusPresent, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessages() }) { Text("OK") }
                    }
                }
            }
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Institute Profile
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Institute Profile", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = instituteInput,
                        onValueChange = { instituteInput = it },
                        label = { Text("Institute Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.updateInstituteName(instituteInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) { Text("Save Name") }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { logoPicker.launch(arrayOf("image/*")) }) {
                            Text(if (settings.logoPath == null) "Set Logo" else "Change Logo")
                        }
                        if (settings.logoPath != null) {
                            OutlinedButton(onClick = { viewModel.clearLogo() }) { Text("Remove Logo") }
                        }
                    }
                }
            }

            // Theme & Attendance Rules
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance & Attendance Rules", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = thresholdInput,
                            onValueChange = { thresholdInput = it.filter(Char::isDigit) },
                            label = { Text("Low Attendance %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            thresholdInput.toIntOrNull()?.let { viewModel.updateThreshold(it) }
                        }) { Text("Set") }
                    }
                    Text("Leave Formula", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.leaveRule == LeaveRule.INCLUDE_LEAVE,
                            onClick = { viewModel.updateLeaveRule(LeaveRule.INCLUDE_LEAVE) },
                            label = { Text("P / (P+A+L)") }
                        )
                        FilterChip(
                            selected = settings.leaveRule == LeaveRule.EXCLUDE_LEAVE,
                            onClick = { viewModel.updateLeaveRule(LeaveRule.EXCLUDE_LEAVE) },
                            label = { Text("P / (P+A)") }
                        )
                    }
                }
            }

            // App Lock
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App Lock", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PIN Lock", fontWeight = FontWeight.Medium)
                            Text(
                                if (settings.appLockEnabled) "Enabled (4–6 digit salted PBKDF2 hash)" else "Disabled",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (settings.appLockEnabled) {
                            OutlinedButton(onClick = { viewModel.disablePin() }) { Text("Disable") }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Button(onClick = { showPinDialog = true }) {
                            Text(if (settings.appLockEnabled) "Change" else "Set PIN")
                        }
                    }
                    if (settings.appLockEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Fingerprint Unlock", modifier = Modifier.weight(1f))
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) }
                            )
                        }
                    }
                }
            }

            // Backup & Restore + Copy Semester
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data & Semesters", style = MaterialTheme.typography.titleMedium)
                    val lastBackup = if (settings.lastBackupTimestamp == 0L) "Never" else DateUtils.formatDateTime(settings.lastBackupTimestamp)
                    Text("Last Backup: $lastBackup", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showBackupWarning = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) { Text("Backup (.zip)") }
                        OutlinedButton(
                            onClick = { openRestoreLauncher.launch(arrayOf("application/zip")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Restore") }
                    }
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onOpenCopySemester,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Copy Students to Next Semester")
                    }
                }
            }
        }
    }
}
