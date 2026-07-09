package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.User
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    userRepository: UserRepository,
    vehicleRepository: VehicleRepository,
    rideRepository: RideRepository,
    badgeRepository: BadgeRepository,
) : ViewModel() {

    val currentUser: User = userRepository.currentUser()

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.observeVehicles()
        .map { all -> all.filter { it.ownerId == currentUser.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), vehicleRepository.vehiclesFor(currentUser.id))

    val totalRides: StateFlow<Int> = rideRepository.observeHistory()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDistanceKm: StateFlow<Double> = rideRepository.observeHistory()
        .map { entries -> entries.sumOf { it.distanceMeters } / 1000.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val badges: StateFlow<List<Badge>> = badgeRepository.observeBadges()

    companion object {
        fun factory(
            userRepository: UserRepository,
            vehicleRepository: VehicleRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
        ) = viewModelFactory {
            initializer { ProfileViewModel(userRepository, vehicleRepository, rideRepository, badgeRepository) }
        }
    }
}
