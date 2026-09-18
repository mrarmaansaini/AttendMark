package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.repository.AttendMarkRepository
import com.example.features.attendance.AttendanceScreen
import com.example.features.dashboard.DashboardScreen
import com.example.features.marks.MarksScreen
import com.example.features.onboarding.OnboardingScreen
import com.example.features.profile.ProfileScreen
import com.example.features.reports.ReportsScreen
import com.example.features.splash.SplashScreen
import com.example.features.students.StudentsScreen
import com.example.features.whatsapp.SendQueueScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    repository: AttendMarkRepository,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val profile by repository.profileFlow.collectAsState(initial = null)
    val activeClassId by repository.activeClassIdFlow.collectAsState(initial = null)
    val allClasses by repository.activeClasses.collectAsState(initial = emptyList())

    val activeClass = remember(allClasses, activeClassId) {
        allClasses.find { it.id == activeClassId } ?: allClasses.firstOrNull()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val startDestination = Screen.Splash.route

    val bottomNavRoutes = listOf(
        BottomNavItem.DASHBOARD.route,
        BottomNavItem.ATTENDANCE.route,
        BottomNavItem.MARKS.route,
        BottomNavItem.STUDENTS.route,
        BottomNavItem.PROFILE.route
    )

    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                // Floating Bottom Bar with Rounded Corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                ambientColor = Color.Black.copy(alpha = 0.1f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavItem.values().forEach { item ->
                                val isSelected = currentDestination?.route == item.route
                                FloatingNavItem(
                                    item = item,
                                    isSelected = isSelected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        val destination = if (profile?.isOnboarded == true) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    repository = repository,
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    repository = repository,
                    activeClass = activeClass,
                    allClasses = allClasses,
                    onSelectClass = { cls ->
                        scope.launch { repository.setActiveClassId(cls.id) }
                    },
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToMarks = { navController.navigate(Screen.Marks.route) },
                    onNavigateToStudents = { navController.navigate(Screen.Students.route) }
                )
            }

            composable(Screen.Attendance.route) {
                AttendanceScreen(
                    repository = repository,
                    activeClass = activeClass,
                    allClasses = allClasses,
                    onSelectClass = { cls ->
                        scope.launch { repository.setActiveClassId(cls.id) }
                    },
                    onNavigateToSendQueue = { batchId ->
                        navController.navigate(Screen.SendQueue.createRoute(batchId))
                    }
                )
            }

            composable(Screen.Marks.route) {
                MarksScreen(
                    repository = repository,
                    activeClass = activeClass,
                    allClasses = allClasses,
                    onSelectClass = { cls ->
                        scope.launch { repository.setActiveClassId(cls.id) }
                    },
                    onNavigateToSendQueue = { batchId ->
                        navController.navigate(Screen.SendQueue.createRoute(batchId))
                    }
                )
            }

            composable(Screen.Students.route) {
                StudentsScreen(
                    repository = repository,
                    activeClass = activeClass,
                    allClasses = allClasses,
                    onSelectClass = { cls ->
                        scope.launch { repository.setActiveClassId(cls.id) }
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    repository = repository,
                    activeClass = activeClass,
                    allClasses = allClasses,
                    onSelectClass = { cls ->
                        scope.launch { repository.setActiveClassId(cls.id) }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    onDataWiped = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.SendQueue.route,
                arguments = listOf(navArgument("batchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
                SendQueueScreen(
                    batchId = batchId,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val pillBgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillBgColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else inactiveColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}
