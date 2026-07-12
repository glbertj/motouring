package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.LegEndReason
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.activeLegDistanceMeters
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.data.model.avgSpeedKmh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _events = MutableSharedFlow<RideSessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RideSessionEvent> = _events.asSharedFlow()

    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                val previous = _session.value
                val next = advance(previous)
                _session.value = next
                next.participants.forEachIndexed { i, p ->
                    val was = previous.participants.getOrNull(i)?.hasFallenBehind ?: false
                    if (p.hasFallenBehind && !was) {
                        _events.emit(RideSessionEvent.RiderFellBehind(p))
                    }
                }
                if (next.completedLegs.size > previous.completedLegs.size) {
                    val closedLeg = next.completedLegs.last()
                    if (closedLeg.endReason == LegEndReason.GOAL_REACHED) {
                        _events.emit(RideSessionEvent.GoalReached(closedLeg))
                    }
                }
            }
        }
    }

    fun setGoal(goal: RideGoal) {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        val rebasedGoal = goal.copy(targetDistanceMeters = current.distanceMeters + goal.targetDistanceMeters)
        _session.value = current.copy(mode = RideMode.GOAL, activeGoal = rebasedGoal)
    }

    fun simulateDrift() {
        val current = _session.value
        if (current.mode != RideMode.GOAL || current.activeGoal == null) return
        val legDistance = current.activeLegDistanceMeters()
        val legDuration = current.activeLegDurationSeconds()
        val closedLeg = Leg(
            goal = current.activeGoal,
            distanceMeters = legDistance,
            durationSeconds = legDuration,
            avgSpeedKmh = avgSpeedKmh(legDistance, legDuration),
            endReason = LegEndReason.DRIFTED,
        )
        _session.value = current.copy(
            mode = RideMode.ENDLESS,
            activeGoal = null,
            completedLegs = current.completedLegs + closedLeg,
        )
        scope.launch { _events.emit(RideSessionEvent.DriftedToEndless) }
    }

    fun stop() {
        job?.cancel()
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        val legDistance = current.activeLegDistanceMeters()
        val legDuration = current.activeLegDurationSeconds()
        val tailLeg = Leg(
            goal = current.activeGoal,
            distanceMeters = legDistance,
            durationSeconds = legDuration,
            avgSpeedKmh = avgSpeedKmh(legDistance, legDuration),
            endReason = LegEndReason.RIDE_ENDED,
        )
        _session.value = current.copy(
            status = RideSessionStatus.ENDED,
            activeGoal = null,
            completedLegs = current.completedLegs + tailLeg,
        )
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val BASE_SPEED_KMH = 28.0
        private const val SPEED_VARIANCE_KMH = 6.0
        private const val SPEAKER_ROTATE_EVERY_SECONDS = 4L
        private const val PACK_SLOT_GAP_METERS = 90.0
        private const val OSC_AMPLITUDE_METERS = 22.0
        private const val OSC_PERIOD_SECONDS = 11.0
        private const val PHASE_STEP_SECONDS = 3.0
        private const val FALL_BEHIND_THRESHOLD_METERS = 400.0
        private const val SWEEP_DRIFT_PER_TICK = 12.0
        private const val SWEEP_DRIFT_MAX = 800.0
        private const val REGROUP_CLOSE_PER_TICK = 60.0

        fun advance(current: RideSession): RideSession {
            if (current.status == RideSessionStatus.ENDED) return current

            val newElapsed = current.elapsedSeconds + 1
            val speed = BASE_SPEED_KMH + SPEED_VARIANCE_KMH * sin(newElapsed / 10.0)
            val distanceDeltaMeters = speed * 1000.0 / 3600.0
            val newDistance = current.distanceMeters + distanceDeltaMeters

            val totalRouteLength = totalRouteLengthMeters(current.route)
            val front = newDistance
            val lastIndex = current.participants.lastIndex

            // Sweep drift: grows each tick, or closes while regrouping. Reset by broadcastRegroup/callFuel.
            val nextDrift = if (current.isRegrouping) {
                (current.sweepDriftMeters - REGROUP_CLOSE_PER_TICK).coerceAtLeast(0.0)
            } else {
                (current.sweepDriftMeters + SWEEP_DRIFT_PER_TICK).coerceAtMost(SWEEP_DRIFT_MAX)
            }
            val stillRegrouping = current.isRegrouping && nextDrift > 0.0

            val speakerIndex = ((newElapsed / SPEAKER_ROTATE_EVERY_SECONDS) % current.participants.size).toInt()
            val newParticipants = current.participants.mapIndexed { index, participant ->
                val baseGap = index * PACK_SLOT_GAP_METERS
                val flex = if (index == 0) 0.0 else OSC_AMPLITUDE_METERS * sin((newElapsed + index * PHASE_STEP_SECONDS) / OSC_PERIOD_SECONDS)
                val sweepExtra = if (index == lastIndex && lastIndex > 0) nextDrift else 0.0
                val dist = (front - baseGap - flex - sweepExtra).coerceAtLeast(0.0)
                val frac = if (totalRouteLength == 0.0) 0.0 else (dist / totalRouteLength).coerceIn(0.0, 1.0)
                participant.copy(
                    position = pointAlongRoute(current.route, frac),
                    distanceAlongRouteMeters = dist,
                    isSpeaking = index == speakerIndex,
                    hasFallenBehind = (front - dist) > FALL_BEHIND_THRESHOLD_METERS,
                )
            }

            val elevationDelta = (2.0 + 1.5 * sin(newElapsed / 7.0)).coerceAtLeast(0.0)
            val advanced = current.copy(
                elapsedSeconds = newElapsed,
                distanceMeters = newDistance,
                speedKmh = speed,
                participants = newParticipants,
                maxSpeedKmh = maxOf(current.maxSpeedKmh, speed),
                elevationGainMeters = current.elevationGainMeters + elevationDelta,
                sweepDriftMeters = nextDrift,
                isRegrouping = stillRegrouping,
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
