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
import com.smartattendance.app.security.PinHasher
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
    val biometricEnabled: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = "",
    val failedPinAttempts: Int = 0,
    val cooldownUntilMillis: Long = 0L
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
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val COOLDOWN_UNTIL = longPreferencesKey("cooldown_until")
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
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
            cooldownUntilMillis = prefs[Keys.COOLDOWN_UNTIL] ?: 0L
        )
    }

    suspend fun updateInstituteName(name: String) {
        context.dataStore.edit { it[Keys.INSTITUTE_NAME] = name.trim().ifEmpty { "My Institute" } }
    }

    suspend fun updateLogoPath(path: String?) {
        context.dataStore.edit {
            if (path == null) it.remove(Keys.LOGO_PATH) else it[Keys.LOGO_PATH] = path
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateAttendanceThreshold(threshold: Int) {
        context.dataStore.edit { it[Keys.ATTENDANCE_THRESHOLD] = threshold.coerceIn(1, 100) }
    }

    suspend fun updateLeaveRule(rule: LeaveRule) {
        context.dataStore.edit { it[Keys.LEAVE_RULE] = rule.name }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TS] = timestamp }
    }

    suspend fun dismissBackupReminder(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[Keys.BACKUP_REMINDER_DISMISSED_AT] = timestamp }
    }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin(pin, salt)
        context.dataStore.edit {
            it[Keys.PIN_SALT] = PinHasher.encodeSalt(salt)
            it[Keys.PIN_HASH] = hash
            it[Keys.APP_LOCK_ENABLED] = true
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.COOLDOWN_UNTIL] = 0L
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = false
            it[Keys.BIOMETRIC_ENABLED] = false
            it.remove(Keys.PIN_SALT)
            it.remove(Keys.PIN_HASH)
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.COOLDOWN_UNTIL] = 0L
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun recordPinFailure(nowMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val attempts = (prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0) + 1
            prefs[Keys.FAILED_PIN_ATTEMPTS] = attempts
            if (attempts >= 5) {
                prefs[Keys.COOLDOWN_UNTIL] = nowMillis + 30_000L
                prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
            }
        }
    }

    suspend fun resetPinFailures() {
        context.dataStore.edit {
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.COOLDOWN_UNTIL] = 0L
        }
    }
}
