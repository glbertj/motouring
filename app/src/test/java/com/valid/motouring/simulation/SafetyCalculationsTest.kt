package com.valid.motouring.simulation

import com.valid.motouring.data.model.SafetyAlertStatus
import com.valid.motouring.data.model.SafetyAlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCalculationsTest {

    @Test
    fun `isRiderInTrouble is true at or above the threshold and false below`() {
        assertFalse(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS - 1))
        assertTrue(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS))
        assertTrue(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS + 50))
    }

    @Test
    fun `buildSafetyAlert lists contacts, sets type, and starts ACTIVE with no responder`() {
        val alert = buildSafetyAlert(
            id = "s1",
            type = SafetyAlertType.SOS,
            fromUserId = "u-me",
            fromName = "Rafi",
            trustedContactNames = listOf("Dinda", "Bagas"),
            startedAtSeconds = 500L,
        )
        assertEquals(SafetyAlertType.SOS, alert.type)
        assertEquals(listOf("Dinda", "Bagas"), alert.notifiedContactNames)
        assertEquals(SafetyAlertStatus.ACTIVE, alert.status)
        assertEquals(null, alert.respondingContactName)
        assertEquals("Rafi", alert.fromName)
    }
}
