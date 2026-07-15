package com.valid.motouring.data.model

enum class VehicleType { MOTORCYCLE, CAR }

data class Vehicle(
    val id: String,
    val ownerId: String,
    val type: VehicleType,
    val make: String,
    val model: String,
    val year: Int,
    val photoRes: Int,
    val odometerKm: Int = 0,
)
