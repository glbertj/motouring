package com.valid.motouring.data.repository

import com.valid.motouring.data.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceRepositoryTest {

    @Test
    fun `itemsFor returns only that vehicle's items`() {
        val repo = MaintenanceRepository()
        val v1 = repo.itemsFor("v-1")
        assertTrue(v1.isNotEmpty())
        assertTrue(v1.all { it.vehicleId == "v-1" })
    }

    @Test
    fun `markServiced resets lastServicedKm to the given odometer for the matching item only`() {
        val repo = MaintenanceRepository()
        repo.markServiced("v-1", ServiceType.TIRES, atOdometerKm = 12_480)
        val tires = repo.itemsFor("v-1").first { it.type == ServiceType.TIRES }
        assertEquals(12_480, tires.lastServicedKm)
        // a different item is untouched
        val oil = repo.itemsFor("v-1").first { it.type == ServiceType.OIL }
        assertEquals(9_900, oil.lastServicedKm)
    }
}
