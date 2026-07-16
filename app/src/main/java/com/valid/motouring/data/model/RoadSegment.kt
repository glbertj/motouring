package com.valid.motouring.data.model

data class SegmentTime(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val timeSeconds: Int,
)

enum class Twistiness { MELLOW, FLOWING, TECHNICAL }

data class RoadSegment(
    val id: String,
    val name: String,
    val region: String,
    val lengthKm: Double,
    val twistiness: Twistiness,
    val routePreviewRes: Int,
    val leaderboard: List<SegmentTime>,
)

data class RideScore(
    val overall: Int,
    val grade: String,
    val lean: Int,
    val smoothness: Int,
    val pace: Int,
)

data class SegmentResult(
    val segmentName: String,
    val timeSeconds: Int,
    val rank: Int,
)
