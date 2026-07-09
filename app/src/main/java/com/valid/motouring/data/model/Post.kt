package com.valid.motouring.data.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarRes: Int,
    val photoResList: List<Int>,
    val caption: String,
    val attachedRideId: String?,
    val likeCount: Int,
    val likedByMe: Boolean,
    val commentIds: List<String>,
    val createdAtEpochSeconds: Long,
)
