package com.example.neusoft_hospital.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.neusoft_hospital.feature.ai.presentation.AiChatScreen
import com.example.neusoft_hospital.feature.ai.presentation.ChatHistoryScreen
import com.example.neusoft_hospital.feature.appointment.presentation.*
import com.example.neusoft_hospital.feature.auth.presentation.*
import com.example.neusoft_hospital.feature.followup.presentation.*
import com.example.neusoft_hospital.feature.preconsult.presentation.*

data class BottomNavItem(
    val label: String,
    val selected: ImageVector,
    val unselected: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Filled.Home, Icons.Outlined.Home, Routes.AppointmentHome.route),
    BottomNavItem("AI问诊", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, Routes.PreConsultHome.route),
    BottomNavItem("AI助手", Icons.Filled.Chat, Icons.Outlined.Chat, Routes.AiChat.route),
    BottomNavItem("健康", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, Routes.FollowUpHome.route),
    BottomNavItem("我的", Icons.Filled.Person, Icons.Outlined.Person, Routes.Profile.route),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selected else item.unselected, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (selected) return@NavigationBarItem
                                android.util.Log.d("MainScaffold", "Tab click: ${item.label} currentRoute=$currentRoute")
                                // 1) 先把栈 pop 到首页，把之前 push 的子页面 (PreConsult/SymptomInput/TriageResult/...) 全部清掉
                                navController.popBackStack(Routes.AppointmentHome.route, inclusive = false)
                                // 2) 再 navigate 目标 tab
                                navController.navigate(item.route) {
                                    popUpTo(Routes.AppointmentHome.route) {
                                        saveState = true
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            // Auth
            composable(Routes.Login.route) { LoginScreen(navController) }
            composable(Routes.Register.route) { RegisterScreen(navController) }
            composable(Routes.Verify.route) { VerifyScreen(navController) }
            composable(Routes.FamilyManage.route) { FamilyManageScreen(navController) }
            composable(
                Routes.FamilyAdd.route,
                arguments = listOf(navArgument("memberId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { FamilyAddScreen(navController) }

            // Main Nav (bottom)
            composable(Routes.AppointmentHome.route) { AppointmentHomeScreen(navController) }
            composable(Routes.MyAppointments.route) { MyAppointmentsScreen(navController) }
            composable(Routes.Profile.route) { ProfileScreen(navController) }
            composable(
                Routes.DepartmentList.route,
                arguments = listOf(navArgument("parentId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { DepartmentListScreen(navController) }
            composable(
                Routes.DoctorList.route,
                arguments = listOf(navArgument("departmentId") { type = NavType.StringType })
            ) { DoctorListScreen(navController) }
            composable(
                Routes.DoctorDetail.route,
                arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
            ) { DoctorDetailScreen(navController) }
            composable(
                Routes.Schedule.route,
                arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
            ) { ScheduleScreen(navController) }
            composable(
                Routes.Booking.route,
                arguments = listOf(
                    navArgument("doctorId") { type = NavType.StringType },
                    navArgument("date") { type = NavType.StringType },
                    navArgument("slotId") { type = NavType.StringType }
                )
            ) { BookingScreen(navController) }
            composable(
                Routes.SmartRecommend.route,
                arguments = listOf(navArgument("symptoms") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) { SmartRecommendScreen(navController) }

            // PreConsult
            composable(Routes.PreConsultHome.route) { PreConsultHomeScreen(navController) }
            composable(
                Routes.SymptomInput.route,
                arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "text" })
            ) { SymptomInputScreen(navController) }
            composable(
                Routes.TriageResult.route,
                arguments = listOf(navArgument("symptoms") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) { TriageResultScreen(navController) }

            // AI Chat
            composable(
                Routes.AiChat.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { AiChatScreen(navController) }
            composable(Routes.ChatHistory.route) { ChatHistoryScreen(navController) }

            // FollowUp
            composable(Routes.FollowUpHome.route) { FollowUpHomeScreen(navController) }
            composable(
                Routes.FollowUpPlan.route,
                arguments = listOf(navArgument("planId") { type = NavType.StringType })
            ) { FollowUpPlanScreen(navController) }
            composable(
                Routes.RehabGuide.route,
                arguments = listOf(navArgument("disease") { type = NavType.StringType })
            ) { RehabGuideScreen(navController) }
            composable(
                Routes.ChronicDashboard.route,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { ChronicDashboardScreen(navController) }
            composable(
                Routes.ChronicInput.route,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { ChronicInputScreen(navController) }
            composable(Routes.ChronicAlerts.route) { ChronicAlertsScreen(navController) }
        }
    }
}
