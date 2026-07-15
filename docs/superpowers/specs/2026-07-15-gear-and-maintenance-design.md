# Motouring — Gear & Maintenance (Spec 4)

**Date:** 2026-07-15
**Status:** Design — awaiting review
**Track:** Niche rider-community features (third of four; follows Spec 3 Safety & SOS).

## 1. Problem & Goals

The app tracks rides and shows a vehicle garage, but nothing about **keeping the machine healthy**. This
spec extends the garage with two rider-practical mechanics:

1. **Per-vehicle service log with mileage reminders** — each vehicle gets an odometer and a small list of
   service items (oil, chain, tires, brakes / coolant). Each item shows how far since it was last done
   against its interval, with a **status** (OK / due soon / overdue). "Mark serviced" resets the
   interval. Due counts surface as a badge on the garage cards.
2. **Pre-ride TCLOCS checklist** — a quick, manual, tap-to-confirm safety check (Tires, Controls, Lights,
   Oil, Chassis, Stands) folded into the Start-Ride flow as an optional step.

**Non-goals (this stays a UI/UX mockup):** no real telemetry, sensors, or OBD — the odometer is a
**seeded mock** value and the checklist is a **manual self-inspection** (the app senses nothing); no live
mileage accumulation from rides (a single ~40 km ride barely moves a 5-digit odometer, and rides carry
only a `vehicleType`, not a `vehicleId`); no persisted checklist history (the ticks reset each ride); no
standalone checklist screen (the check lives in Start-Ride only). The last backlog theme (Road segments &
scoring) stays deferred.

## 2. Design Decisions Already Locked (via brainstorm + visual mockups)

- **Semantic status colors, separate from the accent.** `StatusOk` = green (`#4ADE80`, reuse
  `PoiFuel`/`Speaking`), `StatusDueSoon` = amber (`#F5C34B`, reuse `PoiRest`), `StatusOverdue` = red
  (`#FF3B30`, reuse `SosRed` from Spec 3). Exposed via `MotouringColors` (`statusOk` / `statusDueSoon` /
  `statusOverdue`). No new hexes — these all already exist as raw colors.
- **Mileage = seeded odometer + manual.** A new `odometerKm` on `Vehicle`, seeded so items already sit in
  interesting states out of the box. The odometer is user-editable (tap to update). No ride-linked
  accumulation.
- **Service items:** motorcycle → Oil, Chain, Tires, Brakes; car → Oil, Tires, Brakes, Coolant. Each has a
  `lastServicedKm` and an `intervalKm`.
- **Status rule:** `kmSince = odometer − lastServiced`; `kmSince ≥ interval` → **Overdue**; `kmSince ≥
  interval × 0.85` → **Due soon**; else **OK**. "Needs attention" (the badge count) = due-soon + overdue.
- **"Mark serviced" is one-tap** (no confirm): sets `lastServicedKm = current odometer`, resetting the
  item to OK. Easily redone in a mockup.
- **Checklist: TCLOCS, in Start-Ride, optional.** Six manual tap-to-confirm items; ephemeral checked state
  that resets each ride; **advisory** — a "Skip check & start" path always works (silent skip, no nag).
- **Garage cards get a due badge.** Profile → My Garage cards show a small "N due" chip and become
  tappable → the maintenance screen.

## 3. Data Model

- `Vehicle` gains `odometerKm: Int = 0` (defaulted; seeded in `FakeDataProvider`).
- New file `data/model/ServiceRecord.kt`:

```
enum class ServiceType { OIL, CHAIN, TIRES, BRAKES, COOLANT }
enum class ServiceStatus { OK, DUE_SOON, OVERDUE }   // derived, never stored

data class ServiceItem(
    val vehicleId: String,
    val type: ServiceType,
    val lastServicedKm: Int,
    val intervalKm: Int,
)
```

- New raw colors in `ui/theme/Color.kt` are unnecessary — add `MotouringColors.statusOk = PoiFuel`,
  `statusDueSoon = PoiRest`, `statusOverdue = SosRed` (all already defined).
- The pre-ride checklist needs no persisted model: a static ordered list of TCLOCS labels plus ephemeral
  Compose state. (A small `TclocsItem` enum with the six labels keeps it typed and testable.)

## 4. Pure Logic — `simulation/MaintenanceCalculations.kt`

All headless-testable:

- `fun serviceStatus(odometerKm: Int, lastServicedKm: Int, intervalKm: Int): ServiceStatus` — the rule in
  §2 (constant `DUE_SOON_FRACTION = 0.85`).
- `fun kmSinceService(odometerKm: Int, lastServicedKm: Int): Int` and
  `fun serviceProgressFraction(odometerKm, lastServicedKm, intervalKm): Float` (0..1, clamped) for the bar.
- `fun dueCount(items: List<ServiceItem>, odometerKm: Int): Int` — count of items whose status is
  `DUE_SOON` or `OVERDUE`.

Keeping the status rule and due-count pure means the ViewModel and the garage badge share one tested
source of truth.

## 5. Repositories

- `Vehicle`/`VehicleRepository`: add `fun setOdometer(vehicleId: String, odometerKm: Int)` (immutable
  `map`-copy, same pattern as the existing `addVehicle`).
