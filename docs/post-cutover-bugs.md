# Post-cutover regression bugs (Epic 1 hardening)

Regressions found live-testing the jME3/LWJGL3 build (`JRECutover`). Tim reports while playing; we
triage each, then delegate fixes to focused agents. Compare against the fork goldens in
`docs/fidelity-reference/` and the `docs/game-reference/` data.

Status key: **OPEN** · **IN PROGRESS** · **FIXED** (commit) · **WONTFIX/EXPECTED**

---

## BUG-1 — Window has no maximize button; can't resize or go fullscreen  [OPEN]
*Reported 2026-06-14.* The client window can't be maximized, resized, or taken fullscreen.

**Likely cause.** The jME3/LWJGL3 (GLFW) window isn't created resizable, and the fork's display-mode
handling — fullscreen toggle + resolution picker in `OptionsView`, and `BangPrefs.configureDisplayMode`
— was deferred at the Phase-3 host flip and never wired to the LWJGL3 context (see the explicit
`TODO(phase3-host)` in `BangPrefs.java` about validating modes against GLFW monitor modes / forcing
fullscreen). **Fix area:** `BangDesktop`/`BangApp` `AppSettings` (resizable + window hints) and
`OptionsView` fullscreen/resolution → an LWJGL3 context mode switch/restart.

## BUG-2 — Esc immediately quits the game  [FIXED]
*Reported 2026-06-14.* Pressing **Esc** instantly aborts/exits the client. Not expected — the original
game did not quit on Esc.

**Cause.** jME3 `SimpleApplication.initialize()` auto-registers a default `ESCAPE → stop()` exit
mapping (`INPUT_MAPPING_EXIT`); the cutover host (`JmeApp`) suppressed fly-cam/stats/debug-keys but
not this. **FIXED:** `JmeApp.simpleInitApp` now deletes `INPUT_MAPPING_EXIT`, so Esc no longer aborts.
(Routing Esc to a game back/dismiss handler, if the fork did that, is a separate nicety — TODO if
wanted.)
