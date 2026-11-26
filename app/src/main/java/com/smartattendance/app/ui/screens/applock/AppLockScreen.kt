package com.smartattendance.app.ui.screens.applock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.smartattendance.app.data.local.prefs.AppSettings
import com.smartattendance.app.data.local.prefs.SettingsDataStore
import com.smartattendance.app.security.PinHasher
import com.smartattendance.app.ui.theme.BlueLight
import com.smartattendance.app.ui.theme.BluePrimary
import com.smartattendance.app.ui.theme.StatusAbsent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    settings: AppSettings,
    settingsDataStore: SettingsDataStore,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val cooldownRemainingSec = ((settings.cooldownUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val inCooldown = cooldownRemainingSec > 0L

    LaunchedEffect(settings.cooldownUntilMillis) {
        while (System.currentTimeMillis() < settings.cooldownUntilMillis) {
            nowMillis = System.currentTimeMillis()
            delay(500)
        }
        nowMillis = System.currentTimeMillis()
    }

    val canUseBiometric = remember(settings.biometricEnabled) {
        settings.biometricEnabled &&
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun triggerBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    scope.launch {
                        settingsDataStore.resetPinFailures()
                        onUnlocked()
                    }
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Smart Attendance")
            .setSubtitle("Use your fingerprint or enter your PIN")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) triggerBiometric()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BluePrimary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(32.dp))
                }
                Text("Smart Attendance Locked", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Enter your 4–6 digit PIN to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !inCooldown,
                    modifier = Modifier.fillMaxWidth()
                )

                if (inCooldown) {
                    Text(
                        "Too many wrong attempts. Try again in ${cooldownRemainingSec}s.",
                        color = StatusAbsent,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (errorMsg != null) {
                    Text(errorMsg!!, color = StatusAbsent, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (pinInput.length < 4) {
                            errorMsg = "Enter at least 4 digits."
                            return@Button
                        }
                        if (PinHasher.verifyPin(pinInput, settings.pinSalt, settings.pinHash)) {
                            scope.launch {
                                settingsDataStore.resetPinFailures()
                                onUnlocked()
                            }
                        } else {
                            pinInput = ""
                            scope.launch {
                                settingsDataStore.recordPinFailure()
                                errorMsg = "Incorrect PIN."
                            }
                        }
                    },
                    enabled = !inCooldown,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Unlock")
                }

                if (canUseBiometric) {
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            Text("Unlock with Fingerprint")
                        }
                    }
                }
            }
        }
    }
}
