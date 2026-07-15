# Gear & Maintenance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the vehicle garage with a per-vehicle service log + mileage reminders (seeded odometer, OK/due-soon/overdue status, one-tap mark-serviced, garage due-badges) and an optional in-Start-Ride TCLOCS pre-ride checklist.

**Architecture:** A pure status calc (`MaintenanceCalculations`, TDD'd headless) drives everything. A `MaintenanceRepository` holds seeded per-vehicle `ServiceItem`s; `Vehicle` gains a seeded `odometerKm`. A `VehicleMaintenanceViewModel` combines vehicle + items + computed status for a new maintenance screen reached from Profile → My Garage (cards gain a due-badge). The pre-ride checklist is an ephemeral Compose component in Start-Ride.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), MVVM, kotlinx-coroutines, JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies.**
- **Direct-to-`main`, push after every task** (documented project norm; no branch/PR). Each commit message ends with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`. The maintenance screen, garage badges, and checklist are verified **on-device by the user** — they can't render on the headless VM.
- **Additive model changes only:** `Vehicle.odometerKm` is defaulted so existing construction sites keep compiling.
- **Colors from tokens only:** status colors are aliases of existing raw colors — `MotouringColors.statusOk = PoiFuel` (green), `statusDueSoon = PoiRest` (amber), `statusOverdue = SosRed` (red). No new hexes.
- **Status rule (exact):** `kmSince = max(0, odometer − lastServiced)`; `kmSince ≥ interval` → OVERDUE; `kmSince ≥ interval × DUE_SOON_FRACTION` (0.85) → DUE_SOON; else OK. "Due" (badge count) = DUE_SOON or OVERDUE.

## File Structure

| File | Responsibility |
| --- | --- |
| `data/model/Vehicle.kt` (modify) | `+ odometerKm` |
| `data/model/ServiceRecord.kt` (create) | `ServiceType`, `ServiceStatus`, `ServiceItem` |
| `ui/theme/Color.kt` (modify) | `MotouringColors.statusOk / statusDueSoon / statusOverdue` |
| `simulation/MaintenanceCalculations.kt` (create) | pure `serviceStatus` / `kmSinceService` / `serviceProgressFraction` / `dueCount` |
| `data/fake/FakeDataProvider.kt` (modify) | seed odometers + `serviceItems` |
| `data/repository/VehicleRepository.kt` (modify) | `+ setOdometer` |
| `data/repository/MaintenanceRepository.kt` (create) | seeded items, `itemsFor`, `markServiced` |
| `di/AppContainer.kt` (modify) | register `maintenanceRepository` |
| `ui/vehicle/VehicleMaintenanceViewModel.kt` (create) | vehicle + items + status + due count, `markServiced`, `setOdometer` |
| `ui/vehicle/VehicleMaintenanceScreen.kt` (create) | odometer header (editable), service list, mark-serviced |
| `navigation/Destinations.kt` + `navigation/MotouringNavHost.kt` (modify) | maintenance route |
| `ui/profile/ProfileViewModel.kt` + `ProfileScreen.kt` + `ui/main/MainScaffold.kt` (modify) | due-badge + tappable garage cards |
| `ui/rides/StartRideScreen.kt` (modify) | `PreRideChecklist` optional step |

Command shorthand:
- Single test class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- Full suite: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Model — odometer, service records, status colors

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/model/Vehicle.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/ServiceRecord.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Color.kt`
- Test: `app/src/test/java/com/valid/motouring/data/model/ServiceRecordTest.kt` (create)

**Interfaces:**
- Produces: `Vehicle.odometerKm: Int`; `enum ServiceType { OIL, CHAIN, TIRES, BRAKES, COOLANT }`; `enum ServiceStatus { OK, DUE_SOON, OVERDUE }`; `data class ServiceItem(vehicleId, type, lastServicedKm, intervalKm)`; `MotouringColors.statusOk/statusDueSoon/statusOverdue`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/model/ServiceRecordTest.kt`:

```kotlin
package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceRecordTest {

    @Test
    fun `vehicle odometer defaults to zero`() {
        val v = Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, 0)
        assertEquals(0, v.odometerKm)
    }

    @Test
    fun `service item carries vehicle, type, last-serviced and interval`() {
        val item = ServiceItem("v-1", ServiceType.CHAIN, lastServicedKm = 11_850, intervalKm = 700)
        assertEquals("v-1", item.vehicleId)
        assertEquals(ServiceType.CHAIN, item.type)
        assertEquals(11_850, item.lastServicedKm)
        assertEquals(700, item.intervalKm)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.ServiceRecordTest"`
Expected: FAIL — `ServiceItem` unresolved / `Vehicle` has no odometer arg.

- [ ] **Step 3: Add the odometer field**

In `Vehicle.kt`, add a defaulted field (keeps all existing `Vehicle(...)` sites compiling):

```kotlin
data class Vehicle(
    val id: String,
    val ownerId: String,
    val type: VehicleType,
    val make: String,
    val model: String,
    val year: Int,
    val photoRes: Int,
    val odometerKm: Int = 0,
)
```

- [ ] **Step 4: Create the service-record model**

Create `app/src/main/java/com/valid/motouring/data/model/ServiceRecord.kt`:

```kotlin
package com.valid.motouring.data.model

enum class ServiceType { OIL, CHAIN, TIRES, BRAKES, COOLANT }

enum class ServiceStatus { OK, DUE_SOON, OVERDUE }

data class ServiceItem(
    val vehicleId: String,
    val type: ServiceType,
    val lastServicedKm: Int,
    val intervalKm: Int,
)
```

- [ ] **Step 5: Add the status color aliases**

In `Color.kt`, add to the `MotouringColors` object (after `val sos = SosRed`):

```kotlin
    val sos = SosRed
    val statusOk = PoiFuel
    val statusDueSoon = PoiRest
    val statusOverdue = SosRed
```

- [ ] **Step 6: Run the test + full build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.ServiceRecordTest"`
Expected: PASS (2 tests).

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/Vehicle.kt \
        app/src/main/java/com/valid/motouring/data/model/ServiceRecord.kt \
        app/src/main/java/com/valid/motouring/ui/theme/Color.kt \
        app/src/test/java/com/valid/motouring/data/model/ServiceRecordTest.kt
git commit -m "feat(maintenance): odometer, service-record model, status colors

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Pure maintenance calculations

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/MaintenanceCalculations.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/MaintenanceCalculationsTest.kt`

**Interfaces:**
- Consumes: `ServiceItem`, `ServiceStatus` (Task 1).
- Produces: `const val DUE_SOON_FRACTION = 0.85`; `fun kmSinceService(odometerKm, lastServicedKm): Int`; `fun serviceStatus(odometerKm, lastServicedKm, intervalKm): ServiceStatus`; `fun serviceProgressFraction(odometerKm, lastServicedKm, intervalKm): Float`; `fun dueCount(items: List<ServiceItem>, odometerKm: Int): Int`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/valid/motouring/simulation/MaintenanceCalculationsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceCalculationsTest {

    @Test
    fun `serviceStatus is OK well within interval, DUE_SOON near it, OVERDUE at or past it`() {
        // interval 1000: OK below 850, DUE_SOON in [850,1000), OVERDUE at/above 1000
        assertEquals(ServiceStatus.OK, serviceStatus(odometerKm = 5_800, lastServicedKm = 5_000, intervalKm = 1_000))     // since 800
        assertEquals(ServiceStatus.DUE_SOON, serviceStatus(odometerKm = 5_900, lastServicedKm = 5_000, intervalKm = 1_000)) // since 900
        assertEquals(ServiceStatus.OVERDUE, serviceStatus(odometerKm = 6_000, lastServicedKm = 5_000, intervalKm = 1_000))  // since 1000
        assertEquals(ServiceStatus.OVERDUE, serviceStatus(odometerKm = 9_000, lastServicedKm = 5_000, intervalKm = 1_000))  // way past
    }

    @Test
    fun `kmSinceService never goes negative`() {
        assertEquals(0, kmSinceService(odometerKm = 100, lastServicedKm = 500))
        assertEquals(400, kmSinceService(odometerKm = 900, lastServicedKm = 500))
    }

    @Test
    fun `progress fraction is zero fresh and clamps to one when overdue`() {
        assertEquals(0f, serviceProgressFraction(5_000, 5_000, 1_000), 0.001f)
        assertEquals(0.5f, serviceProgressFraction(5_500, 5_000, 1_000), 0.001f)
        assertEquals(1f, serviceProgressFraction(9_000, 5_000, 1_000), 0.001f)
    }

    @Test
    fun `dueCount counts due-soon and overdue only`() {
        val items = listOf(
            ServiceItem("v", ServiceType.OIL, lastServicedKm = 4_800, intervalKm = 1_000),   // since 200 -> OK  (odo 5000)
            ServiceItem("v", ServiceType.CHAIN, lastServicedKm = 4_100, intervalKm = 1_000),  // since 900 -> DUE_SOON
            ServiceItem("v", ServiceType.TIRES, lastServicedKm = 3_000, intervalKm = 1_000),  // since 2000 -> OVERDUE
        )
        assertEquals(2, dueCount(items, odometerKm = 5_000))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.MaintenanceCalculationsTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement the helpers**

Create `app/src/main/java/com/valid/motouring/simulation/MaintenanceCalculations.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus

/** Fraction of an interval consumed before an item flips from OK to "due soon". */
const val DUE_SOON_FRACTION = 0.85

fun kmSinceService(odometerKm: Int, lastServicedKm: Int): Int = (odometerKm - lastServicedKm).coerceAtLeast(0)

fun serviceStatus(odometerKm: Int, lastServicedKm: Int, intervalKm: Int): ServiceStatus {
    if (intervalKm <= 0) return ServiceStatus.OK
    val since = kmSinceService(odometerKm, lastServicedKm)
    return when {
        since >= intervalKm -> ServiceStatus.OVERDUE
        since >= intervalKm * DUE_SOON_FRACTION -> ServiceStatus.DUE_SOON
        else -> ServiceStatus.OK
    }
}

fun serviceProgressFraction(odometerKm: Int, lastServicedKm: Int, intervalKm: Int): Float {
    if (intervalKm <= 0) return 0f
    return (kmSinceService(odometerKm, lastServicedKm).toFloat() / intervalKm).coerceIn(0f, 1f)
}

fun dueCount(items: List<ServiceItem>, odometerKm: Int): Int =
    items.count { serviceStatus(odometerKm, it.lastServicedKm, it.intervalKm) != ServiceStatus.OK }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.simulation.MaintenanceCalculationsTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/MaintenanceCalculations.kt \
        app/src/test/java/com/valid/motouring/simulation/MaintenanceCalculationsTest.kt
git commit -m "feat(maintenance): pure service status / progress / due-count calc

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Fake data — seed odometers + service items

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Produces: `FakeDataProvider.serviceItems: List<ServiceItem>`; seeded `odometerKm` on the two `u-me` vehicles (`v-1`, `v-2`).

This task is verified by **build + existing tests green** (it's seed data; the values are exercised by later tasks' tests and on-device).

- [ ] **Step 1: Seed the two owned vehicles' odometers**

In `FakeDataProvider.kt`, the `vehicles` list — add `odometerKm` to `v-1` and `v-2` (leave the others at the default 0). Replace those two lines:

```kotlin
        Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, R.drawable.img_vehicle_moto, odometerKm = 12_480),
        Vehicle("v-2", "u-me", VehicleType.CAR, "Toyota", "Raize", 2022, R.drawable.img_vehicle_car, odometerKm = 34_200),
```

- [ ] **Step 2: Add the seeded service items**

Add a new top-level property in `FakeDataProvider` (e.g. just after the `vehicles` list):

```kotlin
    val serviceItems = listOf(
        // Yamaha MT-25 (v-1) @ 12,480 km  -> Tires OVERDUE, Chain DUE_SOON, Oil/Brakes OK  (2 due)
        ServiceItem("v-1", ServiceType.OIL, lastServicedKm = 9_900, intervalKm = 6_000),
        ServiceItem("v-1", ServiceType.CHAIN, lastServicedKm = 11_850, intervalKm = 700),
        ServiceItem("v-1", ServiceType.TIRES, lastServicedKm = 2_300, intervalKm = 10_000),
        ServiceItem("v-1", ServiceType.BRAKES, lastServicedKm = 8_000, intervalKm = 15_000),
        // Toyota Raize (v-2) @ 34,200 km  -> Coolant DUE_SOON, rest OK  (1 due)
        ServiceItem("v-2", ServiceType.OIL, lastServicedKm = 30_000, intervalKm = 10_000),
        ServiceItem("v-2", ServiceType.TIRES, lastServicedKm = 20_000, intervalKm = 40_000),
        ServiceItem("v-2", ServiceType.BRAKES, lastServicedKm = 18_000, intervalKm = 30_000),
        ServiceItem("v-2", ServiceType.COOLANT, lastServicedKm = 5_000, intervalKm = 30_000),
    )
```

(`ServiceItem` / `ServiceType` are in `com.valid.motouring.data.model`, already covered by the file's `import com.valid.motouring.data.model.*` wildcard — no new import.)

- [ ] **Step 3: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(maintenance): seed odometers + service items (some due/overdue)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Repositories — odometer setter + maintenance repo

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/repository/VehicleRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/MaintenanceRepository.kt`
- Modify: `app/src/main/java/com/valid/motouring/di/AppContainer.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/MaintenanceRepositoryTest.kt` (create)

**Interfaces:**
- Consumes: `FakeDataProvider.serviceItems` (Task 3), `ServiceItem`, `ServiceType` (Task 1).
- Produces: `VehicleRepository.setOdometer(vehicleId, odometerKm)`; `MaintenanceRepository` with `observeItems()`, `itemsFor(vehicleId)`, `markServiced(vehicleId, type, atOdometerKm)`; `AppContainer.maintenanceRepository`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/repository/MaintenanceRepositoryTest.kt`:

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceRepositoryTest {

    @Test
    fun `itemsFor returns only that vehicle's items`() {
        val repo = MaintenanceRepository()
        val v1 = repo.itemsFor("v-1")
        assertTrue(v1.isNotEmpty())
        assertTrue(v1.all { it.vehicleId == "v-1" })
    }

    @Test
    fun `markServiced resets lastServicedKm to the given odometer for the matching item only`() {
        val repo = MaintenanceRepository()
        repo.markServiced("v-1", ServiceType.TIRES, atOdometerKm = 12_480)
        val tires = repo.itemsFor("v-1").first { it.type == ServiceType.TIRES }
        assertEquals(12_480, tires.lastServicedKm)
        // a different item is untouched
        val oil = repo.itemsFor("v-1").first { it.type == ServiceType.OIL }
        assertEquals(9_900, oil.lastServicedKm)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.MaintenanceRepositoryTest"`
Expected: FAIL — `MaintenanceRepository` unresolved.

- [ ] **Step 3: Add `setOdometer` to VehicleRepository**

In `VehicleRepository.kt`, add (after `addVehicle`):

```kotlin
    fun setOdometer(vehicleId: String, odometerKm: Int) {
        vehicles.value = vehicles.value.map {
            if (it.id == vehicleId) it.copy(odometerKm = odometerKm) else it
        }
    }
```

- [ ] **Step 4: Create the maintenance repository**

Create `app/src/main/java/com/valid/motouring/data/repository/MaintenanceRepository.kt`:

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MaintenanceRepository {
    private val items = MutableStateFlow(FakeDataProvider.serviceItems)

    fun observeItems(): StateFlow<List<ServiceItem>> = items.asStateFlow()

    fun itemsFor(vehicleId: String): List<ServiceItem> =
        items.value.filter { it.vehicleId == vehicleId }

    fun markServiced(vehicleId: String, type: ServiceType, atOdometerKm: Int) {
        items.value = items.value.map {
            if (it.vehicleId == vehicleId && it.type == type) it.copy(lastServicedKm = atOdometerKm) else it
        }
    }
}
```

- [ ] **Step 5: Register in AppContainer**

In `AppContainer.kt`, add the import and the field:

```kotlin
import com.valid.motouring.data.repository.MaintenanceRepository
```

```kotlin
    val notificationRepository = NotificationRepository()
    val maintenanceRepository = MaintenanceRepository()
```

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.MaintenanceRepositoryTest"`
Expected: PASS (2 tests).

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/VehicleRepository.kt \
        app/src/main/java/com/valid/motouring/data/repository/MaintenanceRepository.kt \
        app/src/main/java/com/valid/motouring/di/AppContainer.kt \
        app/src/test/java/com/valid/motouring/data/repository/MaintenanceRepositoryTest.kt
git commit -m "feat(maintenance): odometer setter + MaintenanceRepository

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Maintenance ViewModel + screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceScreen.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModelTest.kt` (create)

**Interfaces:**
- Consumes: pure calc (Task 2), repos (Task 4), `Vehicle`/`ServiceItem`/`ServiceType`/`ServiceStatus` (Task 1).
- Produces: `VehicleMaintenanceViewModel(vehicleRepository, maintenanceRepository, vehicleId)` exposing `state: StateFlow<MaintenanceState>` with `markServiced(type)` / `setOdometer(km)`; `MaintenanceState(vehicle: Vehicle?, items: List<ServiceItemUi>, dueCount: Int)`; `ServiceItemUi(item, status, kmSince, progress)`; `@Composable VehicleMaintenanceScreen(viewModel)`.

- [ ] **Step 1: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.vehicle

import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.data.repository.VehicleRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleMaintenanceViewModelTest {

    private fun vm() = VehicleMaintenanceViewModel(VehicleRepository(), MaintenanceRepository(), "v-1")

    @Test
    fun `state exposes seeded items with computed status and due count`() {
        val state = vm().state.value
        assertEquals("v-1", state.vehicle?.id)
        assertEquals(ServiceStatus.OVERDUE, state.items.first { it.item.type == ServiceType.TIRES }.status)
        assertEquals(2, state.dueCount) // Tires overdue + Chain due-soon
    }

    @Test
    fun `markServiced resets an item to OK and lowers the due count`() {
        val vm = vm()
        vm.markServiced(ServiceType.TIRES)
        val state = vm.state.value
        assertEquals(ServiceStatus.OK, state.items.first { it.item.type == ServiceType.TIRES }.status)
        assertEquals(1, state.dueCount)
    }

    @Test
    fun `setOdometer recomputes status`() {
        val vm = vm()
        vm.setOdometer(99_999)
        // At 99,999 km every v-1 item is past its interval (oil since 90k≥6k, chain 88k≥700,
        // tires 97k≥10k, brakes 91,999≥15k) → all 4 OVERDUE.
        assertEquals(4, vm.state.value.dueCount)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.vehicle.VehicleMaintenanceViewModelTest"`
Expected: FAIL — `VehicleMaintenanceViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModel.kt`:

```kotlin
package com.valid.motouring.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ServiceItem
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.data.model.ServiceType
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.data.repository.VehicleRepository
import com.valid.motouring.simulation.dueCount
import com.valid.motouring.simulation.kmSinceService
import com.valid.motouring.simulation.serviceProgressFraction
import com.valid.motouring.simulation.serviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceItemUi(
    val item: ServiceItem,
    val status: ServiceStatus,
    val kmSince: Int,
    val progress: Float,
)

data class MaintenanceState(
    val vehicle: Vehicle?,
    val items: List<ServiceItemUi>,
    val dueCount: Int,
)

class VehicleMaintenanceViewModel(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<MaintenanceState> = _state.asStateFlow()

    private fun compute(): MaintenanceState {
        val vehicle = vehicleRepository.observeVehicles().value.firstOrNull { it.id == vehicleId }
        val odo = vehicle?.odometerKm ?: 0
        val items = maintenanceRepository.itemsFor(vehicleId)
        val ui = items.map {
            ServiceItemUi(
                item = it,
                status = serviceStatus(odo, it.lastServicedKm, it.intervalKm),
                kmSince = kmSinceService(odo, it.lastServicedKm),
                progress = serviceProgressFraction(odo, it.lastServicedKm, it.intervalKm),
            )
        }
        return MaintenanceState(vehicle, ui, dueCount(items, odo))
    }

    fun markServiced(type: ServiceType) {
        val odo = _state.value.vehicle?.odometerKm ?: return
        maintenanceRepository.markServiced(vehicleId, type, odo)
        _state.value = compute()
    }

    fun setOdometer(km: Int) {
        vehicleRepository.setOdometer(vehicleId, km)
        _state.value = compute()
    }

    companion object {
        fun factory(
            vehicleRepository: VehicleRepository,
            maintenanceRepository: MaintenanceRepository,
            vehicleId: String,
        ) = viewModelFactory {
            initializer { VehicleMaintenanceViewModel(vehicleRepository, maintenanceRepository, vehicleId) }
        }
    }
}
```

- [ ] **Step 4: Run to verify the VM tests pass**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.vehicle.VehicleMaintenanceViewModelTest"`
Expected: PASS (3 tests) — after setting the Step-1 third assertion to `4`.

- [ ] **Step 5: Create the screen**

Create `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceScreen.kt`:

```kotlin
package com.valid.motouring.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.ServiceStatus
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.Muted

private fun ServiceStatus.color(): Color = when (this) {
    ServiceStatus.OK -> MotouringColors.statusOk
    ServiceStatus.DUE_SOON -> MotouringColors.statusDueSoon
    ServiceStatus.OVERDUE -> MotouringColors.statusOverdue
}

private fun ServiceStatus.label(): String = when (this) {
    ServiceStatus.OK -> "OK"
    ServiceStatus.DUE_SOON -> "Due soon"
    ServiceStatus.OVERDUE -> "Overdue"
}

@Composable
fun VehicleMaintenanceScreen(viewModel: VehicleMaintenanceViewModel) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val vehicle = state.vehicle
        Text(
            text = vehicle?.let { "${it.year} ${it.make} ${it.model}" } ?: "Vehicle",
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { editing = true },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${vehicle?.odometerKm ?: 0}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(start = 6.dp))
            Text("km · tap to edit", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            text = if (state.dueCount > 0) "${state.dueCount} need attention" else "All up to date",
            color = if (state.dueCount > 0) MotouringColors.statusOverdue else MotouringColors.statusOk,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.item.type }) { ui ->
                MotouringCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ui.item.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            StatusChip(ui.status)
                        }
                        Text(
                            "last ${ui.item.lastServicedKm} km · ${ui.kmSince} ago · every ${ui.item.intervalKm}",
                            style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp),
                        )
                        LinearProgressIndicator(
                            progress = { ui.progress },
                            color = ui.status.color(),
                            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 8.dp),
                        )
                        if (ui.status != ServiceStatus.OK) {
                            TextButton(onClick = { viewModel.markServiced(ui.item.type) }, modifier = Modifier.padding(top = 4.dp)) {
                                Text("Mark serviced")
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        OdometerDialog(
            current = state.vehicle?.odometerKm ?: 0,
            onConfirm = { viewModel.setOdometer(it); editing = false },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun StatusChip(status: ServiceStatus) {
    Text(
        text = status.label().uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = status.color(),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(status.color().copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun OdometerDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update odometer") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(7) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.toIntOrNull() ?: current) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
```

- [ ] **Step 6: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceScreen.kt \
        app/src/test/java/com/valid/motouring/ui/vehicle/VehicleMaintenanceViewModelTest.kt
git commit -m "feat(maintenance): maintenance ViewModel + per-vehicle service screen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Garage → maintenance wiring + due badge

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `VehicleMaintenanceViewModel` (Task 5), `dueCount` (Task 2), `MaintenanceRepository` (Task 4).
- Produces: `Destinations.VEHICLE_MAINTENANCE_PATTERN` + `vehicleMaintenance(id)`; `ProfileViewModel.dueCounts: StateFlow<Map<String,Int>>`; `ProfileScreen(..., onVehicleClick: (String) -> Unit)`.

UI/wiring — verified by build + on-device.

- [ ] **Step 1: Add the destination**

In `Destinations.kt`, add after the `NOTIFICATIONS` line:

```kotlin
    const val VEHICLE_MAINTENANCE_PATTERN = "vehicle_maintenance/{vehicleId}"
    fun vehicleMaintenance(vehicleId: String) = "vehicle_maintenance/$vehicleId"
```

- [ ] **Step 2: Add the maintenance route to the nav host**

In `MotouringNavHost.kt`, add a `composable` after the `NOTIFICATIONS` one (follow the existing `POST_DETAIL_PATTERN` id-arg pattern):

```kotlin
        composable(
            Destinations.VEHICLE_MAINTENANCE_PATTERN,
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val vehicleId = requireNotNull(backStackEntry.arguments?.getString("vehicleId"))
            val viewModel: VehicleMaintenanceViewModel = viewModel(
                factory = VehicleMaintenanceViewModel.factory(
                    appContainer.vehicleRepository,
                    appContainer.maintenanceRepository,
                    vehicleId,
                ),
            )
            VehicleMaintenanceScreen(viewModel = viewModel)
        }
```

Add the imports near the other `ui.vehicle` imports:

```kotlin
import com.valid.motouring.ui.vehicle.VehicleMaintenanceScreen
import com.valid.motouring.ui.vehicle.VehicleMaintenanceViewModel
```

(`navArgument`, `NavType`, and `viewModel` are already imported — the `POST_DETAIL_PATTERN` composable uses them.)

- [ ] **Step 3: Add `dueCounts` to ProfileViewModel**

In `ProfileViewModel.kt`, add the constructor param `maintenanceRepository: MaintenanceRepository`, the imports, and the `dueCounts` flow. Imports:

```kotlin
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.simulation.dueCount
import kotlinx.coroutines.flow.combine
```

Constructor:

```kotlin
class ProfileViewModel(
    userRepository: UserRepository,
    vehicleRepository: VehicleRepository,
    rideRepository: RideRepository,
    badgeRepository: BadgeRepository,
    maintenanceRepository: MaintenanceRepository,
) : ViewModel() {
```

After the `vehicles` flow, add:

```kotlin
    val dueCounts: StateFlow<Map<String, Int>> =
        combine(vehicles, maintenanceRepository.observeItems()) { vs, items ->
            vs.associate { v -> v.id to dueCount(items.filter { it.vehicleId == v.id }, v.odometerKm) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
```

Update the `factory` to take + pass `maintenanceRepository`:

```kotlin
        fun factory(
            userRepository: UserRepository,
            vehicleRepository: VehicleRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
            maintenanceRepository: MaintenanceRepository,
        ) = viewModelFactory {
            initializer { ProfileViewModel(userRepository, vehicleRepository, rideRepository, badgeRepository, maintenanceRepository) }
        }
```

- [ ] **Step 4: Make garage cards clickable + badged in ProfileScreen**

In `ProfileScreen.kt`, add the `onVehicleClick` param and collect `dueCounts`. Signature:

```kotlin
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onFriendsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onVehicleClick: (String) -> Unit,
) {
```

After `val badges by ...`:

```kotlin
    val dueCounts by viewModel.dueCounts.collectAsState()
```

Replace the garage `MotouringCard { Row { ... } }` block (the `itemsIndexed` body) with a clickable, badged version:

```kotlin
        itemsIndexed(vehicles) { index, vehicle ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 8.dp)) {
                MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onVehicleClick(vehicle.id) }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = vehicle.photoRes),
                            contentDescription = "${vehicle.make} ${vehicle.model}",
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text(text = "${vehicle.year} ${vehicle.make} ${vehicle.model}", modifier = Modifier.weight(1f))
                        val due = dueCounts[vehicle.id] ?: 0
                        if (due > 0) {
                            Text(
                                text = "$due due",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.valid.motouring.ui.theme.MotouringColors.statusOverdue,
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(com.valid.motouring.ui.theme.MotouringColors.statusOverdue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
```

Add the `background` import at the top of `ProfileScreen.kt` (the rest — `clip`, `RoundedCornerShape` (fully-qualified above), `weight`, `MaterialTheme` — are already imported):

```kotlin
import androidx.compose.foundation.background
```

- [ ] **Step 5: Wire MainScaffold**

In `MainScaffold.kt`, the Profile tab composable — pass `maintenanceRepository` into the factory and add `onVehicleClick`:

```kotlin
                    val viewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.factory(
                            appContainer.userRepository,
                            appContainer.vehicleRepository,
                            appContainer.rideRepository,
                            appContainer.badgeRepository,
                            appContainer.maintenanceRepository,
                        ),
                    )
                    ProfileScreen(
                        viewModel = viewModel,
                        onFriendsClick = { outerNavController.navigate(Destinations.FRIENDS) },
                        onEditProfileClick = { outerNavController.navigate(Destinations.EDIT_PROFILE) },
                        onSettingsClick = { outerNavController.navigate(Destinations.SETTINGS) },
                        onNotificationsClick = { outerNavController.navigate(Destinations.NOTIFICATIONS) },
                        onVehicleClick = { vehicleId -> outerNavController.navigate(Destinations.vehicleMaintenance(vehicleId)) },
                    )
```

- [ ] **Step 6: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/main/java/com/valid/motouring/ui/profile/ProfileViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt \
        app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat(maintenance): garage due-badges + tap-to-open maintenance screen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Pre-ride TCLOCS checklist in Start-Ride

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt`

**Interfaces:**
- Produces: `enum TclocsItem`; `@Composable PreRideChecklist(modifier)` rendered inside `StartRideScreen`.

UI — verified by build + `@Preview` + on-device.

- [ ] **Step 1: Add the checklist component + enum**

In `StartRideScreen.kt`, add imports:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.MotouringColors
```

(`Column`, `Row`, `Text`, `Spacer`, `padding`, `fillMaxWidth`, `height`, `MaterialTheme`, `Modifier`, `remember`, `getValue`, `dp` are already imported in `StartRideScreen.kt`.)

Add the enum + composable at the bottom of the file (before the `@Preview`):

```kotlin
enum class TclocsItem(val label: String) {
    TIRES("Tires & wheels"),
    CONTROLS("Controls"),
    LIGHTS("Lights & electrics"),
    OIL("Oil & fluids"),
    CHASSIS("Chassis"),
    STANDS("Stands"),
}

@Composable
fun PreRideChecklist(modifier: Modifier = Modifier) {
    val checked = remember { mutableStateMapOf<TclocsItem, Boolean>() }
    val total = TclocsItem.entries.size
    val done = TclocsItem.entries.count { checked[it] == true }
    MotouringCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pre-ride check · TCLOCS", style = MaterialTheme.typography.titleSmall)
                    Text("optional — tap to confirm each", style = MaterialTheme.typography.bodySmall)
                }
                Text("$done / $total", style = MaterialTheme.typography.titleSmall,
                    color = if (done == total) MotouringColors.statusOk else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TclocsItem.entries.forEach { item ->
                val isChecked = checked[item] == true
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { checked[item] = !isChecked }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = { checked[item] = it })
                    Text(item.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (done == total) {
                Text(
                    "All clear — ready to ride",
                    style = MaterialTheme.typography.bodySmall,
                    color = MotouringColors.statusOk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Render it in Start-Ride**

In `StartRideScreen`, insert the checklist between the goal picker and the bottom `Spacer(Modifier.weight(1f, ...))`. After the goal-picker `Row { ... }` closes (the one wrapping the `goalPresets.forEach`), add:

```kotlin
        Spacer(modifier = Modifier.height(16.dp))
        PreRideChecklist()
```

(The existing `Start Ride` button stays exactly as-is — the checklist never blocks starting; its ticked state lives in `remember` and resets when the screen leaves composition.)

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt
git commit -m "feat(maintenance): optional pre-ride TCLOCS checklist in Start-Ride

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 7)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (model, maintenance calc, maintenance repo, maintenance VM, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): Profile → My Garage shows a red "2 due" badge on the MT-25 and "1 due" on the Raize; tapping a vehicle opens the maintenance screen (odometer header, tap-to-edit dialog, Oil/Chain/Tires/Brakes with OK/Due-soon/Overdue chips + progress bars); "Mark serviced" on Tires flips it to OK and drops the badge count; and Start-Ride shows the optional TCLOCS checklist under the goal picker (ticking 6/6 shows "All clear", starting a ride still works with any/none ticked).
