package com.valid.motouring.simulation

import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus

/** Fraction of an interval consumed before an item flips from OK to "due soon". */
const val DUE_SOON_FRACTION = 0.85

fun kmSinceService(odometerKm: Int, lastServicedKm: Int): Int = (odometerKm - lastServicedKm).coerceAtLeast(0)

fun serviceStatus(odometerKm: Int, lastServicedKm: Int, intervalKm: Int): ServiceStatus {
    if (intervalKm <= 0) return ServiceStatus.OK
    val since = kmSinceService(odometerKm, lastServicedKm)
    return when {
        since >= intervalKm -> ServiceStatus.OVERDUE
        since >= intervalKm * DUE_SOON_FRACTION -> ServiceStatus.DUE_SOON
        else -> ServiceStatus.OK
    }
}

fun serviceProgressFraction(odometerKm: Int, lastServicedKm: Int, intervalKm: Int): Float {
    if (intervalKm <= 0) return 0f
    return (kmSinceService(odometerKm, lastServicedKm).toFloat() / intervalKm).coerceIn(0f, 1f)
}

fun dueCount(items: List<ServiceItem>, odometerKm: Int): Int =
    items.count { serviceStatus(odometerKm, it.lastServicedKm, it.intervalKm) != ServiceStatus.OK }
