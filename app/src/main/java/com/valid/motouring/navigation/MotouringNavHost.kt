package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.onboarding.SplashScreen

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen()
        }
    }
}
