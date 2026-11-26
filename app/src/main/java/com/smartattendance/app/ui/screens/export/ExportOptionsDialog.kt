package com.smartattendance.app.ui.screens.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.export.ExportFormat
import com.smartattendance.app.export.ExportMode
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import com.smartattendance.app.ui.theme.StatusAbsentBg
import com.smartattendance.app.ui.theme.StatusLeave
import com.smartattendance.app.ui.theme.StatusPresent
import com.smartattendance.app.ui.theme.StatusPresentBg

@Composable
fun ExportOptionsDialog(
    sessionId: Long?,
    subjectId: Long,
    onDismiss: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showUpdateWarning by remember { mutableStateOf(false) }

    val createXlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.XLSX.mimeType)
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    val createPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.onSafTargetSelected(uri) else viewModel.clearPendingSaf() }

    LaunchedEffect(subjectId) {
        viewModel.setModeAndFormat(
            subjectId = subjectId,
            mode = if (sessionId != null) ExportMode.SINGLE_LECTURE else ExportMode.FULL_REGISTER,
            format = ExportFormat.PDF
        )
    }

    LaunchedEffect(state.pendingBundleForSaf) {
        val bundle = state.pendingBundleForSaf ?: return@LaunchedEffect
        if (state.needsNewLocationMessage != null) return@LaunchedEffect
        when (bundle.format) {
            ExportFormat.XLSX -> createXlsxLauncher.launch(bundle.suggestedName)
            ExportFormat.PDF -> createPdfLauncher.launch(bundle.suggestedName)
            ExportFormat.PNG -> openTreeLauncher.launch(null)
        }
    }

    LaunchedEffect(state.pendingShareBundle) {
        val bundle = state.pendingShareBundle ?: return@LaunchedEffect
        viewModel.launchShareIntent(context, bundle)
    }

    if (showUpdateWarning) {
        AlertDialog(
            onDismissRequest = { showUpdateWarning = false },
            title = { Text("Overwrite Existing File?") },
            text = {
                Text("Updating the existing register will rebuild it fresh from the database. Any manual edits made outside the app will be overwritten.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateWarning = false
                        viewModel.updateExistingRegister(sessionId, subjectId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) { Text("Update File") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateWarning = false }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Share") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sessionId != null) {
                        FilterChip(
                            selected = state.selectedMode == ExportMode.SINGLE_LECTURE,
                            onClick = { viewModel.setModeAndFormat(subjectId, ExportMode.SINGLE_LECTURE, state.selectedFormat) },
                            label = { Text("Single Lecture") }
                        )
                    }
                    FilterChip(
                        selected = state.selectedMode == ExportMode.FULL_REGISTER,
                        onClick = { viewModel.setModeAndFormat(subjectId, ExportMode.FULL_REGISTER, state.selectedFormat) },
                        label = { Text("Full Register") }
                    )
                }

                Text("Format", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = state.selectedFormat == fmt,
                            onClick = { viewModel.setModeAndFormat(subjectId, state.selectedMode, fmt) },
                            label = { Text(fmt.extension.uppercase()) }
                        )
                    }
                }

                if (state.isExporting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating export...")
                    }
                }

                state.needsNewLocationMessage?.let { msg ->
                    Surface(color = StatusAbsentBg, shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(msg, color = StatusAbsent, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val bundle = state.pendingBundleForSaf
                                    if (bundle != null) {
                                        when (bundle.format) {
                                            ExportFormat.XLSX -> createXlsxLauncher.launch(bundle.suggestedName)
                                            ExportFormat.PDF -> createPdfLauncher.launch(bundle.suggestedName)
                                            ExportFormat.PNG -> openTreeLauncher.launch(null)
                                        }
                                    }
                                }
                            ) { Text("Pick New Location") }
                        }
                    }
                }

                state.errorMessage?.let { msg ->
                    Surface(color = StatusAbsentBg, shape = RoundedCornerShape(8.dp)) {
                        Text(msg, color = StatusAbsent, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }

                state.statusMessage?.let { msg ->
                    Surface(color = StatusPresentBg, shape = RoundedCornerShape(8.dp)) {
                        Text(msg, color = StatusPresent, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }

                HorizontalDivider()

                if (state.selectedMode == ExportMode.FULL_REGISTER && state.existingRegisterEntry != null) {
                    Button(
                        onClick = { showUpdateWarning = true },
                        enabled = !state.isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Update Existing File")
                    }
                    Text(
                        "Last saved: ${state.existingRegisterEntry?.fileName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusLeave,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { viewModel.prepareExportForSaveAsNew(sessionId, subjectId) },
                    enabled = !state.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(
                        if (state.selectedFormat == ExportFormat.PNG) "Save PNG Pages to Folder"
                        else "Save as New Copy"
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.prepareQuickShare(sessionId, subjectId) },
                    enabled = !state.isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share via Apps")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
