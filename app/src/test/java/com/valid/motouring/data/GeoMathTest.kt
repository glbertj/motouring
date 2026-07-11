package com.valid.motouring.data

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.distanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    @Test
    fun `distance between identical points is zero`() {
        val p = GeoPoint(-6.20, 106.82)
        assertEquals(0.0, distanceKm(p, p), 0.0001)
    }

    @Test
    fun `distance is symmetric and reasonable for nearby Jakarta points`() {
        val a = GeoPoint(-6.2246, 106.8091)
        val b = GeoPoint(-6.1875, 106.8271)
        val ab = distanceKm(a, b)
        val ba = distanceKm(b, a)
        assertEquals(ab, ba, 0.0001)
        // ~4-5 km apart; assert a sane range, not an exact value
        assertTrue("expected 3-6 km, got $ab", ab in 3.0..6.0)
    }
}
