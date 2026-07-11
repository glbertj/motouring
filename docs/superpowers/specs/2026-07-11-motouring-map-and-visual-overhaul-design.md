# Motouring — Map & Visual Overhaul (Spec 1)

**Date:** 2026-07-11
**Status:** Design — awaiting review
**Track:** UI/UX redesign (first of a sequenced set; niche-feature specs follow)

## 1. Problem & Goals

The app works but reads as "plain and boring": a single-orange palette, flat icons, grey vector
placeholders, no real map, a disabled Nearby tab, and an in-ride screen that shows little more than a
speed number over a hand-drawn Canvas route. The bottom bar buries "start a ride" as an ordinary
button.

This spec covers a single, foundation-first visual overhaul. Its goals:

1. Introduce a **real, free, palette-matched map** (MapLibre + OpenFreeMap, no token) as a reusable
   component, and use it in-ride and in Nearby.
2. Rework the **bottom bar** into a Strava-style center **Start Ride** FAB with a quick-action menu.
3. Rebuild the **in-ride screen** as a balanced map-over-dashboard layout with a rich stat set.
4. Build the greenfield **Nearby** screen as a full-map + draggable bottom sheet.
5. A cross-cutting **visual polish** pass: category accent colors, richer icons, and bundled CC0 photos.

**Non-goals (explicitly deferred to later specs):** the four niche-feature themes — Group ride
mechanics, Safety & SOS, Gear & maintenance, Road segments & scoring. They remain a sequenced backlog
(§9). No backend, network sync, auth, or real telemetry — this stays a mockup.

## 2. Design Decisions Already Locked (via visual brainstorm)

- **Map tech:** MapLibre GL Native (Android) + OpenFreeMap tiles. Free, no token/signup. MapLibre is
  the open-source Mapbox GL fork, so a later move to Mapbox is a dependency/style swap, not a rewrite.
- **FAB behavior:** tap opens a **quick-action menu** (Start Solo · Start Group · Plan a route); the
  FAB icon rotates into an X while open.
- **In-ride layout:** **balanced split** — map on top ~half, rich dashboard below.
- **Nearby layout:** **full-map + draggable bottom sheet** (Google-Maps pattern) with peek / half /
  expanded anchors; tapping a POI recenters the map camera to it.
- **Photos:** **bundle CC0 photos locally** as drawables (reliable offline, no new network dep).

## 3. The Map Foundation — `MotouringMap`

A new reusable composable in `ui/components/map/` that wraps the MapLibre `MapView` via `AndroidView`,
owning the view lifecycle. It is the single integration point every map surface uses.

**API (conceptual):**

```
MotouringMap(
  cameraTarget: MapCamera,              // center LatLng + zoom; animates on change
  markers: List<MapMarker>,             // id, LatLng, MarkerStyle (self / buddy / poi-type), selected
  polylines: List<MapPolyline> = [],    // e.g. the ride route
  onMarkerClick: (markerId) -> Unit = {},
  modifier: Modifier,
)
```

**Styling:** a custom MapLibre **style JSON** committed under `res/raw/` that consumes OpenFreeMap's
free vector tiles and recolors land/water/roads/labels to the Analog Dash charcoal palette, so the map
reinforces the design instead of clashing. (Fallback path during bring-up: OpenFreeMap's dark/positron
hosted style; the custom style is the target.)

**Preview / offline fallback (important):** MapLibre's `MapView` renders nothing in Android Studio
`@Preview` (`LocalInspectionMode`), in unit/screenshot tests, or with no network. `MotouringMap`
therefore renders the **existing `RidePlaceholderRoute` Canvas** (generalized to draw its markers +
polyline) whenever `LocalInspectionMode` is true or the SDK/tiles are unavailable. The hand-drawn
Canvas is kept and repurposed as this fallback — previews keep working, and the app degrades
gracefully offline.

**Manifest:** add `INTERNET` permission (tiles load over the network). No API key. Leave the existing
unused `BuildConfig.MAPBOX_PUBLIC_TOKEN` plumbing in place for the future Mapbox migration.

