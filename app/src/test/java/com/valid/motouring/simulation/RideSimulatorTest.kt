package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.GoalType
import com.valid.motouring.data.model.LegEndReason
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorTest {

    private val route = listOf(
        GeoPoint(lat = -6.2246, lng = 106.8091),
        GeoPoint(lat = -6.2153, lng = 106.8149),
        GeoPoint(lat = -6.2088, lng = 106.8206),
    )

    private fun freshSession() = RideSession(
        id = "sim-test",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = listOf(
            RideParticipantState("u-me", "Rafi", 0, route.first(), isSpeaking = false),
            RideParticipantState("u-2", "Dinda", 0, route.first(), isSpeaking = false),
            RideParticipantState("u-3", "Bagas", 0, route.first(), isSpeaking = false),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `advance increases elapsed seconds by 1 each tick`() {
        val next = RideSimulator.advance(freshSession())
        assertEquals(1L, next.elapsedSeconds)
    }

    @Test
    fun `advance increases distance monotonically over repeated ticks`() {
        var session = freshSession()
        var previousDistance = session.distanceMeters
        repeat(20) {
            session = RideSimulator.advance(session)
            assertTrue(session.distanceMeters > previousDistance)
            previousDistance = session.distanceMeters
        }
    }

    @Test
    fun `advance moves the lead participant's position along the route`() {
        var session = freshSession()
        val startPosition = session.participants.first().position
        repeat(30) { session = RideSimulator.advance(session) }
        assertNotEquals(startPosition, session.participants.first().position)
    }

    @Test
    fun `advance rotates which participant is speaking over time`() {
        var session = freshSession()
        val speakingIndexes = mutableSetOf<Int>()
        repeat(30) {
            session = RideSimulator.advance(session)
            speakingIndexes.add(session.participants.indexOfFirst { it.isSpeaking })
        }
        assertTrue("expected more than one participant to speak over 30 ticks", speakingIndexes.size > 1)
    }

    @Test
    fun `advance on an ended session is a no-op`() {
        val ended = freshSession().copy(status = RideSessionStatus.ENDED)
        val next = RideSimulator.advance(ended)
        assertEquals(ended, next)
    }

    @Test
    fun `advance closes the leg and falls back to ENDLESS when the goal distance is crossed`() {
        val goal = RideGoal(GoalType.DISTANCE, "300 m", 300.0)
        var session = freshSession().copy(mode = RideMode.GOAL, activeGoal = goal)
        repeat(60) { session = RideSimulator.advance(session) }
        assertEquals(RideMode.ENDLESS, session.mode)
        assertEquals(null, session.activeGoal)
        assertEquals(1, session.completedLegs.size)
        assertEquals(LegEndReason.GOAL_REACHED, session.completedLegs.first().endReason)
        assertTrue(session.completedLegs.first().distanceMeters >= 300.0)
    }

    @Test
    fun `advance does not close a leg before the goal distance is reached`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", 5_000.0)
        var session = freshSession().copy(mode = RideMode.GOAL, activeGoal = goal)
        repeat(5) { session = RideSimulator.advance(session) }
        assertEquals(RideMode.GOAL, session.mode)
        assertTrue(session.completedLegs.isEmpty())
    }
}
