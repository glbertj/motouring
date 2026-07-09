package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PoiRepository {
    private val pois = MutableStateFlow(FakeDataProvider.pois)

    fun observePois(): StateFlow<List<PointOfInterest>> = pois.asStateFlow()

    fun filterByVehicleType(type: VehicleType): List<PointOfInterest> =
        pois.value.filter { type in it.supportedVehicleTypes }
}
