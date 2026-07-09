# Motouring Android Mockup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a mockup Android app ("Motouring") with ~21 screens covering ride-together group riding, simulated voice calls, POI maps, gamification, and a social feed — all backed by an in-memory fake data layer, no real backend.

**Architecture:** Single-module Kotlin/Compose app, MVVM (Compose screen → `ViewModel` exposing `StateFlow` → in-memory repository → model), manual `AppContainer` for DI, Navigation-Compose for a single-activity nav graph with a 5-tab bottom nav plus pushed flows.

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.0, Jetpack Compose (BOM 2026.06.00, verify/bump to latest stable if Android Studio flags one), Material 3, Navigation-Compose 2.8.x, Mapbox Maps SDK for Android 11.25.0 + `extension-compose`, kotlinx-coroutines 1.9.x. JUnit4 for logic-layer unit tests.

## Global Constraints

- Package id: `com.valid.motouring`
- minSdk 26, targetSdk 35, compileSdk 35
- Kotlin + Jetpack Compose + Material 3 only — no XML layouts, no Fragments
- Single fixed dark theme (charcoal + amber/red accent) — no light/dark toggle
- MVVM with `androidx.lifecycle.ViewModel` + `StateFlow`; DI via a manual `AppContainer` — no Hilt
- Navigation-Compose, single-activity
- Maps: Mapbox (not Google Maps) — `com.mapbox.maps:android` + `com.mapbox.maps:extension-compose`
- Images: plain Compose `Image` + `painterResource` against bundled drawables only — no Coil/network image loading (nothing in this mockup ever loads an image from a URI or URL)
- All data in-memory only (seeded by `FakeDataProvider`) — no Room, no DataStore, no network calls anywhere. Data resets to seed state on process death/relaunch.
- No real auth — Login/Signup accepts any non-empty input and proceeds
- No automated UI test suite required (mockup scope) — screens get a concrete manual smoke-test step instead of instrumentation tests. Logic-bearing classes (repositories, `RideSimulator`, ViewModels with derived state) get real JUnit unit tests.

---

## File Structure

```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/valid/motouring/
      MainActivity.kt
      MotouringApp.kt                     # root Composable, hosts NavHost
      di/AppContainer.kt
      ui/theme/Color.kt
      ui/theme/Type.kt
      ui/theme/Theme.kt
      navigation/Destinations.kt
      navigation/MotouringNavHost.kt
      data/model/User.kt
      data/model/Vehicle.kt
      data/model/RideBuddy.kt
      data/model/RideSession.kt
      data/model/RideHistoryEntry.kt
      data/model/Challenge.kt
      data/model/Badge.kt
      data/model/PointOfInterest.kt
      data/model/Post.kt
      data/model/Comment.kt
      data/model/Notification.kt
      data/fake/FakeDataProvider.kt
      data/repository/UserRepository.kt
      data/repository/VehicleRepository.kt
      data/repository/RideBuddyRepository.kt
      data/repository/RideRepository.kt
      data/repository/ChallengeRepository.kt
      data/repository/BadgeRepository.kt
      data/repository/PoiRepository.kt
      data/repository/PostRepository.kt
      data/repository/NotificationRepository.kt
      simulation/RideSimulator.kt
      ui/components/StatBlock.kt
      ui/components/PostCard.kt
      ui/components/RideBuddyAvatarRow.kt
      ui/components/BadgeChip.kt
      ui/components/SectionHeader.kt
      ui/onboarding/SplashScreen.kt
      ui/onboarding/OnboardingScreen.kt
      ui/onboarding/LoginScreen.kt
      ui/vehicle/VehicleGarageSetupScreen.kt
      ui/vehicle/VehicleGarageViewModel.kt
      ui/home/HomeScreen.kt
      ui/home/HomeViewModel.kt
      ui/social/CreatePostScreen.kt
      ui/social/PostDetailScreen.kt
      ui/social/PostViewModel.kt
      ui/social/FriendsScreen.kt
      ui/social/InviteRideScreen.kt
      ui/nearby/NearbyScreen.kt
      ui/nearby/NearbyViewModel.kt
      ui/challenges/ChallengesScreen.kt
      ui/challenges/ChallengeDetailScreen.kt
      ui/challenges/BadgesScreen.kt
      ui/challenges/BadgeDetailScreen.kt
      ui/challenges/ChallengesViewModel.kt
      ui/rides/RidesHistoryScreen.kt
      ui/rides/StartRideScreen.kt
      ui/rides/RideSessionScreen.kt
      ui/rides/RideSessionViewModel.kt
      ui/rides/RideSummaryScreen.kt
      ui/profile/ProfileScreen.kt
      ui/profile/EditProfileScreen.kt
      ui/profile/SettingsScreen.kt
      ui/profile/NotificationsScreen.kt
  src/test/java/com/valid/motouring/
      simulation/RideSimulatorTest.kt
      data/repository/ChallengeRepositoryTest.kt
      data/repository/PoiRepositoryTest.kt
      data/repository/PostRepositoryTest.kt
      ui/rides/RideSessionViewModelTest.kt
```

---

## Phase A — Foundation

### Task 1: Project scaffold & Gradle setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `local.properties.example`
- Create: `.gitignore`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties` (via `gradle wrapper`)

**Interfaces:**
- Produces: a buildable empty Android app shell with `BuildConfig.MAPBOX_PUBLIC_TOKEN` available to later tasks, package `com.valid.motouring`, Compose + coroutines on the classpath. Mapbox itself is added in Task 15 (see note in Step 2/Step 6 below).

- [ ] **Step 1: Create `.gitignore`**

```gitignore
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
```

- [ ] **Step 2: Create `settings.gradle.kts`**

Mapbox's authenticated maven repo (needed to resolve `com.mapbox.maps:*` dependencies) is deliberately NOT added here — it's added in Task 15, the first task that actually depends on Mapbox. This lets Tasks 1-14 build and test with zero external tokens.

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Motouring"
include(":app")
```

- [ ] **Step 3: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
```

- [ ] **Step 4: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create `local.properties.example` (committed as a template; real `local.properties` stays gitignored)**

```properties
sdk.dir=/path/to/Android/sdk
MAPBOX_DOWNLOADS_TOKEN=your_mapbox_secret_downloads_token
MAPBOX_PUBLIC_TOKEN=your_mapbox_public_access_token
```

Note for whoever runs this: get both tokens from https://account.mapbox.com/access-tokens/ — the downloads token needs the `DOWNLOADS:READ` secret scope, the public token is the default public one. Copy `local.properties.example` to `local.properties` and fill in real values; `local.properties` is gitignored so tokens never get committed.

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.valid.motouring"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.valid.motouring"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mockup"

        buildConfigField(
            "String",
            "MAPBOX_PUBLIC_TOKEN",
            "\"${localProperties.getProperty("MAPBOX_PUBLIC_TOKEN", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Mapbox dependencies are added in Task 15, the first task that uses them —
    // resolving them requires an authenticated Mapbox maven repo (also added in Task 15),
    // so keeping them out until then lets Tasks 1-14 build/test with zero external tokens.

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
```

- [ ] **Step 7: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MotouringApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Motouring"
        android:theme="@style/Theme.Motouring">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Motouring">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`INTERNET` permission is required by the Mapbox SDK to fetch map tiles even though the rest of the app has no networking.

- [ ] **Step 8: Create the minimal theme resource and app icon placeholder needed by the manifest**

Create `app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.Motouring" parent="android:Theme.Black.NoTitleBar" />
</resources>
```

(App icon: use Android Studio's default `ic_launcher` mipmaps generated by the New Project wizard equivalent — for this task, run `Image Asset Studio` later or leave Android Studio's default adaptive icon; not a functional blocker for the mockup.)

- [ ] **Step 9: Create `MotouringApplication.kt`**

Task 15 modifies this to set the Mapbox access token, once the Mapbox dependency exists. For now it's an empty `Application` subclass (still needed because the manifest already references `.MotouringApplication`).

`app/src/main/java/com/valid/motouring/MotouringApplication.kt`:

```kotlin
package com.valid.motouring

import android.app.Application

class MotouringApplication : Application()
```

- [ ] **Step 10: Verify the project builds**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL` (no `MainActivity` yet, so this only compiles the manifest/application class — if Gradle complains `MainActivity` referenced in the manifest doesn't exist, temporarily comment out the `<activity>` block, confirm the build passes, then restore it; Task 8 creates `MainActivity`.)

- [ ] **Step 11: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties local.properties.example .gitignore app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/themes.xml app/src/main/java/com/valid/motouring/MotouringApplication.kt
git commit -m "chore: scaffold Android project with Compose dependencies"
```

---

### Task 2: Theme & design tokens

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/theme/Color.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/theme/Type.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/theme/Theme.kt`

**Interfaces:**
- Produces: `MotouringTheme { content }` composable used by `MainActivity`; color tokens `MotouringColors` object referenced by later UI tasks for non-Material accents (e.g. badge highlight, speaking-now indicator).

- [ ] **Step 1: Create `Color.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.ui.graphics.Color

val Charcoal900 = Color(0xFF12100E)
val Charcoal800 = Color(0xFF1C1917)
val Charcoal700 = Color(0xFF2A2522)
val Charcoal600 = Color(0xFF3D3632)
val Amber500 = Color(0xFFFFA726)
val Amber300 = Color(0xFFFFCC80)
val Red500 = Color(0xFFE53935)
val OffWhite = Color(0xFFF5F1EC)
val Muted = Color(0xFFA89F97)

object MotouringColors {
    val speakingNowHighlight = Amber500
    val badgeLocked = Charcoal600
    val badgeEarned = Amber500
    val liked = Red500
}
```

- [ ] **Step 2: Create `Type.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MotouringTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)
```

- [ ] **Step 3: Create `Theme.kt`**

```kotlin
package com.valid.motouring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MotouringColorScheme = darkColorScheme(
    primary = Amber500,
    secondary = Red500,
    background = Charcoal900,
    surface = Charcoal800,
    surfaceVariant = Charcoal700,
    onPrimary = Charcoal900,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onSurfaceVariant = Muted,
    error = Red500,
)

@Composable
fun MotouringTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotouringColorScheme,
        typography = MotouringTypography,
        content = content,
    )
}
```

Note: `isSystemInDarkTheme()` is imported but intentionally unused — the app always renders `MotouringColorScheme` regardless of system setting, per the single-fixed-theme constraint. Remove the import if your linter flags it as unused.

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/theme/
git commit -m "feat: add fixed dark theme and design tokens"
```

---

### Task 3: Data models

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/model/User.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Vehicle.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/RideBuddy.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/GeoPoint.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/RideSession.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/RideHistoryEntry.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Challenge.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Badge.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/PointOfInterest.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Post.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Comment.kt`
- Create: `app/src/main/java/com/valid/motouring/data/model/Notification.kt`

**Interfaces:**
- Produces: every data class consumed by repositories (Task 5), `FakeDataProvider` (Task 4), and all ViewModels/screens in later tasks. Field names below are final — later tasks reference them verbatim. `GeoPoint(lat: Double, lng: Double)` is this app's own plain coordinate type — the domain layer never imports `com.mapbox.geojson.Point` directly, since Mapbox isn't a dependency until Task 15. UI code that needs a real Mapbox `Point` (Tasks 15, 22) converts at the call site: `Point.fromLngLat(geoPoint.lng, geoPoint.lat)`.

- [ ] **Step 1: Create `User.kt`**

```kotlin
package com.valid.motouring.data.model

data class User(
    val id: String,
    val name: String,
    val avatarRes: Int,
    val vehicleIds: List<String>,
)
```

- [ ] **Step 2: Create `Vehicle.kt`**

```kotlin
package com.valid.motouring.data.model

enum class VehicleType { MOTORCYCLE, CAR }

data class Vehicle(
    val id: String,
    val ownerId: String,
    val type: VehicleType,
    val make: String,
    val model: String,
    val year: Int,
    val photoRes: Int,
)
```

- [ ] **Step 3: Create `RideBuddy.kt`**

```kotlin
package com.valid.motouring.data.model

enum class BuddyStatus { FRIEND, PENDING_SENT, PENDING_RECEIVED, NOT_CONNECTED }

data class RideBuddy(
    val user: User,
    val status: BuddyStatus,
)
```

- [ ] **Step 4: Create `GeoPoint.kt`**

```kotlin
package com.valid.motouring.data.model

data class GeoPoint(val lat: Double, val lng: Double)
```

- [ ] **Step 5: Create `RideSession.kt`**

```kotlin
package com.valid.motouring.data.model

enum class RideSessionStatus { ACTIVE, ENDED }

data class RideParticipantState(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val position: GeoPoint,
    val isSpeaking: Boolean = false,
)

data class RideSession(
    val id: String,
    val vehicleType: VehicleType,
    val route: List<GeoPoint>,
    val participants: List<RideParticipantState>,
    val distanceMeters: Double,
    val speedKmh: Double,
    val elapsedSeconds: Long,
    val status: RideSessionStatus,
)
```

- [ ] **Step 6: Create `RideHistoryEntry.kt`**

```kotlin
package com.valid.motouring.data.model

data class RideHistoryEntry(
    val id: String,
    val title: String,
    val vehicleType: VehicleType,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val routePreviewRes: Int,
    val photoResList: List<Int>,
    val completedAtEpochSeconds: Long,
)
```

- [ ] **Step 7: Create `Challenge.kt`**

```kotlin
package com.valid.motouring.data.model

enum class ChallengeMetric { DISTANCE_KM, RIDE_COUNT }

data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val avatarRes: Int,
    val progressValue: Double,
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val metric: ChallengeMetric,
    val goalValue: Double,
    val currentValue: Double,
    val deadlineEpochSeconds: Long,
    val leaderboard: List<LeaderboardEntry>,
)
```

- [ ] **Step 8: Create `Badge.kt`**

```kotlin
package com.valid.motouring.data.model

data class Badge(
    val id: String,
    val title: String,
    val iconRes: Int,
    val description: String,
    val unlockCriteria: String,
    val isEarned: Boolean,
    val earnedAtEpochSeconds: Long?,
)
```

- [ ] **Step 9: Create `PointOfInterest.kt`**

```kotlin
package com.valid.motouring.data.model

enum class PoiType { GAS_STATION, REPAIR_SHOP }

data class PointOfInterest(
    val id: String,
    val name: String,
    val type: PoiType,
    val location: GeoPoint,
    val supportedVehicleTypes: Set<VehicleType>,
    val rating: Double,
)
```

- [ ] **Step 10: Create `Post.kt`**

```kotlin
package com.valid.motouring.data.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarRes: Int,
    val photoResList: List<Int>,
    val caption: String,
    val attachedRideId: String?,
    val likeCount: Int,
    val likedByMe: Boolean,
    val commentIds: List<String>,
    val createdAtEpochSeconds: Long,
)
```

- [ ] **Step 11: Create `Comment.kt`**

```kotlin
package com.valid.motouring.data.model

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarRes: Int,
    val text: String,
    val createdAtEpochSeconds: Long,
)
```

- [ ] **Step 12: Create `Notification.kt`**

```kotlin
package com.valid.motouring.data.model

enum class NotificationType { RIDE_INVITE, BADGE_EARNED, CHALLENGE_PROGRESS, SOCIAL }

data class Notification(
    val id: String,
    val type: NotificationType,
    val message: String,
    val createdAtEpochSeconds: Long,
    val isRead: Boolean,
)
```

- [ ] **Step 13: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 14: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/model/
git commit -m "feat: add data models for mock domain layer"
```

---

### Task 4: Placeholder drawables & fake data provider

**Files:**
- Create: `app/src/main/res/drawable/ic_avatar_placeholder.xml`
- Create: `app/src/main/res/drawable/ic_vehicle_motorcycle_placeholder.xml`
- Create: `app/src/main/res/drawable/ic_vehicle_car_placeholder.xml`
- Create: `app/src/main/res/drawable/ic_route_preview_placeholder.xml`
- Create: `app/src/main/res/drawable/ic_photo_placeholder.xml`
- Create: `app/src/main/res/drawable/ic_badge_placeholder.xml`
- Create: `app/src/main/java/com/valid/motouring/data/fake/FakeDataProvider.kt`

