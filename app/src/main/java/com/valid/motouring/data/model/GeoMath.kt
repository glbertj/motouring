package com.valid.motouring.data.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance between two points, in kilometers. */
fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
    val earthRadiusKm = 6_371.0
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * earthRadiusKm * asin(sqrt(h))
}
