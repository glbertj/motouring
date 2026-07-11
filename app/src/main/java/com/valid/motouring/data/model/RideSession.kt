package com.valid.motouring.data.model

enum class RideSessionStatus { ACTIVE, ENDED }

enum class RideMode { GOAL, ENDLESS }

enum class GoalType { DISTANCE, DESTINATION }

data class RideGoal(
    val type: GoalType,
    val label: String,
    val targetDistanceMeters: Double,
)

enum class LegEndReason { GOAL_REACHED, DRIFTED, RIDE_ENDED }

data class Leg(
    val goal: RideGoal?,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val endReason: LegEndReason,
)

sealed interface RideSessionEvent {
    data class GoalReached(val leg: Leg) : RideSessionEvent
    object DriftedToEndless : RideSessionEvent
}

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
    val mode: RideMode = RideMode.ENDLESS,
    val activeGoal: RideGoal? = null,
    val completedLegs: List<Leg> = emptyList(),
    val maxSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
)

fun RideSession.activeLegDistanceMeters(): Double =
    distanceMeters - completedLegs.sumOf { it.distanceMeters }

fun RideSession.activeLegDurationSeconds(): Long =
    elapsedSeconds - completedLegs.sumOf { it.durationSeconds }

fun avgSpeedKmh(distanceMeters: Double, durationSeconds: Long): Double =
    if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else 0.0

fun RideSession.toGoalMeters(): Double =
    activeGoal?.let { (it.targetDistanceMeters - distanceMeters).coerceAtLeast(0.0) } ?: 0.0
