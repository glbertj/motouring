# Motouring — Scenic-Route Discovery (Spec 6)

**Date:** 2026-07-16
**Status:** Design — awaiting review
**Track:** Standalone follow-up — the one sub-idea deferred from Spec 5 (Road Segments & Scoring). Not part
of the original four-theme backlog (which is complete); a small extra surface.

## 1. Problem & Goals

The app can track and score rides, but offers nothing to help a rider decide **where** to go. This spec
adds a **scenic-route discovery** browse surface — a curated set of scenic routes you can browse, open for
detail (photo, description, map preview), and launch a ride from. It gives the Start-Ride FAB's currently
dead-ended **"Plan a route"** action a real destination.

Two screens on top of a seeded repository:
1. **Scenic Routes browse** — hero-photo cards (name, region, distance · est. time, vibe chips).
2. **Scenic Route detail** — hero photo, name/region, distance/duration, vibe chips, description, a
   `MotouringMap` route preview, and a **"Ride this route"** call-to-action.

**Non-goals (this stays a UI/UX mockup):** no real route planning/navigation, no GPS, no network; the
routes are **seeded** in-memory. **"Ride this route" navigates to the existing Start-Ride flow — it does
NOT thread the scenic route's polyline into the ride simulator** (the in-ride route stays the existing
`sampleRoute`; threading a chosen route would touch the recently-reworked Start-Ride → RideSession path and
isn't worth it for a mockup). No filtering/search (a small curated list doesn't need it).

## 2. Design Decisions Already Locked (via brainstorm)

- **Entry point: repurpose the FAB "Plan a route" action.** It currently navigates to Start Ride; it will
  navigate to the Scenic Routes browse instead. Flow: FAB → Plan a route → browse → detail → "Ride this
  route" → Start Ride.
- **Route preview on detail via `MotouringMap`.** Each scenic route carries a short `route: List<GeoPoint>`
  polyline; the detail screen renders it with the existing `MotouringMap` (Canvas fallback under
  `LocalInspectionMode`, MapLibre on-device) — differentiating the detail from a plain photo screen and
  reusing the map component.
- **"Ride this route" is a light navigation** to `START_RIDE` (the route selection is illustrative; the
  ride sim keeps using `sampleRoute`).
- **Reuse the browse+detail idiom** from Segments/Nearby: `MotouringCard` list + id-arg detail route +
  snapshot ViewModels. Hero images come from the bundled `img_road_*` drawables.
- **Vibe tags** are a small enum (`ScenicVibe { COASTAL, MOUNTAIN, FOREST, URBAN }`), rendered as chips
  reusing existing accent tokens.

## 3. Data Model

New file `data/model/ScenicRoute.kt`:

```
enum class ScenicVibe { COASTAL, MOUNTAIN, FOREST, URBAN }

data class ScenicRoute(
    val id: String,
    val name: String,
    val region: String,
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val vibe: List<ScenicVibe>,
    val heroPhotoRes: Int,
    val description: String,
    val route: List<GeoPoint>,
)
```

No changes to existing models. (`GeoPoint` already exists.)

## 4. Repository

New `data/repository/ScenicRouteRepository.kt`: `MutableStateFlow<List<ScenicRoute>>` seeded from
`FakeDataProvider`. `observeRoutes()`, `routes(): List<ScenicRoute>`, `route(id): ScenicRoute?`. Registered
in `AppContainer`. (Mirrors `SegmentRepository`/`PoiRepository`.)

`FakeDataProvider` seeds ~4 scenic routes with real names/regions, `img_road_*` heroes, short `route`
polylines (like `sampleRoute`), and a couple of vibe tags each.

## 5. UI

- **`ui/scenic/ScenicRoutesViewModel.kt` + `ScenicRoutesScreen.kt`** (new) — a browse list over
  `ScenicRouteRepository.observeRoutes()`. Each card: hero image, name, region, "{distanceKm} km ·
  {estimatedMinutes} min", and vibe chips. Tapping a card opens the detail.
- **`ui/scenic/ScenicRouteDetailViewModel.kt` + `ScenicRouteDetailScreen.kt`** (new) — a header (hero photo,
  name, region, distance/duration, vibe chips), the description, a **`MotouringMap`** showing the route
  polyline (`MapPolyline(route)`, camera on the route's first point, no markers), and a **"Ride this
  route"** button that invokes an `onRideRoute` callback. The detail VM is a snapshot exposing the looked-up
  route.
- **Vibe chip** — a small labeled chip; colors reuse existing accent tokens (e.g. `MotouringColors.rider` /
  `poiRest` / `poiFuel` / `riderPurple` per vibe), no new hexes.

## 6. Navigation

- `Destinations`: add `SCENIC_ROUTES = "scenic_routes"`, `SCENIC_ROUTE_DETAIL_PATTERN =
  "scenic_route_detail/{routeId}"`, `fun scenicRouteDetail(id)`.
- `MotouringNavHost`: add both composables (detail uses the id-arg pattern), building the VMs via factories
  from `appContainer.scenicRouteRepository`. The browse's `onRouteClick` → `scenicRouteDetail(id)`; the
  detail's `onRideRoute` → `START_RIDE`.
- `MainScaffold`: change the FAB menu's **`onPlanRoute`** to navigate to `SCENIC_ROUTES` (instead of
  `START_RIDE`).

## 7. File-by-File Summary

| File | Change |
| --- | --- |
| `data/model/ScenicRoute.kt` (new) | `ScenicVibe`, `ScenicRoute` |
| `data/repository/ScenicRouteRepository.kt` (new) | seeded routes, `observeRoutes`/`routes`/`route` |
| `di/AppContainer.kt` | register `scenicRouteRepository` |
| `data/fake/FakeDataProvider.kt` | seed ~4 scenic routes |
| `ui/scenic/ScenicRoutesViewModel.kt` + `ScenicRoutesScreen.kt` (new) | browse list |
| `ui/scenic/ScenicRouteDetailViewModel.kt` + `ScenicRouteDetailScreen.kt` (new) | detail + map preview + Ride CTA |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` | scenic routes + wiring |
| `ui/main/MainScaffold.kt` | FAB "Plan a route" → `SCENIC_ROUTES` |

## 8. Testing & Verification

- **TDD the small logic that exists** (headless): `ScenicRouteRepository.route(id)` returns the match /
  null; the VMs expose the seeded list / the looked-up route (constructed directly, snapshot pattern — no
  coroutine in init, like the Segments VMs). Keep all existing tests green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — the browse cards, the
  map preview, and the FAB flow can't be seen on the headless VM.
- Build stays green headless: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 9. Risks / Open Questions

- **"Ride this route" doesn't change the actual ride route** (uses `sampleRoute`). Accepted — consistent
  with the mockup's no-real-navigation stance; a future spec could thread the route through if wanted.
- **`MotouringMap` on detail needs device network for tiles** and won't render in `@Preview`/on the VM (the
  Canvas fallback covers previews). Same constraint as every other map surface.
- **FAB behavior change** — "Plan a route" no longer goes to Start Ride. That's the intent (it was a
  dead-end); the scenic detail's "Ride this route" is the new path into Start Ride.

## 10. Scope Boundary

**In this spec:** a seeded scenic-route repository, a browse list, a detail screen (photo + description +
map preview + Ride CTA), and the FAB "Plan a route" repurpose.

**With this, the deferred sub-idea from the four-theme backlog is also built.** No further niche-feature
work is queued.
