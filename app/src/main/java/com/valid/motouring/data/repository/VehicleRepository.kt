package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VehicleRepository {
    private val vehicles = MutableStateFlow(FakeDataProvider.vehicles)

    fun observeVehicles(): StateFlow<List<Vehicle>> = vehicles.asStateFlow()

    fun vehiclesFor(userId: String): List<Vehicle> =
        vehicles.value.filter { it.ownerId == userId }

    fun addVehicle(vehicle: Vehicle) {
        vehicles.value = vehicles.value + vehicle
    }
}
