package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.VehicleType

data class LifetimeTotals(val distanceKm: Double, val rideCount: Int, val movingHours: Double)
data class WeekDistance(val weekIndex: Int, val distanceKm: Double)
data class PersonalRecords(val longestRideKm: Double, val fastestAvgKmh: Double, val bestScore: Int)
data class VehicleSplit(val motoKm: Double, val carKm: Double)

private const val WEEK_SECONDS = 604_800L

fun lifetimeTotals(entries: List<RideHistoryEntry>): LifetimeTotals = LifetimeTotals(
    distanceKm = entries.sumOf { it.distanceMeters } / 1000.0,
    rideCount = entries.size,
    movingHours = entries.sumOf { it.durationSeconds } / 3600.0,
)

/** Weekly km, bucketed by absolute week, with the min→max span zero-filled so the bar chart is contiguous. */
fun weeklyDistanceKm(entries: List<RideHistoryEntry>): List<WeekDistance> {
    if (entries.isEmpty()) return emptyList()
    val byWeek = entries
        .groupBy { (it.completedAtEpochSeconds / WEEK_SECONDS).toInt() }
        .mapValues { (_, es) -> es.sumOf { it.distanceMeters } / 1000.0 }
    return (byWeek.keys.min()..byWeek.keys.max()).map { w -> WeekDistance(w, byWeek[w] ?: 0.0) }
}

fun personalRecords(entries: List<RideHistoryEntry>): PersonalRecords = PersonalRecords(
    longestRideKm = (entries.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0,
    fastestAvgKmh = entries.maxOfOrNull { it.avgSpeedKmh } ?: 0.0,
    bestScore = entries.mapNotNull { it.rideScore?.overall }.maxOrNull() ?: 0,
)

fun vehicleSplit(entries: List<RideHistoryEntry>): VehicleSplit = VehicleSplit(
    motoKm = entries.filter { it.vehicleType == VehicleType.MOTORCYCLE }.sumOf { it.distanceMeters } / 1000.0,
    carKm = entries.filter { it.vehicleType == VehicleType.CAR }.sumOf { it.distanceMeters } / 1000.0,
)

fun rideScoreTrend(entries: List<RideHistoryEntry>): List<Int> =
    entries.filter { it.rideScore != null }
        .sortedBy { it.completedAtEpochSeconds }
        .map { it.rideScore!!.overall }
