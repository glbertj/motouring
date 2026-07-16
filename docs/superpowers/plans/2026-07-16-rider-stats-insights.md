# Rider Stats & Insights Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Rider Stats & Insights dashboard over ride history — lifetime totals, a weekly-distance bar chart, personal records, a moto-vs-car split, and a ride-score sparkline — reached from Profile.

**Architecture:** A pure, TDD'd `InsightsCalculations` aggregates `List<RideHistoryEntry>` into small result types. A snapshot `InsightsViewModel` computes them from `RideRepository`. The `InsightsScreen` lays out stat tiles + three `Canvas`-drawn charts (Analog-Dash palette). No new dependencies.

**Tech Stack:** Kotlin, Jetpack Compose (Material3 + `Canvas`), MVVM, kotlinx-coroutines, JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies** (charts are Compose `Canvas`, as `RidePlaceholderRoute`/`InstrumentRing` already do).
- **Direct-to-`main`, push after every task.** Each commit message ends with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`. The `Canvas` charts are verified **on-device by the user**.
- **Colors from tokens only:** single-series charts use `MotouringColors.goal` (orange); the categorical vehicle split uses `rider` (moto) + `poiRest` (car), both **direct-labeled**; grid/labels use `Muted`/`MutedDim`/`Charcoal600`. No new hexes.
- **Additive:** growing `rideHistory` keeps the existing r-1/r-2/r-3 entries (referenced by other previews) and appends more.

## File Structure

| File | Responsibility |
| --- | --- |
| `simulation/InsightsCalculations.kt` (create) | pure aggregations + result types |
| `data/fake/FakeDataProvider.kt` (modify) | grow `rideHistory` to ~12 across ~9 weeks |
| `ui/insights/InsightsViewModel.kt` (create) | snapshot VM over `RideRepository` |
| `ui/insights/InsightsCharts.kt` (create) | `WeeklyDistanceChart`, `VehicleSplitBar`, `ScoreTrendSparkline` |
| `ui/insights/InsightsScreen.kt` (create) | dashboard layout |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` (modify) | `INSIGHTS` route |
| `ui/profile/ProfileScreen.kt` + `ui/main/MainScaffold.kt` (modify) | "Insights" entry + wiring |

Command shorthand:
- Single test class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- Full suite: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Pure insights calculations

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/InsightsCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/InsightsCalculationsTest.kt`

**Interfaces:**
- Consumes: `RideHistoryEntry`, `VehicleType`, `RideScore` (existing).
- Produces: `LifetimeTotals`, `WeekDistance`, `PersonalRecords`, `VehicleSplit`; `fun lifetimeTotals`, `weeklyDistanceKm`, `personalRecords`, `vehicleSplit`, `rideScoreTrend`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/InsightsCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.RideScore
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculationsTest {

    private val WEEK = 604_800L

    private fun entry(km: Double, sec: Long, avg: Double, type: VehicleType, week: Long, score: Int?) =
        RideHistoryEntry(
            id = "e", title = "Ride", vehicleType = type, distanceMeters = km * 1000.0,
            durationSeconds = sec, avgSpeedKmh = avg, routePreviewRes = 0, photoResList = emptyList(),
            completedAtEpochSeconds = week * WEEK + 100,
            rideScore = score?.let { RideScore(it, "B", it, it, it) },
        )

    private val entries = listOf(
        entry(10.0, 1_800, 20.0, VehicleType.MOTORCYCLE, week = 100, score = 60),
        entry(30.0, 3_600, 30.0, VehicleType.CAR, week = 102, score = 80),   // week 101 is empty → zero-filled
        entry(20.0, 3_600, 24.0, VehicleType.MOTORCYCLE, week = 102, score = null),
    )

    @Test
    fun `lifetime totals sum distance, count, and moving hours`() {
        val t = lifetimeTotals(entries)
        assertEquals(60.0, t.distanceKm, 0.001)
        assertEquals(3, t.rideCount)
        assertEquals((1_800 + 3_600 + 3_600) / 3600.0, t.movingHours, 0.001)
    }

    @Test
    fun `weekly distance buckets by week and zero-fills the span`() {
        val w = weeklyDistanceKm(entries)
        assertEquals(listOf(100, 101, 102), w.map { it.weekIndex })
        assertEquals(listOf(10.0, 0.0, 50.0), w.map { it.distanceKm })
    }

    @Test
    fun `personal records take the max distance, avg speed, and score`() {
        val r = personalRecords(entries)
        assertEquals(30.0, r.longestRideKm, 0.001)
        assertEquals(30.0, r.fastestAvgKmh, 0.001)
        assertEquals(80, r.bestScore)
    }

    @Test
    fun `vehicle split sums distance per type`() {
        val s = vehicleSplit(entries)
        assertEquals(30.0, s.motoKm, 0.001) // 10 + 20
        assertEquals(30.0, s.carKm, 0.001)
    }

    @Test
    fun `score trend is ordered by time and skips score-less rides`() {
        assertEquals(listOf(60, 80), rideScoreTrend(entries))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.InsightsCalculationsTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the helpers**

Create `app/src/main/java/com/valid/motouring/simulation/InsightsCalculations.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.VehicleType

