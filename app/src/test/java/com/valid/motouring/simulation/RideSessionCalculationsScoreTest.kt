package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSessionCalculationsScoreTest {

    @Test
    fun `toHistoryEntry attaches a computed ride score`() {
        val session = RideSession(
            id = "s", vehicleType = VehicleType.MOTORCYCLE,
            route = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.1, 0.1)),
            participants = listOf(RideParticipantState("u-me", "Rafi", 0, GeoPoint(0.0, 0.0))),
            distanceMeters = 12_000.0, speedKmh = 28.0, elapsedSeconds = 1_543,
            status = RideSessionStatus.ACTIVE, maxSpeedKmh = 34.0, elevationGainMeters = 300.0,
        )
        val entry = session.toHistoryEntry(id = "r", completedAtEpochSeconds = 0, routePreviewRes = 0)
        assertNotNull(entry.rideScore)
        assertTrue(entry.rideScore!!.overall in 0..100)
    }
}
