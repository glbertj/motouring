package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MaintenanceRepository {
    private val items = MutableStateFlow(FakeDataProvider.serviceItems)

    fun observeItems(): StateFlow<List<ServiceItem>> = items.asStateFlow()

    fun itemsFor(vehicleId: String): List<ServiceItem> =
        items.value.filter { it.vehicleId == vehicleId }

    fun markServiced(vehicleId: String, type: ServiceType, atOdometerKm: Int) {
        items.value = items.value.map {
            if (it.vehicleId == vehicleId && it.type == type) it.copy(lastServicedKm = atOdometerKm) else it
        }
    }
}
