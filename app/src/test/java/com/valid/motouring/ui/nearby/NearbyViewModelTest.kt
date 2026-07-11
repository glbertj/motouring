package com.valid.motouring.ui.nearby

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.repository.PoiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyViewModelTest {

    private fun vm() = NearbyViewModel(PoiRepository(), FakeDataProvider.userLocation)

    @Test
    fun `default state lists all pois sorted by ascending distance`() {
        val state = vm().state.value
        assertEquals(PoiFilter.ALL, state.filter)
        assertEquals(FakeDataProvider.pois.size, state.items.size)
        val distances = state.items.map { it.distanceKm }
        assertEquals(distances.sorted(), distances)
    }

    @Test
    fun `filter REPAIR keeps only repair shops`() {
        val vm = vm()
        vm.setFilter(PoiFilter.REPAIR)
        val state = vm.state.value
        assertTrue(state.items.isNotEmpty())
        assertTrue(state.items.all { it.poi.type == com.valid.motouring.data.model.PoiType.REPAIR_SHOP })
    }

    @Test
    fun `selecting a poi sets selectedId, marks it, and moves the camera to it`() {
        val vm = vm()
        val target = vm.state.value.items[1].poi
        vm.select(target.id)
        val state = vm.state.value
        assertEquals(target.id, state.selectedId)
        assertEquals(target.location, state.cameraTarget)
        assertTrue(state.items.first { it.poi.id == target.id }.selected)
    }
}