data class LifetimeTotals(val distanceKm: Double, val rideCount: Int, val movingHours: Double)
data class WeekDistance(val weekIndex: Int, val distanceKm: Double)
data class PersonalRecords(val longestRideKm: Double, val fastestAvgKmh: Double, val bestScore: Int)
data class VehicleSplit(val motoKm: Double, val carKm: Double)

private const val WEEK_SECONDS = 604_800L

fun lifetimeTotals(entries: List<RideHistoryEntry>): LifetimeTotals = LifetimeTotals(
    distanceKm = entries.sumOf { it.distanceMeters } / 1000.0,
    rideCount = entries.size,
    movingHours = entries.sumOf { it.durationSeconds } / 3600.0,
)

/** Weekly km, bucketed by absolute week, with the min→max span zero-filled so the bar chart is contiguous. */
fun weeklyDistanceKm(entries: List<RideHistoryEntry>): List<WeekDistance> {
    if (entries.isEmpty()) return emptyList()
    val byWeek = entries
        .groupBy { (it.completedAtEpochSeconds / WEEK_SECONDS).toInt() }
        .mapValues { (_, es) -> es.sumOf { it.distanceMeters } / 1000.0 }
    return (byWeek.keys.min()..byWeek.keys.max()).map { w -> WeekDistance(w, byWeek[w] ?: 0.0) }
}

