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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import com.aaron.cameraparams.BuildConfig
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.theme.CameraParamsTheme
import com.aaron.cameraparams.ui.screens.*

import androidx.annotation.StringRes

sealed class Screen(val route: String, @StringRes val label: Int, val icon: Int) {
    object Overview : Screen("overview", R.string.nav_overview, R.drawable.ic_overview)
    object Categories : Screen("categories", R.string.nav_categories, R.drawable.ic_categories)
    object RawJson : Screen("raw_json", R.string.nav_raw_json, R.drawable.ic_raw_json)
    object Favorites : Screen("favorites", R.string.nav_favorites, R.drawable.ic_favorites)
    object ParameterDetail : Screen("detail/{parameterKey}?origin={origin}", R.string.nav_detail, R.drawable.ic_overview) // Using Overview icon as placeholder for detail
}

@Composable
fun CameraSelector(
    state: CameraHeaderState,
    onIntent: (CameraIntent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.url_privacy_policy)
    val playStoreUrl = stringResource(R.string.url_play_store)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.cd_menu),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_privacy_policy)) },
                onClick = {
                    menuExpanded = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                    context.startActivity(intent)
                }
            )
            
            if (BuildConfig.STORE_NAME == "Google Play") {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_rate_app)) },
                    onClick = {
                        menuExpanded = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                        context.startActivity(intent)
                    }
                )
            }

            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_about)) },
                onClick = {
                    menuExpanded = false
                    // Show a simple about dialog or navigate
                }
            )
        }

        Spacer(Modifier.width(8.dp))
        Text(
            state.cameraName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.cd_select_camera),
                modifier = Modifier.padding(4.dp)
            )
        }
        
        Spacer(Modifier.width(8.dp))

        Surface(
            color = Color(0xFF2C2E33),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(width = 48.dp, height = 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    state.cameraId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.cameras.forEachIndexed { index, camId ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.camera_label_format, camId)) },
                    onClick = {
                        onIntent(CameraIntent.SelectCamera(index))
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

    MainScreenContent(
        headerState = uiState.header,
        navController = navController,
        onIntent = { viewModel.handleIntent(it) }
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
fun MainScreenContent(
    headerState: CameraHeaderState,
    navController: NavHostController,
    onIntent: (CameraIntent) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
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
                Surface(
                    shadowElevation = 4.dp,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    CameraSelector(
                        state = headerState,
                        onIntent = onIntent
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { 
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
                    } == true

                    NavigationBarItem(
                        icon = { Icon(painterResource(screen.icon), contentDescription = null) },
                        label = { Text(stringResource(screen.label)) },
                        selected = isSelected,
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
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun FavoritesScreen() {
    Surface {
        Text(stringResource(R.string.favorites_coming_soon), modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CameraParamsTheme {
        val navController = rememberNavController()
        MainScreenContent(
            headerState = CameraHeaderState(
                cameraName = "Rear Camera",
                cameraId = "0",
                cameras = listOf("0", "1", "2")
            ),
            navController = navController,
            onIntent = {}
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.content_area_placeholder))
            }
        }
    }
}