**Interfaces:**
- Consumes: all model classes from Task 3.
- Produces: `FakeDataProvider` object with vals `currentUserId: String`, `users: List<User>`, `vehicles: List<Vehicle>`, `rideBuddies: List<RideBuddy>`, `rideHistory: List<RideHistoryEntry>`, `challenges: List<Challenge>`, `badges: List<Badge>`, `pois: List<PointOfInterest>`, `posts: List<Post>`, `comments: List<Comment>`, `notifications: List<Notification>` — the seed data every repository in Task 5 wraps in mutable in-memory state.

- [ ] **Step 1: Create the six placeholder vector drawables**

All six follow the same simple pattern — a single-color Material-style glyph on a transparent background, reused across many mock entries. `ic_avatar_placeholder.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnSurface">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z" />
</vector>
```

`ic_vehicle_motorcycle_placeholder.xml` (motorcycle glyph):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFA726"
        android:pathData="M19.44,9.03L15.41,5H11v2h3.59l2,2H5c-2.8,0 -5,2.2 -5,5s2.2,5 5,5c2.46,0 4.45,-1.69 4.9,-4h2.2c0.45,2.31 2.44,4 4.9,4 2.8,0 5,-2.2 5,-5 0,-2.02 -1.16,-3.7 -2.56,-4.97zM5,17c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zM17,17c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3z" />
</vector>
```

`ic_vehicle_car_placeholder.xml` (car glyph):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#E53935"
        android:pathData="M18.92,6.01C18.72,5.42 18.16,5 17.5,5h-11c-0.66,0 -1.22,0.42 -1.42,1.01L3,12v8c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1h12v1c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-8l-2.08,-5.99zM6.5,16C5.67,16 5,15.33 5,14.5S5.67,13 6.5,13s1.5,0.67 1.5,1.5S7.33,16 6.5,16zM17.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM5,11l1.5,-4.5h11L19,11H5z" />
</vector>
```

`ic_route_preview_placeholder.xml` (simple winding line on a map card background):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="80dp"
    android:viewportWidth="120"
    android:viewportHeight="80">
    <path android:fillColor="#2A2522" android:pathData="M0,0h120v80h-120z" />
    <path
        android:strokeColor="#FFA726"
        android:strokeWidth="3"
        android:pathData="M10,60 C40,10 60,70 110,20" />
</vector>
```

`ic_photo_placeholder.xml` (generic image glyph):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#3D3632" android:pathData="M0,0h24v24h-24z" />
    <path
        android:fillColor="#A89F97"
        android:pathData="M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.9,13.98l2.1,2.53 3.1,-3.99 4,5.48H5l3.9,-4.02z" />
</vector>
```

`ic_badge_placeholder.xml` (medal glyph):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="56dp"
    android:height="56dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFA726"
        android:pathData="M12,2l-1.9,4.4L5,7l3.6,3.5L7.8,15,12,12.6,16.2,15l-0.8,-4.5L19,7l-5.1,-0.6z" />
</vector>
```

- [ ] **Step 2: Create `FakeDataProvider.kt`**

```kotlin
package com.valid.motouring.data.fake

import com.valid.motouring.R
import com.valid.motouring.data.model.*

object FakeDataProvider {

    const val currentUserId = "u-me"

    // Route roughly following Jl. Sudirman -> Jl. Thamrin, Jakarta, used both as a
    // ride-history route preview and as the polyline RideSimulator animates along.
    val sampleRoute = listOf(
        GeoPoint(lat = -6.2246, lng = 106.8091),
        GeoPoint(lat = -6.2153, lng = 106.8149),
        GeoPoint(lat = -6.2088, lng = 106.8206),
        GeoPoint(lat = -6.1976, lng = 106.8235),
        GeoPoint(lat = -6.1875, lng = 106.8271),
    )

    val users = listOf(
        User("u-me", "Rafi", R.drawable.ic_avatar_placeholder, listOf("v-1", "v-2")),
        User("u-2", "Dinda", R.drawable.ic_avatar_placeholder, listOf("v-3")),
        User("u-3", "Bagas", R.drawable.ic_avatar_placeholder, listOf("v-4")),
        User("u-4", "Sarah", R.drawable.ic_avatar_placeholder, listOf("v-5")),
        User("u-5", "Yoga", R.drawable.ic_avatar_placeholder, listOf("v-6")),
        User("u-6", "Nadia", R.drawable.ic_avatar_placeholder, listOf("v-7")),
    )

    val vehicles = listOf(
        Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, R.drawable.ic_vehicle_motorcycle_placeholder),
        Vehicle("v-2", "u-me", VehicleType.CAR, "Toyota", "Raize", 2022, R.drawable.ic_vehicle_car_placeholder),
        Vehicle("v-3", "u-2", VehicleType.MOTORCYCLE, "Honda", "CBR150R", 2021, R.drawable.ic_vehicle_motorcycle_placeholder),
        Vehicle("v-4", "u-3", VehicleType.CAR, "Honda", "Civic", 2020, R.drawable.ic_vehicle_car_placeholder),
        Vehicle("v-5", "u-4", VehicleType.MOTORCYCLE, "Kawasaki", "Z250", 2022, R.drawable.ic_vehicle_motorcycle_placeholder),
        Vehicle("v-6", "u-5", VehicleType.MOTORCYCLE, "Yamaha", "R15", 2023, R.drawable.ic_vehicle_motorcycle_placeholder),
        Vehicle("v-7", "u-6", VehicleType.CAR, "Mazda", "CX-5", 2021, R.drawable.ic_vehicle_car_placeholder),
    )

    val rideBuddies = listOf(
        RideBuddy(users[1], BuddyStatus.FRIEND),
        RideBuddy(users[2], BuddyStatus.FRIEND),
        RideBuddy(users[3], BuddyStatus.FRIEND),
        RideBuddy(users[4], BuddyStatus.PENDING_RECEIVED),
        RideBuddy(users[5], BuddyStatus.NOT_CONNECTED),
    )

    val rideHistory = listOf(
        RideHistoryEntry("r-1", "Sudirman Sunday Loop", VehicleType.MOTORCYCLE, 18_400.0, 2_700, 24.5, R.drawable.ic_route_preview_placeholder, listOf(R.drawable.ic_photo_placeholder), 1_752_000_000),
        RideHistoryEntry("r-2", "Weekend Car Meet", VehicleType.CAR, 42_000.0, 5_400, 28.0, R.drawable.ic_route_preview_placeholder, listOf(R.drawable.ic_photo_placeholder, R.drawable.ic_photo_placeholder), 1_752_400_000),
        RideHistoryEntry("r-3", "Night Ride to Puncak", VehicleType.MOTORCYCLE, 65_000.0, 9_000, 26.0, R.drawable.ic_route_preview_placeholder, emptyList(), 1_752_800_000),
    )

    val challenges = listOf(
        Challenge(
            id = "c-1",
            title = "Ride 100km This Week",
            description = "Log at least 100km across any rides before the week ends.",
            metric = ChallengeMetric.DISTANCE_KM,
            goalValue = 100.0,
            currentValue = 62.0,
            deadlineEpochSeconds = 1_753_000_000,
            leaderboard = listOf(
                LeaderboardEntry("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 88.0),
                LeaderboardEntry("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 62.0),
                LeaderboardEntry("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 40.0),
            ),
        ),
        Challenge(
            id = "c-2",
            title = "5 Rides This Month",
            description = "Complete 5 separate ride sessions this month, solo or group.",
            metric = ChallengeMetric.RIDE_COUNT,
            goalValue = 5.0,
            currentValue = 3.0,
            deadlineEpochSeconds = 1_754_500_000,
            leaderboard = listOf(
                LeaderboardEntry("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 3.0),
                LeaderboardEntry("u-4", "Sarah", R.drawable.ic_avatar_placeholder, 2.0),
            ),
        ),
        Challenge(
            id = "c-3",
            title = "Group Ride Weekend",
            description = "Join or host a group ride with 3+ riders this weekend.",
            metric = ChallengeMetric.RIDE_COUNT,
            goalValue = 1.0,
            currentValue = 0.0,
            deadlineEpochSeconds = 1_753_200_000,
            leaderboard = emptyList(),
        ),
    )

    val badges = listOf(
        Badge("b-1", "First Ride", R.drawable.ic_badge_placeholder, "Complete your first tracked ride", "Complete 1 ride", true, 1_751_000_000),
        Badge("b-2", "Century Rider", R.drawable.ic_badge_placeholder, "Ride 100km in a single session", "Single ride >= 100km", false, null),
        Badge("b-3", "Squad Leader", R.drawable.ic_badge_placeholder, "Host a group ride with 5+ riders", "Host group ride, 5+ participants", true, 1_752_100_000),
        Badge("b-4", "Night Owl", R.drawable.ic_badge_placeholder, "Complete a ride starting after 10pm", "Ride start time >= 22:00", true, 1_752_800_500),
        Badge("b-5", "Early Bird", R.drawable.ic_badge_placeholder, "Complete a ride starting before 6am", "Ride start time <= 06:00", false, null),
        Badge("b-6", "Wrench Turner", R.drawable.ic_badge_placeholder, "Check in at 3 different repair shops", "3 unique repair shop check-ins", false, null),
    )

    val pois = listOf(
        PointOfInterest("p-1", "Pertamina Sudirman", PoiType.GAS_STATION, GeoPoint(lat = -6.2088, lng = 106.8206), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.3),
        PointOfInterest("p-2", "Shell Thamrin", PoiType.GAS_STATION, GeoPoint(lat = -6.1976, lng = 106.8235), setOf(VehicleType.CAR), 4.1),
        PointOfInterest("p-3", "Bengkel Motor Jaya", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.2153, lng = 106.8149), setOf(VehicleType.MOTORCYCLE), 4.6),
        PointOfInterest("p-4", "Auto Repair Kemang", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.2608, lng = 106.8130), setOf(VehicleType.CAR), 4.4),
        PointOfInterest("p-5", "Pertamina Kuningan", PoiType.GAS_STATION, GeoPoint(lat = -6.2241, lng = 106.8306), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.0),
        PointOfInterest("p-6", "Bengkel Jaya Motor 2", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.1875, lng = 106.8271), setOf(VehicleType.MOTORCYCLE), 4.2),
    )

    val comments = listOf(
        Comment("cm-1", "post-1", "u-2", "Dinda", R.drawable.ic_avatar_placeholder, "Nice route!", 1_752_000_500),
        Comment("cm-2", "post-1", "u-3", "Bagas", R.drawable.ic_avatar_placeholder, "Let's ride together next time", 1_752_000_800),
        Comment("cm-3", "post-2", "u-4", "Sarah", R.drawable.ic_avatar_placeholder, "That CX-5 looks clean", 1_752_400_400),
    )

    val posts = listOf(
        Post("post-1", "u-me", "Rafi", R.drawable.ic_avatar_placeholder, listOf(R.drawable.ic_photo_placeholder), "Sunday morning loop around Sudirman", "r-1", 12, false, listOf("cm-1", "cm-2"), 1_752_000_100),
        Post("post-2", "u-6", "Nadia", R.drawable.ic_avatar_placeholder, listOf(R.drawable.ic_photo_placeholder, R.drawable.ic_photo_placeholder), "Weekend car meet turnout was huge", null, 24, true, listOf("cm-3"), 1_752_400_100),
        Post("post-3", "u-2", "Dinda", R.drawable.ic_avatar_placeholder, listOf(R.drawable.ic_photo_placeholder), "New chain and sprocket installed", null, 8, false, emptyList(), 1_752_600_000),
    )

    val notifications = listOf(
        Notification("n-1", NotificationType.RIDE_INVITE, "Bagas invited you to a group ride", 1_752_900_000, false),
        Notification("n-2", NotificationType.BADGE_EARNED, "You earned the Night Owl badge", 1_752_800_600, false),
        Notification("n-3", NotificationType.CHALLENGE_PROGRESS, "You're 62% through Ride 100km This Week", 1_752_950_000, true),
        Notification("n-4", NotificationType.SOCIAL, "Dinda commented on your post", 1_752_000_900, true),
    )
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/ app/src/main/java/com/valid/motouring/data/fake/
git commit -m "feat: add placeholder drawables and seed fake data"
```

---

### Task 5: Repositories + unit tests

**Files:**
- Create: `app/src/main/java/com/valid/motouring/data/repository/UserRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/VehicleRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/RideBuddyRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/RideRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/ChallengeRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/BadgeRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/PoiRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/PostRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/data/repository/NotificationRepository.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/ChallengeRepositoryTest.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/PoiRepositoryTest.kt`
- Test: `app/src/test/java/com/valid/motouring/data/repository/PostRepositoryTest.kt`

**Interfaces:**
- Consumes: `FakeDataProvider` (Task 4), all models (Task 3).
- Produces: nine repository classes, each with an `observeX(): StateFlow<List<X>>` method. Exact method names below are final — ViewModels in later tasks call these verbatim: `UserRepository.currentUser()`, `UserRepository.userById(id)`, `VehicleRepository.observeVehicles()`, `VehicleRepository.vehiclesFor(userId)`, `VehicleRepository.addVehicle(vehicle)`, `RideBuddyRepository.observeBuddies()`, `RideBuddyRepository.friends()`, `RideBuddyRepository.updateStatus(userId, status)`, `RideRepository.observeHistory()`, `RideRepository.addHistoryEntry(entry)`, `ChallengeRepository.observeChallenges()`, `ChallengeRepository.challenge(id)`, `BadgeRepository.observeBadges()`, `BadgeRepository.badge(id)`, `BadgeRepository.markEarned(id)`, `PoiRepository.observePois()`, `PoiRepository.filterByVehicleType(type)`, `PostRepository.observePosts()`, `PostRepository.addPost(post)`, `PostRepository.toggleLike(postId)`, `PostRepository.commentsFor(postId)`, `PostRepository.addComment(comment)`, `NotificationRepository.observeNotifications()`, `NotificationRepository.markRead(id)`, `NotificationRepository.unreadCount()`.

- [ ] **Step 1: Create `UserRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepository {
    private val users = MutableStateFlow(FakeDataProvider.users)

    fun observeUsers(): StateFlow<List<User>> = users.asStateFlow()

    fun currentUser(): User =
        users.value.first { it.id == FakeDataProvider.currentUserId }

    fun userById(id: String): User? = users.value.firstOrNull { it.id == id }
}
```

- [ ] **Step 2: Create `VehicleRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VehicleRepository {
    private val vehicles = MutableStateFlow(FakeDataProvider.vehicles)

    fun observeVehicles(): StateFlow<List<Vehicle>> = vehicles.asStateFlow()

    fun vehiclesFor(userId: String): List<Vehicle> =
        vehicles.value.filter { it.ownerId == userId }

    fun addVehicle(vehicle: Vehicle) {
        vehicles.value = vehicles.value + vehicle
    }
}
```

- [ ] **Step 3: Create `RideBuddyRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideBuddyRepository {
    private val buddies = MutableStateFlow(FakeDataProvider.rideBuddies)

    fun observeBuddies(): StateFlow<List<RideBuddy>> = buddies.asStateFlow()

    fun friends(): List<RideBuddy> =
        buddies.value.filter { it.status == BuddyStatus.FRIEND }

    fun updateStatus(userId: String, status: BuddyStatus) {
        buddies.value = buddies.value.map {
            if (it.user.id == userId) it.copy(status = status) else it
        }
    }
}
```

- [ ] **Step 4: Create `RideRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.RideHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideRepository {
    private val history = MutableStateFlow(
        FakeDataProvider.rideHistory.sortedByDescending { it.completedAtEpochSeconds }
    )

    fun observeHistory(): StateFlow<List<RideHistoryEntry>> = history.asStateFlow()

    fun addHistoryEntry(entry: RideHistoryEntry) {
        history.value = (listOf(entry) + history.value)
            .sortedByDescending { it.completedAtEpochSeconds }
    }
}
```

- [ ] **Step 5: Create `ChallengeRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Challenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChallengeRepository {
    private val challenges = MutableStateFlow(FakeDataProvider.challenges)

    fun observeChallenges(): StateFlow<List<Challenge>> = challenges.asStateFlow()

    fun challenge(id: String): Challenge? = challenges.value.firstOrNull { it.id == id }
}
```

- [ ] **Step 6: Create `ChallengeRepositoryTest.kt`**

```kotlin
package com.valid.motouring.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeRepositoryTest {

    @Test
    fun `observeChallenges emits every seeded challenge`() {
        val repo = ChallengeRepository()
        assertEquals(3, repo.observeChallenges().value.size)
    }

    @Test
    fun `challenge returns the matching entry by id`() {
        val repo = ChallengeRepository()
        val found = repo.challenge("c-1")
        assertEquals("Ride 100km This Week", found?.title)
    }

    @Test
    fun `challenge returns null for an unknown id`() {
        val repo = ChallengeRepository()
        assertNull(repo.challenge("does-not-exist"))
    }
}
```

- [ ] **Step 7: Run the new tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.data.repository.ChallengeRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed

- [ ] **Step 8: Create `BadgeRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Badge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BadgeRepository {
    private val badges = MutableStateFlow(FakeDataProvider.badges)

    fun observeBadges(): StateFlow<List<Badge>> = badges.asStateFlow()

    fun badge(id: String): Badge? = badges.value.firstOrNull { it.id == id }

    fun markEarned(id: String, earnedAtEpochSeconds: Long) {
        badges.value = badges.value.map {
            if (it.id == id) it.copy(isEarned = true, earnedAtEpochSeconds = earnedAtEpochSeconds) else it
        }
    }
}
```

- [ ] **Step 9: Create `PoiRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PoiRepository {
    private val pois = MutableStateFlow(FakeDataProvider.pois)

    fun observePois(): StateFlow<List<PointOfInterest>> = pois.asStateFlow()

    fun filterByVehicleType(type: VehicleType): List<PointOfInterest> =
        pois.value.filter { type in it.supportedVehicleTypes }
}
```

- [ ] **Step 10: Create `PoiRepositoryTest.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiRepositoryTest {

    @Test
    fun `filterByVehicleType only returns POIs supporting that vehicle type`() {
        val repo = PoiRepository()
        val motorcycleResults = repo.filterByVehicleType(VehicleType.MOTORCYCLE)

        assertTrue(motorcycleResults.isNotEmpty())
        assertTrue(motorcycleResults.all { VehicleType.MOTORCYCLE in it.supportedVehicleTypes })
    }

    @Test
    fun `filterByVehicleType excludes POIs that don't support that vehicle type`() {
        val repo = PoiRepository()
        val carResults = repo.filterByVehicleType(VehicleType.CAR)

        assertTrue(carResults.none { VehicleType.CAR !in it.supportedVehicleTypes })
    }
}
```

- [ ] **Step 11: Run the new tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.data.repository.PoiRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed

- [ ] **Step 12: Create `PostRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Comment
import com.valid.motouring.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostRepository {
    private val posts = MutableStateFlow(
        FakeDataProvider.posts.sortedByDescending { it.createdAtEpochSeconds }
    )
    private val comments = MutableStateFlow(FakeDataProvider.comments)

    fun observePosts(): StateFlow<List<Post>> = posts.asStateFlow()

    fun addPost(post: Post) {
        posts.value = (listOf(post) + posts.value)
            .sortedByDescending { it.createdAtEpochSeconds }
    }

    fun toggleLike(postId: String) {
        posts.value = posts.value.map {
            if (it.id == postId) {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likeCount = if (it.likedByMe) it.likeCount - 1 else it.likeCount + 1,
                )
            } else it
        }
    }

    fun commentsFor(postId: String): List<Comment> =
        comments.value.filter { it.postId == postId }

    fun addComment(comment: Comment) {
        comments.value = comments.value + comment
        posts.value = posts.value.map {
            if (it.id == comment.postId) it.copy(commentIds = it.commentIds + comment.id) else it
        }
    }
}
```

- [ ] **Step 13: Create `PostRepositoryTest.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostRepositoryTest {

    @Test
    fun `toggleLike increments count and flips likedByMe when not yet liked`() {
        val repo = PostRepository()
        val before = repo.observePosts().value.first { !it.likedByMe }

        repo.toggleLike(before.id)

        val after = repo.observePosts().value.first { it.id == before.id }
        assertTrue(after.likedByMe)
        assertEquals(before.likeCount + 1, after.likeCount)
    }

    @Test
    fun `toggleLike decrements count and flips likedByMe when already liked`() {
        val repo = PostRepository()
        val before = repo.observePosts().value.first { it.likedByMe }

        repo.toggleLike(before.id)

        val after = repo.observePosts().value.first { it.id == before.id }
        assertTrue(!after.likedByMe)
        assertEquals(before.likeCount - 1, after.likeCount)
    }

    @Test
    fun `addComment appends to commentsFor and links id back onto the post`() {
        val repo = PostRepository()
        val targetPost = repo.observePosts().value.first()
        val comment = Comment(
            id = "cm-new",
            postId = targetPost.id,
            authorId = "u-me",
            authorName = "Rafi",
            authorAvatarRes = 0,
            text = "Great ride!",
            createdAtEpochSeconds = 1_753_000_000,
        )

        repo.addComment(comment)

        assertTrue(repo.commentsFor(targetPost.id).any { it.id == "cm-new" })
        val updatedPost = repo.observePosts().value.first { it.id == targetPost.id }
        assertTrue(updatedPost.commentIds.contains("cm-new"))
    }
}
```

- [ ] **Step 14: Run the new tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.data.repository.PostRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed

- [ ] **Step 15: Create `NotificationRepository.kt`**

```kotlin
package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationRepository {
    private val notifications = MutableStateFlow(FakeDataProvider.notifications)

    fun observeNotifications(): StateFlow<List<Notification>> = notifications.asStateFlow()

    fun markRead(id: String) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun unreadCount(): Int = notifications.value.count { !it.isRead }
}
```

- [ ] **Step 16: Verify the full module still compiles and all repository tests pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 8 tests passed (3 challenge + 2 poi + 3 post)

- [ ] **Step 17: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/ app/src/test/java/com/valid/motouring/data/repository/
git commit -m "feat: add in-memory repositories with unit tests"
```

