package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.GroupSignalType
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorDriversTest {

    private val route = listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.1875, 106.8271))

    private fun session(drift: Double = 0.0, regrouping: Boolean = false) = RideSession(
        id = "g",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = assignInitialRoles(
            listOf(
                RideParticipantState("u-me", "Rafi", 0, route.first()),
                RideParticipantState("u-2", "Dinda", 0, route.first()),
                RideParticipantState("u-3", "Bagas", 0, route.first()),
            ),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
        sweepDriftMeters = drift,
        isRegrouping = regrouping,
    )

    @Test
    fun `setRole moves the badge and demotes the old lead`() = runTest {
        val sim = RideSimulator(this, session())
        sim.setRole("u-2", RiderRole.LEAD)
        val ps = sim.session.value.participants
        assertEquals(RiderRole.LEAD, ps.first { it.userId == "u-2" }.role)
        assertEquals(RiderRole.RIDER, ps.first { it.userId == "u-me" }.role)
    }

    @Test
    fun `broadcastRegroup flags regrouping and emits a REGROUP signal`() = runTest {
        val sim = RideSimulator(this, session(drift = 500.0))
        val event = async { sim.events.first() }
        sim.broadcastRegroup()
        assertTrue(sim.session.value.isRegrouping)
        val signal = (event.await() as RideSessionEvent.GroupSignalRaised).signal
        assertEquals(GroupSignalType.REGROUP, signal.type)
        assertEquals("Rafi", signal.fromName)
    }

    @Test
    fun `regrouping closes the sweep drift over repeated ticks`() = runTest {
        // 600m at REGROUP_CLOSE_PER_TICK=60m/tick closes in exactly 10 ticks. We stop there rather
        // than over-running: once isRegrouping flips false, advance() correctly resumes the (separately
        // tested, Task 3) organic sweep-drift growth, which would push sweepDriftMeters back off zero.
        var s = session(drift = 600.0, regrouping = true)
        repeat(10) { s = RideSimulator.advance(s) }
        assertEquals(0.0, s.sweepDriftMeters, 0.001)
        assertTrue(!s.isRegrouping)
    }

    @Test
    fun `callFuel emits a FUEL signal carrying the rally poi`() = runTest {
        val poi = PointOfInterest("p1", "Pertamina", PoiType.GAS_STATION, route.first(), setOf(VehicleType.MOTORCYCLE), 4.3)
        val sim = RideSimulator(this, session())
        val event = async { sim.events.first() }
        sim.callFuel(poi)
        val signal = (event.await() as RideSessionEvent.GroupSignalRaised).signal
        assertEquals(GroupSignalType.FUEL, signal.type)
        assertEquals("Pertamina", signal.rallyPoi?.name)
    }

    @Test
    fun `forceSweepBehind drives the sweep past the threshold on the next tick`() = runTest {
        val sim = RideSimulator(this, session())
        sim.forceSweepBehind()
        assertTrue(sim.session.value.sweepDriftMeters >= 800.0)
        val next = RideSimulator.advance(sim.session.value)
        assertTrue(next.participants.last().hasFallenBehind)
    }
}
