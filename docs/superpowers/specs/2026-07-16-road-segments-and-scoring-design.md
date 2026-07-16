# Motouring — Road Segments & Scoring (Spec 5)

**Date:** 2026-07-16
**Status:** Design — awaiting review
**Track:** Niche rider-community features — the **last** of the four themes (follows Spec 4 Gear & Maintenance).

## 1. Problem & Goals

The app tracks rides but does nothing competitive or expressive with them. This final theme adds a
Strava-flavored **scored riding** layer on top of the existing ride-tracking + badges + leaderboard
substrate:

1. **Twisty-road segments + time leaderboards** — a set of curated road "segments" (a named twisty
   stretch), each with a ranked leaderboard of times and your best. Browse them and open a segment to
   see the board.
2. **Per-ride ride score** — a computed **0–100 score + letter grade** with three simulated sub-scores
   (**Lean / Smoothness / Pace**), shown on the post-ride summary.
3. **A light link between them** — finishing a ride shows a "you set a time on {segment}" result on the
   summary, so scoring and segments feel connected rather than two separate features.

**Non-goals (this stays a UI/UX mockup):** no real GPS matching of rides to segments, no real
telemetry/lean sensors (all scores are **simulated** from the existing fake ride telemetry — max/avg
speed, elevation, distance), no crowd/global leaderboards or network sync (leaderboards are seeded
in-memory), and **no scenic-route discovery** (deferred — it's a separate browse surface). This is the
last niche-feature theme; after it the backlog is complete.

## 2. Design Decisions Already Locked (via brainstorm)

- **Scope:** segments + time leaderboards + the per-ride ride score. Scenic-route discovery deferred.
- **Ride score shape:** a headline **0–100 + letter grade**, plus three sub-scores **Lean**,
  **Smoothness**, **Pace** (small bars). All computed by a **pure, TDD'd** function from the ride's
  telemetry.
- **Segments live off the Rides tab.** The bottom bar is full (Home · Nearby · FAB · Rides · Profile), so
  `RidesHistoryScreen` gains a **"Segments"** entry that opens the segments list; the list and detail are
  outer-nav routes (like the other detail screens), reached via callbacks threaded through `MainScaffold`.
- **Ride↔segment link is a light, simulated callout.** The ride summary shows one seeded segment result
  ("Sudirman Sprint · 4:32 · #3") — a simulated time inserted into that segment's leaderboard to compute
  a rank. No real route matching.
- **Reuse the leaderboard idiom.** Segment times mirror the existing `LeaderboardEntry` shape; the current
  user (`u-me`) always appears so "your best" + your row highlight + rank work.

## 3. Data Model

New file `data/model/RoadSegment.kt`:

```
data class SegmentTime(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val timeSeconds: Int,   // lower is better
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
    val overall: Int,        // 0..100
    val grade: String,       // A / B / C / D derived from overall
    val lean: Int,           // 0..100
    val smoothness: Int,     // 0..100
    val pace: Int,           // 0..100
)

data class SegmentResult(
    val segmentName: String,
    val timeSeconds: Int,
    val rank: Int,
)
```

- `RideHistoryEntry` gains `rideScore: RideScore? = null` and `segmentResult: SegmentResult? = null`
  (both defaulted — existing construction sites keep compiling). Populated when a ride is saved; seeded
  on the sample history entries. Carrying both on the entry keeps `RideSummaryScreen` a pure,
  param-driven display (no summary ViewModel / repo lookup needed).

## 4. Pure Logic — `simulation/ScoringCalculations.kt`

All headless-testable:

- `fun rideScore(maxSpeedKmh, avgSpeedKmh, elevationGainMeters, distanceMeters): RideScore` — computes the
  three sub-scores from **simulated proxies**, each clamped 0..100, then `overall` = their mean and
  `grade` banded (A ≥ 85, B ≥ 70, C ≥ 55, else D):
  - **Lean/cornering** ← the speed spread (`maxSpeed − avgSpeed`) and elevation-per-km (twisty/hilly →
    more lean).
  - **Smoothness** ← inverse of the relative speed spread (avg close to max = smoother).
  - **Pace** ← `avgSpeed` against a reference band.
  (Exact coefficients finalized in the plan; the point is a deterministic, monotonic mapping.)
- `fun sortedByTime(times: List<SegmentTime>): List<SegmentTime>` — ascending (fastest first).
- `fun rankOf(timeSeconds: Int, times: List<SegmentTime>): Int` — 1-based rank a time would take.

Keeping scoring + ranking pure means the summary, the segment leaderboard, and the seeded
segment-result callout all share one tested source of truth.

## 5. Repository

New `data/repository/SegmentRepository.kt`: `MutableStateFlow<List<RoadSegment>>` seeded from
`FakeDataProvider`. `observeSegments()`, `segments(): List<RoadSegment>`, `segment(id): RoadSegment?`.
Registered in `AppContainer`.

## 6. Scoring integration (ride summary)

- `RideSession.toHistoryEntry(...)` (`simulation/RideSessionCalculations.kt`) computes the `RideScore` from
  the finished session (`maxSpeedKmh`, derived `avgSpeedKmh`, `elevationGainMeters`, `distanceMeters`) and
  sets it on the entry — so a freshly-completed ride carries its score with no extra wiring (the pure
  `rideScore` needs no repository).
- The **segment result** is computed in `RideSessionViewModel.endRide()` (which injects
  `SegmentRepository`): it picks a seeded segment, derives a plausible time from the ride, computes the
  rank via `rankOf`, and attaches a `SegmentResult` to the saved entry. (Kept out of the pure
  `toHistoryEntry` because it needs the segment leaderboard.)
- `RideSummaryScreen` gains a **Ride Score** block (big overall number + grade + three labeled sub-score
  bars: Lean / Smoothness / Pace, reusing the progress-bar idiom) and a **Segment result** callout
  ("{segment name} · {your time} · #{rank}") — both read straight off the entry, so the summary stays a
  pure param-driven screen.
- `FakeDataProvider` seeds a `rideScore` **and** a `segmentResult` on the three sample history entries so
  history/summary previews render out of the box.

## 7. Segments UI

- **`ui/segments/SegmentsScreen.kt` + `SegmentsViewModel.kt`** (new) — a browse list over
  `SegmentRepository`. Each row: name, region, length, a twistiness chip, and your-best vs the leader's
  time (from the seeded leaderboard, `u-me` = you). Tapping a row opens the detail.
- **`ui/segments/SegmentDetailScreen.kt` + `SegmentDetailViewModel.kt`** (new) — a header (route preview,
  length, twistiness) + the ranked leaderboard (`sortedByTime`): position, avatar/initials, name, time,
  with **your row highlighted** and your rank called out.
- **`RidesHistoryScreen`** gains a **"Segments"** entry (a button/row at the top) that invokes an
  `onSegmentsClick` callback.
- **Nav:** `Destinations` gains `SEGMENTS` and `SEGMENT_DETAIL_PATTERN = "segment_detail/{segmentId}"` +
  `segmentDetail(id)`. `MotouringNavHost` adds both composables (detail uses the existing id-arg pattern).
  `MainScaffold` wires `RidesHistoryScreen(onSegmentsClick = { outerNav.navigate(SEGMENTS) })`, and
  `SegmentsScreen`'s `onSegmentClick` → `segmentDetail(id)`.

## 8. File-by-File Summary

| File | Change |
| --- | --- |
| `data/model/RoadSegment.kt` (new) | `SegmentTime`, `Twistiness`, `RoadSegment`, `RideScore`, `SegmentResult` |
| `data/model/RideHistoryEntry.kt` | `+ rideScore: RideScore?`, `+ segmentResult: SegmentResult?` |
| `simulation/ScoringCalculations.kt` (new) | pure `rideScore` / `sortedByTime` / `rankOf` |
| `simulation/RideSessionCalculations.kt` | `toHistoryEntry` computes + attaches `rideScore` |
| `ui/rides/RideSessionViewModel.kt` | `endRide()` attaches a `SegmentResult` (injects `SegmentRepository`) |
| `data/repository/SegmentRepository.kt` (new) | seeded segments, `observeSegments`/`segment` |
| `di/AppContainer.kt` | register `segmentRepository` |
| `data/fake/FakeDataProvider.kt` | seed segments + leaderboards + `rideScore`/`segmentResult` on sample history |
| `ui/rides/RideSummaryScreen.kt` | ride-score block + segment-result callout |
| `ui/segments/SegmentsScreen.kt` + `SegmentsViewModel.kt` (new) | segments browse |
| `ui/segments/SegmentDetailScreen.kt` + `SegmentDetailViewModel.kt` (new) | leaderboard detail |
| `ui/rides/RidesHistoryScreen.kt` | "Segments" entry |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` + `ui/main/MainScaffold.kt` | routes + wiring |

## 9. Testing & Verification

- **TDD the pure logic** (headless): `rideScore` — sub-scores clamp 0..100, monotonic (a spirited ride
  with high max-vs-avg spread + elevation scores higher Lean than a flat cruise), grade banding at the
  boundaries; `sortedByTime` ascending; `rankOf` (fastest → 1, slowest+1 → last, ties). `SegmentRepository`
  `segment(id)` returns the right one / null. Keep all existing tests green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — the segments list,
  the leaderboard, and the ride-score block can't be seen on the headless VM.
- Build stays green headless: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 10. Risks / Open Questions

- **Ride↔segment matching is faked.** The summary's segment result is a seeded/simulated attempt, not real
  GPS matching. Accepted — consistent with the mockup's no-telemetry stance.
- **Score formula is illustrative.** The proxy coefficients are tuned for plausible-looking numbers, not
  real riding dynamics; on-device tuning may adjust them.
- **`RideScore` on `RideHistoryEntry`.** Adding a nullable field is additive; seeded entries get a score,
  new rides compute one — no migration concerns (in-memory mockup).

## 11. Scope Boundary & Backlog

**In this spec:** twisty-road segments + time leaderboards (browse + detail), a per-ride ride score
(headline + Lean/Smoothness/Pace) on the summary, and a light seeded segment-result callout.

**Deferred:** scenic-route discovery (a curated scenic-routes browse surface) — the one remaining
sub-idea, can be a small standalone spec later if wanted. **With this theme, the original four-theme
niche-features backlog is complete.**