---

### Task 6: AppContainer DI

**Files:**
- Create: `app/src/main/java/com/valid/motouring/di/AppContainer.kt`

**Interfaces:**
- Consumes: all nine repository classes from Task 5.
- Produces: `AppContainer` — a plain class instantiated once in `MainActivity` (Task 8) holding one instance of each repository. Later ViewModel factories read `container.userRepository`, `container.vehicleRepository`, etc.

- [ ] **Step 1: Create `AppContainer.kt`**

```kotlin
package com.valid.motouring.di

import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.ChallengeRepository
import com.valid.motouring.data.repository.NotificationRepository
import com.valid.motouring.data.repository.PoiRepository
import com.valid.motouring.data.repository.PostRepository
import com.valid.motouring.data.repository.RideBuddyRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.data.repository.VehicleRepository

class AppContainer {
    val userRepository = UserRepository()
    val vehicleRepository = VehicleRepository()
    val rideBuddyRepository = RideBuddyRepository()
    val rideRepository = RideRepository()
    val challengeRepository = ChallengeRepository()
    val badgeRepository = BadgeRepository()
    val poiRepository = PoiRepository()
    val postRepository = PostRepository()
    val notificationRepository = NotificationRepository()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/valid/motouring/di/AppContainer.kt
git commit -m "feat: add manual AppContainer for repository DI"
```

---

## Phase B — Simulation Engine

### Task 7: RideSimulator + unit tests

**Files:**
- Create: `app/src/main/java/com/valid/motouring/simulation/RideSimulator.kt`
- Test: `app/src/test/java/com/valid/motouring/simulation/RideSimulatorTest.kt`

**Interfaces:**
- Consumes: `RideSession`, `RideParticipantState`, `RideSessionStatus`, `GeoPoint` (Task 3). Uses only this app's own `GeoPoint` type — Mapbox isn't a dependency yet (added in Task 15), and the haversine/route-interpolation math here is plain lat/lng arithmetic that doesn't need a map SDK at all.
- Produces: `RideSimulator(scope: CoroutineScope, initialSession: RideSession)` with `val session: StateFlow<RideSession>`, `fun start()`, `fun stop()`, and a companion `RideSimulator.advance(current: RideSession): RideSession` pure function that Task 22's `RideSessionViewModel` and this task's tests both call directly.

- [ ] **Step 1: Write the failing test for `advance()`**

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideSimulatorTest {

    private val route = listOf(
        GeoPoint(lat = -6.2246, lng = 106.8091),
        GeoPoint(lat = -6.2153, lng = 106.8149),
        GeoPoint(lat = -6.2088, lng = 106.8206),
    )

    private fun freshSession() = RideSession(
        id = "sim-test",
        vehicleType = VehicleType.MOTORCYCLE,
        route = route,
        participants = listOf(
            RideParticipantState("u-me", "Rafi", 0, route.first(), isSpeaking = false),
            RideParticipantState("u-2", "Dinda", 0, route.first(), isSpeaking = false),
            RideParticipantState("u-3", "Bagas", 0, route.first(), isSpeaking = false),
        ),
        distanceMeters = 0.0,
        speedKmh = 0.0,
        elapsedSeconds = 0,
        status = RideSessionStatus.ACTIVE,
    )

    @Test
    fun `advance increases elapsed seconds by 1 each tick`() {
        val next = RideSimulator.advance(freshSession())
        assertEquals(1L, next.elapsedSeconds)
    }

    @Test
    fun `advance increases distance monotonically over repeated ticks`() {
        var session = freshSession()
        var previousDistance = session.distanceMeters
        repeat(20) {
            session = RideSimulator.advance(session)
            assertTrue(session.distanceMeters > previousDistance)
            previousDistance = session.distanceMeters
        }
    }

    @Test
    fun `advance moves the lead participant's position along the route`() {
        var session = freshSession()
        val startPosition = session.participants.first().position
        repeat(30) { session = RideSimulator.advance(session) }
        assertNotEquals(startPosition, session.participants.first().position)
    }

    @Test
    fun `advance rotates which participant is speaking over time`() {
        var session = freshSession()
        val speakingIndexes = mutableSetOf<Int>()
        repeat(30) {
            session = RideSimulator.advance(session)
            speakingIndexes.add(session.participants.indexOfFirst { it.isSpeaking })
        }
        assertTrue("expected more than one participant to speak over 30 ticks", speakingIndexes.size > 1)
    }

    @Test
    fun `advance on an ended session is a no-op`() {
        val ended = freshSession().copy(status = RideSessionStatus.ENDED)
        val next = RideSimulator.advance(ended)
        assertEquals(ended, next)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails (RideSimulator doesn't exist yet)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: FAIL with "unresolved reference: RideSimulator"

- [ ] **Step 3: Implement `RideSimulator.kt`**

```kotlin
package com.valid.motouring.simulation

import com.valid.motouring.data.model.GeoPoint
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RideSimulator(
    private val scope: CoroutineScope,
    initialSession: RideSession,
) {
    private val _session = MutableStateFlow(initialSession)
    val session: StateFlow<RideSession> = _session.asStateFlow()
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                _session.value = advance(_session.value)
            }
        }
    }

    fun stop() {
        job?.cancel()
        _session.value = _session.value.copy(status = RideSessionStatus.ENDED)
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val BASE_SPEED_KMH = 28.0
        private const val SPEED_VARIANCE_KMH = 6.0
        private const val SPEAKER_ROTATE_EVERY_SECONDS = 4L

        fun advance(current: RideSession): RideSession {
            if (current.status == RideSessionStatus.ENDED) return current

            val newElapsed = current.elapsedSeconds + 1
            val speed = BASE_SPEED_KMH + SPEED_VARIANCE_KMH * sin(newElapsed / 10.0)
            val distanceDeltaMeters = speed * 1000.0 / 3600.0
            val newDistance = current.distanceMeters + distanceDeltaMeters

            val totalRouteLength = totalRouteLengthMeters(current.route)
            val routeFraction = if (totalRouteLength == 0.0) 0.0 else (newDistance / totalRouteLength).coerceIn(0.0, 1.0)
            val newLeadPosition = pointAlongRoute(current.route, routeFraction)

            val speakerIndex = ((newElapsed / SPEAKER_ROTATE_EVERY_SECONDS) % current.participants.size).toInt()
            val newParticipants = current.participants.mapIndexed { index, participant ->
                participant.copy(
                    position = if (index == 0) newLeadPosition else participant.position,
                    isSpeaking = index == speakerIndex,
                )
            }

            return current.copy(
                elapsedSeconds = newElapsed,
                distanceMeters = newDistance,
                speedKmh = speed,
                participants = newParticipants,
            )
        }

        private fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
            val earthRadiusMeters = 6_371_000.0
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLon = Math.toRadians(b.lng - a.lng)
            val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            return 2 * earthRadiusMeters * asin(sqrt(h))
        }

        private fun totalRouteLengthMeters(route: List<GeoPoint>): Double =
            route.zipWithNext { a, b -> haversineMeters(a, b) }.sum()

        private fun pointAlongRoute(route: List<GeoPoint>, fraction: Double): GeoPoint {
            if (route.size < 2) return route.first()
            val targetDistance = totalRouteLengthMeters(route) * fraction
            var covered = 0.0
            for (i in 0 until route.size - 1) {
                val segStart = route[i]
                val segEnd = route[i + 1]
                val segLength = haversineMeters(segStart, segEnd)
                val reachesTarget = covered + segLength >= targetDistance
                val isLastSegment = i == route.size - 2
                if (reachesTarget || isLastSegment) {
                    val segFraction = if (segLength == 0.0) 0.0 else ((targetDistance - covered) / segLength).coerceIn(0.0, 1.0)
                    val lat = segStart.lat + (segEnd.lat - segStart.lat) * segFraction
                    val lng = segStart.lng + (segEnd.lng - segStart.lng) * segFraction
                    return GeoPoint(lat = lat, lng = lng)
                }
                covered += segLength
            }
            return route.last()
        }
    }
}
```

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.simulation.RideSimulatorTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/simulation/ app/src/test/java/com/valid/motouring/simulation/
git commit -m "feat: add RideSimulator ticker for animated ride tracking"
```

---

## Phase C — Navigation Skeleton & Pre-Auth Flow

### Task 8: Navigation skeleton, MainActivity, Splash screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/navigation/Destinations.kt`
- Create: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`
- Create: `app/src/main/java/com/valid/motouring/MainActivity.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/onboarding/SplashScreen.kt`

**Interfaces:**
- Consumes: `AppContainer` (Task 6), `MotouringTheme` (Task 2).
- Produces: `Destinations` object with every route constant used by all later screen tasks (route strings are final — copy verbatim); `MotouringNavHost(appContainer, navController)` composable that every subsequent task adds one `composable(...)` block to; `MainActivity` as the single activity entry point.

- [ ] **Step 1: Create `Destinations.kt` with every route this app will ever need**

```kotlin
package com.valid.motouring.navigation

object Destinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val VEHICLE_GARAGE_SETUP = "vehicle_garage_setup"

    const val MAIN = "main"
    const val HOME = "home"
    const val NEARBY = "nearby"
    const val CHALLENGES = "challenges"
    const val RIDES_HISTORY = "rides_history"
    const val PROFILE = "profile"

    const val CHALLENGE_DETAIL_PATTERN = "challenge_detail/{challengeId}"
    fun challengeDetail(challengeId: String) = "challenge_detail/$challengeId"

    const val BADGES = "badges"
    const val BADGE_DETAIL_PATTERN = "badge_detail/{badgeId}"
    fun badgeDetail(badgeId: String) = "badge_detail/$badgeId"

    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    const val START_RIDE = "start_ride"
    const val FRIENDS = "friends"
    const val INVITE_RIDE = "invite_ride"

    const val RIDE_SESSION_PATTERN = "ride_session/{vehicleType}/{isGroup}"
    fun rideSession(vehicleType: String, isGroup: Boolean) = "ride_session/$vehicleType/$isGroup"

    const val RIDE_SUMMARY_PATTERN = "ride_summary/{historyEntryId}"
    fun rideSummary(historyEntryId: String) = "ride_summary/$historyEntryId"

    const val CREATE_POST = "create_post"
    const val POST_DETAIL_PATTERN = "post_detail/{postId}"
    fun postDetail(postId: String) = "post_detail/$postId"
}
```

- [ ] **Step 2: Create `SplashScreen.kt` (static for now — auto-navigation is wired in Task 9 once Onboarding exists)**

```kotlin
package com.valid.motouring.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Motouring", style = MaterialTheme.typography.headlineMedium)
    }
}
```

- [ ] **Step 3: Create `MotouringNavHost.kt` wiring only Splash for now**

```kotlin
package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.onboarding.SplashScreen

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen()
        }
    }
}
```

- [ ] **Step 4: Create `MainActivity.kt`**

```kotlin
package com.valid.motouring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valid.motouring.di.AppContainer
import com.valid.motouring.navigation.MotouringNavHost
import com.valid.motouring.ui.theme.MotouringTheme

