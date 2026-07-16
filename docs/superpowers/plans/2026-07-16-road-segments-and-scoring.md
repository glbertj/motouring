# Road Segments & Scoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add twisty-road segments with time leaderboards (browse + detail), a per-ride ride score (headline + Lean/Smoothness/Pace) on the summary, and a light seeded segment-result callout.

**Architecture:** A pure scoring calc (`ScoringCalculations`, TDD'd) computes the ride score and leaderboard ranks. `RideScore`/`SegmentResult` ride on `RideHistoryEntry` (score computed in the pure `toHistoryEntry`; the repo-dependent segment result attached in `endRide`), so `RideSummaryScreen` stays a pure display. Segments live in a seeded `SegmentRepository`, browsed via a new Segments list + detail (leaderboard) reached from the Rides tab.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), MVVM, kotlinx-coroutines, JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies.**
- **Direct-to-`main`, push after every task** (documented project norm; no branch/PR). Each commit message ends with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`. The segments list, leaderboard, and ride-score block are verified **on-device by the user**.
- **Additive model changes only:** `RideHistoryEntry.rideScore`/`.segmentResult` are defaulted so existing construction sites keep compiling.
- **Colors from tokens only:** grade/score use existing `MotouringColors` (e.g. `goal` orange for the score, `statusOk/statusDueSoon/statusOverdue` for grade bands if desired); no new hexes.
- **Scoring is illustrative & deterministic:** the exact formula below is the source of truth for the tests; it's a simulated proxy, not real riding dynamics.

## File Structure

| File | Responsibility |
| --- | --- |
| `data/model/RoadSegment.kt` (create) | `SegmentTime`, `Twistiness`, `RoadSegment`, `RideScore`, `SegmentResult` |
| `data/model/RideHistoryEntry.kt` (modify) | `+ rideScore`, `+ segmentResult` |
| `simulation/ScoringCalculations.kt` (create) | pure `rideScore` / `sortedByTime` / `rankOf` |
| `simulation/RideSessionCalculations.kt` (modify) | `toHistoryEntry` attaches `rideScore` |
| `data/fake/FakeDataProvider.kt` (modify) | seed segments + leaderboards + `rideScore`/`segmentResult` on sample history |
| `data/repository/SegmentRepository.kt` (create) | seeded segments, `observeSegments`/`segments`/`segment` |
| `di/AppContainer.kt` (modify) | register `segmentRepository` |
| `ui/rides/RideSessionViewModel.kt` (modify) | `endRide` attaches a `SegmentResult` (injects `SegmentRepository`) |
| `navigation/MotouringNavHost.kt` (modify) | pass `segmentRepository` into the ride-session factory; add segments routes |
| `ui/rides/RideSummaryScreen.kt` (modify) | ride-score block + segment-result callout |
| `ui/segments/SegmentsScreen.kt` + `SegmentsViewModel.kt` (create) | segments browse |
| `ui/segments/SegmentDetailScreen.kt` + `SegmentDetailViewModel.kt` (create) | leaderboard detail |
| `ui/rides/RidesHistoryScreen.kt` (modify) | "Segments" entry |
| `navigation/Destinations.kt` + `ui/main/MainScaffold.kt` (modify) | segments routes + wiring |

Command shorthand:
- Single test class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- Full suite: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Model — segments, scores, history fields

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/model/RoadSegment.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideHistoryEntry.kt`
- Test: `app/src/test/java/com/valid/motouring/data/model/RoadSegmentTest.kt` (create)

**Interfaces:**
- Produces: `SegmentTime`, `enum Twistiness { MELLOW, FLOWING, TECHNICAL }`, `RoadSegment`, `RideScore`, `SegmentResult`; `RideHistoryEntry.rideScore: RideScore?`, `.segmentResult: SegmentResult?`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/model/RoadSegmentTest.kt`:

```kotlin
package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoadSegmentTest {

    @Test
    fun `road segment carries its leaderboard`() {
        val seg = RoadSegment(
            id = "seg-1", name = "Puncak Pass", region = "Bogor", lengthKm = 8.1,
            twistiness = Twistiness.TECHNICAL, routePreviewRes = 0,
            leaderboard = listOf(SegmentTime("u-me", "Rafi", 0, timeSeconds = 512)),
        )
        assertEquals(1, seg.leaderboard.size)
        assertEquals(512, seg.leaderboard.first().timeSeconds)
    }

    @Test
    fun `history entry defaults score and segment result to null`() {
        val entry = RideHistoryEntry("r", "Ride", VehicleType.MOTORCYCLE, 1000.0, 100, 30.0, 0, emptyList(), 0)
        assertNull(entry.rideScore)
        assertNull(entry.segmentResult)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.RoadSegmentTest"`
Expected: FAIL — `RoadSegment` unresolved / `rideScore` unknown.

- [ ] **Step 3: Create the segment/score model**

Create `app/src/main/java/com/valid/motouring/data/model/RoadSegment.kt`:

```kotlin
package com.valid.motouring.data.model

data class SegmentTime(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val timeSeconds: Int,
)

enum class Twistiness { MELLOW, FLOWING, TECHNICAL }

data class RoadSegment(
    val id: String,
    val name: String,
    val region: String,
    val lengthKm: Double,
    val twistiness: Twistiness,
    val routePreviewRes: Int,
    val leaderboard: List<SegmentTime>,
)

data class RideScore(
    val overall: Int,
    val grade: String,
    val lean: Int,
    val smoothness: Int,
    val pace: Int,
)

data class SegmentResult(
    val segmentName: String,
    val timeSeconds: Int,
    val rank: Int,
)
```

- [ ] **Step 4: Add the history-entry fields**

In `RideHistoryEntry.kt`, add the two defaulted fields after `legs`:

```kotlin
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
    val rideScore: RideScore? = null,
    val segmentResult: SegmentResult? = null,
)
```

- [ ] **Step 5: Run the test + full build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.RoadSegmentTest"`
Expected: PASS (2 tests).

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/RoadSegment.kt \
        app/src/main/java/com/valid/motouring/data/model/RideHistoryEntry.kt \
        app/src/test/java/com/valid/motouring/data/model/RoadSegmentTest.kt
git commit -m "feat(segments): road-segment, ride-score, segment-result model

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Pure scoring calculations

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/ScoringCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/ScoringCalculationsTest.kt`

**Interfaces:**
- Consumes: `RideScore`, `SegmentTime` (Task 1).
- Produces: `fun rideScore(maxSpeedKmh, avgSpeedKmh, elevationGainMeters, distanceMeters): RideScore`; `fun sortedByTime(times: List<SegmentTime>): List<SegmentTime>`; `fun rankOf(timeSeconds: Int, times: List<SegmentTime>): Int`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/ScoringCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.SegmentTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringCalculationsTest {

    @Test
    fun `rideScore computes the documented sub-scores and grade for a known ride`() {
        // spread=6, km=12, elevPerKm=25→cap 20; lean=35+24+20=79; smoothness=100-(6/34)*220≈61; pace=(28-15)*4=52; overall=64→C
        val s = rideScore(maxSpeedKmh = 34.0, avgSpeedKmh = 28.0, elevationGainMeters = 300.0, distanceMeters = 12_000.0)
        assertEquals(79, s.lean)
        assertEquals(61, s.smoothness)
        assertEquals(52, s.pace)
        assertEquals(64, s.overall)
        assertEquals("C", s.grade)
    }

    @Test
    fun `rideScore clamps every sub-score to 0..100 on extreme input`() {
        // a very spiky ride: huge spread → lean caps at 100, smoothness floors at 0, pace caps at 100
        val s = rideScore(maxSpeedKmh = 200.0, avgSpeedKmh = 90.0, elevationGainMeters = 5_000.0, distanceMeters = 40_000.0)
        assertTrue(s.lean in 0..100 && s.smoothness in 0..100 && s.pace in 0..100 && s.overall in 0..100)
        assertEquals(100, s.pace)
        assertEquals(0, s.smoothness)
    }

    @Test
    fun `a smooth, spirited, fast ride grades an A`() {
        // spread=10, km=10, elevPerKm=20; lean=95, smoothness=60, pace=100 → overall=85 → A
        val s = rideScore(maxSpeedKmh = 55.0, avgSpeedKmh = 45.0, elevationGainMeters = 200.0, distanceMeters = 10_000.0)
        assertEquals(85, s.overall)
        assertEquals("A", s.grade)
    }

    @Test
    fun `higher speed spread yields a higher lean sub-score`() {
        val calm = rideScore(30.0, 28.0, 100.0, 10_000.0)
        val spirited = rideScore(45.0, 28.0, 100.0, 10_000.0)
        assertTrue(spirited.lean > calm.lean)
    }

    @Test
    fun `sortedByTime is ascending and rankOf is one-based`() {
        val times = listOf(
            SegmentTime("a", "A", 0, 300),
            SegmentTime("b", "B", 0, 200),
            SegmentTime("c", "C", 0, 250),
        )
        assertEquals(listOf(200, 250, 300), sortedByTime(times).map { it.timeSeconds })
        assertEquals(1, rankOf(190, times))
        assertEquals(2, rankOf(240, times)) // one faster (200)
        assertEquals(4, rankOf(400, times))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.ScoringCalculationsTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the helpers**

Create `app/src/main/java/com/valid/motouring/simulation/ScoringCalculations.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideScore
import com.valid.motouring.data.model.SegmentTime

/**
 * Simulated ride score from the fake telemetry. Deterministic; illustrative coefficients (see the spec).
 * - lean: rewards speed spread (max−avg) and elevation-per-km (capped so the sim's large elevation can't dominate)
 * - smoothness: penalizes a large spread relative to top speed
 * - pace: rewards average speed above a walking-pace floor
 */
fun rideScore(maxSpeedKmh: Double, avgSpeedKmh: Double, elevationGainMeters: Double, distanceMeters: Double): RideScore {
    val spread = (maxSpeedKmh - avgSpeedKmh).coerceAtLeast(0.0)
    val km = (distanceMeters / 1000.0).coerceAtLeast(0.1)
    val elevPerKm = (elevationGainMeters / km).coerceAtMost(20.0)
    val lean = (35.0 + spread * 4.0 + elevPerKm).coerceIn(0.0, 100.0).toInt()
    val smoothness = (100.0 - (if (maxSpeedKmh > 0) spread / maxSpeedKmh else 0.0) * 220.0).coerceIn(0.0, 100.0).toInt()
    val pace = ((avgSpeedKmh - 15.0) * 4.0).coerceIn(0.0, 100.0).toInt()
    val overall = (lean + smoothness + pace) / 3
    val grade = when {
        overall >= 85 -> "A"
        overall >= 70 -> "B"
        overall >= 55 -> "C"
        else -> "D"
    }
    return RideScore(overall = overall, grade = grade, lean = lean, smoothness = smoothness, pace = pace)
}

fun sortedByTime(times: List<SegmentTime>): List<SegmentTime> = times.sortedBy { it.timeSeconds }

/** 1-based rank a [timeSeconds] would take on this board (number strictly faster, plus one). */
fun rankOf(timeSeconds: Int, times: List<SegmentTime>): Int = times.count { it.timeSeconds < timeSeconds } + 1
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.ScoringCalculationsTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/ScoringCalculations.kt \
        app/src/test/java/com/valid/motouring/simulation/ScoringCalculationsTest.kt
git commit -m "feat(segments): pure ride-score + leaderboard sort/rank calc

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Seed segments + scored sample history

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Produces: `FakeDataProvider.segments: List<RoadSegment>`; `rideScore`/`segmentResult` seeded on the three `rideHistory` entries.

Verified by **build + existing tests green** (seed data; used by later tasks + on-device).

- [ ] **Step 1: Add the segments list**

In `FakeDataProvider.kt`, add a new property (e.g. after `pois`). `u-me` appears on most boards so "your best" works; seg-2 omits `u-me` to show a "no time yet" state:

```kotlin
    val segments = listOf(
        RoadSegment(
            "seg-1", "Sudirman Sprint", "Jakarta", 2.4, Twistiness.MELLOW, R.drawable.img_road_1,
            listOf(
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 182),
                SegmentTime("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 205),
                SegmentTime("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 214),
                SegmentTime("u-4", "Sarah", R.drawable.ic_avatar_placeholder, 231),
            ),
        ),
        RoadSegment(
            "seg-2", "Puncak Pass", "Bogor", 8.1, Twistiness.TECHNICAL, R.drawable.img_road_6,
            listOf(
                SegmentTime("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 498),
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 512),
                SegmentTime("u-5", "Yoga", R.drawable.ic_avatar_placeholder, 540),
            ),
        ),
        RoadSegment(
            "seg-3", "Thamrin Flow", "Jakarta", 3.2, Twistiness.FLOWING, R.drawable.img_road_3,
            listOf(
                SegmentTime("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 268),
                SegmentTime("u-6", "Nadia", R.drawable.ic_avatar_placeholder, 275),
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 290),
            ),
        ),
    )
```

- [ ] **Step 2: Seed scores on the sample history**

In the `rideHistory` list, add `rideScore`/`segmentResult` to the three entries via `.copy(...)` is not possible in a literal — instead append the two args to each `RideHistoryEntry(...)` constructor call. The entries currently end with `..., completedAtEpochSeconds)`; add the named args:

```kotlin
    val rideHistory = listOf(
        RideHistoryEntry("r-1", "Sudirman Sunday Loop", VehicleType.MOTORCYCLE, 18_400.0, 2_700, 24.5, R.drawable.img_road_1, listOf(R.drawable.img_road_2), 1_752_000_000,
            rideScore = RideScore(72, "B", 78, 70, 68), segmentResult = SegmentResult("Sudirman Sprint", 209, 3)),
        RideHistoryEntry("r-2", "Weekend Car Meet", VehicleType.CAR, 42_000.0, 5_400, 28.0, R.drawable.img_road_3, listOf(R.drawable.img_road_4, R.drawable.img_road_5), 1_752_400_000,
            rideScore = RideScore(61, "C", 55, 74, 52), segmentResult = SegmentResult("Thamrin Flow", 281, 2)),
        RideHistoryEntry("r-3", "Night Ride to Puncak", VehicleType.MOTORCYCLE, 65_000.0, 9_000, 26.0, R.drawable.img_road_6, emptyList(), 1_752_800_000,
            rideScore = RideScore(84, "B", 92, 66, 94), segmentResult = SegmentResult("Puncak Pass", 505, 2)),
    )
```

(`RoadSegment`, `SegmentTime`, `Twistiness`, `RideScore`, `SegmentResult` are covered by the file's `import com.valid.motouring.data.model.*` wildcard — no new import.)

- [ ] **Step 3: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(segments): seed segments + scored sample history

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: SegmentRepository

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/repository/SegmentRepository.kt`
- Modify: `app/src/main/java/com/valid/motouring/di/AppContainer.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/SegmentRepositoryTest.kt` (create)

**Interfaces:**
- Consumes: `FakeDataProvider.segments` (Task 3), `RoadSegment` (Task 1).
- Produces: `SegmentRepository` with `observeSegments()`, `segments()`, `segment(id): RoadSegment?`; `AppContainer.segmentRepository`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/repository/SegmentRepositoryTest.kt`:

```kotlin
package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentRepositoryTest {

    @Test
    fun `segments are seeded and segment finds by id`() {
        val repo = SegmentRepository()
        assertTrue(repo.segments().isNotEmpty())
        val first = repo.segments().first()
        assertEquals(first, repo.segment(first.id))
        assertNull(repo.segment("nope"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.SegmentRepositoryTest"`
Expected: FAIL — `SegmentRepository` unresolved.

- [ ] **Step 3: Create the repository**

Create `app/src/main/java/com/valid/motouring/data/repository/SegmentRepository.kt`:

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RoadSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SegmentRepository {
    private val segments = MutableStateFlow(FakeDataProvider.segments)

    fun observeSegments(): StateFlow<List<RoadSegment>> = segments.asStateFlow()

    fun segments(): List<RoadSegment> = segments.value

    fun segment(id: String): RoadSegment? = segments.value.firstOrNull { it.id == id }
}
```

- [ ] **Step 4: Register in AppContainer**

In `AppContainer.kt`, add the import and field:

```kotlin
import com.valid.motouring.data.repository.SegmentRepository
```

```kotlin
    val maintenanceRepository = MaintenanceRepository()
    val segmentRepository = SegmentRepository()
```

- [ ] **Step 5: Run to verify it passes + build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.SegmentRepositoryTest"`
Expected: PASS.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/SegmentRepository.kt \
        app/src/main/java/com/valid/motouring/di/AppContainer.kt \
        app/src/test/java/com/valid/motouring/data/repository/SegmentRepositoryTest.kt
git commit -m "feat(segments): SegmentRepository seeded from fake data

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Attach scoring at ride end

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSessionCalculations.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt` (ride-session factory — add `segmentRepository`)
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSessionCalculationsScoreTest.kt` (create)

**Interfaces:**
- Consumes: `rideScore` / `rankOf` (Task 2), `SegmentRepository` (Task 4), `SegmentResult` (Task 1).
- Produces: `toHistoryEntry` sets `rideScore`; `RideSessionViewModel.endRide()` attaches a `SegmentResult`.

- [ ] **Step 1: Write the failing test (toHistoryEntry attaches a score)**

Create `app/src/test/java/com/valid/motouring/simulation/RideSessionCalculationsScoreTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSessionCalculationsScoreTest {

    @Test
    fun `toHistoryEntry attaches a computed ride score`() {
        val session = RideSession(
            id = "s", vehicleType = VehicleType.MOTORCYCLE,
            route = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.1, 0.1)),
            participants = listOf(RideParticipantState("u-me", "Rafi", 0, GeoPoint(0.0, 0.0))),
            distanceMeters = 12_000.0, speedKmh = 28.0, elapsedSeconds = 1_543,
            status = RideSessionStatus.ACTIVE, maxSpeedKmh = 34.0, elevationGainMeters = 300.0,
        )
        val entry = session.toHistoryEntry(id = "r", completedAtEpochSeconds = 0, routePreviewRes = 0)
        assertNotNull(entry.rideScore)
        assertTrue(entry.rideScore!!.overall in 0..100)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSessionCalculationsScoreTest"`
Expected: FAIL — `entry.rideScore` is null (not attached yet).

- [ ] **Step 3: Attach `rideScore` in `toHistoryEntry`**

In `RideSessionCalculations.kt`, the `toHistoryEntry` function builds a `RideHistoryEntry`. Add `rideScore = ...` to the constructed entry (using the session's telemetry + the existing `avgSpeedKmh` helper):

```kotlin
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
    rideScore = rideScore(maxSpeedKmh, avgSpeedKmh(distanceMeters, elapsedSeconds), elevationGainMeters, distanceMeters),
)
```

(`rideScore` is in the same `simulation` package — no import needed.)

- [ ] **Step 4: Run to verify the test passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSessionCalculationsScoreTest"`
Expected: PASS.

- [ ] **Step 5: Attach the segment result in `endRide`**

In `RideSessionViewModel.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.SegmentResult
import com.valid.motouring.data.repository.SegmentRepository
import com.valid.motouring.simulation.rankOf
```

Add `segmentRepository` to the constructor (after the existing repos):

```kotlin
    private val poiRepository: PoiRepository,
    private val rideBuddyRepository: RideBuddyRepository,
    private val notificationRepository: NotificationRepository,
    private val segmentRepository: SegmentRepository,
) : ViewModel() {
```

In `endRide()`, after `val entry = finalSession.toHistoryEntry(...)`, compute a plausible segment attempt from the ride's average speed over the first seeded segment and attach it before saving. Replace the block that builds + adds the entry:

```kotlin
        val baseEntry = finalSession.toHistoryEntry(
            id = "r-${System.currentTimeMillis()}",
            completedAtEpochSeconds = completedAt,
            routePreviewRes = R.drawable.ic_route_preview_placeholder,
        )
        val segment = segmentRepository.segments().firstOrNull()
        val entry = if (segment != null) {
            val avg = baseEntry.avgSpeedKmh.coerceAtLeast(1.0)
            val timeSeconds = (segment.lengthKm / avg * 3600.0).toInt()
            baseEntry.copy(
                segmentResult = SegmentResult(segment.name, timeSeconds, rankOf(timeSeconds, segment.leaderboard)),
            )
        } else {
            baseEntry
        }
        rideRepository.addHistoryEntry(entry)
```

(Verify the two `badgeRepository.markEarned(...)` calls that follow still reference `entry.legs` — unchanged.)

- [ ] **Step 6: Thread `segmentRepository` through the factory + nav**

In `RideSessionViewModel.factory(...)`, add `segmentRepository: SegmentRepository` as a parameter and pass it to the constructor call (append after `notificationRepository`). In `MotouringNavHost.kt`, the `RideSessionViewModel.factory(...)` call — add:

```kotlin
                    notificationRepository = appContainer.notificationRepository,
                    segmentRepository = appContainer.segmentRepository,
```

- [ ] **Step 7: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSessionCalculations.kt \
        app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/test/java/com/valid/motouring/simulation/RideSessionCalculationsScoreTest.kt
git commit -m "feat(segments): attach ride score + segment result at ride end

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Ride-score block + segment-result callout on the summary

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt`

**Interfaces:**
- Consumes: `RideHistoryEntry.rideScore` / `.segmentResult` (Task 1), `MotouringColors`.

UI — verified by build + `@Preview` + on-device (the sample entries carry a score from Task 3).

- [ ] **Step 1: Add the score block + callout**

In `RideSummaryScreen.kt`, add imports:

```kotlin
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted
```

Insert the Ride Score block after the stats `Row` (after line ~59, before the `if (earnedBadges...)` block):

```kotlin
        entry.rideScore?.let { score ->
            SectionHeader(title = "Ride Score")
            MotouringCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${score.overall}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MotouringColors.goal)
                        Spacer(Modifier.padding(start = 6.dp))
                        Text("/ 100 · ${score.grade}", style = MaterialTheme.typography.titleMedium, color = Muted)
                    }
                    Spacer(Modifier.height(12.dp))
                    ScoreBar("Lean", score.lean)
                    ScoreBar("Smoothness", score.smoothness)
                    ScoreBar("Pace", score.pace)
                }
            }
            entry.segmentResult?.let { seg ->
                MotouringCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${seg.segmentName} · ${seg.timeSeconds / 60}:${(seg.timeSeconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.bodyMedium)
                        Text("#${seg.rank}", style = MaterialTheme.typography.titleMedium, color = MotouringColors.goal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
```

Add the `ScoreBar` helper at the bottom of the file (before the previews):

```kotlin
@Composable
private fun ScoreBar(label: String, value: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
            Text("$value", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            color = MotouringColors.goal,
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp),
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt
git commit -m "feat(segments): ride-score block + segment-result callout on summary

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Segments browse screen + Rides entry

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/segments/SegmentsViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/segments/SegmentsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/segments/SegmentsViewModelTest.kt` (create)

**Interfaces:**
- Consumes: `SegmentRepository` (Task 4), `RoadSegment`/`SegmentTime` (Task 1), `sortedByTime` (Task 2).
- Produces: `SegmentsViewModel(segmentRepository, currentUserId)` with `segments: StateFlow<List<RoadSegment>>` + `yourBest(segment): SegmentTime?`; `Destinations.SEGMENTS`; `SegmentsScreen(viewModel, onSegmentClick)`; `RidesHistoryScreen(history, onSegmentsClick)`.

- [ ] **Step 1: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/segments/SegmentsViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.segments

import com.valid.motouring.data.repository.SegmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentsViewModelTest {

    private fun vm() = SegmentsViewModel(SegmentRepository(), "u-me")

    @Test
    fun `segments are exposed and yourBest finds the current user's time`() {
        val vm = vm()
        assertTrue(vm.segments.value.isNotEmpty())
        val withMe = vm.segments.value.first { seg -> seg.leaderboard.any { it.userId == "u-me" } }
        assertEquals("u-me", vm.yourBest(withMe)?.userId)
        val withoutMe = vm.segments.value.first { seg -> seg.leaderboard.none { it.userId == "u-me" } }
        assertNull(vm.yourBest(withoutMe))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.segments.SegmentsViewModelTest"`
Expected: FAIL — `SegmentsViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/segments/SegmentsViewModel.kt`:

```kotlin
package com.valid.motouring.ui.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RoadSegment
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.data.repository.SegmentRepository
import kotlinx.coroutines.flow.StateFlow

class SegmentsViewModel(
    segmentRepository: SegmentRepository,
    private val currentUserId: String,
) : ViewModel() {

    val segments: StateFlow<List<RoadSegment>> = segmentRepository.observeSegments()

    fun yourBest(segment: RoadSegment): SegmentTime? =
        segment.leaderboard.firstOrNull { it.userId == currentUserId }

    companion object {
        fun factory(segmentRepository: SegmentRepository, currentUserId: String) = viewModelFactory {
            initializer { SegmentsViewModel(segmentRepository, currentUserId) }
        }
    }
}
```

- [ ] **Step 4: Run to verify the VM test passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.segments.SegmentsViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Create the screen**

Create `app/src/main/java/com/valid/motouring/ui/segments/SegmentsScreen.kt`:

```kotlin
package com.valid.motouring.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Twistiness
import com.valid.motouring.simulation.sortedByTime
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun formatTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
fun SegmentsScreen(viewModel: SegmentsViewModel, onSegmentClick: (String) -> Unit) {
    val segments by viewModel.segments.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Segments", style = MaterialTheme.typography.headlineMedium) }
        items(segments, key = { it.id }) { segment ->
            MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onSegmentClick(segment.id) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(segment.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TwistinessChip(segment.twistiness)
                    }
                    Text("${segment.region} · ${"%.1f".format(segment.lengthKm)} km", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp))
                    val leader = sortedByTime(segment.leaderboard).firstOrNull()
                    val yours = viewModel.yourBest(segment)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Leader ${leader?.let { formatTime(it.timeSeconds) } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("You ${yours?.let { formatTime(it.timeSeconds) } ?: "no time yet"}", style = MaterialTheme.typography.bodySmall, color = if (yours != null) MotouringColors.goal else Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TwistinessChip(t: Twistiness) {
    val (label, color) = when (t) {
        Twistiness.MELLOW -> "Mellow" to MotouringColors.statusOk
        Twistiness.FLOWING -> "Flowing" to MotouringColors.statusDueSoon
        Twistiness.TECHNICAL -> "Technical" to MotouringColors.statusOverdue
    }
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
```

- [ ] **Step 6: Add the destinations + nav route**

In `Destinations.kt`, add all three lines now (the `segmentDetail` route is consumed by this task's nav code even though its composable lands in Task 8; the pattern/builder are inert without the composable, so this is safe):

```kotlin
    const val SEGMENTS = "segments"
    const val SEGMENT_DETAIL_PATTERN = "segment_detail/{segmentId}"
    fun segmentDetail(segmentId: String) = "segment_detail/$segmentId"
```

In `MotouringNavHost.kt`, add a composable (near the other Rides/detail routes):

```kotlin
        composable(Destinations.SEGMENTS) {
            val viewModel: SegmentsViewModel = viewModel(
                factory = SegmentsViewModel.factory(appContainer.segmentRepository, appContainer.userRepository.currentUser().id),
            )
            SegmentsScreen(viewModel = viewModel, onSegmentClick = { id -> navController.navigate(Destinations.segmentDetail(id)) })
        }
```

Add the imports near the other `ui` imports:

```kotlin
import com.valid.motouring.ui.segments.SegmentsScreen
import com.valid.motouring.ui.segments.SegmentsViewModel
```

(`navArgument`, `NavType`, `viewModel`, `composable` are already imported. The `segmentDetail` builder used above was added to `Destinations` in this step.)

- [ ] **Step 7: Add the Rides "Segments" entry**

In `RidesHistoryScreen.kt`, add the param + a top entry. Signature:

```kotlin
fun RidesHistoryScreen(history: List<RideHistoryEntry>, onSegmentsClick: () -> Unit = {}) {
```

No new imports are needed (`MotouringCard`, `Row`, `Arrangement`, `Text`, `MaterialTheme`, `Modifier`, `fillMaxWidth`, `padding` are already imported; `item {}` is a `LazyListScope` member like the existing `itemsIndexed`).

At the top of the `LazyColumn` (before `itemsIndexed`), add:

```kotlin
        item {
            MotouringCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = onSegmentsClick) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🏁 Segments & leaderboards", style = MaterialTheme.typography.titleMedium)
                    Text("›", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
```

- [ ] **Step 8: Wire MainScaffold**

In `MainScaffold.kt`, the Rides tab composable — pass the callback:

```kotlin
                composable(BottomTab.Rides.route) {
                    val history by appContainer.rideRepository.observeHistory().collectAsState()
                    RidesHistoryScreen(history = history, onSegmentsClick = { outerNavController.navigate(Destinations.SEGMENTS) })
                }
```

- [ ] **Step 9: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass (incl. the new VM test).

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/segments/SegmentsViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/segments/SegmentsScreen.kt \
        app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt \
        app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt \
        app/src/test/java/com/valid/motouring/ui/segments/SegmentsViewModelTest.kt
git commit -m "feat(segments): segments browse screen + Rides entry

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Segment detail (leaderboard)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt` (if not already added in Task 7)
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/segments/SegmentDetailViewModelTest.kt` (create)

**Interfaces:**
- Consumes: `SegmentRepository` (Task 4), `sortedByTime`/`rankOf` (Task 2).
- Produces: `SegmentDetailViewModel(segmentRepository, segmentId, currentUserId)` with `state: StateFlow<SegmentDetailState>`; `Destinations.SEGMENT_DETAIL_PATTERN` + `segmentDetail(id)`; `SegmentDetailScreen(viewModel)`.

- [ ] **Step 1: Verify the destination exists**

`Destinations.SEGMENT_DETAIL_PATTERN` + `segmentDetail(id)` were already added in Task 7 Step 6. Confirm they're present in `Destinations.kt`; if for any reason they're missing, add them now:

```kotlin
    const val SEGMENT_DETAIL_PATTERN = "segment_detail/{segmentId}"
    fun segmentDetail(segmentId: String) = "segment_detail/$segmentId"
```

- [ ] **Step 2: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/segments/SegmentDetailViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.segments

import com.valid.motouring.data.repository.SegmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentDetailViewModelTest {

    @Test
    fun `state exposes the segment with a time-sorted board and the user's rank`() {
        val repo = SegmentRepository()
        val seg = repo.segments().first { s -> s.leaderboard.any { it.userId == "u-me" } }
        val vm = SegmentDetailViewModel(repo, seg.id, "u-me")
        val state = vm.state.value
        assertEquals(seg.id, state.segment?.id)
        // board is ascending by time
        val times = state.rankedBoard.map { it.timeSeconds }
        assertEquals(times.sorted(), times)
        // your rank matches your position in the sorted board (1-based)
        val yourTime = seg.leaderboard.first { it.userId == "u-me" }.timeSeconds
        assertEquals(state.rankedBoard.indexOfFirst { it.timeSeconds == yourTime } + 1, state.yourRank)
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.segments.SegmentDetailViewModelTest"`
Expected: FAIL — `SegmentDetailViewModel` unresolved.

- [ ] **Step 4: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailViewModel.kt`:

```kotlin
package com.valid.motouring.ui.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RoadSegment
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.data.repository.SegmentRepository
import com.valid.motouring.simulation.rankOf
import com.valid.motouring.simulation.sortedByTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SegmentDetailState(
    val segment: RoadSegment?,
    val rankedBoard: List<SegmentTime>,
    val yourRank: Int?,
    val currentUserId: String,
)

class SegmentDetailViewModel(
    segmentRepository: SegmentRepository,
    segmentId: String,
    private val currentUserId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(build(segmentRepository.segment(segmentId)))
    val state: StateFlow<SegmentDetailState> = _state.asStateFlow()

    private fun build(segment: RoadSegment?): SegmentDetailState {
        val board = segment?.leaderboard?.let { sortedByTime(it) } ?: emptyList()
        val yourTime = board.firstOrNull { it.userId == currentUserId }?.timeSeconds
        val yourRank = yourTime?.let { rankOf(it, board) }
        return SegmentDetailState(segment, board, yourRank, currentUserId)
    }

    companion object {
        fun factory(segmentRepository: SegmentRepository, segmentId: String, currentUserId: String) = viewModelFactory {
            initializer { SegmentDetailViewModel(segmentRepository, segmentId, currentUserId) }
        }
    }
}
```

- [ ] **Step 5: Run to verify the VM test passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.segments.SegmentDetailViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Create the screen**

Create `app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailScreen.kt`:

```kotlin
package com.valid.motouring.ui.segments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.SegmentTime
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun formatTime(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
fun SegmentDetailScreen(viewModel: SegmentDetailViewModel) {
    val state by viewModel.state.collectAsState()
    val segment = state.segment ?: return
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column {
                Image(
                    painter = painterResource(id = segment.routePreviewRes),
                    contentDescription = segment.name,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.padding(20.dp)) {
                    Text(segment.name, style = MaterialTheme.typography.headlineMedium)
                    Text("${segment.region} · ${"%.1f".format(segment.lengthKm)} km · ${segment.twistiness.name.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    state.yourRank?.let {
                        Text("Your best: #$it of ${state.rankedBoard.size}", style = MaterialTheme.typography.titleSmall, color = MotouringColors.goal, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        itemsIndexed(state.rankedBoard, key = { _, t -> t.userId }) { index, time ->
            LeaderRow(rank = index + 1, time = time, isYou = time.userId == state.currentUserId)
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, time: SegmentTime, isYou: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (isYou) MotouringColors.goal.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$rank", style = MaterialTheme.typography.titleMedium, color = if (rank <= 3) MotouringColors.goal else Muted, modifier = Modifier.width(28.dp))
        Box(Modifier.size(32.dp).clip(CircleShape).background(Charcoal700), contentAlignment = Alignment.Center) {
            Text(time.name.take(1), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.padding(start = 10.dp))
        Text(if (isYou) "${time.name} (you)" else time.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(formatTime(time.timeSeconds), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
```

Add the `width` import used by `LeaderRow`:

```kotlin
import androidx.compose.foundation.layout.width
```

- [ ] **Step 7: Add the nav route**

In `MotouringNavHost.kt`, add the detail composable (id-arg pattern):

```kotlin
        composable(
            Destinations.SEGMENT_DETAIL_PATTERN,
            arguments = listOf(navArgument("segmentId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val segmentId = requireNotNull(backStackEntry.arguments?.getString("segmentId"))
            val viewModel: SegmentDetailViewModel = viewModel(
                factory = SegmentDetailViewModel.factory(appContainer.segmentRepository, segmentId, appContainer.userRepository.currentUser().id),
            )
            SegmentDetailScreen(viewModel = viewModel)
        }
```

Add the imports:

```kotlin
import com.valid.motouring.ui.segments.SegmentDetailScreen
import com.valid.motouring.ui.segments.SegmentDetailViewModel
```

- [ ] **Step 8: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/segments/SegmentDetailScreen.kt \
        app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/test/java/com/valid/motouring/ui/segments/SegmentDetailViewModelTest.kt
git commit -m "feat(segments): segment detail leaderboard screen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 8)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (model, scoring calc, toHistoryEntry-score, segment repo, segments VM, segment-detail VM, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): the ride summary shows a Ride Score card (overall + grade + Lean/Smoothness/Pace bars) and a segment-result callout; the Rides tab shows a "Segments & leaderboards" entry that opens the segments list (each row: name, region, length, twistiness chip, your-best vs leader); tapping a segment opens the leaderboard with your row highlighted and your rank; a freshly completed ride produces a summary with a computed score + segment result.
