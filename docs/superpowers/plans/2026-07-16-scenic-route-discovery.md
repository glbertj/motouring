# Scenic-Route Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A curated scenic-routes browse surface — a list and a detail (hero photo, description, `MotouringMap` route preview, "Ride this route" CTA) — reached by repurposing the Start-Ride FAB's "Plan a route" action.

**Architecture:** A seeded `ScenicRouteRepository` behind two snapshot ViewModels + two screens (browse + detail), wired via new nav routes. The FAB "Plan a route" navigates to the browse; the detail's "Ride this route" navigates to the existing Start-Ride flow (the route polyline is illustrative — it is NOT threaded into the ride simulator).

**Tech Stack:** Kotlin, Jetpack Compose (Material3), MVVM, kotlinx-coroutines, MapLibre, JUnit4. In-memory fake data; no backend.

## Global Constraints

- **No new dependencies.**
- **Direct-to-`main`, push after every task** (documented project norm; no branch/PR). Each commit message ends with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer.
- **Headless build must stay green:** `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`. The browse cards, map preview, and FAB flow are verified **on-device by the user**.
- **Colors from tokens only:** vibe chips reuse existing `MotouringColors` accents (`rider`/`riderPurple`/`poiFuel`/`poiRest`); no new hexes.
- **No route threading:** "Ride this route" only navigates to `START_RIDE`; do not modify the Start-Ride/RideSession/ride-sim wiring.

## File Structure

| File | Responsibility |
| --- | --- |
| `data/model/ScenicRoute.kt` (create) | `ScenicVibe`, `ScenicRoute` |
| `data/fake/FakeDataProvider.kt` (modify) | seed ~4 scenic routes |
| `data/repository/ScenicRouteRepository.kt` (create) | seeded routes, `observeRoutes`/`routes`/`route` |
| `di/AppContainer.kt` (modify) | register `scenicRouteRepository` |
| `ui/scenic/VibeChip.kt` (create) | shared vibe chip (label + accent color) |
| `ui/scenic/ScenicRoutesViewModel.kt` + `ScenicRoutesScreen.kt` (create) | browse list |
| `ui/scenic/ScenicRouteDetailViewModel.kt` + `ScenicRouteDetailScreen.kt` (create) | detail + map + Ride CTA |
| `navigation/Destinations.kt` + `MotouringNavHost.kt` (modify) | scenic routes + wiring |
| `ui/main/MainScaffold.kt` (modify) | FAB "Plan a route" → `SCENIC_ROUTES` |

Command shorthand:
- Single test class: `./gradlew testDebugUnitTest --tests "com.valid.motouring.<pkg>.<Class>"`
- Full suite: `./gradlew testDebugUnitTest`
- Build: `./gradlew assembleDebug`

---

## Task 1: Model — scenic route + vibe

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/model/ScenicRoute.kt`
- Test: `app/src/test/java/com/valid/motouring/data/model/ScenicRouteTest.kt` (create)

**Interfaces:**
- Produces: `enum ScenicVibe { COASTAL, MOUNTAIN, FOREST, URBAN }`; `data class ScenicRoute(id, name, region, distanceKm, estimatedMinutes, vibe: List<ScenicVibe>, heroPhotoRes, description, route: List<GeoPoint>)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/model/ScenicRouteTest.kt`:

```kotlin
package com.valid.motouring.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenicRouteTest {

    @Test
    fun `scenic route carries its vibe tags and route`() {
        val r = ScenicRoute(
            id = "sc-1", name = "Puncak Ridge", region = "Bogor", distanceKm = 42.0, estimatedMinutes = 95,
            vibe = listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), heroPhotoRes = 0,
            description = "Cool mountain air and switchbacks.",
            route = listOf(GeoPoint(-6.7, 106.9), GeoPoint(-6.6, 107.0)),
        )
        assertEquals(listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), r.vibe)
        assertEquals(2, r.route.size)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.ScenicRouteTest"`
Expected: FAIL — `ScenicRoute` unresolved.