class MainActivity : ComponentActivity() {
    private val appContainer = AppContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotouringTheme {
                MotouringNavHost(appContainer = appContainer)
            }
        }
    }
}
```

- [ ] **Step 5: Build and install the app on a device/emulator, verify manually**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`, app installs. Launch "Motouring" from the launcher.
Manual check: screen shows the text "Motouring" centered on the dark charcoal background. No crash.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/valid/motouring/navigation/ app/src/main/java/com/valid/motouring/MainActivity.kt app/src/main/java/com/valid/motouring/ui/onboarding/SplashScreen.kt
git commit -m "feat: add navigation skeleton, MainActivity, and static splash screen"
```

---

### Task 9: Onboarding screen + Splash auto-navigation

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/SplashScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `Destinations.ONBOARDING`, `Destinations.LOGIN` (Task 8).
- Produces: `SplashScreen(onTimeout: () -> Unit)`, `OnboardingScreen(onFinished: () -> Unit)` — both take plain lambdas, no `NavController` dependency, so they stay independently previewable/testable.

- [ ] **Step 1: Modify `SplashScreen.kt` to accept a timeout callback**

```kotlin
package com.valid.motouring.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Motouring", style = MaterialTheme.typography.headlineMedium)
    }
}
```

- [ ] **Step 2: Create `OnboardingScreen.kt`**

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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
```

- [ ] **Step 3: Modify `MotouringNavHost.kt` to wire Splash's timeout and add the Onboarding destination**

```kotlin
package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.onboarding.OnboardingScreen
import com.valid.motouring.ui.onboarding.SplashScreen

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Destinations.LOGIN) },
            )
        }
    }
}
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: launch the app, see "Motouring" splash for ~1.2s, then land on Onboarding page 1 ("Ride Together"). Swipe left twice through pages 2 and 3, or tap "Next". On page 3 ("Earn Badges & Challenges") the button changes to "Get Started". Do not tap it yet — `Destinations.LOGIN` isn't wired into the graph until Task 10, so tapping it now would crash.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/onboarding/ app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add onboarding carousel and wire splash auto-navigation"
```

---

### Task 10: Login/Signup screen (mock)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/onboarding/LoginScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `Destinations.LOGIN`, `Destinations.VEHICLE_GARAGE_SETUP` (Task 8).
- Produces: `LoginScreen(onLoginSuccess: () -> Unit)` — any non-blank username and password proceeds, no validation beyond non-blank.

- [ ] **Step 1: Create `LoginScreen.kt`**

```kotlin
package com.valid.motouring.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Welcome back", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Log in to continue riding with your crew", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username or email") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true,
        )

        Button(
            onClick = onLoginSuccess,
            enabled = username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log In")
        }
    }
}
```

- [ ] **Step 2: Modify `MotouringNavHost.kt` to add the Login destination**

```kotlin
package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.onboarding.LoginScreen
import com.valid.motouring.ui.onboarding.OnboardingScreen
import com.valid.motouring.ui.onboarding.SplashScreen

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Destinations.LOGIN) },
            )
        }
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Destinations.VEHICLE_GARAGE_SETUP) },
            )
        }
    }
}
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: walk Splash → Onboarding → tap "Get Started" → lands on Login. "Log In" button is disabled with both fields empty. Type any text into both fields — button becomes enabled. Do not tap it yet — `Destinations.VEHICLE_GARAGE_SETUP` isn't wired until Task 11.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/onboarding/LoginScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add mock login/signup screen"
```

---

### Task 11: Vehicle Garage Setup screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleGarageViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/vehicle/VehicleGarageSetupScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `VehicleRepository.addVehicle(vehicle)` (Task 5), `Vehicle`/`VehicleType` (Task 3), `AppContainer` (Task 6), `Destinations.VEHICLE_GARAGE_SETUP`/`Destinations.MAIN` (Task 8).
- Produces: `VehicleGarageViewModel(vehicleRepository, currentUserId)` with `fun addVehicle(type, make, model, year)` and a `companion object { fun factory(...) }`; `VehicleGarageSetupScreen(viewModel, onVehicleAdded: () -> Unit)`.

- [ ] **Step 1: Create `VehicleGarageViewModel.kt`**

```kotlin
package com.valid.motouring.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.VehicleRepository

class VehicleGarageViewModel(
    private val vehicleRepository: VehicleRepository,
    private val currentUserId: String,
) : ViewModel() {

    fun addVehicle(type: VehicleType, make: String, model: String, year: Int) {
        val photoRes = if (type == VehicleType.MOTORCYCLE) {
            R.drawable.ic_vehicle_motorcycle_placeholder
        } else {
            R.drawable.ic_vehicle_car_placeholder
        }
        vehicleRepository.addVehicle(
            Vehicle(
                id = "v-${System.currentTimeMillis()}",
                ownerId = currentUserId,
                type = type,
                make = make,
                model = model,
                year = year,
                photoRes = photoRes,
            ),
        )
    }

    companion object {
        fun factory(vehicleRepository: VehicleRepository, currentUserId: String) = viewModelFactory {
            initializer { VehicleGarageViewModel(vehicleRepository, currentUserId) }
        }
    }
}
```

- [ ] **Step 2: Create `VehicleGarageSetupScreen.kt`**

```kotlin
package com.valid.motouring.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valid.motouring.data.model.VehicleType

@Composable
fun VehicleGarageSetupScreen(
    viewModel: VehicleGarageViewModel,
    onVehicleAdded: () -> Unit,
) {
    var selectedType by remember { mutableStateOf(VehicleType.MOTORCYCLE) }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    val yearValue = year.toIntOrNull()
    val canContinue = make.isNotBlank() && model.isNotBlank() && yearValue != null

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Add your first ride", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Register a motorcycle or car so we know what you're riding",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == VehicleType.MOTORCYCLE,
                onClick = { selectedType = VehicleType.MOTORCYCLE },
                label = { Text("Motorcycle") },
            )
            FilterChip(
                selected = selectedType == VehicleType.CAR,
                onClick = { selectedType = VehicleType.CAR },
                label = { Text("Car") },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = make,
            onValueChange = { make = it },
            label = { Text("Make (e.g. Yamaha)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model (e.g. MT-25)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter(Char::isDigit) },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.addVehicle(selectedType, make, model, requireNotNull(yearValue))
                onVehicleAdded()
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}
```

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Vehicle Garage Setup destination**

```kotlin
package com.valid.motouring.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valid.motouring.di.AppContainer
import com.valid.motouring.ui.onboarding.LoginScreen
import com.valid.motouring.ui.onboarding.OnboardingScreen
import com.valid.motouring.ui.onboarding.SplashScreen
import com.valid.motouring.ui.vehicle.VehicleGarageSetupScreen
import com.valid.motouring.ui.vehicle.VehicleGarageViewModel

@Composable
fun MotouringNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = { navController.navigate(Destinations.LOGIN) },
            )
        }
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Destinations.VEHICLE_GARAGE_SETUP) },
            )
        }
        composable(Destinations.VEHICLE_GARAGE_SETUP) {
            val viewModel: VehicleGarageViewModel = viewModel(
                factory = VehicleGarageViewModel.factory(
                    appContainer.vehicleRepository,
                    appContainer.userRepository.currentUser().id,
                ),
            )
            VehicleGarageSetupScreen(
                viewModel = viewModel,
                onVehicleAdded = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: walk Splash → Onboarding → Login → tap "Log In" → lands on Vehicle Garage Setup. "Continue" is disabled until Make, Model, and a numeric Year are all filled. Toggle between Motorcycle/Car chips works. Do not tap "Continue" yet — `Destinations.MAIN` isn't wired until Task 12.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/vehicle/ app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add vehicle garage setup screen"
```

---

## Phase D — Main App Shell & Home Feed

### Task 12: Shared components, bottom-nav scaffold, and Home feed

**Files:**
- Modify: `app/build.gradle.kts` (add `material-icons-extended`)
- Create: `app/src/main/java/com/valid/motouring/ui/components/SectionHeader.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/StatBlock.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/RideBuddyAvatarRow.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/BadgeChip.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/components/PostCard.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/main/BottomTab.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `Post`, `Badge`, `Challenge` models (Task 3); `PostRepository`, `ChallengeRepository` (Task 5); `AppContainer` (Task 6); `Destinations.MAIN/HOME/NEARBY/CHALLENGES/RIDES_HISTORY/PROFILE/START_RIDE/CREATE_POST` and `Destinations.postDetail(id)` (Task 8).
- Produces: `SectionHeader(title, actionLabel?, onActionClick?)`, `StatBlock(label, value)`, `RideBuddyAvatarRow(avatarResList, maxVisible)`, `BadgeChip(badge, onClick)`, `PostCard(post, onLikeClick, onCardClick)` — all reused by every later screen task. `MainScaffold(appContainer, outerNavController)` with an `implementedTabRoutes` set that Tasks 15/16/17/18/24 each add one entry to. `HomeViewModel(postRepository, challengeRepository)` with `val posts`, `val featuredChallenge`, `fun toggleLike(postId)`.

- [ ] **Step 1: Modify `app/build.gradle.kts` to add the extended Material icon set**

Add this line inside the existing `dependencies { ... }` block, right after the `material3` line:

```kotlin
    implementation("androidx.compose.material:material-icons-extended")
```

- [ ] **Step 2: Create `SectionHeader.kt`**

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
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) { Text(actionLabel) }
        }
    }
}
```

- [ ] **Step 3: Create `StatBlock.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 4: Create `RideBuddyAvatarRow.kt`**

```kotlin
package com.valid.motouring.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image

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
                modifier = Modifier.size(32.dp).clip(CircleShape).padding(1.dp),
            )
        }
        val overflow = avatarResList.size - maxVisible
        if (overflow > 0) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+$overflow", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
```

- [ ] **Step 5: Create `BadgeChip.kt`**

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge

@Composable
fun BadgeChip(badge: Badge, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = badge.iconRes),
            contentDescription = badge.title,
            modifier = Modifier.size(56.dp).alpha(if (badge.isEarned) 1f else 0.35f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}
```

- [ ] **Step 6: Create `PostCard.kt`**

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
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
    Card(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
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
                    modifier = Modifier.fillMaxWidth().height(160.dp),
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
```

- [ ] **Step 7: Create `HomeViewModel.kt`**

```kotlin
package com.valid.motouring.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.data.model.Post
import com.valid.motouring.data.repository.ChallengeRepository
import com.valid.motouring.data.repository.PostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val postRepository: PostRepository,
    challengeRepository: ChallengeRepository,
) : ViewModel() {

    val posts: StateFlow<List<Post>> = postRepository.observePosts()

    val featuredChallenge: StateFlow<Challenge?> = challengeRepository.observeChallenges()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = challengeRepository.observeChallenges().value.firstOrNull(),
        )

    fun toggleLike(postId: String) = postRepository.toggleLike(postId)

    companion object {
        fun factory(postRepository: PostRepository, challengeRepository: ChallengeRepository) = viewModelFactory {
            initializer { HomeViewModel(postRepository, challengeRepository) }
        }
    }
}
```

- [ ] **Step 8: Create `HomeScreen.kt`**

```kotlin
package com.valid.motouring.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.PostCard
import com.valid.motouring.ui.components.SectionHeader

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
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(challenge.title, style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { (challenge.currentValue / challenge.goalValue).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = onCreatePostClick)
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLikeClick = { viewModel.toggleLike(post.id) },
                onCardClick = { onPostClick(post.id) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}
```

- [ ] **Step 9: Create `BottomTab.kt`**

```kotlin
package com.valid.motouring.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.valid.motouring.navigation.Destinations

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomTab(Destinations.HOME, "Home", Icons.Filled.Home)
    data object Nearby : BottomTab(Destinations.NEARBY, "Nearby", Icons.Filled.Place)
    data object Challenges : BottomTab(Destinations.CHALLENGES, "Challenges", Icons.Filled.EmojiEvents)
    data object Rides : BottomTab(Destinations.RIDES_HISTORY, "Rides", Icons.Filled.History)
    data object Profile : BottomTab(Destinations.PROFILE, "Profile", Icons.Filled.Person)

    companion object {
        val all = listOf(Home, Nearby, Challenges, Rides, Profile)
    }
}
```

- [ ] **Step 10: Create `MainScaffold.kt`**

```kotlin
package com.valid.motouring.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.valid.motouring.di.AppContainer
import com.valid.motouring.navigation.Destinations
import com.valid.motouring.ui.home.HomeScreen
import com.valid.motouring.ui.home.HomeViewModel

// Tab tasks (Nearby: Task 15, Challenges/Badges: Tasks 16-17, Rides: Task 18, Profile: Tasks 24-27)
// each add their own route to this set once their composable() entry below is wired in.
private val implementedTabRoutes = setOf(BottomTab.Home.route)

@Composable
fun MainScaffold(
    appContainer: AppContainer,
    outerNavController: NavHostController,
) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = tabNavController.currentBackStackEntryAsState().value?.destination
                BottomTab.all.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        enabled = tab.route in implementedTabRoutes,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = BottomTab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomTab.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(appContainer.postRepository, appContainer.challengeRepository),
                )
                HomeScreen(
                    viewModel = viewModel,
                    onStartRideClick = { outerNavController.navigate(Destinations.START_RIDE) },
                    onPostClick = { postId -> outerNavController.navigate(Destinations.postDetail(postId)) },
                    onCreatePostClick = { outerNavController.navigate(Destinations.CREATE_POST) },
                )
            }
        }
    }
}
```

Note: `currentDestination?.hierarchy` uses `androidx.navigation.NavDestination.hierarchy`, add `import androidx.navigation.NavDestination.Companion.hierarchy` alongside the other imports above.

- [ ] **Step 11: Modify `MotouringNavHost.kt` to add the Main destination**

Add this import and `composable` block to the `NavHost` builder created in Task 11 (after the `VEHICLE_GARAGE_SETUP` block):

```kotlin
import com.valid.motouring.ui.main.MainScaffold
```

```kotlin
        composable(Destinations.MAIN) {
            MainScaffold(appContainer = appContainer, outerNavController = navController)
        }
