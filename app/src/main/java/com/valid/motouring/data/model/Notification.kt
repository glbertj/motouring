package com.valid.motouring.data.model

enum class NotificationType { RIDE_INVITE, BADGE_EARNED, CHALLENGE_PROGRESS, SOCIAL }

data class Notification(
    val id: String,
    val type: NotificationType,
    val message: String,
    val createdAtEpochSeconds: Long,
    val isRead: Boolean,
)
