package com.nafs.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nafs.app.R
import com.nafs.app.presentation.areas.LifeAreasScreen
import com.nafs.app.presentation.checkin.DailyCheckInScreen
import com.nafs.app.presentation.habitdetail.HabitDetailScreen
import com.nafs.app.presentation.habits.HabitEditorScreen
import com.nafs.app.presentation.habits.HabitsScreen

private data class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(TodayRoute, R.string.nav_today, Icons.Rounded.TaskAlt),
    TopLevelDestination(HabitsRoute, R.string.nav_habits, Icons.Rounded.Repeat),
    TopLevelDestination(AreasRoute, R.string.nav_areas, Icons.Rounded.Workspaces),
)

@Composable
fun NafsApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = topLevelDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.hasRoute(destination.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<TodayRoute> {
                DailyCheckInScreen(
                    onHabitClick = { id -> navController.navigate(HabitDetailRoute(id)) },
                )
            }
            composable<HabitsRoute> {
                HabitsScreen(
                    onHabitClick = { id -> navController.navigate(HabitDetailRoute(id)) },
                    onAddHabit = { navController.navigate(HabitEditorRoute()) },
                    onEditHabit = { id -> navController.navigate(HabitEditorRoute(habitId = id)) },
                )
            }
            composable<AreasRoute> {
                LifeAreasScreen()
            }
            composable<HabitDetailRoute> {
                HabitDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(HabitEditorRoute(habitId = id)) },
                )
            }
            composable<HabitEditorRoute> {
                HabitEditorScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
