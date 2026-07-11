package com.valid.motouring.ui.nearby

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PointOfInterest

enum class PoiFilter { ALL, FUEL, REPAIR, REST }

data class NearbyPoi(val poi: PointOfInterest, val distanceKm: Double, val selected: Boolean)

data class NearbyUiState(
    val items: List<NearbyPoi>,
    val filter: PoiFilter,
    val selectedId: String?,
    val cameraTarget: GeoPoint,
)
