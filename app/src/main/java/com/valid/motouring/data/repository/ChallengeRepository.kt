package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Challenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChallengeRepository {
    private val challenges = MutableStateFlow(FakeDataProvider.challenges)

    fun observeChallenges(): StateFlow<List<Challenge>> = challenges.asStateFlow()

    fun challenge(id: String): Challenge? = challenges.value.firstOrNull { it.id == id }
}
