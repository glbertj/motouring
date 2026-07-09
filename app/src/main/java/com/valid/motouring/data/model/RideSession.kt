package com.valid.motouring.data.model

enum class RideSessionStatus { ACTIVE, ENDED }

data class RideParticipantState(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val position: GeoPoint,
    val isSpeaking: Boolean = false,
)

data class RideSession(
    val id: String,
    val vehicleType: VehicleType,
    val route: List<GeoPoint>,
    val participants: List<RideParticipantState>,
    val distanceMeters: Double,
    val speedKmh: Double,
    val elapsedSeconds: Long,
    val status: RideSessionStatus,
)
