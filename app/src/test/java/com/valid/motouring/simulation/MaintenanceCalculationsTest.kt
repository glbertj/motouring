package com.valid.motouring.simulation

import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceCalculationsTest {

    @Test
    fun `serviceStatus is OK well within interval, DUE_SOON near it, OVERDUE at or past it`() {
        // interval 1000: OK below 850, DUE_SOON in [850,1000), OVERDUE at/above 1000
        assertEquals(ServiceStatus.OK, serviceStatus(odometerKm = 5_800, lastServicedKm = 5_000, intervalKm = 1_000))     // since 800
        assertEquals(ServiceStatus.DUE_SOON, serviceStatus(odometerKm = 5_900, lastServicedKm = 5_000, intervalKm = 1_000)) // since 900
        assertEquals(ServiceStatus.OVERDUE, serviceStatus(odometerKm = 6_000, lastServicedKm = 5_000, intervalKm = 1_000))  // since 1000
        assertEquals(ServiceStatus.OVERDUE, serviceStatus(odometerKm = 9_000, lastServicedKm = 5_000, intervalKm = 1_000))  // way past
    }

    @Test
    fun `kmSinceService never goes negative`() {
        assertEquals(0, kmSinceService(odometerKm = 100, lastServicedKm = 500))
        assertEquals(400, kmSinceService(odometerKm = 900, lastServicedKm = 500))
    }

    @Test
    fun `progress fraction is zero fresh and clamps to one when overdue`() {
        assertEquals(0f, serviceProgressFraction(5_000, 5_000, 1_000), 0.001f)
        assertEquals(0.5f, serviceProgressFraction(5_500, 5_000, 1_000), 0.001f)
        assertEquals(1f, serviceProgressFraction(9_000, 5_000, 1_000), 0.001f)
    }

    @Test
    fun `dueCount counts due-soon and overdue only`() {
        val items = listOf(
            ServiceItem("v", ServiceType.OIL, lastServicedKm = 4_800, intervalKm = 1_000),   // since 200 -> OK  (odo 5000)
            ServiceItem("v", ServiceType.CHAIN, lastServicedKm = 4_100, intervalKm = 1_000),  // since 900 -> DUE_SOON
            ServiceItem("v", ServiceType.TIRES, lastServicedKm = 3_000, intervalKm = 1_000),  // since 2000 -> OVERDUE
        )
        assertEquals(2, dueCount(items, odometerKm = 5_000))
    }
}
