package com.valid.motouring.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.simulation.LifetimeTotals
import com.valid.motouring.simulation.PersonalRecords
import com.valid.motouring.simulation.VehicleSplit
import com.valid.motouring.simulation.WeekDistance
import com.valid.motouring.simulation.lifetimeTotals
import com.valid.motouring.simulation.personalRecords
import com.valid.motouring.simulation.rideScoreTrend
import com.valid.motouring.simulation.vehicleSplit
import com.valid.motouring.simulation.weeklyDistanceKm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InsightsState(
    val totals: LifetimeTotals,
    val weekly: List<WeekDistance>,
    val records: PersonalRecords,
    val split: VehicleSplit,
    val scoreTrend: List<Int>,
)

class InsightsViewModel(
    rideRepository: RideRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(build(rideRepository.observeHistory().value))
    val state: StateFlow<InsightsState> = _state.asStateFlow()

    private fun build(entries: List<com.valid.motouring.data.model.RideHistoryEntry>) = InsightsState(
        totals = lifetimeTotals(entries),
        weekly = weeklyDistanceKm(entries),
        records = personalRecords(entries),
        split = vehicleSplit(entries),
        scoreTrend = rideScoreTrend(entries),
    )

    companion object {
        fun factory(rideRepository: RideRepository) = viewModelFactory {
            initializer { InsightsViewModel(rideRepository) }
        }
    }
}
