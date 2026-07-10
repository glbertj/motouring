# Motouring — Ride Modes — Design

## Purpose

This is the first of five specs decomposing the "niche rider-community features" brainstorm parked in the [Analog Dash design system spec's appendix](2026-07-09-motouring-analog-dash-design-system.md#appendix-feature-seed-parked). It designs and builds the **Goal vs. Endless ride mode** concept end to end: a ride session starts with a goal (distance or destination), reaching it triggers a celebration and a choice (new goal, which becomes a waypoint/stop, or Endless), and drifting off-route silently falls into Endless rather than failing the route. Endless is always the default fallback.

Like the rest of the app, this is a **UI/UX mockup only** — no real GPS, no real routing, no backend. Everything here is locally simulated.

## Relationship to the other 4 specs

The full niche-features brainstorm decomposed into five independent specs, each with its own spec → plan → implementation cycle:

1. **Ride Modes** (this spec) — Goal/Endless, multi-stop trips, single-rider session state
2. **Safety/Emergency** — SOS/breakdown broadcast, headcount-at-stops, road captain/sweep rider roles, crash/no-movement detection
3. **Trip Planning** — multi-day itineraries, fuel-range planning, curated road library, route/briefing cards, weather/hazard overlay
4. **Gear/Maintenance** — per-vehicle service log, helmet age tracking, pre-ride checklist, consumables wear
5. **Group Culture** — kickstands-up countdown, hand-signal reference, mentorship pairing, richer post-ride debrief, chapter/crew structures

Specs 2 and 5 will reference concepts this spec does not define (road captain/sweep roles, group pace/regroup mechanics) — those are explicitly out of scope here; see below.

## Scope

In scope:
- Goal/Endless ride mode mechanic for a single rider's session (identical whether that rider is riding solo or as part of a group)
- Multi-stop trip accumulation: an ordered list of completed legs plus one active leg
- The celebration + choice UX on reaching a goal
- Silent fallback to Endless on simulated drift
- A new **Ride Session** screen (placeholder map, not real Mapbox)
- Extending **Start Ride** (initial goal selection) and **Ride Summary** (leg-by-leg breakdown) — both already exist
- Compose `@Preview` coverage for new/changed plain-state composables

Out of scope (deferred to other specs or explicitly not built):
- Group pace/regroup mechanics, "ride your own ride" nudges, road captain/sweep roles — deferred to Safety and/or Group Culture specs
- Real Mapbox integration / real GPS / real routing — the placeholder route visual is designed to be swapped later without touching this spec's ViewModel or state
- Trip-planning concepts (multi-day itineraries, curated route library, fuel-range) — the new-goal recommendation list here is a small self-contained fake preset list, not a dependency on the Trip Planning spec
- Any weather/hazard overlay

## Data Model

`RideSession`, `RideSimulator`, `RideParticipantState`, and `RideHistoryEntry` (`data/model/RideSession.kt`, `simulation/RideSimulator.kt`, `data/model/RideHistoryEntry.kt`) already exist — the route-following simulation logic was already built ahead of the screen. This spec extends those rather than introducing a parallel model, and follows their existing conventions (meters, not km; `Long`/`Double` counters, no `java.time` types).

```kotlin
enum class RideMode { GOAL, ENDLESS }

enum class GoalType { DISTANCE, DESTINATION }

data class RideGoal(
    val type: GoalType,
    val label: String,                  // "25 km" or "Warung Kopi Susu"
    val targetDistanceMeters: Double,   // absolute cumulative RideSession.distanceMeters at which this goal completes
)

enum class LegEndReason { GOAL_REACHED, DRIFTED, RIDE_ENDED }

data class Leg(
    val goal: RideGoal?,                // null only for a trailing Endless leg
    val distanceMeters: Double,         // this leg's own distance, not cumulative
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val endReason: LegEndReason,
)

// Additions to the existing RideSession data class:
data class RideSession(
    val id: String,
    val vehicleType: VehicleType,
    val route: List<GeoPoint>,
    val participants: List<RideParticipantState>,
    val distanceMeters: Double,               // unchanged: running TOTAL across the whole trip
    val speedKmh: Double,
    val elapsedSeconds: Long,                 // unchanged: running TOTAL across the whole trip
    val status: RideSessionStatus,
    val mode: RideMode = RideMode.GOAL,       // NEW
    val activeGoal: RideGoal? = null,         // NEW — null while mode == ENDLESS
    val completedLegs: List<Leg> = emptyList(), // NEW
)

sealed interface RideSessionEvent {
    data class GoalReached(val leg: Leg) : RideSessionEvent  // triggers celebration + choice card
    object DriftedToEndless : RideSessionEvent                // triggers a quiet toast, no celebration
}
```

`distanceMeters`/`elapsedSeconds` stay cumulative trip-wide totals exactly as `RideSimulator.advance()` already computes them today — a leg's own stats are derived as the delta since the last leg boundary (`legStartDistance = completedLegs.sumOf { it.distanceMeters }`), so no change to the existing accumulation logic is needed.

**State machine rule:** `RideSimulator.advance()` (the existing companion function) gains one check per tick: if `mode == GOAL && activeGoal != null && newDistance >= activeGoal.targetDistanceMeters`, it closes the current leg (`endReason = GOAL_REACHED`, appended to `completedLegs`), clears `activeGoal`, and sets `mode = ENDLESS`. The trip-wide `distanceMeters`/`elapsedSeconds` ticker itself never stops or resets — this is what makes "no dead stop" true at the data level, not just visually.

- Picking a new goal from the choice card sets `activeGoal` back to a non-null value and `mode = GOAL` — it does not restart tracking or reset any counters.
- The manual "simulate off-route" action calls a new `RideSimulator.simulateDrift()` method that force-closes the current `GOAL` leg immediately (`endReason = DRIFTED`) — no celebration, since nothing was completed.
- Ending the ride force-closes whatever leg is active with `endReason = RIDE_ENDED`.

`RideSimulator` gains a second stream, `val events: SharedFlow<RideSessionEvent>`, alongside its existing `session: StateFlow<RideSession>`. Each tick (or `simulateDrift()` call) that closes a leg emits the corresponding one-shot event immediately after updating `_session`. Events are one-shot rather than folded into `RideSession` state deliberately: it avoids the celebration overlay/choice sheet replaying on rotation or recomposition, since `RideSession` itself is continuous and doesn't encode "did we already show this."

## UX Flow

**Start Ride** (existing `StartRideScreen`, extended): after the existing solo/group + vehicle selection, the rider picks an initial `RideGoal` from a preset picker — a small self-contained list of distance presets (+10/25/50 km) plus 2-3 fake named nearby destinations. `onStartRide`'s signature grows from `(VehicleType, Boolean) -> Unit` to `(VehicleType, Boolean, RideGoal) -> Unit`. This is the only change to this screen.

**Ride Session** (new screen, placeholder map):
- Top: live stat HUD (distance/speed/duration, rendered in `IBM Plex Mono` per the Analog Dash design tokens) plus the current goal label (e.g. "→ 25 km" or "→ Warung Kopi Susu")
- Center: a static styled polyline illustration (using `Charcoal`/`AccentPrimary` tokens) rendering `RideSession.route`, with the animated dot marker positioned by `RideSimulator`'s existing `pointAlongRoute` logic (unchanged). Built so the illustration can be swapped for a real Mapbox `MapView` later without any change to `RideSimulator` or `RideSession`.
- Group rider list + voice-call bar: unchanged from the original spec.
- Persistent bottom chip **"Set a goal"** — visible only while `mode == ENDLESS`; opens the goal picker sheet (same preset list as Start Ride) and turns the pick into the current leg's goal.
- Demo/debug affordance: a **"Simulate off-route"** action (overflow menu), enabled only during a `GOAL` leg — manually triggers `DriftedToEndless` for controllable, deterministic demoing rather than relying on a random timer or hidden route-choice logic.

**Goal-reached moment:** `GoalReached` → a celebratory overlay (spring-based burst animation per the Analog Dash motion system, showing that leg's recap: distance/time) plays for ~2-3s on top of the still-live HUD (tracking is never paused) → settles into a non-blocking bottom sheet: "Nice! Pick a new goal, or keep riding" with preset options plus an explicit "Go Endless" button. No response within a few seconds auto-dismisses into Endless, consistent with Endless being the default fallback.

**Drifted-to-Endless moment:** `DriftedToEndless` → a brief, quiet toast/snackbar ("Off route — tracking continues"). No sheet, no celebration — nothing was accomplished. The rider can still tap "Set a goal" afterward.

**Ending the ride:** the existing "End Ride" action calls `RideSimulator.stop()` (existing), which now also force-closes whatever leg is active (`endReason = RIDE_ENDED`, appended to `completedLegs`) before the final `RideSession` is mapped into a `RideHistoryEntry` for Ride Summary.

## Ride Summary Integration

`RideHistoryEntry` (existing) gains one new field: `legs: List<Leg> = emptyList()`. `RideSummaryScreen` (existing) keeps its current aggregate header (the existing `StatBlock` row for distance/duration/avg speed, route preview, `BadgeChip` row) and adds a new **"Stops"** section below it, using the existing `SectionHeader` component (as the Badges section already does): one row per entry in `entry.legs`, in order, showing that leg's goal label, distance, duration, and avg speed. A tail Endless leg with no goal (`goal == null`) is labeled "Free ride" if it has nonzero distance, and omitted if negligible/zero. Leg rows use `MotouringCard` with `StaggeredEntrance`, consistent with how other list content is staggered elsewhere in the design system.

## Design System Integration

This spec does not touch `ui/theme/*` or `ui/components/*` (owned by the Analog Dash track) — it consumes existing tokens:

- `InstrumentRing` — shown during an active `GOAL` leg, open center slot displaying the mono distance-remaining value as progress toward the goal
- `MotouringCard` — choice sheet and Ride Summary leg rows
- `StaggeredEntrance` — Ride Summary leg list
- `IBM Plex Mono` — all HUD numeral readouts
- Celebration overlay motion uses the spec's spring/overshoot language, not a custom easing curve

## Edge Cases

- **Ending mid-celebration/choice-sheet:** treated as an implicit "keep riding" cancel — the sheet dismisses and the ride ends normally with that leg recorded as-is.
- **Solo vs. group:** identical from the local rider's perspective. `mode`/`activeGoal`/`completedLegs` live on the local rider's own `RideSession`; other participants' goals/legs are not modeled in this spec (group pace/regroup mechanics are explicitly deferred — see Scope). Group members still appear in `participants: List<RideParticipantState>` and the rider list exactly as today, just not goal-aware.
- **Zero-distance leg:** if a rider picks a new goal immediately after reaching one without moving, it's still recorded as a valid leg with near-zero stats and shown as-is in Summary — no special-casing.

## Testing

Consistent with the project's existing approach:
- Compose `@Preview` functions for new/changed composables that take plain state — Ride Session HUD, celebration overlay, choice sheet, Ride Summary leg rows
- Manual smoke test of a full session: start with a goal → reach it → pick a new goal → go Endless → simulate drift → end ride → verify Ride Summary shows the correct legs
- No unit test suite — this is pure UI/state simulation with no business logic warranting one, matching the rest of the mockup
