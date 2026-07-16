package com.valid.motouring.ui.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RoadSegment
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.data.repository.SegmentRepository
import com.valid.motouring.simulation.rankOf
import com.valid.motouring.simulation.sortedByTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SegmentDetailState(
    val segment: RoadSegment?,
    val rankedBoard: List<SegmentTime>,
    val yourRank: Int?,
    val currentUserId: String,
)

class SegmentDetailViewModel(
    segmentRepository: SegmentRepository,
    segmentId: String,
    private val currentUserId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(build(segmentRepository.segment(segmentId)))
    val state: StateFlow<SegmentDetailState> = _state.asStateFlow()

    private fun build(segment: RoadSegment?): SegmentDetailState {
        val board = segment?.leaderboard?.let { sortedByTime(it) } ?: emptyList()
        val yourTime = board.firstOrNull { it.userId == currentUserId }?.timeSeconds
        val yourRank = yourTime?.let { rankOf(it, board) }
        return SegmentDetailState(segment, board, yourRank, currentUserId)
    }

    companion object {
        fun factory(segmentRepository: SegmentRepository, segmentId: String, currentUserId: String) = viewModelFactory {
            initializer { SegmentDetailViewModel(segmentRepository, segmentId, currentUserId) }
        }
    }
}
