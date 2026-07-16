package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideScore
import com.valid.motouring.data.model.SegmentTime

/**
 * Simulated ride score from the fake telemetry. Deterministic; illustrative coefficients (see the spec).
 * - lean: rewards speed spread (max−avg) and elevation-per-km (capped so the sim's large elevation can't dominate)
 * - smoothness: penalizes a large spread relative to top speed
 * - pace: rewards average speed above a walking-pace floor
 */
fun rideScore(maxSpeedKmh: Double, avgSpeedKmh: Double, elevationGainMeters: Double, distanceMeters: Double): RideScore {
    val spread = (maxSpeedKmh - avgSpeedKmh).coerceAtLeast(0.0)
    val km = (distanceMeters / 1000.0).coerceAtLeast(0.1)
    val elevPerKm = (elevationGainMeters / km).coerceAtMost(20.0)
    val lean = (35.0 + spread * 4.0 + elevPerKm).coerceIn(0.0, 100.0).toInt()
    val smoothness = (100.0 - (if (maxSpeedKmh > 0) spread / maxSpeedKmh else 0.0) * 220.0).coerceIn(0.0, 100.0).toInt()
    val pace = ((avgSpeedKmh - 15.0) * 4.0).coerceIn(0.0, 100.0).toInt()
    val overall = (lean + smoothness + pace) / 3
    val grade = when {
        overall >= 85 -> "A"
        overall >= 70 -> "B"
        overall >= 55 -> "C"
        else -> "D"
    }
    return RideScore(overall = overall, grade = grade, lean = lean, smoothness = smoothness, pace = pace)
}

fun sortedByTime(times: List<SegmentTime>): List<SegmentTime> = times.sortedBy { it.timeSeconds }

/** 1-based rank a [timeSeconds] would take on this board (number strictly faster, plus one). */
fun rankOf(timeSeconds: Int, times: List<SegmentTime>): Int = times.count { it.timeSeconds < timeSeconds } + 1