**Dependency:** add `org.maplibre.gl:android-sdk` (Maven Central) to `app/build.gradle.kts`. Builds
headlessly on the VM; rendering is verified on-device by the user.

## 4. Bottom Bar → Center FAB

In `ui/main/MainScaffold.kt` and `ui/main/BottomTab.kt`:

- **Enable Nearby** as a real tab: add its `composable()` route to the nested NavHost and drop the
  `implementedTabRoutes` gate (the "Task 15" block).
- Replace the 5-across `NavigationBar` with a **4 tabs + center FAB** layout: Home · Nearby ·
  **[FAB]** · Rides · Profile. Implement as a `Box`/custom bar (the FAB floats above the bar, notched
  in) rather than fighting `NavigationBar`'s equal-weight slots.
- **FAB:** rounded-square (18–20dp radius) orange gradient, raised with shadow, play-triangle icon.
  Tapping toggles a **quick-action menu**: three labeled pills arc up (Start Solo · Start Group ·
  Plan a route) over a scrim; the FAB icon springs from ▶ to ✕. Start Solo / Start Group route into
  `START_RIDE` with the mode preselected; Plan a route → `START_RIDE` (route-planning is a later
  concern, so it lands on Start Ride for now).
- Keep the existing selected-tab scale animation and `MotouringMotion` springs.

## 5. In-Ride Screen — Balanced Split

Rework `ui/rides/RideSessionScreen.kt` + `RideSessionHud.kt`. All existing behavior is preserved:
goal/endless modes, `GoalCelebrationOverlay`, `GoalChoiceSheet`, `UndoGoalSnackbar`, `DriftToast`, and
the `RideSessionViewModel` event stream. Only the visual frame and stat richness change. The three raw
debug buttons ("Set a goal", "Simulate off-route", "End Ride") move into an unobtrusive control
row / overflow so they stop dominating the screen.

**Layout (top→bottom):**

- **Top ~55% — `MotouringMap`**: the ride route as a polyline, the rider's own puck, and buddy markers
  at their `RideParticipantState.position`s. Camera follows the lead position. Overlaid: a small
  "◍ N riding" pill (top-left) and the goal chip "→ {goal}" (top-right); a floating **speed** readout
  (large IBM Plex Mono numerals) bottom-left of the map.
- **Bottom ~45% — dashboard:**
  - A **goal-progress `InstrumentRing`** (reuse existing) showing km-to-goal + percent, beside a
    **6-cell stat grid**: Distance · Avg speed · Elapsed · Max speed · Climb (m) · To-goal (km).
    In Endless mode the ring switches to the existing "endless since last stop" treatment.
  - A **group bar**: overlapping buddy avatars (colored initials) + a live "**{name} speaking**"
    indicator with a green pulse, driven by the existing `isSpeaking` state.

**Model / simulator additions** (`data/model/RideSession.kt`, `simulation/RideSimulator.kt`,
`RideSessionCalculations.kt`): track **maxSpeed** and a fake **elevationGain** (simulator increments
per tick), and derive **avgSpeed** (distance ÷ elapsed) and **toGoalKm** (activeGoal − distance).
Participants render as colored-initial avatars (add a stable color per participant id; use existing
name field).

## 6. Nearby Screen (greenfield)

New `ui/nearby/NearbyScreen.kt` + `NearbyViewModel.kt` over the existing `PoiRepository`
(`observePois`, `filterByVehicleType`) and `PointOfInterest` model.

**Layout:** full-screen `MotouringMap` with the user's location puck and a colored **type-pin** per
POI. Filter chips pinned top (All · Fuel · Repair · Food · + vehicle filter). A **`BottomSheetScaffold`**
holds the POI list with three anchors — **peek** (opens here, ~1–2 cards, title "N places nearby"),
**half**, **expanded** (full list, thin map sliver). Cards show type thumbnail, name, ★ rating,
distance, and vehicle-fit tags.

