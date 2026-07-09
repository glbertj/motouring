package com.valid.motouring.data.repository

import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiRepositoryTest {

    @Test
    fun `filterByVehicleType only returns POIs supporting that vehicle type`() {
        val repo = PoiRepository()
        val motorcycleResults = repo.filterByVehicleType(VehicleType.MOTORCYCLE)

        assertTrue(motorcycleResults.isNotEmpty())
        assertTrue(motorcycleResults.all { VehicleType.MOTORCYCLE in it.supportedVehicleTypes })
    }

    @Test
    fun `filterByVehicleType excludes POIs that don't support that vehicle type`() {
        val repo = PoiRepository()
        val carResults = repo.filterByVehicleType(VehicleType.CAR)

        assertTrue(carResults.none { VehicleType.CAR !in it.supportedVehicleTypes })
    }
}
