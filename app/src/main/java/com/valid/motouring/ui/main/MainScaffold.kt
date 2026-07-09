package com.valid.motouring.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.valid.motouring.ui.challenges.ChallengesScreen
import com.valid.motouring.ui.challenges.ChallengesViewModel
import com.valid.motouring.ui.home.HomeScreen
import com.valid.motouring.ui.home.HomeViewModel
import com.valid.motouring.ui.profile.ProfileScreen
import com.valid.motouring.ui.profile.ProfileViewModel
import com.valid.motouring.ui.rides.RidesHistoryScreen
import com.valid.motouring.ui.theme.MotouringMotion

// Tab tasks (Nearby: Task 15, Challenges/Badges: Tasks 16-17, Rides: Task 18, Profile: Tasks 24-27)
// each add their own route to this set once their composable() entry below is wired in.
private val implementedTabRoutes = setOf(
    BottomTab.Home.route,
    BottomTab.Challenges.route,
    BottomTab.Rides.route,
    BottomTab.Profile.route,
)

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
                    val isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = isSelected,
                        enabled = tab.route in implementedTabRoutes,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1f,
                                animationSpec = MotouringMotion.press(),
                                label = "tabIconScale",
                            )
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                            )
                        },
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
            enterTransition = { fadeIn(animationSpec = MotouringMotion.comfy()) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
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
            composable(BottomTab.Challenges.route) {
                val viewModel: ChallengesViewModel = viewModel(
                    factory = ChallengesViewModel.factory(appContainer.challengeRepository, appContainer.badgeRepository),
                )
                ChallengesScreen(
                    viewModel = viewModel,
                    onChallengeClick = { id -> outerNavController.navigate(Destinations.challengeDetail(id)) },
                    onSeeAllBadgesClick = { outerNavController.navigate(Destinations.BADGES) },
                    onBadgeClick = { id -> outerNavController.navigate(Destinations.badgeDetail(id)) },
                )
            }
            composable(BottomTab.Rides.route) {
                val history by appContainer.rideRepository.observeHistory().collectAsState()
                RidesHistoryScreen(history = history)
            }
            composable(BottomTab.Profile.route) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.factory(
                        appContainer.userRepository,
                        appContainer.vehicleRepository,
                        appContainer.rideRepository,
                        appContainer.badgeRepository,
                    ),
                )
                ProfileScreen(
                    viewModel = viewModel,
                    onFriendsClick = { outerNavController.navigate(Destinations.FRIENDS) },
                    onEditProfileClick = { outerNavController.navigate(Destinations.EDIT_PROFILE) },
                    onSettingsClick = { outerNavController.navigate(Destinations.SETTINGS) },
                    onNotificationsClick = { outerNavController.navigate(Destinations.NOTIFICATIONS) },
                )
            }
        }
    }
}
