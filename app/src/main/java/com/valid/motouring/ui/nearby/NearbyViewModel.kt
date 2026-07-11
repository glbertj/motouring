package com.valid.motouring.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.distanceKm
import com.valid.motouring.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyViewModel(
    private val poiRepository: PoiRepository,
    private val userLocation: GeoPoint,
) : ViewModel() {

    private val allPois: List<PointOfInterest> = poiRepository.observePois().value
    private var filter = PoiFilter.ALL
    private var selectedId: String? = null

    private val _state = MutableStateFlow(build())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    fun setFilter(f: PoiFilter) { filter = f; _state.value = build() }

    fun select(id: String) {
        selectedId = id
        _state.value = build()
    }

    fun clearSelection() { selectedId = null; _state.value = build() }

    private fun build(): NearbyUiState {
        val filtered = allPois
            .filter { poi ->
                when (filter) {
                    PoiFilter.ALL -> true
                    PoiFilter.FUEL -> poi.type == PoiType.GAS_STATION
                    PoiFilter.REPAIR -> poi.type == PoiType.REPAIR_SHOP
                    PoiFilter.REST -> poi.type == PoiType.REST_STOP
                }
            }
            .map { NearbyPoi(it, distanceKm(userLocation, it.location), it.id == selectedId) }
            .sortedBy { it.distanceKm }
        val camera = selectedId?.let { id -> allPois.firstOrNull { it.id == id }?.location } ?: userLocation
        return NearbyUiState(items = filtered, filter = filter, selectedId = selectedId, cameraTarget = camera)
    }

    companion object {
        fun factory(poiRepository: PoiRepository) = viewModelFactory {
            initializer { NearbyViewModel(poiRepository, FakeDataProvider.userLocation) }
        }
    }
}
