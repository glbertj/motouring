package com.valid.motouring.simulation

import com.valid.motouring.data.model.GoalType
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.LegEndReason
import com.valid.motouring.data.model.RideGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSessionCalculationsTest {

    @Test
    fun `single short leg with no goal returns a generic title`() {
        val legs = listOf(Leg(goal = null, distanceMeters = 50.0, durationSeconds = 20, avgSpeedKmh = 9.0, endReason = LegEndReason.RIDE_ENDED))
        assertEquals("Ride", composeTripTitle(legs))
    }

    @Test
    fun `single goal leg followed by a meaningful free tail composes an arrow title`() {
        val goal = RideGoal(GoalType.DISTANCE, "25 km", 25_000.0)
        val legs = listOf(
            Leg(goal = goal, distanceMeters = 25_000.0, durationSeconds = 3000, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = null, distanceMeters = 2_000.0, durationSeconds = 300, avgSpeedKmh = 24.0, endReason = LegEndReason.RIDE_ENDED),
        )
        assertEquals("25 km → Free ride", composeTripTitle(legs))
    }

    @Test
    fun `three stops compose a multi-arrow title and drop a negligible tail`() {
        val goalA = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0)
        val goalB = RideGoal(GoalType.DESTINATION, "Warung Kopi Susu", 18_000.0)
        val legs = listOf(
            Leg(goal = goalA, distanceMeters = 10_000.0, durationSeconds = 1200, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = goalB, distanceMeters = 8_000.0, durationSeconds = 960, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = null, distanceMeters = 20.0, durationSeconds = 5, avgSpeedKmh = 14.0, endReason = LegEndReason.RIDE_ENDED),
        )
        assertEquals("10 km → Warung Kopi Susu", composeTripTitle(legs))
    }

    @Test
    fun `explorerBadgeEarned requires at least 3 goal legs`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", 5_000.0)
        val twoLegs = listOf(
            Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED),
            Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED),
        )
        assertFalse(explorerBadgeEarned(twoLegs))
        val threeLegs = twoLegs + Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED)
        assertTrue(explorerBadgeEarned(threeLegs))
    }

    @Test
    fun `neverEndingBadgeEarned requires 50km or more on a single goal-less leg`() {
        val short = listOf(Leg(null, 10_000.0, 1200, 30.0, LegEndReason.RIDE_ENDED))
        assertFalse(neverEndingBadgeEarned(short))
        val long = listOf(Leg(null, 50_500.0, 6000, 30.0, LegEndReason.RIDE_ENDED))
        assertTrue(neverEndingBadgeEarned(long))
    }
}
