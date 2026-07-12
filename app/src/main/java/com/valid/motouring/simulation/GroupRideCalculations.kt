package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.distanceKm

/** Self (index 0) leads; the last rider sweeps; everyone else rides. Roles are labels, not positions. */
fun assignInitialRoles(participants: List<RideParticipantState>): List<RideParticipantState> {
    val lastIndex = participants.lastIndex
    return participants.mapIndexed { index, p ->
        val role = when {
            index == 0 -> RiderRole.LEAD
            index == lastIndex && lastIndex > 0 -> RiderRole.SWEEP
            else -> RiderRole.RIDER
        }
        p.copy(role = role)
    }
}

/** Assign [role] to [userId]; if it is LEAD or SWEEP, demote whoever currently holds it (single-holder invariant). */
fun withRole(participants: List<RideParticipantState>, userId: String, role: RiderRole): List<RideParticipantState> =
    participants.map { p ->
        when {
            p.userId == userId -> p.copy(role = role)
            (role == RiderRole.LEAD || role == RiderRole.SWEEP) && p.role == role -> p.copy(role = RiderRole.RIDER)
            else -> p
        }
    }

/** Front-most rider (largest distance along route) first. */
fun sortedByPackPosition(participants: List<RideParticipantState>): List<RideParticipantState> =
    participants.sortedByDescending { it.distanceAlongRouteMeters }

/** Gap in metres from each rider to the rider immediately ahead; the front rider's gap is 0. */
fun gapsToAheadMeters(sorted: List<RideParticipantState>): List<Double> =
    sorted.mapIndexed { index, p ->
        if (index == 0) 0.0 else sorted[index - 1].distanceAlongRouteMeters - p.distanceAlongRouteMeters
    }

/** Nearest GAS_STATION POI to [from] by great-circle distance, or null if there are none. */
fun nearestGasStation(pois: List<PointOfInterest>, from: GeoPoint): PointOfInterest? =
    pois.filter { it.type == PoiType.GAS_STATION }.minByOrNull { distanceKm(from, it.location) }
