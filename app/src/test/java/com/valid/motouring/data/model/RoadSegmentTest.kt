package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoadSegmentTest {

    @Test
    fun `road segment carries its leaderboard`() {
        val seg = RoadSegment(
            id = "seg-1", name = "Puncak Pass", region = "Bogor", lengthKm = 8.1,
            twistiness = Twistiness.TECHNICAL, routePreviewRes = 0,
            leaderboard = listOf(SegmentTime("u-me", "Rafi", 0, timeSeconds = 512)),
        )
        assertEquals(1, seg.leaderboard.size)
        assertEquals(512, seg.leaderboard.first().timeSeconds)
    }

    @Test
    fun `history entry defaults score and segment result to null`() {
        val entry = RideHistoryEntry("r", "Ride", VehicleType.MOTORCYCLE, 1000.0, 100, 30.0, 0, emptyList(), 0)
        assertNull(entry.rideScore)
        assertNull(entry.segmentResult)
    }
}