```

- [ ] **Step 12: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: walk Splash → Onboarding → Login → Vehicle Garage Setup → fill the form → tap "Continue". Lands on Home tab showing the "Start Group Ride" button, the "Ride 100km This Week" progress card, and a scrollable feed of 3 seeded posts with working like buttons (heart fills/count increments on tap, un-taps to decrement). The other 4 bottom nav items are visible but disabled/dimmed — that's expected until their tasks land.

- [ ] **Step 13: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/valid/motouring/ui/components/ app/src/main/java/com/valid/motouring/ui/main/ app/src/main/java/com/valid/motouring/ui/home/ app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add shared components, bottom-nav scaffold, and Home feed"
```

---

## Phase E — Social: Create Post & Post Detail

### Task 13: PostViewModel (create + detail) and Create Post screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/social/PostViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/social/CreatePostScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `PostRepository`, `RideRepository` (Task 5), `Post`/`Comment`/`RideHistoryEntry` (Task 3), `AppContainer.userRepository.currentUser()` (Task 5/6).
- Produces: `PostViewModel(postRepository, rideRepository, currentUserId, currentUserName, currentUserAvatarRes, postId: String?)` with `val post: StateFlow<Post?>`, `val comments: StateFlow<List<Comment>>`, `val rideHistory: StateFlow<List<RideHistoryEntry>>`, `fun toggleLike()`, `fun addComment(text: String)`, `fun createPost(caption: String, attachedRideId: String?)`. `CreatePostScreen(viewModel, onPosted: () -> Unit)`. Task 14 reuses this same `PostViewModel` in detail mode (non-null `postId`).

- [ ] **Step 1: Create `PostViewModel.kt`**

```kotlin
package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.model.Comment
import com.valid.motouring.data.model.Post
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.repository.PostRepository
import com.valid.motouring.data.repository.RideRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PostViewModel(
    private val postRepository: PostRepository,
    rideRepository: RideRepository,
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentUserAvatarRes: Int,
    private val postId: String?,
) : ViewModel() {

    val post: StateFlow<Post?> = postRepository.observePosts()
        .map { posts -> posts.firstOrNull { it.id == postId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val comments: StateFlow<List<Comment>> = postRepository.observePosts()
        .map { postId?.let { postRepository.commentsFor(it) } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rideHistory: StateFlow<List<RideHistoryEntry>> = rideRepository.observeHistory()

    fun toggleLike() {
        postId?.let { postRepository.toggleLike(it) }
    }

    fun addComment(text: String) {
        val targetPostId = postId ?: return
        if (text.isBlank()) return
        postRepository.addComment(
            Comment(
                id = "cm-${System.currentTimeMillis()}",
                postId = targetPostId,
                authorId = currentUserId,
                authorName = currentUserName,
                authorAvatarRes = currentUserAvatarRes,
                text = text,
                createdAtEpochSeconds = System.currentTimeMillis() / 1000,
            ),
        )
    }

    fun createPost(caption: String, attachedRideId: String?) {
        postRepository.addPost(
            Post(
                id = "post-${System.currentTimeMillis()}",
                authorId = currentUserId,
                authorName = currentUserName,
                authorAvatarRes = currentUserAvatarRes,
                photoResList = listOf(R.drawable.ic_photo_placeholder),
                caption = caption,
                attachedRideId = attachedRideId,
                likeCount = 0,
                likedByMe = false,
                commentIds = emptyList(),
                createdAtEpochSeconds = System.currentTimeMillis() / 1000,
            ),
        )
    }

    companion object {
        fun factory(
            postRepository: PostRepository,
            rideRepository: RideRepository,
            currentUserId: String,
            currentUserName: String,
            currentUserAvatarRes: Int,
            postId: String?,
        ) = viewModelFactory {
            initializer {
                PostViewModel(postRepository, rideRepository, currentUserId, currentUserName, currentUserAvatarRes, postId)
            }
        }
    }
}
```

- [ ] **Step 2: Create `CreatePostScreen.kt`**

```kotlin
package com.valid.motouring.ui.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.R
import com.valid.motouring.data.model.RideHistoryEntry

@Composable
fun CreatePostScreen(
    viewModel: PostViewModel,
    onPosted: () -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    var attachedRide by remember { mutableStateOf<RideHistoryEntry?>(null) }
    var rideMenuExpanded by remember { mutableStateOf(false) }
    val rideHistory by viewModel.rideHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "New Post", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_photo_placeholder),
            contentDescription = "Selected photo",
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column {
            TextButton(onClick = { rideMenuExpanded = true }) {
                Text(attachedRide?.title ?: "Attach a ride (optional)")
            }
            DropdownMenu(expanded = rideMenuExpanded, onDismissRequest = { rideMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = { attachedRide = null; rideMenuExpanded = false },
                )
                rideHistory.forEach { ride ->
                    DropdownMenuItem(
                        text = { Text(ride.title) },
                        onClick = { attachedRide = ride; rideMenuExpanded = false },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = {
                viewModel.createPost(caption, attachedRide?.id)
                onPosted()
            },
            enabled = caption.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Post")
        }
    }
}
```

Note: `Column` doesn't support `Modifier.weight` directly outside a `Column`/`Row` scope — since this `Column { ... Spacer(Modifier.weight(1f)) ... }` is already inside the outer `Column`, that's correct; just make sure the `Spacer` line sits directly inside the outer `Column`'s trailing lambda (it does, per the code above), not inside the ride-picker's inner `Column`.

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Create Post destination**

Add these imports:

```kotlin
import com.valid.motouring.ui.social.CreatePostScreen
import com.valid.motouring.ui.social.PostViewModel
```

Add this `composable` block (after the `Destinations.MAIN` block):

```kotlin
        composable(Destinations.CREATE_POST) {
            val currentUser = appContainer.userRepository.currentUser()
            val viewModel: PostViewModel = viewModel(
                factory = PostViewModel.factory(
                    appContainer.postRepository,
                    appContainer.rideRepository,
                    currentUser.id,
                    currentUser.name,
                    currentUser.avatarRes,
                    postId = null,
                ),
            )
            CreatePostScreen(
                viewModel = viewModel,
                onPosted = { navController.popBackStack() },
            )
        }
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Home, tap "New Post" next to the Feed header. Type a caption, optionally attach a ride from the dropdown, tap "Post". You're returned to Home and the new post appears at the top of the feed with your name and avatar.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/PostViewModel.kt app/src/main/java/com/valid/motouring/ui/social/CreatePostScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add create post flow"
```

---

### Task 14: Post Detail screen (comments + like)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/social/PostDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `PostViewModel` (Task 13, instantiated here with a non-null `postId`), `PostCard` (Task 12).
- Produces: `PostDetailScreen(viewModel: PostViewModel)` — renders the post, its comment thread, and a comment-input bar.

- [ ] **Step 1: Create `PostDetailScreen.kt`**

```kotlin
package com.valid.motouring.ui.social

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.PostCard

@Composable
fun PostDetailScreen(viewModel: PostViewModel) {
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    val currentPost = post ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item {
                PostCard(post = currentPost, onLikeClick = viewModel::toggleLike, onCardClick = {})
                Spacer(modifier = Modifier.height(16.dp))
                Text("Comments", style = MaterialTheme.typography.titleMedium)
            }
            items(comments, key = { it.id }) { comment ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Image(
                        painter = painterResource(id = comment.authorAvatarRes),
                        contentDescription = comment.authorName,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = comment.authorName, style = MaterialTheme.typography.labelSmall)
                        Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment") },
            )
            IconButton(
                onClick = {
                    viewModel.addComment(commentText)
                    commentText = ""
                },
                enabled = commentText.isNotBlank(),
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
```

- [ ] **Step 2: Modify `MotouringNavHost.kt` to add the Post Detail destination**

Add these imports:

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.valid.motouring.ui.social.PostDetailScreen
```

Add this `composable` block (after the `Destinations.CREATE_POST` block):

```kotlin
        composable(
            Destinations.POST_DETAIL_PATTERN,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val postId = requireNotNull(backStackEntry.arguments?.getString("postId"))
            val currentUser = appContainer.userRepository.currentUser()
            val viewModel: PostViewModel = viewModel(
                factory = PostViewModel.factory(
                    appContainer.postRepository,
                    appContainer.rideRepository,
                    currentUser.id,
                    currentUser.name,
                    currentUser.avatarRes,
                    postId = postId,
                ),
            )
            PostDetailScreen(viewModel = viewModel)
        }
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Home, tap a post card. Lands on Post Detail showing the same post, its seeded comments below, and a comment input bar. Type a comment and tap send — it appears at the bottom of the comment list immediately. Tapping the like heart on the detail screen updates the count.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/PostDetailScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add post detail screen with comments"
```

---

## Phase F — Nearby Map

### Task 15: Nearby screen (Mapbox POIs, vehicle-type filter)

This is the first task that actually depends on Mapbox — Tasks 1-14 deliberately built and tested without it. This task adds the Mapbox maven repo, the SDK dependencies, and wires the runtime access token, then builds the screen.

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/valid/motouring/MotouringApplication.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/nearby/NearbyViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/nearby/NearbyScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `PoiRepository` (Task 5), `PointOfInterest`/`PoiType`/`VehicleType`/`GeoPoint` (Task 3), `FakeDataProvider.sampleRoute` for initial camera center (Task 4).
- Produces: `NearbyViewModel(poiRepository)` with `val selectedType: StateFlow<VehicleType?>`, `val filteredPois: StateFlow<List<PointOfInterest>>`, `fun selectType(type: VehicleType?)`. `NearbyScreen(viewModel)`. The domain layer's `GeoPoint` is converted to a real Mapbox `Point` only at this UI layer, via `Point.fromLngLat(geoPoint.lng, geoPoint.lat)`.

This task requires a real `MAPBOX_DOWNLOADS_TOKEN` and `MAPBOX_PUBLIC_TOKEN` in `local.properties` (see Task 1's `local.properties.example`) before `./gradlew` can resolve the new dependencies — get both from https://account.mapbox.com/access-tokens/ before starting this task.

- [ ] **Step 1: Modify `settings.gradle.kts` to add Mapbox's authenticated maven repo**

Add this `maven { ... }` block inside the existing `dependencyResolutionManagement { repositories { ... } }` block, after `mavenCentral()`:

```kotlin
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN")
                    .orElse(providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN"))
                    .getOrElse("")
            }
        }
```

- [ ] **Step 2: Modify `app/build.gradle.kts` to add the Mapbox dependencies**

Replace the comment block left by Task 1 with real dependency lines:

```kotlin
    implementation("com.mapbox.maps:android:11.25.0")
    implementation("com.mapbox.maps:extension-compose:11.25.0")
```

- [ ] **Step 3: Modify `MotouringApplication.kt` to set the Mapbox access token at process start**

```kotlin
package com.valid.motouring

import android.app.Application
import com.mapbox.common.MapboxOptions

class MotouringApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
    }
}
```

- [ ] **Step 4: Verify the new dependencies resolve**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If this fails with a 401/403 fetching `com.mapbox.maps:*`, the `MAPBOX_DOWNLOADS_TOKEN` in `local.properties` is missing or wrong — stop and get a valid one before continuing.

- [ ] **Step 5: Create `NearbyViewModel.kt`**

```kotlin
package com.valid.motouring.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NearbyViewModel(private val poiRepository: PoiRepository) : ViewModel() {

    private val _selectedType = MutableStateFlow<VehicleType?>(null)
    val selectedType: StateFlow<VehicleType?> = _selectedType.asStateFlow()

    val filteredPois: StateFlow<List<PointOfInterest>> =
        combine(poiRepository.observePois(), _selectedType) { pois, type ->
            if (type == null) pois else pois.filter { type in it.supportedVehicleTypes }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = poiRepository.observePois().value,
        )

    fun selectType(type: VehicleType?) {
        _selectedType.value = type
    }

    companion object {
        fun factory(poiRepository: PoiRepository) = viewModelFactory {
            initializer { NearbyViewModel(poiRepository) }
        }
    }
}
```

- [ ] **Step 6: Create `NearbyScreen.kt`**

Mapbox's generated Compose annotation composables (`CircleAnnotation`, `PointAnnotation`, etc.) occasionally rename properties between minor SDK releases — if any property below doesn't resolve, cross-check against the current guide at https://docs.mapbox.com/android/maps/guides/using-jetpack-compose/ and adjust the property name (the shape of the API — a trailing lambda setting `point`/`circleColorInt`/`circleRadius` — has been stable across v11.x).

```kotlin
package com.valid.motouring.ui.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.valid.motouring.data.model.PoiType
import com.valid.motouring.data.model.PointOfInterest
import com.valid.motouring.data.model.VehicleType

@Composable
fun NearbyScreen(viewModel: NearbyViewModel) {
    val pois by viewModel.filteredPois.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(106.8206, -6.2088))
            zoom(12.0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = selectedType == null, onClick = { viewModel.selectType(null) }, label = { Text("All") })
            FilterChip(
                selected = selectedType == VehicleType.MOTORCYCLE,
                onClick = { viewModel.selectType(VehicleType.MOTORCYCLE) },
                label = { Text("Motorcycle") },
            )
            FilterChip(
                selected = selectedType == VehicleType.CAR,
                onClick = { viewModel.selectType(VehicleType.CAR) },
                label = { Text("Car") },
            )
        }

        MapboxMap(
            modifier = Modifier.fillMaxWidth().weight(1f),
            mapViewportState = mapViewportState,
        ) {
            pois.forEach { poi ->
                CircleAnnotation(point = Point.fromLngLat(poi.location.lng, poi.location.lat)) {
                    circleColorInt = if (poi.type == PoiType.GAS_STATION) {
                        android.graphics.Color.parseColor("#FFA726")
                    } else {
                        android.graphics.Color.parseColor("#E53935")
                    }
                    circleRadius = 8.0
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
        ) {
            items(pois, key = { it.id }) { poi -> PoiListRow(poi) }
        }
    }
}

