package com.smartattendance.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smartattendance.app.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueDark,
    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = BlueLight,
    onSecondaryContainer = BlueDark,
    background = SoftSurface,
    onBackground = TextPrimaryLight,
    surface = CardWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = BlueLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = StatusAbsent,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5FA3FF),
    onPrimary = Color(0xFF041C3D),
    primaryContainer = BlueDark,
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF88BDFF),
    onSecondary = Color(0xFF041C3D),
    secondaryContainer = DarkBlueCard,
    onSecondaryContainer = TextPrimaryDark,
    background = DarkBlueBackground,
    onBackground = TextPrimaryDark,
    surface = DarkBlueSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkBlueCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBlueBorder,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF490000)
)

@Composable
fun SmartAttendanceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = SmartAttendanceTypography,
        content = content
    )
}
