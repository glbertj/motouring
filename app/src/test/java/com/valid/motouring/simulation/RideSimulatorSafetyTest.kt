package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorSafetyTest {

    private val route = listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.1875, 106.8271))

    private fun groupSession() = RideSession(
        id = "s",
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
    )

    @Test
    fun `simulateHardStop emits HardStopDetected`() = runTest {
        val sim = RideSimulator(this, groupSession())
        val event = async { sim.events.first() }
        sim.simulateHardStop()
        assertEquals(RideSessionEvent.HardStopDetected, event.await())
    }

    @Test
    fun `simulateRiderInTrouble emits RiderInTrouble for the sweep`() = runTest {
        val sim = RideSimulator(this, groupSession())
        val event = async { sim.events.first() }
        sim.simulateRiderInTrouble()
        val e = event.await()
        assertTrue(e is RideSessionEvent.RiderInTrouble)
        assertEquals("Bagas", (e as RideSessionEvent.RiderInTrouble).participant.name)
    }
}
