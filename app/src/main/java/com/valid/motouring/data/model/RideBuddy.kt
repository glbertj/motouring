package com.valid.motouring.data.model

enum class BuddyStatus { FRIEND, PENDING_SENT, PENDING_RECEIVED, NOT_CONNECTED }

data class RideBuddy(
    val user: User,
    val status: BuddyStatus,
    val isTrustedContact: Boolean = false,
)
