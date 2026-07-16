package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicRouteRepositoryTest {

    @Test
    fun `routes are seeded and route finds by id`() {
        val repo = ScenicRouteRepository()
        assertTrue(repo.routes().isNotEmpty())
        val first = repo.routes().first()
        assertEquals(first, repo.route(first.id))
        assertNull(repo.route("nope"))
    }
}
