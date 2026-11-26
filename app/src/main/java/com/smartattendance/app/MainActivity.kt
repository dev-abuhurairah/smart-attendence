package com.smartattendance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.data.local.prefs.AppSettings
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.ui.navigation.SmartAttendanceNavGraph
import com.smartattendance.app.ui.theme.SmartAttendanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsDataStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            SmartAttendanceTheme(themeMode = settings.themeMode) {
                SmartAttendanceNavGraph()
            }
        }
    }
}
