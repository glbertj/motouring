package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafetyAlertTest {

    @Test
    fun `safety alert defaults to active with no responder`() {
        val a = SafetyAlert("a1", SafetyAlertType.SOS, "u-me", "Rafi", listOf("Dinda"), startedAtSeconds = 100L)
        assertEquals(SafetyAlertStatus.ACTIVE, a.status)
        assertNull(a.respondingContactName)
        assertEquals(listOf("Dinda"), a.notifiedContactNames)
    }

    @Test
    fun `ride buddy defaults to not a trusted contact`() {
        val buddy = RideBuddy(User("u-2", "Dinda", 0, emptyList()), BuddyStatus.FRIEND)
        assertEquals(false, buddy.isTrustedContact)
    }
}
