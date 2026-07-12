package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RideParticipantStateTest {

    @Test
    fun `new participant fields default to rider role, zero progress, not behind`() {
        val p = RideParticipantState("u-1", "Rafi", 0, GeoPoint(0.0, 0.0))
        assertEquals(RiderRole.RIDER, p.role)
        assertEquals(0.0, p.distanceAlongRouteMeters, 0.0)
        assertFalse(p.hasFallenBehind)
    }

    @Test
    fun `group signal carries a rally poi for fuel`() {
        val poi = PointOfInterest("p", "Shell", PoiType.GAS_STATION, GeoPoint(0.0, 0.0), setOf(VehicleType.CAR), 4.0)
        val signal = GroupSignal(GroupSignalType.FUEL, "u-1", "Rafi", rallyPoi = poi)
        assertEquals(poi, signal.rallyPoi)
        assertEquals("Rafi", signal.fromName)
    }
}
