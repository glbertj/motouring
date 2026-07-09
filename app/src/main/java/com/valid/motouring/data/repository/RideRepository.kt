package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RideHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideRepository {
    private val history = MutableStateFlow(
        FakeDataProvider.rideHistory.sortedByDescending { it.completedAtEpochSeconds }
    )

    fun observeHistory(): StateFlow<List<RideHistoryEntry>> = history.asStateFlow()

    fun addHistoryEntry(entry: RideHistoryEntry) {
        history.value = (listOf(entry) + history.value)
            .sortedByDescending { it.completedAtEpochSeconds }
    }
}
