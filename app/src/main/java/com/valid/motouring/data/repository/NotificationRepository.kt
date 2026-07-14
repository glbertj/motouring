package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationRepository {
    private val notifications = MutableStateFlow(FakeDataProvider.notifications)

    fun observeNotifications(): StateFlow<List<Notification>> = notifications.asStateFlow()

    fun add(notification: Notification) {
        notifications.value = listOf(notification) + notifications.value
    }

    fun markRead(id: String) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun unreadCount(): Int = notifications.value.count { !it.isRead }
}