fun personalRecords(entries: List<RideHistoryEntry>): PersonalRecords = PersonalRecords(
    longestRideKm = (entries.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0,
    fastestAvgKmh = entries.maxOfOrNull { it.avgSpeedKmh } ?: 0.0,
    bestScore = entries.mapNotNull { it.rideScore?.overall }.maxOrNull() ?: 0,
)

fun vehicleSplit(entries: List<RideHistoryEntry>): VehicleSplit = VehicleSplit(
    motoKm = entries.filter { it.vehicleType == VehicleType.MOTORCYCLE }.sumOf { it.distanceMeters } / 1000.0,
    carKm = entries.filter { it.vehicleType == VehicleType.CAR }.sumOf { it.distanceMeters } / 1000.0,
)

fun rideScoreTrend(entries: List<RideHistoryEntry>): List<Int> =
    entries.filter { it.rideScore != null }
        .sortedBy { it.completedAtEpochSeconds }
        .map { it.rideScore!!.overall }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.InsightsCalculationsTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/InsightsCalculations.kt \
        app/src/test/java/com/valid/motouring/simulation/InsightsCalculationsTest.kt
git commit -m "feat(insights): pure ride-history aggregations

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Grow the seeded ride history

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Produces: `rideHistory` grows from 3 to 12 entries spanning ~9 weeks (the existing r-1/r-2/r-3 kept; r-4..r-12 appended), each with a `rideScore` for the trend.

Verified by **build + existing tests green** (seed data).

- [ ] **Step 1: Append nine dated entries**

In `FakeDataProvider.kt`, the `rideHistory` list currently ends with the r-3 entry. Keep the three existing entries and append these nine (varied week, vehicle, distance, score; `completedAtEpochSeconds` steps back ~1 week each from r-1's `1_752_000_000`). Add them inside the `listOf(...)`, after r-3:

```kotlin
        RideHistoryEntry("r-4", "Kemang Evening Spin", VehicleType.MOTORCYCLE, 22_000.0, 3_300, 24.0, R.drawable.img_road_2, emptyList(), 1_751_395_200, rideScore = RideScore(74, "B", 76, 72, 74)),
        RideHistoryEntry("r-5", "Weekend Car Meet II", VehicleType.CAR, 55_000.0, 6_600, 30.0, R.drawable.img_road_3, emptyList(), 1_750_790_400, rideScore = RideScore(60, "C", 55, 72, 53)),
        RideHistoryEntry("r-6", "Sudirman Morning", VehicleType.MOTORCYCLE, 31_000.0, 4_200, 26.5, R.drawable.img_road_1, emptyList(), 1_750_185_600, rideScore = RideScore(78, "B", 82, 70, 82)),
        RideHistoryEntry("r-7", "Short City Hop", VehicleType.MOTORCYCLE, 12_000.0, 1_800, 24.0, R.drawable.img_road_4, emptyList(), 1_749_580_800, rideScore = RideScore(58, "C", 50, 70, 54)),
        RideHistoryEntry("r-8", "Bandung Day Trip", VehicleType.CAR, 88_000.0, 9_000, 35.0, R.drawable.img_road_5, emptyList(), 1_748_976_000, rideScore = RideScore(88, "A", 90, 80, 94)),
        RideHistoryEntry("r-9", "Puncak Loop", VehicleType.MOTORCYCLE, 44_000.0, 5_400, 29.0, R.drawable.img_road_6, emptyList(), 1_748_371_200, rideScore = RideScore(72, "B", 75, 70, 71)),
        RideHistoryEntry("r-10", "Sunday Sudirman", VehicleType.MOTORCYCLE, 18_500.0, 2_700, 24.7, R.drawable.img_road_1, emptyList(), 1_747_766_400, rideScore = RideScore(62, "C", 60, 72, 54)),
        RideHistoryEntry("r-11", "Suburb Errand Run", VehicleType.CAR, 27_000.0, 3_600, 27.0, R.drawable.img_road_2, emptyList(), 1_747_161_600, rideScore = RideScore(70, "B", 72, 70, 68)),
        RideHistoryEntry("r-12", "Long South Ride", VehicleType.MOTORCYCLE, 63_000.0, 8_100, 28.0, R.drawable.img_road_3, emptyList(), 1_746_556_800, rideScore = RideScore(85, "A", 88, 74, 93)),
```

(`RideHistoryEntry`/`RideScore`/`VehicleType` are covered by the file's `import com.valid.motouring.data.model.*` — no new import. `photoResList` is `emptyList()`, `legs`/`segmentResult` use their defaults; `rideScore` is a named arg.)

- [ ] **Step 2: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(insights): grow seeded ride history for the charts

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: InsightsViewModel

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/insights/InsightsViewModel.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/insights/InsightsViewModelTest.kt`

**Interfaces:**
- Consumes: `InsightsCalculations` (Task 1), `RideRepository`.
- Produces: `data class InsightsState(totals, weekly, records, split, scoreTrend)`; `InsightsViewModel(rideRepository)` with `state: StateFlow<InsightsState>`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/ui/insights/InsightsViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.insights

import com.valid.motouring.data.repository.RideRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsViewModelTest {

    @Test
    fun `state aggregates the seeded ride history`() {
        val repo = RideRepository()
        val vm = InsightsViewModel(repo)
        val state = vm.state.value
        assertEquals(repo.observeHistory().value.size, state.totals.rideCount)
        assertTrue(state.totals.distanceKm > 0.0)
        assertTrue(state.weekly.isNotEmpty())
        assertTrue(state.scoreTrend.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.insights.InsightsViewModelTest"`
Expected: FAIL — `InsightsViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/insights/InsightsViewModel.kt`:

```kotlin
package com.valid.motouring.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.simulation.LifetimeTotals
import com.valid.motouring.simulation.PersonalRecords
import com.valid.motouring.simulation.VehicleSplit
import com.valid.motouring.simulation.WeekDistance
import com.valid.motouring.simulation.lifetimeTotals
import com.valid.motouring.simulation.personalRecords
import com.valid.motouring.simulation.rideScoreTrend
import com.valid.motouring.simulation.vehicleSplit
import com.valid.motouring.simulation.weeklyDistanceKm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InsightsState(
    val totals: LifetimeTotals,
    val weekly: List<WeekDistance>,
    val records: PersonalRecords,
    val split: VehicleSplit,
    val scoreTrend: List<Int>,
)

class InsightsViewModel(
    rideRepository: RideRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(build(rideRepository.observeHistory().value))
    val state: StateFlow<InsightsState> = _state.asStateFlow()

    private fun build(entries: List<com.valid.motouring.data.model.RideHistoryEntry>) = InsightsState(
        totals = lifetimeTotals(entries),
        weekly = weeklyDistanceKm(entries),
        records = personalRecords(entries),
        split = vehicleSplit(entries),
        scoreTrend = rideScoreTrend(entries),
    )

    companion object {
        fun factory(rideRepository: RideRepository) = viewModelFactory {
            initializer { InsightsViewModel(rideRepository) }
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.insights.InsightsViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/insights/InsightsViewModel.kt \
        app/src/test/java/com/valid/motouring/ui/insights/InsightsViewModelTest.kt
git commit -m "feat(insights): snapshot InsightsViewModel over ride history

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: The charts (Canvas)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/insights/InsightsCharts.kt`

**Interfaces:**
- Consumes: `WeekDistance`/`VehicleSplit` (Task 1), `MotouringColors`, `Muted`, `Charcoal600`.
- Produces: `@Composable WeeklyDistanceChart(weeks, modifier)`, `VehicleSplitBar(split, modifier)`, `ScoreTrendSparkline(scores, modifier)`.

UI/Canvas — verified by build + on-device.

- [ ] **Step 1: Create the charts file**

Create `app/src/main/java/com/valid/motouring/ui/insights/InsightsCharts.kt`:

```kotlin
package com.valid.motouring.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.simulation.VehicleSplit
import com.valid.motouring.simulation.WeekDistance
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted
import kotlin.math.roundToInt

@Composable
fun WeeklyDistanceChart(weeks: List<WeekDistance>, modifier: Modifier = Modifier) {
    if (weeks.isEmpty()) {
        Text("No rides yet", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    val maxKm = weeks.maxOf { it.distanceKm }.coerceAtLeast(1.0)
    Column(modifier.fillMaxWidth()) {
        Text("Peak ${maxKm.roundToInt()} km / week", color = Muted, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 6.dp)) {
            val n = weeks.size
            val gap = 6.dp.toPx()
            val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
            val topPad = 8.dp.toPx()
            drawLine(Charcoal600, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            weeks.forEachIndexed { i, w ->
                val h = (w.distanceKm / maxKm * (size.height - topPad)).toFloat()
                val x = i * (barW + gap)
                if (h > 0f) {
                    drawRoundRect(
                        color = MotouringColors.goal,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleSplitBar(split: VehicleSplit, modifier: Modifier = Modifier) {
    val total = (split.motoKm + split.carKm).coerceAtLeast(0.001)
    val motoPct = (split.motoKm / total * 100).roundToInt()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
            if (split.motoKm > 0) Box(Modifier.weight(split.motoKm.toFloat()).fillMaxHeight().background(MotouringColors.rider))
            if (split.motoKm > 0 && split.carKm > 0) Spacer(Modifier.width(2.dp))
            if (split.carKm > 0) Box(Modifier.weight(split.carKm.toFloat()).fillMaxHeight().background(MotouringColors.poiRest))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Moto ${split.motoKm.roundToInt()} km · ${motoPct}%", color = MotouringColors.rider, style = MaterialTheme.typography.labelMedium)
            Text("Car ${split.carKm.roundToInt()} km · ${100 - motoPct}%", color = MotouringColors.poiRest, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ScoreTrendSparkline(scores: List<Int>, modifier: Modifier = Modifier) {
    if (scores.size < 2) {
        Text("Not enough rides yet", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }
    val minS = scores.min()
    val range = (scores.max() - minS).coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Text("Latest ${scores.last()}", color = Muted, style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(64.dp).padding(top = 6.dp)) {
            val n = scores.size
            val dx = if (n > 1) size.width / (n - 1) else 0f
            val pad = 6.dp.toPx()
            fun pt(i: Int): Offset {
                val v = (scores[i] - minS).toFloat() / range
                return Offset(i * dx, size.height - pad - v * (size.height - pad * 2))
            }
            for (i in 0 until n - 1) {
                drawLine(MotouringColors.goal, pt(i), pt(i + 1), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            drawCircle(MotouringColors.goal, radius = 5.dp.toPx(), center = pt(n - 1))
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/insights/InsightsCharts.kt
git commit -m "feat(insights): Canvas weekly-distance, vehicle-split, score-trend charts

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: InsightsScreen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/insights/InsightsScreen.kt`

**Interfaces:**
- Consumes: `InsightsViewModel` (Task 3), the charts (Task 4), `StatBlock`, `SectionHeader`.
- Produces: `@Composable InsightsScreen(viewModel: InsightsViewModel)`.

UI — verified by build + on-device.

- [ ] **Step 1: Create the screen**

Create `app/src/main/java/com/valid/motouring/ui/insights/InsightsScreen.kt`:

```kotlin
package com.valid.motouring.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Distance", value = "${state.totals.distanceKm.roundToInt()} km")
            StatBlock(label = "Rides", value = state.totals.rideCount.toString())
            StatBlock(label = "Hours", value = "${state.totals.movingHours.roundToInt()}")
        }

        SectionHeader(title = "Weekly Distance")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { WeeklyDistanceChart(state.weekly) }
        }

        SectionHeader(title = "Personal Records")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBlock(label = "Longest", value = "${state.records.longestRideKm.roundToInt()} km")
            StatBlock(label = "Fastest Avg", value = "${state.records.fastestAvgKmh.roundToInt()} km/h")
            StatBlock(label = "Best Score", value = state.records.bestScore.toString())
        }

        SectionHeader(title = "By Vehicle")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { VehicleSplitBar(state.split) }
        }

        SectionHeader(title = "Ride Score Trend")
        MotouringCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) { ScoreTrendSparkline(state.scoreTrend) }
        }
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
git add app/src/main/java/com/valid/motouring/ui/insights/InsightsScreen.kt
git commit -m "feat(insights): insights dashboard screen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Nav route + Profile "Insights" entry

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `InsightsScreen`/`InsightsViewModel` (Tasks 3/5), `appContainer.rideRepository`.
- Produces: `Destinations.INSIGHTS`; `ProfileScreen(..., onInsightsClick)`.

UI/wiring — verified by build + on-device.

- [ ] **Step 1: Add the destination**

In `Destinations.kt`, add:

```kotlin
    const val INSIGHTS = "insights"
```

- [ ] **Step 2: Add the nav composable**

In `MotouringNavHost.kt`, add a composable (near `BADGES`), following the simple-route pattern:

```kotlin
        composable(Destinations.INSIGHTS) {
            val viewModel: InsightsViewModel = viewModel(
                factory = InsightsViewModel.factory(appContainer.rideRepository),
            )
            InsightsScreen(viewModel = viewModel)
        }
```

Add the imports:

```kotlin
import com.valid.motouring.ui.insights.InsightsScreen
import com.valid.motouring.ui.insights.InsightsViewModel
```

(`viewModel`/`composable` are already imported.)

- [ ] **Step 3: Add the "Insights" entry to ProfileScreen**

In `ProfileScreen.kt`, add the param (after `onNotificationsClick`):

```kotlin
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onInsightsClick: () -> Unit,
) {
```

Add an "Insights" `TextButton` in the action list (next to the other `TextButton`s near the bottom — before "Ride Buddies"):

```kotlin
            TextButton(onClick = onInsightsClick) { Text("Insights") }
            TextButton(onClick = onFriendsClick) { Text("Ride Buddies") }
```

- [ ] **Step 4: Wire MainScaffold**

In `MainScaffold.kt`, the Profile-tab `ProfileScreen(...)` call — add the callback (after `onNotificationsClick`):

```kotlin
                        onNotificationsClick = { outerNavController.navigate(Destinations.NOTIFICATIONS) },
                        onInsightsClick = { outerNavController.navigate(Destinations.INSIGHTS) },
```

- [ ] **Step 5: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt \
        app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat(insights): Insights nav route + Profile entry

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 6)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (insights calc, insights VM, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): Profile → "Insights" opens the dashboard — lifetime totals (distance/rides/hours), a weekly-distance bar chart with ~9 orange bars + a "peak" label, personal-records tiles (longest/fastest/best score), a moto-vs-car split bar with labels, and a ride-score sparkline with the latest point emphasized.
