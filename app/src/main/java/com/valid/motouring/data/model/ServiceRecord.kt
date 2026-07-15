package com.valid.motouring.data.model

enum class ServiceType { OIL, CHAIN, TIRES, BRAKES, COOLANT }

enum class ServiceStatus { OK, DUE_SOON, OVERDUE }

data class ServiceItem(
    val vehicleId: String,
    val type: ServiceType,
    val lastServicedKm: Int,
    val intervalKm: Int,
)
