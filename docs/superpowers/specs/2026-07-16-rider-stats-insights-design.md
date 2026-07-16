# Motouring — Rider Stats & Insights (Spec 7)

**Date:** 2026-07-16
**Status:** Design — awaiting review
**Track:** New feature (post-backlog) — a data-viz dashboard over ride history. Requested directly by the user.

## 1. Problem & Goals

The app tracks rides but surfaces only three flat numbers on Profile (rides, distance, badges). This
spec adds a **Rider Stats & Insights** dashboard that turns the ride history into a readable picture of a
rider's activity — lifetime totals, a weekly-distance chart, personal records, a moto-vs-car split, and a
recent ride-score trend. Reached from Profile.

**Five sections:**
1. **Lifetime totals** — distance · rides · moving hours (stat tiles).
2. **Weekly distance** — a bar chart of km per week (the hero).
3. **Personal records** — longest ride · fastest avg speed · best ride-score (stat tiles).
4. **Vehicle split** — moto vs car distance (a labeled segmented bar).
5. **Ride-score trend** — a sparkline of recent ride scores.

**Non-goals (this stays a UI/UX mockup):** no backend/analytics service — everything is computed
in-memory from the seeded `RideHistoryEntry` list; no charting library (charts are drawn with Compose
`Canvas` — no new dependencies); no date-range filters or drill-down (a small history doesn't need them).

## 2. Design Decisions Already Locked (via brainstorm + data-viz method)

- **Entry point:** Profile gains an **"Insights"** action (alongside Ride Buddies / Notifications /
  Settings) that opens a new `InsightsScreen` on the outer nav graph.
- **Forms by the data's job (data-viz procedure):** lifetime totals and personal records are **stat
  tiles** (headline numbers — *not* charts, reusing `StatBlock`); weekly distance is a single-series
  **bar chart**; the ride-score trend is a single-series **sparkline (line)**; the vehicle split is the one
  **categorical** piece (2 hues), shown as a **segmented bar with direct labels**.
- **Color (data-viz color-by-job):** the single-series charts (weekly bars, sparkline) use the **orange
  accent alone** (`MotouringColors.goal`) — one accent, no legend needed (the title names the series).
  The categorical vehicle split uses two distinct brand tokens — **moto = `rider` (blue)**, **car =
  `poiRest` (amber)** — which the palette validator scored **CVD-excellent** (ΔE 104, far above the 12
  threshold) with passing chroma/contrast; both segments are **direct-labeled** (secondary encoding), so
  identity is never color-alone. Grid/axis/labels wear the muted ink tokens (`Muted`/`MutedDim`), never a
  series color. No new hexes.
- **Marks (data-viz mark specs):** thin bars with slightly-rounded tops anchored to a faint baseline; a
  2px sparkline with an emphasized endpoint dot; recessive gridline(s); **selective** direct labels (label
  the tallest week / the latest score, not every mark). Numbers use tabular figures.
- **Charts are Compose `Canvas`** (the app already draws with Canvas in `RidePlaceholderRoute`/
  `InstrumentRing`). No hover layer (that's a web/SVG paradigm) — this is a touch mockup; key marks are
  direct-labeled and the on-device pass is the visual gate.
- **Seed more history:** the dashboard needs data, so `FakeDataProvider.rideHistory` grows from 3 to ~12
  entries spanning ~8–10 recent weeks, with varied distance / vehicle / score.

## 3. Data Model

No new persisted model. Small UI-result types live with the pure calc (§4): `LifetimeTotals`,
`WeekDistance`, `PersonalRecords`, `VehicleSplit`, plus `List<Int>` for the score trend. Existing
`RideHistoryEntry` already carries everything needed (`distanceMeters`, `durationSeconds`, `avgSpeedKmh`,
`vehicleType`, `completedAtEpochSeconds`, `rideScore`).

## 4. Pure Logic — `simulation/InsightsCalculations.kt`

All headless-testable, pure over `List<RideHistoryEntry>`:

- `data class LifetimeTotals(distanceKm: Double, rideCount: Int, movingHours: Double)` +
  `fun lifetimeTotals(entries): LifetimeTotals`.
- `data class WeekDistance(weekIndex: Int, distanceKm: Double)` +
  `fun weeklyDistanceKm(entries): List<WeekDistance>` — bucket by ISO-week epoch
  (`completedAtEpochSeconds / 604_800`), **fill the min→max week span with zeros** so the bar chart is
  contiguous, sorted ascending. Deterministic from the data (no wall-clock).
- `data class PersonalRecords(longestRideKm: Double, fastestAvgKmh: Double, bestScore: Int)` +
  `fun personalRecords(entries): PersonalRecords` — max distance, max avg speed, max `rideScore.overall`
  (entries without a score are ignored for best-score; 0 if none).
- `data class VehicleSplit(motoKm: Double, carKm: Double)` + `fun vehicleSplit(entries): VehicleSplit` —
  distance summed by `VehicleType`.
- `fun rideScoreTrend(entries): List<Int>` — `rideScore.overall` for entries that have one, ordered by
  `completedAtEpochSeconds` ascending.

## 5. ViewModel

`ui/insights/InsightsViewModel.kt` (new) — takes `RideRepository`; exposes a snapshot
`state: StateFlow<InsightsState>` (or the individual computed values) built from
`rideRepository.observeHistory()` via the pure functions. Snapshot pattern (computed in init from the
current history) so it's directly unit-testable, mirroring the Segments/Maintenance VMs. A factory wires
`appContainer.rideRepository`.

## 6. UI — `ui/insights/`

- **`InsightsScreen.kt`** — a scrolling column: a lifetime-totals `StatBlock` row; the **`WeeklyDistanceChart`**;
  a personal-records `StatBlock` row; the **`VehicleSplitBar`**; and the **`ScoreTrendSparkline`** — each
  under a `SectionHeader`.
- **`WeeklyDistanceChart.kt`** (Canvas) — orange bars (`MotouringColors.goal`), rounded tops, a faint
  baseline, the tallest bar's value direct-labeled, sparse week ticks. Handles the empty case (no rides →
  a muted "No rides yet" line).
- **`VehicleSplitBar.kt`** (Canvas or Row of weighted boxes) — a horizontal segmented bar, moto = `rider`,
  car = `poiRest`, a 2px gap between segments, each segment labeled "{type} · {km} km ({pct}%)".
- **`ScoreTrendSparkline.kt`** (Canvas) — a 2px orange polyline over the recent scores with an emphasized
  endpoint dot; the latest score direct-labeled. Handles < 2 points gracefully (single dot / empty note).

These are Compose UI (Canvas), verified on-device.

## 7. Navigation

- `Destinations`: add `INSIGHTS = "insights"`.
- `MotouringNavHost`: add the `INSIGHTS` composable (builds `InsightsViewModel` via factory from
  `appContainer.rideRepository`).
- `ProfileScreen`: add an `onInsightsClick: () -> Unit` param + an "Insights" `TextButton` in the action
  list. `MainScaffold` wires `onInsightsClick = { outerNav.navigate(INSIGHTS) }`.

## 8. File-by-File Summary

| File | Change |
| --- | --- |
| `simulation/InsightsCalculations.kt` (new) | pure `lifetimeTotals` / `weeklyDistanceKm` / `personalRecords` / `vehicleSplit` / `rideScoreTrend` + result types |
| `data/fake/FakeDataProvider.kt` (modify) | grow `rideHistory` to ~12 entries across ~8–10 weeks |
| `ui/insights/InsightsViewModel.kt` (new) | snapshot VM over `RideRepository` |
| `ui/insights/InsightsScreen.kt` (new) | dashboard layout |
| `ui/insights/WeeklyDistanceChart.kt` (new) | Canvas bar chart |
| `ui/insights/VehicleSplitBar.kt` (new) | segmented split bar |
| `ui/insights/ScoreTrendSparkline.kt` (new) | Canvas sparkline |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` | `INSIGHTS` route |
| `ui/profile/ProfileScreen.kt` + `ui/main/MainScaffold.kt` | "Insights" entry + wiring |

## 9. Testing & Verification

- **TDD the pure logic** (headless): `lifetimeTotals` (sum + hours); `weeklyDistanceKm` (correct buckets,
  zero-filled span, sorted); `personalRecords` (max distance/avg/score, score-less entries ignored);
  `vehicleSplit` (per-type sums); `rideScoreTrend` (ordered, score-less skipped). Keep all existing tests
  green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — the charts (Canvas)
  can't be seen on the headless VM.
- Build stays green headless: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 10. Risks / Open Questions

- **Sparse data.** Charts need enough history to look real; the seed grows to ~12 rides. Weekly buckets
  are zero-filled across the span so the bar chart is contiguous.
- **Canvas charts aren't unit-tested** (they're `Canvas` interop); the pure aggregations are the tested
  surface, and the charts are verified on-device.
- **Vehicle-split lightness.** The moto/car pair failed only the validator's lightness band (both are
  light brand tokens); accepted because CVD separation is excellent and both segments are direct-labeled
  (secondary encoding) — and they're the app's own accent tokens, so no new hexes are introduced.

## 11. Scope Boundary

**In this spec:** a pure insights aggregation layer, a snapshot ViewModel, an Insights screen with a
totals row, a weekly-distance bar chart, a records row, a vehicle-split bar, and a score-trend sparkline,
reached from Profile.

**Out:** date-range filters, drill-down, per-segment/per-route analytics, export, real analytics backend.
