package com.valid.motouring.ui.components.map

import com.valid.motouring.ui.theme.MotouringColors
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerStyleTest {

    @Test
    fun `role styles map to the analog-dash role colors`() {
        assertEquals(MotouringColors.goal, MarkerStyle.LEAD.color())
        assertEquals(MotouringColors.poiRest, MarkerStyle.SWEEP.color())
        assertEquals(MotouringColors.rider, MarkerStyle.RIDER.color())
        assertEquals(MotouringColors.riderCoral, MarkerStyle.BEHIND.color())
    }
}
