package com.valid.motouring.data.model

enum class PoiType { GAS_STATION, REPAIR_SHOP, REST_STOP }

data class PointOfInterest(
    val id: String,
    val name: String,
    val type: PoiType,
    val location: GeoPoint,
    val supportedVehicleTypes: Set<VehicleType>,
    val rating: Double,
)
