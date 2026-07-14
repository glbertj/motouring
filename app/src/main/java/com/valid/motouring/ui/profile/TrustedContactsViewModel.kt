package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrustedContactsViewModel(
    private val rideBuddyRepository: RideBuddyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(friends())
    val state: StateFlow<List<RideBuddy>> = _state.asStateFlow()

    private fun friends() = rideBuddyRepository.observeBuddies().value.filter { it.status == BuddyStatus.FRIEND }

    fun toggle(userId: String, trusted: Boolean) {
        rideBuddyRepository.setTrusted(userId, trusted)
        _state.value = friends()
    }

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { TrustedContactsViewModel(rideBuddyRepository) }
        }
    }
}
