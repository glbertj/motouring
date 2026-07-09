package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeRepositoryTest {

    @Test
    fun `observeChallenges emits every seeded challenge`() {
        val repo = ChallengeRepository()
        assertEquals(3, repo.observeChallenges().value.size)
    }

    @Test
    fun `challenge returns the matching entry by id`() {
        val repo = ChallengeRepository()
        val found = repo.challenge("c-1")
        assertEquals("Ride 100km This Week", found?.title)
    }

    @Test
    fun `challenge returns null for an unknown id`() {
        val repo = ChallengeRepository()
        assertNull(repo.challenge("does-not-exist"))
    }
}
