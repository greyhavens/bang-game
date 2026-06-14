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

## BUG-3 — Game-start camera intro (sweep / 360 rotate / zoom-out) doesn't play  [OPEN]
*Reported 2026-06-14 — documented in `docs/fidelity-reference/video/StartGameAndZoomOut.mp4`.* At
game start the camera should do the scripted sweep/rotate + zoom-out to the board overview; it doesn't.

**Likely cause.** The scripted camera-path intro isn't triggered/wired post-cutover. The path
machinery all exists — `app/.../camera/{CameraHandler,SwingPath,SplinePath,PanPath,CameraPath}` and
`GameInputHandler.swingCamera` — and *manual* scroll-wheel rotate works (verified), but the **auto**
game-start camera move isn't invoked. **Fix area:** find where the fork kicked off the start-of-game
camera path (likely a `BangController`/`GameInputHandler` intro hook fired on board-ready / fight
start) and confirm it runs on the jME3 `CameraHandler`. Cross-ref `StartGameAndZoomOut.mp4`.

## BUG-2 — Esc immediately quits the game  [FIXED]
*Reported 2026-06-14.* Pressing **Esc** instantly aborts/exits the client. Not expected — the original
game did not quit on Esc.

**Cause.** jME3 `SimpleApplication.initialize()` auto-registers a default `ESCAPE → stop()` exit
mapping (`INPUT_MAPPING_EXIT`); the cutover host (`JmeApp`) suppressed fly-cam/stats/debug-keys but
not this. **FIXED:** `JmeApp.simpleInitApp` now deletes `INPUT_MAPPING_EXIT`, so Esc no longer aborts.
(Routing Esc to a game back/dismiss handler, if the fork did that, is a separate nicety — TODO if
wanted.)
