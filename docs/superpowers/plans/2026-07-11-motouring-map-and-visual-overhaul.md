# Map & Visual Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Motouring a real map, a Strava-style center Start-Ride FAB, a rich in-ride dashboard, a live Nearby screen, and a color/photo polish pass — turning the "plain and boring" mockup into a vivid one.

**Architecture:** Add MapLibre GL Native (free, token-free OpenFreeMap tiles) behind one reusable `MotouringMap` Compose wrapper that falls back to the existing hand-drawn Canvas in previews/offline. Every map surface (in-ride, Nearby) consumes that one component. New UI keeps the existing MVVM + in-memory-repository patterns; new pure logic (simulator stats, POI distance/filtering, Nearby state) is TDD'd; Compose screens are verified by build + the user's on-device review on the Arch host.

**Tech Stack:** Kotlin, Jetpack Compose (Material3, BOM 2026.06.00), Navigation-Compose 2.8.5, MapLibre GL Native Android `org.maplibre.gl:android-sdk:13.3.1`, JUnit4 + kotlinx-coroutines-test.

## Global Constraints

- Package root `com.valid.motouring`; minSdk 26, target/compile 35, JVM 17.
- Compose-only, Material3, dark theme. No XML layouts. Follow existing `ui/<feature>/` + `ui/components/` + `ui/theme/` structure.
- Mockup only: no backend, network sync, or auth. The one network use introduced is map tiles (INTERNET permission already present in `AndroidManifest.xml`).
- Map tiles: OpenFreeMap, **no API key/token/signup**. Style URL `https://tiles.openfreemap.org/styles/dark`. Leave the unused `BuildConfig.MAPBOX_PUBLIC_TOKEN` plumbing in place for a future Mapbox swap.
- Build must stay green on the headless VM: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` with no map token configured. MapLibre rendering is verified on-device by the user, not on the VM.
- Preserve all existing in-ride behavior: goal/endless modes, celebration overlay, choice sheet, undo snackbar, drift toast, and the `RideSessionViewModel` event stream. This overhaul changes only visual framing + stat richness.
- Visual spec of record for each screen is the approved brainstorm mockups in `.superpowers/brainstorm/188932-1783774300/content/` (`bottom-bar.html`, `inride-layout.html`, `nearby-sheet.html`). Match their layout, palette, and stat sets.
- Accent-color set (Task 1) is the single source for category colors; never hardcode hex in screens.
- Commit after each task. Do not create a branch (project works direct-to-`main` per CLAUDE.md); push is the user's call.

---

## File Structure

**Create:**
- `ui/components/map/MotouringMap.kt` — the reusable map wrapper + `MapCamera`/`MapMarker`/`MapPolyline`/`MarkerStyle` types + inspection/offline fallback.
- `ui/components/map/MapViewLifecycle.kt` — `rememberMapViewWithLifecycle()` interop helper.
- `data/model/GeoMath.kt` — pure `distanceKm(a, b)` haversine util (shared by Nearby).
- `ui/nearby/NearbyScreen.kt`, `ui/nearby/NearbyViewModel.kt`, `ui/nearby/NearbyUiState.kt` — the Nearby feature.
- `ui/rides/RideDashboard.kt` — the new in-ride bottom dashboard (stat grid + goal ring + group bar), extracted so `RideSessionScreen` stays small.
- `ui/main/StartRideFab.kt` — the center FAB + quick-action menu composable.
- `app/src/main/res/CREDITS.md` — photo attributions (Task 10).
- Test files under `app/src/test/java/com/valid/motouring/...` as noted per task.

**Modify:**
- `app/build.gradle.kts` — add MapLibre dependency.
- `ui/theme/Color.kt` — add category accent colors.
- `data/model/RideSession.kt` — add `maxSpeedKmh`, `elevationGainMeters`, `toGoalMeters()`.
- `simulation/RideSimulator.kt` — accumulate max speed + elevation in `advance()`.
- `data/model/PointOfInterest.kt` — add `REST_STOP` type.
- `data/fake/FakeDataProvider.kt` — add rest-stop POIs, a `userLocation`, wire new photos.
- `ui/rides/RidePlaceholderRoute.kt` — generalize to draw markers + polyline (becomes the map fallback).
- `ui/rides/RideSessionScreen.kt` + `ui/rides/RideSessionHud.kt` — balanced-split rebuild.
- `ui/main/MainScaffold.kt` — 4-tab + center FAB bar; enable Nearby route.
- Photo drawables under `app/src/main/res/drawable/` + `FakeDataProvider` refs (Task 10).

---

## Task 1: Category accent colors

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Color.kt`

**Interfaces:**
- Produces: `MotouringColors.poiFuel`, `.poiRepair`, `.poiRest`, `.rider`, `.speaking` (all `androidx.compose.ui.graphics.Color`).

- [ ] **Step 1: Add the accent colors**

Append to `Color.kt` after the existing top-level `val`s and inside `MotouringColors`:

```kotlin
// Category accents (used by POI pins, group bar, tags). A restrained multi-hue set on top of the orange.
val PoiFuel = Color(0xFF4ADE80)     // green
val PoiRepair = AccentPrimary       // brand orange
val PoiRest = Color(0xFFF5C34B)     // amber
val RiderBlue = Color(0xFF7CB8FF)   // self / rider
val SpeakingGreen = Color(0xFF4ADE80)
```

Inside `object MotouringColors { ... }` add:

```kotlin
    val poiFuel = PoiFuel
    val poiRepair = PoiRepair
    val poiRest = PoiRest
    val rider = RiderBlue
    val speaking = SpeakingGreen
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/theme/Color.kt
git commit -m "feat: add category accent colors to theme"
```

---

## Task 2: MapLibre dependency + lifecycle-aware MapView

**Files:**
- Modify: `app/build.gradle.kts:72-74` (replace the stale "Task 15" comment block)
- Create: `app/src/main/java/com/valid/motouring/ui/components/map/MapViewLifecycle.kt`

**Interfaces:**
- Produces: `rememberMapViewWithLifecycle(): org.maplibre.android.maps.MapView` — an initialized, lifecycle-bound MapView ready to host in `AndroidView`.

- [ ] **Step 1: Add the dependency**

In `app/build.gradle.kts`, replace lines 72-74 (the Mapbox "Task 15" comment) with:

```kotlin
    // MapLibre GL Native — free vector maps via token-free OpenFreeMap tiles.
    // MapLibre is the open-source fork of Mapbox GL, so a later Mapbox swap is a dependency change.
    implementation("org.maplibre.gl:android-sdk:13.3.1")
```

- [ ] **Step 2: Create the lifecycle helper**

Create `MapViewLifecycle.kt`:

