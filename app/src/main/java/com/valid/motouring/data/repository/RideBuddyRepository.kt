package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideBuddyRepository {
    private val buddies = MutableStateFlow(FakeDataProvider.rideBuddies)

    fun observeBuddies(): StateFlow<List<RideBuddy>> = buddies.asStateFlow()

    fun friends(): List<RideBuddy> =
        buddies.value.filter { it.status == BuddyStatus.FRIEND }

    fun trustedContacts(): List<RideBuddy> =
        buddies.value.filter { it.status == BuddyStatus.FRIEND && it.isTrustedContact }

    fun setTrusted(userId: String, trusted: Boolean) {
        buddies.value = buddies.value.map {
            if (it.user.id == userId) it.copy(isTrustedContact = trusted) else it
        }
    }

    fun updateStatus(userId: String, status: BuddyStatus) {
        buddies.value = buddies.value.map {
            if (it.user.id == userId) it.copy(status = status) else it
        }
    }
}
