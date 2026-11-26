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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartattendance.app.ui.screens.home.HomeScreen
import com.smartattendance.app.ui.theme.BluePrimary

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val PROGRAMS = "programs"
    const val SHEETS = "sheets"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
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
            PhasePlaceholderScreen(
                title = "Setup Wizard",
                subtitle = "Scheduled for Phase 2",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROGRAMS) {
            PhasePlaceholderScreen(
                title = "Programs",
                subtitle = "Scheduled for Phase 3",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SHEETS) {
            PhasePlaceholderScreen(
                title = "Attendance Sheets",
                subtitle = "Scheduled for Phase 4",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.REPORTS) {
            PhasePlaceholderScreen(
                title = "Reports",
                subtitle = "Scheduled for Phase 6",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            PhasePlaceholderScreen(
                title = "Settings",
                subtitle = "Scheduled for Phase 7",
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
