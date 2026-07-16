package com.valid.motouring.ui.segments

import com.valid.motouring.data.repository.SegmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentsViewModelTest {

    private fun vm() = SegmentsViewModel(SegmentRepository(), "u-me")

    @Test
    fun `segments are exposed and yourBest finds the current user's time`() {
        val vm = vm()
        assertTrue(vm.segments.value.isNotEmpty())
        val withMe = vm.segments.value.first { seg -> seg.leaderboard.any { it.userId == "u-me" } }
        assertEquals("u-me", vm.yourBest(withMe)?.userId)
        val withoutMe = vm.segments.value.first { seg -> seg.leaderboard.none { it.userId == "u-me" } }
        assertNull(vm.yourBest(withoutMe))
    }
}
