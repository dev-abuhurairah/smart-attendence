package com.smartattendance.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val instituteName: String = "My Institute",
    val todayFormatted: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
    val activeProgramCount: Int = 0,
    val activeStudentCount: Int = 0,
    val showBackupReminder: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    companion object {
        private const val FOURTEEN_DAYS_MS = 14L * 24L * 60L * 60L * 1000L
    }

    val uiState: StateFlow<HomeUiState> = homeRepository.observeHomeSummary()
        .map { summary ->
            val now = System.currentTimeMillis()
            val backupDue = (now - summary.settings.lastBackupTimestamp) > FOURTEEN_DAYS_MS
            val notRecentlyDismissed = (now - summary.settings.backupReminderDismissedAt) > FOURTEEN_DAYS_MS
            val hasData = summary.activeProgramCount > 0 || summary.activeStudentCount > 0
            HomeUiState(
                instituteName = summary.instituteName,
                todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                activeProgramCount = summary.activeProgramCount,
                activeStudentCount = summary.activeStudentCount,
                showBackupReminder = hasData && backupDue && notRecentlyDismissed,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun dismissBackupReminder() {
        viewModelScope.launch {
            settingsDataStore.dismissBackupReminder()
        }
    }
}
