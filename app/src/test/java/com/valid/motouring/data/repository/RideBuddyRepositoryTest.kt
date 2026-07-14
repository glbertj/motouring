package com.valid.motouring.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideBuddyRepositoryTest {

    // Pick a friend that is NOT already flagged, so this is robust to FakeDataProvider
    // pre-flagging some friends (Task 10) — there is always at least one un-flagged friend.
    @Test
    fun `setTrusted flips a friend's membership in trustedContacts, filtered to flagged only`() {
        val repo = RideBuddyRepository()
        val friend = repo.friends().first { !it.isTrustedContact }
        assertFalse(repo.trustedContacts().any { it.user.id == friend.user.id })
        repo.setTrusted(friend.user.id, true)
        assertTrue(repo.trustedContacts().any { it.user.id == friend.user.id })
        assertTrue(repo.trustedContacts().all { it.isTrustedContact })
        repo.setTrusted(friend.user.id, false)
        assertFalse(repo.trustedContacts().any { it.user.id == friend.user.id })
    }
}
