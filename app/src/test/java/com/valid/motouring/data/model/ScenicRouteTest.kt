package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenicRouteTest {

    @Test
    fun `scenic route carries its vibe tags and route`() {
        val r = ScenicRoute(
            id = "sc-1", name = "Puncak Ridge", region = "Bogor", distanceKm = 42.0, estimatedMinutes = 95,
            vibe = listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), heroPhotoRes = 0,
            description = "Cool mountain air and switchbacks.",
            route = listOf(GeoPoint(-6.7, 106.9), GeoPoint(-6.6, 107.0)),
        )
        assertEquals(listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), r.vibe)
        assertEquals(2, r.route.size)
    }
}
