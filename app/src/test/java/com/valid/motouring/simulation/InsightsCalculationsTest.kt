package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.RideScore
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculationsTest {

    private val WEEK = 604_800L

    private fun entry(km: Double, sec: Long, avg: Double, type: VehicleType, week: Long, score: Int?) =
        RideHistoryEntry(
            id = "e", title = "Ride", vehicleType = type, distanceMeters = km * 1000.0,
            durationSeconds = sec, avgSpeedKmh = avg, routePreviewRes = 0, photoResList = emptyList(),
            completedAtEpochSeconds = week * WEEK + 100,
            rideScore = score?.let { RideScore(it, "B", it, it, it) },
        )

    private val entries = listOf(
        entry(10.0, 1_800, 20.0, VehicleType.MOTORCYCLE, week = 100, score = 60),
        entry(30.0, 3_600, 30.0, VehicleType.CAR, week = 102, score = 80),   // week 101 is empty → zero-filled
        entry(20.0, 3_600, 24.0, VehicleType.MOTORCYCLE, week = 102, score = null),
    )

    @Test
    fun `lifetime totals sum distance, count, and moving hours`() {
        val t = lifetimeTotals(entries)
        assertEquals(60.0, t.distanceKm, 0.001)
        assertEquals(3, t.rideCount)
        assertEquals((1_800 + 3_600 + 3_600) / 3600.0, t.movingHours, 0.001)
    }

    @Test
    fun `weekly distance buckets by week and zero-fills the span`() {
        val w = weeklyDistanceKm(entries)
        assertEquals(listOf(100, 101, 102), w.map { it.weekIndex })
        assertEquals(listOf(10.0, 0.0, 50.0), w.map { it.distanceKm })
    }

    @Test
    fun `personal records take the max distance, avg speed, and score`() {
        val r = personalRecords(entries)
        assertEquals(30.0, r.longestRideKm, 0.001)
        assertEquals(30.0, r.fastestAvgKmh, 0.001)
        assertEquals(80, r.bestScore)
    }

    @Test
    fun `vehicle split sums distance per type`() {
        val s = vehicleSplit(entries)
        assertEquals(30.0, s.motoKm, 0.001) // 10 + 20
        assertEquals(30.0, s.carKm, 0.001)
    }

    @Test
    fun `score trend is ordered by time and skips score-less rides`() {
        assertEquals(listOf(60, 80), rideScoreTrend(entries))
    }
}
