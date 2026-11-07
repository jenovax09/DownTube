package com.snaptube.clone.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snaptube.clone.AppContainer
import com.snaptube.clone.ui.details.ChooseFormatScreen
import com.snaptube.clone.ui.home.HomeScreen
import com.snaptube.clone.ui.library.LibraryScreen
import com.snaptube.clone.ui.search.SearchScreen
import com.snaptube.clone.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val FORMAT = "format"
}

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, com.snaptube.clone.R.string.nav_home, Icons.Filled.Home),
    TabItem(Routes.SEARCH, com.snaptube.clone.R.string.nav_search, Icons.Filled.Search),
    TabItem(Routes.LIBRARY, com.snaptube.clone.R.string.nav_library, Icons.Filled.VideoLibrary),
    TabItem(Routes.SETTINGS, com.snaptube.clone.R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun SnapTubeNavHost(
    container: AppContainer,
    incomingLink: StateFlow<String?>,
    onConsumeLink: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute != Routes.FORMAT

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(androidx.compose.ui.res.stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    incomingLink = incomingLink,
                    onConsumeLink = onConsumeLink,
                    onResultUrl = { url -> navController.navigate("${Routes.FORMAT}?url=$url") }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onResultClick = { url -> navController.navigate("${Routes.FORMAT}?url=$url") }
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(container = container)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container)
            }
            composable("${Routes.FORMAT}?url={url}") { entry ->
                val url = entry.arguments?.getString("url").orEmpty()
                ChooseFormatScreen(
                    container = container,
                    url = url,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
