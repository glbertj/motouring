# Group Ride Mechanics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the group ride feel alive — rider roles (Lead/Sweep/Rider), a genuinely moving pack with an ordered formation view, a regroup ping, and a fuel call — all on top of the existing `RideSession` / `RideSimulator` substrate.

**Architecture:** Extend the pure `RideSession` model with role + per-rider progress + two transient signal fields. All pack geometry, drift, role reassignment, and nearest-fuel selection are pure functions (TDD'd headless). `RideSimulator.advance()` moves every participant and emits a fell-behind event; three new driver methods broadcast signals. The in-ride UI reads the enriched session: role-colored map markers, a Stats/Pack dashboard toggle, and two transient banners.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), MVVM, kotlinx-coroutines, MapLibre (OpenGL ES), JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies.** Everything uses libraries already on the classpath.
- **Direct-to-`main`, push after every task** so the Arch host can pull (documented project norm; no branch/PR). Each commit message ends with the repo's `Co-Authored-By` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` run on this VM with no map token. MapLibre rendering, the dashboard toggle, the formation list, and banners are verified **on-device by the user** — they cannot be seen on the VM.
- **Additive model changes only:** every new data-class field gets a default so existing construction sites and tests keep compiling.
- **Colors come from `MotouringColors`** (`ui/theme/Color.kt`): `goal` (orange), `poiRest` (amber), `rider` (blue), `riderCoral` (coral), `speaking` (green). Do not introduce new hexes.
- **`PoiType` for fuel is `GAS_STATION`** (there is no `FUEL` enum value).

---

## File Structure

| File | Responsibility |
| --- | --- |
| `data/model/RideSession.kt` (modify) | `RiderRole`, participant fields, `GroupSignal(Type)`, two new events |
| `simulation/GroupRideCalculations.kt` (create) | Pure helpers: role assignment/reassignment, pack order + gaps, nearest gas |
| `simulation/RideSimulator.kt` (modify) | Move all participants, sweep drift + fell-behind emit, `setRole`/`broadcastRegroup`/`callFuel`/`forceSweepBehind` |
| `ui/rides/RideSessionViewModel.kt` (modify) | Driver pass-throughs, `poiRepository` injection, role auto-assign in factory |
| `navigation/MotouringNavHost.kt` (modify) | Pass `poiRepository` into the ride-session VM factory |
| `ui/components/map/MotouringMap.kt` (modify) | Role `MarkerStyle` variants + colors + self ring |
| `ui/rides/RideSessionHud.kt` (modify) | Build markers from role, self ring, fuel rally marker |
| `ui/rides/RideDashboard.kt` (modify) | Stats/Pack toggle + `FormationList` + role-reassign menu |
| `ui/rides/RideSessionBanners.kt` (modify) | `RegroupBanner`, `FuelCallBanner` |
| `ui/rides/RideSessionScreen.kt` (modify) | Consume new events → banner state; Regroup/Fuel action row |
| `data/fake/FakeDataProvider.kt` (modify) | Role-aware, spread group preview sessions |

Test command shorthand used below:
- Single class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- All unit tests: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Model — roles, per-rider progress, signals, events

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideSession.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt:42-47` (add no-op branches so the event `when` stays exhaustive/clean; filled in Task 10)
- Test: `app/src/test/java/com/valid/motouring/data/model/RideParticipantStateTest.kt` (create)

**Interfaces:**
- Produces: `enum RiderRole { LEAD, SWEEP, RIDER }`; `RideParticipantState.role: RiderRole`, `.distanceAlongRouteMeters: Double`, `.hasFallenBehind: Boolean`; `RideSession.sweepDriftMeters: Double`, `.isRegrouping: Boolean`; `enum GroupSignalType { REGROUP, FUEL }`; `data class GroupSignal(type, fromUserId, fromName, rallyPoi: PointOfInterest?)`; `RideSessionEvent.RiderFellBehind(participant)`, `RideSessionEvent.GroupSignalRaised(signal)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/model/RideParticipantStateTest.kt`:

```kotlin
package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RideParticipantStateTest {

    @Test
    fun `new participant fields default to rider role, zero progress, not behind`() {
        val p = RideParticipantState("u-1", "Rafi", 0, GeoPoint(0.0, 0.0))
        assertEquals(RiderRole.RIDER, p.role)
        assertEquals(0.0, p.distanceAlongRouteMeters, 0.0)
        assertFalse(p.hasFallenBehind)
    }

    @Test
    fun `group signal carries a rally poi for fuel`() {
        val poi = PointOfInterest("p", "Shell", PoiType.GAS_STATION, GeoPoint(0.0, 0.0), setOf(VehicleType.CAR), 4.0)
        val signal = GroupSignal(GroupSignalType.FUEL, "u-1", "Rafi", rallyPoi = poi)
        assertEquals(poi, signal.rallyPoi)
        assertEquals("Rafi", signal.fromName)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.RideParticipantStateTest"`
Expected: FAIL — compilation error (`RiderRole` / `GroupSignal` unresolved).

- [ ] **Step 3: Add the model changes**

`RideSession.kt` is already `package com.valid.motouring.data.model`, so `PointOfInterest`, `PoiType`, `GeoPoint`, and `VehicleType` are in-package — no new imports are needed.

Add the role enum above `RideParticipantState`:

```kotlin
enum class RiderRole { LEAD, SWEEP, RIDER }
```

Replace `RideParticipantState` (lines 30-36) with:

```kotlin
data class RideParticipantState(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val position: GeoPoint,
    val isSpeaking: Boolean = false,
    val role: RiderRole = RiderRole.RIDER,
    val distanceAlongRouteMeters: Double = 0.0,
    val hasFallenBehind: Boolean = false,
)
```

Add two fields to `RideSession` (after `elevationGainMeters = 0.0,` on line 51, before the closing paren):

```kotlin
    val maxSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val sweepDriftMeters: Double = 0.0,
    val isRegrouping: Boolean = false,
)
```

Add the signal types + events. Replace the `RideSessionEvent` block (lines 25-28) with:

```kotlin
enum class GroupSignalType { REGROUP, FUEL }

data class GroupSignal(
    val type: GroupSignalType,
    val fromUserId: String,
    val fromName: String,
    val rallyPoi: PointOfInterest? = null,
)

sealed interface RideSessionEvent {
    data class GoalReached(val leg: Leg) : RideSessionEvent
    object DriftedToEndless : RideSessionEvent
    data class RiderFellBehind(val participant: RideParticipantState) : RideSessionEvent
    data class GroupSignalRaised(val signal: GroupSignal) : RideSessionEvent
}
```

- [ ] **Step 4: Keep `RideSessionScreen` compiling**

In `RideSessionScreen.kt`, the events `when` (lines 43-47) currently handles two cases. Add no-op branches for the new events (Task 10 fills them in):

```kotlin
            when (event) {
                is RideSessionEvent.GoalReached -> celebrationLeg = event.leg
                RideSessionEvent.DriftedToEndless -> showDriftToast = true
                is RideSessionEvent.RiderFellBehind -> {}
                is RideSessionEvent.GroupSignalRaised -> {}
            }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.RideParticipantStateTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Verify the whole build + existing tests are green**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/RideSession.kt \
        app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt \
        app/src/test/java/com/valid/motouring/data/model/RideParticipantStateTest.kt
git commit -m "feat(group-ride): roles, per-rider progress, signal model + events

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Pure group-ride calculations

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/GroupRideCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/GroupRideCalculationsTest.kt`

**Interfaces:**
- Consumes: `RiderRole`, `RideParticipantState`, `PointOfInterest`, `PoiType`, `GeoPoint`, `distanceKm` (Task 1 + existing).
- Produces:
  - `fun assignInitialRoles(participants: List<RideParticipantState>): List<RideParticipantState>`
  - `fun withRole(participants: List<RideParticipantState>, userId: String, role: RiderRole): List<RideParticipantState>`
  - `fun sortedByPackPosition(participants: List<RideParticipantState>): List<RideParticipantState>`
  - `fun gapsToAheadMeters(sorted: List<RideParticipantState>): List<Double>`
  - `fun nearestGasStation(pois: List<PointOfInterest>, from: GeoPoint): PointOfInterest?`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/GroupRideCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupRideCalculationsTest {

    private fun p(id: String, dist: Double = 0.0, role: RiderRole = RiderRole.RIDER) =
        RideParticipantState(id, id.uppercase(), 0, GeoPoint(0.0, 0.0), role = role, distanceAlongRouteMeters = dist)

    @Test
    fun `assignInitialRoles makes first Lead and last Sweep`() {
        val result = assignInitialRoles(listOf(p("u-me"), p("u-2"), p("u-3")))
        assertEquals(RiderRole.LEAD, result[0].role)
        assertEquals(RiderRole.RIDER, result[1].role)
        assertEquals(RiderRole.SWEEP, result[2].role)
    }

    @Test
    fun `assignInitialRoles on a solo rider leaves them Lead with no Sweep`() {
        val result = assignInitialRoles(listOf(p("u-me")))
        assertEquals(RiderRole.LEAD, result[0].role)
    }

    @Test
    fun `withRole assigns and demotes the previous holder of that role`() {
        val start = assignInitialRoles(listOf(p("u-me"), p("u-2"), p("u-3"))) // u-me LEAD
        val result = withRole(start, "u-2", RiderRole.LEAD)
        assertEquals(RiderRole.LEAD, result.first { it.userId == "u-2" }.role)
        assertEquals(RiderRole.RIDER, result.first { it.userId == "u-me" }.role) // old lead demoted
        assertEquals(RiderRole.SWEEP, result.first { it.userId == "u-3" }.role)  // untouched
    }

    @Test
    fun `sortedByPackPosition orders front-most first and gaps measure to the rider ahead`() {
        val sorted = sortedByPackPosition(listOf(p("back", 100.0), p("front", 500.0), p("mid", 300.0)))
        assertEquals(listOf("front", "mid", "back"), sorted.map { it.userId })
        assertEquals(listOf(0.0, 200.0, 200.0), gapsToAheadMeters(sorted))
    }

    @Test
    fun `nearestGasStation picks the closest gas station and ignores other poi types`() {
        val here = GeoPoint(-6.2088, 106.8206)
        val far = PointOfInterest("far", "Far Gas", PoiType.GAS_STATION, GeoPoint(-6.30, 106.90), setOf(VehicleType.CAR), 4.0)
        val near = PointOfInterest("near", "Near Gas", PoiType.GAS_STATION, GeoPoint(-6.2090, 106.8208), setOf(VehicleType.CAR), 4.0)
        val repair = PointOfInterest("rep", "Repair", PoiType.REPAIR_SHOP, here, setOf(VehicleType.CAR), 4.0)
        assertEquals("near", nearestGasStation(listOf(far, repair, near), here)?.id)
        assertNull(nearestGasStation(listOf(repair), here))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.GroupRideCalculationsTest"`
Expected: FAIL — unresolved references to the helper functions.

- [ ] **Step 3: Implement the helpers**

Create `app/src/main/java/com/valid/motouring/simulation/GroupRideCalculations.kt`:

```kotlin
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
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.GroupRideCalculationsTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/GroupRideCalculations.kt \
        app/src/test/java/com/valid/motouring/simulation/GroupRideCalculationsTest.kt
git commit -m "feat(group-ride): pure pack geometry, role assignment, nearest-gas helpers

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Simulator moves the whole pack + detects fall-behind

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt` (rewrite the participant block in `advance()`, lines 122-142; add constants + fell-behind emission in `start()`, lines 45-58)
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorGroupTest.kt` (create)

**Interfaces:**
- Consumes: `RideSession.sweepDriftMeters` / `.isRegrouping`, `RideParticipantState.distanceAlongRouteMeters` / `.hasFallenBehind` (Task 1).
- Produces: `advance()` now sets every participant's `distanceAlongRouteMeters`, `position`, `hasFallenBehind`, and advances `sweepDriftMeters` / `isRegrouping`. `start()` emits `RideSessionEvent.RiderFellBehind` once per false→true transition. New private consts: `PACK_SLOT_GAP_METERS`, `OSC_AMPLITUDE_METERS`, `OSC_PERIOD_SECONDS`, `PHASE_STEP_SECONDS`, `FALL_BEHIND_THRESHOLD_METERS`, `SWEEP_DRIFT_PER_TICK`, `SWEEP_DRIFT_MAX`, `REGROUP_CLOSE_PER_TICK`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/RideSimulatorGroupTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorGroupTest {

    private val route = listOf(
        GeoPoint(-6.2246, 106.8091),
        GeoPoint(-6.2153, 106.8149),
        GeoPoint(-6.2088, 106.8206),
        GeoPoint(-6.1976, 106.8235),
        GeoPoint(-6.1875, 106.8271),
    )

    private fun groupSession() = RideSession(
        id = "g",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = assignInitialRoles(
            listOf(
                RideParticipantState("u-me", "Rafi", 0, route.first()),
                RideParticipantState("u-2", "Dinda", 0, route.first()),
                RideParticipantState("u-3", "Bagas", 0, route.first()),
            ),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `after enough ticks every participant has moved and the pack is spread`() {
        var s = groupSession()
        repeat(120) { s = RideSimulator.advance(s) }
        // front rider (self) is ahead of the sweep by a real gap
        val self = s.participants.first()
        val sweep = s.participants.last()
        assertTrue(self.distanceAlongRouteMeters > sweep.distanceAlongRouteMeters)
        assertTrue("pack should be spread", self.distanceAlongRouteMeters - sweep.distanceAlongRouteMeters > 100.0)
    }

    @Test
    fun `the sweep eventually falls behind`() {
        var s = groupSession()
        var everBehind = false
        repeat(200) {
            s = RideSimulator.advance(s)
            if (s.participants.last().hasFallenBehind) everBehind = true
        }
        assertTrue("sweep should cross the fall-behind threshold", everBehind)
    }

    @Test
    fun `the leader never falls behind`() {
        var s = groupSession()
        repeat(200) {
            s = RideSimulator.advance(s)
            assertTrue(!s.participants.first().hasFallenBehind)
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorGroupTest"`
Expected: FAIL — the sweep never sets `hasFallenBehind` (buddies don't move yet).

- [ ] **Step 3: Add constants**

In `RideSimulator.kt`, inside `companion object` (after line 112, `SPEAKER_ROTATE_EVERY_SECONDS`), add:

```kotlin
        private const val PACK_SLOT_GAP_METERS = 90.0
        private const val OSC_AMPLITUDE_METERS = 22.0
        private const val OSC_PERIOD_SECONDS = 11.0
        private const val PHASE_STEP_SECONDS = 3.0
        private const val FALL_BEHIND_THRESHOLD_METERS = 400.0
        private const val SWEEP_DRIFT_PER_TICK = 12.0
        private const val SWEEP_DRIFT_MAX = 800.0
        private const val REGROUP_CLOSE_PER_TICK = 60.0
```

- [ ] **Step 4: Rewrite the participant block in `advance()`**

Replace lines 122-142 (from `val totalRouteLength = ...` down to the end of the `advanced` assignment, i.e. through `)` closing `current.copy(...)`) with:

```kotlin
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
```

This replacement fully removes the old `val routeFraction = ...` and `val newLeadPosition = ...` lines (the new block derives every participant's position itself). Verify `speed`, `newElapsed`, `newDistance` (lines 117-120) remain defined above this block and untouched, and that the goal-leg-closing `return if (...)` block below (original lines 144-162) is left as-is.

- [ ] **Step 5: Emit `RiderFellBehind` in `start()`**

In `start()` (lines 45-58), after `_session.value = next` and before/after the existing goal-reached block, add the transition check:

```kotlin
                _session.value = next
                next.participants.forEachIndexed { i, p ->
                    val was = previous.participants.getOrNull(i)?.hasFallenBehind ?: false
                    if (p.hasFallenBehind && !was) {
                        _events.emit(RideSessionEvent.RiderFellBehind(p))
                    }
                }
                if (next.completedLegs.size > previous.completedLegs.size) {
```

(Keep the existing goal-reached `if` block that follows.)

- [ ] **Step 6: Run to verify group tests pass**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorGroupTest"`
Expected: PASS (3 tests).

- [ ] **Step 7: Run the full simulator + calc suite to confirm no regressions**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.*"`
Expected: PASS — including the pre-existing `RideSimulatorTest` and `RideSimulatorStatsTest`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt \
        app/src/test/java/com/valid/motouring/simulation/RideSimulatorGroupTest.kt
git commit -m "feat(group-ride): simulator moves the whole pack and detects fall-behind

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Simulator driver methods — setRole, regroup, fuel, force-behind

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt` (add methods after `simulateDrift()`, ~line 86; add imports)
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorDriversTest.kt` (create)

**Interfaces:**
- Consumes: `withRole` (Task 2), `GroupSignal`, `GroupSignalType`, `RiderRole`, `PointOfInterest`, `RideSessionEvent.GroupSignalRaised` (Task 1).
- Produces: `fun setRole(userId: String, role: RiderRole)`, `fun broadcastRegroup()`, `fun callFuel(nearestFuel: PointOfInterest?)`, `fun forceSweepBehind()`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/RideSimulatorDriversTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.GroupSignalType
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorDriversTest {

    private val route = listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.1875, 106.8271))

    private fun session(drift: Double = 0.0, regrouping: Boolean = false) = RideSession(
        id = "g",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = assignInitialRoles(
            listOf(
                RideParticipantState("u-me", "Rafi", 0, route.first()),
                RideParticipantState("u-2", "Dinda", 0, route.first()),
                RideParticipantState("u-3", "Bagas", 0, route.first()),
            ),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
        sweepDriftMeters = drift,
        isRegrouping = regrouping,
    )

    @Test
    fun `setRole moves the badge and demotes the old lead`() = runTest {
        val sim = RideSimulator(this, session())
        sim.setRole("u-2", RiderRole.LEAD)
        val ps = sim.session.value.participants
        assertEquals(RiderRole.LEAD, ps.first { it.userId == "u-2" }.role)
        assertEquals(RiderRole.RIDER, ps.first { it.userId == "u-me" }.role)
    }

    @Test
    fun `broadcastRegroup flags regrouping and emits a REGROUP signal`() = runTest {
        val sim = RideSimulator(this, session(drift = 500.0))
        val event = async { sim.events.first() }
        sim.broadcastRegroup()
        assertTrue(sim.session.value.isRegrouping)
        val signal = (event.await() as RideSessionEvent.GroupSignalRaised).signal
        assertEquals(GroupSignalType.REGROUP, signal.type)
        assertEquals("Rafi", signal.fromName)
    }

    @Test
    fun `regrouping closes the sweep drift over repeated ticks`() = runTest {
        var s = session(drift = 600.0, regrouping = true)
        repeat(20) { s = RideSimulator.advance(s) }
        assertEquals(0.0, s.sweepDriftMeters, 0.001)
        assertTrue(!s.isRegrouping)
    }

    @Test
    fun `callFuel emits a FUEL signal carrying the rally poi`() = runTest {
        val poi = PointOfInterest("p1", "Pertamina", PoiType.GAS_STATION, route.first(), setOf(VehicleType.MOTORCYCLE), 4.3)
        val sim = RideSimulator(this, session())
        val event = async { sim.events.first() }
        sim.callFuel(poi)
        val signal = (event.await() as RideSessionEvent.GroupSignalRaised).signal
        assertEquals(GroupSignalType.FUEL, signal.type)
        assertEquals("Pertamina", signal.rallyPoi?.name)
    }

    @Test
    fun `forceSweepBehind drives the sweep past the threshold on the next tick`() = runTest {
        val sim = RideSimulator(this, session())
        sim.forceSweepBehind()
        assertTrue(sim.session.value.sweepDriftMeters >= 800.0)
        val next = RideSimulator.advance(sim.session.value)
        assertTrue(next.participants.last().hasFallenBehind)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorDriversTest"`
Expected: FAIL — `setRole` / `broadcastRegroup` / `callFuel` unresolved.

- [ ] **Step 3: Add imports**

In `RideSimulator.kt`, add these imports (alongside the existing `com.valid.motouring.data.model.*` imports):

```kotlin
import com.valid.motouring.data.model.GroupSignal
import com.valid.motouring.data.model.GroupSignalType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RiderRole
```

- [ ] **Step 4: Add the driver methods**

In `RideSimulator.kt`, after `simulateDrift()` (line 86) and before `stop()`, add:

```kotlin
    fun setRole(userId: String, role: RiderRole) {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        _session.value = current.copy(participants = withRole(current.participants, userId, role))
    }

    fun broadcastRegroup() {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        _session.value = current.copy(isRegrouping = true)
        val self = current.participants.firstOrNull() ?: return
        scope.launch {
            _events.emit(RideSessionEvent.GroupSignalRaised(GroupSignal(GroupSignalType.REGROUP, self.userId, self.name)))
        }
    }

    fun callFuel(nearestFuel: PointOfInterest?) {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        _session.value = current.copy(isRegrouping = true) // rally = tighten the pack
        val self = current.participants.firstOrNull() ?: return
        scope.launch {
            _events.emit(
                RideSessionEvent.GroupSignalRaised(
                    GroupSignal(GroupSignalType.FUEL, self.userId, self.name, rallyPoi = nearestFuel),
                ),
            )
        }
    }

    /** Debug: shove the sweep past the fall-behind threshold so the auto regroup event can be demoed on demand. */
    fun forceSweepBehind() {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        _session.value = current.copy(sweepDriftMeters = SWEEP_DRIFT_MAX, isRegrouping = false)
    }
```

`SWEEP_DRIFT_MAX` is the private companion const defined in Task 3; instance methods of `RideSimulator` can read it directly, so nothing new needs to be exposed.

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorDriversTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt \
        app/src/test/java/com/valid/motouring/simulation/RideSimulatorDriversTest.kt
git commit -m "feat(group-ride): simulator drivers for role, regroup, fuel, force-behind

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: ViewModel pass-throughs + POI injection + role auto-assign

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt:181-190`

**Interfaces:**
- Consumes: simulator drivers (Task 4), `assignInitialRoles` / `nearestGasStation` (Task 2), `PoiRepository`.
- Produces: `RideSessionViewModel.setRole`, `.broadcastRegroup`, `.callFuel`, `.forceSweepBehind`; `factory(... , poiRepository: PoiRepository)`.

This task is verified by **build + existing tests green** (the pure logic it calls is already unit-tested in Tasks 2–4; `RideSessionViewModel` starts a coroutine in `init` and is not unit-tested in this codebase — matching the existing test layout).

- [ ] **Step 1: Add imports + constructor param**

In `RideSessionViewModel.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.GroupSignal
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.repository.PoiRepository
import com.valid.motouring.simulation.assignInitialRoles
import com.valid.motouring.simulation.nearestGasStation
```

Add `poiRepository` to the primary constructor (line 27-31):

```kotlin
class RideSessionViewModel(
    initialSession: RideSession,
    private val rideRepository: RideRepository,
    private val badgeRepository: BadgeRepository,
    private val poiRepository: PoiRepository,
) : ViewModel() {
```

- [ ] **Step 2: Add the driver methods**

After `fun simulateDrift() = simulator.simulateDrift()` (line 44), add:

```kotlin
    fun setRole(userId: String, role: RiderRole) = simulator.setRole(userId, role)

    fun broadcastRegroup() = simulator.broadcastRegroup()

    fun forceSweepBehind() = simulator.forceSweepBehind()

    fun callFuel() {
        val self = simulator.session.value.participants.firstOrNull()
        val nearest = self?.let { nearestGasStation(poiRepository.observePois().value, it.position) }
        simulator.callFuel(nearest)
    }
```

- [ ] **Step 3: Auto-assign roles + thread poiRepository through the factory**

In the `factory(...)` signature (lines 66-73), add the parameter:

```kotlin
        fun factory(
            vehicleType: VehicleType,
            isGroup: Boolean,
            initialGoal: RideGoal?,
            userRepository: UserRepository,
            rideBuddyRepository: RideBuddyRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
            poiRepository: PoiRepository,
        ) = viewModelFactory {
```

Wrap the built participant list with `assignInitialRoles`. Change line 78 `val participants = buildList {` ... to assign into a raw list and then roles. Replace `val participants = buildList {` with `val rawParticipants = buildList {` and after the closing `}` of the buildList (line 99), add:

```kotlin
                val participants = assignInitialRoles(rawParticipants)
```

Pass `poiRepository` into the constructor call (line 113):

```kotlin
                RideSessionViewModel(initialSession, rideRepository, badgeRepository, poiRepository)
```

- [ ] **Step 4: Pass poiRepository at the nav call site**

In `MotouringNavHost.kt`, in the `RideSessionViewModel.factory(...)` call (lines 182-190), add the argument after `badgeRepository = appContainer.badgeRepository,`:

```kotlin
                    badgeRepository = appContainer.badgeRepository,
                    poiRepository = appContainer.poiRepository,
```

- [ ] **Step 5: Verify build + full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat(group-ride): VM drivers, POI injection, auto role assignment

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Map — role marker styles + self ring

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/components/map/MotouringMap.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/components/map/MarkerStyleTest.kt` (create)

**Interfaces:**
- Produces: `MarkerStyle.LEAD/SWEEP/RIDER/BEHIND`; `MapMarker.isSelf: Boolean`; role colors in `MarkerStyle.color()`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/ui/components/map/MarkerStyleTest.kt`:

```kotlin
package com.valid.motouring.ui.components.map

import com.valid.motouring.ui.theme.MotouringColors
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerStyleTest {

    @Test
    fun `role styles map to the analog-dash role colors`() {
        assertEquals(MotouringColors.goal, MarkerStyle.LEAD.color())
        assertEquals(MotouringColors.poiRest, MarkerStyle.SWEEP.color())
        assertEquals(MotouringColors.rider, MarkerStyle.RIDER.color())
        assertEquals(MotouringColors.riderCoral, MarkerStyle.BEHIND.color())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.components.map.MarkerStyleTest"`
Expected: FAIL — `MarkerStyle.LEAD` unresolved.

- [ ] **Step 3: Extend the enum, colors, and marker data**

In `MotouringMap.kt`, replace line 33-34:

```kotlin
enum class MarkerStyle { SELF, BUDDY, POI_FUEL, POI_REPAIR, POI_REST, LEAD, SWEEP, RIDER, BEHIND }
data class MapMarker(val id: String, val point: GeoPoint, val style: MarkerStyle, val selected: Boolean = false, val isSelf: Boolean = false)
```

Extend `MarkerStyle.color()` (lines 43-49) with the new cases:

```kotlin
fun MarkerStyle.color(): ComposeColor = when (this) {
    MarkerStyle.SELF -> MotouringColors.rider
    MarkerStyle.BUDDY -> MotouringColors.poiRest
    MarkerStyle.POI_FUEL -> MotouringColors.poiFuel
    MarkerStyle.POI_REPAIR -> MotouringColors.poiRepair
    MarkerStyle.POI_REST -> MotouringColors.poiRest
    MarkerStyle.LEAD -> MotouringColors.goal
    MarkerStyle.SWEEP -> MotouringColors.poiRest
    MarkerStyle.RIDER -> MotouringColors.rider
    MarkerStyle.BEHIND -> MotouringColors.riderCoral
}
```

- [ ] **Step 4: Draw the self ring**

Add the `isSelf` feature property in `markerFeatureCollection` (after line 134 `addBooleanProperty("selected", m.selected)`):

```kotlin
            addBooleanProperty("isSelf", m.isSelf)
```

In `addMarkerSourceAndLayer` (lines 140-163), make the stroke reflect `isSelf` — replace the two stroke property lines (159-160) with:

```kotlin
            PropertyFactory.circleStrokeColor(
                Expression.switchCase(
                    Expression.get("isSelf"),
                    Expression.color(com.valid.motouring.ui.theme.OffWhite.toArgb()),
                    Expression.color(Charcoal950.toArgb()),
                ),
            ),
            PropertyFactory.circleStrokeWidth(
                Expression.switchCase(Expression.get("isSelf"), Expression.literal(3f), Expression.literal(2f)),
            ),
```

- [ ] **Step 5: Run to verify the color test passes + build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.components.map.MarkerStyleTest"`
Expected: PASS.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/map/MotouringMap.kt \
        app/src/test/java/com/valid/motouring/ui/components/map/MarkerStyleTest.kt
git commit -m "feat(group-ride): role marker styles + self ring on MotouringMap

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: In-ride map HUD — role-colored markers + fuel rally marker

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt`

**Interfaces:**
- Consumes: role marker styles + `MapMarker.isSelf` (Task 6), `RideParticipantState.role`/`.hasFallenBehind` (Task 1).
- Produces: `RideSessionHud(session, rallyPoi: PointOfInterest? = null, modifier)`.

UI/interop — verified by build (headless), `@Preview`, and on-device review.

- [ ] **Step 1: Add a role→style mapper and rebuild markers**

In `RideSessionHud.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.RiderRole
```

Add a private mapper below the imports (before `@Composable`):

```kotlin
private fun RiderRole.markerStyle(hasFallenBehind: Boolean): MarkerStyle = when {
    hasFallenBehind -> MarkerStyle.BEHIND
    this == RiderRole.LEAD -> MarkerStyle.LEAD
    this == RiderRole.SWEEP -> MarkerStyle.SWEEP
    else -> MarkerStyle.RIDER
}
```

Change the composable signature (line 23) to accept the rally POI:

```kotlin
fun RideSessionHud(session: RideSession, rallyPoi: PointOfInterest? = null, modifier: Modifier = Modifier) {
```

Replace the marker construction (lines 25-31) with role-based markers plus the optional rally marker:

```kotlin
        val riderMarkers = session.participants.mapIndexed { i, p ->
            MapMarker(
                id = p.userId,
                point = p.position,
                style = p.role.markerStyle(p.hasFallenBehind),
                isSelf = i == 0,
            )
        }
        val markers = if (rallyPoi != null) {
            riderMarkers + MapMarker("rally-${rallyPoi.id}", rallyPoi.location, MarkerStyle.POI_FUEL, selected = true)
        } else {
            riderMarkers
        }
```

- [ ] **Step 2: Build + previews**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Note for the user (on-device / Android Studio): the two `@Preview`s in this file now render the pack in role colors once Task 11 updates the preview sessions.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt
git commit -m "feat(group-ride): role-colored rider markers + fuel rally marker in HUD

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Dashboard — Stats/Pack toggle, formation list, role reassignment

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideDashboard.kt`

**Interfaces:**
- Consumes: `sortedByPackPosition` / `gapsToAheadMeters` (Task 2), `RiderRole` (Task 1), `MotouringColors`.
- Produces: `RideDashboard(session, onSetRole: (String, RiderRole) -> Unit, modifier)`.

UI — verified by build + on-device.

- [ ] **Step 1: Add imports**

In `RideDashboard.kt`, add:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.simulation.gapsToAheadMeters
import com.valid.motouring.simulation.sortedByPackPosition
import com.valid.motouring.ui.theme.MotouringColors
```

(`getValue` is already imported at line 24.)

- [ ] **Step 2: Add the toggle to `RideDashboard`**

Replace the `RideDashboard` body (lines 51-73) with a version that carries a Stats/Pack toggle and the reassign callback:

```kotlin
@Composable
fun RideDashboard(
    session: RideSession,
    onSetRole: (String, RiderRole) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showPack by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val goal = session.activeGoal
            if (session.mode == RideMode.GOAL && goal != null) {
                val progress = (session.distanceMeters / goal.targetDistanceMeters).toFloat().coerceIn(0f, 1f)
                InstrumentRing(progress = progress, size = 72.dp) {
                    Text("%.1f".format(session.toGoalMeters() / 1000.0), style = MotouringTextStyles.statValue)
                    Text("KM LEFT", style = MotouringTextStyles.statLabel, color = Muted)
                }
            } else {
                InstrumentRing(progress = 0f, size = 72.dp) {
                    Text("${session.activeLegDurationSeconds() / 60}", style = MotouringTextStyles.statValue)
                    Text("MIN", style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                StatPackToggle(showPack = showPack, onToggle = { showPack = it })
                Spacer(Modifier.height(8.dp))
                if (showPack) {
                    FormationList(session.participants, onSetRole)
                } else {
                    StatGrid(session)
                }
            }
        }
        if (!showPack) {
            Spacer(Modifier.height(12.dp))
            GroupBar(session.participants)
        }
    }
}

@Composable
private fun StatPackToggle(showPack: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ToggleCell("Stats", selected = !showPack, modifier = Modifier.weight(1f)) { onToggle(false) }
        ToggleCell("Pack", selected = showPack, modifier = Modifier.weight(1f)) { onToggle(true) }
    }
}

@Composable
private fun ToggleCell(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) Charcoal700 else ComposeColor.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MotouringColors.rider else Muted)
    }
}
```

- [ ] **Step 3: Add the `FormationList` composable**

Add at the end of the file:

```kotlin
@Composable
private fun FormationList(participants: List<RideParticipantState>, onSetRole: (String, RiderRole) -> Unit) {
    val sorted = sortedByPackPosition(participants)
    val gaps = gapsToAheadMeters(sorted)
    Column(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(8.dp)) {
        sorted.forEachIndexed { index, p ->
            FormationRow(participant = p, gapAheadMeters = gaps[index], onSetRole = onSetRole)
        }
    }
}

@Composable
private fun FormationRow(participant: RideParticipantState, gapAheadMeters: Double, onSetRole: (String, RiderRole) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(roleColor(participant)),
                contentAlignment = Alignment.Center,
            ) { Text(participant.name.take(1), color = Color(0xFF100E0C), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(participant.name, style = MaterialTheme.typography.bodyMedium)
                if (gapAheadMeters > 0.0) {
                    Text("%.1f km back".format(gapAheadMeters / 1000.0), style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
            if (participant.isSpeaking) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MotouringColors.speaking))
                Spacer(Modifier.width(8.dp))
            }
            RoleBadge(participant)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Make Lead") }, onClick = { onSetRole(participant.userId, RiderRole.LEAD); menuOpen = false })
            DropdownMenuItem(text = { Text("Make Sweep") }, onClick = { onSetRole(participant.userId, RiderRole.SWEEP); menuOpen = false })
            DropdownMenuItem(text = { Text("Make Rider") }, onClick = { onSetRole(participant.userId, RiderRole.RIDER); menuOpen = false })
        }
    }
}

private fun roleColor(p: RideParticipantState): ComposeColor = when {
    p.hasFallenBehind -> MotouringColors.riderCoral
    p.role == RiderRole.LEAD -> MotouringColors.goal
    p.role == RiderRole.SWEEP -> MotouringColors.poiRest
    else -> MotouringColors.rider
}

@Composable
private fun RoleBadge(p: RideParticipantState) {
    val (label, color) = when (p.role) {
        RiderRole.LEAD -> "LEAD" to MotouringColors.goal
        RiderRole.SWEEP -> "SWEEP" to MotouringColors.poiRest
        RiderRole.RIDER -> return
    }
    Text(label, style = MotouringTextStyles.statLabel, color = color)
}
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideDashboard.kt
git commit -m "feat(group-ride): Stats/Pack toggle, formation list, role-reassign menu

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Banners — regroup + fuel call

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionBanners.kt`

**Interfaces:**
- Produces: `RegroupBanner(message: String, modifier)`, `FuelCallBanner(fromName: String, poiName: String?, modifier)`.

UI — verified by build + `@Preview` + on-device.

- [ ] **Step 1: Add the two banners**

In `RideSessionBanners.kt`, add (after `DriftToast`, before the previews):

```kotlin
@Composable
fun RegroupBanner(message: String, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = com.valid.motouring.ui.theme.MotouringColors.riderCoral,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
fun FuelCallBanner(fromName: String, poiName: String?, modifier: Modifier = Modifier) {
    val text = if (poiName != null) "$fromName needs fuel — rally at $poiName" else "$fromName needs fuel — find a stop"
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = com.valid.motouring.ui.theme.MotouringColors.poiFuel,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}
```

- [ ] **Step 2: Add previews**

Add two more preview functions alongside the existing ones:

```kotlin
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RegroupBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme { RegroupBanner("Bagas fell behind — regrouping") }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FuelCallBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme { FuelCallBanner("Dinda", "Pertamina Sudirman") }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionBanners.kt
git commit -m "feat(group-ride): regroup and fuel-call banners

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Screen — consume events, banner state, action row

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt`

**Interfaces:**
- Consumes: `RegroupBanner` / `FuelCallBanner` (Task 9), VM drivers (Task 5), `RideSessionHud(rallyPoi=...)` (Task 7), `RideDashboard(onSetRole=...)` (Task 8), `RideSessionEvent.RiderFellBehind` / `GroupSignalRaised`, `GroupSignal`, `GroupSignalType` (Task 1).

UI — verified by build + on-device.

- [ ] **Step 1: Add imports + banner state**

In `RideSessionScreen.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.GroupSignal
import com.valid.motouring.data.model.GroupSignalType
import com.valid.motouring.data.model.PointOfInterest
```

Add state next to the existing flags (after line 39):

```kotlin
    var regroupMessage by remember { mutableStateOf<String?>(null) }
    var fuelSignal by remember { mutableStateOf<GroupSignal?>(null) }
    var rallyPoi by remember { mutableStateOf<PointOfInterest?>(null) }
```

- [ ] **Step 2: Handle the new events**

Replace the no-op branches added in Task 1 (`is RideSessionEvent.RiderFellBehind -> {}` / `is RideSessionEvent.GroupSignalRaised -> {}`) with:

```kotlin
                is RideSessionEvent.RiderFellBehind ->
                    regroupMessage = "${event.participant.name} fell behind — regrouping"
                is RideSessionEvent.GroupSignalRaised -> when (event.signal.type) {
                    GroupSignalType.REGROUP -> regroupMessage = "Regroup — wait for me"
                    GroupSignalType.FUEL -> {
                        fuelSignal = event.signal
                        rallyPoi = event.signal.rallyPoi
                    }
                }
```

- [ ] **Step 3: Add auto-dismiss timers**

After the existing `LaunchedEffect(showDriftToast)` block (line 81), add:

```kotlin
    LaunchedEffect(regroupMessage) {
        if (regroupMessage != null) { delay(4_000); regroupMessage = null }
    }
    LaunchedEffect(fuelSignal) {
        if (fuelSignal != null) { delay(6_000); fuelSignal = null }
    }
    LaunchedEffect(rallyPoi) {
        if (rallyPoi != null) { delay(10_000); rallyPoi = null }
    }
```

- [ ] **Step 4: Pass rallyPoi to the HUD and onSetRole to the dashboard**

Update the HUD call (line 85):

```kotlin
            RideSessionHud(session = session, rallyPoi = rallyPoi, modifier = Modifier.weight(0.55f).fillMaxWidth())
```

Update the dashboard call (line 87):

```kotlin
                RideDashboard(session = session, onSetRole = { userId, role -> viewModel.setRole(userId, role) })
```

- [ ] **Step 5: Add the Regroup / Fuel action row + debug trigger**

Replace `RideDebugControls(...)` call (lines 89-94) and its definition (lines 136-152) so the row carries the real group actions. New call:

```kotlin
                RideDebugControls(
                    showSetGoal = session.mode == RideMode.ENDLESS,
                    onSetGoal = { showChoiceSheet = true; showDriftToast = false },
                    onRegroup = { viewModel.broadcastRegroup() },
                    onFuel = { viewModel.callFuel() },
                    onForceBehind = { viewModel.forceSweepBehind() },
                    onDrift = { viewModel.simulateDrift() },
                    onEnd = { onEndRide(viewModel.endRide()) },
                )
```

New definition:

```kotlin
@Composable
private fun RideDebugControls(
    showSetGoal: Boolean,
    onSetGoal: () -> Unit,
    onRegroup: () -> Unit,
    onFuel: () -> Unit,
    onForceBehind: () -> Unit,
    onDrift: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onRegroup) { Text("Regroup") }
        TextButton(onClick = onFuel) { Text("Fuel") }
        if (showSetGoal) TextButton(onClick = onSetGoal) { Text("Goal") }
        TextButton(onClick = onForceBehind) { Text("Behind") }
        TextButton(onClick = onDrift) { Text("Off-route") }
        Spacer(Modifier.weight(1f))
        Button(onClick = onEnd) { Text("End") }
    }
}
```

- [ ] **Step 6: Render the banners**

At the end of the outer `Box` (after the `if (showDriftToast)` block, line 132), add:

```kotlin
        regroupMessage?.let {
            RegroupBanner(message = it, modifier = Modifier.align(Alignment.BottomCenter))
        }
        fuelSignal?.let {
            FuelCallBanner(fromName = it.fromName, poiName = it.rallyPoi?.name, modifier = Modifier.align(Alignment.BottomCenter))
        }
```

- [ ] **Step 7: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt
git commit -m "feat(group-ride): wire regroup/fuel events, banners, and action row

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Fake data — role-aware group preview sessions

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Consumes: `RiderRole` (Task 1). Produces role-aware preview sessions so `@Preview`s and screenshot builds render the pack + a fallen-behind sweep.

UI/preview data — verified by build + on-device previews.

- [ ] **Step 1: Enrich the preview sessions**

In `FakeDataProvider.kt`, replace `previewRideSessionWithGoal()` (lines 144-155) with a 3-rider group so the pack UI renders:

```kotlin
    fun previewRideSessionWithGoal(): RideSession = RideSession(
        id = "preview-goal",
        vehicleType = VehicleType.MOTORCYCLE,
        route = sampleRoute,
        participants = listOf(
            RideParticipantState("u-me", "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute[4], role = RiderRole.LEAD, distanceAlongRouteMeters = 6_000.0),
            RideParticipantState("u-2", "Dinda", R.drawable.ic_avatar_placeholder, sampleRoute[3], isSpeaking = true, role = RiderRole.RIDER, distanceAlongRouteMeters = 5_880.0),
            RideParticipantState("u-3", "Bagas", R.drawable.ic_avatar_placeholder, sampleRoute[1], role = RiderRole.SWEEP, distanceAlongRouteMeters = 5_400.0, hasFallenBehind = true),
        ),
        distanceMeters = 6_000.0,
        speedKmh = 28.0,
        elapsedSeconds = 720,
        status = RideSessionStatus.ACTIVE,
        mode = RideMode.GOAL,
        activeGoal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0),
    )
```

Replace `previewRideSessionEndless()` (lines 157-170) participant line (161) with a two-rider group:

```kotlin
        participants = listOf(
            RideParticipantState("u-me", "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute[4], role = RiderRole.LEAD, distanceAlongRouteMeters = 12_500.0),
            RideParticipantState("u-2", "Dinda", R.drawable.ic_avatar_placeholder, sampleRoute[2], role = RiderRole.SWEEP, distanceAlongRouteMeters = 12_050.0),
        ),
```

(Add `import com.valid.motouring.data.model.RiderRole` — or rely on the existing wildcard `import com.valid.motouring.data.model.*` on line 4, which already covers it. No new import needed.)

- [ ] **Step 2: Build + full test suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(group-ride): role-aware group preview sessions

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 11)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (model, group calcs, simulator group + drivers, marker styles, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green with no map token.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): start a **group** ride and confirm — role-colored markers with a self ring; the Stats/Pack toggle; the formation list with gaps, speaking pulse, and a coral "behind" rider; tapping a rider reassigns the role (badge + marker color update); the "Behind" debug button (or ~15s of riding) raises the auto regroup banner; "Regroup" tightens the pack; "Fuel" raises the fuel banner and highlights the nearest gas-station marker.
