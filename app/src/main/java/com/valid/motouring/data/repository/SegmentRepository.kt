package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RoadSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SegmentRepository {
    private val segments = MutableStateFlow(FakeDataProvider.segments)

    fun observeSegments(): StateFlow<List<RoadSegment>> = segments.asStateFlow()

    fun segments(): List<RoadSegment> = segments.value

    fun segment(id: String): RoadSegment? = segments.value.firstOrNull { it.id == id }
}
