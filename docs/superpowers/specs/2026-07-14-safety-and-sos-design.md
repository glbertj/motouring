# Motouring — Safety & SOS (Spec 3)

**Date:** 2026-07-14
**Status:** Design — awaiting review
**Track:** Niche rider-community features (second of four; follows Spec 2 Group Ride Mechanics).

## 1. Problem & Goals

Riding has real risk, and the app so far has nothing for when something goes wrong. This spec adds a
rider-specific **safety layer** on top of the existing group-ride substrate — high emotional payoff,
and a natural companion to Spec 2's group mechanics. Three mechanics:

1. **One-tap SOS** — a rider fires an SOS from the in-ride screen (hold-to-send); the app "shares live
   location" with their trusted contacts and alerts the group (simulated).
2. **Crash / fall auto-detection** — a simulated "hard stop" starts a 15-second countdown; if the
   rider doesn't cancel, the app auto-fires an SOS-style alert to the group + trusted contacts.
3. **Escalated "rider in trouble" alert** — when a Spec-2 fallen-behind rider stays lost past a
   threshold, the transient regroup nudge **escalates** into a grave, persistent, actionable safety
   alert — deliberately distinct from the regroup ping.

**Non-goals (this stays a UI/UX mockup):** no backend, no real location sharing, SMS, telephony, or
push; no real crash sensors (the "hard stop" is a debug-triggered simulation — the ride simulator
never actually stops); no real audio/vibration (a haptic/buzz cue is represented visually only); no
global/standalone SOS outside a ride (the whole safety model lives in the ride context for now). The
remaining two backlog themes (Gear & maintenance, Road segments & scoring) stay deferred.

## 2. Design Decisions Already Locked (via brainstorm + visual mockups)

- **Danger-red semantic layer.** Safety gets its own red (`SosRed`, proposed `#FF3B30`), deliberately
  separate from the orange regroup accent, so "something is actually wrong" never reads like a routine
  notification.
- **Trusted contacts = flagged friends.** A rider flags existing friends (`RideBuddy` with
  `BuddyStatus.FRIEND`) as trusted contacts; their initials appear in every alert. Managed in Settings.
- **SOS activation: hold-to-send.** A round red SOS button on the in-ride map; press-and-hold ~2s fills
  a ring, then fires. The hold *is* the confirm — accidental taps do nothing, no dialog. Fastest
  one-handed path in a real emergency.
- **Crash countdown: 15s.** A full-screen red-ring takeover on a detected hard stop. A large neutral
  "I'm OK" cancels everything; hitting 0 (or "Send alert now") fires the alert and drops into the
  persistent **SOS-active** state, which **keeps "sharing location" until the rider taps "I'm safe
  now."**
- **Escalation reuses Spec-2 state.** The "rider in trouble" alert is derived from Spec 2's
  `sweepDriftMeters` crossing a high threshold — no separate timer. Ignoring a regroup long enough is
  what escalates it; broadcasting regroup resets it.
- **Alerts persist, and feel two-way.** SOS/crash/trouble alerts do not auto-dismiss; a simulated
  "{contact} responding" trickles in after a few seconds.

## 3. Data Model

New file `data/model/SafetyAlert.kt`:

```
enum class SafetyAlertType { SOS, CRASH, RIDER_IN_TROUBLE }
enum class SafetyAlertStatus { ACTIVE, RESOLVED }

data class SafetyAlert(
    val id: String,
    val type: SafetyAlertType,
    val fromUserId: String,
    val fromName: String,
    val notifiedContactNames: List<String>,   // trusted-contact names, plus a "group" marker
    val respondingContactName: String? = null, // simulated ack, filled in after a delay
    val status: SafetyAlertStatus = SafetyAlertStatus.ACTIVE,
    val startedAtSeconds: Long,
)
```

Small additive changes elsewhere (all defaulted, so existing construction sites keep compiling):

- `RideBuddy` gains `isTrustedContact: Boolean = false`.
- `NotificationType` gains a `SAFETY` case.
- `RideSessionEvent` (sealed interface) gains `HardStopDetected` (object) and
  `RiderInTrouble(participant: RideParticipantState)`.
- New color token `SosRed` (`#FF3B30`) in `ui/theme/Color.kt`, exposed via `MotouringColors.sos`.

## 4. Simulator & Pure Logic

**`RideSimulator` (`simulation/RideSimulator.kt`):**

