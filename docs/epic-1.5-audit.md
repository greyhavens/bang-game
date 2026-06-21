# Epic 1.5 — jME1→jME3 migration fidelity backlog

Output of the 2026-06-19 multi-agent audit (`wf_31aa4472-5c4`): 5 parallel read-only lenses
(render-state defaults · coordinate conventions · bake pipelines · no-op stubs/deferred markers ·
texture/material features) diffing the jME3 cutover (`/path/to/bang-JRECutover`) against the
known-good master fork (`/path/to/bang-game`). 38 raw findings → deduped/ranked below.

**Verdict:** the cutover is a *phased* port, not a lazy one — fidelity was deliberately deferred and the
holes are self-labeled (`// Flagged no-op for Phase 2/3`, `TODO(phase3-host)`, `// Phase-4 fidelity`).
But the deferrals are **broader than the bug list we'd built from live QA**: two HIGH-visibility features
render *nothing* today (in-UI 3D unit previews; all round/game-over/scenario/loading marquees) and were
never reported because QA hadn't hit those screens. Dominant gap-classes: **stub/no-op holes (14)**,
**texture/material (6)**, **bake-pipeline (9)**, **coordinate-convention (5)**, **render-state (3)**.

Status key: **OPEN** · **DONE** (fixed) · cross-refs to `docs/post-cutover-bugs.md` BUG-NN.
Do the work as Epic 1.5. Every item: diff against the master fork and match it (hard rule).

---

## TIER 1 — HIGH player-visibility (do first)

### E15-1 — In-UI 3D unit previews render nothing (`BGeomView` no-op)  [OPEN, NEW]
`bui/.../BGeomView.java:91` `renderComponent()` is empty ("nothing to draw inline here"); `update()`
still steps the spatial but it's never drawn. **Every** `BGeomView` consumer is blank: Ranch
`UnitView`/`UnitInspector`, pre-game Big Shot `SelectionView`, tutorial unit views. Fork did the
`unsetOrtho/draw(geom)/setOrtho` dance to draw a live rotating 3D model into the component rect.
**Fix:** install the per-component jME3 `ViewPort` path from the `BGeomView` javadoc (camera viewport =
component screen rect, attach `_geom`, render+depth-clear per frame; register/unregister on
wasAdded/wasRemoved via the existing `registerGeomView`/`unregisterGeomView` hooks on `BRootNode`).

### E15-2 — Round/Game-Over/scenario/loading marquees invisible (`_mroot` never attached)  [OPEN, NEW]
`client/shared/.../game/client/BoardView.java:1387` `addMarquee()` builds `_mroot` (a `BRootNode`) and
adds `_mwindow`, but `TODO(phase3-host)`: since `BRootNode` is no longer a `Spatial` it's never attached
to any viewport/host root, so it's never rendered. All marquees — "Round Over", "Game Over", the
scenario announcement (`_bangobj.marquee`), and the loading-percentage label — show nothing.
**Fix:** route the marquee windows through the host's live BUI root (the same `Jme3RootNode` the rest of
BUI uses) on a high z-order overlay layer, instead of the orphan `_mroot`.

### E15-3 — Claim/counter score number floats off its billboard (corner-anchored Quad)  [OPEN → BUG-22]
`CounterSprite.java:83,100` (and `GenericCounterNode.java:46,76`, currently dormant) rebuild the count as
a corner-anchored jME3 `Quad(w,h)` attached to the billboard with **no** `-w/2,-h/2` recenter (fork Quad
was center-anchored). The number hangs up-and-right of the pivot — which sinks it into the claim rock
(the occlusion the user saw). **Fix:** `setLocalTranslation(-w/2,-h/2,0)` after rebuilding the mesh, like
`IconViz.attachCentered`. Apply to both `CounterSprite` and `GenericCounterNode`.

