package com.valid.motouring.data.model

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarRes: Int,
    val text: String,
    val createdAtEpochSeconds: Long,
)
