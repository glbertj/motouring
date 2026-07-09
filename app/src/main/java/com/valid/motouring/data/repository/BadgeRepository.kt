package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Badge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BadgeRepository {
    private val badges = MutableStateFlow(FakeDataProvider.badges)

    fun observeBadges(): StateFlow<List<Badge>> = badges.asStateFlow()

    fun badge(id: String): Badge? = badges.value.firstOrNull { it.id == id }

    fun markEarned(id: String, earnedAtEpochSeconds: Long) {
        badges.value = badges.value.map {
            if (it.id == id) it.copy(isEarned = true, earnedAtEpochSeconds = earnedAtEpochSeconds) else it
        }
    }
}
