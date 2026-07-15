package com.valid.motouring.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.data.repository.VehicleRepository
import com.valid.motouring.simulation.dueCount
import com.valid.motouring.simulation.kmSinceService
import com.valid.motouring.simulation.serviceProgressFraction
import com.valid.motouring.simulation.serviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceItemUi(
    val item: ServiceItem,
    val status: ServiceStatus,
    val kmSince: Int,
    val progress: Float,
)

data class MaintenanceState(
    val vehicle: Vehicle?,
    val items: List<ServiceItemUi>,
    val dueCount: Int,
)

class VehicleMaintenanceViewModel(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<MaintenanceState> = _state.asStateFlow()

    private fun compute(): MaintenanceState {
        val vehicle = vehicleRepository.observeVehicles().value.firstOrNull { it.id == vehicleId }
        val odo = vehicle?.odometerKm ?: 0
        val items = maintenanceRepository.itemsFor(vehicleId)
        val ui = items.map {
            ServiceItemUi(
                item = it,
                status = serviceStatus(odo, it.lastServicedKm, it.intervalKm),
                kmSince = kmSinceService(odo, it.lastServicedKm),
                progress = serviceProgressFraction(odo, it.lastServicedKm, it.intervalKm),
            )
        }
        return MaintenanceState(vehicle, ui, dueCount(items, odo))
    }

    fun markServiced(type: ServiceType) {
        val odo = _state.value.vehicle?.odometerKm ?: return
        maintenanceRepository.markServiced(vehicleId, type, odo)
        _state.value = compute()
    }

    fun setOdometer(km: Int) {
        vehicleRepository.setOdometer(vehicleId, km)
        _state.value = compute()
    }

    companion object {
        fun factory(
            vehicleRepository: VehicleRepository,
            maintenanceRepository: MaintenanceRepository,
            vehicleId: String,
        ) = viewModelFactory {
            initializer { VehicleMaintenanceViewModel(vehicleRepository, maintenanceRepository, vehicleId) }
        }
    }
}