- New driver `simulateHardStop()` — a debug trigger (mirrors Spec 2's `forceSweepBehind`) that emits
  `RideSessionEvent.HardStopDetected`. The speed model is untouched; the "crash" is entirely simulated.
- **Escalation in the tick loop:** the existing `start()` loop already watches per-participant
  transitions. Add: when a participant's `sweepDriftMeters`-derived "how far behind" crosses
  `IN_TROUBLE_THRESHOLD_METERS` (~700m, below the 800m drift cap), emit `RiderInTrouble(participant)`
  **exactly once** on the crossing (a second flag or a `wasInTrouble` comparison, same false→true
  edge pattern as `RiderFellBehind`). `broadcastRegroup()` / `callFuel()` reset drift and therefore
  clear the escalation.

**New pure helpers `simulation/SafetyCalculations.kt` (TDD'd):**

- `buildSafetyAlert(type, self: RideParticipantState, trustedContactNames: List<String>, startedAtSeconds): SafetyAlert`
  — assembles the alert, listing the trusted-contact names (plus the group marker).
- `isRiderInTrouble(sweepDriftMeters: Double): Boolean` — the escalation predicate (drift ≥ threshold).
- `resolve(alert: SafetyAlert): SafetyAlert` — returns the alert with `status = RESOLVED`.

Keeping alert assembly and the escalation predicate pure means the simulator/ViewModel wiring stays
thin and the logic is headless-testable.

## 5. ViewModel

`RideSessionViewModel` (`ui/rides/RideSessionViewModel.kt`):

- Injects `RideBuddyRepository` (its `factory` already receives it — thread it into the constructor)
  to resolve `trustedContacts()` at alert time.
- `triggerSos()` — builds a `SafetyAlert(type = SOS, ...)` from self + trusted-contact names, exposes
  it as active-alert state, and records a `SAFETY` `Notification`.
- `simulateHardStop()` — pass-through to the simulator driver (the screen owns the countdown; the VM
  only re-broadcasts the event stream, which already flows through).
- `confirmCrashAlert()` — the crash countdown's timeout/"send now" path; builds a
  `SafetyAlert(type = CRASH, ...)` the same way as `triggerSos`.
- `raiseInTroubleAlert(participant)` — builds a `SafetyAlert(type = RIDER_IN_TROUBLE, ...)` for the
  named rider (driven by the `RiderInTrouble` event).
- `resolveActiveAlert()` — resolves + clears the active alert ("I'm safe now").
- The active alert is exposed as observable state (a `StateFlow<SafetyAlert?>` on the VM, or screen
  `remember` state fed by the event stream — implementation picks the cleaner of the two). A simulated
  `respondingContactName` is set a few seconds after an alert goes active.

## 6. In-Ride UI (`ui/rides/`)

- **`SosButton.kt`** (new) — a round `SosRed` button pinned bottom-right over the map. A
  press-and-hold gesture fills a ring over ~2s (`pointerInput` + an animated progress); completing the
  hold calls `onFire`. Releasing early cancels. No confirmation dialog.
- **`CrashCountdownOverlay.kt`** (new) — a full-screen red-tinted takeover shown on `HardStopDetected`:
  a shrinking `SosRed` ring with the seconds remaining, "Are you OK?", a large neutral **"I'm OK"**
  button (cancels), and a smaller "Send alert now" action. A 15s `LaunchedEffect` counts down; reaching
  0 or "Send now" calls `confirmCrashAlert()`; "I'm OK" dismisses. (A small buzz/haptic indicator is
  drawn but no real vibration/audio is used.)
- **`SafetyBanners.kt`** (new) — `SosActiveBanner` (persistent `MotouringCard` in the danger-red layer:
  "SOS active · sharing live location", the sent-to names, a simulated "{contact} responding ✓", and an
  **"I'm safe now"** action) and `RiderInTroubleCard` (persistent red card: "{name} may be in trouble",
  the how-lost detail, and **Locate / Call / Alert group** actions). Both follow the existing
  `RideSessionBanners` `MotouringCard` pattern but read visibly graver than the orange regroup banner.
- **`RideSessionScreen.kt`** — adds the `SosButton` to the map overlay; consumes `HardStopDetected`
  (→ show `CrashCountdownOverlay`) and `RiderInTrouble` (→ raise `RiderInTroubleCard`); holds the
  `activeAlert` and renders `SosActiveBanner` when an alert is active. The debug control row gains a
  "Crash" trigger (calls `simulateHardStop()`), alongside Spec 2's Regroup/Fuel/Behind.

## 7. Trusted-Contacts Management & Notifications

- `RideBuddyRepository` gains `trustedContacts(): List<RideBuddy>` (friends with `isTrustedContact`)
  and `setTrusted(userId: String, trusted: Boolean)`.
- A **trusted-contacts UI** in Settings: either a section in `SettingsScreen` or a small
  `TrustedContactsScreen` reached from it, listing friends with a `Switch` to flag each as trusted
  (implementation picks whichever fits the existing Settings structure).
- Firing any alert records a `Notification` with `type = SAFETY` so it also appears in the existing
  notifications list.
- `FakeDataProvider` pre-flags 1–2 friends (e.g. Dinda, Bagas) as trusted so the flow demos with real
  names/initials out of the box.

## 8. File-by-File Summary

| File | Change |
| --- | --- |
| `data/model/SafetyAlert.kt` (new) | `SafetyAlertType`, `SafetyAlertStatus`, `SafetyAlert` |
| `data/model/RideBuddy.kt` | `+ isTrustedContact` |
| `data/model/Notification.kt` | `NotificationType.SAFETY` |
| `data/model/RideSession.kt` | `RideSessionEvent.HardStopDetected` / `.RiderInTrouble` |
| `ui/theme/Color.kt` | `SosRed` + `MotouringColors.sos` |
| `simulation/RideSimulator.kt` | `simulateHardStop()`, `RiderInTrouble` escalation emit |
| `simulation/SafetyCalculations.kt` (new) | pure `buildSafetyAlert` / `isRiderInTrouble` / `resolve` |
| `data/repository/RideBuddyRepository.kt` | `trustedContacts()`, `setTrusted()` |
| `ui/rides/RideSessionViewModel.kt` | alert drivers, `RideBuddyRepository` injection, `SAFETY` notification |
| `ui/rides/SosButton.kt` (new) | hold-to-send SOS control |
| `ui/rides/CrashCountdownOverlay.kt` (new) | 15s crash countdown takeover |
| `ui/rides/SafetyBanners.kt` (new) | `SosActiveBanner`, `RiderInTroubleCard` |
| `ui/rides/RideSessionScreen.kt` | wire button, overlay, banners, events, Crash debug trigger |
| `ui/profile/SettingsScreen.kt` (+ maybe `TrustedContactsScreen.kt`) | flag friends as trusted |
| `data/fake/FakeDataProvider.kt` | pre-flag trusted friends |

## 9. Testing & Verification

- **TDD the pure logic** (headless): `isRiderInTrouble` threshold (true at/above, false below);
  `buildSafetyAlert` lists the right contact names + type; `resolve` flips to `RESOLVED`;
  `trustedContacts()` filters correctly. Simulator: `simulateHardStop()` emits `HardStopDetected`;
  `RiderInTrouble` fires exactly once when drift crosses `IN_TROUBLE_THRESHOLD_METERS` and not again
  while remaining over it. Keep all existing tests green.
- **On-device visual review by the user** on the Arch host is the primary UI gate — the SOS hold
  gesture, the crash overlay, and the banners can't be seen on the headless VM.
- Build stays green headless: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.

## 10. Risks / Open Questions

- **Escalation coupling to Spec 2.** Reusing `sweepDriftMeters` means only the Sweep (rear rider) can
  escalate to "in trouble," and a regroup resets it. Acceptable and intentional for a mockup; if a
  future spec wants any rider to escalate, that's a per-rider "time behind" field then.
- **Countdown while backgrounded.** The 15s countdown is a UI `LaunchedEffect`; on a real device
  leaving the screen would pause it. Fine for a reviewed mockup.
- **Danger-red vs the map's route color.** `SosRed` (#FF3B30) sits near the orange route/`poiRepair`
  hue; on-device tuning may be needed so the SOS button and overlay clearly separate from the map.

## 11. Scope Boundary & Backlog

**In this spec:** one-tap hold-to-send SOS, crash/fall auto-detection countdown, escalated
rider-in-trouble alert, trusted-contacts management, SAFETY notifications.

**Deferred — each its own brainstorm → spec → build cycle, in order:**
1. **Gear & maintenance** — per-vehicle service log with mileage reminders, pre-ride checklist.
2. **Road segments & scoring** — twisty-road segments + leaderboards, cornering/lean score, scenic
   discovery.
