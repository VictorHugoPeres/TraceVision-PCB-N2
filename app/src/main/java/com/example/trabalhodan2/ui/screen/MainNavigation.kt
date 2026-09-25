package com.example.trabalhodan2.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trabalhodan2.ui.theme.TechBorder
import com.example.trabalhodan2.ui.theme.TechPrimary
import com.example.trabalhodan2.ui.theme.TechSurface
import com.example.trabalhodan2.viewmodel.InferenceViewModel

sealed class Screen(val route: String, val title: String) {
    object Detect : Screen("detect", "Detectar")
    object Result : Screen("result", "Resultado")
    object History : Screen("history", "Histórico")
    object Detail : Screen("detail", "Detalhes")
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: InferenceViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == Screen.Detect.route || currentRoute == Screen.History.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = TechSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp
                ) {
                    NavigationBar(
                        containerColor = TechSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.border(width = 0.5.dp, color = TechBorder)
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Detectar") },
                            label = { Text("Detectar") },
                            selected = currentRoute == Screen.Detect.route,
                            onClick = {
                                if (currentRoute != Screen.Detect.route) {
                                    navController.navigate(Screen.Detect.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TechPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = TechPrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
                            label = { Text("Histórico") },
                            selected = currentRoute == Screen.History.route,
                            onClick = {
                                if (currentRoute != Screen.History.route) {
                                    navController.navigate(Screen.History.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TechPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = TechPrimary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Detect.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Detect.route) {
                ConfigScreen(
                    viewModel = viewModel,
                    onNavigateToResult = {
                        navController.navigate(Screen.Result.route)
                    }
                )
            }
            composable(Screen.Result.route) {
                ResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDetails = {
                        navController.navigate(Screen.Detail.route)
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = {
                        navController.navigate(Screen.Detail.route)
                    }
                )
            }
            composable(Screen.Detail.route) {
                DetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
