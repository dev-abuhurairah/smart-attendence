package com.smartattendance.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartattendance.app.ui.screens.attendance.TakeAttendanceScreen
import com.smartattendance.app.ui.screens.home.HomeScreen
import com.smartattendance.app.ui.screens.programs.ProgramsScreen
import com.smartattendance.app.ui.screens.programs.SemestersScreen
import com.smartattendance.app.ui.screens.programs.SubjectsScreen
import com.smartattendance.app.ui.screens.reports.ReportsScreen
import com.smartattendance.app.ui.screens.settings.SettingsScreen
import com.smartattendance.app.ui.screens.setup.CopySemesterScreen
import com.smartattendance.app.ui.screens.setup.ManageCatalogScreen
import com.smartattendance.app.ui.screens.setup.SetupWizardScreen
import com.smartattendance.app.ui.screens.sheets.AttendanceSheetsScreen
import com.smartattendance.app.ui.screens.sheets.SheetDetailScreen
import com.smartattendance.app.ui.theme.BluePrimary

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val MANAGE_CATALOG = "manage_catalog"
    const val PROGRAMS = "programs"
    const val SEMESTERS = "semesters/{programId}"
    const val SUBJECTS = "subjects/{semesterId}"
    const val TAKE_ATTENDANCE = "take_attendance/{subjectId}/{date}/{lectureNumber}"
    const val SHEETS = "sheets"
    const val SHEET_DETAIL = "sheet_detail/{sessionId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val COPY_SEMESTER = "copy_semester"

    fun semesters(programId: Long) = "semesters/$programId"
    fun subjects(semesterId: Long) = "subjects/$semesterId"
    fun takeAttendance(subjectId: Long, date: String, lectureNumber: Int) =
        "take_attendance/$subjectId/$date/$lectureNumber"
    fun sheetDetail(sessionId: Long) = "sheet_detail/$sessionId"
}

@Composable
fun SmartAttendanceNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSetup = { navController.navigate(Routes.SETUP) },
                onNavigateToPrograms = { navController.navigate(Routes.PROGRAMS) },
                onNavigateToSheets = { navController.navigate(Routes.SHEETS) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETUP) {
            SetupWizardScreen(
                onBack = { navController.popBackStack() },
                onOpenManageCatalog = { navController.navigate(Routes.MANAGE_CATALOG) },
                onSetupSavedNavigateToPrograms = {
                    navController.navigate(Routes.PROGRAMS) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(Routes.MANAGE_CATALOG) {
            ManageCatalogScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROGRAMS) {
            ProgramsScreen(
                onBack = { navController.popBackStack() },
                onProgramSelected = { programId ->
                    navController.navigate(Routes.semesters(programId))
                },
                onGoToSetup = { navController.navigate(Routes.SETUP) }
            )
        }
        composable(
            route = Routes.SEMESTERS,
            arguments = listOf(navArgument("programId") { type = NavType.LongType })
        ) { backStackEntry ->
            val programId = backStackEntry.arguments?.getLong("programId") ?: 0L
            SemestersScreen(
                programId = programId,
                onBack = { navController.popBackStack() },
                onSemesterSelected = { semesterId ->
                    navController.navigate(Routes.subjects(semesterId))
                }
            )
        }
        composable(
            route = Routes.SUBJECTS,
            arguments = listOf(navArgument("semesterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val semesterId = backStackEntry.arguments?.getLong("semesterId") ?: 0L
            SubjectsScreen(
                semesterId = semesterId,
                onBack = { navController.popBackStack() },
                onStartAttendance = { subjectId, date, lectureNumber ->
                    navController.navigate(Routes.takeAttendance(subjectId, date, lectureNumber))
                }
            )
        }
        composable(
            route = Routes.TAKE_ATTENDANCE,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.LongType },
                navArgument("date") { type = NavType.StringType },
                navArgument("lectureNumber") { type = NavType.IntType }
            )
        ) {
            TakeAttendanceScreen(
                onBack = { navController.popBackStack() },
                onSavedDone = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.SHEETS) {
            AttendanceSheetsScreen(
                onBack = { navController.popBackStack() },
                onOpenSheet = { sessionId ->
                    navController.navigate(Routes.sheetDetail(sessionId))
                }
            )
        }
        composable(
            route = Routes.SHEET_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            SheetDetailScreen(
                onBack = { navController.popBackStack() },
                onEditSheet = { subjectId, date, lectureNumber ->
                    navController.navigate(Routes.takeAttendance(subjectId, date, lectureNumber))
                }
            )
        }
        composable(Routes.REPORTS) {
            ReportsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCopySemester = { navController.navigate(Routes.COPY_SEMESTER) }
            )
        }
        composable(Routes.COPY_SEMESTER) {
            CopySemesterScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhasePlaceholderScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
