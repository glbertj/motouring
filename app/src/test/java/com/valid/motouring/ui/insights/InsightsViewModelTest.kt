package com.valid.motouring.ui.insights

import com.valid.motouring.data.repository.RideRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsViewModelTest {

    @Test
    fun `state aggregates the seeded ride history`() {
        val repo = RideRepository()
        val vm = InsightsViewModel(repo)
        val state = vm.state.value
        assertEquals(repo.observeHistory().value.size, state.totals.rideCount)
        assertTrue(state.totals.distanceKm > 0.0)
        assertTrue(state.weekly.isNotEmpty())
        assertTrue(state.scoreTrend.isNotEmpty())
    }
}
