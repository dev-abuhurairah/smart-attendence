package com.smartattendance.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.backup.BackupManager
import com.smartattendance.app.data.local.prefs.AppSettings
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    private val _messages = MutableStateFlow<Pair<String?, String?>>(null to null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.settingsFlow,
        _busy,
        _messages
    ) { settings, busy, msg ->
        SettingsUiState(
            settings = settings,
            isBusy = busy,
            statusMessage = msg.first,
            errorMessage = msg.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateInstituteName(name: String) = viewModelScope.launch {
        settingsDataStore.updateInstituteName(name)
        _messages.value = "Institute name saved." to null
    }

    fun saveLogoFromUri(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val res = withContext(Dispatchers.IO) {
            runCatching {
                val target = File(context.filesDir, "institute_logo.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not read selected image.")
                target.absolutePath
            }
        }
        _busy.value = false
        res.fold(
            onSuccess = { path ->
                settingsDataStore.updateLogoPath(path)
                _messages.value = "Institute logo updated." to null
            },
            onFailure = { err ->
                _messages.value = null to (err.message ?: "Failed to save logo.")
            }
        )
    }

    fun clearLogo() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            File(context.filesDir, "institute_logo.png").delete()
        }
        settingsDataStore.updateLogoPath(null)
        _messages.value = "Logo removed." to null
    }

    fun updateThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsDataStore.updateThemeMode(mode)
    }

    fun updateThreshold(threshold: Int) = viewModelScope.launch {
        settingsDataStore.updateAttendanceThreshold(threshold)
        _messages.value = "Attendance threshold updated to $threshold%." to null
    }

    fun updateLeaveRule(rule: LeaveRule) = viewModelScope.launch {
        settingsDataStore.updateLeaveRule(rule)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        if (pin.length !in 4..6 || !pin.all(Char::isDigit)) {
            _messages.value = null to "PIN must be 4 to 6 digits."
            return@launch
        }
        settingsDataStore.setPin(pin)
        _messages.value = "App Lock PIN enabled." to null
    }

    fun disablePin() = viewModelScope.launch {
        settingsDataStore.clearPin()
        _messages.value = "App Lock disabled." to null
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setBiometricEnabled(enabled)
    }

    fun createBackupToUri(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    backupManager.createBackupZip(out).getOrThrow()
                } ?: throw IllegalStateException("Unable to open destination file.")
            }
        }
        _busy.value = false
        result.fold(
            onSuccess = { _messages.value = "Backup created successfully." to null },
            onFailure = { err -> _messages.value = null to (err.message ?: "Backup failed.") }
        )
    }

    fun restoreBackupFromUri(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    backupManager.restoreBackupZip(input).getOrThrow()
                } ?: throw IllegalStateException("Unable to open backup file.")
            }
        }
        _busy.value = false
        result.fold(
            onSuccess = { _messages.value = "Backup restored successfully!" to null },
            onFailure = { err ->
                _messages.value = null to ("Restore failed (rolled back safely): ${err.message ?: "Invalid file"}")
            }
        )
    }

    fun clearMessages() {
        _messages.update { null to null }
    }
}
