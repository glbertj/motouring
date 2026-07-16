package com.valid.motouring.simulation

import com.valid.motouring.data.model.SegmentTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringCalculationsTest {

    @Test
    fun `rideScore computes the documented sub-scores and grade for a known ride`() {
        // spread=6, km=12, elevPerKm=25→cap 20; lean=35+24+20=79; smoothness=100-(6/34)*220≈61; pace=(28-15)*4=52; overall=64→C
        val s = rideScore(maxSpeedKmh = 34.0, avgSpeedKmh = 28.0, elevationGainMeters = 300.0, distanceMeters = 12_000.0)
        assertEquals(79, s.lean)
        assertEquals(61, s.smoothness)
        assertEquals(52, s.pace)
        assertEquals(64, s.overall)
        assertEquals("C", s.grade)
    }

    @Test
    fun `rideScore clamps every sub-score to 0 to 100 on extreme input`() {
        // a very spiky ride: huge spread → lean caps at 100, smoothness floors at 0, pace caps at 100
        val s = rideScore(maxSpeedKmh = 200.0, avgSpeedKmh = 90.0, elevationGainMeters = 5_000.0, distanceMeters = 40_000.0)
        assertTrue(s.lean in 0..100 && s.smoothness in 0..100 && s.pace in 0..100 && s.overall in 0..100)
        assertEquals(100, s.pace)
        assertEquals(0, s.smoothness)
    }

    @Test
    fun `a smooth, spirited, fast ride grades an A`() {
        // spread=10, km=10, elevPerKm=20; lean=95, smoothness=60, pace=100 → overall=85 → A
        val s = rideScore(maxSpeedKmh = 55.0, avgSpeedKmh = 45.0, elevationGainMeters = 200.0, distanceMeters = 10_000.0)
        assertEquals(85, s.overall)
        assertEquals("A", s.grade)
    }

    @Test
    fun `higher speed spread yields a higher lean sub-score`() {
        val calm = rideScore(30.0, 28.0, 100.0, 10_000.0)
        val spirited = rideScore(45.0, 28.0, 100.0, 10_000.0)
        assertTrue(spirited.lean > calm.lean)
    }

    @Test
    fun `sortedByTime is ascending and rankOf is one-based`() {
        val times = listOf(
            SegmentTime("a", "A", 0, 300),
            SegmentTime("b", "B", 0, 200),
            SegmentTime("c", "C", 0, 250),
        )
        assertEquals(listOf(200, 250, 300), sortedByTime(times).map { it.timeSeconds })
        assertEquals(1, rankOf(190, times))
        assertEquals(2, rankOf(240, times)) // one faster (200)
        assertEquals(4, rankOf(400, times))
    }
}
