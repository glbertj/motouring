# Motouring "Analog Dash" Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every currently-built screen and shared component in the Motouring mockup with the "Analog Dash" design system — instrument-cluster palette, a circular gauge-ring signature component, a Space Grotesk/Inter/IBM Plex Mono type system, and soft spring-based motion — with no changes to data models, navigation structure, or business logic.

**Architecture:** Pure restyle on top of the existing MVVM/Compose architecture. `ui/theme/*` is rebuilt (colors, type, shape, motion tokens), a handful of new shared components are added (`InstrumentRing`, `MotouringCard`, motion helpers), existing shared components are rebuilt against the new tokens, and every screen is updated to use them. No repository, ViewModel, navigation route, or data model changes anywhere in this plan.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (existing `androidx.compose.material3:material3` — no new Gradle dependencies needed; custom fonts use Compose's built-in `res/font` support, no downloadable-fonts API since that would require network access).

## Global Constraints

- Package id: `com.valid.motouring`. Kotlin + Jetpack Compose + Material 3 only.
- No new data models, no new navigation destinations, no new screens, no ViewModel/repository logic changes. This plan only touches `ui/theme/*`, `ui/components/*`, screen Composables, `MotouringNavHost.kt`, `MainScaffold.kt`, `BottomTab.kt`, and 6 `res/drawable/*.xml` files.
- Color tokens: `Charcoal950 #100E0C`, `Charcoal900 #15130F`, `Charcoal800 #1A1714`, `Charcoal700 #241E19`, `Charcoal600 #2A2522`, `Charcoal500 #3D3632`, `AccentPrimary #FF5A36`, `OffWhite #F5F1EC`, `Muted #A89F97`, `MutedDim #7A8087`. No secondary/tertiary accent hues.
- Type: Space Grotesk (headlines/titles/buttons), Inter (body/labels), IBM Plex Mono (numeric stat readouts only, via `MotouringTextStyles`, never via `MaterialTheme.typography`).
- Shape scale: 10dp (chips/pills), 14dp (buttons/list items), 18dp (cards/sheets). No sharp corners.
- No Material tonal elevation, no drop shadows on dark surfaces — elevated surfaces use a subtle vertical gradient fill + 1dp `Charcoal600` hairline border.
- Motion default: spring-based ("comfy"), `dampingRatio = 0.7f`, `stiffness = Spring.StiffnessLow` for entrances/ring fills; `dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium` for press feedback. No linear/tween easing anywhere in this system.
- The 6 data-bound placeholder drawables (`ic_avatar_placeholder`, `ic_vehicle_motorcycle_placeholder`, `ic_vehicle_car_placeholder`, `ic_route_preview_placeholder`, `ic_photo_placeholder`, `ic_badge_placeholder`) are redrawn in place, same resource names — `Badge.iconRes`, `Vehicle.photoRes`, `RideHistoryEntry.routePreviewRes`, `Post.photoResList`, `User.avatarRes` are plain `Int` fields, so no data model changes are needed or allowed here.
- UI-chrome icons (bottom nav, like/comment/send) stay as existing `Icons.Filled.*` Material glyphs — not redrawn in this phase (see design spec's Iconography section for why).
- `Nearby` and `Ride Session` screens do not exist yet (blocked on the Mapbox token, itself not yet in `build.gradle.kts`) — out of scope for this entire plan.
- Verify each task with `./gradlew :app:compileDebugKotlin` (or `:app:assembleDebug` for the two tasks touching `res/font` or `res/drawable`, since Kotlin-only compilation doesn't validate resource merging).
- No automated test suite for this phase (styling/motion only, no logic) — Compose `@Preview`s are the verification mechanism, consistent with the original plan's approach. Screens that take a `ViewModel` directly still don't get previews in this phase (matches the existing pattern already in the codebase).

---

## File Structure

```
app/src/main/res/font/
  space_grotesk.ttf              (variable font, weight instanced via FontVariation)
  inter.ttf                      (variable font, weight instanced via FontVariation)
  ibm_plex_mono_medium.ttf
  ibm_plex_mono_bold.ttf
app/src/main/res/drawable/            (6 files redrawn in place, same names)
app/src/main/java/com/valid/motouring/
  ui/theme/Color.kt                   (rebuilt)
  ui/theme/Type.kt                    (rebuilt)
  ui/theme/Theme.kt                   (rebuilt)
  ui/theme/Motion.kt                  (new)
  ui/components/PressScale.kt         (new)
  ui/components/StaggeredEntrance.kt  (new)
  ui/components/InstrumentRing.kt     (new)
  ui/components/MotouringCard.kt      (new)
  ui/components/StatBlock.kt          (rebuilt)
  ui/components/SectionHeader.kt      (rebuilt)
  ui/components/RideBuddyAvatarRow.kt (rebuilt)
  ui/components/BadgeChip.kt          (rebuilt)
  ui/components/PostCard.kt           (rebuilt)
  navigation/MotouringNavHost.kt      (modified: root Surface + transitions)
  ui/main/MainScaffold.kt             (modified: tab cross-fade)
  ui/main/BottomTab.kt                (unchanged type, icons stay Material)
  ui/onboarding/*.kt                  (3 screens restyled)
  ui/vehicle/VehicleGarageSetupScreen.kt (restyled)
  ui/home/HomeScreen.kt               (restyled)
  ui/challenges/*.kt                  (4 screens restyled)
  ui/rides/*.kt                       (3 screens restyled)
  ui/profile/*.kt                     (4 screens restyled)
  ui/social/*.kt                      (4 screens restyled)
```

---

## Phase A — Design Tokens Foundation

### Task 1: Bundle custom fonts and rebuild the type system

**Files:**
- Create: `app/src/main/res/font/space_grotesk.ttf`, `app/src/main/res/font/inter.ttf` (variable fonts — verified only variable-font sources exist for these two families on the canonical Google Fonts repo, see Step 1)
- Create: `app/src/main/res/font/ibm_plex_mono_medium.ttf`, `ibm_plex_mono_bold.ttf` (static — verified available as separate per-weight files)
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Type.kt`

**Interfaces:**
- Produces: `SpaceGrotesk`, `Inter`, `IbmPlexMono` (`FontFamily`), `MotouringTypography` (`Typography`, same slot names as before: headlineMedium/titleLarge/titleMedium/bodyLarge/bodyMedium/labelSmall), `MotouringTextStyles` object with `statValue`, `statValueLarge`, `statLabel` (`TextStyle`) — consumed by Task 6 (`StatBlock`) and later screen tasks.

- [ ] **Step 1: Download the font files and place them in `res/font/`**

Space Grotesk and Inter are published upstream only as variable fonts (single file, weight axis) — there is no static per-weight TTF in the canonical source. IBM Plex Mono does have static per-weight files. Download exactly these three files (verified reachable):

```bash
curl -sL -o app/src/main/res/font/space_grotesk.ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/spacegrotesk/SpaceGrotesk%5Bwght%5D.ttf"
curl -sL -o app/src/main/res/font/inter.ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz,wght%5D.ttf"
curl -sL -o app/src/main/res/font/ibm_plex_mono_medium.ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/IBMPlexMono-Medium.ttf"
curl -sL -o app/src/main/res/font/ibm_plex_mono_bold.ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/IBMPlexMono-Bold.ttf"
```

Verify each downloaded file is a real font, not an error page (Git LFS pointers or HTML error bodies are a few hundred bytes; real TTFs here are hundreds of KB to a few MB):

```bash
ls -la app/src/main/res/font/
file app/src/main/res/font/*.ttf
```

Expected: `file` reports each as `TrueType Font data` (or similar), and all four are well over 10KB.

- [ ] **Step 2: Rewrite `Type.kt`** (variable-font weights are selected via `FontVariation.Settings`, not separate files)

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.valid.motouring.R

val SpaceGrotesk = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val Inter = FontFamily(
    Font(
        R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold),
)

val MotouringTypography = Typography(
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
)

object MotouringTextStyles {
    val statValue = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    val statValueLarge = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    val statLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
}
```

- [ ] **Step 3: Verify it builds (resource merging needs a full assemble, not just Kotlin compile)**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/font/ app/src/main/java/com/valid/motouring/ui/theme/Type.kt
git commit -m "feat: bundle Space Grotesk/Inter/IBM Plex Mono and rebuild type system"
```

---

### Task 2: Rebuild color tokens, theme, and shape scale

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `Charcoal950/900/800/700/600/500`, `AccentPrimary`, `OffWhite`, `Muted`, `MutedDim` (`Color`); `MotouringColors` object with `ringTrack`, `ringProgress`, `ringTick`, `ringGlow`, `badgeLocked`, `badgeEarned`, `liked`; `MotouringTheme { }` composable applying `MotouringColorScheme` + `MotouringTypography` + `MotouringShapes`. Every later task's screens/components reference these names verbatim.

- [ ] **Step 1: Rewrite `Color.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.ui.graphics.Color

val Charcoal950 = Color(0xFF100E0C)
val Charcoal900 = Color(0xFF15130F)
val Charcoal800 = Color(0xFF1A1714)
val Charcoal700 = Color(0xFF241E19)
val Charcoal600 = Color(0xFF2A2522)
val Charcoal500 = Color(0xFF3D3632)
val AccentPrimary = Color(0xFFFF5A36)
val OffWhite = Color(0xFFF5F1EC)
val Muted = Color(0xFFA89F97)
val MutedDim = Color(0xFF7A8087)

object MotouringColors {
    val ringTrack = Charcoal600
    val ringProgress = AccentPrimary
    val ringTick = Charcoal500
    val ringGlow = Charcoal700
    val badgeLocked = Charcoal600
    val badgeEarned = AccentPrimary
    val liked = AccentPrimary
}
```

- [ ] **Step 2: Rewrite `Theme.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MotouringColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = Charcoal950,
    secondary = AccentPrimary,
    onSecondary = Charcoal950,
    background = Charcoal900,
    onBackground = OffWhite,
    surface = Charcoal800,
    onSurface = OffWhite,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = Muted,
    outline = Charcoal600,
    error = AccentPrimary,
    onError = Charcoal950,
)

private val MotouringShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun MotouringTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotouringColorScheme,
        typography = MotouringTypography,
        shapes = MotouringShapes,
        content = content,
    )
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/theme/Color.kt app/src/main/java/com/valid/motouring/ui/theme/Theme.kt
git commit -m "feat: rebuild Analog Dash color tokens, theme, and shape scale"
```

---

### Task 3: Motion tokens and reusable motion helpers

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/theme/Motion.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/PressScale.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/StaggeredEntrance.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `MotouringMotion.comfy<T>()`, `MotouringMotion.press<T>()` (both `SpringSpec<T>`), `MotouringMotion.staggerDelayMs` (`Long`); `Modifier.pressScale(interactionSource: InteractionSource)`; `StaggeredEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit)` composable. Consumed by `MotouringCard` (Task 5), `InstrumentRing` (Task 4), and every screen task from Task 11 onward.

- [ ] **Step 1: Create `Motion.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object MotouringMotion {
    fun <T> comfy(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    const val staggerDelayMs = 70L
}
```

- [ ] **Step 2: Create `PressScale.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import com.valid.motouring.ui.theme.MotouringMotion

fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MotouringMotion.press(),
        label = "pressScale",
    )
    this.scale(scale)
}
```

- [ ] **Step 3: Create `StaggeredEntrance.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.valid.motouring.ui.theme.MotouringMotion
import kotlinx.coroutines.delay

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * MotouringMotion.staggerDelayMs)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = MotouringMotion.comfy()) +
            slideInVertically(animationSpec = MotouringMotion.comfy(), initialOffsetY = { it / 6 }),
        modifier = modifier,
    ) {
        content()
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/theme/Motion.kt app/src/main/java/com/valid/motouring/ui/components/PressScale.kt app/src/main/java/com/valid/motouring/ui/components/StaggeredEntrance.kt
git commit -m "feat: add spring-based motion tokens and reusable motion helpers"
```

---

## Phase B — Signature Component

### Task 4: InstrumentRing — the gauge-ring signature component

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/components/InstrumentRing.kt`

**Interfaces:**
- Consumes: `MotouringColors` (Task 2), `MotouringMotion` (Task 3).
- Produces: `InstrumentRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 64.dp, strokeWidth: Dp = 4.dp, showTicks: Boolean = size >= 48.dp, showGlow: Boolean = size >= 56.dp, content: @Composable () -> Unit = {})` — replaces every `LinearProgressIndicator` from Task 14 onward.

- [ ] **Step 1: Create `InstrumentRing.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.MotouringColors
import com.valid.motouring.ui.theme.MotouringMotion
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InstrumentRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 4.dp,
    showTicks: Boolean = size >= 48.dp,
    showGlow: Boolean = size >= 56.dp,
    content: @Composable () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MotouringMotion.comfy(),
        label = "ringProgress",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val radius = (size.toPx() - stroke) / 2f
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

            if (showGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MotouringColors.ringGlow.copy(alpha = 0.5f), MotouringColors.ringGlow.copy(alpha = 0f)),
                        center = center,
                        radius = radius * 1.3f,
                    ),
                    radius = radius * 1.3f,
                    center = center,
                )
            }

            drawCircle(
                color = MotouringColors.ringTrack,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                color = MotouringColors.ringProgress,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (showTicks) {
                val tickLength = stroke * 1.5f
                for (angleDeg in listOf(0f, 90f, 180f, 270f)) {
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val outer = Offset(
                        center.x + (radius + stroke) * cos(angleRad).toFloat(),
                        center.y + (radius + stroke) * sin(angleRad).toFloat(),
                    )
                    val inner = Offset(
                        center.x + (radius + stroke - tickLength) * cos(angleRad).toFloat(),
                        center.y + (radius + stroke - tickLength) * sin(angleRad).toFloat(),
                    )
                    drawLine(color = MotouringColors.ringTick, start = inner, end = outer, strokeWidth = 1.5.dp.toPx())
                }
            }
        }
        content()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun InstrumentRingPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        InstrumentRing(progress = 0.62f, size = 64.dp) {
            androidx.compose.material3.Text(
                text = "62%",
                style = com.valid.motouring.ui.theme.MotouringTextStyles.statValue,
            )
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/InstrumentRing.kt
git commit -m "feat: add InstrumentRing signature gauge component"
```

---

## Phase C — Icon Redraws

### Task 5: Redraw the 6 data-bound placeholder drawables

**Files:**
- Modify: `app/src/main/res/drawable/ic_avatar_placeholder.xml`
- Modify: `app/src/main/res/drawable/ic_vehicle_motorcycle_placeholder.xml`
- Modify: `app/src/main/res/drawable/ic_vehicle_car_placeholder.xml`
- Modify: `app/src/main/res/drawable/ic_route_preview_placeholder.xml`
- Modify: `app/src/main/res/drawable/ic_photo_placeholder.xml`
- Modify: `app/src/main/res/drawable/ic_badge_placeholder.xml`

**Interfaces:**
- Consumes: nothing (plain resource files).
- Produces: same resource ids as before (`R.drawable.ic_avatar_placeholder`, etc.) — no changes needed anywhere these are referenced (`FakeDataProvider.kt`, `VehicleGarageViewModel.kt`).

- [ ] **Step 1: Redraw `ic_avatar_placeholder.xml`** (simple rounded-shoulder silhouette, line style)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#2A2522"
        android:pathData="M0,0h24v24h-24z" />
    <path
        android:strokeColor="#A89F97"
        android:strokeWidth="1.4"
        android:pathData="M12,6.5m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0" />
    <path
        android:strokeColor="#A89F97"
        android:strokeWidth="1.4"
        android:pathData="M6,19c0,-3.3 2.7,-6 6,-6s6,2.7 6,6" />
</vector>
```

- [ ] **Step 2: Redraw `ic_vehicle_motorcycle_placeholder.xml`** (two wheels + frame line, geometric)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.4"
        android:pathData="M5,17m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0" />
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.4"
        android:pathData="M19,17m-2.5,0a2.5,2.5 0,1 1,5 0a2.5,2.5 0,1 1,-5 0" />
    <path
        android:strokeColor="#F5F1EC"
        android:strokeWidth="1.4"
        android:pathData="M5,17L10,10L14,10L11,17M14,10L17,17M10,10L8,7L11,7" />
</vector>
```

- [ ] **Step 3: Redraw `ic_vehicle_car_placeholder.xml`** (rounded body + two wheels, geometric)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.4"
        android:pathData="M6.5,16m-1.8,0a1.8,1.8 0,1 1,3.6 0a1.8,1.8 0,1 1,-3.6 0" />
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.4"
        android:pathData="M17.5,16m-1.8,0a1.8,1.8 0,1 1,3.6 0a1.8,1.8 0,1 1,-3.6 0" />
    <path
        android:strokeColor="#F5F1EC"
        android:strokeWidth="1.4"
        android:pathData="M4,16v-2.5L6,9h12l2,4.5V16M4,16h16" />
</vector>
```

- [ ] **Step 4: Redraw `ic_route_preview_placeholder.xml`** (contour-style winding route lines)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="80dp"
    android:viewportWidth="120"
    android:viewportHeight="80">
    <path android:fillColor="#241E19" android:pathData="M0,0h120v80h-120z" />
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="2.5"
        android:pathData="M10,60 C40,10 60,70 110,20" />
    <path
        android:strokeColor="#3D3632"
        android:strokeWidth="1.5"
        android:pathData="M0,45 C35,25 70,55 120,35" />
</vector>
```

- [ ] **Step 5: Redraw `ic_photo_placeholder.xml`** (mountain/sun line motif)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#241E19" android:pathData="M0,0h24v24h-24z" />
    <path
        android:strokeColor="#3D3632"
        android:strokeWidth="1.4"
        android:pathData="M4,17L9,10L13,14L16,11L20,17z" />
    <path
        android:strokeColor="#A89F97"
        android:strokeWidth="1.4"
        android:pathData="M16,6m-1.5,0a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0" />
</vector>
```

- [ ] **Step 6: Redraw `ic_badge_placeholder.xml`** (ring medallion instead of a filled star)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="56dp"
    android:height="56dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.6"
        android:pathData="M12,10m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0" />
    <path
        android:strokeColor="#FF5A36"
        android:strokeWidth="1.6"
        android:pathData="M9,14.5L7,21L12,18.5L17,21L15,14.5" />
    <path
        android:strokeColor="#F5F1EC"
        android:strokeWidth="1.6"
        android:pathData="M9,10L11,12L15,7.5" />
</vector>
```

- [ ] **Step 7: Verify resources merge and app builds**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/drawable/
git commit -m "feat: redraw placeholder drawables as thin-line geometric icons"
```

---

## Phase D — Shared Components Rebuild

### Task 6: MotouringCard — shared card container

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/components/MotouringCard.kt`

**Interfaces:**
- Consumes: `Charcoal600/700/900` (Task 2), `Modifier.pressScale` (Task 3).
- Produces: `MotouringCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` — drop-in replacement for Material `Card` at every call site from Task 13 onward (same content-lambda shape as `Card`, so call sites only rename `Card(` → `MotouringCard(`).

- [ ] **Step 1: Create `MotouringCard.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal700
import com.valid.motouring.ui.theme.Charcoal900

private val cardShape = RoundedCornerShape(18.dp)

@Composable
fun MotouringCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var base = modifier
        .clip(cardShape)
        .background(Brush.verticalGradient(listOf(Charcoal700, Charcoal900)))
        .border(1.dp, Charcoal600, cardShape)

    if (onClick != null) {
        base = base
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    }

    Column(modifier = base, content = content)
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/MotouringCard.kt
git commit -m "feat: add MotouringCard shared container (gradient fill, hairline border, press scale)"
```

---

### Task 7: Rebuild StatBlock and SectionHeader

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/components/StatBlock.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/SectionHeader.kt`

**Interfaces:**
- Consumes: `MotouringTextStyles` (Task 1), `MutedDim` (Task 2).
- Produces: same public signatures as before (`StatBlock(label, value, modifier)`, `SectionHeader(title, modifier, actionLabel, onActionClick)`) — every existing call site keeps working unmodified.

- [ ] **Step 1: Rewrite `StatBlock.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.valid.motouring.ui.theme.MotouringTextStyles
import com.valid.motouring.ui.theme.MutedDim

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MotouringTextStyles.statValue, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label.uppercase(), style = MotouringTextStyles.statLabel, color = MutedDim)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StatBlockPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StatBlock(label = "Distance", value = "18.4 km")
    }
}
```

- [ ] **Step 2: Rewrite `SectionHeader.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) { Text(actionLabel) }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = {})
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/StatBlock.kt app/src/main/java/com/valid/motouring/ui/components/SectionHeader.kt
git commit -m "feat: restyle StatBlock with mono readouts and tighten SectionHeader"
```

---

### Task 8: Rebuild RideBuddyAvatarRow and BadgeChip

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/components/RideBuddyAvatarRow.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/BadgeChip.kt`

**Interfaces:**
- Consumes: `Charcoal600/800` (Task 2), `InstrumentRing` (Task 4), redrawn `ic_badge_placeholder` (Task 5).
- Produces: same public signatures as before — `RideBuddyAvatarRow(avatarResList, modifier, maxVisible)`, `BadgeChip(badge, onClick, modifier)`.

- [ ] **Step 1: Rewrite `RideBuddyAvatarRow.kt`** (thin ring border per avatar instead of bare padding)

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.Charcoal800
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun RideBuddyAvatarRow(
    avatarResList: List<Int>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4,
) {
    Row(modifier = modifier) {
        avatarResList.take(maxVisible).forEach { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Charcoal600, CircleShape)
                    .padding(2.dp),
            )
        }
        val overflow = avatarResList.size - maxVisible
        if (overflow > 0) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Charcoal800)
                    .border(1.5.dp, Charcoal600, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+$overflow", style = MotouringTextStyles.statLabel)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideBuddyAvatarRowPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideBuddyAvatarRow(
            avatarResList = com.valid.motouring.data.fake.FakeDataProvider.users.map { it.avatarRes },
        )
    }
}
```

- [ ] **Step 2: Rewrite `BadgeChip.kt`** (ring wraps the medallion instead of alpha-dimming)

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.theme.Charcoal600
import com.valid.motouring.ui.theme.MotouringColors

@Composable
fun BadgeChip(badge: Badge, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InstrumentRing(
            progress = if (badge.isEarned) 1f else 0f,
            size = 56.dp,
            strokeWidth = 2.5.dp,
            showTicks = false,
            showGlow = badge.isEarned,
        ) {
            Image(
                painter = painterResource(id = badge.iconRes),
                contentDescription = badge.title,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeChipPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeChip(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first(), onClick = {})
    }
}
```

Note: `MotouringColors`/`Charcoal600` imports are unused in this file as written — remove them if the linter flags unused imports (kept here only if a future tweak wants a locked-badge tint; the ring track/progress colors already come from `InstrumentRing` itself).

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/RideBuddyAvatarRow.kt app/src/main/java/com/valid/motouring/ui/components/BadgeChip.kt
git commit -m "feat: restyle RideBuddyAvatarRow and BadgeChip with ring-based treatment"
```

---

### Task 9: Rebuild PostCard

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/components/PostCard.kt`

**Interfaces:**
- Consumes: `MotouringCard` (Task 6), `MotouringColors.liked` (Task 2).
- Produces: same signature as before — `PostCard(post, onLikeClick, onCardClick, modifier)`.

- [ ] **Step 1: Rewrite `PostCard.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Post
import com.valid.motouring.ui.theme.MotouringColors

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MotouringCard(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = post.authorAvatarRes),
                    contentDescription = post.authorName,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = post.authorName, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (post.photoResList.isNotEmpty()) {
                Image(
                    painter = painterResource(id = post.photoResList.first()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.likedByMe) MotouringColors.liked else LocalContentColor.current,
                    )
                }
                Text(text = "${post.likeCount}")
                Spacer(modifier = Modifier.width(16.dp))
                Icon(imageVector = Icons.Filled.ChatBubbleOutline, contentDescription = "Comments")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${post.commentIds.size}")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        PostCard(post = com.valid.motouring.data.fake.FakeDataProvider.posts.first(), onLikeClick = {}, onCardClick = {})
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/PostCard.kt
git commit -m "feat: restyle PostCard onto MotouringCard"
```

---

## Phase E — Navigation Motion

### Task 10: Spring-based screen transitions, root background, and tab switch motion

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `MotouringMotion` (Task 3), `MaterialTheme.colorScheme.background` (Task 2).
- Produces: no signature changes — `MotouringNavHost` and `MainScaffold` keep the same public parameters; every `composable()` route gains enter/exit transitions and the whole nav tree renders on an explicit background `Surface` for the first time (previously relied on the Activity theme's window background only).

- [ ] **Step 1: Add a root `Surface` and default transitions in `MotouringNavHost.kt`**

Add these imports alongside the existing ones:

```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.valid.motouring.ui.theme.MotouringMotion
```

Wrap the existing `NavHost(...)` body in a `Surface`, and give it default enter/exit transitions (individual `composable()` calls keep their existing content untouched — only the `NavHost(...)` call signature changes):

```kotlin
@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = Destinations.SPLASH,
            enterTransition = {
                slideInHorizontally(animationSpec = MotouringMotion.comfy(), initialOffsetX = { it / 3 }) +
                    fadeIn(animationSpec = tween(220))
            },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = {
                slideOutHorizontally(animationSpec = MotouringMotion.comfy(), targetOffsetX = { it / 3 }) +
                    fadeOut(animationSpec = tween(180))
            },
        ) {
            // ... every existing composable(...) block below is unchanged ...
        }
    }
}
```

- [ ] **Step 2: Apply the same transitions to the nested tab `NavHost` in `MainScaffold.kt`**

Add the same imports used above (`tween`, `fadeIn`, `fadeOut`, `MotouringMotion`) and update the inner `NavHost(...)` call:

```kotlin
NavHost(
    navController = tabNavController,
    startDestination = BottomTab.Home.route,
    modifier = Modifier.padding(innerPadding),
    enterTransition = { fadeIn(animationSpec = MotouringMotion.comfy()) },
    exitTransition = { fadeOut(animationSpec = tween(150)) },
) {
    // ... existing composable(...) blocks unchanged ...
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat: add spring-based screen transitions and explicit background Surface"
```

---

## Phase F — Screen Restyles

### Task 11: Onboarding flow (Splash, Onboarding, Login)

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/SplashScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/LoginScreen.kt`

**Interfaces:**
- Consumes: theme tokens only (Tasks 1-2). No parameter changes to any of the three screens.

- [ ] **Step 1: Rewrite `SplashScreen.kt`** (fade-in wordmark instead of an instant static Text)

```kotlin
package com.valid.motouring.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.valid.motouring.ui.theme.MotouringMotion
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MotouringMotion.comfy(),
        label = "splashAlpha",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1200)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Motouring",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.alpha(alpha),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SplashScreen(onTimeout = {})
    }
}
```

- [ ] **Step 2: Rewrite `OnboardingScreen.kt`** (accent dots use `AccentPrimary`, no other structural change)

```kotlin
package com.valid.motouring.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.theme.AccentPrimary
import com.valid.motouring.ui.theme.Charcoal600
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        "Ride Together",
        "Start group rides, see your ride buddies live on the map, and talk over voice while you ride.",
    ),
    OnboardingPage(
        "Track Every Ride",
        "Distance, speed, and route — tracked automatically for motorcycles and cars.",
    ),
    OnboardingPage(
        "Earn Badges & Challenges",
        "Take on challenges like riding 100km in a week and collect badges as you go.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val item = onboardingPages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = item.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(onboardingPages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) AccentPrimary else Charcoal600,
                            shape = CircleShape,
                        ),
                )
            }
        }

        if (pagerState.currentPage == onboardingPages.lastIndex) {
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                Text("Get Started")
            }
        } else {
            Button(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        OnboardingScreen(onFinished = {})
    }
}
```

- [ ] **Step 3: `LoginScreen.kt` needs no code change** — it already uses only `MaterialTheme.typography`/default `Button`/`OutlinedTextField`, all of which now inherit the new theme automatically. Confirm this by re-reading the file and checking there's no hardcoded color/shape; if there is none (there isn't, per the current source), skip to Step 4.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/onboarding/
git commit -m "feat: restyle onboarding flow with fade-in splash and accent dots"
```

---

### Task 12: Vehicle Garage Setup screen

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleGarageSetupScreen.kt`

**Interfaces:**
- Consumes: theme tokens only. No parameter changes.

- [ ] **Step 1: Confirm no code change is needed** — `VehicleGarageSetupScreen.kt` uses only `MaterialTheme.typography`, default `Button`/`FilterChip`/`OutlinedTextField`, all theme-driven. Re-read the file to confirm there's no hardcoded `Color(...)` or shape value (there isn't, per the current source read during planning). No diff required.

- [ ] **Step 2: Verify it still compiles after all prior theme changes**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

(No commit needed — this task makes no changes. If a future review finds this screen actually needs a tweak once seen on-device, fold it into Task 20's final smoke-test pass instead of reopening this task.)

---

### Task 13: Home screen

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `MotouringCard` (Task 6), `InstrumentRing` (Task 4), `StaggeredEntrance` (Task 3), `PostCard`/`SectionHeader` (Tasks 7, 9).
- Produces: same signature — `HomeScreen(viewModel, onStartRideClick, onPostClick, onCreatePostClick)`.

- [ ] **Step 1: Rewrite `HomeScreen.kt`**

```kotlin
package com.valid.motouring.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.PostCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartRideClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
) {
    val posts by viewModel.posts.collectAsState()
    val featuredChallenge by viewModel.featuredChallenge.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Button(onClick = onStartRideClick, modifier = Modifier.fillMaxWidth()) {
                Text("Start Group Ride")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        featuredChallenge?.let { challenge ->
            item {
                MotouringCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(challenge.title.uppercase(), style = MotouringTextStyles.statLabel)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = challenge.currentValue.toInt().toString(),
                                    style = MotouringTextStyles.statValue,
                                )
                                Text(
                                    text = "/${challenge.goalValue.toInt()} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        InstrumentRing(
                            progress = (challenge.currentValue / challenge.goalValue).toFloat(),
                            size = 64.dp,
                        ) {
                            Text(
                                text = "${(challenge.currentValue / challenge.goalValue * 100).toInt()}%",
                                style = MotouringTextStyles.statLabel,
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = onCreatePostClick)
        }
        itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                PostCard(
                    post = post,
                    onLikeClick = { viewModel.toggleLike(post.id) },
                    onCardClick = { onPostClick(post.id) },
                )
            }
        }
    }
}
```

Note: this replaces `items(posts, key = { it.id })` with `itemsIndexed` (needs `import androidx.compose.foundation.lazy.itemsIndexed` instead of `items`) so each post can be given its stagger index.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/home/HomeScreen.kt
git commit -m "feat: restyle Home screen with InstrumentRing challenge card and staggered feed"
```

---

### Task 14: Challenges + Challenge Detail

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengesScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengeDetailScreen.kt`

**Interfaces:**
- Consumes: `MotouringCard`, `InstrumentRing`, `StaggeredEntrance`.
- Produces: same signatures as before for both screens.

- [ ] **Step 1: Rewrite `ChallengesScreen.kt`**

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel,
    onChallengeClick: (String) -> Unit,
    onSeeAllBadgesClick: () -> Unit,
    onBadgeClick: (String) -> Unit,
) {
    val challenges by viewModel.challenges.collectAsState()
    val badges by viewModel.badges.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { SectionHeader(title = "Active Challenges") }
        itemsIndexed(challenges, key = { _, it -> it.id }) { index, challenge ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                ChallengeRow(challenge = challenge, onClick = { onChallengeClick(challenge.id) })
            }
        }
        item {
            SectionHeader(title = "Badges", actionLabel = "See All", onActionClick = onSeeAllBadgesClick)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                badges.take(4).forEach { badge ->
                    BadgeChip(
                        badge = badge,
                        onClick = { onBadgeClick(badge.id) },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeRow(challenge: Challenge, onClick: () -> Unit) {
    MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = challenge.title, style = MaterialTheme.typography.titleMedium)
                Text(text = challenge.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                    style = MotouringTextStyles.statLabel,
                )
            }
            InstrumentRing(progress = (challenge.currentValue / challenge.goalValue).toFloat(), size = 48.dp)
        }
    }
}
```

- [ ] **Step 2: Rewrite `ChallengeDetailScreen.kt`**

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.ui.components.InstrumentRing
import com.valid.motouring.ui.components.StaggeredEntrance
import com.valid.motouring.ui.theme.MotouringTextStyles

@Composable
fun ChallengeDetailScreen(challenge: Challenge) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(text = challenge.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = challenge.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            InstrumentRing(
                progress = (challenge.currentValue / challenge.goalValue).toFloat(),
                size = 96.dp,
            ) {
                Text(
                    text = "${(challenge.currentValue / challenge.goalValue * 100).toInt()}%",
                    style = MotouringTextStyles.statValueLarge,
                )
            }
            Text(
                text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                style = MotouringTextStyles.statLabel,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Leaderboard", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(challenge.leaderboard) { index, entry ->
            StaggeredEntrance(index = index) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "#${index + 1}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = entry.avatarRes),
                        contentDescription = entry.name,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = entry.name, modifier = Modifier.weight(1f))
                    Text(text = entry.progressValue.toInt().toString(), style = MotouringTextStyles.statValue)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallengeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        ChallengeDetailScreen(challenge = com.valid.motouring.data.fake.FakeDataProvider.challenges.first())
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/challenges/ChallengesScreen.kt app/src/main/java/com/valid/motouring/ui/challenges/ChallengeDetailScreen.kt
git commit -m "feat: restyle Challenges and Challenge Detail with InstrumentRing progress"
```

---

### Task 15: Badges + Badge Detail

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/BadgesScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/BadgeDetailScreen.kt`

**Interfaces:**
- Consumes: `BadgeChip` (Task 8, already ring-based — `BadgesScreen` needs only a stagger wrapper), `InstrumentRing` (Task 4) for the detail screen's large medallion.

- [ ] **Step 1: Rewrite `BadgesScreen.kt`** (grid entrance stagger by index)

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun BadgesScreen(badges: List<Badge>, onBadgeClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        itemsIndexed(badges, key = { _, it -> it.id }) { index, badge ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(8.dp)) {
                BadgeChip(badge = badge, onClick = { onBadgeClick(badge.id) })
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgesScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgesScreen(badges = com.valid.motouring.data.fake.FakeDataProvider.badges, onBadgeClick = {})
    }
}
```

- [ ] **Step 2: Rewrite `BadgeDetailScreen.kt`** (large InstrumentRing medallion instead of alpha-dimmed image)

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.components.InstrumentRing

@Composable
fun BadgeDetailScreen(badge: Badge) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InstrumentRing(
            progress = if (badge.isEarned) 1f else 0f,
            size = 120.dp,
            strokeWidth = 5.dp,
            showGlow = badge.isEarned,
        ) {
            Image(
                painter = painterResource(id = badge.iconRes),
                contentDescription = badge.title,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = badge.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = badge.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Unlock criteria: ${badge.unlockCriteria}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (badge.isEarned) "Earned" else "Not yet earned",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeDetailScreen(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first())
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/challenges/BadgesScreen.kt app/src/main/java/com/valid/motouring/ui/challenges/BadgeDetailScreen.kt
git commit -m "feat: restyle Badges grid and Badge Detail with InstrumentRing medallion"
```

---

### Task 16: Rides History, Start Ride, Ride Summary

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt`

**Interfaces:**
- Consumes: `MotouringCard` (Task 6), `StaggeredEntrance` (Task 3). `StatBlock`/`BadgeChip`/`SectionHeader` already updated (Tasks 7-8) so `RideSummaryScreen` needs no direct changes to those calls.

- [ ] **Step 1: Rewrite `RidesHistoryScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun RidesHistoryScreen(history: List<RideHistoryEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(history, key = { _, it -> it.id }) { index, entry ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 12.dp)) {
                RideHistoryCard(entry)
            }
        }
    }
}

@Composable
private fun RideHistoryCard(entry: RideHistoryEntry) {
    MotouringCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = entry.routePreviewRes),
                contentDescription = entry.title,
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatBlock(label = "Distance", value = "${"%.1f".format(entry.distanceMeters / 1000.0)} km")
                StatBlock(label = "Duration", value = "${entry.durationSeconds / 60} min")
                StatBlock(label = "Avg Speed", value = "${entry.avgSpeedKmh.toInt()} km/h")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidesHistoryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RidesHistoryScreen(history = com.valid.motouring.data.fake.FakeDataProvider.rideHistory)
    }
}
```

- [ ] **Step 2: `StartRideScreen.kt` needs no code change** — it uses only default `Button`/`FilterChip`/`OutlinedButton`/`MaterialTheme.typography`, all theme-driven with no hardcoded colors/shapes. Re-confirm during implementation; skip to Step 3 if still true.

- [ ] **Step 3: `RideSummaryScreen.kt` needs no code change** — it already composes `StatBlock`, `BadgeChip`, `SectionHeader` (all restyled in Tasks 7-8) with no hardcoded colors/shapes of its own. Re-confirm during implementation; skip to Step 4 if still true.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt
git commit -m "feat: restyle Rides History onto MotouringCard with staggered entrance"
```

---

### Task 17: Profile + Edit Profile

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/EditProfileScreen.kt`

**Interfaces:**
- Consumes: `MotouringCard`, `StaggeredEntrance`. No parameter changes.

- [ ] **Step 1: Rewrite `ProfileScreen.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onFriendsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val totalRides by viewModel.totalRides.collectAsState()
    val totalDistanceKm by viewModel.totalDistanceKm.collectAsState()
    val badges by viewModel.badges.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = viewModel.currentUser.avatarRes),
                    contentDescription = viewModel.currentUser.name,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Text(text = viewModel.currentUser.name, style = MaterialTheme.typography.headlineMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatBlock(label = "Rides", value = totalRides.toString())
                StatBlock(label = "Total Distance", value = "${"%.1f".format(totalDistanceKm)} km")
                StatBlock(label = "Badges", value = badges.count { it.isEarned }.toString())
            }

            SectionHeader(title = "My Garage")
        }
        itemsIndexed(vehicles) { index, vehicle ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 8.dp)) {
                MotouringCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = vehicle.photoRes),
                            contentDescription = "${vehicle.make} ${vehicle.model}",
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text(text = "${vehicle.year} ${vehicle.make} ${vehicle.model}")
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Badges")
            Row {
                badges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onFriendsClick) { Text("Ride Buddies") }
            TextButton(onClick = onNotificationsClick) { Text("Notifications") }
            TextButton(onClick = onEditProfileClick) { Text("Edit Profile") }
            TextButton(onClick = onSettingsClick) { Text("Settings") }
        }
    }
}
```

- [ ] **Step 2: `EditProfileScreen.kt` needs no code change** — only default `Button`/`OutlinedTextField`/`MaterialTheme.typography`, all theme-driven. Re-confirm during implementation; skip to Step 3 if still true.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt
git commit -m "feat: restyle Profile vehicle list onto MotouringCard with staggered entrance"
```

---

### Task 18: Settings + Notifications

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/NotificationsScreen.kt`

**Interfaces:**
- Consumes: `MotouringCard`, `StaggeredEntrance`.

- [ ] **Step 1: `SettingsScreen.kt` needs no code change** — only default `Switch`/`MaterialTheme.typography`, all theme-driven. Re-confirm during implementation; skip to Step 2 if still true.

- [ ] **Step 2: Rewrite `NotificationsScreen.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Notification
import com.valid.motouring.ui.components.MotouringCard
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(notifications, key = { _, it -> it.id }) { index, notification ->
            StaggeredEntrance(index = index, modifier = Modifier.padding(bottom = 8.dp)) {
                NotificationRow(notification = notification, onClick = { viewModel.markRead(notification.id) })
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    MotouringCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            text = notification.message,
            style = if (notification.isRead) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/NotificationsScreen.kt
git commit -m "feat: restyle Notifications onto MotouringCard with staggered entrance"
```

---

### Task 19: Create Post + Post Detail

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/social/CreatePostScreen.kt`

**Interfaces:**
- Consumes: theme tokens only. `PostDetailScreen.kt` already composes the restyled `PostCard` (Task 9) with no hardcoded styling of its own.

- [ ] **Step 1: `CreatePostScreen.kt` needs a shape update for the photo preview** — add a clip so the placeholder photo respects the 14dp shape scale instead of a hard rectangle. Add `import androidx.compose.foundation.shape.RoundedCornerShape` and `import androidx.compose.ui.draw.clip`, then change:

```kotlin
Image(
    painter = painterResource(id = R.drawable.ic_photo_placeholder),
    contentDescription = "Selected photo",
    modifier = Modifier.fillMaxWidth().height(180.dp),
)
```

to:

```kotlin
Image(
    painter = painterResource(id = R.drawable.ic_photo_placeholder),
    contentDescription = "Selected photo",
    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
)
```

Everything else in the file (`OutlinedTextField`, `Button`, `DropdownMenu`) is already theme-driven — no other change needed.

- [ ] **Step 2: Confirm `PostDetailScreen.kt` needs no change** — it only composes `PostCard` (already restyled) plus default `OutlinedTextField`/`IconButton`/`MaterialTheme.typography`. Re-confirm during implementation; no diff expected.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/CreatePostScreen.kt
git commit -m "feat: clip Create Post photo preview to the shape scale"
```

---

### Task 20: Friends + Invite Ride, and final smoke test

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/social/FriendsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/social/InviteRideScreen.kt`

**Interfaces:**
- Consumes: `StaggeredEntrance` (Task 3). No parameter changes.

- [ ] **Step 1: Rewrite `FriendsScreen.kt`** (stagger the buddy list; row content otherwise unchanged since it's all theme-driven already)

```kotlin
package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val buddies by viewModel.buddies.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(buddies, key = { _, it -> it.user.id }) { index, buddy ->
            StaggeredEntrance(index = index) {
                BuddyRow(
                    buddy = buddy,
                    onAccept = { viewModel.accept(buddy.user.id) },
                    onAdd = { viewModel.sendRequest(buddy.user.id) },
                )
            }
        }
    }
}

@Composable
private fun BuddyRow(buddy: RideBuddy, onAccept: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = buddy.user.avatarRes),
                contentDescription = buddy.user.name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = buddy.user.name, style = MaterialTheme.typography.titleMedium)
        }
        when (buddy.status) {
            BuddyStatus.FRIEND -> Text(text = "Friend", style = MaterialTheme.typography.labelSmall)
            BuddyStatus.PENDING_SENT -> Text(text = "Requested", style = MaterialTheme.typography.labelSmall)
            BuddyStatus.PENDING_RECEIVED -> Button(onClick = onAccept) { Text("Accept") }
            BuddyStatus.NOT_CONNECTED -> Button(onClick = onAdd) { Text("Add") }
        }
    }
}
```

- [ ] **Step 2: Rewrite `InviteRideScreen.kt`** (same stagger treatment)

```kotlin
package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.ui.components.StaggeredEntrance

@Composable
fun InviteRideScreen(viewModel: InviteRideViewModel, onDone: () -> Unit) {
    val friends by viewModel.friends.collectAsState()
    val selectedUserIds by viewModel.selectedUserIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item { Text(text = "Invite ride buddies", style = MaterialTheme.typography.headlineMedium) }
            itemsIndexed(friends, key = { _, it -> it.user.id }) { index, buddy ->
                StaggeredEntrance(index = index) {
                    FriendSelectRow(
                        buddy = buddy,
                        isSelected = buddy.user.id in selectedUserIds,
                        onToggle = { viewModel.toggleSelected(buddy.user.id) },
                    )
                }
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("Done (${selectedUserIds.size} invited)")
        }
    }
}

@Composable
private fun FriendSelectRow(buddy: RideBuddy, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = buddy.user.avatarRes),
                contentDescription = buddy.user.name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = buddy.user.name, style = MaterialTheme.typography.titleMedium)
        }
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}
```

- [ ] **Step 3: Verify the whole app compiles and assembles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Manual smoke test of the full navigation graph**

Install the debug APK on a device/emulator and walk the entire flow: Splash → Onboarding → Login → Vehicle Garage Setup → Home (verify the challenge card's InstrumentRing renders and animates in) → Nearby tab (still disabled, expected) → Challenges (verify ring progress on each row, badges grid) → Badge Detail → Rides (verify staggered card entrance) → Start Ride → Invite Ride → back → Profile → Edit Profile → Settings → Notifications → Friends → Create Post → Post Detail → back through the whole stack, confirming screen transitions slide/fade smoothly in both directions and nothing crashes or renders unstyled (stock Material blue/purple, sharp corners, or default sans type would indicate a missed spot).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/FriendsScreen.kt app/src/main/java/com/valid/motouring/ui/social/InviteRideScreen.kt
git commit -m "feat: restyle Friends and Invite Ride with staggered entrance"
```

- [ ] **Step 6: Push**

```bash
git push origin main
```

---

## Self-Review Notes

- **Spec coverage:** every design-spec section has a task — tokens (1-2), motion (3), signature component (4), icons (5), shared components (6-9), navigation motion (10), and all 20 screens (11-20, several needing no code change because they're already fully theme-driven, per Material 3's automatic propagation of `colorScheme`/`typography`/`shapes`).
- **Type consistency:** `InstrumentRing(progress, modifier, size, strokeWidth, showTicks, showGlow, content)` signature (Task 4) is used identically in Tasks 8, 13, 14, 15. `MotouringCard(modifier, onClick, content)` (Task 6) is used identically in Tasks 9, 13, 14, 16, 17, 18. `StaggeredEntrance(index, modifier, content)` (Task 3) is used identically in every screen task. `MotouringTextStyles.statValue/statValueLarge/statLabel` (Task 1) names match everywhere they're referenced.
- **No placeholders:** every step has complete code; the three "no code change needed" screens (`LoginScreen`, `VehicleGarageSetupScreen`, `StartRideScreen`, `RideSummaryScreen`, `EditProfileScreen`, `SettingsScreen`, `PostDetailScreen`) are explicitly confirmed against their actual current source (read during planning) rather than assumed.
