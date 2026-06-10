package com.aaron.cameraparams.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    object ParameterDetail : Screen("detail/{parameterKey}?origin={origin}", "Detail", Icons.Default.Info)
}

@Composable
fun CameraSelector(name: String, id: String, cameras: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Menu,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(4.dp)
        )
        
        Spacer(Modifier.width(8.dp))

        Surface(
            color = Color(0xFF2C2E33),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(width = 48.dp, height = 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cameras.forEachIndexed { index, camId ->
                DropdownMenuItem(
                    text = { Text("Camera $camId") },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: CameraViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val items = listOf(
        Screen.Overview,
        Screen.Categories,
        Screen.RawJson,
        Screen.Favorites
    )

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isMainTab = currentDestination?.hierarchy?.any { dest ->
                items.any { it.route == dest.route }
            } == true
            
            if (isMainTab) {
                Surface(shadowElevation = 4.dp) {
                    CameraSelector(
                        name = uiState.cameraName,
                        id = uiState.cameraId,
                        cameras = uiState.cameras,
                        onSelect = { viewModel.selectCamera(it) }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { 
                            val route = it.route ?: return@any false
                            if (route.startsWith("detail/")) {
                                val originArg = navBackStackEntry?.arguments?.getString("origin")
                                if (originArg != null) {
                                    screen.route == originArg
                                } else {
                                    screen == Screen.Categories
                                }
                            } else {
                                route == screen.route
                            }
                        } == true,
                        onClick = {
                            if (currentDestination?.route != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Overview.route, Modifier.padding(innerPadding)) {
            composable(Screen.Overview.route) { 
                OverviewScreen(viewModel, onNavigateToDetail = { key ->
                    navController.navigate("detail/$key?origin=${Screen.Overview.route}")
                }) 
            }
            composable(Screen.Categories.route) { 
                CategoriesScreen(viewModel, onNavigateToDetail = { key ->
                    navController.navigate("detail/$key?origin=${Screen.Categories.route}")
                }) 
            }
            composable(Screen.RawJson.route) { RawJsonScreen(viewModel) }
            composable(Screen.Favorites.route) { FavoritesScreen() }
            composable(
                route = Screen.ParameterDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("origin") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
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
