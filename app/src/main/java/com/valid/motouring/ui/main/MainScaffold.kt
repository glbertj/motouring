package com.valid.motouring.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
import com.valid.motouring.ui.nearby.NearbyScreen
import com.valid.motouring.ui.nearby.NearbyViewModel
import com.valid.motouring.ui.profile.ProfileScreen
import com.valid.motouring.ui.profile.ProfileViewModel
import com.valid.motouring.ui.rides.RidesHistoryScreen
import com.valid.motouring.ui.theme.MotouringMotion
import com.valid.motouring.ui.theme.MutedDim

// The bottom bar shows Home / Nearby, a centered Start-Ride FAB, then Rides / Profile.
// Challenges is intentionally not on the bar -- it's reached via the trophy icon in the
// Home screen header (see the Home composable() entry below) but stays a full nested route.
private val leftTabs = listOf(BottomTab.Home, BottomTab.Nearby)
private val rightTabs = listOf(BottomTab.Rides, BottomTab.Profile)

@Composable
fun MainScaffold(
    appContainer: AppContainer,
    outerNavController: NavHostController,
) {
    val tabNavController = rememberNavController()
    var fabMenuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                MotouringBottomBar(
                    tabNavController = tabNavController,
                    fabMenuOpen = fabMenuOpen,
                    onFabToggle = { fabMenuOpen = !fabMenuOpen },
                )
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
                        onOpenChallenges = {
                            tabNavController.navigate(BottomTab.Challenges.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                composable(BottomTab.Nearby.route) {
                    val viewModel: NearbyViewModel = viewModel(factory = NearbyViewModel.factory(appContainer.poiRepository))
                    NearbyScreen(viewModel = viewModel)
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
                            appContainer.maintenanceRepository,
                        ),
                    )
                    ProfileScreen(
                        viewModel = viewModel,
                        onFriendsClick = { outerNavController.navigate(Destinations.FRIENDS) },
                        onEditProfileClick = { outerNavController.navigate(Destinations.EDIT_PROFILE) },
                        onSettingsClick = { outerNavController.navigate(Destinations.SETTINGS) },
                        onNotificationsClick = { outerNavController.navigate(Destinations.NOTIFICATIONS) },
                        onVehicleClick = { vehicleId -> outerNavController.navigate(Destinations.vehicleMaintenance(vehicleId)) },
                    )
                }
            }
        }

        if (fabMenuOpen) {
            RideActionMenu(
                onDismiss = { fabMenuOpen = false },
                onStartSolo = { fabMenuOpen = false; outerNavController.navigate(Destinations.START_RIDE) },
                onStartGroup = { fabMenuOpen = false; outerNavController.navigate(Destinations.START_RIDE) },
                onPlanRoute = { fabMenuOpen = false; outerNavController.navigate(Destinations.START_RIDE) },
            )
        }
    }
}

@Composable
private fun MotouringBottomBar(
    tabNavController: NavHostController,
    fabMenuOpen: Boolean,
    onFabToggle: () -> Unit,
) {
    val currentDestination = tabNavController.currentBackStackEntryAsState().value?.destination
    fun go(tab: BottomTab) = tabNavController.navigate(tab.route) {
        popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    Box {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                leftTabs.forEach { tab ->
                    TabItem(
                        tab = tab,
                        isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = { go(tab) },
                    )
                }
                Spacer(Modifier.width(64.dp)) // room for the FAB
                rightTabs.forEach { tab ->
                    TabItem(
                        tab = tab,
                        isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = { go(tab) },
                    )
                }
            }
        }
        StartRideFab(
            open = fabMenuOpen,
            onToggle = onFabToggle,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-22).dp),
        )
    }
}

@Composable
private fun TabItem(tab: BottomTab, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = MotouringMotion.press(),
        label = "tabIconScale",
    )
    val tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MutedDim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        )
        Text(tab.label, color = tint)
    }
}
