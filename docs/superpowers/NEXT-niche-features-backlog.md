# Motouring — Next Features Backlog (niche rider-community themes)

**Purpose:** Hand-off doc so a fresh Claude Code session can pick up the next work with zero re-explaining.
**Last updated:** 2026-07-15, after Spec 4 (Gear & Maintenance) shipped to `origin/main`.

---

## TL;DR — how to resume

Open a fresh session in `/home/valid/projects/motouring` and paste:

> Read `docs/superpowers/NEXT-niche-features-backlog.md`. Let's build the next theme: **Road segments & scoring** (the last of the four). Brainstorm it first (use the visual companion for layout questions), then spec → plan → subagent-driven build with Sonnet 5 doing the coding, direct-to-main like last time.

That's it. Swap the theme name when you get to later ones.

---

## Where the project is now

Motouring is a **UI/UX-only mockup** Android app (Kotlin/Compose, Material3, MVVM, in-memory fake data — no backend/network/auth) for a moto/car "ride together" social app. Built by Gilbert (Valid).

Six bodies of work are DONE and on `main`:
1. **"Analog Dash" design system** — charcoal instrument-cluster palette, `InstrumentRing` gauge, Space Grotesk / Inter / IBM Plex Mono type, spring motion. Colors live in `ui/theme/Color.kt` (`MotouringColors`: `poiFuel/poiRepair/poiRest/rider/riderPurple/riderCoral/speaking/goal` + charcoal ramp + `AccentPrimary` orange).
2. **Goal-vs-Endless ride modes** — the in-ride goal/drift/celebration flow (already shipped).
3. **Map & Visual Overhaul (Spec 1)** — MapLibre GL + OpenFreeMap token-free dark maps behind reusable `MotouringMap` (`ui/components/map/`), Strava-style center Start-Ride FAB + quick menu, balanced-split in-ride screen (map + 6-stat dashboard + group/voice bar), Nearby (full-map + draggable sheet, tap-to-recenter), bundled CC0 photos. Spec + plan in `docs/superpowers/`.
4. **Group Ride Mechanics (Spec 2)** — rider roles (Lead/Sweep/Rider, auto + reassignable via `RiderRole` + `assignInitialRoles`/`withRole`), a moving pack (`RideSimulator.advance()` moves every participant, Sweep drifts behind via `sweepDriftMeters`/`isRegrouping`), in-ride Stats/Pack toggle + formation list (`RideDashboard`), role-colored map markers + self ring (`MarkerStyle` LEAD/SWEEP/RIDER/BEHIND + `MapMarker.isSelf`), regroup ping (auto `RiderFellBehind` event + manual broadcast that tightens the pack) and fuel call (nearest-`GAS_STATION` rally + banner). Pure logic in `simulation/GroupRideCalculations.kt`. Spec + plan in `docs/superpowers/`. **On-device visual pass still pending on the Arch host.**
5. **Safety & SOS (Spec 3)** — hold-to-send `SosButton`, crash/fall auto-detection (`simulateHardStop` → `HardStopDetected` → 15s `CrashCountdownOverlay` → auto-alert), an escalated `RiderInTrouble` alert (reuses Spec 2 `sweepDriftMeters` crossing `IN_TROUBLE_THRESHOLD_METERS`=700), trusted contacts = flagged friends (`RideBuddy.isTrustedContact`, managed in `TrustedContactsScreen` off Settings), `SAFETY` notifications, and a danger-red layer (`MotouringColors.sos` = `#FF3B30`) kept distinct from the orange regroup. `SafetyAlert` model + `activeAlert: StateFlow` on `RideSessionViewModel` (self SOS/crash outranks a buddy in-trouble); pure logic in `simulation/SafetyCalculations.kt`. Spec + plan in `docs/superpowers/`. **On-device visual pass still pending on the Arch host.** Optional follow-ups logged (highest: a11y `semantics` on `SosButton`).
6. **Gear & Maintenance (Spec 4)** — per-vehicle odometer (`Vehicle.odometerKm`, seeded) + a service log with mileage-based OK/due-soon/overdue status (pure `simulation/MaintenanceCalculations.kt`: `serviceStatus`/`dueCount`, 0.85 due-soon fraction), one-tap mark-serviced + editable odometer (`MaintenanceRepository`, `VehicleRepository.setOdometer`), a `VehicleMaintenanceScreen` reached from Profile → My Garage (cards gain a "N due" badge, `ProfileViewModel.dueCounts`), and an optional advisory in-Start-Ride TCLOCS `PreRideChecklist` (ephemeral, never blocks Start). Semantic status colors (`MotouringColors.statusOk/statusDueSoon/statusOverdue`, aliases of green/amber/red). Spec + plan in `docs/superpowers/`. **On-device visual pass still pending on the Arch host.** Follow-up flagged: the "N due" badge uses red for any due count (due-soon-only vehicle reads red vs its amber row chip) — a design-taste call for on-device review.

