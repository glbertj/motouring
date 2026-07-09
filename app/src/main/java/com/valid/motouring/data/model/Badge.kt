package com.valid.motouring.data.model

data class Badge(
    val id: String,
    val title: String,
    val iconRes: Int,
    val description: String,
    val unlockCriteria: String,
    val isEarned: Boolean,
    val earnedAtEpochSeconds: Long?,
)
