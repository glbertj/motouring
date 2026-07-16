package com.valid.motouring.ui.segments

import com.valid.motouring.data.repository.SegmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentDetailViewModelTest {

    @Test
    fun `state exposes the segment with a time-sorted board and the user's rank`() {
        val repo = SegmentRepository()
        val seg = repo.segments().first { s -> s.leaderboard.any { it.userId == "u-me" } }
        val vm = SegmentDetailViewModel(repo, seg.id, "u-me")
        val state = vm.state.value
        assertEquals(seg.id, state.segment?.id)
        // board is ascending by time
        val times = state.rankedBoard.map { it.timeSeconds }
        assertEquals(times.sorted(), times)
        // your rank matches your position in the sorted board (1-based)
        val yourTime = seg.leaderboard.first { it.userId == "u-me" }.timeSeconds
        assertEquals(state.rankedBoard.indexOfFirst { it.timeSeconds == yourTime } + 1, state.yourRank)
    }
}
