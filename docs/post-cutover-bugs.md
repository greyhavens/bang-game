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

## BUG-4 — Claim/counter numbers render horizontally MIRRORED (backwards)  [FIXED — live confirmed 2026-06-18]
**RESOLVED:** the counter/claim score numbers now render upright and correctly oriented (no mirror) at
all camera azimuths — confirmed live by Tim. The remaining counter-number issue (the number being
occluded behind the claim rock mesh) is tracked separately as **BUG-22**.

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

## BUG-7 — Artillery explosion is massively oversized  [LIKELY FIXED — needs live confirm]
*Reported 2026-06-14.* When artillery fires, the explosion is far too large. **Defect capture
(CURRENT jME3 build, not a fork golden):** `docs/fidelity-reference/video/ArtilleryOversizeExplosion.mp4`
— compare to the correct fork explosion scale in `ArtilleryKillShotgunner.mp4`.

**Root cause (found 2026-06-18, shared with BUG-12).** The fork integrates particle position in
**milliseconds** (`Particle.updateAndCheck`: `position += velocity * secondsPassed * 1000f`); jME3
integrates in seconds. The cutover `ParticleConverter` baked velocity/gravity WITHOUT that ×1000
factor, so for a world-space (non-following) emitter the per-particle motion was ~1000× too small —
every burst collapsed into a near-motionless clump at the origin (one giant overlapping fireball
reading as a single oversized blob, with the additive quad edges piling up). **FIXED:**
`ParticleConverter` now bakes world-space velocity/gravity as `forkValue × 1000 × effectScale`
(`MS_PER_SEC` constant). Offscreen `ft_fx_explosion` now renders a proper fireball core with debris
flying outward (golden re-captured, was a clumped blob). Per-particle **size** mapping was already
correct (size renders at the node scale and isn't ms-integrated). Needs a live artillery shot to
confirm the on-board scale matches `ArtilleryKillShotgunner.mp4`.

## BUG-12 — Bonus-pickup star-burst missing / renders as an opaque white box  [FIXED — golden + offscreen confirmed]
*Reported earlier; re-diagnosed 2026-06-18.* Picking up a board bonus should play
`BonusEffect.ACTIVATED_BONUS = "frontier_town/get_bonus"` — a burst of ~15 clean transparent additive
yellow stars radiating around the unit (fork reference: `baseline/correct/PickupClock/*`). In the
cutover it showed no visible burst in-game, and the offscreen golden showed a single star on an opaque
white box.

**Root cause.** SAME ms-vs-seconds velocity-integration bug as BUG-7. The 66 star particles all spawn
at one point and should fly outward (fork `vel=0.56`, `maxAngle≈98°`), but the baked velocity was
~1000× too small, so all 66 stacked motionless at the origin → one star + a square of overlapping
transparent quad borders. (The earlier "palette+tRNS → RGBA8" texture fix was real and correct, but it
was NOT the cause of the missing burst; the texture decodes fine — 132 transparent + 124 opaque texels.
The "white box" was additionally a harness-only artifact: additive quads wrote alpha=0 into the
captured framebuffer, which a PNG viewer shows as white. `OffscreenRenderApp` now forces the captured
PNG opaque, since the live game renders to an opaque backbuffer.)

**Fix.** (1) `ParticleConverter`: bake world-space velocity/gravity with the `×1000` ms factor (the
shared BUG-7 fix). (2) `OffscreenRenderApp`: write captured frames opaque so particle goldens look like
the in-game result. Offscreen `ft_fx_get_bonus` now renders a clean radiating yellow star-burst
matching the fork PickupClock reference; golden re-captured (was the bad white-box golden);
`verifyVisuals` all 23 PASS bit-exact; static-model goldens unchanged (≤0.000002), `verifyCorpus`
310/310.

## Particle effects to REVISIT (visual fidelity, lower priority)
*Noted 2026-06-18 while fixing BUG-7/BUG-12.* The ms-integration fix made every world-space burst move
correctly, but a few effects still don't perfectly match the fork and warrant a future eyeball pass
(use the `/fx` sandbox + `renderParticleToPng`, compare against the fork live or a fork-baked render):

- **`indian_post/rain`** — renders as a single thin spreading wedge/streak, not many dispersed
  raindrops. It now MOVES (was a static white square before the fix, so this is strictly an
  improvement and not a regression), but the shape is wrong. Likely tied to its emitter shape/`maxAngle`
  being very narrow and/or the dropped fork influences (`WanderInfluence`/`BasicDrag` have no jME3
  equivalent — see the `bakeParticles` "dropped influences" tally). Out of scope for BUG-12.
- **`indian_post/fireflies`** — plausible (scattered glowing dots) but sparse vs the fork; worth a
  side-by-side.
- **Additive sprite quad EDGES on a dark backdrop** — get_bonus stars / explosion sparks show a faint
  dark square halo around each sprite in the offscreen render (the additive quad's anti-aliased border
  over a near-black background). Over the textured in-game board this is far less visible, but if a
  pickup looks "boxy" in-game it's this; a fix would be premultiplied-alpha or a softer sprite border.
- **34 of 60 baked effects report DROPPED fork influences** (`BasicDrag` ×12, `BasicVortex` ×18,
  `SwarmInfluence` ×5, `WanderInfluence` ×17) — jME3 has no equivalent, so those effects' subtle
  swirl/drag/wander motion is absent. Cosmetic; revisit per-effect if any looks notably wrong live.

## BUG-2 — Esc immediately quits the game  [FIXED]
*Reported 2026-06-14.* Pressing **Esc** instantly aborts/exits the client. Not expected — the original
game did not quit on Esc.

**Cause.** jME3 `SimpleApplication.initialize()` auto-registers a default `ESCAPE → stop()` exit
mapping (`INPUT_MAPPING_EXIT`); the cutover host (`JmeApp`) suppressed fly-cam/stats/debug-keys but
not this. **FIXED:** `JmeApp.simpleInitApp` now deletes `INPUT_MAPPING_EXIT`, so Esc no longer aborts.
(Routing Esc to a game back/dismiss handler, if the fork did that, is a separate nicety — TODO if
wanted.)

## BUG-20 — Attack-target predicted-damage number is rotated 90° on its side  [FIXED — live confirmed 2026-06-19]
**FIXED:** `PieceTarget.PTARG_COORDS` (the `_ptquad` UV array) still used the fork's jME1 Quad winding `{(0,2),(0,0),(2,0),(2,2)}`, which on jME3's differently-wound Quad mapped U→local +Y → 90° rotation. Reordered to jME3 winding `{(0,0),(2,0),(2,2),(0,2)}` (matching the BUG-4 `RenderUtil.createTextTexture` fix). `_tgtquad`/`_modquad` use stock-UV icons (unaffected); BUG-17 centering untouched. Live: upright "2" over a targeted enemy.
*Reported 2026-06-18 (screenshot: `baseline/incorrect/attacktarget/SelectedForAttackNumberOffBy90Degrees.png`).*
When you select one of your units and target an enemy unit, the small predicted-damage number that
appears inside the attack reticle over the enemy is **rotated 90°** (lying on its side) instead of
upright/screen-facing.

**Likely cause.** Same migration-map family as **BUG-4** (counter numbers) — a text quad/billboard on
the **Z-up** board. The 90° rotation is the classic `BillboardNode → BillboardControl` Z-up/Y-up
alignment mismatch (default `Alignment.Screen` assumes Y-up). This number is rendered by the attack
target reticle path — `PieceTarget` (the `_modquad`/damage-readout) and/or the shared text-quad helper
`RenderUtil.createTextQuad`/`createTextTexture` — so check whether the readout quad is oriented for
Z-up like the BUG-4 counter fix did. **Fix area:** `client/shared/.../game/client/sprite/PieceTarget.java`
(damage-mod readout) + its billboard/text orientation; mirror whatever the BUG-4 counter-orientation
fix uses (`Alignment.Camera` / explicit up-vector / pre-rotated quad). **Always diff against the fork
`PieceTarget`** — BUG-17 already centered these quads, so this is orientation only, don't regress the
centering. Verify with the new offscreen effect harness (`renderEffectToPng`) if the readout can be
posed there, else live.

## BUG-21 — Frontier Town backdrop/sky renders OVER buildings (sky on top instead of behind)  [FIXED — live confirmed 2026-06-18]
**FIXED 2026-06-18 (two-part).** The real defect: the sky was drawn IN FRONT of the buildings — the
town building tops (and rocky-mesa backdrop) were sliced off at a hard horizontal line and replaced by
flat sky. TWO causes, both in `SkyNode`:
1. **Depth func (the main one).** The dome material used `setDepthFunc(TestFunction.Always)`. jME3 renders
   the **Sky bucket AFTER the Opaque bucket** (`RenderManager.renderViewPortQueues`: Opaque @ ~L1240, then
   Sky @ ~L1248), so an Always depth func made the dome ignore the depth buffer and **paint over the
   already-drawn buildings**. The fork used `CF_ALWAYS` too, but jME1 drew the sky FIRST (behind geometry);
   under jME3's reversed order the correct match is `LessOrEqual`, so closer opaque geometry occludes the
   sky and it only fills the empty background. **Changed dome depthFunc Always → LessOrEqual.**
2. **Face culling (necessary precondition).** Dome + cloud materials had no `FaceCullMode`; jME3 defaults
   to `Back`, which culled the inside-view dome away entirely. **Set `FaceCullMode.Off`** on both (matches
   the fork's no-CullState/both-faces dome). Without this the dome didn't draw at all.
Verified live on the town hub (`/tmp/townhub.png`): all buildings render complete with the rocky-mesa
backdrop and sky only above. Single consumer (`BoardView`), so in-game board sky is fixed too.

NOTE — a wide white band in an earlier capture (`/tmp/skycheck.png`) was a transient mid-load frame
(ground texture still streaming, rendered white), NOT a sky defect — don't chase it.

REMAINING (separate, pre-existing Phase-4 backdrop-fidelity gaps — NOT part of this fix): distance FOG is
disabled (`BoardView.refreshFog()` is a deliberate no-op; the fork hazed distant ground into the horizon
via a `FogState` — would need a jME3 `FogFilter`), and the SkyNode cloud layer shows no visible clouds.
Flag for a decision if the bare horizon / missing clouds matters.

### (historical note) the FaceCullMode change referenced above
**FIXED 2026-06-18.** Root cause: `SkyNode`'s `Dome` and cloud-plane materials set **no `FaceCullMode`**,
and jME3 `RenderState` defaults to `FaceCullMode.Back`; with the inside-view dome winding the only
camera-facing triangles were back-faces and got culled, so the viewport clear-colour (the board's
sky-horizon blue from `BoardView.refreshBackgroundColor`) showed through as a flat fill. The fork's
jME1 `Dome`/`Disk` carried no cull state (drew both faces). **Fix:** `SkyNode` now sets
`setFaceCullMode(FaceCullMode.Off)` on both the dome and cloud materials (matches the fork's
draw-both-faces behaviour, winding-independent). Verified on the town menu and in-game board; single
consumer (`BoardView`), low risk. Cloud edge-fade fidelity remains the separate Phase-4 javadoc item.

*Reported 2026-06-18 (screenshot: `baseline/incorrect/town/TopOfFrontierTownCutOff.png`).* On the
town menu screen, the buildings render along the bottom but the entire upper ~60% of the view is a
flat, uniform blue — the proper backdrop/sky (gradient + clouds + distant skyline) is missing/cut off.

**Likely cause.** The town menu is a 3D board (`TownView` → `TownBoardView extends BoardView`, loading
`menu/<town>/town.board`) and the sky is `client/shared/.../game/client/SkyNode.java` — a jME3 `Dome`
mesh with an `Unshaded` gradient material (rebuilt at cutover; its own javadoc flags "dome shading,
cloud edge fade" as a **Phase-4 fidelity review item**). The flat uniform blue is the viewport
clear-color showing **past the edge of the sky dome**: the prime suspect is the `Dome` being built/
oriented for **Y-up** and not reoriented/sized for the **Z-up** board, so it only covers part of the
upper hemisphere (same Z-up vs Y-up migration family as BUG-4/BUG-20). Secondary suspects: the dome
gradient/cloud texture not loading (so it paints flat), or the camera frustum far-plane clipping the
backdrop. **Fix area:** `SkyNode` (Dome orientation/radius for Z-up, gradient + cloud mesh), how
`BoardView` attaches/positions the sky, and the town camera framing. **Diff against the fork SkyNode**
and match its dome coverage/shading. Verify live on the town screen (and cross-check an in-game board,
since the same SkyNode renders there).

## BUG-22 — Claim/counter score number occluded behind the claim rock mesh (number invisible)  [OPEN]
*Reported 2026-06-18 (screenshot: `baseline/incorrect/ClaimJumpingNumber/ClaimJumpingNumberRendersBEHINDtexture.png`).*
Split off from BUG-4 (mirroring is now FIXED). The claim/counter prop renders, but its **count number
is swallowed by the rock texture** — drawn behind/inside the prop mesh so nothing is visible.

**Likely cause.** Same jME3 corner-anchored-`Quad` family as BUG-17/skull/IconViz. In `CounterSprite.createGeometry`
the number is `new Geometry("counter", new Quad(25, 25))` on a billboard at `(0, 0, (_config.height + 0.5f)*TILE_SIZE)`.
The fork's `new Quad("counter", 25, 25)` was **center-anchored**; jME3's `Quad` is **corner-anchored** (spans
(0,0)-(25,25)) and `CounterSprite` never re-centers it — so the quad hangs ~12.5 units **down-and-left** of the
billboard pivot, dropping the number down into the claim rock mesh. Depth state is faithful to the fork
(`applyOverlayZBuf` = depthTest on, depthWrite off, `LessOrEqual` — matches the fork `overlayZBuf`), so the
opaque rock (drawn first, in front of the sunk-down number) occludes it via the LEQUAL test. **Fix:** center
the counter quad on the billboard pivot (translate by `-12.5,-12.5`, like `IconViz.attachCentered`) so it floats
above the rock as in the fork; if any residual occlusion remains, also confirm the claim prop's baked mesh
height vs the fork (`_config.height`) and the overlay draw order. **Diff against fork `CounterSprite`.** Same
fix likely applies to `GenericCounterNode` (the totem/score counter) — audit it too.

## BUG-23 — Sky has no clouds (master has white/yellow-tinged clouds woven into the blue)  [OPEN]
*Reported 2026-06-18.* Target look: `baseline/correct/FrontierTown/FrontierTown.png` (and
`barbershopHighlighted.png`) — the master sky is blue with soft **white, yellow-tinged clouds** threaded
through it. Our render shows a plain blue gradient with **no visible clouds at all**. (Split out from the
BUG-21 "remaining" note now that we have the master reference.)

**Likely cause.** `SkyNode` builds a cloud-plane mesh (`createCloudMesh`) textured with
`textures/environ/clouds.png` (`Unshaded` + `VertexColor` + alpha blend, `FaceCullMode.Off`, at
`CLOUD_HEIGHT=500`), but no clouds appear. Suspects: the **clouds texture transfer/bake** is incomplete or
wrong (verify `assets/rsrc/textures/environ/clouds.png` content/alpha vs master — the user suspects an
incomplete copy); the cloud mesh UVs / per-vertex alpha / color tint don't reproduce the fork `Disk`
(the yellow tinge implies a cloud color/tint or gradient-blend the cutover dropped); cloud plane scale
(`CLOUD_TEXTURE_SCALE`/`CLOUD_RADIUS`) or height puts the clouds out of view; or alpha-blend makes them
invisible. **CHECK AGAINST MASTER FORK CODE** (`bang-game .../game/client/SkyNode.java` cloud `Disk`
setup: texture state, `ctex.setScale`, per-vertex color buffer, wrap) and the master `clouds.png`. Verify
live on the town hub against `FrontierTown.png`.

## BUG-24 — Ground/terrain texture too plain (master has grey gravel mixed into the sand)  [OPEN]
*Reported 2026-06-18.* Target: `baseline/correct/FrontierTown/FrontierTown.png` — the plaza ground in
front of the buildings has **grey gravel blended into the sand/tan**, a richer multi-texture terrain. Our
render shows flat, uniform sand — the gravel layer is missing, so the ground looks plainer/lower-fidelity.

**Likely cause.** Terrain texture-splat transfer incomplete. The board terrain is rendered via
`TerrainNode` (multi-texture/alpha-splat over the heightfield). The deploy log shows
`terrain.alpha.N has linear color space ... AlphaMap` warnings, hinting the splat alpha maps / ground
texture set aren't fully/correctly ported. Suspects: a ground texture layer (the gravel) isn't being
applied/blended; the terrain splat alpha maps or the per-tile ground-texture set didn't transfer; or the
terrain material only samples the base sand layer. **CHECK AGAINST MASTER FORK CODE** — compare
`TerrainNode` texture/splat setup and the town board's ground texture set vs `bang-game`. Verify against
`FrontierTown.png`.

## BUG-25 — Building hover-highlight renders the WHOLE building as a solid white block  [OPEN]
*Reported 2026-06-18 (defect `baseline/incorrect/town/FrontierTownBadBuilding.png`; correct
`baseline/correct/FrontierTown/barbershopHighlighted.png`).* When you mouse-over a town building (a shop
you can enter), it should get a **subtle translucent white brightening** (all wood/window/sign detail still
visible), clearing when the mouse leaves. Instead the ENTIRE building renders as a **fully opaque white
silhouette** — all detail blown out.

**Likely cause.** Badly-ported hover-highlight material/effect. The town-building hover highlight lives in
`client/shared/.../client/TownView.java` (building sprites / mouse-over). The fork applied a light
translucent highlight (e.g. a low-alpha additive/emissive overlay or a `MaterialState`/color tint with
small alpha); the cutover's port likely applies it at **full opacity / no alpha blend** (or as a flat
white unshaded replace), so the whole building goes solid white. **CHECK AGAINST MASTER FORK CODE** — find
how the fork highlighted a hovered building (`TownView`/building sprite: the highlight color, alpha, and
blend/material state) and reproduce that subtle blend in jME3; ensure it clears on mouse-exit. Verify live
(hover a building) against `barbershopHighlighted.png`.
