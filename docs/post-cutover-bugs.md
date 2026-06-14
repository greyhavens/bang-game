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

## BUG-4 — Claim/counter numbers render horizontally MIRRORED (backwards)  [OPEN]
*Reported 2026-06-14 (screenshot: a "2" shows reversed/≈"S"; a "3" looked ok at some angles).* The
6b fix corrected the 90° **rotation** (text is upright) but a **left-right mirror** remains, and it's
**camera-angle dependent**. NOTE: the offscreen `createTextTexture` replica renders fine — so the
mirror is in the **live billboard**, not the texture generation.

**Likely cause.** `GenericCounterNode.java:54` / `CounterSprite.java:107` add a default-alignment
`BillboardControl` (`Alignment.Screen`) on the **Z-up** board. Screen alignment assumes **Y-up**, so
on the Z-up scene the billboard's computed right-axis can invert depending on camera **azimuth** →
mirrored glyphs (angle-dependent, matching "2 backwards / 3 ok"). This is the left-right half of the
migration-map `BillboardNode → BillboardControl` alignment risk the 6b notes flagged. **Fix:** orient
the billboard for Z-up (try `Alignment.Camera` and/or set the billboard up-vector / pre-orient the
quad) so text reads correctly from any azimuth; audit `PieceTarget`/`OneArmedBanditSprite`/`IconViz`
family for the same. Cross-ref `docs/fidelity-reference/claimNumbers.png`.

**Second facet — glyph clipped at the bottom** *(2026-06-14, ref `baseline/jme3-phase8/bug4-counter-3-bottom-clipped.png`)*: even when correctly oriented (a "3" at a good azimuth reads upright, confirming the mirror is azimuth-dependent), the digit's **bottom is cut off**. Cause is in the text-texture sizing — `RenderUtil.createTextTexture` / the quad height (`qrat = TILE_SIZE*0.8f / tcoords[2].y`) isn't capturing the full glyph bounds (font ascent+**descent**/baseline), so the bottom is cropped. Same `createTextTexture` sizing issue family as the `DamageIconViz` Phase-4 per-glyph-sizing TODO. Fix: size the text texture/tcoords to the full glyph metrics so nothing clips.

## BUG-5 — WASD camera pan is far too slow (a few pixels)  [OPEN]
*Reported 2026-06-14.* WASD panning works (6b wired `GodViewHandler`) but moves only a few pixels per
press — pan speed is way too low.

**Likely cause.** `GodViewHandler`'s analog pan applies the per-frame analog `value` (tpf-weighted,
~0.016) with too small a movement constant, so each step is tiny. **Fix:** scale up the pan speed
(the FORWARD/BACKWARD/LEFT/RIGHT pan magnitude per analog unit) in `app/.../camera/GodViewHandler.java`
to match the fork's pan rate. (Likely a small constant tweak.)

## BUG-6 — Scroll wheel does not rotate the camera  [OPEN]
*Reported 2026-06-14 — confirms the live risk the final-verification agent flagged.* The rotate
handler exists (`GameInputHandler._swingListener.mouseWheeled → swingCamera(±90°)`), but the
mouse-wheel event never reaches it.

**Likely cause.** jME3→BUI wheel-event **delivery**: `Jme3RootNode`'s `RawInputListener`
(`onMouseMotionEvent`'s `getDeltaWheel()`) isn't forwarding wheel deltas into the BUI event path, so
`_swingListener` never fires. **Fix area:** `bui/.../Jme3RootNode.java` wheel forwarding +
`GameInputHandler` swing-listener wiring. Cross-ref `docs/fidelity-reference/video/ScrollwheelRotatesCamera.mp4`.
(Camera cluster with BUG-3 + BUG-5 — likely one fix agent.)

## BUG-7 — Artillery explosion is massively oversized  [OPEN]
*Reported 2026-06-14.* When artillery fires, the explosion is far too large. **Defect capture
(CURRENT jME3 build, not a fork golden):** `docs/fidelity-reference/video/ArtilleryOversizeExplosion.mp4`
— compare to the correct fork explosion scale in `ArtilleryKillShotgunner.mp4`.

**Likely cause.** Particle **size/scale** in the explosion bake — separate from the emission-mode fix.
The `RepeatType` fix corrected one-shot-vs-continuous (no runaway accumulation), but the explosion's
per-particle **size** (`ParticleConverter` `setStartSize`/`setEndSize`) and/or the effect node **scale**
(`particles.properties` `scale`, or a fork→jME3 size-unit mismatch) is too big. **Fix area:**
`ParticleConverter` size mapping + `ParticleCache`/`particles.properties` scale for the `explosion`
effect; validate the rendered size against the `ArtilleryKillShotgunner` golden.

## BUG-2 — Esc immediately quits the game  [FIXED]
*Reported 2026-06-14.* Pressing **Esc** instantly aborts/exits the client. Not expected — the original
game did not quit on Esc.

**Cause.** jME3 `SimpleApplication.initialize()` auto-registers a default `ESCAPE → stop()` exit
mapping (`INPUT_MAPPING_EXIT`); the cutover host (`JmeApp`) suppressed fly-cam/stats/debug-keys but
not this. **FIXED:** `JmeApp.simpleInitApp` now deletes `INPUT_MAPPING_EXIT`, so Esc no longer aborts.
(Routing Esc to a game back/dismiss handler, if the fork did that, is a separate nicety — TODO if
wanted.)