- New `data/repository/MaintenanceRepository.kt`: holds `MutableStateFlow<List<ServiceItem>>` seeded from
  `FakeDataProvider`. `observeItems()`, `itemsFor(vehicleId): List<ServiceItem>`, and
  `markServiced(vehicleId, type, atOdometerKm)` (copies the matching item with `lastServicedKm =
  atOdometerKm`). Registered in `AppContainer`.

## 6. UI

- **`ui/vehicle/VehicleMaintenanceScreen.kt` + `VehicleMaintenanceViewModel.kt`** (new). The VM takes
  `VehicleRepository` + `MaintenanceRepository` + a `vehicleId`, and exposes the vehicle, its service items
  with computed `ServiceStatus`, and the due count. Screen: an **odometer header** (vehicle name/photo +
  the odometer, tappable to edit via a small dialog), a "N need attention" summary, and a **service-item
  list** — each row shows the icon, name, "last {km} · {kmSince} ago · every {interval}", a status chip
  (`statusOk`/`statusDueSoon`/`statusOverdue`), a progress bar colored by status, and a one-tap **Mark
  serviced** action (calls `markServiced(type, currentOdometer)`).
- **Profile → My Garage** (`ProfileScreen` + `ProfileViewModel`): each vehicle card gains a small
  **"N due"** status chip (from `dueCount`, needs `MaintenanceRepository` wired into `ProfileViewModel`)
  and becomes **clickable → navigates** to the maintenance screen for that vehicle.
- **Pre-ride checklist** (`StartRideScreen`): a new **`PreRideChecklist`** composable — an optional card
  under the already-selected vehicle titled "Pre-ride check · TCLOCS", six tap-to-confirm rows with a
  checkbox each and an "N / 6" progress readout. Checked state is `remember`ed locally and **resets when
  the ride starts**. The existing Start action stays; a **"Skip check & start"** affordance always works
  (the checklist never blocks starting).

## 7. Navigation

- `Destinations`: add `VEHICLE_MAINTENANCE_PATTERN = "vehicle_maintenance/{vehicleId}"` +
  `fun vehicleMaintenance(vehicleId: String)`.
- `MotouringNavHost`: add the `vehicle_maintenance/{vehicleId}` composable (builds
  `VehicleMaintenanceViewModel` via its factory from `appContainer`), and wire the Profile garage card's
  `onClick` to navigate to it.

## 8. File-by-File Summary

| File | Change |
| --- | --- |
| `data/model/Vehicle.kt` | `+ odometerKm` |
| `data/model/ServiceRecord.kt` (new) | `ServiceType`, `ServiceStatus`, `ServiceItem` |
| `ui/theme/Color.kt` | `MotouringColors.statusOk / statusDueSoon / statusOverdue` (aliases) |
| `simulation/MaintenanceCalculations.kt` (new) | pure `serviceStatus` / `kmSince` / `progressFraction` / `dueCount` |
| `data/repository/VehicleRepository.kt` | `+ setOdometer` |
| `data/repository/MaintenanceRepository.kt` (new) | seeded service items, `itemsFor`, `markServiced` |
| `di/AppContainer.kt` | register `maintenanceRepository` |
| `ui/vehicle/VehicleMaintenanceViewModel.kt` (new) | vehicle + items + due count, `markServiced`, `setOdometer` |
| `ui/vehicle/VehicleMaintenanceScreen.kt` (new) | odometer header, service list, mark-serviced |
| `ui/rides/StartRideScreen.kt` | `PreRideChecklist` optional step |
| `ui/profile/ProfileScreen.kt` + `ProfileViewModel.kt` | due badge + tappable garage cards |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` | maintenance route + wiring |
| `data/fake/FakeDataProvider.kt` | seed odometers + service items (some due/overdue) |

## 9. Testing & Verification

- **TDD the pure logic** (headless): `serviceStatus` boundaries (just-below vs at the due-soon fraction; at
  vs past interval), `kmSinceService`, `serviceProgressFraction` clamping (fresh service → ~0, way overdue
  → 1.0), `dueCount`. `MaintenanceRepository.markServiced` resets `lastServicedKm` and `itemsFor` filters;
  `VehicleRepository.setOdometer` updates. Keep all existing tests green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — the maintenance
  screen, the garage badges, and the checklist can't be seen on the headless VM.
- Build stays green headless: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 10. Risks / Open Questions

- **Checklist ↔ vehicle type.** TCLOCS is motorcycle vocabulary; for a car the six labels are reused as-is
  in this mockup (a future pass could adapt labels per `VehicleType`). Accepted.
- **Odometer edit UX.** A minimal numeric dialog is enough for a mockup; no validation beyond non-negative.
- **Garage badge coupling.** Showing the due badge on Profile cards couples `ProfileViewModel` to
  `MaintenanceRepository`. Acceptable — it reuses the same pure `dueCount`.

## 11. Scope Boundary & Backlog

**In this spec:** per-vehicle odometer + service log with status/reminders, mark-serviced, editable
odometer, garage due-badges, and an optional in-Start-Ride TCLOCS checklist.

**Deferred — its own brainstorm → spec → build cycle:**
1. **Road segments & scoring** — twisty-road segments + leaderboards, cornering/lean score, scenic
   discovery. (Last of the four niche themes.)
