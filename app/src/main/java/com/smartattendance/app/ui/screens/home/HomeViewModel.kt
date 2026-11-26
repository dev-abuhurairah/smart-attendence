package com.smartattendance.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val instituteName: String = "My Institute",
    val todayFormatted: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
    val activeProgramCount: Int = 0,
    val activeStudentCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    homeRepository: HomeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = homeRepository.observeHomeSummary()
        .map { summary ->
            HomeUiState(
                instituteName = summary.instituteName,
                todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                activeProgramCount = summary.activeProgramCount,
                activeStudentCount = summary.activeStudentCount,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
