package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepository {
    private val users = MutableStateFlow(FakeDataProvider.users)

    fun observeUsers(): StateFlow<List<User>> = users.asStateFlow()

    fun currentUser(): User =
        users.value.first { it.id == FakeDataProvider.currentUserId }

    fun userById(id: String): User? = users.value.firstOrNull { it.id == id }

    fun updateName(userId: String, name: String) {
        users.value = users.value.map { if (it.id == userId) it.copy(name = name) else it }
    }
}
