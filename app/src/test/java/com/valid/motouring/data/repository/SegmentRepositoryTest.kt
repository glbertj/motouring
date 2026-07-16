package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentRepositoryTest {

    @Test
    fun `segments are seeded and segment finds by id`() {
        val repo = SegmentRepository()
        assertTrue(repo.segments().isNotEmpty())
        val first = repo.segments().first()
        assertEquals(first, repo.segment(first.id))
        assertNull(repo.segment("nope"))
    }
}