```kotlin
package com.valid.motouring.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

/** Returns a MapLibre [MapView] whose lifecycle is bound to the current LifecycleOwner. */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context) // must run before constructing a MapView
        MapView(context).apply { onCreate(null) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (dependency resolves from Maven Central; the helper compiles).

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/valid/motouring/ui/components/map/MapViewLifecycle.kt
git commit -m "feat: add MapLibre dependency and lifecycle-bound MapView helper"
```

---

## Task 3: `MotouringMap` reusable component (+ fallback)

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RidePlaceholderRoute.kt` (generalize to draw markers + polyline)
- Create: `app/src/main/java/com/valid/motouring/ui/components/map/MotouringMap.kt`

**Interfaces:**
- Consumes: `rememberMapViewWithLifecycle()` (Task 2); `GeoPoint`; `MotouringColors` (Task 1).
- Produces:
  - `data class MapCamera(val target: GeoPoint, val zoom: Double = 12.0)`
  - `enum class MarkerStyle { SELF, BUDDY, POI_FUEL, POI_REPAIR, POI_REST }`
  - `data class MapMarker(val id: String, val point: GeoPoint, val style: MarkerStyle, val selected: Boolean = false)`
  - `data class MapPolyline(val points: List<GeoPoint>)`
  - `@Composable fun MotouringMap(cameraTarget: MapCamera, markers: List<MapMarker>, polyline: MapPolyline?, onMarkerClick: (String) -> Unit, modifier: Modifier)`

- [ ] **Step 1: Generalize the Canvas fallback**

Rewrite `RidePlaceholderRoute.kt` so it can render an arbitrary set of colored markers + an optional polyline (this becomes `MotouringMap`'s inspection/offline fallback while remaining usable standalone). Keep the existing preview.

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800

data class FallbackMarker(val point: GeoPoint, val color: Color, val radiusPx: Float = 12f)

/** Hand-drawn map stand-in. Used directly, and as MotouringMap's preview/offline fallback. */
@Composable
fun RidePlaceholderRoute(
    route: List<GeoPoint>,
    markers: List<FallbackMarker>,
    modifier: Modifier = Modifier,
) {
    val allPoints = route + markers.map { it.point }
    if (allPoints.isEmpty()) return
    val minLat = allPoints.minOf { it.lat }
    val maxLat = allPoints.maxOf { it.lat }
    val minLng = allPoints.minOf { it.lng }
    val maxLng = allPoints.maxOf { it.lng }

    fun GeoPoint.toOffset(w: Float, h: Float, pad: Float): Offset {
        val x = if (maxLng == minLng) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
        val y = if (maxLat == minLat) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
        return Offset(x * w + pad, y * h + pad)
    }

    Canvas(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Charcoal800),
    ) {
        val pad = 24f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val pts = route.map { it.toOffset(w, h, pad) }
        for (i in 0 until pts.size - 1) {
            drawLine(AccentPrimary, pts[i], pts[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        markers.forEach { m -> drawCircle(m.color, radius = m.radiusPx, center = m.point.toOffset(w, h, pad)) }
        if (route.isEmpty() && markers.size >= 2) {
            // no route: hint connectivity between markers
            val mp = markers.map { it.point.toOffset(w, h, pad) }
            for (i in 0 until mp.size - 1) drawLine(Charcoal600, mp[i], mp[i + 1], strokeWidth = 3f)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidePlaceholderRoutePreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        val route = com.valid.motouring.data.fake.FakeDataProvider.sampleRoute
        RidePlaceholderRoute(
            route = route,
            markers = listOf(FallbackMarker(route[2], AccentPrimary)),
            modifier = Modifier.fillMaxSize().let { it },
        )
    }
}
```

Note: the old signature took `markerPosition: GeoPoint` and a fixed 160dp height. Callers (`RideSessionScreen`) are updated in Task 5 to the new `markers` + explicit-height form.

- [ ] **Step 2: Write `MotouringMap`**

Create `MotouringMap.kt`. Real MapView at runtime; `RidePlaceholderRoute` fallback under `LocalInspectionMode`. Route via a GeoJSON source + `LineLayer`; markers via a GeoJSON source + `CircleLayer` colored by a per-feature property; taps via `queryRenderedFeatures` on the marker layer.

```kotlin
package com.valid.motouring.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.ui.rides.FallbackMarker
import com.valid.motouring.ui.rides.RidePlaceholderRoute
import com.valid.motouring.ui.theme.MotouringColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

data class MapCamera(val target: GeoPoint, val zoom: Double = 12.0)
enum class MarkerStyle { SELF, BUDDY, POI_FUEL, POI_REPAIR, POI_REST }
data class MapMarker(val id: String, val point: GeoPoint, val style: MarkerStyle, val selected: Boolean = false)
data class MapPolyline(val points: List<GeoPoint>)

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val ROUTE_SOURCE = "route-src"
private const val ROUTE_LAYER = "route-lyr"
private const val MARKER_SOURCE = "marker-src"
private const val MARKER_LAYER = "marker-lyr"

@Composable
fun MarkerStyle.color(): ComposeColor = when (this) {
    MarkerStyle.SELF -> MotouringColors.rider
    MarkerStyle.BUDDY -> MotouringColors.poiRest
    MarkerStyle.POI_FUEL -> MotouringColors.poiFuel
    MarkerStyle.POI_REPAIR -> MotouringColors.poiRepair
    MarkerStyle.POI_REST -> MotouringColors.poiRest
}

@Composable
fun MotouringMap(
    cameraTarget: MapCamera,
    markers: List<MapMarker>,
    polyline: MapPolyline?,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        // Preview / screenshot-test / offline fallback: hand-drawn Canvas.
        val fb = markers.map { FallbackMarker(it.point, it.style.color(), if (it.selected) 18f else 12f) }
        RidePlaceholderRoute(route = polyline?.points ?: emptyList(), markers = fb, modifier = modifier)
        return
    }

    val colorsArgb = MarkerStyle.entries.associate { it.name to it.color().toArgb() }
    val mapView = rememberMapViewWithLifecycle()

    AndroidView(factory = { mapView }, modifier = modifier) { mv ->
        mv.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                renderRoute(style, polyline)
                renderMarkers(style, markers, colorsArgb)
                map.setOnMarkerLayerClick(onMarkerClick)
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(cameraTarget.target.lat, cameraTarget.target.lng))
                .zoom(cameraTarget.zoom).build()
        }
    }
}

private fun renderRoute(style: Style, polyline: MapPolyline?) {
    if (polyline == null || polyline.points.size < 2) return
    val line = LineString.fromLngLats(polyline.points.map { Point.fromLngLat(it.lng, it.lat) })
    style.addSource(GeoJsonSource(ROUTE_SOURCE, line))
    style.addLayer(
        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
            PropertyFactory.lineColor(MotouringColors.poiRepair.toArgb()),
            PropertyFactory.lineWidth(4f),
        ),
    )
}

private fun renderMarkers(style: Style, markers: List<MapMarker>, colorsArgb: Map<String, Int>) {
    val features = markers.map { m ->
        Feature.fromGeometry(Point.fromLngLat(m.point.lng, m.point.lat)).apply {
            addStringProperty("id", m.id)
            addStringProperty("style", m.style.name)
            addBooleanProperty("selected", m.selected)
        }
    }
    style.addSource(GeoJsonSource(MARKER_SOURCE, FeatureCollection.fromFeatures(features)))
    // color per feature via a match expression on the "style" property
    val stops = colorsArgb.flatMap { (name, argb) -> listOf(name, argb) }.toTypedArray()
    style.addLayer(
        CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
            PropertyFactory.circleColor(
                org.maplibre.android.style.expressions.Expression.match(
                    org.maplibre.android.style.expressions.Expression.get("style"),
                    org.maplibre.android.style.expressions.Expression.color(colorsArgb.values.first()),
                    *matchStops(colorsArgb),
                ),
            ),
            PropertyFactory.circleRadius(
                org.maplibre.android.style.expressions.Expression.switchCase(
                    org.maplibre.android.style.expressions.Expression.get("selected"),
                    org.maplibre.android.style.expressions.Expression.literal(11f),
                    org.maplibre.android.style.expressions.Expression.literal(7f),
                ),
            ),
            PropertyFactory.circleStrokeColor(android.graphics.Color.parseColor("#100E0C")),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
}

private fun matchStops(colorsArgb: Map<String, Int>) =
    colorsArgb.flatMap { (name, argb) ->
        listOf(
            org.maplibre.android.style.expressions.Expression.literal(name),
            org.maplibre.android.style.expressions.Expression.color(argb),
        )
    }.toTypedArray()

private fun MapLibreMap.setOnMarkerLayerClick(onMarkerClick: (String) -> Unit) {
    addOnMapClickListener { latLng ->
        val screen = projection.toScreenLocation(latLng)
        val feats = queryRenderedFeatures(screen, MARKER_LAYER)
        val id = feats.firstOrNull()?.getStringProperty("id")
        if (id != null) { onMarkerClick(id); true } else false
    }
}
```

