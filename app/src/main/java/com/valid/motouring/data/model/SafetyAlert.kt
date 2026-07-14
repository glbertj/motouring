package com.valid.motouring.data.model

enum class SafetyAlertType { SOS, CRASH, RIDER_IN_TROUBLE }

enum class SafetyAlertStatus { ACTIVE, RESOLVED }

data class SafetyAlert(
    val id: String,
    val type: SafetyAlertType,
    val fromUserId: String,
    val fromName: String,
    val notifiedContactNames: List<String>,
    val respondingContactName: String? = null,
    val status: SafetyAlertStatus = SafetyAlertStatus.ACTIVE,
    val startedAtSeconds: Long,
)
