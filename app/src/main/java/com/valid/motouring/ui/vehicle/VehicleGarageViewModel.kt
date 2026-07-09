package com.valid.motouring.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.VehicleRepository

class VehicleGarageViewModel(
    private val vehicleRepository: VehicleRepository,
    private val currentUserId: String,
) : ViewModel() {

    fun addVehicle(type: VehicleType, make: String, model: String, year: Int) {
        val photoRes = if (type == VehicleType.MOTORCYCLE) {
            R.drawable.ic_vehicle_motorcycle_placeholder
        } else {
            R.drawable.ic_vehicle_car_placeholder
        }
        vehicleRepository.addVehicle(
            Vehicle(
                id = "v-${System.currentTimeMillis()}",
                ownerId = currentUserId,
                type = type,
                make = make,
                model = model,
                year = year,
                photoRes = photoRes,
            ),
        )
    }

    companion object {
        fun factory(vehicleRepository: VehicleRepository, currentUserId: String) = viewModelFactory {
            initializer { VehicleGarageViewModel(vehicleRepository, currentUserId) }
        }
    }
}
