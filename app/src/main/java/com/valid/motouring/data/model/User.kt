package com.valid.motouring.data.model

data class User(
    val id: String,
    val name: String,
    val avatarRes: Int,
    val vehicleIds: List<String>,
)
