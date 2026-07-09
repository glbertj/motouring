# Motouring dev workflow

This repo is edited from two places that stay in sync via git push/pull (no shared filesystem):

- **This VM** (headless, terminal-only) — Claude Code does all the actual coding here.
- **User's Arch host** — runs Android Studio (GUI) for visual review, layout preview, emulator/device testing, and any manual edits.

## Sync rule

- Claude Code (me): commit and `git push origin main` after every logical unit of work (a completed feature slice, a passing test/build, before handing back to the user) — not after every tiny edit, but don't let uncommitted work pile up across a whole session either.
- Before starting new work in a session, `git pull origin main` first in case the user made edits in Android Studio on the host.
- If the user mentions they edited something in Android Studio, `git pull` before touching related files, to avoid clobbering or conflicting with their changes.