### E15-4 — Attack-target count/reticle number rotated 90° (`PTARG_COORDS` winding)  [OPEN → BUG-20]
`PieceTarget.java:228,347` applies the fork's literal `PTARG_COORDS = {(0,2),(0,0),(2,0),(2,2)}` to a
jME3 `Quad` whose vertex winding differs from jME1's, so U maps to local +Y → the number renders
rotated/transposed 90°. This is the sibling of the BUG-4 fix in `RenderUtil.createTextTexture` (which WAS
reordered to jME3 winding); `PTARG_COORDS` is a separate hardcoded array that was missed. **Fix:** reorder
`PTARG_COORDS` to jME3 winding (BL,BR,TR,TL → e.g. `{(0,0),(2,0),(2,2),(0,2)}`), validate vs the sprite.
*Related:* `PieceStatus.rotateWithCamera` (`PieceStatus.java:117`) is also a no-op (texture transform
deferred) so status overlays don't stay camera-aligned — re-do via a Z-up billboard.

### E15-5 — Ground texture too plain / stretched: per-type tiling (`terrain.scale`) dropped  [OPEN → BUG-24]
`RenderUtil.getGroundTexture` (`:285`) dropped the fork's `tex.setScale(1/terrain.scale)` (jME3 Texture2D
has no transform) with a comment claiming the splat material applies tiling — **but it doesn't**:
`TerrainNode` tb0 is plain `x*step0` and `TerrainSplat.vert` passes UVs straight through, so every ground
texture tiles at scale=1 → stretched/low-frequency/plain, and the grey-gravel detail never reads.
**Fix:** bake `terrain.scale` into the ground UVs (multiply tb0 by the layer's scale) or add a per-layer
`texCoordScale` material param to `TerrainSplat.j3md/.vert` set from `TerrainConfig.scale`. *Related (also
BUG-24):* `shouldRenderSplats()` now gates only on medium-detail (confirm the live pref is medium);
`TerrainNode.refreshShaders` is a no-op (fixed-function splat only); board fog disabled (see E15-11).

---

## TIER 2 — MEDIUM player-visibility

### E15-6 — Sky clouds missing: cloud plane stuck in the Sky bucket  [OPEN → BUG-23]
`SkyNode.java:62,93` — the cloud `Geometry` inherits `Bucket.Sky` (from the node) and is alpha-blended
with overlay z-buffer (no depth write). jME3's Sky bucket expects opaque sky and doesn't order/composite
transparent alpha the way the Transparent bucket does, so the translucent cloud fan likely renders in the
wrong order or is effectively invisible — a strong candidate for "sky has no clouds." **Fix:** put the
cloud Geometry in `Bucket.Transparent` (leave only the gradient dome in Bucket.Sky); confirm `clouds.png`
alpha composites over the dome. (Verify live whether clouds appear at all; also check the clouds.png
transfer/tint for the yellow-tinge look from `FrontierTown.png`.)

### E15-7 — Iron-plate / hero spheremap shine replaced by flat tint (often a no-op)  [OPEN → BUG-15]
`IronPlateViz.java:77`, `HeroInfluenceViz.java` — fork pushed a sphere-map (env-map) additive overlay pass
on each model mesh (metallic sheen / hero player-colour). Cutover `setBrighten` just multiplies the
material `Color` by (1+amount); no spheremap, and on baked materials lacking a `Color` param it's a total
no-op (invisible). **Fix:** author a spheremap env-map MatDef applied as an additive overlay pass, ramped
by the existing control; at minimum guard the brighten stand-in works on Color-less materials.

### E15-8 — RepairViz heal glow halo never composited  [OPEN → BUG-13]
`RepairViz.java:116,117` — the `Glow` inner class still renders the unit to a texture (RTT seam works) but
the additive spinning screen-space pane composite is a Phase-4 no-op, so the halo never draws (only the
sparkle swirl remains). **Fix:** composite the captured texture as `BlendMode.AlphaAdditive` spinning
GUI/ortho quads sized to the unit's projected screen radius, depth-test off.

### E15-9 — Pooled streamer/sparkle emitters lost their additive glow  [OPEN → BUG-14]
`ParticlePool.java:64,65,197` — fork gave streamer & sparkles `addAlpha` (SRC_ALPHA/ONE, additive glow)
and dustRing/steamCloud `blendAlpha`. Cutover shares ONE alpha-blended `_dustmat` across all four, so
streamer/sparkles are now plain alpha (no emissive accumulation). Also dims `RepairViz` heal swirls.
**Fix:** give streamer + sparkle emitters their own `applyAddAlpha` material; keep dustRing/steamCloud on
alpha.

### E15-10 — SafeMarker drops per-facing rotation + emissive glow  [OPEN → BUG-16]
`SafeMarkerSprite.java:38,78` — fork built 8 states (on/off × 4 rotations) + an emissive overlay; cutover
builds only on/off (no rotation, no glow), deferred to Phase-4. **Fix:** 4 rotated UV/geometry variants
keyed on orientation + an additive emissive overlay for the "on" state. *(Note: BUG-16 was filed as "wreck
debris ignores facing" — this is the same "lost per-facing rotation" class; confirm whether wreck debris
is a separate site or the same.)*

### E15-11 — Board fog (`refreshFog`) is a no-op → no distance haze  [OPEN]
`BoardView.java:1124` `refreshFog()` empty; fog color only feeds the viewport clear color. Terrain splat
DOES self-fog (`TerrainSplat applyFog`), so it's *half-applied*: fogged ground but crisp un-fogged
units/props/sky edges — inconsistent on foggy boards. **Fix:** wire a jME3 `FogFilter` (jme3-effects) on
the main ViewPort from `_board.getFogDensity()/getFogColor()`, or give all board materials the same fog
path. *(Contributes to the bare-horizon look noted under BUG-24/BUG-21.)*

### E15-12 — `OneArmedBanditSprite` wildcard icon not re-centered  [OPEN → BUG-17 class]
`OneArmedBanditSprite.java:72` — corner-anchored icon translated only `(0, TILE_SIZE/4, 0)` with no
`-CARD_SIZE/2` shift → floats up-and-right. **Fix:** `setLocalTranslation(-CARD_SIZE/2, TILE_SIZE/4 -
CARD_SIZE/2, 0)`. *Same family:* `HeroInfluenceViz.java:100` floating level number not re-centered.

### E15-13 — PieceStatus icon background renders nothing  [OPEN, NEW]
`PieceStatus.java:127` — the returned `BBackground.render()` is empty (immediate-mode quad draw gone), so
iconic unit-status backgrounds are blank in the status UI. **Fix:** render the icon textures via the BUI
`Jme3RenderBackend` (BImage/quad in `renderComponent`).

### E15-14 — Custom hardware cursors never install (`BCursor.show` no-op)  [OPEN, NEW]
`bui/.../BCursor.java:64` empty — no GLFW/jME3 cursor install anywhere; all context cursors
(move/attack/UI-hover) fall back to the default OS arrow. **Fix:** implement against LWJGL3 GLFW
(`glfwCreateCursor`/`glfwSetCursor`) or jME3 `inputManager.setMouseCursor`.

### E15-15 — Camera not disabled during pre-round setup / board tour  [OPEN, NEW]
`BangBoardView.java:424` — fork ended `prepareForRound()` with `setEnabled(false)` to lock camera input
until units were placed; cutover replaced it with a comment, so the player can fight the scripted
board-tour/setup camera. **Fix:** restore the `setEnabled(false)` (the `GodViewHandler._enabled` flag
already gates input; the `setEnabled(true)` re-enable in `BangController` still exists).

### E15-16 — Particle gravity not rotated into Z-up → pulls sideways  [OPEN, NEW]
`ParticleConverter.java:355,388` — fork rotated the gravity vector by the emitter's y-up→z-up transform
each frame; cutover bakes gravity as-is, and jME3 applies it in unrotated world axes (it rotates spawn
pos/velocity but NOT gravity), so authored-downward gravity pulls along world +Y (horizontal) on the Z-up
board. **Fix:** pre-rotate baked gravity (and wind) by the `HALF_PI`-about-X quaternion for world-space
emitters in the converter.

### E15-17 — Dropped particle influences (Drag/Vortex/Swarm/Wander)  [OPEN]
`ParticleConverter.java:380` — only BasicGravity/BasicWind are converted; Drag (deceleration), Vortex
(swirl), Swarm (chase target), Wander (random walk) are tallied as dropped (~34/60 effects lose ≥1).
Affected motion is linear/omnidirectional instead of curved/decelerating. **Fix:** run `ParticlesToJ3o`
over staged rsrc for the exact per-effect tally, then author custom jME3 `ParticleInfluencer`s for at
least BasicDrag + the most visible Vortex/Swarm cases (snow_storm, mystical_ground, nightmare).
*Related no-op:* `BoardView.refreshWindInfluence/addWindInfluence` (`:1153,:549`) — particles ignore board
wind entirely (no drift).

---

## TIER 3 — LOW / latent (cosmetic, edge-case, or memory-only)

- **terrain-splats-off-at-low-detail** (`TerrainNode.java:1204`) — splats gated on medium-detail only;
  verify the live pref is medium (if so, benign; the real BUG-24 cause is E15-5).
- **imagecache-srgb-colorspace-tag** (`app/.../ImageCache.java:144`) — convertImage tags textures sRGB
  while gamma correction is on, applied inconsistently vs deliberately-Linear terrain/shadow/water tex;
  could shift blend math/hue. Decide one gamma policy and verify vs goldens.
- **model-texture-dual-load-path** (`ModelCache.java:231`/`TextureCache.java:72`, → BUG-19) — colorized vs
  un-colorized model textures load via different pipelines (AssetManager vs ImageCache) with possible
  V-flip/POT/colorspace differences. Verify the zation path matches the bake goldens.
- **applyblendalpha-docstring-no-bucket-trap** (`RenderUtil.java:130,140`) — `applyBlendAlpha` javadoc
  claims it moves geometry to the transparent queue but only sets blend mode; callers must set the bucket
  themselves. Latent foot-gun (root-cause class for any missed-bucket alpha). Fix docstring or add a
  Geometry-taking overload.
- **holdhandler-bonus-fade-blend-no-bucket** (`HoldHandler.java:127`/`EffectHandler.java:723`) —
  nugget/bonus fade is alpha-blended in the default (opaque) queue; matches fork's lack of explicit queue,
  at most a latent edge artifact.
- **enable-texture-compression-noop** (`RenderUtil.java:481`) — terrain S3TC/DXT compression dropped;
  VRAM/bandwidth only, no visual delta.
- **bangui-configicons-noop** (`BangUI.java:249`) — window/taskbar icon never set (default GLFW icon).
- **Bake approximations** (`ParticleConverter`): minimumAngle ignored (hollow rings become solid),
  velocityVariation maps the fork cone to an omnidirectional box (can reverse particles),
  controlFlow release rate clamped to count/mean-life (can thin dense continuous emitters), forced
  `setRandomAngle(true)` + jME3's per-particle spin jitter (fork used uniform start/rate),
  ImagesX/Y hardcoded 1×1 (latent — no multi-frame sprites today), authored `bounds` not applied to
  emitters (possible early-cull/pop of far world-space bursts). All low-visibility / partly unavoidable
  with the stock emitter; track as fidelity polish.

---

## Notes
- The synthesis stage of the workflow over-compressed (emitted 2/38 items); this doc is reconstructed
  from the 5 audit agents' raw structured findings (recovered from the run transcript).
- Known bugs already tracked in `docs/post-cutover-bugs.md`: BUG-13 (E15-8), BUG-14 (E15-9), BUG-15
  (E15-7), BUG-16 (E15-10), BUG-20 (E15-4), BUG-22 (E15-3), BUG-23 (E15-6), BUG-24 (E15-5). NEW from this
  audit: E15-1 (BGeomView), E15-2 (marquees), E15-13 (status icons), E15-14 (cursors), E15-15 (pre-round
  camera), E15-16 (gravity z-up), plus the Tier-3 latent items.