**Interaction (must match Google Maps):**
- Tapping a **pin** or a **card** → the map **animates the camera to that POI**, marks that pin
  selected (enlarged/glow), raises the sheet to **half**, and scrolls the matching card into view.
- Filter chips filter both pins and list together.

**Data additions:** add a **`REST_STOP`/Food** `PoiType` plus a few sample POIs, and a `distanceKm`
field on the sample data (or derive from a fixed user location) so cards can show distance. Wire the
already-instantiated `poiRepository` into the new ViewModel.

## 7. Visual Polish (cross-cutting)

- **Category accent colors** added to `MotouringColors` (`ui/theme/Color.kt`) on top of the single
  orange: Fuel = green `#4ADE80`, Repair = brand orange `#FF5A36`, Food/Rest = amber `#F5C34B`,
  Rider/Self = blue `#7CB8FF`, Speaking = green `#4ADE80`. Used by pins, POI thumbnails, the group
  bar, and small category tags. (Exact hexes finalizable during implementation; the point is a
  restrained multi-hue set, not a rainbow.)
- **Icons:** prefer filled/duotone `material-icons-extended` glyphs with accent color over flat
  outlines; give each `PoiType` a distinct icon; FAB uses a play/plus glyph.
- **Bundled CC0 photos:** fetch ~10–15 CC0 / permissively-licensed photos (Unsplash/Pexels license —
  free commercial use, no attribution required), commit as size-optimized `WebP` drawables, and record
  sources in a `CREDITS.md`. Categories: ~6 road/landscape shots (feed `PostCard`s + ride-summary
  hero), ~3 vehicle photos. **People avatars stay as generated colored-initial avatars** (consistent,
  zero copyright risk) rather than stock face photos. Models already store `@DrawableRes Int`, so this
  is swapping placeholder resource ids for real ones — **no Coil / no network image path added**.
- **Stretch (optional):** replace `ic_route_preview_placeholder` thumbnails in Rides history /
  summary with a static MapLibre snapshot of the route. Nice-to-have; not required for this spec.

## 8. Testing & Verification

- This is a UI mockup; the primary gate is **on-device visual review by the user on the Arch host**
  (the VM is headless — MapLibre and the sheet can't be seen here).
- Keep existing unit tests green. Add focused unit tests for the new pure logic: simulator `maxSpeed` /
  `elevationGain` accumulation, `RideSessionCalculations` avg-speed / to-goal derivations, and POI
  distance/type filtering.
- `MotouringMap`, being `AndroidView` interop, is not unit-tested; its **fallback Canvas path** keeps
  `@Preview`s and any screenshot tests rendering.
- Build must stay green on the headless VM (`./gradlew assembleDebug`) with no map token configured.

## 9. Scope Boundary & Backlog

**In this spec:** map foundation, FAB menu, in-ride split, Nearby sheet, visual polish.

**Deferred — each gets its own brainstorm → spec → build cycle, in this order:**
1. **Group ride mechanics** — rider roles (lead/sweep), "regroup — wait for me" ping, pack/formation
   view, fuel-stop voting. *(Next up; highest synergy with existing group-ride + voice sim.)*
2. **Safety & SOS** — one-tap SOS live-location share, simulated crash/fall detection auto-alert,
   "rider fell behind" group alert.
3. **Gear & maintenance** — per-vehicle service log with mileage reminders, pre-ride checklist.
4. **Road segments & scoring** — twisty-road segments + leaderboards, cornering/lean score,
   scenic-route discovery.

## 10. Risks / Open Questions

- **MapLibre style fidelity:** recoloring OpenFreeMap vector layers to the exact charcoal palette is
  iterative; the custom `res/raw` style JSON may need on-device tuning. Fallback hosted dark style
  de-risks bring-up.
- **Tile network dependency:** the map needs connectivity on the test device; acceptable for a
  reviewed mockup, and the fallback Canvas covers the offline/preview case.
- **APK size:** bundled WebP photos add weight; keep the set small (~10–15) and optimized.
