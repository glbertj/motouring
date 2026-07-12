package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupRideCalculationsTest {

    private fun p(id: String, dist: Double = 0.0, role: RiderRole = RiderRole.RIDER) =
        RideParticipantState(id, id.uppercase(), 0, GeoPoint(0.0, 0.0), role = role, distanceAlongRouteMeters = dist)

    @Test
    fun `assignInitialRoles makes first Lead and last Sweep`() {
        val result = assignInitialRoles(listOf(p("u-me"), p("u-2"), p("u-3")))
        assertEquals(RiderRole.LEAD, result[0].role)
        assertEquals(RiderRole.RIDER, result[1].role)
        assertEquals(RiderRole.SWEEP, result[2].role)
    }

    @Test
    fun `assignInitialRoles on a solo rider leaves them Lead with no Sweep`() {
        val result = assignInitialRoles(listOf(p("u-me")))
        assertEquals(RiderRole.LEAD, result[0].role)
    }

    @Test
    fun `withRole assigns and demotes the previous holder of that role`() {
        val start = assignInitialRoles(listOf(p("u-me"), p("u-2"), p("u-3"))) // u-me LEAD
        val result = withRole(start, "u-2", RiderRole.LEAD)
        assertEquals(RiderRole.LEAD, result.first { it.userId == "u-2" }.role)
        assertEquals(RiderRole.RIDER, result.first { it.userId == "u-me" }.role) // old lead demoted
        assertEquals(RiderRole.SWEEP, result.first { it.userId == "u-3" }.role)  // untouched
    }

    @Test
    fun `sortedByPackPosition orders front-most first and gaps measure to the rider ahead`() {
        val sorted = sortedByPackPosition(listOf(p("back", 100.0), p("front", 500.0), p("mid", 300.0)))
        assertEquals(listOf("front", "mid", "back"), sorted.map { it.userId })
        assertEquals(listOf(0.0, 200.0, 200.0), gapsToAheadMeters(sorted))
    }

    @Test
    fun `nearestGasStation picks the closest gas station and ignores other poi types`() {
        val here = GeoPoint(-6.2088, 106.8206)
        val far = PointOfInterest("far", "Far Gas", PoiType.GAS_STATION, GeoPoint(-6.30, 106.90), setOf(VehicleType.CAR), 4.0)
        val near = PointOfInterest("near", "Near Gas", PoiType.GAS_STATION, GeoPoint(-6.2090, 106.8208), setOf(VehicleType.CAR), 4.0)
        val repair = PointOfInterest("rep", "Repair", PoiType.REPAIR_SHOP, here, setOf(VehicleType.CAR), 4.0)
        assertEquals("near", nearestGasStation(listOf(far, repair, near), here)?.id)
        assertNull(nearestGasStation(listOf(repair), here))
    }
}
