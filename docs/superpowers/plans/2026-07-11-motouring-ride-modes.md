# Ride Modes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Goal vs. Endless ride-mode mechanic from `docs/superpowers/specs/2026-07-10-motouring-ride-modes-design.md` — multi-stop trip legs, a celebration/choice UX on reaching a goal, silent drift-to-Endless fallback, two new badges, and a new Ride Session screen wired end-to-end from Start Ride through Ride Summary.

**Architecture:** Extends the existing `RideSession`/`RideSimulator`/`RideHistoryEntry` types (already present in the codebase, pre-built ahead of the screen) rather than introducing parallel models. `RideSimulator.advance()` gains a pure goal-crossing check; new instance methods `setGoal`/`simulateDrift` handle rider-triggered transitions; `stop()` is extended to close the trailing leg. A new `RideSessionViewModel` owns the `RideSimulator` and exposes `session`/`events` to a new `RideSessionScreen`, built from small previewable stateless composables. `RIDE_SESSION_PATTERN` and `RIDE_SUMMARY_PATTERN` already exist as route constants in `Destinations.kt` (pre-scaffolded) but have no composable/caller wired — this plan fills that gap.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, `androidx.lifecycle` ViewModel + `viewModelFactory`, Navigation-Compose, JUnit 4 + `kotlinx-coroutines-test` (already a declared test dependency, used here for the first time on instance-level coroutine behavior).

## Global Constraints

- Package `com.valid.motouring`; Kotlin/Compose/Material 3; minSdk 26 / compileSdk 35.
- Distances/durations use `Double` meters and `Long` seconds throughout — no `java.time` types, matching `RideSession`/`RideHistoryEntry`'s existing conventions.
- In-memory only — no persistence across process death, no network calls.
- No real Mapbox/GPS in this plan — the route visual is a static Canvas polyline driven by `RideSession.participants.first().position` (already computed by the existing `RideSimulator`).
- Do not modify `ui/theme/*` or existing files under `ui/components/*` — those are owned by the parallel Analog Dash design-system track. New screen-specific composables that consume those tokens are fine and go under `ui/rides/`.
- **Testing reality check:** both specs' "no unit test suite" line is inaccurate — the repo already has `app/src/test/java/.../simulation/RideSimulatorTest.kt` plus three repository tests (JUnit 4, `kotlinx-coroutines-test` already declared). This plan follows the codebase's actual, established split: **pure/testable logic (`RideSimulator`, the new calculations file) gets JUnit tests; ViewModels and Compose screens do not** (no existing ViewModel in this codebase has a test) — those get `@Preview` coverage plus a manual smoke test, exactly like every other screen in the app.
- Follow existing file conventions: one type/small cluster of tightly-coupled types per model file, `viewModelFactory { initializer { ... } }` for ViewModel construction, `StateFlow`-backed repositories.

---

### Task 1: Extend the ride-session domain model

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideSession.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideHistoryEntry.kt`

**Interfaces:**
- Produces: `RideMode`, `GoalType`, `RideGoal`, `LegEndReason`, `Leg`, `RideSessionEvent`, extended `RideSession` (fields `mode`, `activeGoal`, `completedLegs`), extension functions `RideSession.activeLegDistanceMeters()`, `RideSession.activeLegDurationSeconds()`, top-level `avgSpeedKmh(distanceMeters, durationSeconds)`. `RideHistoryEntry` gains `legs: List<Leg>`.
- Consumes: nothing new (pure additions to existing file).

- [ ] **Step 1: Replace the full contents of `RideSession.kt`**

```kotlin
package com.valid.motouring.data.model

enum class RideSessionStatus { ACTIVE, ENDED }

enum class RideMode { GOAL, ENDLESS }

enum class GoalType { DISTANCE, DESTINATION }

data class RideGoal(
    val type: GoalType,
    val label: String,
    val targetDistanceMeters: Double,
)

enum class LegEndReason { GOAL_REACHED, DRIFTED, RIDE_ENDED }

data class Leg(
    val goal: RideGoal?,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val endReason: LegEndReason,
)

sealed interface RideSessionEvent {
    data class GoalReached(val leg: Leg) : RideSessionEvent
    object DriftedToEndless : RideSessionEvent
}

data class RideParticipantState(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val position: GeoPoint,
    val isSpeaking: Boolean = false,
)

data class RideSession(
    val id: String,
    val vehicleType: VehicleType,
    val route: List<GeoPoint>,
    val participants: List<RideParticipantState>,
    val distanceMeters: Double,
    val speedKmh: Double,
    val elapsedSeconds: Long,
    val status: RideSessionStatus,
    val mode: RideMode = RideMode.ENDLESS,
    val activeGoal: RideGoal? = null,
    val completedLegs: List<Leg> = emptyList(),
)

fun RideSession.activeLegDistanceMeters(): Double =
    distanceMeters - completedLegs.sumOf { it.distanceMeters }

fun RideSession.activeLegDurationSeconds(): Long =
    elapsedSeconds - completedLegs.sumOf { it.durationSeconds }

fun avgSpeedKmh(distanceMeters: Double, durationSeconds: Long): Double =
    if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else 0.0
```

- [ ] **Step 2: Add the `legs` field to `RideHistoryEntry`**

Modify `RideHistoryEntry.kt` to:

```kotlin
package com.valid.motouring.data.model

data class RideHistoryEntry(
    val id: String,
    val title: String,
    val vehicleType: VehicleType,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val routePreviewRes: Int,
    val photoResList: List<Int>,
    val completedAtEpochSeconds: Long,
    val legs: List<Leg> = emptyList(),
)
```

- [ ] **Step 3: Confirm the existing test suite still compiles and passes**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests (including `RideSimulatorTest`) still pass — the new fields all have defaults, so no existing call site breaks.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/RideSession.kt app/src/main/java/com/valid/motouring/data/model/RideHistoryEntry.kt
git commit -m "feat: extend ride session model with goal/leg/mode types"
```

---