**Reusable pieces the next features can lean on:**
- `MotouringMap(cameraTarget, markers, polyline, onMarkerClick, modifier)` with `MapCamera`/`MapMarker`/`MapPolyline`/`MarkerStyle`.
- `RideSession` / `RideParticipantState` (has `position`, `isSpeaking`, `name`, `avatarRes`) + `RideSimulator` (ticks telemetry) — the substrate for group-ride features.
- Vehicle garage (`VehicleRepository`, `Vehicle` with type/photo), badges/challenges, POI (`PoiType` incl. `REST_STOP`, `PoiRepository`), `FakeDataProvider` for all mock content, `AppContainer` manual DI.

## Working agreement (same as Spec 1)

- **Workflow:** `superpowers:brainstorming` (offer the visual companion for real layout questions) → `writing-plans` → `subagent-driven-development`. One theme = one spec/plan/build cycle.
- **Coding model:** Sonnet 5 for implementer + reviewer subagents; the most-capable model for the final whole-branch review.
- **Git:** direct-to-`main`, no branch/PR (documented project norm); push after each task so the Arch host can pull. VM is headless — Android Studio / device review happens on the host (MapLibre won't render on the VM; map tiles need device network).
- **Scope discipline:** each theme is decomposed so tasks are independently testable; TDD the pure logic, verify UI on-device.

---

## The four themes (sequenced — build top to bottom)

### 1. Group ride mechanics  ✅ DONE (Spec 2, shipped 2026-07-12)
Rider roles, moving-pack formation view (Stats/Pack toggle), living regroup ping, and fuel call
(reframed from "fuel-stop vote" → broadcast-and-rally, since one empty tank strands everyone). See
`docs/superpowers/specs/2026-07-11-group-ride-mechanics-design.md`. On-device pass pending.

### 2. Safety & SOS  ✅ DONE (Spec 3, shipped 2026-07-14)
Hold-to-send SOS, crash/fall auto-detection countdown, and an escalated "rider in trouble" alert
(reuses Spec 2 drift; distinct from the transient regroup). Trusted contacts = flagged friends. See
`docs/superpowers/specs/2026-07-14-safety-and-sos-design.md`. On-device pass pending.

### 3. Gear & maintenance  ✅ DONE (Spec 4, shipped 2026-07-15)
Per-vehicle service log + mileage reminders (odometer, OK/due-soon/overdue), one-tap mark-serviced,
garage due-badges + tap-to-open maintenance screen, and an optional in-Start-Ride TCLOCS checklist. See
`docs/superpowers/specs/2026-07-15-gear-and-maintenance-design.md`. On-device pass pending.

### 4. Road segments & scoring  ← DO THIS NEXT (last of the four)
- **Twisty-road "segments"** with leaderboards (Strava-segment style).
- **Cornering / lean score** per ride.
- **Scenic-route discovery.**
- Extends ride tracking + badges.

---

## Notes / open threads carried over from Spec 1 (optional polish, not blocking)

- Freshly-*completed* rides still show `ic_route_preview_placeholder` while seeded rides show photos — the **route map-snapshot** replacement was deferred (could be a small standalone task: render a MapLibre static snapshot of the route for `RideHistoryEntry.routePreviewRes`).
- Newly-created posts (`PostViewModel`) still use `ic_photo_placeholder`.
- A handful of cosmetic minors are logged in `.superpowers/sdd/progress.md` (gitignored, VM-local) for triage if ever revisited.
