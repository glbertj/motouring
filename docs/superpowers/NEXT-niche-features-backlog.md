# Motouring — Next Features Backlog (niche rider-community themes)

**Purpose:** Hand-off doc so a fresh Claude Code session can pick up the next work with zero re-explaining.
**Last updated:** 2026-07-11, after Spec 1 (Map & Visual Overhaul) shipped to `origin/main`.

---

## TL;DR — how to resume

Open a fresh session in `/home/valid/projects/motouring` and paste:

> Read `docs/superpowers/NEXT-niche-features-backlog.md`. Let's build the next theme: **Group ride mechanics**. Brainstorm it first (use the visual companion for layout questions), then spec → plan → subagent-driven build with Sonnet 5 doing the coding, direct-to-main like last time.

That's it. Swap the theme name when you get to later ones.

---

## Where the project is now

Motouring is a **UI/UX-only mockup** Android app (Kotlin/Compose, Material3, MVVM, in-memory fake data — no backend/network/auth) for a moto/car "ride together" social app. Built by Gilbert (Valid).

Three bodies of work are DONE and on `main`:
1. **"Analog Dash" design system** — charcoal instrument-cluster palette, `InstrumentRing` gauge, Space Grotesk / Inter / IBM Plex Mono type, spring motion. Colors live in `ui/theme/Color.kt` (`MotouringColors`: `poiFuel/poiRepair/poiRest/rider/riderPurple/riderCoral/speaking/goal` + charcoal ramp + `AccentPrimary` orange).
2. **Goal-vs-Endless ride modes** — the in-ride goal/drift/celebration flow (already shipped).
3. **Map & Visual Overhaul (Spec 1)** — MapLibre GL + OpenFreeMap token-free dark maps behind reusable `MotouringMap` (`ui/components/map/`), Strava-style center Start-Ride FAB + quick menu, balanced-split in-ride screen (map + 6-stat dashboard + group/voice bar), Nearby (full-map + draggable sheet, tap-to-recenter), bundled CC0 photos. Spec + plan in `docs/superpowers/`.

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

### 1. Group ride mechanics  ← DO THIS NEXT (highest synergy)
Builds directly on the existing group ride + `isSpeaking` voice sim + `MotouringMap`.
- **Rider roles** — assign Lead / Sweep (tail) / regular; show role on each participant.
- **"Regroup — wait for me" ping** — a rider signals they've fallen behind; the group gets a banner/toast.
- **Pack / formation view** — who's where in the group (order, gaps) — could ride on the in-ride map with role-colored markers.
- **Fuel-stop vote** — someone proposes a stop (tie into Nearby POIs); others vote yes/no, tally shown.
- Likely touches: `RideSession`/`RideParticipantState` (+role, +behind flag), the in-ride screen, `RideSimulator`, a small vote/ping model.

### 2. Safety & SOS
- **One-tap SOS** — share live location with a trusted contact (simulated).
- **Crash/fall detection** — simulated auto-alert to the group after a "hard stop."
- **"Rider fell behind" group alert** — distinct from the manual regroup ping.
- High emotional payoff, very rider-specific.

### 3. Gear & maintenance
- **Per-vehicle service log** — oil / chain / tires, with mileage-based reminders. Extends the vehicle garage.
- **Pre-ride safety checklist** (e.g. TCLOCS-style).

### 4. Road segments & scoring
- **Twisty-road "segments"** with leaderboards (Strava-segment style).
- **Cornering / lean score** per ride.
- **Scenic-route discovery.**
- Extends ride tracking + badges.

---

## Notes / open threads carried over from Spec 1 (optional polish, not blocking)

- Freshly-*completed* rides still show `ic_route_preview_placeholder` while seeded rides show photos — the **route map-snapshot** replacement was deferred (could be a small standalone task: render a MapLibre static snapshot of the route for `RideHistoryEntry.routePreviewRes`).
- Newly-created posts (`PostViewModel`) still use `ic_photo_placeholder`.
- A handful of cosmetic minors are logged in `.superpowers/sdd/progress.md` (gitignored, VM-local) for triage if ever revisited.