### Task 2: `RideSimulator.advance()` closes a leg when a goal is reached

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorTest.kt`

**Interfaces:**
- Consumes: `RideMode`, `RideGoal`, `GoalType`, `Leg`, `LegEndReason`, `activeLegDistanceMeters()`, `activeLegDurationSeconds()`, `avgSpeedKmh()` from Task 1.
- Produces: `RideSimulator.advance(current: RideSession): RideSession` now closes a leg and flips to `ENDLESS` when `distanceMeters` crosses `activeGoal.targetDistanceMeters`.

- [ ] **Step 1: Write the failing tests**

Add to `RideSimulatorTest.kt` (new imports needed: `GoalType`, `LegEndReason`, `RideGoal`, `RideMode`):

```kotlin
    @Test
    fun `advance closes the leg and falls back to ENDLESS when the goal distance is crossed`() {
        val goal = RideGoal(GoalType.DISTANCE, "300 m", 300.0)
        var session = freshSession().copy(mode = RideMode.GOAL, activeGoal = goal)
        repeat(60) { session = RideSimulator.advance(session) }
        assertEquals(RideMode.ENDLESS, session.mode)
        assertEquals(null, session.activeGoal)
        assertEquals(1, session.completedLegs.size)
        assertEquals(LegEndReason.GOAL_REACHED, session.completedLegs.first().endReason)
        assertTrue(session.completedLegs.first().distanceMeters >= 300.0)
    }

    @Test
    fun `advance does not close a leg before the goal distance is reached`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", 5_000.0)
        var session = freshSession().copy(mode = RideMode.GOAL, activeGoal = goal)
        repeat(5) { session = RideSimulator.advance(session) }
        assertEquals(RideMode.GOAL, session.mode)
        assertTrue(session.completedLegs.isEmpty())
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: the two new tests FAIL (`mode` stays `GOAL`, `completedLegs` stays empty) — `advance()` doesn't check the goal yet.

- [ ] **Step 3: Implement the goal-crossing check**

In `RideSimulator.kt`, replace the `advance` function body (inside the `companion object`) with:

```kotlin
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
```

Add imports at the top of the file: `com.valid.motouring.data.model.Leg`, `com.valid.motouring.data.model.LegEndReason`, `com.valid.motouring.data.model.RideMode`, `com.valid.motouring.data.model.activeLegDistanceMeters`, `com.valid.motouring.data.model.activeLegDurationSeconds`, `com.valid.motouring.data.model.avgSpeedKmh`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: BUILD SUCCESSFUL, all tests pass (the 5 original + 2 new).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt app/src/test/java/com/valid/motouring/simulation/RideSimulatorTest.kt
git commit -m "feat: close a leg and fall back to Endless when advance() crosses the goal"
```

---

### Task 3: `RideSimulator` gains `setGoal`, `simulateDrift`, an events stream, and `stop()` closes the trailing leg

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorTest.kt`

**Interfaces:**
- Consumes: everything from Task 1 and 2.
- Produces: `RideSimulator.setGoal(goal: RideGoal)`, `RideSimulator.simulateDrift()`, `RideSimulator.events: SharedFlow<RideSessionEvent>`, and `RideSimulator.stop()` now appends a trailing `Leg` (`endReason = RIDE_ENDED`) before ending the session. `start()` emits `RideSessionEvent.GoalReached` on the `events` flow when a tick closes a goal leg.

- [ ] **Step 1: Write the failing tests**

Add to `RideSimulatorTest.kt` (new imports: `kotlinx.coroutines.async`, `kotlinx.coroutines.test.runTest`, `kotlinx.coroutines.flow.first`, `RideSessionEvent`):

```kotlin
    @Test
    fun `setGoal switches mode to GOAL and sets the goal`() = runTest {
        val simulator = RideSimulator(this, freshSession())
        val goal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0)
        simulator.setGoal(goal)
        assertEquals(RideMode.GOAL, simulator.session.value.mode)
        assertEquals(goal, simulator.session.value.activeGoal)
    }

    @Test
    fun `simulateDrift closes the active leg as DRIFTED, emits an event, and falls back to ENDLESS`() = runTest {
        val goal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0)
        val simulator = RideSimulator(this, freshSession().copy(mode = RideMode.GOAL, activeGoal = goal))
        val eventDeferred = async { simulator.events.first() }
        simulator.simulateDrift()
        assertEquals(RideSessionEvent.DriftedToEndless, eventDeferred.await())
        val session = simulator.session.value
        assertEquals(RideMode.ENDLESS, session.mode)
        assertEquals(null, session.activeGoal)
        assertEquals(1, session.completedLegs.size)
        assertEquals(LegEndReason.DRIFTED, session.completedLegs.first().endReason)
    }

    @Test
    fun `simulateDrift is a no-op while already in ENDLESS mode`() = runTest {
        val simulator = RideSimulator(this, freshSession())
        simulator.simulateDrift()
        assertTrue(simulator.session.value.completedLegs.isEmpty())
    }

    @Test
    fun `stop closes the active leg as RIDE_ENDED and ends the session`() = runTest {
        val goal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0)
        val simulator = RideSimulator(
            this,
            freshSession().copy(mode = RideMode.GOAL, activeGoal = goal, distanceMeters = 4_000.0, elapsedSeconds = 500),
        )
        simulator.stop()
        val session = simulator.session.value
        assertEquals(RideSessionStatus.ENDED, session.status)
        assertEquals(1, session.completedLegs.size)
        assertEquals(LegEndReason.RIDE_ENDED, session.completedLegs.first().endReason)
        assertEquals(4_000.0, session.completedLegs.first().distanceMeters, 0.01)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: FAIL to compile (`setGoal`/`simulateDrift`/`events` don't exist yet) — this is the expected red state.

- [ ] **Step 3: Implement `setGoal`, `simulateDrift`, `events`, and extend `stop()`**

In `RideSimulator.kt`, add these imports: `com.valid.motouring.data.model.RideGoal`, `com.valid.motouring.data.model.RideSessionEvent`, `kotlinx.coroutines.flow.MutableSharedFlow`, `kotlinx.coroutines.flow.SharedFlow`, `kotlinx.coroutines.flow.asSharedFlow`.

Replace the class body (everything between the class declaration and the `companion object`) with:

```kotlin
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
        _session.value = current.copy(mode = RideMode.GOAL, activeGoal = goal)
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

```

(Leave the existing `companion object { ... }` block below this exactly as-is except for the `advance` changes already made in Task 2.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: BUILD SUCCESSFUL, all tests pass (9 total).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt app/src/test/java/com/valid/motouring/simulation/RideSimulatorTest.kt
git commit -m "feat: add setGoal/simulateDrift/events to RideSimulator, close trailing leg on stop"
```

