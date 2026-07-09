package com.valid.motouring.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.ChallengeRepository
import kotlinx.coroutines.flow.StateFlow

class ChallengesViewModel(
    challengeRepository: ChallengeRepository,
    badgeRepository: BadgeRepository,
) : ViewModel() {
    val challenges: StateFlow<List<Challenge>> = challengeRepository.observeChallenges()
    val badges: StateFlow<List<Badge>> = badgeRepository.observeBadges()

    companion object {
        fun factory(challengeRepository: ChallengeRepository, badgeRepository: BadgeRepository) = viewModelFactory {
            initializer { ChallengesViewModel(challengeRepository, badgeRepository) }
        }
    }
}
