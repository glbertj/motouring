package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.StateFlow

class FriendsViewModel(private val rideBuddyRepository: RideBuddyRepository) : ViewModel() {
    val buddies: StateFlow<List<RideBuddy>> = rideBuddyRepository.observeBuddies()

    fun accept(userId: String) = rideBuddyRepository.updateStatus(userId, BuddyStatus.FRIEND)

    fun sendRequest(userId: String) = rideBuddyRepository.updateStatus(userId, BuddyStatus.PENDING_SENT)

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { FriendsViewModel(rideBuddyRepository) }
        }
    }
}