---

### Task 4: Trip-title composition, badge criteria, and `RideSession` → `RideHistoryEntry` mapping

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/RideSessionCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSessionCalculationsTest.kt`

**Interfaces:**
- Consumes: `Leg`, `RideGoal`, `GoalType`, `LegEndReason`, `RideSession`, `RideHistoryEntry`, `avgSpeedKmh()` from Task 1.
- Produces: `composeTripTitle(legs: List<Leg>): String`, `explorerBadgeEarned(legs: List<Leg>): Boolean`, `neverEndingBadgeEarned(legs: List<Leg>): Boolean`, `RideSession.toHistoryEntry(id: String, completedAtEpochSeconds: Long, routePreviewRes: Int): RideHistoryEntry`. Task 7 (`RideSessionViewModel`) consumes all four.

- [ ] **Step 1: Write the failing tests**

Create `RideSessionCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GoalType
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.LegEndReason
import com.valid.motouring.data.model.RideGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSessionCalculationsTest {

    @Test
    fun `single short leg with no goal returns a generic title`() {
        val legs = listOf(Leg(goal = null, distanceMeters = 50.0, durationSeconds = 20, avgSpeedKmh = 9.0, endReason = LegEndReason.RIDE_ENDED))
        assertEquals("Ride", composeTripTitle(legs))
    }

    @Test
    fun `single goal leg followed by a meaningful free tail composes an arrow title`() {
        val goal = RideGoal(GoalType.DISTANCE, "25 km", 25_000.0)
        val legs = listOf(
            Leg(goal = goal, distanceMeters = 25_000.0, durationSeconds = 3000, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = null, distanceMeters = 2_000.0, durationSeconds = 300, avgSpeedKmh = 24.0, endReason = LegEndReason.RIDE_ENDED),
        )
        assertEquals("25 km → Free ride", composeTripTitle(legs))
    }

    @Test
    fun `three stops compose a multi-arrow title and drop a negligible tail`() {
        val goalA = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0)
        val goalB = RideGoal(GoalType.DESTINATION, "Warung Kopi Susu", 18_000.0)
        val legs = listOf(
            Leg(goal = goalA, distanceMeters = 10_000.0, durationSeconds = 1200, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = goalB, distanceMeters = 8_000.0, durationSeconds = 960, avgSpeedKmh = 30.0, endReason = LegEndReason.GOAL_REACHED),
            Leg(goal = null, distanceMeters = 20.0, durationSeconds = 5, avgSpeedKmh = 14.0, endReason = LegEndReason.RIDE_ENDED),
        )
        assertEquals("10 km → Warung Kopi Susu", composeTripTitle(legs))
    }

    @Test
    fun `explorerBadgeEarned requires at least 3 goal legs`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", 5_000.0)
        val twoLegs = listOf(
            Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED),
            Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED),
        )
        assertFalse(explorerBadgeEarned(twoLegs))
        val threeLegs = twoLegs + Leg(goal, 5_000.0, 600, 30.0, LegEndReason.GOAL_REACHED)
        assertTrue(explorerBadgeEarned(threeLegs))
    }

    @Test
    fun `neverEndingBadgeEarned requires 50km or more on a single goal-less leg`() {
        val short = listOf(Leg(null, 10_000.0, 1200, 30.0, LegEndReason.RIDE_ENDED))
        assertFalse(neverEndingBadgeEarned(short))
        val long = listOf(Leg(null, 50_500.0, 6000, 30.0, LegEndReason.RIDE_ENDED))
        assertTrue(neverEndingBadgeEarned(long))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSessionCalculationsTest"`
Expected: FAIL to compile — `composeTripTitle`/`explorerBadgeEarned`/`neverEndingBadgeEarned` don't exist yet.

- [ ] **Step 3: Create `RideSessionCalculations.kt`**

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.avgSpeedKmh

fun composeTripTitle(legs: List<Leg>): String {
    val goalLabels = legs.mapNotNull { it.goal?.label }
    val hasFreeTail = legs.lastOrNull()?.let { it.goal == null && it.distanceMeters >= 500.0 } == true
    return when {
        goalLabels.size >= 2 -> (goalLabels + if (hasFreeTail) listOf("Free ride") else emptyList()).joinToString(" → ")
        goalLabels.size == 1 -> if (hasFreeTail) "${goalLabels[0]} → Free ride" else "${goalLabels[0]} Ride"
        hasFreeTail -> "Free Ride"
        else -> "Ride"
    }
}

fun explorerBadgeEarned(legs: List<Leg>): Boolean = legs.count { it.goal != null } >= 3

fun neverEndingBadgeEarned(legs: List<Leg>): Boolean =
    legs.any { it.goal == null && it.distanceMeters >= 50_000.0 }

fun RideSession.toHistoryEntry(
    id: String,
    completedAtEpochSeconds: Long,
    routePreviewRes: Int,
): RideHistoryEntry = RideHistoryEntry(
    id = id,
    title = composeTripTitle(completedLegs),
    vehicleType = vehicleType,
    distanceMeters = distanceMeters,
    durationSeconds = elapsedSeconds,
    avgSpeedKmh = avgSpeedKmh(distanceMeters, elapsedSeconds),
    routePreviewRes = routePreviewRes,
    photoResList = emptyList(),
    completedAtEpochSeconds = completedAtEpochSeconds,
    legs = completedLegs,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSessionCalculationsTest"`
Expected: BUILD SUCCESSFUL, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSessionCalculations.kt app/src/test/java/com/valid/motouring/simulation/RideSessionCalculationsTest.kt
git commit -m "feat: add trip-title composition, badge criteria, and history-entry mapping"
```

---

### Task 5: Seed goal presets and two new badges

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Consumes: `RideGoal`, `GoalType` (Task 1), `Badge` (existing).
- Produces: `FakeDataProvider.goalPresets: List<RideGoal>`, two new `Badge` entries `"b-7"` (Explorer) and `"b-8"` (Never Ending) appended to `FakeDataProvider.badges`. Task 6 consumes `goalPresets`; Task 7 consumes badge ids `"b-7"`/`"b-8"`.

- [ ] **Step 1: Add `goalPresets` and the two badges**

In `FakeDataProvider.kt`, add this property anywhere above `val badges = listOf(...)`:

```kotlin
    val goalPresets = listOf(
        RideGoal(GoalType.DISTANCE, "10 km", 10_000.0),
        RideGoal(GoalType.DISTANCE, "25 km", 25_000.0),
        RideGoal(GoalType.DISTANCE, "50 km", 50_000.0),
        RideGoal(GoalType.DESTINATION, "Warung Kopi Susu", 8_000.0),
        RideGoal(GoalType.DESTINATION, "Puncak Pass", 60_000.0),
    )
```

Append these two entries to the existing `val badges = listOf(...)`:

```kotlin
        Badge("b-7", "Explorer", R.drawable.ic_badge_placeholder, "Make 3 or more stops in a single ride", "3+ goal stops in one ride", false, null),
        Badge("b-8", "Never Ending", R.drawable.ic_badge_placeholder, "Ride 50km or more without a goal", "50km+ on a single Endless leg", false, null),
```

(No new imports needed — `data.model.*` is already wildcard-imported in this file.)

- [ ] **Step 2: Confirm the project still builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat: seed goal presets and Explorer/Never Ending badges"
```

---

### Task 6: `RideRepository` pending-goal holder + `StartRideScreen` goal picker

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/repository/RideRepository.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt`

**Interfaces:**
- Consumes: `RideGoal` (Task 1), `FakeDataProvider.goalPresets` (Task 5).
- Produces: `RideRepository.setPendingInitialGoal(goal: RideGoal)`, `RideRepository.consumePendingInitialGoal(): RideGoal?` (Task 11 consumes both). `StartRideScreen`'s `onStartRide` callback signature changes to `(VehicleType, Boolean, RideGoal) -> Unit` (Task 11 consumes the new signature).

- [ ] **Step 1: Add the pending-goal holder to `RideRepository`**

Replace the full contents of `RideRepository.kt` with:

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideRepository {
    private val history = MutableStateFlow(
        FakeDataProvider.rideHistory.sortedByDescending { it.completedAtEpochSeconds }
    )

    private var pendingInitialGoal: RideGoal? = null

    fun observeHistory(): StateFlow<List<RideHistoryEntry>> = history.asStateFlow()

    fun addHistoryEntry(entry: RideHistoryEntry) {
        history.value = (listOf(entry) + history.value)
            .sortedByDescending { it.completedAtEpochSeconds }
    }

    fun setPendingInitialGoal(goal: RideGoal) {
        pendingInitialGoal = goal
    }

    fun consumePendingInitialGoal(): RideGoal? {
        val goal = pendingInitialGoal
        pendingInitialGoal = null
        return goal
    }
}
```

- [ ] **Step 2: Add the goal picker to `StartRideScreen`**

Replace the full contents of `StartRideScreen.kt` with:

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.model.VehicleType

@Composable
fun StartRideScreen(
    vehicles: List<Vehicle>,
    onInviteBuddiesClick: () -> Unit,
    onStartRide: (VehicleType, Boolean, RideGoal) -> Unit,
) {
    var isGroup by remember { mutableStateOf(true) }
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }
    var selectedGoal by remember { mutableStateOf(FakeDataProvider.goalPresets.first()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Start a Ride", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isGroup, onClick = { isGroup = false }, label = { Text("Solo") })
            FilterChip(selected = isGroup, onClick = { isGroup = true }, label = { Text("Group") })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Pick a vehicle", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicle?.id == vehicle.id,
                onClick = { selectedVehicle = vehicle },
                label = { Text("${vehicle.make} ${vehicle.model}") },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (isGroup) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onInviteBuddiesClick, modifier = Modifier.fillMaxWidth()) {
                Text("Invite Ride Buddies")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Pick a goal", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FakeDataProvider.goalPresets.forEach { goal ->
                FilterChip(
                    selected = selectedGoal == goal,
                    onClick = { selectedGoal = goal },
                    label = { Text(goal.label) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = { selectedVehicle?.let { onStartRide(it.type, isGroup, selectedGoal) } },
            enabled = selectedVehicle != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Ride")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StartRideScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StartRideScreen(
            vehicles = com.valid.motouring.data.fake.FakeDataProvider.vehicles.filter { it.ownerId == "u-me" },
            onInviteBuddiesClick = {},
            onStartRide = { _, _, _ -> },
        )
    }
}
```

- [ ] **Step 3: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL — `MotouringNavHost.kt` still calls `onStartRide = { vehicleType, isGroup -> ... }` with the old 2-arg signature. This is expected; Task 11 fixes the call site. Confirm the failure is exactly that one call site (no other errors).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/RideRepository.kt app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt
git commit -m "feat: add goal picker to Start Ride and a pending-goal holder on RideRepository"
```

---

### Task 7: `RideSessionViewModel`

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt`

**Interfaces:**
- Consumes: `RideSimulator` (Task 3), `toHistoryEntry`/`explorerBadgeEarned`/`neverEndingBadgeEarned` (Task 4), `RideRepository`/`BadgeRepository`/`UserRepository`/`RideBuddyRepository` (existing + Task 6), `FakeDataProvider.sampleRoute` (existing).
- Produces: `RideSessionViewModel(session: StateFlow<RideSession>, events: SharedFlow<RideSessionEvent>, pickGoal(goal), simulateDrift(), endRide(): String)` and `RideSessionViewModel.factory(vehicleType, isGroup, initialGoal, userRepository, rideBuddyRepository, rideRepository, badgeRepository)`. Task 10 consumes `session`/`events`/`pickGoal`/`simulateDrift`/`endRide`; Task 11 consumes `factory`.

No JUnit test for this file — per the Global Constraints testing note, no ViewModel in this codebase has a unit test; this one delegates all its logic to the already-tested `RideSimulator`/`RideSessionCalculations`, so its own risk surface is thin plumbing, verified via the Task 13 manual smoke test.

- [ ] **Step 1: Create `RideSessionViewModel.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.RideBuddyRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.simulation.RideSimulator
import com.valid.motouring.simulation.explorerBadgeEarned
import com.valid.motouring.simulation.neverEndingBadgeEarned
import com.valid.motouring.simulation.toHistoryEntry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class RideSessionViewModel(
    initialSession: RideSession,
    private val rideRepository: RideRepository,
    private val badgeRepository: BadgeRepository,
) : ViewModel() {

    private val simulator = RideSimulator(viewModelScope, initialSession)

    val session: StateFlow<RideSession> = simulator.session
    val events: SharedFlow<RideSessionEvent> = simulator.events

    init {
        simulator.start()
    }

    fun pickGoal(goal: RideGoal) = simulator.setGoal(goal)

    fun simulateDrift() = simulator.simulateDrift()

    fun endRide(): String {
        simulator.stop()
        val finalSession = simulator.session.value
        val completedAt = System.currentTimeMillis() / 1000
        val entry = finalSession.toHistoryEntry(
            id = "r-${System.currentTimeMillis()}",
            completedAtEpochSeconds = completedAt,
            routePreviewRes = R.drawable.ic_route_preview_placeholder,
        )
        rideRepository.addHistoryEntry(entry)
        if (explorerBadgeEarned(entry.legs)) {
            badgeRepository.markEarned("b-7", completedAt)
        }
        if (neverEndingBadgeEarned(entry.legs)) {
            badgeRepository.markEarned("b-8", completedAt)
        }
        return entry.id
    }

    companion object {
        fun factory(
            vehicleType: VehicleType,
            isGroup: Boolean,
            initialGoal: RideGoal?,
            userRepository: UserRepository,
            rideBuddyRepository: RideBuddyRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
        ) = viewModelFactory {
            initializer {
                val currentUser = userRepository.currentUser()
                val route = FakeDataProvider.sampleRoute
                val participants = buildList {
                    add(
                        RideParticipantState(
                            userId = currentUser.id,
                            name = currentUser.name,
                            avatarRes = currentUser.avatarRes,
                            position = route.first(),
                        ),
                    )
                    if (isGroup) {
                        rideBuddyRepository.friends().forEach { buddy ->
                            add(
                                RideParticipantState(
                                    userId = buddy.user.id,
                                    name = buddy.user.name,
                                    avatarRes = buddy.user.avatarRes,
                                    position = route.first(),
                                ),
                            )
                        }
                    }
                }
                val initialSession = RideSession(
                    id = "rs-${System.currentTimeMillis()}",
                    vehicleType = vehicleType,
                    route = route,
                    participants = participants,
                    distanceMeters = 0.0,
                    speedKmh = 0.0,
                    elapsedSeconds = 0,
                    status = RideSessionStatus.ACTIVE,
                    mode = if (initialGoal != null) RideMode.GOAL else RideMode.ENDLESS,
                    activeGoal = initialGoal,
                    completedLegs = emptyList(),
                )
                RideSessionViewModel(initialSession, rideRepository, badgeRepository)
            }
        }
    }
}
```

- [ ] **Step 2: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (this class isn't referenced from the nav graph yet, so it just needs to compile standalone).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt
git commit -m "feat: add RideSessionViewModel wiring the simulator to repositories"
```

---

### Task 8: `RideSessionHud` and `RidePlaceholderRoute` composables

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RidePlaceholderRoute.kt`

**Interfaces:**
- Consumes: `RideSession`, `RideMode`, `activeLegDistanceMeters()`, `activeLegDurationSeconds()` (Task 1), `InstrumentRing`/`StatBlock` (existing `ui/components`), `GeoPoint` (existing).
- Produces: `RideSessionHud(session: RideSession, modifier: Modifier = Modifier)`, `RidePlaceholderRoute(route: List<GeoPoint>, markerPosition: GeoPoint, modifier: Modifier = Modifier)`. Task 10 consumes both.

- [ ] **Step 1: Add the two preview-fixture builders to `FakeDataProvider.kt`**

Add these two functions to `FakeDataProvider.kt` (anywhere at the top level of the `object`) — they're used by the previews created in Step 2:

```kotlin
    fun previewRideSessionWithGoal(): RideSession = RideSession(
        id = "preview-goal",
        vehicleType = VehicleType.MOTORCYCLE,
        route = sampleRoute,
        participants = listOf(RideParticipantState(currentUserId, "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute.first())),
        distanceMeters = 6_000.0,
        speedKmh = 28.0,
        elapsedSeconds = 720,
        status = RideSessionStatus.ACTIVE,
        mode = RideMode.GOAL,
        activeGoal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0),
    )

    fun previewRideSessionEndless(): RideSession = RideSession(
        id = "preview-endless",
        vehicleType = VehicleType.MOTORCYCLE,
        route = sampleRoute,
        participants = listOf(RideParticipantState(currentUserId, "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute.first())),
        distanceMeters = 12_500.0,
        speedKmh = 26.0,
        elapsedSeconds = 1_500,
        status = RideSessionStatus.ACTIVE,
        mode = RideMode.ENDLESS,
        completedLegs = listOf(
            Leg(RideGoal(GoalType.DISTANCE, "10 km", 10_000.0), 10_000.0, 1_200, 30.0, LegEndReason.GOAL_REACHED),
        ),
    )
```

- [ ] **Step 2: Create `RideSessionHud.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.activeLegDistanceMeters
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

@Composable
fun RideSessionHud(session: RideSession, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Distance", value = "${"%.1f".format(session.distanceMeters / 1000.0)} km")
            StatBlock(label = "Speed", value = "${session.speedKmh.toInt()} km/h")
            StatBlock(label = "Duration", value = "${session.elapsedSeconds / 60} min")
        }
        Spacer(modifier = Modifier.height(12.dp))

        val goal = session.activeGoal
        if (session.mode == RideMode.GOAL && goal != null) {
            val remainingMeters = (goal.targetDistanceMeters - session.activeLegDistanceMeters()).coerceAtLeast(0.0)
            val progress = (session.activeLegDistanceMeters() / goal.targetDistanceMeters).toFloat().coerceIn(0f, 1f)
            val almostThere = progress >= 0.9f

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (almostThere) {
                        val infiniteTransition = rememberInfiniteTransition(label = "almostThereGlow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                            label = "glowAlpha",
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(AccentPrimary.copy(alpha = glowAlpha), AccentPrimary.copy(alpha = 0f)),
                                    ),
                                ),
                        )
                    }
                    InstrumentRing(progress = progress, size = 56.dp) {
                        Text(text = "${"%.1f".format(remainingMeters / 1000.0)} km", style = MotouringTextStyles.statLabel)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "→ ${goal.label}", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val minutesSinceStop = session.activeLegDurationSeconds() / 60
            Text(
                text = "Endless — $minutesSinceStop min since last stop",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSessionHudGoalPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSessionHud(
            session = com.valid.motouring.data.fake.FakeDataProvider.previewRideSessionWithGoal(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSessionHudEndlessPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSessionHud(
            session = com.valid.motouring.data.fake.FakeDataProvider.previewRideSessionEndless(),
        )
    }
}
```

- [ ] **Step 3: Create `RidePlaceholderRoute.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800

@Composable
fun RidePlaceholderRoute(
    route: List<GeoPoint>,
    markerPosition: GeoPoint,
    modifier: Modifier = Modifier,
) {
    val minLat = route.minOf { it.lat }
    val maxLat = route.maxOf { it.lat }
    val minLng = route.minOf { it.lng }
    val maxLng = route.maxOf { it.lng }

    fun GeoPoint.toOffset(width: Float, height: Float, padding: Float): Offset {
        val xFraction = if (maxLng == minLng) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
        val yFraction = if (maxLat == minLat) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
        return Offset(xFraction * width + padding, yFraction * height + padding)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Charcoal800),
    ) {
        val padding = 24f
        val w = size.width - padding * 2
        val h = size.height - padding * 2
        val points = route.map { it.toOffset(w, h, padding) }
        for (i in 0 until points.size - 1) {
            drawLine(color = Charcoal600, start = points[i], end = points[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        drawCircle(color = AccentPrimary, radius = 10f, center = markerPosition.toOffset(w, h, padding))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidePlaceholderRoutePreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val route = com.valid.motouring.data.fake.FakeDataProvider.sampleRoute
        RidePlaceholderRoute(route = route, markerPosition = route[2])
    }
}
```

- [ ] **Step 4: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt app/src/main/java/com/valid/motouring/ui/rides/RidePlaceholderRoute.kt app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat: add RideSessionHud and RidePlaceholderRoute composables"
```

---

### Task 9: `GoalCelebrationOverlay`, `GoalChoiceSheet`, and banner composables

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/GoalCelebrationOverlay.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/rides/GoalChoiceSheet.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionBanners.kt`

**Interfaces:**
- Consumes: `Leg`, `RideGoal` (Task 1), `MotouringCard` (existing `ui/components`).
- Produces: `GoalCelebrationOverlay(leg: Leg, modifier)`, `GoalChoiceSheet(presets: List<RideGoal>, onPickGoal: (RideGoal) -> Unit, onGoEndless: () -> Unit, modifier)`, `UndoGoalSnackbar(onPickGoalClick: () -> Unit, modifier)`, `DriftToast(modifier)`. Task 10 consumes all four.

- [ ] **Step 1: Create `GoalCelebrationOverlay.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Leg
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun GoalCelebrationOverlay(leg: Leg, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Goal reached!", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${"%.1f".format(leg.distanceMeters / 1000.0)} km in ${leg.durationSeconds / 60} min",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun GoalCelebrationOverlayPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        GoalCelebrationOverlay(
            leg = Leg(
                goal = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DISTANCE, "10 km", 10_000.0),
                distanceMeters = 10_200.0,
                durationSeconds = 1_260,
                avgSpeedKmh = 29.0,
                endReason = com.valid.motouring.data.model.LegEndReason.GOAL_REACHED,
            ),
        )
    }
}
```

- [ ] **Step 2: Create `GoalChoiceSheet.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun GoalChoiceSheet(
    presets: List<RideGoal>,
    onPickGoal: (RideGoal) -> Unit,
    onGoEndless: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(presets.firstOrNull()) }
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Nice! Pick a new goal, or keep riding",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            presets.forEach { goal ->
                FilterChip(
                    selected = selected == goal,
                    onClick = { selected = goal },
                    label = { Text(goal.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            OutlinedButton(onClick = onGoEndless, modifier = Modifier.padding(end = 8.dp)) {
                Text("Go Endless")
            }
            Button(onClick = { selected?.let(onPickGoal) }, enabled = selected != null) {
                Text("Pick Goal")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun GoalChoiceSheetPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        GoalChoiceSheet(
            presets = com.valid.motouring.data.fake.FakeDataProvider.goalPresets,
            onPickGoal = {},
            onGoEndless = {},
        )
    }
}
```

- [ ] **Step 3: Create `RideSessionBanners.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard

@Composable
fun UndoGoalSnackbar(onPickGoalClick: () -> Unit, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Went Endless — pick a goal?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onPickGoalClick) { Text("Pick a goal") }
        }
    }
}

@Composable
fun DriftToast(modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Off route — tracking continues",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun UndoGoalSnackbarPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        UndoGoalSnackbar(onPickGoalClick = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun DriftToastPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        DriftToast()
    }
}
```

- [ ] **Step 4: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/GoalCelebrationOverlay.kt app/src/main/java/com/valid/motouring/ui/rides/GoalChoiceSheet.kt app/src/main/java/com/valid/motouring/ui/rides/RideSessionBanners.kt
git commit -m "feat: add celebration overlay, choice sheet, and undo/drift banners"
```

---

### Task 10: `RideSessionScreen` — wire the ViewModel to the UI

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt`

**Interfaces:**
- Consumes: `RideSessionViewModel` (Task 7), `RideSessionHud`/`RidePlaceholderRoute` (Task 8), `GoalCelebrationOverlay`/`GoalChoiceSheet`/`UndoGoalSnackbar`/`DriftToast` (Task 9), `FakeDataProvider.goalPresets` (Task 5), `RideMode`/`RideSessionEvent`/`Leg` (Task 1).
- Produces: `RideSessionScreen(viewModel: RideSessionViewModel, onEndRide: (String) -> Unit)`. Task 11 consumes this. Not preview-covered (takes a ViewModel directly), per the Global Constraints testing note — verified via Task 13's manual smoke test.

- [ ] **Step 1: Create `RideSessionScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Leg
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideSessionEvent
import kotlinx.coroutines.delay

@Composable
fun RideSessionScreen(
    viewModel: RideSessionViewModel,
    onEndRide: (String) -> Unit,
) {
    val session by viewModel.session.collectAsState()
    var celebrationLeg by remember { mutableStateOf<Leg?>(null) }
    var showChoiceSheet by remember { mutableStateOf(false) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var showDriftToast by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RideSessionEvent.GoalReached -> celebrationLeg = event.leg
                RideSessionEvent.DriftedToEndless -> showDriftToast = true
            }
        }
    }

    LaunchedEffect(celebrationLeg) {
        if (celebrationLeg != null) {
            delay(2_500)
            showChoiceSheet = true
        }
    }

    LaunchedEffect(showChoiceSheet) {
        if (showChoiceSheet) {
            delay(5_000)
            if (showChoiceSheet) {
                showChoiceSheet = false
                celebrationLeg = null
                showUndoSnackbar = true
            }
        }
    }

    LaunchedEffect(showUndoSnackbar) {
        if (showUndoSnackbar) {
            delay(4_500)
            showUndoSnackbar = false
        }
    }

    LaunchedEffect(showDriftToast) {
        if (showDriftToast) {
            delay(3_000)
            showDriftToast = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            RideSessionHud(session = session)
            RidePlaceholderRoute(
                route = session.route,
                markerPosition = session.participants.first().position,
            )
            if (session.mode == RideMode.ENDLESS) {
                Button(onClick = { showChoiceSheet = true }) { Text("Set a goal") }
            }
            Button(onClick = { viewModel.simulateDrift() }) { Text("Simulate off-route") }
            Button(onClick = { onEndRide(viewModel.endRide()) }) { Text("End Ride") }
        }

        val leg = celebrationLeg
        if (leg != null && !showChoiceSheet) {
            GoalCelebrationOverlay(leg = leg, modifier = Modifier.align(Alignment.Center))
        }

        if (showChoiceSheet) {
            GoalChoiceSheet(
                presets = FakeDataProvider.goalPresets,
                onPickGoal = { goal ->
                    viewModel.pickGoal(goal)
                    showChoiceSheet = false
                    celebrationLeg = null
                },
                onGoEndless = {
                    showChoiceSheet = false
                    celebrationLeg = null
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showUndoSnackbar) {
            UndoGoalSnackbar(
                onPickGoalClick = {
                    showUndoSnackbar = false
                    showChoiceSheet = true
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showDriftToast) {
            DriftToast(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
```

- [ ] **Step 2: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt
git commit -m "feat: wire RideSessionScreen to RideSessionViewModel and its event stream"
```

---

### Task 11: Wire `RIDE_SESSION_PATTERN` into `MotouringNavHost` and fix the `StartRideScreen` call site

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `RideSessionScreen`/`RideSessionViewModel` (Tasks 7, 10), `RideRepository.setPendingInitialGoal`/`consumePendingInitialGoal` (Task 6), `Destinations.RIDE_SESSION_PATTERN`/`rideSession()`/`RIDE_SUMMARY_PATTERN`/`rideSummary()` (existing, already defined).

- [ ] **Step 1: Add imports**

At the top of `MotouringNavHost.kt`, add:

```kotlin
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.ui.rides.RideSessionScreen
import com.valid.motouring.ui.rides.RideSessionViewModel
```

- [ ] **Step 2: Fix the `START_RIDE` composable's `onStartRide` call site**

Replace:

```kotlin
        composable(Destinations.START_RIDE) {
            val currentUser = appContainer.userRepository.currentUser()
            val vehicles = appContainer.vehicleRepository.vehiclesFor(currentUser.id)
            StartRideScreen(
                vehicles = vehicles,
                onInviteBuddiesClick = { navController.navigate(Destinations.INVITE_RIDE) },
                onStartRide = { vehicleType, isGroup ->
                    navController.navigate(Destinations.rideSession(vehicleType.name, isGroup))
                },
            )
        }
```

with:

```kotlin
        composable(Destinations.START_RIDE) {
            val currentUser = appContainer.userRepository.currentUser()
            val vehicles = appContainer.vehicleRepository.vehiclesFor(currentUser.id)
            StartRideScreen(
                vehicles = vehicles,
                onInviteBuddiesClick = { navController.navigate(Destinations.INVITE_RIDE) },
                onStartRide = { vehicleType, isGroup, goal ->
                    appContainer.rideRepository.setPendingInitialGoal(goal)
                    navController.navigate(Destinations.rideSession(vehicleType.name, isGroup))
                },
            )
        }
```

- [ ] **Step 3: Register the `RIDE_SESSION_PATTERN` composable**

Add this new `composable(...)` block right after the `START_RIDE` block from Step 2:

```kotlin
        composable(
            Destinations.RIDE_SESSION_PATTERN,
            arguments = listOf(
                navArgument("vehicleType") { type = NavType.StringType },
                navArgument("isGroup") { type = NavType.BoolType },
            ),
        ) { backStackEntry ->
            val vehicleType = VehicleType.valueOf(requireNotNull(backStackEntry.arguments?.getString("vehicleType")))
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            val initialGoal = appContainer.rideRepository.consumePendingInitialGoal()
            val viewModel: RideSessionViewModel = viewModel(
                factory = RideSessionViewModel.factory(
                    vehicleType = vehicleType,
                    isGroup = isGroup,
                    initialGoal = initialGoal,
                    userRepository = appContainer.userRepository,
                    rideBuddyRepository = appContainer.rideBuddyRepository,
                    rideRepository = appContainer.rideRepository,
                    badgeRepository = appContainer.badgeRepository,
                ),
            )
            RideSessionScreen(
                viewModel = viewModel,
                onEndRide = { historyEntryId ->
                    navController.navigate(Destinations.rideSummary(historyEntryId)) {
                        popUpTo(Destinations.MAIN)
                    }
                },
            )
        }
```

- [ ] **Step 4: Run the full test suite and confirm the app builds**

Run: `./gradlew testDebugUnitTest compileDebugKotlin`
Expected: BUILD SUCCESSFUL — this resolves the Task 6 compile failure and all unit tests still pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: wire Ride Session into the nav graph, connect Start Ride's goal to it"
```

---

### Task 12: Ride Summary — Stops section

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt`

**Interfaces:**
- Consumes: `RideHistoryEntry.legs` (Task 1), `MotouringCard`/`StaggeredEntrance`/`SectionHeader` (existing `ui/components`).

- [ ] **Step 1: Replace the full contents of `RideSummaryScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.components.StatBlock

@Composable
fun RideSummaryScreen(
    entry: RideHistoryEntry,
    earnedBadges: List<Badge>,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(text = "Ride Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = entry.routePreviewRes),
            contentDescription = entry.title,
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = entry.title, style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatBlock(label = "Distance", value = "${"%.1f".format(entry.distanceMeters / 1000.0)} km")
            StatBlock(label = "Duration", value = "${entry.durationSeconds / 60} min")
            StatBlock(label = "Avg Speed", value = "${entry.avgSpeedKmh.toInt()} km/h")
        }

        if (earnedBadges.isNotEmpty()) {
            SectionHeader(title = "Your Badges")
            Row {
                earnedBadges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }
        }

        val visibleLegs = entry.legs.filter { it.goal != null || it.distanceMeters >= 500.0 }
        if (visibleLegs.isNotEmpty()) {
            SectionHeader(title = "Stops")
            visibleLegs.forEachIndexed { index, leg ->
                StaggeredEntrance(index = index) {
                    MotouringCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = leg.goal?.label ?: "Free ride", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = "${"%.1f".format(leg.distanceMeters / 1000.0)} km", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${leg.durationSeconds / 60} min", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${leg.avgSpeedKmh.toInt()} km/h", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSummaryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSummaryScreen(
            entry = com.valid.motouring.data.fake.FakeDataProvider.rideHistory.first(),
            earnedBadges = com.valid.motouring.data.fake.FakeDataProvider.badges.filter { it.isEarned },
            onDone = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSummaryScreenWithStopsPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val goalA = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DISTANCE, "10 km", 10_000.0)
        val goalB = com.valid.motouring.data.model.RideGoal(com.valid.motouring.data.model.GoalType.DESTINATION, "Warung Kopi Susu", 18_000.0)
        val legs = listOf(
            com.valid.motouring.data.model.Leg(goalA, 10_000.0, 1_200, 30.0, com.valid.motouring.data.model.LegEndReason.GOAL_REACHED),
            com.valid.motouring.data.model.Leg(goalB, 8_000.0, 960, 30.0, com.valid.motouring.data.model.LegEndReason.GOAL_REACHED),
            com.valid.motouring.data.model.Leg(null, 2_000.0, 300, 24.0, com.valid.motouring.data.model.LegEndReason.RIDE_ENDED),
        )
        RideSummaryScreen(
            entry = com.valid.motouring.data.fake.FakeDataProvider.rideHistory.first().copy(legs = legs),
            earnedBadges = com.valid.motouring.data.fake.FakeDataProvider.badges.filter { it.isEarned },
            onDone = {},
        )
    }
}
```

- [ ] **Step 2: Confirm the project builds**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt
git commit -m "feat: show a leg-by-leg Stops section in Ride Summary"
```

---

### Task 13: Full build, test suite, and manual smoke test

**Files:** none (verification only)

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all tests across `RideSimulatorTest`, `RideSessionCalculationsTest`, and the three existing repository tests pass.

- [ ] **Step 2: Build the debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke test on an emulator/device**

Install and run the app, then walk through:
1. Start Ride → pick "Solo" → pick a vehicle → pick the "10 km" goal preset → Start Ride.
2. On the Ride Session screen, confirm the HUD shows distance/speed/duration ticking up, the goal label reads "→ 10 km", and the placeholder route shows a moving marker.
3. Watch the `InstrumentRing` shift into the "almost there" glow as the leg nears 10km, then confirm the celebration overlay appears when the goal is crossed, followed by the choice sheet.
4. Tap a new preset in the choice sheet — confirm the HUD updates to the new goal and tracking never paused (distance/duration kept incrementing throughout).
5. Let a second goal complete and this time let the choice sheet auto-dismiss (don't tap anything) — confirm it falls into Endless, the "time since last stop" HUD label appears, and the undo snackbar shows with a working "Pick a goal" action.
6. Set a third goal via the persistent "Set a goal" chip, then tap "Simulate off-route" — confirm a quiet drift toast appears (no celebration) and the ride falls into Endless.
7. Tap "End Ride" — confirm navigation lands on Ride Summary showing the correct aggregate totals, an auto-composed multi-stop title, and a "Stops" section listing all legs in order with correct per-leg stats.
8. Confirm the "Explorer" and "Never Ending" badges show as earned in the Badges grid (Profile → Badges) if this ride's legs met their criteria.
9. Repeat steps 1-7 with "Group" selected at Start Ride — confirm other participants still appear in spirit (rider list/voice bar behavior is unchanged from before this plan) and the goal/Endless mechanic behaves identically from the local rider's perspective.

- [ ] **Step 4: Report results**

If all steps pass, this plan is complete. If anything fails, treat it as a bug against the specific task above whose deliverable is implicated, fix it there, and re-run this task's steps.

---

## Self-Review Notes

**Spec coverage:** every section of `2026-07-10-motouring-ride-modes-design.md` maps to a task — data model (1), state machine rule incl. events (2, 3), Start Ride goal picker (6), Ride Session screen incl. HUD/placeholder map/persistent chip/debug drift action (8, 10), goal-reached celebration + choice sheet + undo snackbar (9, 10), drifted-to-Endless toast (9, 10), ending the ride (3, 7), Ride Summary leg breakdown + auto-title (4, 12), gamification tie-in (4, 5, 7), design-system integration via existing `InstrumentRing`/`MotouringCard`/`StaggeredEntrance` (8, 9, 12), edge cases — ending mid-celebration (10's cancel-on-end semantics), solo vs. group (7's factory), zero-distance leg (3/4's no-special-casing) — all covered without extra code, since none of these needed special-casing by design.

**Type consistency verified:** `Leg`/`RideGoal`/`LegEndReason`/`RideMode`/`RideSessionEvent` (Task 1) are used identically in Tasks 2, 3, 4, 5, 7, 8, 9, 12. `RideSimulator.events`/`session` (Task 3) match what Task 7 and Task 10 consume. `RideSessionViewModel.factory(...)` parameter names in Task 7 match the call site in Task 11 exactly. `composeTripTitle`/`explorerBadgeEarned`/`neverEndingBadgeEarned`/`toHistoryEntry` (Task 4) match their call sites in Task 7 and their test names in Task 4.

**Placeholder scan:** no TBD/TODO markers; every step has complete, runnable code.
