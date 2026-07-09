# Motouring — "Analog Dash" Design System — Design

## Purpose

The Motouring mockup (25 of 29 screens built per the original [Android mockup design](2026-07-09-motouring-android-mockup-design.md)) currently renders in stock Material 3 defaults — default `Card`, default spacing, default type scale, `LinearProgressIndicator`, no custom shapes, no motion. This spec defines a real, distinctive visual identity and motion language for the app, and applies it across every existing screen.

This is a **restyle, not a rebuild**: no new screens, no new data models, no new navigation. Same `AppContainer`/repository/ViewModel architecture. Every screen and shared component gets its color, type, shape, and motion treatment replaced; none of them change what they show or how they behave.

Niche rider-facing features (safety/emergency tooling, trip planning depth, gear tracking, group-ride culture mechanics, etc.) are an explicitly separate follow-on brainstorm/spec, built on top of this design system rather than inside it. The one exception: this session generated a strong feature seed (a Goal/Endless ride-mode concept) captured verbatim in [Appendix: Feature Seed](#appendix-feature-seed-parked) for that future spec — it is **not** designed or scheduled here.

## Direction: Analog Dash

Grounded in motorcycle/car instrument-cluster culture (a premium sport-bike TFT dash, not a generic dark-mode app): gunmetal surfaces, a single needle-red/orange accent, tabular mono numerals for anything measured, and a circular gauge ring as the one repeated signature motif standing in for progress bars everywhere. This keeps and sharpens the original spec's "charcoal + amber/red, single fixed dark theme" constraint — that constraint is unchanged; everything within it is redone.

Two other directions were explored and rejected: **Topographic Touring** (warm canvas/contour-line adventure-map aesthetic) and **Night Meet** (near-black asphalt + acid-neon track-day aesthetic). Both are legitimate but would mean abandoning the existing amber/charcoal commitment rather than executing it well.

## Design Tokens

**Color** (replaces `Color.kt`):

| Token | Hex | Use |
|---|---|---|
| `Charcoal950` | `#100E0C` | App background (deepest) |
| `Charcoal900` | `#15130F` | Screen background |
| `Charcoal800` | `#1A1714` | Surface (cards, list items) |
| `Charcoal700` | `#241E19` | Elevated surface (top of a subtle gradient, not a flat fill) |
| `Charcoal600` | `#2A2522` | Dividers, gauge track, tick marks, hairline borders |
| `Charcoal500` | `#3D3632` | Secondary borders on deeper/recessed elements |
| `AccentPrimary` | `#FF5A36` | The one accent: CTAs, gauge fill, likes, active states |
| `OffWhite` | `#F5F1EC` | Primary text |
| `Muted` | `#A89F97` | Secondary text |
| `MutedDim` | `#7A8087` | Tertiary/label text (eyebrow labels, units) |

No secondary/tertiary accent hues — one accent color, used consistently, is deliberate (see Restraint below). Vehicle-type distinction (motorcycle vs. car) stays a label/icon distinction, not a color distinction.

**Typography** (replaces `Type.kt`) — three faces, each with one job:

- **Space Grotesk** (bundled as a variable font asset) — all headlines, titles, and button labels. Geometric, technical, warm enough to not feel cold.
- **Inter** (bundled) — all body copy, captions, and UI labels. Neutral workhorse, never competes with the display face.
- **IBM Plex Mono** (bundled) — *only* for numeric stat readouts (distance, speed, duration, percentages, counts). Never used for prose. This is what makes numbers feel instrument-grade rather than decorative.

Extend `MotouringTypography` with the existing Material slots (headlineMedium/titleLarge/titleMedium/bodyLarge/bodyMedium/labelSmall) remapped to Space Grotesk/Inter per above, plus a new non-Material custom style object (e.g. `MotouringTypography.statValue`, `statLabel`) for the mono readouts, since Material's `Typography` has no natural slot for a third face.

**Shape**: corner radius scale — 10dp (chips, pills, small tags), 14dp (buttons, list items), 18dp (cards, sheets). Circular for avatars and the gauge ring container. No sharp corners anywhere — this matches the soft motion language (see below).

**Elevation**: no Material tonal-elevation overlays and no drop shadows on dark surfaces (both read muddy). Elevated surfaces are distinguished by a subtle diagonal gradient fill (`Charcoal700` → `Charcoal900`-ish) plus a 1dp `Charcoal600` hairline border, not by shadow.

## Signature Component: Instrument Ring

A circular progress ring (`InstrumentRing` composable) replaces every `LinearProgressIndicator`/percentage display in the app: challenge progress, badge unlock %, ride-summary stats, profile totals. Middle-restraint execution, not full skeuomorphic bezel:

- Thin ring track (`Charcoal600`) + progress arc (`AccentPrimary`), rounded line caps
- Four faint tick marks at N/E/S/W (`Charcoal500`), shown only at ≥48dp sizes — omitted on small inline instances
- A soft radial glow (`Charcoal700` at low alpha, fading to transparent) behind the ring at larger sizes
- Open center slot for a mono-numeral value, a percentage, or (on badges) an icon

This is the one signature element per screen — everything else (cards, list rows) stays deliberately quiet so the ring doesn't get diluted by competing decoration.

## Motion System

Default personality: **soft and comfy** — spring-based settles with a gentle overshoot, not sharp mechanical snaps. This was tested head-to-head against a faster/terser "mechanical snap" alternative and the soft direction won clearly.

- **Entrances** (list items, cards appearing): spring animation (medium-low stiffness, damping ratio ≈0.7), ~450–550ms perceived settle, staggered ~60–90ms between siblings (e.g. feed posts, challenge list rows).
- **Press feedback** (buttons, cards, list rows): scale to ~0.94–0.96 on press, spring back on release — no linear easing.
- **Instrument Ring fill**: animates via the same spring language (not a linear tween) whenever its value changes or first appears.
- **Screen transitions**: forward push = slide-in-from-right + fade; back = slide-out-to-right + fade; both spring-driven for a soft settle rather than a hard stop.
- **Bottom nav tab switch**: cross-fade content + a small scale bump on the newly-selected tab icon.

No parallax, no per-pixel scroll-linked effects, no motion on every single element — restraint matters as much as the ring does. Motion is spent on entrances, presses, the ring, and screen/tab transitions; nothing else moves.

## Iconography

The current placeholder vector drawables (`ic_avatar_placeholder`, `ic_vehicle_motorcycle_placeholder`, `ic_vehicle_car_placeholder`, `ic_route_preview_placeholder`, `ic_photo_placeholder`, `ic_badge_placeholder`) and the ad-hoc `Icons.Filled.*` Material glyphs sprinkled through screens (heart, chat bubble, etc.) are visually inconsistent with the new system — generic single-path Material stock glyphs next to a considered gauge/mono-numeral aesthetic reads as unfinished.

This phase redraws the full recurring icon set (~12–14 glyphs: 5 bottom-nav icons, like/comment/share, badge medallion, motorcycle/car vehicle glyphs, route preview, photo placeholder, notification bell, settings gear) as a consistent thin-stroke (2dp) geometric line-icon set with rounded caps/joins — matching Space Grotesk's geometric character — as bundled vector drawables, replacing both the crude placeholders and the default Material Icons calls.

## Rollout Scope

All ~25 currently-built screens and every shared component (`StatBlock`, `PostCard`, `RideBuddyAvatarRow`, `BadgeChip`, `SectionHeader`, bottom nav, top bars) are restyled in this phase: `ui/theme/*` rebuilt from scratch, shared components rebuilt against the new tokens (with `InstrumentRing` replacing progress bars wherever one appears), then every screen re-themed and given its entrance/press motion. Compose `@Preview`s are updated alongside each component/screen they cover.

The 3 screens still blocked on the Mapbox token (map-dependent portions of Nearby, Ride Session, Ride Summary) get the same token/type/motion treatment applied to their non-map chrome (top bars, stat readouts, list rows) now; actual map styling is out of scope until Mapbox is unblocked — that's a pre-existing, unrelated blocker, not something this phase changes.

## Out of Scope

- New screens, new features, new data models — that's the follow-on features brainstorm (see Appendix below for the one seed captured this session)
- Anything requiring the Mapbox token
- Backend/persistence/auth changes (none of that exists in this mockup and this spec doesn't add any)

## Testing

Same approach as the original spec: Compose `@Preview` functions updated for every screen/component that takes plain data (visual sanity check), manual smoke test of the full navigation graph to confirm nothing regressed behaviorally during the restyle. No new automated test requirements — this phase changes visuals/motion, not logic.

---

## Appendix: Feature Seed (parked)

Captured during this session, **not designed or scoped here** — seed material for the next features brainstorm:

**Ride Mode: Goal vs. Endless**
- A ride session starts with a goal (distance or destination).
- On reaching the goal, the rider is prompted: pick a new goal (with recommendations) or go Endless.
- Endless mode keeps live tracking running uninterrupted — no dead stop, no forced end-of-session.
- Picking a new goal after the first makes it a waypoint/stop; a single outing can accumulate multiple stops.
- Endless is the **default fallback**: if the rider drifts off-route away from the current goal, the app doesn't nag or fail the route — it silently falls into Endless mode instead.
