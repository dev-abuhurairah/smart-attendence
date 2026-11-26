package com.smartattendance.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.smartattendance.app.ui.theme.SmartAttendanceTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeAndBasicContent_renderCleanly() {
        composeRule.setContent {
            SmartAttendanceTheme {
                androidx.compose.material3.Text("Smart Attendance")
            }
        }
        composeRule.onNodeWithText("Smart Attendance").assertIsDisplayed()
    }
}
