package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceRecordTest {

    @Test
    fun `vehicle odometer defaults to zero`() {
        val v = Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, 0)
        assertEquals(0, v.odometerKm)
    }

    @Test
    fun `service item carries vehicle, type, last-serviced and interval`() {
        val item = ServiceItem("v-1", ServiceType.CHAIN, lastServicedKm = 11_850, intervalKm = 700)
        assertEquals("v-1", item.vehicleId)
        assertEquals(ServiceType.CHAIN, item.type)
        assertEquals(11_850, item.lastServicedKm)
        assertEquals(700, item.intervalKm)
    }
}
