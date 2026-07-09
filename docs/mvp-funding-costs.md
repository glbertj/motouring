# Riding App — MVP Cost Breakdown (excl. wages)

Infra assumption: self-hosted wherever possible (own servers). Google Maps paid tier is acceptable — not being minimized.

## One-time costs

| Item | Cost | Notes |
|---|---|---|
| Google Play Developer account | $25 | one-time, per developer account |
| Apple Developer Program | $99/year | required only if shipping iOS; skip for Android-only MVP |
| Domain name | ~$10–15/year | |
| SSL certificate | $0 | Let's Encrypt, self-managed on own server |

## Recurring costs

| Item | Cost driver | Self-hosted approach |
|---|---|---|
| Server hardware / colocation / power / bandwidth | fixed, own infra | own servers — no cloud hosting bill, but factor in electricity + internet uplink cost |
| Database | $0 (software) | self-hosted Postgres/MySQL on own server |
| Maps API (Google Maps) | pay-as-you-go, scales with map loads / routing / geocoding calls | using paid tier directly — biggest variable line item |
| Push notifications (APNs / FCM) | $0 | free from Apple/Google, just needs certs/keys set up |
| Real-time / chat backend | $0 (software) | self-hosted (e.g. Centrifugo, NATS, or plain WebSocket server) |
| Voice calls (Discord-style, audio only) | bandwidth only, minor | self-hosted WebRTC audio SFU (mediasoup) + coturn for TURN relay — audio-only is ~64-96kbps/user, negligible at this scale |
| Analytics | $0 (software) | self-hosted (Plausible / Umami / Matomo) instead of Mixpanel/Amplitude |
| Crash reporting | $0 (software) | self-hosted GlitchTip (Sentry-API-compatible, much lighter than full self-hosted Sentry, which needs Kafka+ClickHouse and ~16GB RAM minimum on its own) |
| Email (transactional/OTP) | low, or $0 | self-hosted mail server is possible but deliverability is unreliable; a low-volume paid relay may still be worth it for signup/OTP emails only |
| Claude Code / AI coding subscription | monthly | active during build phase |

## VM sizing — super MVP (1,000 users, tens concurrent)

At this scale everything runs on **one VM — 2 vCPU / 4GB RAM / 50GB SSD**. No need to split services onto separate machines; nothing here is heavy enough to require isolation.

Runs on it:
- Backend API
- PostgreSQL
- Redis
- Centrifugo/WebSocket server (real-time tracking + invites)
- mediasoup + coturn (voice calls)
- Umami or Plausible (analytics)
- GlitchTip (error tracking)

Only thing to watch over time: SSD size, as ride/GPS history accumulates — cheap and easy to expand later, not a launch-day concern. If usage grows well past this scale (thousands of concurrent, larger voice rooms), revisit splitting voice/calls onto its own VM since bandwidth needs grow fastest there.

## Not included in bare-minimum MVP ask (phase 2)

- Liability insurance (relevant once "ride buddy" meetups between strangers go live)
- Content moderation tooling
- Customer support tooling
- Marketing / ASO assets
- Legal review (ToS, privacy policy, compliance) — recommend doing this before public launch even if deferred from initial budget

## Summary for the pitch

Two real line items to ask funding for up front: **Google Maps API usage** (variable, scales with users) and **Apple/Google developer accounts** (fixed, tiny). Everything else is either free software self-hosted on infra you already run, or a subscription (Claude Code) tied to build time. Frame the ask as: small fixed cost to launch + a Maps API budget that scales with adoption.
