package com.valid.motouring.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.data.model.Post
import com.valid.motouring.data.repository.ChallengeRepository
import com.valid.motouring.data.repository.PostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val postRepository: PostRepository,
    challengeRepository: ChallengeRepository,
) : ViewModel() {

    val posts: StateFlow<List<Post>> = postRepository.observePosts()

    val featuredChallenge: StateFlow<Challenge?> = challengeRepository.observeChallenges()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = challengeRepository.observeChallenges().value.firstOrNull(),
        )

    fun toggleLike(postId: String) = postRepository.toggleLike(postId)

    companion object {
        fun factory(postRepository: PostRepository, challengeRepository: ChallengeRepository) = viewModelFactory {
            initializer { HomeViewModel(postRepository, challengeRepository) }
        }
    }
}
