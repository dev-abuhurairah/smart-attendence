package com.smartattendance.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartattendance.app.data.local.prefs.AppSettings
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.ui.navigation.SmartAttendanceNavGraph
import com.smartattendance.app.ui.screens.applock.AppLockScreen
import com.smartattendance.app.ui.theme.SmartAttendanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsDataStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            var unlocked by rememberSaveable { mutableStateOf(false) }

            SmartAttendanceTheme(themeMode = settings.themeMode) {
                if (settings.appLockEnabled && !unlocked && settings.pinHash.isNotEmpty()) {
                    AppLockScreen(
                        settings = settings,
                        settingsDataStore = settingsDataStore,
                        onUnlocked = { unlocked = true }
                    )
                } else {
                    SmartAttendanceNavGraph()
                }
            }
        }
    }
}
