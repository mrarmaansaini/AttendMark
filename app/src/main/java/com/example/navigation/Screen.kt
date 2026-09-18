package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Attendance : Screen("attendance")
    object Marks : Screen("marks")
    object Students : Screen("students")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
    object SendQueue : Screen("send_queue/{batchId}") {
        fun createRoute(batchId: String) = "send_queue/$batchId"
    }
}

enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.BarChart),
    ATTENDANCE("attendance", "Attendance", Icons.Default.CheckCircle),
    MARKS("marks", "Marks", Icons.Default.Assignment),
    STUDENTS("students", "Students", Icons.Default.Group),
    PROFILE("profile", "Profile", Icons.Default.Person)
}
