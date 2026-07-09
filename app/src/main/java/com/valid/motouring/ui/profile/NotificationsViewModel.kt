package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Notification
import com.valid.motouring.data.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow

class NotificationsViewModel(private val notificationRepository: NotificationRepository) : ViewModel() {
    val notifications: StateFlow<List<Notification>> = notificationRepository.observeNotifications()

    fun markRead(id: String) = notificationRepository.markRead(id)

    companion object {
        fun factory(notificationRepository: NotificationRepository) = viewModelFactory {
            initializer { NotificationsViewModel(notificationRepository) }
        }
    }
}
