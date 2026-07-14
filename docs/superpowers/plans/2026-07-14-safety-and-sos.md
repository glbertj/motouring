# Safety & SOS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a rider-specific safety layer to the in-ride experience — one-tap hold-to-send SOS, crash/fall auto-detection countdown, and an escalated "rider in trouble" alert — plus trusted-contacts management and SAFETY notifications.

**Architecture:** A pure `SafetyAlert` model + pure helpers (`SafetyCalculations`, TDD'd headless). `RideSimulator` gains a debug hard-stop driver, a debug in-trouble driver, and an organic in-trouble escalation when the Spec-2 sweep drift crosses a threshold. `RideSessionViewModel` owns an `activeAlert: StateFlow<SafetyAlert?>`, builds alerts from trusted contacts, and records SAFETY notifications. The in-ride screen gains a hold-to-send SOS button, a crash countdown overlay, and persistent safety banners in a new danger-red layer. Trusted contacts are flagged friends, managed in a small screen off Settings.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), MVVM, kotlinx-coroutines, MapLibre, JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies.** Everything uses libraries already on the classpath.
- **Direct-to-`main`, push after every task** (documented project norm; no branch/PR). Each commit message ends with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` run on this VM. The SOS hold gesture, crash overlay, and banners are verified **on-device by the user** — they can't be seen on the headless VM.
- **Additive model changes only:** every new data-class field / enum case gets a default or is additive, so existing construction sites and tests keep compiling.
- **Danger-red is its own layer:** SOS/crash/in-trouble UI uses `MotouringColors.sos` (`SosRed` `#FF3B30`), never the orange regroup accent.
- **`PoiType` for fuel is `GAS_STATION`** (unchanged; only relevant where reused).
- **Escalation reuses Spec-2 state:** the in-trouble threshold is `IN_TROUBLE_THRESHOLD_METERS = 700.0`, below Spec 2's `SWEEP_DRIFT_MAX = 800.0`.

## File Structure

| File | Responsibility |
| --- | --- |
| `data/model/SafetyAlert.kt` (create) | `SafetyAlertType`, `SafetyAlertStatus`, `SafetyAlert` |
| `data/model/RideBuddy.kt` (modify) | `+ isTrustedContact` |
| `data/model/Notification.kt` (modify) | `NotificationType.SAFETY` |
| `data/model/RideSession.kt` (modify) | `RideSessionEvent.HardStopDetected` / `.RiderInTrouble` |
| `ui/theme/Color.kt` (modify) | `SosRed` + `MotouringColors.sos` |
| `simulation/SafetyCalculations.kt` (create) | pure `isRiderInTrouble`, `buildSafetyAlert` |
| `simulation/RideSimulator.kt` (modify) | `simulateHardStop()`, `simulateRiderInTrouble()`, organic in-trouble emit |
| `data/repository/RideBuddyRepository.kt` (modify) | `trustedContacts()`, `setTrusted()` |
| `data/repository/NotificationRepository.kt` (modify) | `add()` |
| `ui/rides/RideSessionViewModel.kt` (modify) | alert drivers, repo injection, SAFETY notification |
| `ui/rides/SosButton.kt` (create) | hold-to-send SOS control |
| `ui/rides/CrashCountdownOverlay.kt` (create) | 15s crash countdown takeover |
| `ui/rides/SafetyBanners.kt` (create) | `SosActiveBanner`, `RiderInTroubleCard` |
| `ui/rides/RideSessionScreen.kt` + `RideSessionHud.kt` (modify) | wire button, overlay, banners, events, debug triggers |
| `ui/profile/TrustedContactsScreen.kt` + `TrustedContactsViewModel.kt` (create), `SettingsScreen.kt` + nav (modify) | flag friends as trusted |
| `data/fake/FakeDataProvider.kt` (modify) | pre-flag trusted friends |

Command shorthand:
- Single test class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- Full suite: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Model — SafetyAlert, trusted flag, SAFETY notif, events, color

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/model/SafetyAlert.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideBuddy.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/model/Notification.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideSession.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt` (add no-op event branches; filled in Task 8)
- Test: `app/src/test/java/com/valid/motouring/data/model/SafetyAlertTest.kt` (create)

**Interfaces:**
- Produces: `enum SafetyAlertType { SOS, CRASH, RIDER_IN_TROUBLE }`, `enum SafetyAlertStatus { ACTIVE, RESOLVED }`, `data class SafetyAlert(id, type, fromUserId, fromName, notifiedContactNames: List<String>, respondingContactName: String? = null, status = ACTIVE, startedAtSeconds: Long)`; `RideBuddy.isTrustedContact: Boolean`; `NotificationType.SAFETY`; `RideSessionEvent.HardStopDetected` (object) + `.RiderInTrouble(participant)`; `MotouringColors.sos`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/model/SafetyAlertTest.kt`:

```kotlin
package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafetyAlertTest {

    @Test
    fun `safety alert defaults to active with no responder`() {
        val a = SafetyAlert("a1", SafetyAlertType.SOS, "u-me", "Rafi", listOf("Dinda"), startedAtSeconds = 100L)
        assertEquals(SafetyAlertStatus.ACTIVE, a.status)
        assertNull(a.respondingContactName)
        assertEquals(listOf("Dinda"), a.notifiedContactNames)
    }

    @Test
    fun `ride buddy defaults to not a trusted contact`() {
        val buddy = RideBuddy(User("u-2", "Dinda", 0, emptyList()), BuddyStatus.FRIEND)
        assertEquals(false, buddy.isTrustedContact)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.SafetyAlertTest"`
Expected: FAIL — `SafetyAlert` / `isTrustedContact` unresolved.

- [ ] **Step 3: Create the SafetyAlert model**

Create `app/src/main/java/com/valid/motouring/data/model/SafetyAlert.kt`:

```kotlin
package com.valid.motouring.data.model

enum class SafetyAlertType { SOS, CRASH, RIDER_IN_TROUBLE }

enum class SafetyAlertStatus { ACTIVE, RESOLVED }

data class SafetyAlert(
    val id: String,
    val type: SafetyAlertType,
    val fromUserId: String,
    val fromName: String,
    val notifiedContactNames: List<String>,
    val respondingContactName: String? = null,
    val status: SafetyAlertStatus = SafetyAlertStatus.ACTIVE,
    val startedAtSeconds: Long,
)
```

- [ ] **Step 4: Add the trusted flag**

In `RideBuddy.kt`, add the field:

```kotlin
data class RideBuddy(
    val user: User,
    val status: BuddyStatus,
    val isTrustedContact: Boolean = false,
)
```

- [ ] **Step 5: Add the SAFETY notification type**

In `Notification.kt`, extend the enum:

```kotlin
enum class NotificationType { RIDE_INVITE, BADGE_EARNED, CHALLENGE_PROGRESS, SOCIAL, SAFETY }
```

- [ ] **Step 6: Add the two events**

In `RideSession.kt`, add to the `RideSessionEvent` sealed interface (after `GroupSignalRaised`):

```kotlin
sealed interface RideSessionEvent {
    data class GoalReached(val leg: Leg) : RideSessionEvent
    object DriftedToEndless : RideSessionEvent
    data class RiderFellBehind(val participant: RideParticipantState) : RideSessionEvent
    data class GroupSignalRaised(val signal: GroupSignal) : RideSessionEvent
    object HardStopDetected : RideSessionEvent
    data class RiderInTrouble(val participant: RideParticipantState) : RideSessionEvent
}
```

- [ ] **Step 7: Add the SOS color**

In `Color.kt`, add the raw color after `RiderCoral` (line ~22):

```kotlin
val SosRed = Color(0xFFFF3B30)      // danger — SOS / crash / in-trouble
```

And add to the `MotouringColors` object (after `val goal = AccentPrimary`):

```kotlin
    val goal = AccentPrimary
    val sos = SosRed
```

- [ ] **Step 8: Keep `RideSessionScreen` compiling**

In `RideSessionScreen.kt`, the event `when` (currently 4 branches). Add no-op branches for the two new events (Task 8 fills them in):

```kotlin
                is RideSessionEvent.GroupSignalRaised -> when (event.signal.type) {
                    GroupSignalType.REGROUP -> regroupMessage = "Regroup — wait for me"
                    GroupSignalType.FUEL -> {
                        fuelSignal = event.signal
                        rallyPoi = event.signal.rallyPoi
                    }
                }
                RideSessionEvent.HardStopDetected -> {}
                is RideSessionEvent.RiderInTrouble -> {}
```

- [ ] **Step 9: Run the test + full build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.SafetyAlertTest"`
Expected: PASS (2 tests).

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still pass.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/SafetyAlert.kt \
        app/src/main/java/com/valid/motouring/data/model/RideBuddy.kt \
        app/src/main/java/com/valid/motouring/data/model/Notification.kt \
        app/src/main/java/com/valid/motouring/data/model/RideSession.kt \
        app/src/main/java/com/valid/motouring/ui/theme/Color.kt \
        app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt \
        app/src/test/java/com/valid/motouring/data/model/SafetyAlertTest.kt
git commit -m "feat(safety): SafetyAlert model, trusted flag, SAFETY notif, events, SOS color

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Pure safety calculations

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/SafetyCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/SafetyCalculationsTest.kt`

**Interfaces:**
- Consumes: `SafetyAlert`, `SafetyAlertType` (Task 1).
- Produces: `const val IN_TROUBLE_THRESHOLD_METERS = 700.0`; `fun isRiderInTrouble(sweepDriftMeters: Double): Boolean`; `fun buildSafetyAlert(id: String, type: SafetyAlertType, fromUserId: String, fromName: String, trustedContactNames: List<String>, startedAtSeconds: Long): SafetyAlert`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/SafetyCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.SafetyAlertStatus
import com.valid.motouring.data.model.SafetyAlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCalculationsTest {

    @Test
    fun `isRiderInTrouble is true at or above the threshold and false below`() {
        assertFalse(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS - 1))
        assertTrue(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS))
        assertTrue(isRiderInTrouble(IN_TROUBLE_THRESHOLD_METERS + 50))
    }

    @Test
    fun `buildSafetyAlert lists contacts, sets type, and starts ACTIVE with no responder`() {
        val alert = buildSafetyAlert(
            id = "s1",
            type = SafetyAlertType.SOS,
            fromUserId = "u-me",
            fromName = "Rafi",
            trustedContactNames = listOf("Dinda", "Bagas"),
            startedAtSeconds = 500L,
        )
        assertEquals(SafetyAlertType.SOS, alert.type)
        assertEquals(listOf("Dinda", "Bagas"), alert.notifiedContactNames)
        assertEquals(SafetyAlertStatus.ACTIVE, alert.status)
        assertEquals(null, alert.respondingContactName)
        assertEquals("Rafi", alert.fromName)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.SafetyCalculationsTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the helpers**

Create `app/src/main/java/com/valid/motouring/simulation/SafetyCalculations.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType

/** How far behind (metres of sweep drift) a rider must be before the transient regroup escalates to a safety alert. */
const val IN_TROUBLE_THRESHOLD_METERS = 700.0

fun isRiderInTrouble(sweepDriftMeters: Double): Boolean = sweepDriftMeters >= IN_TROUBLE_THRESHOLD_METERS

/** Single assembly point for every safety alert (SOS / crash / in-trouble), so the three drivers can't drift apart. */
fun buildSafetyAlert(
    id: String,
    type: SafetyAlertType,
    fromUserId: String,
    fromName: String,
    trustedContactNames: List<String>,
    startedAtSeconds: Long,
): SafetyAlert = SafetyAlert(
    id = id,
    type = type,
    fromUserId = fromUserId,
    fromName = fromName,
    notifiedContactNames = trustedContactNames,
    startedAtSeconds = startedAtSeconds,
)
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.SafetyCalculationsTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/SafetyCalculations.kt \
        app/src/test/java/com/valid/motouring/simulation/SafetyCalculationsTest.kt
git commit -m "feat(safety): pure in-trouble threshold + alert builder

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Simulator — hard-stop driver, in-trouble driver + organic escalation

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorSafetyTest.kt` (create)

**Interfaces:**
- Consumes: `isRiderInTrouble` (Task 2), `RideSessionEvent.HardStopDetected` / `.RiderInTrouble` (Task 1).
- Produces: `fun simulateHardStop()`, `fun simulateRiderInTrouble()`; organic `RiderInTrouble` emission in `start()` when `sweepDriftMeters` crosses `IN_TROUBLE_THRESHOLD_METERS`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/RideSimulatorSafetyTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorSafetyTest {

    private val route = listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.1875, 106.8271))

    private fun groupSession() = RideSession(
        id = "s",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = assignInitialRoles(
            listOf(
                RideParticipantState("u-me", "Rafi", 0, route.first()),
                RideParticipantState("u-2", "Dinda", 0, route.first()),
                RideParticipantState("u-3", "Bagas", 0, route.first()),
            ),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `simulateHardStop emits HardStopDetected`() = runTest {
        val sim = RideSimulator(this, groupSession())
        val event = async { sim.events.first() }
        sim.simulateHardStop()
        assertEquals(RideSessionEvent.HardStopDetected, event.await())
    }

    @Test
    fun `simulateRiderInTrouble emits RiderInTrouble for the sweep`() = runTest {
        val sim = RideSimulator(this, groupSession())
        val event = async { sim.events.first() }
        sim.simulateRiderInTrouble()
        val e = event.await()
        assertTrue(e is RideSessionEvent.RiderInTrouble)
        assertEquals("Bagas", (e as RideSessionEvent.RiderInTrouble).participant.name)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorSafetyTest"`
Expected: FAIL — `simulateHardStop` / `simulateRiderInTrouble` unresolved.

- [ ] **Step 3: Add the imports + driver methods**

In `RideSimulator.kt`, add the import (alongside the existing `com.valid.motouring.data.model.*` imports — `isRiderInTrouble` is in the same `simulation` package, so no import needed for it):

(no new import line required; `RideSessionEvent` is already imported.)

Add the two drivers after `forceSweepBehind()` (~line 133), before `stop()`:

```kotlin
    fun simulateHardStop() {
        if (_session.value.status == RideSessionStatus.ENDED) return
        scope.launch { _events.emit(RideSessionEvent.HardStopDetected) }
    }

    /** Debug: raise the escalated in-trouble alert for the sweep (rear rider) on demand. */
    fun simulateRiderInTrouble() {
        val current = _session.value
        if (current.status == RideSessionStatus.ENDED) return
        val sweep = current.participants.lastOrNull() ?: return
        if (current.participants.size <= 1) return
        scope.launch { _events.emit(RideSessionEvent.RiderInTrouble(sweep)) }
    }
```

- [ ] **Step 4: Add the organic escalation to `start()`**

In `start()`, inside the tick loop, after the existing `RiderFellBehind` emission block (the `next.participants.forEachIndexed { ... }` block, ~lines 55-60) and before the `completedLegs` block, add:

```kotlin
                if (next.participants.size > 1 &&
                    isRiderInTrouble(next.sweepDriftMeters) && !isRiderInTrouble(previous.sweepDriftMeters)
                ) {
                    next.participants.lastOrNull()?.let { _events.emit(RideSessionEvent.RiderInTrouble(it)) }
                }
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorSafetyTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Confirm no simulator regressions**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.*"`
Expected: PASS — including the Spec-2 `RideSimulatorGroupTest` / `RideSimulatorDriversTest` and the original simulator suites.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt \
        app/src/test/java/com/valid/motouring/simulation/RideSimulatorSafetyTest.kt
git commit -m "feat(safety): simulator hard-stop + in-trouble drivers and organic escalation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Repositories — trusted contacts + notification add

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/repository/RideBuddyRepository.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/repository/NotificationRepository.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/RideBuddyRepositoryTest.kt` (create)

**Interfaces:**
- Produces: `RideBuddyRepository.trustedContacts(): List<RideBuddy>`, `.setTrusted(userId: String, trusted: Boolean)`; `NotificationRepository.add(notification: Notification)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/repository/RideBuddyRepositoryTest.kt`:

```kotlin
package com.valid.motouring.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideBuddyRepositoryTest {

    // Pick a friend that is NOT already flagged, so this is robust to FakeDataProvider
    // pre-flagging some friends (Task 10) — there is always at least one un-flagged friend.
    @Test
    fun `setTrusted flips a friend's membership in trustedContacts, filtered to flagged only`() {
        val repo = RideBuddyRepository()
        val friend = repo.friends().first { !it.isTrustedContact }
        assertFalse(repo.trustedContacts().any { it.user.id == friend.user.id })
        repo.setTrusted(friend.user.id, true)
        assertTrue(repo.trustedContacts().any { it.user.id == friend.user.id })
        assertTrue(repo.trustedContacts().all { it.isTrustedContact })
        repo.setTrusted(friend.user.id, false)
        assertFalse(repo.trustedContacts().any { it.user.id == friend.user.id })
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.RideBuddyRepositoryTest"`
Expected: FAIL — `trustedContacts` / `setTrusted` unresolved.

- [ ] **Step 3: Extend `RideBuddyRepository`**

Add to `RideBuddyRepository.kt` (after `friends()`):

```kotlin
    fun trustedContacts(): List<RideBuddy> =
        buddies.value.filter { it.status == BuddyStatus.FRIEND && it.isTrustedContact }

    fun setTrusted(userId: String, trusted: Boolean) {
        buddies.value = buddies.value.map {
            if (it.user.id == userId) it.copy(isTrustedContact = trusted) else it
        }
    }
```

- [ ] **Step 4: Extend `NotificationRepository`**

Add to `NotificationRepository.kt` (after `observeNotifications()`):

```kotlin
    fun add(notification: Notification) {
        notifications.value = listOf(notification) + notifications.value
    }
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.RideBuddyRepositoryTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/RideBuddyRepository.kt \
        app/src/main/java/com/valid/motouring/data/repository/NotificationRepository.kt \
        app/src/test/java/com/valid/motouring/data/repository/RideBuddyRepositoryTest.kt
git commit -m "feat(safety): trusted-contacts repo methods + notification add

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: ViewModel — alert state, drivers, repo injection

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt` (ride-session factory call — add `notificationRepository`)

**Interfaces:**
- Consumes: `buildSafetyAlert` (Task 2), simulator drivers (Task 3), `RideBuddyRepository.trustedContacts()` / `NotificationRepository.add()` (Task 4), `SafetyAlert*` (Task 1).
- Produces: `RideSessionViewModel.activeAlert: StateFlow<SafetyAlert?>`; `triggerSos()`, `confirmCrashAlert()`, `raiseInTroubleAlert(participant)`, `simulateHardStop()`, `simulateRiderInTrouble()`, `resolveActiveAlert()`. Factory gains `notificationRepository: NotificationRepository`.

Verified by **build + existing tests green** (the pure logic is unit-tested in Tasks 2-4; `RideSessionViewModel` starts a coroutine in `init` and isn't unit-tested in this codebase — matching the existing layout).

- [ ] **Step 1: Add imports + constructor params + alert state**

In `RideSessionViewModel.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.Notification
import com.valid.motouring.data.model.NotificationType
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType
import com.valid.motouring.data.repository.NotificationRepository
import com.valid.motouring.simulation.buildSafetyAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
```

(`RideParticipantState` and `RideBuddyRepository` are already imported at the top of this file — do not re-add them. `viewModelScope` is already imported too.)

Change the constructor (lines 32-37) to add both repos:

```kotlin
class RideSessionViewModel(
    initialSession: RideSession,
    private val rideRepository: RideRepository,
    private val badgeRepository: BadgeRepository,
    private val poiRepository: PoiRepository,
    private val rideBuddyRepository: RideBuddyRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
```

Add the alert state after `val events` (line 42):

```kotlin
    private val _activeAlert = MutableStateFlow<SafetyAlert?>(null)
    val activeAlert: StateFlow<SafetyAlert?> = _activeAlert.asStateFlow()
```

- [ ] **Step 2: Add the alert drivers**

After `callFuel()` (line 62), add:

```kotlin
    fun triggerSos() = raiseSelfAlert(SafetyAlertType.SOS)

    fun confirmCrashAlert() = raiseSelfAlert(SafetyAlertType.CRASH)

    fun simulateHardStop() = simulator.simulateHardStop()

    fun simulateRiderInTrouble() = simulator.simulateRiderInTrouble()

    fun raiseInTroubleAlert(participant: RideParticipantState) {
        val alert = buildSafetyAlert(
            id = "sa-${System.currentTimeMillis()}",
            type = SafetyAlertType.RIDER_IN_TROUBLE,
            fromUserId = participant.userId,
            fromName = participant.name,
            trustedContactNames = trustedNames(),
            startedAtSeconds = System.currentTimeMillis() / 1000,
        )
        _activeAlert.value = alert
    }

    fun resolveActiveAlert() {
        _activeAlert.value = null
    }

    private fun raiseSelfAlert(type: SafetyAlertType) {
        val self = simulator.session.value.participants.firstOrNull() ?: return
        val contacts = trustedNames()
        val alert = buildSafetyAlert(
            id = "sa-${System.currentTimeMillis()}",
            type = type,
            fromUserId = self.userId,
            fromName = self.name,
            trustedContactNames = contacts,
            startedAtSeconds = System.currentTimeMillis() / 1000,
        )
        _activeAlert.value = alert
        val label = if (type == SafetyAlertType.CRASH) "Crash alert" else "SOS"
        val to = if (contacts.isEmpty()) "your group" else contacts.joinToString(", ")
        notificationRepository.add(
            Notification(
                id = alert.id,
                type = NotificationType.SAFETY,
                message = "$label sent to $to",
                createdAtEpochSeconds = alert.startedAtSeconds,
                isRead = false,
            ),
        )
        // Simulate a contact acknowledging so the state feels two-way.
        viewModelScope.launch {
            delay(3_500)
            val current = _activeAlert.value
            if (current?.id == alert.id && contacts.isNotEmpty()) {
                _activeAlert.value = current.copy(respondingContactName = contacts.first())
            }
        }
    }

    private fun trustedNames(): List<String> = rideBuddyRepository.trustedContacts().map { it.user.name }
```

- [ ] **Step 3: Thread the repos through the factory**

In the `factory(...)` signature (lines 84-93), add `notificationRepository` (it already receives `rideBuddyRepository`):

```kotlin
        fun factory(
            vehicleType: VehicleType,
            isGroup: Boolean,
            initialGoal: RideGoal?,
            userRepository: UserRepository,
            rideBuddyRepository: RideBuddyRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
            poiRepository: PoiRepository,
            notificationRepository: NotificationRepository,
        ) = viewModelFactory {
```

Update the constructor call (line 133):

```kotlin
                RideSessionViewModel(initialSession, rideRepository, badgeRepository, poiRepository, rideBuddyRepository, notificationRepository)
```

- [ ] **Step 4: Pass notificationRepository at the nav call site**

In `MotouringNavHost.kt`, in the `RideSessionViewModel.factory(...)` call, add after `poiRepository = appContainer.poiRepository,`:

```kotlin
                    poiRepository = appContainer.poiRepository,
                    notificationRepository = appContainer.notificationRepository,
```

- [ ] **Step 5: Build + full suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat(safety): VM alert state, drivers, trusted-contact + notification wiring

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: SosButton — hold-to-send control

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/SosButton.kt`

**Interfaces:**
- Produces: `@Composable fun SosButton(onFire: () -> Unit, modifier: Modifier = Modifier)`.

UI/interop — verified by build + `@Preview` + on-device.

- [ ] **Step 1: Create the composable**

Create `app/src/main/java/com/valid/motouring/ui/rides/SosButton.kt`:

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.MotouringColors

private const val HOLD_MS = 2000

@Composable
fun SosButton(onFire: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
            if (progress.value >= 1f) onFire()
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MotouringColors.sos)
            .drawBehind {
                if (progress.value > 0f) {
                    val stroke = 5.dp.toPx()
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("SOS", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SosButtonPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SosButton(onFire = {})
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/SosButton.kt
git commit -m "feat(safety): hold-to-send SOS button

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Crash countdown overlay + safety banners

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/CrashCountdownOverlay.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/rides/SafetyBanners.kt`

**Interfaces:**
- Consumes: `SafetyAlert` (Task 1), `MotouringColors.sos`.
- Produces: `@Composable fun CrashCountdownOverlay(onOk: () -> Unit, onSend: () -> Unit, modifier: Modifier = Modifier)`; `@Composable fun SosActiveBanner(alert: SafetyAlert, onSafe: () -> Unit, modifier: Modifier = Modifier)`; `@Composable fun RiderInTroubleCard(alert: SafetyAlert, onResolve: () -> Unit, modifier: Modifier = Modifier)`.

UI — verified by build + `@Preview` + on-device.

- [ ] **Step 1: Create the crash overlay**

Create `app/src/main/java/com/valid/motouring/ui/rides/CrashCountdownOverlay.kt`:

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valid.motouring.ui.theme.MotouringColors
import kotlinx.coroutines.delay

private const val COUNTDOWN_START = 15

@Composable
fun CrashCountdownOverlay(onOk: () -> Unit, onSend: () -> Unit, modifier: Modifier = Modifier) {
    var remaining by remember { mutableIntStateOf(COUNTDOWN_START) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        onSend()
    }
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text("⚠ POSSIBLE CRASH", color = MotouringColors.sos, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text("Are you OK?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Auto-alerting your group & trusted contacts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(28.dp))
            Text("$remaining", color = MotouringColors.sos, fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onOk,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) { Text("I'm OK", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSend) { Text("Send alert now", color = MotouringColors.sos) }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CrashCountdownOverlayPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        CrashCountdownOverlay(onOk = {}, onSend = {})
    }
}
```

- [ ] **Step 2: Create the safety banners**

Create `app/src/main/java/com/valid/motouring/ui/rides/SafetyBanners.kt`:

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

@Composable
fun SosActiveBanner(alert: SafetyAlert, onSafe: () -> Unit, modifier: Modifier = Modifier) {
    val to = if (alert.notifiedContactNames.isEmpty()) "your group" else alert.notifiedContactNames.joinToString(", ")
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("SOS active · sharing live location", color = MotouringColors.sos, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text("Sent to $to", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                alert.respondingContactName?.let {
                    Text("$it responding ✓", color = MotouringColors.speaking, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSafe) { Text("I'm safe now", color = MotouringColors.sos) }
            }
        }
    }
}

@Composable
fun RiderInTroubleCard(alert: SafetyAlert, onResolve: () -> Unit, modifier: Modifier = Modifier) {
    MotouringCard(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("${alert.fromName} may be in trouble", color = MotouringColors.sos, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text("Out of contact · way behind · stopped off-route", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {}) { Text("Locate") }
                TextButton(onClick = {}) { Text("Call") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onResolve) { Text("Dismiss", color = MotouringColors.sos) }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SosActiveBannerPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SosActiveBanner(
            alert = com.valid.motouring.data.model.SafetyAlert(
                "p", com.valid.motouring.data.model.SafetyAlertType.SOS, "u-me", "Rafi",
                listOf("Dinda", "Bagas"), respondingContactName = "Dinda", startedAtSeconds = 0,
            ),
            onSafe = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RiderInTroubleCardPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RiderInTroubleCard(
            alert = com.valid.motouring.data.model.SafetyAlert(
                "p", com.valid.motouring.data.model.SafetyAlertType.RIDER_IN_TROUBLE, "u-3", "Bagas",
                emptyList(), startedAtSeconds = 0,
            ),
            onResolve = {},
        )
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/CrashCountdownOverlay.kt \
        app/src/main/java/com/valid/motouring/ui/rides/SafetyBanners.kt
git commit -m "feat(safety): crash countdown overlay + SOS-active / in-trouble banners

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: In-ride screen integration

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt` (add SOS button overlay)
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt` (events, crash overlay, banners, debug triggers)

**Interfaces:**
- Consumes: `SosButton` (Task 6), `CrashCountdownOverlay` / `SosActiveBanner` / `RiderInTroubleCard` (Task 7), VM alert drivers + `activeAlert` (Task 5), the two new events (Task 1).
- Produces: `RideSessionHud(session, rallyPoi, onSosFire: () -> Unit = {}, modifier)`.

UI — verified by build + on-device.

- [ ] **Step 1: Add the SOS button to the HUD**

In `RideSessionHud.kt`, add imports:

```kotlin
import androidx.compose.foundation.layout.padding
```

(`Alignment`, `Modifier`, `Box`, `dp` are already imported.)

Change the signature to accept the fire callback:

```kotlin
fun RideSessionHud(session: RideSession, rallyPoi: PointOfInterest? = null, onSosFire: () -> Unit = {}, modifier: Modifier = Modifier) {
```

Inside the HUD's root `Box` (after the goal-chip block, before the `Box` closes), add:

```kotlin
        SosButton(
            onFire = onSosFire,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
```

- [ ] **Step 2: Wire the screen — state + events**

In `RideSessionScreen.kt`, add imports:

```kotlin
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType
```

Add state next to the other `remember`s (after line 45):

```kotlin
    val activeAlert by viewModel.activeAlert.collectAsState()
    var crashCountdownActive by remember { mutableStateOf(false) }
```

Replace the Task-1 no-op branches with real handling:

```kotlin
                RideSessionEvent.HardStopDetected -> crashCountdownActive = true
                is RideSessionEvent.RiderInTrouble -> viewModel.raiseInTroubleAlert(event.participant)
```

- [ ] **Step 3: Wire the HUD + debug row**

Update the `RideSessionHud` call (line 110) to pass the SOS fire callback:

```kotlin
            RideSessionHud(session = session, rallyPoi = rallyPoi, onSosFire = { viewModel.triggerSos() }, modifier = Modifier.weight(0.55f).fillMaxWidth())
```

Add `onCrash` / `onTrouble` to the `RideDebugControls` call (after `onForceBehind = ...`):

```kotlin
                    onForceBehind = { viewModel.forceSweepBehind() },
                    onCrash = { viewModel.simulateHardStop() },
                    onTrouble = { viewModel.simulateRiderInTrouble() },
```

And update the `RideDebugControls` definition signature + body to include them (place before "Off-route"), and make the row horizontally scrollable so the extra buttons don't overflow:

```kotlin
@Composable
private fun RideDebugControls(
    showSetGoal: Boolean,
    onSetGoal: () -> Unit,
    onRegroup: () -> Unit,
    onFuel: () -> Unit,
    onForceBehind: () -> Unit,
    onCrash: () -> Unit,
    onTrouble: () -> Unit,
    onDrift: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onRegroup) { Text("Regroup") }
        TextButton(onClick = onFuel) { Text("Fuel") }
        if (showSetGoal) TextButton(onClick = onSetGoal) { Text("Goal") }
        TextButton(onClick = onForceBehind) { Text("Behind") }
        TextButton(onClick = onCrash) { Text("Crash") }
        TextButton(onClick = onTrouble) { Text("Trouble") }
        TextButton(onClick = onDrift) { Text("Off-route") }
        Button(onClick = onEnd) { Text("End") }
    }
}
```

Add the imports for the scroll modifier at the top of the file:

```kotlin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
```

(Note: the old row used `Spacer(Modifier.weight(1f))` before "End"; that is removed because a horizontally-scrolling row can't use `weight`. The `Spacer` import stays used elsewhere.)

- [ ] **Step 4: Render the crash overlay + safety banners**

At the end of the outer `Box` (after the `fuelSignal?.let { ... }` block, ~line 167), add:

```kotlin
        activeAlert?.let { alert ->
            when (alert.type) {
                SafetyAlertType.RIDER_IN_TROUBLE ->
                    RiderInTroubleCard(alert = alert, onResolve = { viewModel.resolveActiveAlert() }, modifier = Modifier.align(Alignment.BottomCenter))
                else ->
                    SosActiveBanner(alert = alert, onSafe = { viewModel.resolveActiveAlert() }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (crashCountdownActive) {
            CrashCountdownOverlay(
                onOk = { crashCountdownActive = false },
                onSend = { crashCountdownActive = false; viewModel.confirmCrashAlert() },
                modifier = Modifier.align(Alignment.Center),
            )
        }
```

- [ ] **Step 5: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt \
        app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt
git commit -m "feat(safety): wire SOS button, crash overlay, safety banners, debug triggers

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Trusted-contacts management screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt` (add a nav row)
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt` (add route)
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt` (add composable + pass nav callback to Settings)
- Test: `app/src/test/java/com/valid/motouring/ui/profile/TrustedContactsViewModelTest.kt` (create)

**Interfaces:**
- Consumes: `RideBuddyRepository.friends()` / `.trustedContacts()` / `.setTrusted()` (Task 4), `RideBuddy` (Task 1).
- Produces: `TrustedContactsViewModel(rideBuddyRepository)` with `state: StateFlow<List<RideBuddy>>` (friends) + `fun toggle(userId, trusted)`; `Destinations.TRUSTED_CONTACTS`; `SettingsScreen(onOpenTrustedContacts)`.

- [ ] **Step 1: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/profile/TrustedContactsViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.profile

import com.valid.motouring.data.repository.RideBuddyRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedContactsViewModelTest {

    @Test
    fun `state lists friends and toggle flags a trusted contact`() {
        val repo = RideBuddyRepository()
        val vm = TrustedContactsViewModel(repo)
        val friends = vm.state.value
        assertTrue(friends.isNotEmpty())
        // Pick an un-flagged friend so this is robust to FakeDataProvider pre-flagging (Task 10).
        val target = friends.first { !it.isTrustedContact }
        vm.toggle(target.user.id, true)
        assertTrue(vm.state.value.first { it.user.id == target.user.id }.isTrustedContact)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.profile.TrustedContactsViewModelTest"`
Expected: FAIL — `TrustedContactsViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsViewModel.kt`:

```kotlin
package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrustedContactsViewModel(
    private val rideBuddyRepository: RideBuddyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(friends())
    val state: StateFlow<List<RideBuddy>> = _state.asStateFlow()

    private fun friends() = rideBuddyRepository.observeBuddies().value.filter { it.status == BuddyStatus.FRIEND }

    fun toggle(userId: String, trusted: Boolean) {
        rideBuddyRepository.setTrusted(userId, trusted)
        _state.value = friends()
    }

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { TrustedContactsViewModel(rideBuddyRepository) }
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.profile.TrustedContactsViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Create the screen**

Create `app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsScreen.kt`:

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Muted

@Composable
fun TrustedContactsScreen(viewModel: TrustedContactsViewModel) {
    val friends by viewModel.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Trusted Contacts", style = MaterialTheme.typography.headlineMedium)
        Text(
            "They get your SOS & crash alerts with your live location.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        friends.forEach { buddy ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(buddy.user.name, style = MaterialTheme.typography.bodyLarge)
                Switch(checked = buddy.isTrustedContact, onCheckedChange = { viewModel.toggle(buddy.user.id, it) })
            }
        }
    }
}
```

- [ ] **Step 6: Add the destination**

In `Destinations.kt`, add after `SETTINGS`:

```kotlin
    const val SETTINGS = "settings"
    const val TRUSTED_CONTACTS = "trusted_contacts"
```

- [ ] **Step 7: Add the Settings nav row**

In `SettingsScreen.kt`, add a parameter and a clickable row. Change the signature:

```kotlin
fun SettingsScreen(onOpenTrustedContacts: () -> Unit = {}) {
```

Add these imports:

```kotlin
import androidx.compose.foundation.clickable
```

Add the row after the two existing `SettingsToggleRow`s (before the version `Text`):

```kotlin
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTrustedContacts).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Trusted Contacts", style = MaterialTheme.typography.bodyLarge)
            Text(text = "›", style = MaterialTheme.typography.bodyLarge)
        }
```

- [ ] **Step 8: Wire the nav host**

In `MotouringNavHost.kt`, update the `SETTINGS` composable (currently `SettingsScreen()`) to pass the nav callback, and add the `TRUSTED_CONTACTS` composable after it:

```kotlin
        composable(Destinations.SETTINGS) {
            SettingsScreen(onOpenTrustedContacts = { navController.navigate(Destinations.TRUSTED_CONTACTS) })
        }
        composable(Destinations.TRUSTED_CONTACTS) {
            val viewModel: TrustedContactsViewModel = viewModel(
                factory = TrustedContactsViewModel.factory(appContainer.rideBuddyRepository),
            )
            TrustedContactsScreen(viewModel = viewModel)
        }
```

Add the imports near the other `ui.profile` imports:

```kotlin
import com.valid.motouring.ui.profile.TrustedContactsScreen
import com.valid.motouring.ui.profile.TrustedContactsViewModel
```

(`androidx.lifecycle.viewmodel.compose.viewModel` and `androidx.navigation.compose.composable` are already imported.)

- [ ] **Step 9: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass (incl. the new VM test).

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/profile/TrustedContactsScreen.kt \
        app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt \
        app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/test/java/com/valid/motouring/ui/profile/TrustedContactsViewModelTest.kt
git commit -m "feat(safety): trusted-contacts screen + Settings entry

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Fake data — pre-flag trusted friends

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Consumes: `RideBuddy.isTrustedContact` (Task 1).

- [ ] **Step 1: Pre-flag two friends as trusted**

In `FakeDataProvider.kt`, update the `rideBuddies` list (lines 41-47) so the first two friends are trusted:

```kotlin
    val rideBuddies = listOf(
        RideBuddy(users[1], BuddyStatus.FRIEND, isTrustedContact = true),
        RideBuddy(users[2], BuddyStatus.FRIEND, isTrustedContact = true),
        RideBuddy(users[3], BuddyStatus.FRIEND),
        RideBuddy(users[4], BuddyStatus.PENDING_RECEIVED),
        RideBuddy(users[5], BuddyStatus.NOT_CONNECTED),
    )
```

- [ ] **Step 2: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(safety): pre-flag Dinda & Bagas as trusted contacts

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 10)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (model, safety calcs, simulator safety, buddy repo, trusted-contacts VM, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): start a **group** ride and confirm — the red SOS button holds-to-send and drops into the persistent "SOS active · sharing location · {contact} responding · I'm safe now" banner; the "Crash" debug button raises the 15s red countdown, "I'm OK" cancels, letting it hit 0 fires the alert; the "Trouble" debug button (or ~1 min of ignoring the regroup) raises the red "may be in trouble" card, visibly graver than the orange regroup banner; and Settings → Trusted Contacts toggles friends (Dinda & Bagas pre-on).
