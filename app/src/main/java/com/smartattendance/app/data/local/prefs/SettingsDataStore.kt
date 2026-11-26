package com.smartattendance.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartattendance.app.domain.model.LeaveRule
import com.smartattendance.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_attendance_prefs")

data class AppSettings(
    val instituteName: String = "My Institute",
    val logoPath: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val attendanceThreshold: Int = 75,
    val leaveRule: LeaveRule = LeaveRule.INCLUDE_LEAVE,
    val lastBackupTimestamp: Long = 0L,
    val backupReminderDismissedAt: Long = 0L,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val INSTITUTE_NAME = stringPreferencesKey("institute_name")
        val LOGO_PATH = stringPreferencesKey("logo_path")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ATTENDANCE_THRESHOLD = intPreferencesKey("attendance_threshold")
        val LEAVE_RULE = stringPreferencesKey("leave_rule")
        val LAST_BACKUP_TS = longPreferencesKey("last_backup_ts")
        val BACKUP_REMINDER_DISMISSED_AT = longPreferencesKey("backup_reminder_dismissed_at")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            instituteName = prefs[Keys.INSTITUTE_NAME] ?: "My Institute",
            logoPath = prefs[Keys.LOGO_PATH],
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            attendanceThreshold = prefs[Keys.ATTENDANCE_THRESHOLD] ?: 75,
            leaveRule = prefs[Keys.LEAVE_RULE]?.let { runCatching { LeaveRule.valueOf(it) }.getOrNull() } ?: LeaveRule.INCLUDE_LEAVE,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L,
            backupReminderDismissedAt = prefs[Keys.BACKUP_REMINDER_DISMISSED_AT] ?: 0L,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }

    suspend fun updateInstituteName(name: String) {
        context.dataStore.edit { it[Keys.INSTITUTE_NAME] = name.trim().ifEmpty { "My Institute" } }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
