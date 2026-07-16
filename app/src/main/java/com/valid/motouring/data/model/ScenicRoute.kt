package com.valid.motouring.data.model

enum class ScenicVibe { COASTAL, MOUNTAIN, FOREST, URBAN }

data class ScenicRoute(
    val id: String,
    val name: String,
    val region: String,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val vibe: List<ScenicVibe>,
    val heroPhotoRes: Int,
    val description: String,
    val route: List<GeoPoint>,
)
