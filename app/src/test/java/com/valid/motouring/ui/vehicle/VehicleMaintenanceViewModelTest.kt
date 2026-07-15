package com.valid.motouring.ui.vehicle

import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.data.repository.VehicleRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleMaintenanceViewModelTest {

    private fun vm() = VehicleMaintenanceViewModel(VehicleRepository(), MaintenanceRepository(), "v-1")

    @Test
    fun `state exposes seeded items with computed status and due count`() {
        val state = vm().state.value
        assertEquals("v-1", state.vehicle?.id)
        assertEquals(ServiceStatus.OVERDUE, state.items.first { it.item.type == ServiceType.TIRES }.status)
        assertEquals(2, state.dueCount) // Tires overdue + Chain due-soon
    }

    @Test
    fun `markServiced resets an item to OK and lowers the due count`() {
        val vm = vm()
        vm.markServiced(ServiceType.TIRES)
        val state = vm.state.value
        assertEquals(ServiceStatus.OK, state.items.first { it.item.type == ServiceType.TIRES }.status)
        assertEquals(1, state.dueCount)
    }

    @Test
    fun `setOdometer recomputes status`() {
        val vm = vm()
        vm.setOdometer(99_999)
        // At 99,999 km every v-1 item is past its interval (oil since 90k≥6k, chain 88k≥700,
        // tires 97k≥10k, brakes 91,999≥15k) → all 4 OVERDUE.
        assertEquals(4, vm.state.value.dueCount)
    }
}
