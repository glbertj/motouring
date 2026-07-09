package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InviteRideViewModel(rideBuddyRepository: RideBuddyRepository) : ViewModel() {
    val friends: StateFlow<List<RideBuddy>> = MutableStateFlow(rideBuddyRepository.friends()).asStateFlow()

    private val _selectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIds: StateFlow<Set<String>> = _selectedUserIds.asStateFlow()

    fun toggleSelected(userId: String) {
        _selectedUserIds.update { current ->
            if (userId in current) current - userId else current + userId
        }
    }

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { InviteRideViewModel(rideBuddyRepository) }
        }
    }
}