- [ ] **Step 3: Create the model**

Create `app/src/main/java/com/valid/motouring/data/model/ScenicRoute.kt`:

```kotlin
package com.valid.motouring.data.model

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

- [ ] **Step 4: Run the test + full build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.model.ScenicRouteTest"`
Expected: PASS.

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/ScenicRoute.kt \
        app/src/test/java/com/valid/motouring/data/model/ScenicRouteTest.kt
git commit -m "feat(scenic): scenic-route + vibe model

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Seed scenic routes

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Produces: `FakeDataProvider.scenicRoutes: List<ScenicRoute>`.

Verified by **build + existing tests green** (seed data).

- [ ] **Step 1: Add the seeded list**

In `FakeDataProvider.kt`, add a property (e.g. after `segments`). Reuse `img_road_*` heroes and short `route` polylines (any plausible LatLngs):

```kotlin
    val scenicRoutes = listOf(
        ScenicRoute(
            "sc-1", "Puncak Pass Run", "Bogor", 38.0, 90,
            listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), R.drawable.img_road_6,
            "Switchbacks and tea-plantation views up to the pass. Cool air, best early morning before traffic.",
            listOf(GeoPoint(-6.70, 106.90), GeoPoint(-6.66, 106.95), GeoPoint(-6.62, 107.00)),
        ),
        ScenicRoute(
            "sc-2", "South Coast Cruise", "Sukabumi", 64.0, 140,
            listOf(ScenicVibe.COASTAL), R.drawable.img_road_2,
            "Long sweeping bends along the Indian Ocean cliffs. Open throttle, big horizons.",
            listOf(GeoPoint(-7.02, 106.55), GeoPoint(-7.05, 106.62), GeoPoint(-7.08, 106.70)),
        ),
        ScenicRoute(
            "sc-3", "Sudirman City Loop", "Jakarta", 12.0, 35,
            listOf(ScenicVibe.URBAN), R.drawable.img_road_1,
            "A quick after-dark loop through the CBD — lit towers, smooth tarmac, easy pace.",
            listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.2088, 106.8206), GeoPoint(-6.1875, 106.8271)),
        ),
        ScenicRoute(
            "sc-4", "Pine Forest Traverse", "Bandung", 51.0, 120,
            listOf(ScenicVibe.FOREST, ScenicVibe.MOUNTAIN), R.drawable.img_road_4,
            "Dappled light through the pines and cool ridgeline curves. A rider's favourite Sunday.",
            listOf(GeoPoint(-6.85, 107.60), GeoPoint(-6.82, 107.66), GeoPoint(-6.80, 107.72)),
        ),
    )
```

