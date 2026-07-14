package com.valid.motouring.simulation

import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType

/** How far behind (metres of sweep drift) a rider must be before the transient regroup escalates to a safety alert. */
const val IN_TROUBLE_THRESHOLD_METERS = 700.0

fun isRiderInTrouble(sweepDriftMeters: Double): Boolean = sweepDriftMeters >= IN_TROUBLE_THRESHOLD_METERS

/** Single assembly point for every safety alert (SOS / crash / in-trouble), so the three drivers can't drift apart. */
fun buildSafetyAlert(
    id: String,
    type: SafetyAlertType,
    fromUserId: String,
    fromName: String,
    trustedContactNames: List<String>,
    startedAtSeconds: Long,
): SafetyAlert = SafetyAlert(
    id = id,
    type = type,
    fromUserId = fromUserId,
    fromName = fromName,
    notifiedContactNames = trustedContactNames,
    startedAtSeconds = startedAtSeconds,
)
