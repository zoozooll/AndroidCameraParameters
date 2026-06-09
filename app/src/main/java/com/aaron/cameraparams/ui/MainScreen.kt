package com.aaron.cameraparams.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aaron.cameraparams.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Overview : Screen("overview", "Overview", Icons.Default.Home)
    object Categories : Screen("categories", "Categories", Icons.Default.List)
    object RawJson : Screen("raw_json", "Raw (JSON)", Icons.Default.Settings)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Star)
    object ParameterDetail : Screen("detail/{parameterKey}", "Detail", Icons.Default.Info)
}

@Composable
fun MainScreen(viewModel: CameraViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Overview,
        Screen.Categories,
        Screen.RawJson,
        Screen.Favorites
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Overview.route, Modifier.padding(innerPadding)) {
            composable(Screen.Overview.route) { 
                OverviewScreen(viewModel, onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                }) 
            }
            composable(Screen.Categories.route) { 
                CategoriesScreen(viewModel, onNavigateToDetail = { key ->
                    navController.navigate("detail/$key")
                }) 
            }
            composable(Screen.RawJson.route) { RawJsonScreen(viewModel) }
            composable(Screen.Favorites.route) { FavoritesScreen() }
            composable(Screen.ParameterDetail.route) { backStackEntry ->
                val parameterKey = backStackEntry.arguments?.getString("parameterKey") ?: ""
                ParameterDetailScreen(viewModel, parameterKey, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun FavoritesScreen() {
    Surface {
        Text("Favorites coming soon...", modifier = Modifier.padding(16.dp))
    }
}
