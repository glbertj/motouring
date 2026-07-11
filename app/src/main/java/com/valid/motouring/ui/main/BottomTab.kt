package com.valid.motouring.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.valid.motouring.navigation.Destinations

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab(Destinations.HOME, "Home", Icons.Filled.Home)
    data object Nearby : BottomTab(Destinations.NEARBY, "Nearby", Icons.Filled.Place)
    data object Challenges : BottomTab(Destinations.CHALLENGES, "Challenges", Icons.Filled.EmojiEvents)
    data object Rides : BottomTab(Destinations.RIDES_HISTORY, "Rides", Icons.Filled.History)
    data object Profile : BottomTab(Destinations.PROFILE, "Profile", Icons.Filled.Person)

    companion object {
        // lazy: building this list eagerly in BottomTab's own <clinit> can race with a subclass
        // (e.g. Home) being touched first from outside, which forces Home's INSTANCE field to be
        // read here before it's assigned, yielding a null entry. Deferring the list construction
        // to first access avoids that JVM class-init reentrancy.
        val all: List<BottomTab> by lazy { listOf(Home, Nearby, Challenges, Rides, Profile) }
    }
}