Notes for the implementer:
- The `Expression.match`/`switchCase` imports are verbose; you may add `import org.maplibre.android.style.expressions.Expression` and shorten. Keep behavior identical.
- Camera changes across recompositions: for the MVP set `map.cameraPosition` on style load. Task 8 adds animated recenter on POI selection via `map.animateCamera(...)`; if you prefer, thread a `LaunchedEffect(cameraTarget)` that calls `getMapAsync { it.animateCamera(CameraUpdateFactory.newLatLngZoom(...)) }` — acceptable and encouraged.
- Marker/route sources are added once on style load. When `markers`/`polyline` change, update via `(style.getSourceAs<GeoJsonSource>(id))?.setGeoJson(...)`. For in-ride (Task 5) the buddy positions update every tick — implement the update-existing-source path there.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: On-device check (user, Arch host)**

The map can't render on the headless VM. Flag for the user: after pulling, open any screen wired to `MotouringMap` (first appears in Task 5) and confirm tiles load and the dark style shows.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/map/MotouringMap.kt app/src/main/java/com/valid/motouring/ui/rides/RidePlaceholderRoute.kt
git commit -m "feat: add MotouringMap wrapper over MapLibre with Canvas fallback"
```

---

## Task 4: In-ride model + simulator stat additions (TDD)

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/model/RideSession.kt`
- Modify: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorStatsTest.kt`

**Interfaces:**
- Consumes: existing `RideSession`, `RideSimulator.advance()`.
- Produces: `RideSession.maxSpeedKmh: Double`, `RideSession.elevationGainMeters: Double`, `RideSession.toGoalMeters(): Double`. `advance()` accumulates both new fields.

- [ ] **Step 1: Write the failing test**

Create `RideSimulatorStatsTest.kt`:

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.GoalType
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.toGoalMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorStatsTest {

    private fun session() = RideSession(
        id = "t", vehicleType = com.valid.motouring.data.model.VehicleType.MOTORCYCLE,
        route = listOf(GeoPoint(-6.22, 106.80), GeoPoint(-6.18, 106.83)),
        participants = listOf(RideParticipantState("u", "Me", 0, GeoPoint(-6.22, 106.80))),
        distanceMeters = 0.0, speedKmh = 0.0, elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `maxSpeed is the running maximum across ticks`() {
        var s = session()
        var seenMax = 0.0
        repeat(30) {
            s = RideSimulator.advance(s)
            seenMax = maxOf(seenMax, s.speedKmh)
            assertEquals(seenMax, s.maxSpeedKmh, 0.001)
        }
        assertTrue("max should be >= current speed", s.maxSpeedKmh >= s.speedKmh)
    }

    @Test
    fun `elevationGain never decreases`() {
        var s = session()
        var prev = 0.0
        repeat(30) {
            s = RideSimulator.advance(s)
            assertTrue("elevation should be monotonic non-decreasing", s.elevationGainMeters >= prev)
            prev = s.elevationGainMeters
        }
        assertTrue("some climb accumulates", s.elevationGainMeters > 0.0)
    }

    @Test
    fun `toGoalMeters is remaining distance, floored at zero`() {
        val goal = RideGoal(GoalType.DISTANCE, "5 km", targetDistanceMeters = 5000.0)
        val s = session().copy(mode = RideMode.GOAL, activeGoal = goal, distanceMeters = 3000.0)
        assertEquals(2000.0, s.toGoalMeters(), 0.001)
        val past = s.copy(distanceMeters = 6000.0)
        assertEquals(0.0, past.toGoalMeters(), 0.001)
        assertEquals(0.0, session().toGoalMeters(), 0.001) // no goal -> 0
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*RideSimulatorStatsTest*"`
Expected: FAIL — `maxSpeedKmh` / `elevationGainMeters` / `toGoalMeters` unresolved.

- [ ] **Step 3: Add the fields + derivation**

In `RideSession.kt`, add two fields to the `RideSession` data class (after `completedLegs`):

```kotlin
    val maxSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
```

And add the extension after `avgSpeedKmh(...)`:

```kotlin
fun RideSession.toGoalMeters(): Double =
    activeGoal?.let { (it.targetDistanceMeters - distanceMeters).coerceAtLeast(0.0) } ?: 0.0
```

- [ ] **Step 4: Accumulate in `advance()`**

In `RideSimulator.advance()`, within the `advanced` construction, add the two accumulations. Replace the `val advanced = current.copy(...)` block with:

```kotlin
            val elevationDelta = (2.0 + 1.5 * sin(newElapsed / 7.0)).coerceAtLeast(0.0)
            val advanced = current.copy(
                elapsedSeconds = newElapsed,
                distanceMeters = newDistance,
                speedKmh = speed,
                participants = newParticipants,
                maxSpeedKmh = maxOf(current.maxSpeedKmh, speed),
                elevationGainMeters = current.elevationGainMeters + elevationDelta,
            )
```

