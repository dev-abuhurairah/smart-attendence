package com.smartattendance.app.data.repository

import com.smartattendance.app.data.local.db.dao.HomeSummaryDao
import com.smartattendance.app.data.local.prefs.AppSettings
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class HomeSummary(
    val instituteName: String,
    val activeProgramCount: Int,
    val activeStudentCount: Int,
    val settings: AppSettings
)

@Singleton
class HomeRepository @Inject constructor(
    private val homeSummaryDao: HomeSummaryDao,
    private val settingsDataStore: SettingsDataStore
) {
    fun observeHomeSummary(): Flow<HomeSummary> = combine(
        settingsDataStore.settingsFlow,
        homeSummaryDao.observeActiveProgramCount(),
        homeSummaryDao.observeActiveStudentCount()
    ) { settings, programCount, studentCount ->
        HomeSummary(
            instituteName = settings.instituteName,
            activeProgramCount = programCount,
            activeStudentCount = studentCount,
            settings = settings
        )
    }
}