(`ScenicRoute`, `ScenicVibe`, `GeoPoint` are covered by the file's `import com.valid.motouring.data.model.*` wildcard — no new import.)

- [ ] **Step 2: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat(scenic): seed curated scenic routes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: ScenicRouteRepository

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/repository/ScenicRouteRepository.kt`
- Modify: `app/src/main/java/com/valid/motouring/di/AppContainer.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/ScenicRouteRepositoryTest.kt` (create)

**Interfaces:**
- Consumes: `FakeDataProvider.scenicRoutes` (Task 2), `ScenicRoute` (Task 1).
- Produces: `ScenicRouteRepository` with `observeRoutes()`, `routes()`, `route(id): ScenicRoute?`; `AppContainer.scenicRouteRepository`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/valid/motouring/data/repository/ScenicRouteRepositoryTest.kt`:

```kotlin
package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicRouteRepositoryTest {

    @Test
    fun `routes are seeded and route finds by id`() {
        val repo = ScenicRouteRepository()
        assertTrue(repo.routes().isNotEmpty())
        val first = repo.routes().first()
        assertEquals(first, repo.route(first.id))
        assertNull(repo.route("nope"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.ScenicRouteRepositoryTest"`
Expected: FAIL — `ScenicRouteRepository` unresolved.

- [ ] **Step 3: Create the repository**

Create `app/src/main/java/com/valid/motouring/data/repository/ScenicRouteRepository.kt`:

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.ScenicRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenicRouteRepository {
    private val routes = MutableStateFlow(FakeDataProvider.scenicRoutes)

    fun observeRoutes(): StateFlow<List<ScenicRoute>> = routes.asStateFlow()

    fun routes(): List<ScenicRoute> = routes.value

    fun route(id: String): ScenicRoute? = routes.value.firstOrNull { it.id == id }
}
```

- [ ] **Step 4: Register in AppContainer**

In `AppContainer.kt`, add the import and field:

```kotlin
import com.valid.motouring.data.repository.ScenicRouteRepository
```

```kotlin
    val segmentRepository = SegmentRepository()
    val scenicRouteRepository = ScenicRouteRepository()
```

- [ ] **Step 5: Run to verify it passes + build**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.data.repository.ScenicRouteRepositoryTest"`
Expected: PASS.

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/ScenicRouteRepository.kt \
        app/src/main/java/com/valid/motouring/di/AppContainer.kt \
        app/src/test/java/com/valid/motouring/data/repository/ScenicRouteRepositoryTest.kt
git commit -m "feat(scenic): ScenicRouteRepository seeded from fake data

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Browse screen + shared vibe chip

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/scenic/VibeChip.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesScreen.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModelTest.kt` (create)

**Interfaces:**
- Consumes: `ScenicRouteRepository` (Task 3), `ScenicRoute`/`ScenicVibe` (Task 1).
- Produces: `@Composable VibeChip(vibe: ScenicVibe)`; `ScenicRoutesViewModel(scenicRouteRepository)` with `routes: StateFlow<List<ScenicRoute>>`; `ScenicRoutesScreen(viewModel, onRouteClick: (String) -> Unit)`.

- [ ] **Step 1: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import com.valid.motouring.data.repository.ScenicRouteRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicRoutesViewModelTest {

    @Test
    fun `routes are exposed from the repository`() {
        val vm = ScenicRoutesViewModel(ScenicRouteRepository())
        assertTrue(vm.routes.value.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.scenic.ScenicRoutesViewModelTest"`
Expected: FAIL — `ScenicRoutesViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModel.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ScenicRoute
import com.valid.motouring.data.repository.ScenicRouteRepository
import kotlinx.coroutines.flow.StateFlow

class ScenicRoutesViewModel(
    scenicRouteRepository: ScenicRouteRepository,
) : ViewModel() {

    val routes: StateFlow<List<ScenicRoute>> = scenicRouteRepository.observeRoutes()

    companion object {
        fun factory(scenicRouteRepository: ScenicRouteRepository) = viewModelFactory {
            initializer { ScenicRoutesViewModel(scenicRouteRepository) }
        }
    }
}
```

- [ ] **Step 4: Run to verify the VM test passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.scenic.ScenicRoutesViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Create the shared vibe chip**

Create `app/src/main/java/com/valid/motouring/ui/scenic/VibeChip.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.ScenicVibe
import com.valid.motouring.ui.theme.MotouringColors

private fun ScenicVibe.color(): Color = when (this) {
    ScenicVibe.COASTAL -> MotouringColors.rider
    ScenicVibe.MOUNTAIN -> MotouringColors.riderPurple
    ScenicVibe.FOREST -> MotouringColors.poiFuel
    ScenicVibe.URBAN -> MotouringColors.poiRest
}

private fun ScenicVibe.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun VibeChip(vibe: ScenicVibe, modifier: Modifier = Modifier) {
    Text(
        text = vibe.label(),
        style = MaterialTheme.typography.labelSmall,
        color = vibe.color(),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(vibe.color().copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
```

- [ ] **Step 6: Create the browse screen**

Create `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesScreen.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.theme.Muted

@Composable
fun ScenicRoutesScreen(viewModel: ScenicRoutesViewModel, onRouteClick: (String) -> Unit) {
    val routes by viewModel.routes.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Scenic Routes", style = MaterialTheme.typography.headlineMedium) }
        items(routes, key = { it.id }) { route ->
            MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = { onRouteClick(route.id) }) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Image(
                        painter = painterResource(id = route.heroPhotoRes),
                        contentDescription = route.name,
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Text(route.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("${route.region} · ${"%.0f".format(route.distanceKm)} km · ${route.estimatedMinutes} min", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        route.vibe.forEach { VibeChip(it) }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass (incl. the new VM test).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/scenic/VibeChip.kt \
        app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/scenic/ScenicRoutesScreen.kt \
        app/src/test/java/com/valid/motouring/ui/scenic/ScenicRoutesViewModelTest.kt
git commit -m "feat(scenic): scenic-routes browse screen + shared vibe chip

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Detail screen (map preview + Ride CTA)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailScreen.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModelTest.kt` (create)

**Interfaces:**
- Consumes: `ScenicRouteRepository` (Task 3), `VibeChip` (Task 4), `MotouringMap`/`MapCamera`/`MapPolyline` (existing), `GeoPoint`.
- Produces: `ScenicRouteDetailViewModel(scenicRouteRepository, routeId)` with `route: StateFlow<ScenicRoute?>`; `ScenicRouteDetailScreen(viewModel, onRideRoute: () -> Unit)`.

- [ ] **Step 1: Write the failing VM test**

Create `app/src/test/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import com.valid.motouring.data.repository.ScenicRouteRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ScenicRouteDetailViewModelTest {

    @Test
    fun `state exposes the looked-up route`() {
        val repo = ScenicRouteRepository()
        val target = repo.routes().first()
        val vm = ScenicRouteDetailViewModel(repo, target.id)
        assertEquals(target.id, vm.route.value?.id)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.scenic.ScenicRouteDetailViewModelTest"`
Expected: FAIL — `ScenicRouteDetailViewModel` unresolved.

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModel.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ScenicRoute
import com.valid.motouring.data.repository.ScenicRouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenicRouteDetailViewModel(
    scenicRouteRepository: ScenicRouteRepository,
    routeId: String,
) : ViewModel() {

    private val _route = MutableStateFlow(scenicRouteRepository.route(routeId))
    val route: StateFlow<ScenicRoute?> = _route.asStateFlow()

    companion object {
        fun factory(scenicRouteRepository: ScenicRouteRepository, routeId: String) = viewModelFactory {
            initializer { ScenicRouteDetailViewModel(scenicRouteRepository, routeId) }
        }
    }
}
```

- [ ] **Step 4: Run to verify the VM test passes**

Run: `./gradlew testDebugUnitTest --tests "com.valid.motouring.ui.scenic.ScenicRouteDetailViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Create the detail screen**

Create `app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailScreen.kt`:

```kotlin
package com.valid.motouring.ui.scenic

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapPolyline
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.Muted

@Composable
fun ScenicRouteDetailScreen(viewModel: ScenicRouteDetailViewModel, onRideRoute: () -> Unit) {
    val route by viewModel.route.collectAsState()
    val r = route ?: return

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Image(
            painter = painterResource(id = r.heroPhotoRes),
            contentDescription = r.name,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(20.dp)) {
            Text(r.name, style = MaterialTheme.typography.headlineMedium)
            Text("${r.region} · ${"%.0f".format(r.distanceKm)} km · ${r.estimatedMinutes} min", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                r.vibe.forEach { VibeChip(it) }
            }
            Spacer(Modifier.height(14.dp))
            Text(r.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            MotouringMap(
                cameraTarget = MapCamera(r.route.firstOrNull() ?: GeoPoint(0.0, 0.0), zoom = 11.0),
                markers = emptyList(),
                polyline = MapPolyline(r.route),
                onMarkerClick = {},
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRideRoute, modifier = Modifier.fillMaxWidth()) {
                Text("Ride this route")
            }
        }
    }
}
```

- [ ] **Step 6: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModel.kt \
        app/src/main/java/com/valid/motouring/ui/scenic/ScenicRouteDetailScreen.kt \
        app/src/test/java/com/valid/motouring/ui/scenic/ScenicRouteDetailViewModelTest.kt
git commit -m "feat(scenic): scenic-route detail screen with map preview + Ride CTA

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Nav routes + FAB repurpose

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `ScenicRoutesScreen`/`ScenicRoutesViewModel`, `ScenicRouteDetailScreen`/`ScenicRouteDetailViewModel` (Tasks 4/5), `appContainer.scenicRouteRepository` (Task 3).
- Produces: `Destinations.SCENIC_ROUTES` + `SCENIC_ROUTE_DETAIL_PATTERN` + `scenicRouteDetail(id)`; the FAB "Plan a route" navigates to scenic routes.

UI/wiring — verified by build + on-device.

- [ ] **Step 1: Add the destinations**

In `Destinations.kt`, add:

```kotlin
    const val SCENIC_ROUTES = "scenic_routes"
    const val SCENIC_ROUTE_DETAIL_PATTERN = "scenic_route_detail/{routeId}"
    fun scenicRouteDetail(routeId: String) = "scenic_route_detail/$routeId"
```

- [ ] **Step 2: Add the nav composables**

In `MotouringNavHost.kt`, add two composables (near the other browse/detail routes):

```kotlin
        composable(Destinations.SCENIC_ROUTES) {
            val viewModel: ScenicRoutesViewModel = viewModel(
                factory = ScenicRoutesViewModel.factory(appContainer.scenicRouteRepository),
            )
            ScenicRoutesScreen(viewModel = viewModel, onRouteClick = { id -> navController.navigate(Destinations.scenicRouteDetail(id)) })
        }
        composable(
            Destinations.SCENIC_ROUTE_DETAIL_PATTERN,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = requireNotNull(backStackEntry.arguments?.getString("routeId"))
            val viewModel: ScenicRouteDetailViewModel = viewModel(
                factory = ScenicRouteDetailViewModel.factory(appContainer.scenicRouteRepository, routeId),
            )
            ScenicRouteDetailScreen(viewModel = viewModel, onRideRoute = { navController.navigate(Destinations.START_RIDE) })
        }
```

Add the imports near the other `ui` imports:

```kotlin
import com.valid.motouring.ui.scenic.ScenicRouteDetailScreen
import com.valid.motouring.ui.scenic.ScenicRouteDetailViewModel
import com.valid.motouring.ui.scenic.ScenicRoutesScreen
import com.valid.motouring.ui.scenic.ScenicRoutesViewModel
```

(`navArgument`, `NavType`, `viewModel`, `composable` are already imported.)

- [ ] **Step 3: Repurpose the FAB "Plan a route"**

In `MainScaffold.kt`, the FAB menu's `onPlanRoute` currently navigates to `START_RIDE`. Change it to the scenic routes browse:

```kotlin
                onPlanRoute = { fabMenuOpen = false; outerNavController.navigate(Destinations.SCENIC_ROUTES) },
```

(Leave `onStartSolo` / `onStartGroup` pointing at `START_RIDE` unchanged.)

- [ ] **Step 4: Build + full suite**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew testDebugUnitTest`
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/Destinations.kt \
        app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt \
        app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat(scenic): nav routes + FAB 'Plan a route' opens scenic routes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Final Verification (after Task 6)

- [ ] `./gradlew testDebugUnitTest` — all unit tests green (model, scenic repo, both scenic VMs, plus all pre-existing suites).
- [ ] `./gradlew assembleDebug` — headless build green.
- [ ] Push all commits: `git push origin main`.
- [ ] **On-device review by the user** (Arch host): the center FAB → "Plan a route" opens the Scenic Routes list (hero-photo cards with name/region/distance/time + vibe chips); tapping a card opens the detail (hero, description, a MapLibre route preview of the polyline, and a "Ride this route" button); "Ride this route" lands on the Start-Ride screen.