(`sin` is already imported in `RideSimulator.kt`.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*RideSimulatorStatsTest*"`
Expected: PASS (3 tests). Also run the full suite to confirm nothing regressed: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/RideSession.kt app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt app/src/test/java/com/valid/motouring/simulation/RideSimulatorStatsTest.kt
git commit -m "feat: track max speed, elevation gain, and to-goal distance in ride session"
```

---

## Task 5: In-ride balanced-split rebuild

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideDashboard.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt`

**Interfaces:**
- Consumes: `MotouringMap`, `MapCamera`, `MapMarker`, `MapPolyline`, `MarkerStyle` (Task 3); `RideSession.maxSpeedKmh/elevationGainMeters/toGoalMeters()` (Task 4); existing `InstrumentRing`, `StatBlock`, `avgSpeedKmh`, `activeLegDurationSeconds`.
- Produces: `@Composable fun RideDashboard(session: RideSession, modifier: Modifier)`.

Visual spec: `.superpowers/brainstorm/188932-1783774300/content/inride-layout.html`, option **C** (map top ~55%, dashboard bottom ~45%: floating speed on map; goal ring + 6-stat grid + group/voice bar below).

- [ ] **Step 1: Build the dashboard composable**

Create `RideDashboard.kt`. The 6 stats: Distance, Avg, Elapsed, Max, Climb, To-goal. Group bar: overlapping colored-initial avatars + speaking indicator (green pulse) from `isSpeaking`.

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.activeLegDurationSeconds
import com.valid.motouring.data.model.avgSpeedKmh
import com.valid.motouring.data.model.toGoalMeters
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

private val avatarColors = listOf(
    Color(0xFF7CB8FF), Color(0xFFF5C34B), Color(0xFF8B7BE8), Color(0xFF4ADE80), Color(0xFFFF8A65),
)
private fun colorFor(id: String) = avatarColors[(id.hashCode() and 0x7fffffff) % avatarColors.size]

@Composable
fun RideDashboard(session: RideSession, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val goal = session.activeGoal
            if (session.mode == RideMode.GOAL && goal != null) {
                val progress = (session.distanceMeters / goal.targetDistanceMeters).toFloat().coerceIn(0f, 1f)
                InstrumentRing(progress = progress, size = 72.dp) {
                    Text("%.1f".format(session.toGoalMeters() / 1000.0), style = MotouringTextStyles.statValue)
                    Text("KM LEFT", style = MotouringTextStyles.statLabel, color = Muted)
                }
            } else {
                InstrumentRing(progress = 0f, size = 72.dp) {
                    Text("${session.activeLegDurationSeconds() / 60}", style = MotouringTextStyles.statValue)
                    Text("MIN", style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
            Spacer(Modifier.width(12.dp))
            StatGrid(session, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        GroupBar(session.participants)
    }
}

@Composable
private fun StatGrid(session: RideSession, modifier: Modifier = Modifier) {
    val cells = listOf(
        "Dist" to "%.1f".format(session.distanceMeters / 1000.0),
        "Avg" to "${avgSpeedKmh(session.distanceMeters, session.elapsedSeconds).toInt()}",
        "Time" to "${session.elapsedSeconds / 60}:${(session.elapsedSeconds % 60).toString().padStart(2, '0')}",
        "Max" to "${session.maxSpeedKmh.toInt()}",
        "Climb" to "${session.elevationGainMeters.toInt()}",
        "Goal" to if (session.activeGoal != null) "%.1f".format(session.toGoalMeters() / 1000.0) else "—",
    )
    Column(modifier = modifier.clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(vertical = 6.dp)) {
        cells.chunked(3).forEach { rowCells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowCells.forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(4.dp)) {
                        Text(value, style = MotouringTextStyles.statValue)
                        Text(label.uppercase(), style = MotouringTextStyles.statLabel, color = Muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupBar(participants: List<RideParticipantState>) {
    val speaker = participants.firstOrNull { it.isSpeaking }
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Charcoal800).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            participants.take(4).forEachIndexed { i, p ->
                Box(
                    modifier = Modifier.offset(x = (i * -8).dp).size(26.dp).clip(CircleShape).background(colorFor(p.userId)),
                    contentAlignment = Alignment.Center,
                ) { Text(p.name.take(1), color = Color(0xFF100E0C), fontWeight = FontWeight.Bold) }
            }
            if (participants.size > 4) {
                Box(Modifier.offset(x = (4 * -8).dp).size(26.dp).clip(CircleShape).background(Charcoal700), contentAlignment = Alignment.Center) {
                    Text("+${participants.size - 4}", style = MotouringTextStyles.statLabel, color = Muted)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (speaker != null) {
            val t = rememberInfiniteTransition(label = "speak")
            val a by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "speakA")
            Box(Modifier.size(8.dp).clip(CircleShape).background(MotouringColors.speaking.copy(alpha = a)))
            Spacer(Modifier.width(6.dp))
            Text("${speaker.name} speaking", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
```

- [ ] **Step 2: Slim down `RideSessionHud` to the map + floating speed**

Repurpose `RideSessionHud.kt` into the map region: it renders `MotouringMap` (route polyline, self + buddy markers) with a floating speed readout and small status pills. Replace the whole file body's `RideSessionHud` composable with:

```kotlin
@Composable
fun RideSessionHud(session: RideSession, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val markers = session.participants.mapIndexed { i, p ->
            MapMarker(
                id = p.userId,
                point = p.position,
                style = if (i == 0) MarkerStyle.SELF else MarkerStyle.BUDDY,
            )
        }
        MotouringMap(
            cameraTarget = MapCamera(session.participants.first().position, zoom = 13.0),
            markers = markers,
            polyline = MapPolyline(session.route),
            onMarkerClick = {},
            modifier = Modifier.matchParentSize(),
        )
        Text(
            "${session.speedKmh.toInt()}",
            style = MotouringTextStyles.statValueLarge,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        )
        Text(
            "KM/H",
            style = MotouringTextStyles.statLabel, color = Muted,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 8.dp),
        )
        session.activeGoal?.let {
            Text(
                "→ ${it.label}",
                style = MaterialTheme.typography.labelLarge, color = MotouringColors.poiRepair,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }
    }
}
```

Update the imports at the top of `RideSessionHud.kt` accordingly (remove the now-unused InstrumentRing/StatBlock/goal-glow imports; add `com.valid.motouring.ui.components.map.*`, `MotouringColors`, `matchParentSize` comes from Box scope). Keep the two `@Preview` functions but pass sessions through `MotouringMap`'s fallback (they run under `LocalInspectionMode`, so they render the Canvas — fine).

- [ ] **Step 3: Recompose `RideSessionScreen` into the balanced split**

In `RideSessionScreen.kt`, replace the inner `Column { RideSessionHud(...); RidePlaceholderRoute(...); Button... }` (lines ~79-93) with a weighted split and a compact control row. Keep ALL overlay logic (celebration/sheet/undo/drift) and `LaunchedEffect`s unchanged.

```kotlin
        Column(modifier = Modifier.fillMaxSize()) {
            RideSessionHud(session = session, modifier = Modifier.weight(0.55f).fillMaxWidth())
            Column(modifier = Modifier.weight(0.45f).fillMaxWidth()) {
                RideDashboard(session = session)
                Spacer(Modifier.weight(1f))
                RideDebugControls(
                    showSetGoal = session.mode == RideMode.ENDLESS,
                    onSetGoal = { showChoiceSheet = true; showDriftToast = false },
                    onDrift = { viewModel.simulateDrift() },
                    onEnd = { onEndRide(viewModel.endRide()) },
                )
            }
        }
```

And add a small controls composable at the bottom of `RideSessionScreen.kt` (these are demo affordances, kept but visually quiet per the spec):

```kotlin
@Composable
private fun RideDebugControls(
    showSetGoal: Boolean,
    onSetGoal: () -> Unit,
    onDrift: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSetGoal) TextButton(onClick = onSetGoal) { Text("Set goal") }
        TextButton(onClick = onDrift) { Text("Off-route") }
        Spacer(Modifier.weight(1f))
        Button(onClick = onEnd) { Text("End Ride") }
    }
}
```

Update `RideSessionScreen.kt` imports: remove the old `RidePlaceholderRoute` call, `padding(16.dp)` on the outer column; add `Arrangement`, `Spacer`, `TextButton`, `fillMaxWidth`. The `Box(Modifier.fillMaxSize())` wrapper and all overlay `if` blocks stay as-is.

- [ ] **Step 4: Build + full test suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL; existing tests green.

- [ ] **Step 5: On-device check (user)**

Flag: user starts a ride on the host and confirms the map fills the top, the 6-stat dashboard + goal ring + group bar show below, the speaker pulse animates, and the goal/drift/celebration flows still work via the quiet control row.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideDashboard.kt app/src/main/java/com/valid/motouring/ui/rides/RideSessionHud.kt app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt
git commit -m "feat: rebuild in-ride screen as balanced map+dashboard split"
```

---

## Task 6: POI model + rest-stops + distance util (TDD)

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/model/PointOfInterest.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/GeoMath.kt`
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`
- Test: `app/src/test/java/com/valid/motouring/data/GeoMathTest.kt`

**Interfaces:**
- Produces: `PoiType.REST_STOP`; `fun distanceKm(a: GeoPoint, b: GeoPoint): Double`; `FakeDataProvider.userLocation: GeoPoint`; rest-stop POIs in `FakeDataProvider.pois`.

- [ ] **Step 1: Write the failing test**

Create `GeoMathTest.kt`:

```kotlin
package com.valid.motouring.data

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.distanceKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    @Test
    fun `distance between identical points is zero`() {
        val p = GeoPoint(-6.20, 106.82)
        assertEquals(0.0, distanceKm(p, p), 0.0001)
    }

    @Test
    fun `distance is symmetric and reasonable for nearby Jakarta points`() {
        val a = GeoPoint(-6.2246, 106.8091)
        val b = GeoPoint(-6.1875, 106.8271)
        val ab = distanceKm(a, b)
        val ba = distanceKm(b, a)
        assertEquals(ab, ba, 0.0001)
        // ~4-5 km apart; assert a sane range, not an exact value
        assertTrue("expected 3-6 km, got $ab", ab in 3.0..6.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*GeoMathTest*"`
Expected: FAIL — `distanceKm` unresolved.

- [ ] **Step 3: Add `GeoMath.kt`**

```kotlin
package com.valid.motouring.data.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance between two points, in kilometers. */
fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
    val earthRadiusKm = 6_371.0
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * earthRadiusKm * asin(sqrt(h))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "*GeoMathTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Add the REST_STOP type + data**

In `PointOfInterest.kt`, change the enum to:

```kotlin
enum class PoiType { GAS_STATION, REPAIR_SHOP, REST_STOP }
```

In `FakeDataProvider.kt`, add a `userLocation` near `sampleRoute` (use the route midpoint area):

```kotlin
    val userLocation = GeoPoint(lat = -6.2088, lng = 106.8206)
```

And append rest-stop POIs to the `pois` list (before its closing `)`):

```kotlin
        PointOfInterest("p-7", "Warung Rindu Alam", PoiType.REST_STOP, GeoPoint(lat = -6.2015, lng = 106.8180), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.5),
        PointOfInterest("p-8", "Kopi Titik Temu", PoiType.REST_STOP, GeoPoint(lat = -6.2200, lng = 106.8250), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.7),
```

- [ ] **Step 6: Build + full suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all tests green. (Note: adding an enum constant can make a `when` over `PoiType` non-exhaustive. Search for `PoiType.` usages and add a `REST_STOP` branch anywhere the compiler flags. As of this plan no UI consumes `PoiType` yet; the Nearby screen in Task 8 handles all three.)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/PointOfInterest.kt app/src/main/java/com/valid/motouring/data/model/GeoMath.kt app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt app/src/test/java/com/valid/motouring/data/GeoMathTest.kt
git commit -m "feat: add rest-stop POIs, user location, and distance util"
```

---

## Task 7: Nearby ViewModel (TDD)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/nearby/NearbyUiState.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/nearby/NearbyViewModel.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/nearby/NearbyViewModelTest.kt`

**Interfaces:**
- Consumes: `PoiRepository.observePois()`, `PointOfInterest`, `PoiType`, `distanceKm`, `FakeDataProvider.userLocation`.
- Produces:
  - `enum class PoiFilter { ALL, FUEL, REPAIR, REST }`
  - `data class NearbyPoi(val poi: PointOfInterest, val distanceKm: Double, val selected: Boolean)`
  - `data class NearbyUiState(val items: List<NearbyPoi>, val filter: PoiFilter, val selectedId: String?, val cameraTarget: GeoPoint)`
  - `NearbyViewModel(poiRepository, userLocation)` with `state: StateFlow<NearbyUiState>`, `fun setFilter(f)`, `fun select(id)`, `fun clearSelection()`, and `companion object { fun factory(...) }`.

- [ ] **Step 1: Write the failing test**

Create `NearbyViewModelTest.kt`:

```kotlin
package com.valid.motouring.ui.nearby

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.repository.PoiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyViewModelTest {

    private fun vm() = NearbyViewModel(PoiRepository(), FakeDataProvider.userLocation)

    @Test
    fun `default state lists all pois sorted by ascending distance`() {
        val state = vm().state.value
        assertEquals(PoiFilter.ALL, state.filter)
        assertEquals(FakeDataProvider.pois.size, state.items.size)
        val distances = state.items.map { it.distanceKm }
        assertEquals(distances.sorted(), distances)
    }

    @Test
    fun `filter REPAIR keeps only repair shops`() {
        val vm = vm()
        vm.setFilter(PoiFilter.REPAIR)
        val state = vm.state.value
        assertTrue(state.items.isNotEmpty())
        assertTrue(state.items.all { it.poi.type == com.valid.motouring.data.model.PoiType.REPAIR_SHOP })
    }

    @Test
    fun `selecting a poi sets selectedId, marks it, and moves the camera to it`() {
        val vm = vm()
        val target = vm.state.value.items[1].poi
        vm.select(target.id)
        val state = vm.state.value
        assertEquals(target.id, state.selectedId)
        assertEquals(target.location, state.cameraTarget)
        assertTrue(state.items.first { it.poi.id == target.id }.selected)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*NearbyViewModelTest*"`
Expected: FAIL — classes unresolved.

- [ ] **Step 3: Implement the state + ViewModel**

Create `NearbyUiState.kt`:

```kotlin
package com.valid.motouring.ui.nearby

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PointOfInterest

enum class PoiFilter { ALL, FUEL, REPAIR, REST }

data class NearbyPoi(val poi: PointOfInterest, val distanceKm: Double, val selected: Boolean)

data class NearbyUiState(
    val items: List<NearbyPoi>,
    val filter: PoiFilter,
    val selectedId: String?,
    val cameraTarget: GeoPoint,
)
```

Create `NearbyViewModel.kt`:

```kotlin
package com.valid.motouring.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.distanceKm
import com.valid.motouring.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyViewModel(
    private val poiRepository: PoiRepository,
    private val userLocation: GeoPoint,
) : ViewModel() {

    private val allPois: List<PointOfInterest> = poiRepository.observePois().value
    private var filter = PoiFilter.ALL
    private var selectedId: String? = null

    private val _state = MutableStateFlow(build())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    fun setFilter(f: PoiFilter) { filter = f; _state.value = build() }

    fun select(id: String) {
        selectedId = id
        _state.value = build()
    }

    fun clearSelection() { selectedId = null; _state.value = build() }

    private fun build(): NearbyUiState {
        val filtered = allPois
            .filter { poi ->
                when (filter) {
                    PoiFilter.ALL -> true
                    PoiFilter.FUEL -> poi.type == PoiType.GAS_STATION
                    PoiFilter.REPAIR -> poi.type == PoiType.REPAIR_SHOP
                    PoiFilter.REST -> poi.type == PoiType.REST_STOP
                }
            }
            .map { NearbyPoi(it, distanceKm(userLocation, it.location), it.id == selectedId) }
            .sortedBy { it.distanceKm }
        val camera = selectedId?.let { id -> allPois.firstOrNull { it.id == id }?.location } ?: userLocation
        return NearbyUiState(items = filtered, filter = filter, selectedId = selectedId, cameraTarget = camera)
    }

    companion object {
        fun factory(poiRepository: PoiRepository) = viewModelFactory {
            initializer { NearbyViewModel(poiRepository, FakeDataProvider.userLocation) }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*NearbyViewModelTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/nearby/NearbyUiState.kt app/src/main/java/com/valid/motouring/ui/nearby/NearbyViewModel.kt app/src/test/java/com/valid/motouring/ui/nearby/NearbyViewModelTest.kt
git commit -m "feat: add NearbyViewModel with filtering, distance sort, and selection"
```

---

## Task 8: Nearby screen (map + draggable sheet)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/nearby/NearbyScreen.kt`

**Interfaces:**
- Consumes: `NearbyViewModel`, `NearbyUiState`, `PoiFilter`, `NearbyPoi` (Task 7); `MotouringMap`, `MapCamera`, `MapMarker`, `MarkerStyle` (Task 3); `PoiType`, `MotouringColors`.
- Produces: `@Composable fun NearbyScreen(viewModel: NearbyViewModel)`.

Visual spec: `.superpowers/brainstorm/188932-1783774300/content/nearby-sheet.html` — full-screen map, filter chips pinned top, `BottomSheetScaffold` list with a peek height; tapping a pin or card recenters the map (via `NearbyViewModel.select`, which moves `cameraTarget`) and marks the pin.

- [ ] **Step 1: Implement the screen**

```kotlin
package com.valid.motouring.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.ui.components.map.MapCamera
import com.valid.motouring.ui.components.map.MapMarker
import com.valid.motouring.ui.components.map.MarkerStyle
import com.valid.motouring.ui.components.map.MotouringMap
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.Muted

private fun PoiType.markerStyle() = when (this) {
    PoiType.GAS_STATION -> MarkerStyle.POI_FUEL
    PoiType.REPAIR_SHOP -> MarkerStyle.POI_REPAIR
    PoiType.REST_STOP -> MarkerStyle.POI_REST
}

private fun PoiType.color() = when (this) {
    PoiType.GAS_STATION -> MotouringColors.poiFuel
    PoiType.REPAIR_SHOP -> MotouringColors.poiRepair
    PoiType.REST_STOP -> MotouringColors.poiRest
}

private fun PoiType.emoji() = when (this) {
    PoiType.GAS_STATION -> "⛽"
    PoiType.REPAIR_SHOP -> "🔧"
    PoiType.REST_STOP -> "🍜"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(viewModel: NearbyViewModel) {
    val state by viewModel.state.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val listState = rememberLazyListState()

    // recenter + reveal on selection
    LaunchedEffect(state.selectedId) {
        val idx = state.items.indexOfFirst { it.selected }
        if (idx >= 0) {
            scaffoldState.bottomSheetState.partialExpand()
            listState.animateScrollToItem(idx)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetContainerColor = Charcoal800,
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("${state.items.size} places nearby", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(state = listState) {
                    items(state.items, key = { it.poi.id }) { np -> PoiCard(np, onClick = { viewModel.select(np.poi.id) }) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val markers = state.items.map {
                MapMarker(it.poi.id, it.poi.location, it.poi.type.markerStyle(), selected = it.selected)
            }
            MotouringMap(
                cameraTarget = MapCamera(state.cameraTarget, zoom = 13.0),
                markers = markers,
                polyline = null,
                onMarkerClick = { id -> viewModel.select(id) },
                modifier = Modifier.matchParentSize(),
            )
            FilterRow(state.filter, onSelect = viewModel::setFilter, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(current: PoiFilter, onSelect: (PoiFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        listOf(
            PoiFilter.ALL to "All", PoiFilter.FUEL to "⛽ Fuel",
            PoiFilter.REPAIR to "🔧 Repair", PoiFilter.REST to "🍜 Food",
        ).forEach { (f, label) ->
            FilterChip(selected = current == f, onClick = { onSelect(f) }, label = { Text(label) }, modifier = Modifier.padding(end = 6.dp))
        }
    }
}

@Composable
private fun PoiCard(np: NearbyPoi, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(13.dp))
            .background(if (np.selected) np.poi.type.color().copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickableNoRipple(onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(np.poi.type.color().copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(np.poi.type.emoji())
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(np.poi.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row {
                Text("★ %.1f".format(np.poi.rating), style = MotouringTextStyles.statLabel, color = MotouringColors.poiRest)
                Spacer(Modifier.width(8.dp))
                Text("%.1f km".format(np.distanceKm), style = MotouringTextStyles.statLabel, color = Muted)
            }
        }
    }
}
```

Add the small helpers used above (place at the bottom of the file):

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
```

(If `clickable` with default ripple is acceptable, use it directly and drop the helper. The named helper only documents intent.)

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. If `partialExpand()` is flagged, ensure `ExperimentalMaterial3Api` opt-in is present (it is on `NearbyScreen`).

- [ ] **Step 3: On-device check (user)**

Flag: user opens Nearby (enabled in Task 9), drags the sheet peek→expanded, taps a pin and a card, and confirms the map recenters to that POI and the pin highlights — matching Google Maps behavior.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/nearby/NearbyScreen.kt
git commit -m "feat: add Nearby screen with map, filter chips, and draggable POI sheet"
```

---

## Task 9: Center Start-Ride FAB + enable Nearby tab

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/main/StartRideFab.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `NearbyScreen`, `NearbyViewModel` (Tasks 7-8); `appContainer.poiRepository`; `Destinations.START_RIDE`.
- Produces: `@Composable fun StartRideFab(onStartSolo, onStartGroup, onPlanRoute)` and the 4-tab + FAB bottom bar.

Visual spec: `.superpowers/brainstorm/188932-1783774300/content/bottom-bar.html`, option **B** — tap toggles a quick-action menu (Start Solo / Start Group / Plan a route); FAB icon ▶ → ✕.

- [ ] **Step 1: Build the FAB + menu**

Create `StartRideFab.kt`:

```kotlin
package com.valid.motouring.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.MotouringMotion

@Composable
fun StartRideFab(
    onStartSolo: () -> Unit,
    onStartGroup: () -> Unit,
    onPlanRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 45f else 0f, MotouringMotion.press(), label = "fabRot")

    Box(modifier) {
        // The FAB button itself
        Surface(
            color = AccentPrimary,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.size(60.dp).clickable { open = !open },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (open) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = "Start ride",
                    tint = Color(0xFF100E0C),
                    modifier = Modifier.rotate(if (open) 0f else rot),
                )
            }
        }
    }

    // The menu (scrim + items) is hosted by the caller as a full-screen overlay:
    if (open) {
        RideActionMenu(
            onDismiss = { open = false },
            onStartSolo = { open = false; onStartSolo() },
            onStartGroup = { open = false; onStartGroup() },
            onPlanRoute = { open = false; onPlanRoute() },
        )
    }
}

@Composable
private fun RideActionMenu(
    onDismiss: () -> Unit,
    onStartSolo: () -> Unit,
    onStartGroup: () -> Unit,
    onPlanRoute: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(onClick = onDismiss)) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuItem("Plan a route", onPlanRoute)
                    MenuItem("Start group ride", onStartGroup)
                    MenuItem("Start solo ride", onStartSolo)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Surface(
        color = com.valid.motouring.ui.theme.Charcoal700,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) {
            Text(label, color = com.valid.motouring.ui.theme.OffWhite)
        }
    }
}
```

Note: the FAB overlay is drawn full-screen when open; because `MainScaffold` hosts it inside the `Scaffold` content `Box`, the scrim covers the tab content. Ensure the FAB+menu is composed in a top-level `Box` that overlays the whole scaffold (see Step 2).

- [ ] **Step 2: Rebuild the bottom bar in `MainScaffold`**

Replace the `NavigationBar { ... }` in the `Scaffold`'s `bottomBar` with a custom bar showing 4 tabs split 2+2. Wrap the whole `Scaffold` in a `Box` so the FAB + menu overlay sits above everything. Enable the Nearby route in the nested `NavHost`, and remove the `implementedTabRoutes` gate.

Key changes:
1. Delete the `implementedTabRoutes` set and the `enabled = tab.route in implementedTabRoutes` argument.
2. Define the tab order as two groups around the FAB:

```kotlin
private val leftTabs = listOf(BottomTab.Home, BottomTab.Nearby)
private val rightTabs = listOf(BottomTab.Rides, BottomTab.Profile)
```

(Challenges moves off the bar — it remains reachable from its existing detail routes; if you prefer to keep 5 destinations, place Challenges under Profile's stack. For this spec the bar is Home · Nearby · FAB · Rides · Profile, matching the mockup.)

3. Structure:

```kotlin
@Composable
fun MainScaffold(appContainer: AppContainer, outerNavController: NavHostController) {
    val tabNavController = rememberNavController()
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                MotouringBottomBar(
                    tabNavController = tabNavController,
                    onFabStartSolo = { outerNavController.navigate(Destinations.START_RIDE) },
                    onFabStartGroup = { outerNavController.navigate(Destinations.START_RIDE) },
                    onFabPlanRoute = { outerNavController.navigate(Destinations.START_RIDE) },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = tabNavController,
                startDestination = BottomTab.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = MotouringMotion.comfy()) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
            ) {
                // ... existing Home / Challenges / Rides / Profile composable() entries unchanged ...
                composable(BottomTab.Nearby.route) {
                    val vm: NearbyViewModel = viewModel(factory = NearbyViewModel.factory(appContainer.poiRepository))
                    NearbyScreen(viewModel = vm)
                }
            }
        }
    }
}
```

4. `MotouringBottomBar` renders the two tab groups + the center FAB. Use a `Row` with the four `NavigationBarItem`-style tabs and a spacer gap in the middle; overlay `StartRideFab` centered, raised above the bar (negative offset). Reuse the existing selected-scale animation per tab (`animateFloatAsState` on icon, `MotouringMotion.press()`). Implement the tab click/selected logic exactly as the current `NavigationBarItem` block does (same `navigate` with `popUpTo(findStartDestination)`, `launchSingleTop`, `restoreState`, and the `currentDestination?.hierarchy?.any { it.route == tab.route }` selection check). Keep it a straightforward custom `Row`; the FAB's `onFab*` callbacks are threaded from `MainScaffold`.

```kotlin
@Composable
private fun MotouringBottomBar(
    tabNavController: NavHostController,
    onFabStartSolo: () -> Unit,
    onFabStartGroup: () -> Unit,
    onFabPlanRoute: () -> Unit,
) {
    val currentDestination = tabNavController.currentBackStackEntryAsState().value?.destination
    fun go(tab: BottomTab) = tabNavController.navigate(tab.route) {
        popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    Box {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                leftTabs.forEach { TabItem(it, currentDestination, ::go) }
                Spacer(Modifier.width(64.dp)) // room for the FAB
                rightTabs.forEach { TabItem(it, currentDestination, ::go) }
            }
        }
        StartRideFab(
            onStartSolo = onFabStartSolo,
            onStartGroup = onFabStartGroup,
            onPlanRoute = onFabPlanRoute,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-22).dp),
        )
    }
}
```

(Implement `TabItem` as a small column with the scaled `Icon` + `Text(label)`, colored by selection using `MaterialTheme.colorScheme.onSurface` vs `MutedDim`, reusing the existing animation. Add the necessary imports: `Row`, `Spacer`, `height`, `width`, `offset`, `Surface`, `Box`, `Arrangement`, `Alignment`, and the Nearby imports.)

- [ ] **Step 3: Build + full suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: On-device check (user)**

Flag: user confirms the center FAB opens the 3-action menu (▶→✕), Start Solo/Group jump to Start Ride, and the Nearby tab now opens the new screen.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/main/StartRideFab.kt app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat: center Start-Ride FAB with quick-action menu; enable Nearby tab"
```

---

## Task 10: Bundled CC0 photos + wiring

**Files:**
- Create: `app/src/main/res/drawable/img_road_1.jpg` … `img_road_6.jpg`, `img_vehicle_car.jpg`, `img_vehicle_moto.jpg` (downloaded)
- Create: `app/src/main/res/CREDITS.md`
- Modify: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt` (point post/vehicle/summary image refs at the new drawables)

**Interfaces:**
- Consumes: existing `@DrawableRes Int` fields on models (`Post.photoRes`/`photoResList`, `Vehicle` image, `RideHistoryEntry.routePreviewRes`/`photoResList`). No new API — swaps resource ids.

- [ ] **Step 1: Download CC0 photos**

Lorem Picsum serves Unsplash photos under the Unsplash License (free commercial use, no attribution required); deterministic by seed. From the repo root run:

```bash
mkdir -p app/src/main/res/drawable
for i in 1 2 3 4 5 6; do
  curl -sL "https://picsum.photos/seed/motouring-road-$i/800/500.jpg" -o "app/src/main/res/drawable/img_road_$i.jpg"
done
curl -sL "https://picsum.photos/seed/motouring-car/800/500.jpg" -o app/src/main/res/drawable/img_vehicle_car.jpg
curl -sL "https://picsum.photos/seed/motouring-moto/800/500.jpg" -o app/src/main/res/drawable/img_vehicle_moto.jpg
```

Verify each file is a valid non-empty image:

```bash
file app/src/main/res/drawable/img_road_*.jpg app/src/main/res/drawable/img_vehicle_*.jpg
```

Expected: each reports `JPEG image data`. (Android drawable filenames must be lowercase `a-z0-9_` — the names above comply. If any download is empty, re-run; do not commit zero-byte files.)

- [ ] **Step 2: Add CREDITS.md**

Create `app/src/main/res/CREDITS.md`:

```markdown
# Image credits

Placeholder photography via Lorem Picsum (https://picsum.photos), which serves
photos from Unsplash under the Unsplash License (https://unsplash.com/license) —
free for commercial and non-commercial use, no permission or attribution required.
Bundled here purely as mockup placeholders:

- img_road_1..6.jpg — scenery/road placeholders (feed cards, ride-summary hero)
- img_vehicle_car.jpg, img_vehicle_moto.jpg — vehicle placeholders
```

- [ ] **Step 3: Wire the drawables into fake data**

In `FakeDataProvider.kt`, replace `R.drawable.ic_photo_placeholder` references on posts/ride-summary and the vehicle image placeholders with the new drawables (cycle through `img_road_1..6` for feed/hero images; use `img_vehicle_car` / `img_vehicle_moto` by vehicle type). Keep `ic_avatar_placeholder` for people (avatars stay as generated initials elsewhere; the resource ref is harmless). Search for `ic_photo_placeholder`, `ic_vehicle_car_placeholder`, `ic_vehicle_motorcycle_placeholder`, `ic_route_preview_placeholder` in `FakeDataProvider.kt` and repoint each image/photo field. Example edits:

```kotlin
// posts: was R.drawable.ic_photo_placeholder
photoRes = R.drawable.img_road_1,   // vary: img_road_1..6 across the seeded posts
// vehicles:
imageRes = R.drawable.img_vehicle_moto,   // or img_vehicle_car for cars
```

(Use whatever the actual field names are on `Post`/`Vehicle`/`RideHistoryEntry` — grep confirms them. Do NOT change `RideSessionViewModel`'s `routePreviewRes = R.drawable.ic_route_preview_placeholder`; the route-thumbnail replacement is the optional stretch deferred out of this spec.)

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (new drawables resolve; no unresolved `R.drawable.*`).

- [ ] **Step 5: On-device check (user)**

Flag: user confirms feed cards, ride-summary hero, and vehicle rows now show real photos and the app reads colorful rather than flat.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/img_road_*.jpg app/src/main/res/drawable/img_vehicle_*.jpg app/src/main/res/CREDITS.md app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt
git commit -m "feat: bundle CC0 placeholder photos and wire them into fake data"
```

---

## Final Verification

- [ ] `./gradlew assembleDebug testDebugUnitTest` — both green on the VM.
- [ ] Push to `origin/main` (with the user's go-ahead per CLAUDE.md) so the Arch host can pull.
- [ ] User on-device pass on the host: FAB menu, in-ride split + live map + stats + speaker pulse, Nearby map + sheet + tap-to-recenter, category colors + photos throughout. Note any style/aesthetic tweaks (esp. the OpenFreeMap `dark` style fidelity — the maintainer flags it as not-yet-complete; if it looks off, a later polish task can bundle a recolored style JSON over `https://tiles.openfreemap.org/planet`).

## Self-Review notes (author)

- **Spec coverage:** map foundation (T2-3), FAB+menu (T9), enable Nearby (T9), in-ride split + 6 stats + group/voice bar (T4-5), Nearby sheet + recenter (T6-8), accent colors (T1), photos (T10). All spec §3-7 items map to a task. Charcoal map style: satisfied by OpenFreeMap `dark` (hosted), with bundled-style restyle noted as deferred/optional per spec §7 stretch.
- **Type consistency:** `MapMarker`/`MapCamera`/`MapPolyline`/`MarkerStyle` defined in T3 and consumed unchanged in T5/T8; `distanceKm` defined T6, used T7; `NearbyViewModel.factory(poiRepository)` defined T7, called T9; `RideSession.toGoalMeters()`/`maxSpeedKmh`/`elevationGainMeters` defined T4, used T5.
- **Deferred (not in this plan):** the four niche-feature themes (spec §9), route-thumbnail map snapshots (spec §7 stretch), a bundled recolored map style (spec §10).
