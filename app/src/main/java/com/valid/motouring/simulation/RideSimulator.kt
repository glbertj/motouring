package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.LegEndReason
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.activeLegDistanceMeters
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.data.model.avgSpeedKmh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RideSimulator(
    private val scope: CoroutineScope,
    initialSession: RideSession,
) {
    private val _session = MutableStateFlow(initialSession)
    val session: StateFlow<RideSession> = _session.asStateFlow()
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                _session.value = advance(_session.value)
            }
        }
    }

    fun stop() {
        job?.cancel()
        _session.value = _session.value.copy(status = RideSessionStatus.ENDED)
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val BASE_SPEED_KMH = 28.0
        private const val SPEED_VARIANCE_KMH = 6.0
        private const val SPEAKER_ROTATE_EVERY_SECONDS = 4L

        fun advance(current: RideSession): RideSession {
            if (current.status == RideSessionStatus.ENDED) return current

            val newElapsed = current.elapsedSeconds + 1
            val speed = BASE_SPEED_KMH + SPEED_VARIANCE_KMH * sin(newElapsed / 10.0)
            val distanceDeltaMeters = speed * 1000.0 / 3600.0
            val newDistance = current.distanceMeters + distanceDeltaMeters

            val totalRouteLength = totalRouteLengthMeters(current.route)
            val routeFraction = if (totalRouteLength == 0.0) 0.0 else (newDistance / totalRouteLength).coerceIn(0.0, 1.0)
            val newLeadPosition = pointAlongRoute(current.route, routeFraction)

            val speakerIndex = ((newElapsed / SPEAKER_ROTATE_EVERY_SECONDS) % current.participants.size).toInt()
            val newParticipants = current.participants.mapIndexed { index, participant ->
                participant.copy(
                    position = if (index == 0) newLeadPosition else participant.position,
                    isSpeaking = index == speakerIndex,
                )
            }

            val advanced = current.copy(
                elapsedSeconds = newElapsed,
                distanceMeters = newDistance,
                speedKmh = speed,
                participants = newParticipants,
            )

            val goal = current.activeGoal
            return if (current.mode == RideMode.GOAL && goal != null && newDistance >= goal.targetDistanceMeters) {
                val legDistance = advanced.activeLegDistanceMeters()
                val legDuration = advanced.activeLegDurationSeconds()
                val closedLeg = Leg(
                    goal = goal,
                    distanceMeters = legDistance,
                    durationSeconds = legDuration,
                    avgSpeedKmh = avgSpeedKmh(legDistance, legDuration),
                    endReason = LegEndReason.GOAL_REACHED,
                )
                advanced.copy(
                    mode = RideMode.ENDLESS,
                    activeGoal = null,
                    completedLegs = advanced.completedLegs + closedLeg,
                )
            } else {
                advanced
            }
        }

        private fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
            val earthRadiusMeters = 6_371_000.0
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLon = Math.toRadians(b.lng - a.lng)
            val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            return 2 * earthRadiusMeters * asin(sqrt(h))
        }

        private fun totalRouteLengthMeters(route: List<GeoPoint>): Double =
            route.zipWithNext { a, b -> haversineMeters(a, b) }.sum()

        private fun pointAlongRoute(route: List<GeoPoint>, fraction: Double): GeoPoint {
            if (route.size < 2) return route.first()
            val targetDistance = totalRouteLengthMeters(route) * fraction
            var covered = 0.0
            for (i in 0 until route.size - 1) {
                val segStart = route[i]
                val segEnd = route[i + 1]
                val segLength = haversineMeters(segStart, segEnd)
                val reachesTarget = covered + segLength >= targetDistance
                val isLastSegment = i == route.size - 2
                if (reachesTarget || isLastSegment) {
                    val segFraction = if (segLength == 0.0) 0.0 else ((targetDistance - covered) / segLength).coerceIn(0.0, 1.0)
                    val lat = segStart.lat + (segEnd.lat - segStart.lat) * segFraction
                    val lng = segStart.lng + (segEnd.lng - segStart.lng) * segFraction
                    return GeoPoint(lat = lat, lng = lng)
                }
                covered += segLength
            }
            return route.last()
        }
    }
}
