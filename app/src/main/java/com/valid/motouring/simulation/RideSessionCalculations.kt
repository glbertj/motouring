package com.valid.motouring.simulation

import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.avgSpeedKmh

fun composeTripTitle(legs: List<Leg>): String {
    val goalLabels = legs.mapNotNull { it.goal?.label }
    val hasFreeTail = legs.lastOrNull()?.let { it.goal == null && it.distanceMeters >= 500.0 } == true
    return when {
        goalLabels.size >= 2 -> (goalLabels + if (hasFreeTail) listOf("Free ride") else emptyList()).joinToString(" → ")
        goalLabels.size == 1 -> if (hasFreeTail) "${goalLabels[0]} → Free ride" else "${goalLabels[0]} Ride"
        hasFreeTail -> "Free Ride"
        else -> "Ride"
    }
}

fun explorerBadgeEarned(legs: List<Leg>): Boolean = legs.count { it.goal != null } >= 3

fun neverEndingBadgeEarned(legs: List<Leg>): Boolean =
    legs.any { it.goal == null && it.distanceMeters >= 50_000.0 }

fun RideSession.toHistoryEntry(
    id: String,
    completedAtEpochSeconds: Long,
    routePreviewRes: Int,
): RideHistoryEntry = RideHistoryEntry(
    id = id,
    title = composeTripTitle(completedLegs),
    vehicleType = vehicleType,
    distanceMeters = distanceMeters,
    durationSeconds = elapsedSeconds,
    avgSpeedKmh = avgSpeedKmh(distanceMeters, elapsedSeconds),
    routePreviewRes = routePreviewRes,
    photoResList = emptyList(),
    completedAtEpochSeconds = completedAtEpochSeconds,
    legs = completedLegs,
)
