package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.GoalType
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.toGoalMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorStatsTest {

    private fun session() = RideSession(
        id = "t", vehicleType = com.valid.motouring.data.model.VehicleType.MOTORCYCLE,
        route = listOf(GeoPoint(-6.22, 106.80), GeoPoint(-6.18, 106.83)),
        participants = listOf(RideParticipantState("u", "Me", 0, GeoPoint(-6.22, 106.80))),
        distanceMeters = 0.0, speedKmh = 0.0, elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `maxSpeed is the running maximum across ticks`() {
        var s = session()
        var seenMax = 0.0
        repeat(30) {
            s = RideSimulator.advance(s)
            seenMax = maxOf(seenMax, s.speedKmh)
            assertEquals(seenMax, s.maxSpeedKmh, 0.001)
        }
        assertTrue("max should be >= current speed", s.maxSpeedKmh >= s.speedKmh)
    }

    @Test
    fun `elevationGain never decreases`() {
        var s = session()
        var prev = 0.0
        repeat(30) {
            s = RideSimulator.advance(s)
            assertTrue("elevation should be monotonic non-decreasing", s.elevationGainMeters >= prev)
            prev = s.elevationGainMeters
        }
        assertTrue("some climb accumulates", s.elevationGainMeters > 0.0)
    }

    @Test
    fun `toGoalMeters is remaining distance, floored at zero`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", targetDistanceMeters = 5000.0)
        val s = session().copy(mode = RideMode.GOAL, activeGoal = goal, distanceMeters = 3000.0)
        assertEquals(2000.0, s.toGoalMeters(), 0.001)
        val past = s.copy(distanceMeters = 6000.0)
        assertEquals(0.0, past.toGoalMeters(), 0.001)
        assertEquals(0.0, session().toGoalMeters(), 0.001) // no goal -> 0
    }
}
