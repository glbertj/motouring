# Motouring — Android Mockup Design

## Purpose

Motouring is a mockup Android app for a "ride together" social app for motorcycle and car riders — combining group riding, voice calls while riding, POI maps (gas stations, repair shops), gamification (badges, challenges), and solo ride tracking (à la Yamaha's official app), with a Strava/Reclub-style social feed.

This is a **UI/UX mockup only**: no real backend, no real GPS/audio/network access. All "live" behavior (ride tracking, voice calls) is simulated locally so it feels convincing in a demo. Built by Valid (Virtue Digital Indonesia).

## Scope

In scope:
- Full Android app (single module), all screens navigable, populated with realistic fake data
- Simulated ride tracking (animated map marker, incrementing stats) and simulated voice call UI
- Social feed with photo posts, likes, comments (mock, local only)
- Gamification: challenges (e.g. "ride 100km this week") with progress + leaderboard, badges
- POI map for gas stations/repair shops, filterable by vehicle type
- Vehicle garage (register motorcycle/car: type, make, model, year, photo)

Out of scope (explicitly not built):
- Real backend/API/database — no network calls at all
- Real authentication (login/signup accepts any input and proceeds)
- Real GPS tracking, real WebRTC voice/audio, real push notifications
- Data persistence across app restarts — everything resets to seed data on relaunch
- Payments, liability/insurance flows, content moderation, customer support tooling

## Tech Stack

- **Language/UI**: Kotlin, Jetpack Compose, Material 3
- **Package id**: `com.valid.motouring`
- **Theme**: single fixed dark, moto-inspired theme (charcoal + amber/red accent) — no light/dark toggle
- **Architecture**: MVVM — Compose screens → `ViewModel` (exposes `StateFlow`) → in-memory fake repositories → data models
- **Navigation**: Navigation-Compose, single-activity, bottom nav for main tabs + pushed screens for flows
- **DI**: lightweight manual `AppContainer` (no Hilt) — minimizes setup friction for a mockup-scale app
- **Maps**: Mapbox Maps SDK for Android + Mapbox Directions API for route polylines (same stack Strava uses). No billing/self-hosting required for mockup/dev use (free tier).
- **Images**: Coil, backed by local drawables / bundled placeholder images (no real upload/storage)
- **Simulation**: Kotlin coroutine tickers drive animated ride tracking and the voice-call "speaking now" indicator

## Data Persistence

In-memory only. `FakeDataProvider` objects seed all data (users, vehicles, ride buddies, ride history, challenges, badges, POIs, posts) on app start. Anything the user creates during a session (new post, new vehicle, joined challenge) lives only in memory and resets when the app is killed and relaunched.

## Simulation Approach

Ride tracking and voice calls have no real backend to drive them, so they're simulated to still feel "live":

- **Ride Session**: a coroutine ticker (~1s interval) advances a rider marker along a predefined Mapbox polyline and increments distance/speed/duration `StateFlow`s for the local user and each fake group participant.
- **Voice call bar**: the same ticker pattern cycles a "speaking now" highlight across fake participant avatars every few seconds.
- Ending a ride stops the ticker and hands the accumulated stats + route off to the Ride Summary screen (which may also earn/display a badge).

No loading or error states are needed anywhere — all data is local and instant, so screens render directly into their populated state.

## Screens & Navigation

**Pre-auth flow** (pushed, no bottom nav):
1. Splash
2. Onboarding carousel
3. Login/Signup (mock — any input succeeds)
4. Vehicle Garage Setup (add first motorcycle/car: type, make, model, year, photo)

**Main app** — bottom nav, 5 tabs:

5. **Home** — social feed (ride-buddy photo posts + activity updates), "Start Ride" CTA, active challenge preview
6. **Nearby** — Mapbox map of gas stations/repair shops, filterable by vehicle type (motorcycle/car)
7. **Challenges** — active challenges list
8. Challenge Detail (progress + leaderboard) — pushed from Challenges
9. Badges grid — pushed from Challenges (or a sub-tab within it)
10. Badge Detail (unlock criteria, progress) — pushed from Badges
11. **Rides** — ride history list (past completed rides with summaries)
12. **Profile** — stats, vehicle garage, badges summary
13. Edit Profile — pushed from Profile
14. Settings — pushed from Profile
15. Notifications — pushed from Profile (or bell icon in top bar)

**Pushed flows** (reached from tabs above, not tabs themselves):
16. Start Ride (solo/group toggle, pick vehicle, invite ride buddies if group)
17. Friends / Ride Buddies list
18. Invite to Group Ride flow
19. Ride Session (full-screen: animated map, live stats, group rider list, voice-call bar)
20. Ride Summary (post-ride recap: distance, route map, stats, badges earned)
21. Create Post (pick photo(s), caption, optionally attach a completed ride)
22. Post Detail (view/add comments, mock like toggle)

## Data Models (mock)

- `User` — id, name, avatar, vehicles[]
- `Vehicle` — id, type (MOTORCYCLE/CAR), make, model, year, photo
- `RideBuddy` — user + friend/relationship status
- `RideSession` — id, participants, vehicle type, route polyline, live distance/speed/duration, status
- `RideHistoryEntry` — completed ride summary (distance, duration, avg speed, route, photos)
- `Challenge` — id, title, description, goal metric (e.g. distance/rides), progress, deadline, leaderboard
- `Badge` — id, title, icon, description, unlock criteria, earned state/date
- `PointOfInterest` — id, name, type (gas station/repair shop), location, supported vehicle type, rating
- `Post` — id, author, photos[], caption, optional attached `RideHistoryEntry`, like count/state, comments[]
- `Comment` — id, author, text, timestamp
- `Notification` — id, type, message, timestamp, read state

## Testing

Given this is a UI mockup with no business logic beyond local simulation, testing is limited to:
- Compose UI previews for each screen (visual sanity check, no CI requirement)
- Manual smoke test of the full navigation graph (every screen reachable, back stack behaves correctly)
- No unit/instrumentation test suite required for this phase — revisit if this evolves into a production build
