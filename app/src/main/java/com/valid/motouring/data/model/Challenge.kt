package com.valid.motouring.data.model

enum class ChallengeMetric { DISTANCE_KM, RIDE_COUNT }

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val progressValue: Double,
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val metric: ChallengeMetric,
    val goalValue: Double,
    val currentValue: Double,
    val deadlineEpochSeconds: Long,
    val leaderboard: List<LeaderboardEntry>,
)
