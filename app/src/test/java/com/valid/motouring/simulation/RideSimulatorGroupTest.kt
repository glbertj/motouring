package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorGroupTest {

    private val route = listOf(
        GeoPoint(-6.2246, 106.8091),
        GeoPoint(-6.2153, 106.8149),
        GeoPoint(-6.2088, 106.8206),
        GeoPoint(-6.1976, 106.8235),
        GeoPoint(-6.1875, 106.8271),
    )

    private fun groupSession() = RideSession(
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
    )

    @Test
    fun `after enough ticks every participant has moved and the pack is spread`() {
        var s = groupSession()
        repeat(120) { s = RideSimulator.advance(s) }
        // front rider (self) is ahead of the sweep by a real gap
        val self = s.participants.first()
        val sweep = s.participants.last()
        assertTrue(self.distanceAlongRouteMeters > sweep.distanceAlongRouteMeters)
        assertTrue("pack should be spread", self.distanceAlongRouteMeters - sweep.distanceAlongRouteMeters > 100.0)
    }

    @Test
    fun `the sweep eventually falls behind`() {
        var s = groupSession()
        var everBehind = false
        repeat(200) {
            s = RideSimulator.advance(s)
            if (s.participants.last().hasFallenBehind) everBehind = true
        }
        assertTrue("sweep should cross the fall-behind threshold", everBehind)
    }

    @Test
    fun `the leader never falls behind`() {
        var s = groupSession()
        repeat(200) {
            s = RideSimulator.advance(s)
            assertTrue(!s.participants.first().hasFallenBehind)
        }
    }
}
