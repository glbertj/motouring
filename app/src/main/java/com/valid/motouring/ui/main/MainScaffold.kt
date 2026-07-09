package com.valid.motouring.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.navigation.Destinations
import com.valid.motouring.ui.home.HomeScreen
import com.valid.motouring.ui.home.HomeViewModel

// Tab tasks (Nearby: Task 15, Challenges/Badges: Tasks 16-17, Rides: Task 18, Profile: Tasks 24-27)
// each add their own route to this set once their composable() entry below is wired in.
private val implementedTabRoutes = setOf(BottomTab.Home.route)

@Composable
fun MainScaffold(
    appContainer: AppContainer,
    outerNavController: NavHostController,
) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = tabNavController.currentBackStackEntryAsState().value?.destination
                BottomTab.all.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        enabled = tab.route in implementedTabRoutes,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomTab.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(appContainer.postRepository, appContainer.challengeRepository),
                )
                HomeScreen(
                    viewModel = viewModel,
                    onStartRideClick = { outerNavController.navigate(Destinations.START_RIDE) },
                    onPostClick = { postId -> outerNavController.navigate(Destinations.postDetail(postId)) },
                    onCreatePostClick = { outerNavController.navigate(Destinations.CREATE_POST) },
                )
            }
        }
    }
}
