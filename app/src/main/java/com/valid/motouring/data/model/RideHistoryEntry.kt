package com.valid.motouring.data.model

data class RideHistoryEntry(
    val id: String,
    val title: String,
    val vehicleType: VehicleType,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val routePreviewRes: Int,
    val photoResList: List<Int>,
    val completedAtEpochSeconds: Long,
    val legs: List<Leg> = emptyList(),
    val rideScore: RideScore? = null,
    val segmentResult: SegmentResult? = null,
)
