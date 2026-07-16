package com.valid.motouring.ui.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RoadSegment
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.data.repository.SegmentRepository
import kotlinx.coroutines.flow.StateFlow

class SegmentsViewModel(
    segmentRepository: SegmentRepository,
    private val currentUserId: String,
) : ViewModel() {

    val segments: StateFlow<List<RoadSegment>> = segmentRepository.observeSegments()

    fun yourBest(segment: RoadSegment): SegmentTime? =
        segment.leaderboard.firstOrNull { it.userId == currentUserId }

    companion object {
        fun factory(segmentRepository: SegmentRepository, currentUserId: String) = viewModelFactory {
            initializer { SegmentsViewModel(segmentRepository, currentUserId) }
        }
    }
}
