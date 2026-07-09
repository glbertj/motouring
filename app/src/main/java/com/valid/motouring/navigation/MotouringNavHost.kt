package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.challenges.BadgeDetailScreen
import com.valid.motouring.ui.challenges.BadgesScreen
import com.valid.motouring.ui.challenges.ChallengeDetailScreen
import com.valid.motouring.ui.main.MainScaffold
import com.valid.motouring.ui.onboarding.LoginScreen
import com.valid.motouring.ui.onboarding.OnboardingScreen
import com.valid.motouring.ui.onboarding.SplashScreen
import com.valid.motouring.ui.profile.EditProfileScreen
import com.valid.motouring.ui.social.CreatePostScreen
import com.valid.motouring.ui.social.FriendsScreen
import com.valid.motouring.ui.social.FriendsViewModel
import com.valid.motouring.ui.social.InviteRideScreen
import com.valid.motouring.ui.social.InviteRideViewModel
import com.valid.motouring.ui.social.PostDetailScreen
import com.valid.motouring.ui.social.PostViewModel
import com.valid.motouring.ui.rides.RideSummaryScreen
import com.valid.motouring.ui.rides.StartRideScreen
import com.valid.motouring.ui.vehicle.VehicleGarageSetupScreen
import com.valid.motouring.ui.vehicle.VehicleGarageViewModel

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Destinations.LOGIN) },
            )
        }
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Destinations.VEHICLE_GARAGE_SETUP) },
            )
        }
        composable(Destinations.VEHICLE_GARAGE_SETUP) {
            val viewModel: VehicleGarageViewModel = viewModel(
                factory = VehicleGarageViewModel.factory(
                    appContainer.vehicleRepository,
                    appContainer.userRepository.currentUser().id,
                ),
            )
            VehicleGarageSetupScreen(
                viewModel = viewModel,
                onVehicleAdded = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.MAIN) {
            MainScaffold(appContainer = appContainer, outerNavController = navController)
        }
        composable(Destinations.CREATE_POST) {
            val currentUser = appContainer.userRepository.currentUser()
            val viewModel: PostViewModel = viewModel(
                factory = PostViewModel.factory(
                    appContainer.postRepository,
                    appContainer.rideRepository,
                    currentUser.id,
                    currentUser.name,
                    currentUser.avatarRes,
                    postId = null,
                ),
            )
            CreatePostScreen(
                viewModel = viewModel,
                onPosted = { navController.popBackStack() },
            )
        }
        composable(
            Destinations.POST_DETAIL_PATTERN,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val postId = requireNotNull(backStackEntry.arguments?.getString("postId"))
            val currentUser = appContainer.userRepository.currentUser()
            val viewModel: PostViewModel = viewModel(
                factory = PostViewModel.factory(
                    appContainer.postRepository,
                    appContainer.rideRepository,
                    currentUser.id,
                    currentUser.name,
                    currentUser.avatarRes,
                    postId = postId,
                ),
            )
            PostDetailScreen(viewModel = viewModel)
        }
        composable(Destinations.FRIENDS) {
            val viewModel: FriendsViewModel = viewModel(
                factory = FriendsViewModel.factory(appContainer.rideBuddyRepository),
            )
            FriendsScreen(viewModel = viewModel)
        }
        composable(Destinations.INVITE_RIDE) {
            val viewModel: InviteRideViewModel = viewModel(
                factory = InviteRideViewModel.factory(appContainer.rideBuddyRepository),
            )
            InviteRideScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
        }
        composable(Destinations.START_RIDE) {
            val currentUser = appContainer.userRepository.currentUser()
            val vehicles = appContainer.vehicleRepository.vehiclesFor(currentUser.id)
            StartRideScreen(
                vehicles = vehicles,
                onInviteBuddiesClick = { navController.navigate(Destinations.INVITE_RIDE) },
                onStartRide = { vehicleType, isGroup ->
                    navController.navigate(Destinations.rideSession(vehicleType.name, isGroup))
                },
            )
        }
        composable(Destinations.EDIT_PROFILE) {
            val currentUser = appContainer.userRepository.currentUser()
            EditProfileScreen(
                initialName = currentUser.name,
                onSave = { newName ->
                    appContainer.userRepository.updateName(currentUser.id, newName)
                    navController.popBackStack()
                },
            )
        }
        composable(
            Destinations.CHALLENGE_DETAIL_PATTERN,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val challengeId = requireNotNull(backStackEntry.arguments?.getString("challengeId"))
            val challenge = appContainer.challengeRepository.challenge(challengeId)
            if (challenge != null) {
                ChallengeDetailScreen(challenge = challenge)
            }
        }
        composable(Destinations.BADGES) {
            val badges by appContainer.badgeRepository.observeBadges().collectAsState()
            BadgesScreen(
                badges = badges,
                onBadgeClick = { id -> navController.navigate(Destinations.badgeDetail(id)) },
            )
        }
        composable(
            Destinations.BADGE_DETAIL_PATTERN,
            arguments = listOf(navArgument("badgeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val badgeId = requireNotNull(backStackEntry.arguments?.getString("badgeId"))
            val badge = appContainer.badgeRepository.badge(badgeId)
            if (badge != null) {
                BadgeDetailScreen(badge = badge)
            }
        }
        composable(
            Destinations.RIDE_SUMMARY_PATTERN,
            arguments = listOf(navArgument("historyEntryId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val historyEntryId = requireNotNull(backStackEntry.arguments?.getString("historyEntryId"))
            val entry = appContainer.rideRepository.observeHistory().value.firstOrNull { it.id == historyEntryId }
            val earnedBadges = appContainer.badgeRepository.observeBadges().value.filter { it.isEarned }
            if (entry != null) {
                RideSummaryScreen(
                    entry = entry,
                    earnedBadges = earnedBadges,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
