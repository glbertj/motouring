# Motouring — Group Ride Mechanics (Spec 2)

**Date:** 2026-07-11
**Status:** Design — awaiting review
**Track:** Niche rider-community features (first of four; see backlog). Builds on Spec 1 (Map & Visual Overhaul).

## 1. Problem & Goals

The app has a group-ride substrate — `RideSession` carries a participant list, the simulator
rotates a fake voice speaker, and the in-ride map draws a marker per rider — but the group is
inert. Buddies are frozen at the route's start point (only self moves), everyone is
indistinguishable, and there's no way to express the social mechanics that make riding *together*
feel real: who's leading, who's tailing, who's fallen behind, and "we need to stop."

This spec makes the group ride feel alive by adding four synergistic mechanics on top of that
substrate:

1. **Rider roles** — Lead / Sweep / Rider, auto-assigned at ride start and reassignable in-ride.
2. **Pack / formation view** — a genuinely moving pack rendered as role-colored markers on the map
   plus an ordered formation list (order + live gaps) in the dashboard.
3. **Regroup ping** — the group is alerted when a rider falls behind, and any rider can broadcast
   "regroup — wait for me," which visibly tightens the pack.
4. **Fuel call** — a rider broadcasts "I need fuel"; the group is alerted and rallied to the nearest
   fuel stop. (Reframed from the backlog's "fuel-stop vote" — a vote makes no sense when one empty
   tank strands everyone, so it's a broadcast-and-rally, not a tally.)

**Non-goals (this stays a UI/UX mockup):** no backend, network sync, auth, or real telemetry; no
real re-routing of the ride to the fuel POI (rally = alert + highlighted destination marker + pack
tighten, not a route change); no multi-device presence. The remaining three backlog themes (Safety &
SOS, Gear & maintenance, Road segments & scoring) stay deferred, each to its own spec.

## 2. Design Decisions Already Locked (via brainstorm)

- **Simulation depth: moving pack.** The simulator gives every participant its own progress along the
  route with realistic, time-varying gaps — Lead ahead, Sweep at the tail — and lets the Sweep drift
  behind. Every mechanic reads as real rather than scripted.
- **Roles: auto + reassignable.** At ride start, self = Lead and the rear buddy = Sweep automatically;
  tapping a rider in the formation list reassigns their role.
- **Formation view: Stats / Pack toggle** (option C from the visual brainstorm). A segmented toggle in
  the dashboard swaps the stat grid for a full vertical pack list. Role-colored markers are always on
  the map. Chosen because it gives the pack list room (connecting line, gaps, behind-flag) without
  permanently spending the stat grid.
- **Signals: living.** The simulator auto-emits a "fell behind" event when the Sweep's gap crosses a
  threshold; broadcasting Regroup visibly closes the gaps over the next ticks; a fuel call names the
  nearest fuel POI as the rally point, highlights its marker, and tightens the pack.
- **Fuel is a broadcast, not a vote.** No yes/no tally UI.

## 3. Data Model — `data/model/RideSession.kt`

New role enum:

```
enum class RiderRole { LEAD, SWEEP, RIDER }
```

`RideParticipantState` gains three fields (all defaulted, so existing construction sites keep
compiling):

```
data class RideParticipantState(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val position: GeoPoint,                  // now derived each tick from distanceAlongRouteMeters
    val isSpeaking: Boolean = false,
    val role: RiderRole = RiderRole.RIDER,
    val distanceAlongRouteMeters: Double = 0.0,   // per-rider progress; drives position, order, gaps
    val hasFallenBehind: Boolean = false,
)
```

Ordering and gaps are derived from `distanceAlongRouteMeters` (front rider = largest distance; gap to
the rider ahead = difference in metres). `position` is recomputed from that distance via the existing
`pointAlongRoute` helper.

New transient signal type:

```
enum class GroupSignalType { REGROUP, FUEL }

data class GroupSignal(
    val type: GroupSignalType,
    val fromUserId: String,
    val fromName: String,
    val rallyPoi: PointOfInterest? = null,   // set for FUEL
)
```

`RideSessionEvent` (the existing sealed interface consumed by `RideSessionScreen`) gains two cases:

```
data class RiderFellBehind(val participant: RideParticipantState) : RideSessionEvent
data class GroupSignalRaised(val signal: GroupSignal) : RideSessionEvent
```

## 4. Simulator — `simulation/RideSimulator.kt` (+ pure helpers)

**`advance()` moves every participant.** Today only index 0 (self) advances; the rest keep their
static initial position. The new pure `advance()`:

- Advances each participant's `distanceAlongRouteMeters` per tick. The Lead (front of pack) tracks the
  session speed; trailing riders move with small deterministic variance (a function of elapsed time +
  a per-rider phase) so inter-rider gaps flex naturally. The Sweep trails the most.
- Recomputes each participant's `position` from its distance along the route.
- When a rider's gap to the rider ahead crosses `FALL_BEHIND_THRESHOLD_METERS`, sets
  `hasFallenBehind = true` and the loop emits `RiderFellBehind(participant)` **exactly once** on the
  transition (mirroring how a new `GOAL_REACHED` leg emits `GoalReached`).

**New driver methods** (each mutates `_session` and/or emits on `_events`, mirroring
`setGoal`/`simulateDrift`):

- `setRole(userId: String, role: RiderRole)` — assigns the role; enforces the single-Lead /
  single-Sweep invariant by demoting whoever currently holds that role to `RIDER` (a swap).
- `broadcastRegroup()` — flags the pack to "regrouping": over the next ticks, riders that are behind
  accelerate to close their gaps; clears `hasFallenBehind`; emits
  `GroupSignalRaised(GroupSignal(REGROUP, self))`.
- `callFuel(nearestFuel: PointOfInterest?)` — emits `GroupSignalRaised(GroupSignal(FUEL, self,
  rallyPoi = nearestFuel))` and engages the same pack-tighten as regroup. The nearest POI is passed in
  (computed in the ViewModel, which owns the POI repo) so `advance()` stays a pure function of session
  state.

**Pure, unit-tested helpers** (in `RideSessionCalculations.kt` or a new `GroupRideCalculations.kt`):

- `packOrder(participants)` / `gapsMeters(participants)` — ordering + inter-rider gaps from distances.
- `sweepGapExceedsThreshold(...)` — the drift-detection predicate.
- `nearestFuelPoi(pois, from: GeoPoint): PointOfInterest?` — nearest `POI_FUEL` by haversine.
- `assignInitialRoles(participants): List<RideParticipantState>` — self → Lead, rear buddy → Sweep,
  rest → Rider, plus the staggered initial `distanceAlongRouteMeters` offsets that spread the pack at
  ride start.

## 5. In-Ride UI — `ui/rides/`

**Map (`RideSessionHud.kt`).** Markers are colored by **role** instead of the flat self/buddy split:
Lead = `MotouringColors.goal` (orange), Sweep = `poiRest` (amber), Rider = `rider` (blue), fallen-
behind = `riderCoral`. The self marker keeps an identifying ring so "you" always reads regardless of
role. When a fuel call is active, the rally fuel POI is drawn as a highlighted (`selected`) POI marker.

**Dashboard (`RideDashboard.kt`).** The goal-progress `InstrumentRing` stays pinned at the top. Below
it, a **Stats / Pack segmented toggle**:

- **Stats** — today's `StatGrid` + `GroupBar` (unchanged).
- **Pack** — a new `FormationList`: a vertical, front-to-back ordered list with a connecting line down
  the left edge; each row shows the role-colored avatar, name (self tagged "· you"), a role badge, the
  gap to the rider ahead, a speaking pulse (driven by existing `isSpeaking`), and a "behind" flag when
  `hasFallenBehind`. **Tapping a row opens a role-reassignment menu** (Make Lead / Make Sweep / Make
  Rider), built on the existing `GoalChoiceSheet` / `MotouringCard` sheet pattern.

**Banners (`RideSessionBanners.kt`).** Two new bottom banners in the established `MotouringCard`
style, with the existing flag + timed `LaunchedEffect` auto-dismiss:

- `RegroupBanner` — auto form ("Dimas fell behind — regrouping") and broadcast-confirmation form
  ("Regroup — wait for me").
- `FuelCallBanner` — "{name} needs fuel — rally at {poi name}".

**Screen (`RideSessionScreen.kt`).** The events `LaunchedEffect` gains handling for `RiderFellBehind`
(→ auto regroup banner) and `GroupSignalRaised` (→ the matching banner). A small **action row**
surfaces the real in-ride controls **Regroup** and **Fuel**; the existing `RideDebugControls` gains a
"Force behind" trigger to demo the auto fell-behind event on demand.

## 6. Wiring & Fake Data

- **`ui/components/map/MotouringMap.kt`** — extend `MarkerStyle` with the role variants (Lead / Sweep /
  Rider / Behind), add their colors to `MarkerStyle.color()` and to the MapLibre `match` expression,
  and add a way to mark the self marker (a ring / `isSelf` flag on `MapMarker`). The `LocalInspectionMode`
  fallback Canvas is updated to color its markers the same way so previews stay representative.
- **`RideSessionViewModel.kt`** — add pass-throughs `setRole`, `broadcastRegroup`, `callFuel`
  (`callFuel` computes `nearestFuelPoi` from the injected repo, then calls the simulator). Its
  `factory` gains a `poiRepository` parameter and runs `assignInitialRoles` when building the
  participant list. Update the nav wiring in `MotouringNavHost.kt` to pass `poiRepository`.
- **`data/fake/FakeDataProvider.kt`** — ensure group rides seed at least three color-distinct buddies
  (via `RideBuddyRepository.friends()`), confirm at least one `POI_FUEL` sits near `sampleRoute` for a
  believable rally, and update `previewRideSessionWithGoal()` / `previewRideSessionEndless()` (and add a
  group preview) so roles, distances, and a fallen-behind rider render in `@Preview`.

## 7. Testing & Verification

- **TDD the pure logic** (headless, runs on the VM): `advance()` moves all participants and computes
  gaps; Sweep drift crosses the threshold and emits a single `RiderFellBehind`; `broadcastRegroup`
  closes gaps over ticks and clears the behind flag; `setRole` preserves the single-Lead / single-Sweep
  invariant; `nearestFuelPoi` picks the closest fuel POI; `assignInitialRoles` yields self = Lead, rear
  = Sweep with staggered offsets. Keep all existing tests green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — MapLibre, the
  segmented toggle, the formation list, and the banners can't be seen on the headless VM.
- Build must stay green headless: `./gradlew assembleDebug` (and `testDebugUnitTest`).

## 8. File-by-File Summary

| File | Change |
| --- | --- |
| `data/model/RideSession.kt` | `RiderRole`, participant fields, `GroupSignal(Type)`, two new events |
| `simulation/RideSimulator.kt` | move all participants, drift detection, `setRole` / `broadcastRegroup` / `callFuel` |
| `simulation/GroupRideCalculations.kt` (new) | pure gap/order/nearest-fuel/auto-assign helpers |
| `ui/rides/RideSessionViewModel.kt` | driver pass-throughs, `poiRepository` injection, role init |
| `ui/rides/RideDashboard.kt` | Stats/Pack toggle + `FormationList` + role-reassign menu |
| `ui/rides/RideSessionHud.kt` | role-colored markers, self ring, fuel rally marker |
| `ui/rides/RideSessionBanners.kt` | `RegroupBanner`, `FuelCallBanner` |
| `ui/rides/RideSessionScreen.kt` | consume new events, banner state, Regroup/Fuel action row |
| `ui/components/map/MotouringMap.kt` | role `MarkerStyle` variants + colors + self marker |
| `navigation/MotouringNavHost.kt` | pass `poiRepository` into the ride-session VM factory |
| `data/fake/FakeDataProvider.kt` | color-distinct buddies, near-route fuel POI, role-aware previews |

## 9. Risks / Open Questions

- **Route length vs ride distance.** The route is a short 5-point polyline while ride distance grows
  unbounded; per-rider position mapping must clamp or wrap `distanceAlongRouteMeters` onto the route
  (follow whatever the current `pointAlongRoute` mapping does for self). The *gaps* are what matter
  visually, so exact wrap behavior is low-stakes.
- **Marker distinctness.** Lead (orange) vs the app's orange accent, and self-ring legibility, may need
  on-device tuning; the role palette is otherwise well separated.
- **Roles decoupled from order.** After manual reassignment a "Lead" badge can sit on a mid-pack rider.
  Accepted for a mockup — roles are labels, not enforced positions.

## 10. Scope Boundary & Backlog

**In this spec:** rider roles, moving pack + formation view, regroup ping (auto + broadcast), fuel call.

**Deferred — each its own brainstorm → spec → build cycle, in order:**
1. **Safety & SOS** — one-tap SOS live-location share, simulated crash/fall auto-alert, "rider fell
   behind" alert distinct from the manual regroup.
2. **Gear & maintenance** — per-vehicle service log with mileage reminders, pre-ride checklist.
3. **Road segments & scoring** — twisty-road segments + leaderboards, cornering/lean score, scenic
   discovery.