@Composable
private fun PoiListRow(poi: PointOfInterest) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = poi.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (poi.type == PoiType.GAS_STATION) "Gas station" else "Repair shop",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA89F97),
            )
            Text(text = "Rating: ${poi.rating}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

- [ ] **Step 7: Modify `MainScaffold.kt` to wire the Nearby tab**

Add this import:

```kotlin
import com.valid.motouring.ui.nearby.NearbyScreen
import com.valid.motouring.ui.nearby.NearbyViewModel
```

Change `implementedTabRoutes` to include Nearby:

```kotlin
private val implementedTabRoutes = setOf(BottomTab.Home.route, BottomTab.Nearby.route)
```

Add this `composable` block inside the tab `NavHost` (after the Home tab's block):

```kotlin
            composable(BottomTab.Nearby.route) {
                val viewModel: NearbyViewModel = viewModel(
                    factory = NearbyViewModel.factory(appContainer.poiRepository),
                )
                NearbyScreen(viewModel = viewModel)
            }
```

- [ ] **Step 8: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`. If `local.properties` is missing a real `MAPBOX_PUBLIC_TOKEN`, the map tiles won't load (blank/gray map) but the app must not crash — the filter chips and POI list below still work.
Manual check: tap the Nearby tab (now enabled). See a map with colored dots for gas stations (amber) and repair shops (red), and a scrollable list below showing all 6 seeded POIs. Tap "Motorcycle" — list and map narrow to motorcycle-supporting POIs only. Tap "Car" — narrows to car-supporting POIs. Tap "All" — back to all 6.

- [ ] **Step 9: Commit**

```bash
git add settings.gradle.kts app/build.gradle.kts app/src/main/java/com/valid/motouring/MotouringApplication.kt app/src/main/java/com/valid/motouring/ui/nearby/ app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat: add Mapbox dependency and Nearby POI map with vehicle-type filter"
```

---

## Phase G — Gamification: Challenges & Badges

### Task 16: Challenges list + Challenge Detail (leaderboard)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengesViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengesScreen.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengeDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `ChallengeRepository`, `BadgeRepository` (Task 5), `Challenge`/`Badge`/`LeaderboardEntry` (Task 3), `BadgeChip`/`SectionHeader` (Task 12), `Destinations.CHALLENGE_DETAIL_PATTERN`/`challengeDetail(id)`/`BADGES` (Task 8).
- Produces: `ChallengesViewModel(challengeRepository, badgeRepository)` with `val challenges: StateFlow<List<Challenge>>`, `val badges: StateFlow<List<Badge>>`. `ChallengesScreen(viewModel, onChallengeClick: (String) -> Unit, onSeeAllBadgesClick: () -> Unit, onBadgeClick: (String) -> Unit)`. `ChallengeDetailScreen(challenge: Challenge)` — stateless, takes the already-looked-up model directly (no ViewModel needed for a read-only detail view).

- [ ] **Step 1: Create `ChallengesViewModel.kt`**

```kotlin
package com.valid.motouring.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.ChallengeRepository
import kotlinx.coroutines.flow.StateFlow

class ChallengesViewModel(
    challengeRepository: ChallengeRepository,
    badgeRepository: BadgeRepository,
) : ViewModel() {
    val challenges: StateFlow<List<Challenge>> = challengeRepository.observeChallenges()
    val badges: StateFlow<List<Badge>> = badgeRepository.observeBadges()

    companion object {
        fun factory(challengeRepository: ChallengeRepository, badgeRepository: BadgeRepository) = viewModelFactory {
            initializer { ChallengesViewModel(challengeRepository, badgeRepository) }
        }
    }
}
```

- [ ] **Step 2: Create `ChallengesScreen.kt`**

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.SectionHeader

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
        items(challenges, key = { it.id }) { challenge ->
            ChallengeRow(challenge = challenge, onClick = { onChallengeClick(challenge.id) })
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = challenge.title, style = MaterialTheme.typography.titleMedium)
            Text(text = challenge.description, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { (challenge.currentValue / challenge.goalValue).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
```

- [ ] **Step 3: Create `ChallengeDetailScreen.kt`**

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Challenge

@Composable
fun ChallengeDetailScreen(challenge: Challenge) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(text = challenge.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = challenge.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (challenge.currentValue / challenge.goalValue).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${challenge.currentValue.toInt()} / ${challenge.goalValue.toInt()}",
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Leaderboard", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(challenge.leaderboard) { index, entry ->
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
                Text(text = entry.progressValue.toInt().toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
```

Add `import androidx.compose.foundation.lazy.itemsIndexed` alongside the other `androidx.compose.foundation.lazy.*` imports above.

- [ ] **Step 4: Modify `MainScaffold.kt` to wire the Challenges tab**

Add these imports:

```kotlin
import com.valid.motouring.ui.challenges.ChallengesScreen
import com.valid.motouring.ui.challenges.ChallengesViewModel
```

Change `implementedTabRoutes`:

```kotlin
private val implementedTabRoutes = setOf(BottomTab.Home.route, BottomTab.Nearby.route, BottomTab.Challenges.route)
```

Add this `composable` block inside the tab `NavHost`:

```kotlin
            composable(BottomTab.Challenges.route) {
                val viewModel: ChallengesViewModel = viewModel(
                    factory = ChallengesViewModel.factory(appContainer.challengeRepository, appContainer.badgeRepository),
                )
                ChallengesScreen(
                    viewModel = viewModel,
                    onChallengeClick = { id -> outerNavController.navigate(Destinations.challengeDetail(id)) },
                    onSeeAllBadgesClick = { outerNavController.navigate(Destinations.BADGES) },
                    onBadgeClick = { id -> outerNavController.navigate(Destinations.badgeDetail(id)) },
                )
            }
```

- [ ] **Step 5: Modify `MotouringNavHost.kt` to add the Challenge Detail destination**

Add these imports:

```kotlin
import com.valid.motouring.ui.challenges.ChallengeDetailScreen
```

Add this `composable` block:

```kotlin
        composable(
            Destinations.CHALLENGE_DETAIL_PATTERN,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val challengeId = requireNotNull(backStackEntry.arguments?.getString("challengeId"))
            val challenge = appContainer.challengeRepository.challenge(challengeId)
            if (challenge != null) {
                ChallengeDetailScreen(challenge = challenge)
            }
        }
```

- [ ] **Step 6: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: tap the Challenges tab (now enabled). See 3 challenge cards with progress bars, and a row of 4 badge icons below (locked ones dimmed). Tap a challenge — lands on Challenge Detail showing its full description, progress, and leaderboard ranked by progress value.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/challenges/ app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add challenges list and challenge detail with leaderboard"
```

---

### Task 17: Badges grid + Badge Detail

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/challenges/BadgesScreen.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/challenges/BadgeDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `BadgeRepository.observeBadges()` (Task 5), `Badge` (Task 3), `BadgeChip` (Task 12), `Destinations.BADGES`/`BADGE_DETAIL_PATTERN`/`badgeDetail(id)` (Task 8).
- Produces: `BadgesScreen(badges: List<Badge>, onBadgeClick: (String) -> Unit)`, `BadgeDetailScreen(badge: Badge)` — both stateless (no ViewModel; the caller collects `observeBadges()` directly since this is a read-only list).

- [ ] **Step 1: Create `BadgesScreen.kt`**

```kotlin
package com.valid.motouring.ui.challenges

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.ui.components.BadgeChip

@Composable
fun BadgesScreen(badges: List<Badge>, onBadgeClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(badges, key = { it.id }) { badge ->
            BadgeChip(
                badge = badge,
                onClick = { onBadgeClick(badge.id) },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Create `BadgeDetailScreen.kt`**

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge

@Composable
fun BadgeDetailScreen(badge: Badge) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = badge.iconRes),
            contentDescription = badge.title,
            modifier = Modifier.size(96.dp).alpha(if (badge.isEarned) 1f else 0.35f),
        )
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
```

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Badges and Badge Detail destinations**

Add these imports:

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.valid.motouring.ui.challenges.BadgeDetailScreen
import com.valid.motouring.ui.challenges.BadgesScreen
```

Add these `composable` blocks (after the `Destinations.CHALLENGE_DETAIL_PATTERN` block):

```kotlin
        composable(Destinations.BADGES) {
            val badges by appContainer.badgeRepository.observeBadges().collectAsState()
            BadgesScreen(
                badges = badges,
                onBadgeClick = { id -> navController.navigate(Destinations.badgeDetail(id)) },
            )
        }
        composable(
            Destinations.BADGE_DETAIL_PATTERN,
            arguments = listOf(navArgument("badgeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val badgeId = requireNotNull(backStackEntry.arguments?.getString("badgeId"))
            val badge = appContainer.badgeRepository.badge(badgeId)
            if (badge != null) {
                BadgeDetailScreen(badge = badge)
            }
        }
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Challenges tab, tap "See All" next to Badges — lands on a 3-column grid of all 6 seeded badges, locked ones visibly dimmed. Tap any badge — lands on Badge Detail showing its full description, unlock criteria, and earned status. Also verify tapping one of the 4 preview badges directly from the Challenges tab (Task 16) lands on the same detail screen.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/challenges/BadgesScreen.kt app/src/main/java/com/valid/motouring/ui/challenges/BadgeDetailScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add badges grid and badge detail screens"
```

---

## Phase H — Rides History

### Task 18: Rides History screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `RideRepository.observeHistory()` (Task 5), `RideHistoryEntry` (Task 3), `StatBlock` (Task 12).
- Produces: `RidesHistoryScreen(history: List<RideHistoryEntry>)` — stateless; the caller collects `observeHistory()` directly (read-only list, no derived state needed).

- [ ] **Step 1: Create `RidesHistoryScreen.kt`**

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.StatBlock

@Composable
fun RidesHistoryScreen(history: List<RideHistoryEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(history, key = { it.id }) { entry -> RideHistoryCard(entry) }
    }
}

@Composable
private fun RideHistoryCard(entry: RideHistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = entry.routePreviewRes),
                contentDescription = entry.title,
                modifier = Modifier.fillMaxWidth().height(100.dp),
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
```

- [ ] **Step 2: Modify `MainScaffold.kt` to wire the Rides tab**

Add these imports:

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.valid.motouring.ui.rides.RidesHistoryScreen
```

Change `implementedTabRoutes`:

```kotlin
private val implementedTabRoutes = setOf(
    BottomTab.Home.route,
    BottomTab.Nearby.route,
    BottomTab.Challenges.route,
    BottomTab.Rides.route,
)
```

Add this `composable` block inside the tab `NavHost`:

```kotlin
            composable(BottomTab.Rides.route) {
                val history by appContainer.rideRepository.observeHistory().collectAsState()
                RidesHistoryScreen(history = history)
            }
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: tap the Rides tab (now enabled). See 3 seeded ride cards, each with a route preview image and distance/duration/avg speed stats.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat: add rides history screen"
```

---

## Phase I — Ride Buddies & Invite

### Task 19: Friends / Ride Buddies list

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/social/FriendsViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/social/FriendsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `RideBuddyRepository` (Task 5), `RideBuddy`/`BuddyStatus` (Task 3), `Destinations.FRIENDS` (Task 8).
- Produces: `FriendsViewModel(rideBuddyRepository)` with `val buddies: StateFlow<List<RideBuddy>>`, `fun accept(userId: String)`, `fun sendRequest(userId: String)`. `FriendsScreen(viewModel)`.

- [ ] **Step 1: Create `FriendsViewModel.kt`**

```kotlin
package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.StateFlow

class FriendsViewModel(private val rideBuddyRepository: RideBuddyRepository) : ViewModel() {
    val buddies: StateFlow<List<RideBuddy>> = rideBuddyRepository.observeBuddies()

    fun accept(userId: String) = rideBuddyRepository.updateStatus(userId, BuddyStatus.FRIEND)

    fun sendRequest(userId: String) = rideBuddyRepository.updateStatus(userId, BuddyStatus.PENDING_SENT)

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { FriendsViewModel(rideBuddyRepository) }
        }
    }
}
```

- [ ] **Step 2: Create `FriendsScreen.kt`**

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
import androidx.compose.foundation.lazy.items
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

@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val buddies by viewModel.buddies.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(buddies, key = { it.user.id }) { buddy ->
            BuddyRow(
                buddy = buddy,
                onAccept = { viewModel.accept(buddy.user.id) },
                onAdd = { viewModel.sendRequest(buddy.user.id) },
            )
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

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Friends destination**

Add these imports:

```kotlin
import com.valid.motouring.ui.social.FriendsScreen
import com.valid.motouring.ui.social.FriendsViewModel
```

Add this `composable` block:

```kotlin
        composable(Destinations.FRIENDS) {
            val viewModel: FriendsViewModel = viewModel(
                factory = FriendsViewModel.factory(appContainer.rideBuddyRepository),
            )
            FriendsScreen(viewModel = viewModel)
        }
```

- [ ] **Step 4: Build and verify it compiles (no reachable entry point yet — Profile wiring lands in Task 24)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. There's no button pointing at `Destinations.FRIENDS` yet (Profile, which will link to it, isn't built until Task 24), so this task is verified by compilation only; the manual click-through check happens once Task 24 wires the entry point.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/FriendsViewModel.kt app/src/main/java/com/valid/motouring/ui/social/FriendsScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add friends/ride buddies list screen"
```

---

### Task 20: Invite to Group Ride flow

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/social/InviteRideViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/social/InviteRideScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `RideBuddyRepository.friends()` (Task 5), `RideBuddy` (Task 3), `Destinations.INVITE_RIDE` (Task 8).
- Produces: `InviteRideViewModel(rideBuddyRepository)` with `val friends: StateFlow<List<RideBuddy>>`, `val selectedUserIds: StateFlow<Set<String>>`, `fun toggleSelected(userId: String)`. `InviteRideScreen(viewModel, onDone: () -> Unit)`.

This screen lets the user browse friends and check who they'd invite — a real, interactive selection UI. It intentionally does **not** need to report the selection back to Start Ride: per the design spec, `RideSession` always simulates with a fixed cast of fake participants regardless of who was actually checked here, since there's no real backend to actually deliver an invite. Tapping "Done" simply returns to Start Ride.

- [ ] **Step 1: Create `InviteRideViewModel.kt`**

```kotlin
package com.valid.motouring.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.RideBuddy
import com.valid.motouring.data.repository.RideBuddyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InviteRideViewModel(rideBuddyRepository: RideBuddyRepository) : ViewModel() {
    val friends: StateFlow<List<RideBuddy>> = MutableStateFlow(rideBuddyRepository.friends()).asStateFlow()

    private val _selectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIds: StateFlow<Set<String>> = _selectedUserIds.asStateFlow()

    fun toggleSelected(userId: String) {
        _selectedUserIds.update { current ->
            if (userId in current) current - userId else current + userId
        }
    }

    companion object {
        fun factory(rideBuddyRepository: RideBuddyRepository) = viewModelFactory {
            initializer { InviteRideViewModel(rideBuddyRepository) }
        }
    }
}
```

- [ ] **Step 2: Create `InviteRideScreen.kt`**

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
import androidx.compose.foundation.lazy.items
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

@Composable
fun InviteRideScreen(viewModel: InviteRideViewModel, onDone: () -> Unit) {
    val friends by viewModel.friends.collectAsState()
    val selectedUserIds by viewModel.selectedUserIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item { Text(text = "Invite ride buddies", style = MaterialTheme.typography.headlineMedium) }
            items(friends, key = { it.user.id }) { buddy ->
                FriendSelectRow(
                    buddy = buddy,
                    isSelected = buddy.user.id in selectedUserIds,
                    onToggle = { viewModel.toggleSelected(buddy.user.id) },
                )
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

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Invite Ride destination**

Add these imports:

```kotlin
import com.valid.motouring.ui.social.InviteRideScreen
import com.valid.motouring.ui.social.InviteRideViewModel
```

Add this `composable` block:

```kotlin
        composable(Destinations.INVITE_RIDE) {
            val viewModel: InviteRideViewModel = viewModel(
                factory = InviteRideViewModel.factory(appContainer.rideBuddyRepository),
            )
            InviteRideScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
        }
```

- [ ] **Step 4: Build and verify it compiles (no reachable entry point yet — Start Ride wiring lands in Task 21)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/social/InviteRideViewModel.kt app/src/main/java/com/valid/motouring/ui/social/InviteRideScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add invite to group ride flow"
```

---

## Phase J — Ride Flow

### Task 21: Start Ride screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `VehicleRepository.vehiclesFor(userId)` (Task 5), `Vehicle`/`VehicleType` (Task 3), `Destinations.START_RIDE`/`INVITE_RIDE`/`rideSession(vehicleType, isGroup)` (Task 8).
- Produces: `StartRideScreen(vehicles: List<Vehicle>, onInviteBuddiesClick: () -> Unit, onStartRide: (VehicleType, Boolean) -> Unit)` — stateless; local UI selection (solo/group toggle, chosen vehicle) lives in `remember` state since nothing here needs to survive navigation or be observed elsewhere.

- [ ] **Step 1: Create `StartRideScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.model.VehicleType

@Composable
fun StartRideScreen(
    vehicles: List<Vehicle>,
    onInviteBuddiesClick: () -> Unit,
    onStartRide: (VehicleType, Boolean) -> Unit,
) {
    var isGroup by remember { mutableStateOf(true) }
    var selectedVehicle by remember { mutableStateOf(vehicles.firstOrNull()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Start a Ride", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !isGroup, onClick = { isGroup = false }, label = { Text("Solo") })
            FilterChip(selected = isGroup, onClick = { isGroup = true }, label = { Text("Group") })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Pick a vehicle", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        vehicles.forEach { vehicle ->
            FilterChip(
                selected = selectedVehicle?.id == vehicle.id,
                onClick = { selectedVehicle = vehicle },
                label = { Text("${vehicle.make} ${vehicle.model}") },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (isGroup) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onInviteBuddiesClick, modifier = Modifier.fillMaxWidth()) {
                Text("Invite Ride Buddies")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { selectedVehicle?.let { onStartRide(it.type, isGroup) } },
            enabled = selectedVehicle != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Ride")
        }
    }
}
```

- [ ] **Step 2: Modify `MotouringNavHost.kt` to add the Start Ride destination**

Add this import:

```kotlin
import com.valid.motouring.ui.rides.StartRideScreen
```

Add this `composable` block:

```kotlin
        composable(Destinations.START_RIDE) {
            val currentUser = appContainer.userRepository.currentUser()
            val vehicles = appContainer.vehicleRepository.vehiclesFor(currentUser.id)
            StartRideScreen(
                vehicles = vehicles,
                onInviteBuddiesClick = { navController.navigate(Destinations.INVITE_RIDE) },
                onStartRide = { vehicleType, isGroup ->
                    navController.navigate(Destinations.rideSession(vehicleType.name, isGroup))
                },
            )
        }
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Home, tap "Start Group Ride" — lands on Start Ride showing your registered vehicle as a selectable chip (from Vehicle Garage Setup). Toggle Solo/Group. With Group selected, tap "Invite Ride Buddies" — lands on the Invite screen (Task 20), check a couple of friends, tap "Done (N invited)" — returns here. Do not tap "Start Ride" yet — `Destinations.RIDE_SESSION_PATTERN` isn't wired until Task 22.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add start ride screen"
```

---

### Task 22: Ride Session screen (animated map, live stats, voice call bar)

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt`
- Test: `app/src/test/java/com/valid/motouring/ui/rides/RideSessionViewModelTest.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `RideSimulator` (Task 7), `RideRepository` (Task 5), `FakeDataProvider.sampleRoute`/`rideBuddies` (Task 4), `User`/`VehicleType`/`RideSession`/`RideHistoryEntry`/`GeoPoint` (Task 3), `Destinations.RIDE_SESSION_PATTERN`/`rideSession(...)`/`rideSummary(...)` (Task 8), Mapbox (added in Task 15).
- Produces: `RideSessionViewModel(rideRepository, currentUser, vehicleType, isGroup)` with `val session: StateFlow<RideSession>`, `fun endRide(): String` (returns the new `RideHistoryEntry.id`). `RideSessionScreen(viewModel, onRideEnded: (String) -> Unit)` — the ViewModel and its `RideSession` stay in terms of this app's own `GeoPoint`; the screen converts to a real Mapbox `Point` only where it calls into `MapboxMap`/`CircleAnnotation`.

- [ ] **Step 1: Write the failing ViewModel tests**

```kotlin
package com.valid.motouring.ui.rides

import com.valid.motouring.data.model.User
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.RideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RideSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testUser = User("u-test", "Test Rider", 0, emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `session starts with zero distance`() {
        val viewModel = RideSessionViewModel(RideRepository(), testUser, VehicleType.MOTORCYCLE, isGroup = false)
        assertEquals(0.0, viewModel.session.value.distanceMeters, 0.0001)
    }

    @Test
    fun `session ticks forward as virtual time advances`() {
        val viewModel = RideSessionViewModel(RideRepository(), testUser, VehicleType.MOTORCYCLE, isGroup = false)
        testDispatcher.scheduler.advanceTimeBy(5_000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.session.value.elapsedSeconds > 0)
    }

    @Test
    fun `solo ride has exactly one participant`() {
        val viewModel = RideSessionViewModel(RideRepository(), testUser, VehicleType.MOTORCYCLE, isGroup = false)
        assertEquals(1, viewModel.session.value.participants.size)
    }

    @Test
    fun `group ride includes fake ride buddies`() {
        val viewModel = RideSessionViewModel(RideRepository(), testUser, VehicleType.MOTORCYCLE, isGroup = true)
        assertTrue(viewModel.session.value.participants.size > 1)
    }

    @Test
    fun `endRide stops the simulator and records ride history`() {
        val rideRepository = RideRepository()
        val viewModel = RideSessionViewModel(rideRepository, testUser, VehicleType.CAR, isGroup = false)
        testDispatcher.scheduler.advanceTimeBy(3_000)
        testDispatcher.scheduler.runCurrent()

        val historyId = viewModel.endRide()

        assertTrue(rideRepository.observeHistory().value.any { it.id == historyId })
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail (RideSessionViewModel doesn't exist yet)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.ui.rides.RideSessionViewModelTest"`
Expected: FAIL with "unresolved reference: RideSessionViewModel"

- [ ] **Step 3: Create `RideSessionViewModel.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.BuddyStatus
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.User
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.simulation.RideSimulator
import kotlinx.coroutines.flow.StateFlow

class RideSessionViewModel(
    private val rideRepository: RideRepository,
    private val currentUser: User,
    private val vehicleType: VehicleType,
    private val isGroup: Boolean,
) : ViewModel() {

    private val simulator = RideSimulator(
        scope = viewModelScope,
        initialSession = buildInitialSession(),
    )

    val session: StateFlow<RideSession> = simulator.session

    init {
        simulator.start()
    }

    fun endRide(): String {
        simulator.stop()
        val finalSession = simulator.session.value
        val hours = finalSession.elapsedSeconds / 3600.0
        val entry = RideHistoryEntry(
            id = "r-${System.currentTimeMillis()}",
            title = if (isGroup) "Group Ride" else "Solo Ride",
            vehicleType = vehicleType,
            distanceMeters = finalSession.distanceMeters,
            durationSeconds = finalSession.elapsedSeconds,
            avgSpeedKmh = if (hours > 0) (finalSession.distanceMeters / 1000.0) / hours else 0.0,
            routePreviewRes = R.drawable.ic_route_preview_placeholder,
            photoResList = emptyList(),
            completedAtEpochSeconds = System.currentTimeMillis() / 1000,
        )
        rideRepository.addHistoryEntry(entry)
        return entry.id
    }

    override fun onCleared() {
        simulator.stop()
    }

    private fun buildInitialSession(): RideSession {
        val route = FakeDataProvider.sampleRoute
        val participants = mutableListOf(
            RideParticipantState(currentUser.id, currentUser.name, currentUser.avatarRes, route.first()),
        )
        if (isGroup) {
            val fakeBuddies = FakeDataProvider.rideBuddies
                .filter { it.status == BuddyStatus.FRIEND }
                .take(3)
            participants += fakeBuddies.map {
                RideParticipantState(it.user.id, it.user.name, it.user.avatarRes, route.first())
            }
        }
        return RideSession(
            id = "session-${System.currentTimeMillis()}",
            vehicleType = vehicleType,
            route = route,
            participants = participants,
            distanceMeters = 0.0,
            speedKmh = 0.0,
            elapsedSeconds = 0,
            status = RideSessionStatus.ACTIVE,
        )
    }

    companion object {
        fun factory(
            rideRepository: RideRepository,
            currentUser: User,
            vehicleType: VehicleType,
            isGroup: Boolean,
        ) = viewModelFactory {
            initializer { RideSessionViewModel(rideRepository, currentUser, vehicleType, isGroup) }
        }
    }
}
```

- [ ] **Step 4: Run the tests and verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.valid.motouring.ui.rides.RideSessionViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 5: Create `RideSessionScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.ui.components.StatBlock
import com.valid.motouring.ui.theme.MotouringColors

@Composable
fun RideSessionScreen(viewModel: RideSessionViewModel, onRideEnded: (String) -> Unit) {
    val session by viewModel.session.collectAsState()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            val start = session.route.first()
            center(Point.fromLngLat(start.lng, start.lat))
            zoom(13.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = mapViewportState) {
            session.participants.forEach { participant ->
                CircleAnnotation(point = Point.fromLngLat(participant.position.lng, participant.position.lat)) {
                    circleColorInt = if (participant.isSpeaking) {
                        android.graphics.Color.parseColor("#FFA726")
                    } else {
                        android.graphics.Color.parseColor("#F5F1EC")
                    }
                    circleRadius = 10.0
                }
            }
        }

        Card(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatBlock(label = "Distance", value = "${"%.1f".format(session.distanceMeters / 1000.0)} km")
                StatBlock(label = "Speed", value = "${session.speedKmh.toInt()} km/h")
                StatBlock(label = "Time", value = formatElapsed(session.elapsedSeconds))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(modifier = Modifier.padding(12.dp)) {
                    session.participants.forEach { participant ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            val avatarModifier = if (participant.isSpeaking) {
                                Modifier.size(40.dp).clip(CircleShape)
                                    .border(2.dp, MotouringColors.speakingNowHighlight, CircleShape)
                            } else {
                                Modifier.size(40.dp).clip(CircleShape)
                            }
                            Image(
                                painter = painterResource(id = participant.avatarRes.let { if (it == 0) android.R.drawable.ic_menu_report_image else it }),
                                contentDescription = participant.name,
                                modifier = avatarModifier,
                            )
                            Text(text = participant.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Button(
                onClick = { onRideEnded(viewModel.endRide()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("End Ride")
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
```

Note: the `participant.avatarRes.let { if (it == 0) ... }` guard only matters for the unit-test-constructed `RideParticipantState("u-test", "Test Rider", 0, ...)` in Step 1's tests, which never reaches this Composable — in the running app every participant comes from `FakeDataProvider`/`AppContainer.userRepository.currentUser()`, which always has a real drawable resource. Feel free to drop the guard and just use `painterResource(id = participant.avatarRes)` directly.

- [ ] **Step 6: Modify `MotouringNavHost.kt` to add the Ride Session destination**

Add these imports:

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.ui.rides.RideSessionScreen
import com.valid.motouring.ui.rides.RideSessionViewModel
```

(`NavType`/`navArgument` may already be imported from Task 14 — don't duplicate the import line.)

Add this `composable` block:

```kotlin
        composable(
            Destinations.RIDE_SESSION_PATTERN,
            arguments = listOf(
                navArgument("vehicleType") { type = NavType.StringType },
                navArgument("isGroup") { type = NavType.BoolType },
            ),
        ) { backStackEntry ->
            val vehicleType = VehicleType.valueOf(requireNotNull(backStackEntry.arguments?.getString("vehicleType")))
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            val currentUser = appContainer.userRepository.currentUser()
            val viewModel: RideSessionViewModel = viewModel(
                factory = RideSessionViewModel.factory(appContainer.rideRepository, currentUser, vehicleType, isGroup),
            )
            RideSessionScreen(
                viewModel = viewModel,
                onRideEnded = { historyEntryId ->
                    navController.navigate(Destinations.rideSummary(historyEntryId)) {
                        popUpTo(Destinations.START_RIDE) { inclusive = true }
                    }
                },
            )
        }
```

- [ ] **Step 7: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Start Ride, tap "Start Ride". Lands on a full-screen map with a stats card on top (distance/speed/time all ticking up roughly once per second) and a participant bar on the bottom (the speaking highlight rotates between avatars every few seconds). Do not tap "End Ride" yet — `Destinations.RIDE_SUMMARY_PATTERN` isn't wired until Task 23.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSessionViewModel.kt app/src/main/java/com/valid/motouring/ui/rides/RideSessionScreen.kt app/src/test/java/com/valid/motouring/ui/rides/ app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add animated ride session screen with live stats and voice call bar"
```

---

### Task 23: Ride Summary screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `RideRepository.observeHistory()`, `BadgeRepository.observeBadges()` (Task 5), `RideHistoryEntry`/`Badge` (Task 3), `StatBlock`/`BadgeChip` (Task 12), `Destinations.RIDE_SUMMARY_PATTERN` (Task 8).
- Produces: `RideSummaryScreen(entry: RideHistoryEntry, earnedBadges: List<Badge>, onDone: () -> Unit)` — stateless.

- [ ] **Step 1: Create `RideSummaryScreen.kt`**

```kotlin
package com.valid.motouring.ui.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.RideHistoryEntry
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock

@Composable
fun RideSummaryScreen(
    entry: RideHistoryEntry,
    earnedBadges: List<Badge>,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Ride Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = entry.routePreviewRes),
            contentDescription = entry.title,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = entry.title, style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatBlock(label = "Distance", value = "${"%.1f".format(entry.distanceMeters / 1000.0)} km")
            StatBlock(label = "Duration", value = "${entry.durationSeconds / 60} min")
            StatBlock(label = "Avg Speed", value = "${entry.avgSpeedKmh.toInt()} km/h")
        }

        if (earnedBadges.isNotEmpty()) {
            SectionHeader(title = "Your Badges")
            Row {
                earnedBadges.take(4).forEach { badge ->
                    BadgeChip(badge = badge, onClick = {}, modifier = Modifier.padding(end = 16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
```

- [ ] **Step 2: Modify `MotouringNavHost.kt` to add the Ride Summary destination**

Add this import:

```kotlin
import com.valid.motouring.ui.rides.RideSummaryScreen
```

Add this `composable` block:

```kotlin
        composable(
            Destinations.RIDE_SUMMARY_PATTERN,
            arguments = listOf(navArgument("historyEntryId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val historyEntryId = requireNotNull(backStackEntry.arguments?.getString("historyEntryId"))
            val entry = appContainer.rideRepository.observeHistory().value.firstOrNull { it.id == historyEntryId }
            val earnedBadges = appContainer.badgeRepository.observeBadges().value.filter { it.isEarned }
            if (entry != null) {
                RideSummaryScreen(
                    entry = entry,
                    earnedBadges = earnedBadges,
                    onDone = { navController.popBackStack() },
                )
            }
        }
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Ride Session, tap "End Ride". Lands on Ride Summary showing the ride's title, a route preview image, distance/duration/avg speed stats, and a row of already-earned badges. Tap "Done" — returns to Home, and the Rides tab now shows this new ride at the top of the history list.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add ride summary screen"
```

---

## Phase K — Profile

### Task 24: Profile screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/profile/ProfileViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt`

**Interfaces:**
- Consumes: `UserRepository.currentUser()`, `VehicleRepository`, `RideRepository`, `BadgeRepository` (Task 5); `StatBlock`/`BadgeChip` (Task 12); `Destinations.FRIENDS`/`EDIT_PROFILE`/`SETTINGS`/`NOTIFICATIONS` (Task 8).
- Produces: `ProfileViewModel(userRepository, vehicleRepository, rideRepository, badgeRepository)` with `val currentUser: User`, `val vehicles: StateFlow<List<Vehicle>>`, `val totalRides: StateFlow<Int>`, `val totalDistanceKm: StateFlow<Double>`, `val badges: StateFlow<List<Badge>>`. `ProfileScreen(viewModel, onFriendsClick, onEditProfileClick, onSettingsClick, onNotificationsClick)`.

- [ ] **Step 1: Create `ProfileViewModel.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Badge
import com.valid.motouring.data.model.User
import com.valid.motouring.data.model.Vehicle
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    userRepository: UserRepository,
    vehicleRepository: VehicleRepository,
    rideRepository: RideRepository,
    badgeRepository: BadgeRepository,
) : ViewModel() {

    val currentUser: User = userRepository.currentUser()

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.observeVehicles()
        .map { all -> all.filter { it.ownerId == currentUser.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), vehicleRepository.vehiclesFor(currentUser.id))

    val totalRides: StateFlow<Int> = rideRepository.observeHistory()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDistanceKm: StateFlow<Double> = rideRepository.observeHistory()
        .map { entries -> entries.sumOf { it.distanceMeters } / 1000.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val badges: StateFlow<List<Badge>> = badgeRepository.observeBadges()

    companion object {
        fun factory(
            userRepository: UserRepository,
            vehicleRepository: VehicleRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
        ) = viewModelFactory {
            initializer { ProfileViewModel(userRepository, vehicleRepository, rideRepository, badgeRepository) }
        }
    }
}
```

- [ ] **Step 2: Create `ProfileScreen.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.valid.motouring.ui.components.BadgeChip
import com.valid.motouring.ui.components.SectionHeader
import com.valid.motouring.ui.components.StatBlock

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
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
        items(vehicles) { vehicle ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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

Note: use `items(vehicles)` from `androidx.compose.foundation.lazy.items` — add that import alongside the others above.

- [ ] **Step 3: Modify `MainScaffold.kt` to wire the Profile tab**

Add these imports:

```kotlin
import com.valid.motouring.ui.profile.ProfileScreen
import com.valid.motouring.ui.profile.ProfileViewModel
```

Change `implementedTabRoutes`:

```kotlin
private val implementedTabRoutes = setOf(
    BottomTab.Home.route,
    BottomTab.Nearby.route,
    BottomTab.Challenges.route,
    BottomTab.Rides.route,
    BottomTab.Profile.route,
)
```

Add this `composable` block inside the tab `NavHost`:

```kotlin
            composable(BottomTab.Profile.route) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.factory(
                        appContainer.userRepository,
                        appContainer.vehicleRepository,
                        appContainer.rideRepository,
                        appContainer.badgeRepository,
                    ),
                )
                ProfileScreen(
                    viewModel = viewModel,
                    onFriendsClick = { outerNavController.navigate(Destinations.FRIENDS) },
                    onEditProfileClick = { outerNavController.navigate(Destinations.EDIT_PROFILE) },
                    onSettingsClick = { outerNavController.navigate(Destinations.SETTINGS) },
                    onNotificationsClick = { outerNavController.navigate(Destinations.NOTIFICATIONS) },
                )
            }
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: tap the Profile tab (now enabled — all 5 tabs are enabled). See your name/avatar, ride/distance/badge stats, your registered vehicle in "My Garage", and a badges preview row. Tap "Ride Buddies" — lands on the Friends screen from Task 19 (now reachable end-to-end: Accept/Add buttons work). Do not tap "Edit Profile"/"Settings"/"Notifications" yet — those land in Tasks 25-27.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/ProfileViewModel.kt app/src/main/java/com/valid/motouring/ui/profile/ProfileScreen.kt app/src/main/java/com/valid/motouring/ui/main/MainScaffold.kt
git commit -m "feat: add profile screen with stats, garage, and badges"
```

---

### Task 25: Edit Profile screen

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/data/repository/UserRepository.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/profile/EditProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `UserRepository` (Task 5), `Destinations.EDIT_PROFILE` (Task 8).
- Produces: `UserRepository.updateName(userId: String, name: String)`. `EditProfileScreen(initialName: String, onSave: (String) -> Unit)` — stateless.

- [ ] **Step 1: Modify `UserRepository.kt` to add a name-update method**

Add this method inside the existing `UserRepository` class (after `userById`):

```kotlin
    fun updateName(userId: String, name: String) {
        users.value = users.value.map { if (it.id == userId) it.copy(name = name) else it }
    }
```

- [ ] **Step 2: Create `EditProfileScreen.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditProfileScreen(initialName: String, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Edit Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSave(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}
```

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Edit Profile destination**

Add this import:

```kotlin
import com.valid.motouring.ui.profile.EditProfileScreen
```

Add this `composable` block:

```kotlin
        composable(Destinations.EDIT_PROFILE) {
            val currentUser = appContainer.userRepository.currentUser()
            EditProfileScreen(
                initialName = currentUser.name,
                onSave = { newName ->
                    appContainer.userRepository.updateName(currentUser.id, newName)
                    navController.popBackStack()
                },
            )
        }
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Profile, tap "Edit Profile". Change the name, tap "Save" — returns to Profile and the new name is displayed at the top.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/data/repository/UserRepository.kt app/src/main/java/com/valid/motouring/ui/profile/EditProfileScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add edit profile screen"
```

---

### Task 26: Settings screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `BuildConfig.VERSION_NAME` (auto-generated by AGP, Task 1), `Destinations.SETTINGS` (Task 8).
- Produces: `SettingsScreen()` — self-contained; toggle state is local `remember` state since there's no persistence layer to back it (mockup scope, per Global Constraints).

- [ ] **Step 1: Create `SettingsScreen.kt`**

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.BuildConfig

@Composable
fun SettingsScreen() {
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var useMetricUnits by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        SettingsToggleRow(
            label = "Push Notifications",
            checked = pushNotificationsEnabled,
            onCheckedChange = { pushNotificationsEnabled = it },
        )
        SettingsToggleRow(
            label = "Use Metric Units",
            checked = useMetricUnits,
            onCheckedChange = { useMetricUnits = it },
        )

        Text(
            text = "Motouring v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

- [ ] **Step 2: Modify `MotouringNavHost.kt` to add the Settings destination**

Add this import:

```kotlin
import com.valid.motouring.ui.profile.SettingsScreen
```

Add this `composable` block:

```kotlin
        composable(Destinations.SETTINGS) {
            SettingsScreen()
        }
```

- [ ] **Step 3: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Profile, tap "Settings". Toggle both switches — they visually flip. App version shows at the bottom (e.g. "Motouring v0.1.0-mockup").

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add settings screen"
```

---

### Task 27: Notifications screen

**Files:**
- Create: `app/src/main/java/com/valid/motouring/ui/profile/NotificationsViewModel.kt`
- Create: `app/src/main/java/com/valid/motouring/ui/profile/NotificationsScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt`

**Interfaces:**
- Consumes: `NotificationRepository` (Task 5), `Notification`/`NotificationType` (Task 3), `Destinations.NOTIFICATIONS` (Task 8).
- Produces: `NotificationsViewModel(notificationRepository)` with `val notifications: StateFlow<List<Notification>>`, `fun markRead(id: String)`. `NotificationsScreen(viewModel)`.

- [ ] **Step 1: Create `NotificationsViewModel.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.Notification
import com.valid.motouring.data.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow

class NotificationsViewModel(private val notificationRepository: NotificationRepository) : ViewModel() {
    val notifications: StateFlow<List<Notification>> = notificationRepository.observeNotifications()

    fun markRead(id: String) = notificationRepository.markRead(id)

    companion object {
        fun factory(notificationRepository: NotificationRepository) = viewModelFactory {
            initializer { NotificationsViewModel(notificationRepository) }
        }
    }
}
```

- [ ] **Step 2: Create `NotificationsScreen.kt`**

```kotlin
package com.valid.motouring.ui.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valid.motouring.data.model.Notification

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(notifications, key = { it.id }) { notification ->
            NotificationRow(notification = notification, onClick = { viewModel.markRead(notification.id) })
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        onClick = onClick,
    ) {
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

- [ ] **Step 3: Modify `MotouringNavHost.kt` to add the Notifications destination**

Add these imports:

```kotlin
import com.valid.motouring.ui.profile.NotificationsScreen
import com.valid.motouring.ui.profile.NotificationsViewModel
```

Add this `composable` block:

```kotlin
        composable(Destinations.NOTIFICATIONS) {
            val viewModel: NotificationsViewModel = viewModel(
                factory = NotificationsViewModel.factory(appContainer.notificationRepository),
            )
            NotificationsScreen(viewModel = viewModel)
        }
```

- [ ] **Step 4: Build, install, and manually verify**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.
Manual check: from Profile, tap "Notifications". See the 4 seeded notifications, unread ones in bolder text. Tap an unread one — it visually switches to the read (regular-weight) style.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/profile/NotificationsViewModel.kt app/src/main/java/com/valid/motouring/ui/profile/NotificationsScreen.kt app/src/main/java/com/valid/motouring/navigation/MotouringNavHost.kt
git commit -m "feat: add notifications screen"
```

---

## Phase L — Final Integration

### Task 28: Full regression pass, README, and end-to-end smoke test

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: every screen and route wired in Tasks 1-27. No new production interfaces — this task verifies the whole graph holds together and documents how to build/run it.

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all 18 tests pass (5 in `RideSimulatorTest`, 3 in `ChallengeRepositoryTest`, 2 in `PoiRepositoryTest`, 3 in `PostRepositoryTest`, 5 in `RideSessionViewModelTest`).

- [ ] **Step 2: Run a full clean build**

Run: `./gradlew clean :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Create `README.md`**

```markdown
# Motouring

A mockup Android app for a "ride together" social app for motorcycle and car riders — group rides with simulated voice calls, gas station/repair shop maps, gamified challenges and badges, and Strava/Yamaha-style ride tracking.

This is a **UI/UX mockup**: there is no backend. All data is seeded in-memory (see `FakeDataProvider`) and resets whenever the app process is killed. Ride tracking and voice calls are simulated locally via a coroutine ticker (`RideSimulator`) rather than real GPS/WebRTC.

See `docs/superpowers/specs/2026-07-09-motouring-android-mockup-design.md` for the full design spec and `docs/superpowers/plans/2026-07-09-motouring-android-mockup.md` for the implementation plan this was built from.

## Setup

1. Copy `local.properties.example` to `local.properties`.
2. Fill in `sdk.dir` (your Android SDK path).
3. Get a Mapbox account at https://account.mapbox.com and fill in:
   - `MAPBOX_DOWNLOADS_TOKEN` — a secret token with the `DOWNLOADS:READ` scope (needed to resolve the Mapbox Gradle dependency).
   - `MAPBOX_PUBLIC_TOKEN` — your default public token (needed at runtime to render the map).
4. Build and install: `./gradlew :app:installDebug`

## Tech stack

Kotlin, Jetpack Compose, Material 3, Navigation-Compose, Mapbox Maps SDK (`extension-compose`), MVVM with a manual `AppContainer` for DI (no Hilt). See the design spec for full rationale.
```

- [ ] **Step 4: Install and manually walk the entire app**

Run: `./gradlew :app:installDebug`
Expected: `BUILD SUCCESSFUL`.

Manual walkthrough (confirm each step, in order, on a fresh launch):

1. Splash (~1.2s) → Onboarding (swipe through all 3 pages) → tap "Get Started"
2. Login → type any username/password → tap "Log In"
3. Vehicle Garage Setup → fill make/model/year, pick Motorcycle or Car → tap "Continue"
4. Land on Home tab: "Start Group Ride" button, challenge progress card, feed of posts. Like a post — count updates.
5. Tap "New Post" → fill a caption, attach a ride → tap "Post" → new post appears at top of feed
6. Tap that new post → Post Detail → add a comment → it appears immediately
7. Nearby tab → map with colored POI dots + list below → filter by Motorcycle/Car/All
8. Challenges tab → tap a challenge → Challenge Detail with leaderboard → back → tap "See All" badges → Badges grid → tap a badge → Badge Detail
9. Rides tab → see seeded ride history cards
10. Profile tab → stats, garage, badges preview → tap "Ride Buddies" → Friends list, Accept/Add a buddy → back
11. Profile → "Notifications" → tap an unread one, it becomes read-styled → back
12. Profile → "Edit Profile" → change name → Save → name updates on Profile → back
13. Profile → "Settings" → toggle both switches, see app version → back
14. Home → "Start Group Ride" → Start Ride screen → toggle Group → "Invite Ride Buddies" → check a couple, "Done" → back on Start Ride → tap "Start Ride"
15. Ride Session: full-screen map, stats ticking up, speaking-highlight rotating on the participant bar → tap "End Ride"
16. Ride Summary: title, route image, stats, badges row → tap "Done" → back on Home
17. Rides tab → the ride you just completed now appears at the top of the history list
18. Press the system back button from each of the 5 bottom-nav tabs while on Home — confirm it exits the app (expected: Home/MAIN is the root of the back stack after onboarding, so back here is app-exit, same as any bottom-nav-rooted Android app)

If every step above works without a crash, the mockup is feature-complete per the design spec.

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add project README with setup instructions"
```

## Phase M — Compose Previews

### Task 29: Add `@Preview` functions for every stateless screen/component

**Files:**
- Modify: `app/src/main/java/com/valid/motouring/ui/components/StatBlock.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/BadgeChip.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/PostCard.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/RideBuddyAvatarRow.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/components/SectionHeader.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/SplashScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/onboarding/LoginScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/ChallengeDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/BadgesScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/challenges/BadgeDetailScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RidesHistoryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/StartRideScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/rides/RideSummaryScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/EditProfileScreen.kt`
- Modify: `app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt`

**Interfaces:**
- Consumes: every stateless composable from Tasks 9-27, `FakeDataProvider` (Task 4), `MotouringTheme` (Task 2).
- Produces: one `@Preview`-annotated `private fun ...Preview()` per file, each wrapping the real composable in `MotouringTheme { ... }` with representative fake data or no-op lambdas. `ViewModel`-coupled screens (Home, Nearby, Challenges, Create Post, Post Detail, Friends, Invite Ride, Ride Session, Profile, Notifications, Vehicle Garage Setup) are intentionally excluded per the spec's Testing section.

- [ ] **Step 1: Add a preview to `StatBlock.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StatBlockPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StatBlock(label = "Distance", value = "18.4 km")
    }
}
```

- [ ] **Step 2: Add a preview to `BadgeChip.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeChipPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeChip(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first(), onClick = {})
    }
}
```

- [ ] **Step 3: Add a preview to `PostCard.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        PostCard(post = com.valid.motouring.data.fake.FakeDataProvider.posts.first(), onLikeClick = {}, onCardClick = {})
    }
}
```

- [ ] **Step 4: Add a preview to `RideBuddyAvatarRow.kt`**

Append to the end of the file:

```kotlin

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

- [ ] **Step 5: Add a preview to `SectionHeader.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SectionHeader(title = "Feed", actionLabel = "New Post", onActionClick = {})
    }
}
```

- [ ] **Step 6: Add a preview to `SplashScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SplashScreen(onTimeout = {})
    }
}
```

- [ ] **Step 7: Add a preview to `OnboardingScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        OnboardingScreen(onFinished = {})
    }
}
```

- [ ] **Step 8: Add a preview to `LoginScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
```

- [ ] **Step 9: Add a preview to `ChallengeDetailScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ChallengeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        ChallengeDetailScreen(challenge = com.valid.motouring.data.fake.FakeDataProvider.challenges.first())
    }
}
```

- [ ] **Step 10: Add a preview to `BadgesScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgesScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgesScreen(badges = com.valid.motouring.data.fake.FakeDataProvider.badges, onBadgeClick = {})
    }
}
```

- [ ] **Step 11: Add a preview to `BadgeDetailScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BadgeDetailScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        BadgeDetailScreen(badge = com.valid.motouring.data.fake.FakeDataProvider.badges.first())
    }
}
```

- [ ] **Step 12: Add a preview to `RidesHistoryScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RidesHistoryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RidesHistoryScreen(history = com.valid.motouring.data.fake.FakeDataProvider.rideHistory)
    }
}
```

- [ ] **Step 13: Add a preview to `StartRideScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun StartRideScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        StartRideScreen(
            vehicles = com.valid.motouring.data.fake.FakeDataProvider.vehicles.filter { it.ownerId == "u-me" },
            onInviteBuddiesClick = {},
            onStartRide = { _, _ -> },
        )
    }
}
```

- [ ] **Step 14: Add a preview to `RideSummaryScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RideSummaryScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        RideSummaryScreen(
            entry = com.valid.motouring.data.fake.FakeDataProvider.rideHistory.first(),
            earnedBadges = com.valid.motouring.data.fake.FakeDataProvider.badges.filter { it.isEarned },
            onDone = {},
        )
    }
}
```

- [ ] **Step 15: Add a preview to `EditProfileScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        EditProfileScreen(initialName = "Rafi", onSave = {})
    }
}
```

- [ ] **Step 16: Add a preview to `SettingsScreen.kt`**

Append to the end of the file:

```kotlin

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    com.valid.motouring.ui.theme.MotouringTheme {
        SettingsScreen()
    }
}
```

- [ ] **Step 17: Verify everything still compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. Open a few of the modified files in Android Studio and confirm the preview pane renders each composable without a "Render Problem" error.

- [ ] **Step 18: Commit**

```bash
git add app/src/main/java/com/valid/motouring/ui/components/ app/src/main/java/com/valid/motouring/ui/onboarding/ app/src/main/java/com/valid/motouring/ui/challenges/ app/src/main/java/com/valid/motouring/ui/rides/ app/src/main/java/com/valid/motouring/ui/profile/EditProfileScreen.kt app/src/main/java/com/valid/motouring/ui/profile/SettingsScreen.kt
git commit -m "test: add Compose previews for stateless screens and components"
```
