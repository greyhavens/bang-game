# Isolated effect-test harness

Status: design + working prototype (2026-06-18). Prototype:
`tools/j3o-converter/.../RenderEffectToPng.java` + gradle task `renderEffectToPng`.

## Problem

Iterating on Bang's floating visual effects (damage numbers, kill skull, repair/heal icon,
influence/power-up indicator, board-bonus star-burst, wreck debris, explosions) currently means a
slow live loop: kill stale JVMs → `./gradlew deploy` → boot `BangServer` → boot `BangDesktop`
`-test -autoplay` → wait for "Starting In Play Phase" → drive `/tmp/banginput.py` → grab `bin/shot`
→ eyeball. Worse, the scenario is **random per launch** (cj/gr/cr/lg), so reproducing the exact
situation that shows a bug can take several relaunches, and the client idles out (BUG-18) mid-session.

We want a fast, repeatable, ideally automated way to exercise one effect on one unit on demand,
measure it, and compare it against the known-good fork at `/home/tim/Repos/bang-game`.

## Recommended approach

**Primary: A — an offscreen effect-render harness** that extends the existing
`tools/j3o-converter` offscreen render pipeline, backed by **C — a JUnit position-math test** for
the cheap geometric regressions. **B — a scripted in-game sandbox** is the realistic-fidelity
fallback for the handful of effects that genuinely need the live board/particle/RTT stack, but it is
the most expensive to build and run and should be deferred.

Rationale: the j3o-converter already boots jME3 fully headless on the isolated
jME3 + LWJGL3 + `:app` classpath (`OffscreenRenderApp`, `RenderCounterToPng`), already renders real
baked models through the runtime load/pose path (`RenderModelToPng` / `Models.loadModel`), already
captures correct sRGB PNGs, already orbits camera azimuths (`RenderCounterToPng`), and already has a
golden-diff gate (`SnapshotDiff` / `verifyVisuals` / `golden-manifest.txt`). The effect harness is a
small, additive extension of proven infrastructure — sub-second per render, deterministic, no server,
no client, no MySQL, no randomness — and it plugs straight into `bin/qa` and the goldens. It is also
the only approach that can settle **BUG-11** empirically (see below).

### Feasibility finding (the key research result)

Can the *real* effect-viz classes (`IconViz` / `DamageIconViz` / `EffectViz`) render headless?
Investigation says: **the geometry/placement can, but not by instantiating the full viz objects on
this classpath.**

- The viz classes need **no `BangObject`, no network, no `PlayerObject`** to build/pose — confirmed at
  every call site. But they DO need a live jME3 `AssetManager` + camera (unavoidable, fine here) AND a
  real `BangContext` + `BangBoardView` + `PieceSprite` graph: `DamageIconViz.displayDamageIconViz`
  calls `view.getPieceSprite(target)`, `IconViz.didInit` calls `IconConfig.createIcon(_ctx, ...)`, and
  `UnitSprite.createGeometry` hard-requires `view.getTerrainNode().createHighlight(...)`.
- Critically, **`:client:shared` (where the viz classes live) is deliberately OFF the harness's
  render classpath** (`renderToPngRuntime` = jME3 + LWJGL3 + `:app` only — see
  `tools/j3o-converter/build.gradle`). Adding `project(":client:shared")` to that config is *safe*
  (it pulls no LWJGL2/fork, so no `org.lwjgl` sealing clash) but standing up a stub
  `BangContext`/`BangBoardView`/`PieceSprite`/`TerrainNode` is heavy.
- **What the viz actually does visually is small and fully reproducible inline.** `IconViz` floats a
  `BillboardControl` `Node` at local Z = `piece.getHeight() * TILE_SIZE`, hangs a corner-anchored
  `Quad` of side `ICON_SIZE = TILE_SIZE/2` on it, and re-centers the quad by `(-w/2,-h/2)`
  (`IconViz.attachCentered`). `DamageIconViz` adds a text quad (`RenderUtil.createTextQuad`) and
  optional attack/defend icons at `DAMAGE_SIZE = TILE_SIZE*0.3`. The leaf builders
  (`IconConfig.createIcon`, `RenderUtil.createTextQuad`) take only a lightweight `BasicContext` and
  return plain jME3 `Geometry`.

So the harness reproduces the **exact placement logic and constants** (same `getHeight()*TILE_SIZE`
anchor, same `ICON_SIZE`, same corner-anchored-quad re-centering, same `BillboardControl`) with a
plain textured quad, staying on the isolated classpath. This is faithful for everything BUG-11 is
about (where the icon sits relative to the model). The next landable step (below) is to put
`:client:shared` on the render classpath and call the real `IconConfig.createIcon` /
`RenderUtil.createTextQuad` leaf builders behind a tiny `BasicContext` adapter, so the *pixels*
(font, outline, colorization) are byte-identical too — without ever needing the board/sprite graph.

## Alternatives and tradeoffs

| | A. Offscreen render harness | B. Scripted in-game sandbox | C. JUnit position-math test |
|---|---|---|---|
| Speed | ~1-2 s/render, headless | minutes (deploy+server+client) | ms, no GL |
| Determinism | full (fixed model/pose/azimuth) | needs a pinned board + scenario | full |
| Fidelity | geometry + pixels of the effect quad; not the full live scene/lighting/particles | highest — the real client | none (asserts transforms only) |
| Catches position bugs (BUG-11, reticle, skull centre) | **yes, and measures azimuth dependence** | yes, but eyeballed | yes (the math), not the render |
| Catches pixel/material bugs (BUG-12 blend, BUG-13/14 glow, BUG-15 spheremap) | partial (the quad's own blend/texture); RTT/particle-pool effects need more scaffolding | yes | no |
| Server/effect-application logic (damage→death→respawn) | no | yes | could, with offline `BangManager` (none exists yet) |
| Build cost | low (extends existing harness) | high (new debug command + fixed board/scenario) | low |
| Fork side-by-side | **yes — render fork model+icon at matched azimuths, overlay/diff** | possible but manual | n/a |

### B in detail (when we do need it)

The mechanics are all present and clean (no core game-loop change needed):
- `BangManager.deployEffect(int, Effect)` is **public** — the single lever. Damage = `DamageEffect`,
  heal = `RepairEffect`, kill = `DamageEffect(100)` / `RemovePieceEffect`, spawn = `AddPieceEffect`;
  respawn is automatic via `RespawnDelegate` when the scenario installs it.
- **No in-game debug/cheat command exists.** Cleanest hook: parse a `/`-prefixed admin-gated command
  in `BangManager.createSpeakHandler()` and dispatch to `deployEffect`/`addPiece` (no marshaller
  regen). Isolate behind an admin-token + debug-flag check.
- Deterministic board+scenario: build a `BangConfig` with `type=TUTORIAL` and an explicit
  `addRound(scenarioId, boardName, bdata)` — pinning `scenario` + `board` (or embedding board bytes
  in `bdata`) removes ALL selection randomness. A tiny tutorial XML (one `add_piece` + an idle `wait`)
  under `assets/rsrc/tutorials/` is the ready-made "fixed tiny board + one unit + no-op scenario".
- `tools/bot-client/BangBotClient` already drives a game headless as a wire client (no jME) and is the
  model for scripting B if we want it automated rather than interactive.

This is the path to exercise the *server-side* damage/death/respawn loop and the full live render. It
is strictly more work than A and should be built only once A + C stop being enough.

### B — BUILT (2026-06-18): the deterministic pickup/effect sandbox

The minimal interactive version of B now exists, gated so it can never touch normal gameplay. It is an
admin-gated `/fx` chat command parsed in `BangManager.createSpeakHandler()` (no marshaller/regen), and
it dispatches straight to the existing public `deployEffect`/`addPiece`/`Bonus.affect` levers.

**Double gate (both required):**
1. The SERVER JVM must be started with `-Dbang.debug.effects=true` (constant `DEBUG_EFFECTS`).
2. The chatting player must hold the **admin token** (`tokens.isAdmin()` — the same token `-autoplay`
   needs, so the dev `test` account already has it).

When both hold and a message starts with `/fx`, the command is consumed (NOT broadcast) and routed to
`BangManager.handleDebugEffectCommand`. Otherwise the message is ordinary chat — zero behaviour change.

**Commands** (typed into the in-game chat box during the IN_PLAY phase):
- `/fx bonus [type]` — spawn a board bonus of `type` (default `repair`) ON the caller's first live
  unit's tile, give it a real piece id + board presence, then ACTIVATE it through the normal
  `Bonus.affect(...)` → `deployEffect(...)` path. This runs the full pickup end-to-end, including the
  shared `frontier_town/get_bonus` star-burst — the deterministic lever that verifies **BUG-12**. (Use
  any bonus type id from `BonusConfig`, e.g. `repair`, `nugget`, etc.)
- `/fx effect <name>` — report a raw effect string (e.g. `/fx effect frontier_town/get_bonus`) on the
  caller's first unit, isolating ONE particle viz with no bonus side-effects.
- `/fx help` — usage.

**Auto-demo + camera-lock (the robust, no-chat path — RECOMMENDED).** Driving `/fx` over the in-game
chat box turned out brittle under `-autoplay` (the observer is not a player, and the BUI text field's
Enter did not always submit), and a transient ~0.5 s burst happening *off-camera* can't be screenshotted
at all. So the sandbox also ships a fully server-driven demo plus a client camera-lock, both behind the
same `-Dbang.debug.effects=true` flag:

- **Server auto-demo** (`BangManager.maybeStartEffectDemo`): once the round is in play, an `Interval`
  repeatedly picks the first live unit and forces it to consume a board bonus through the real
  `Bonus.affect → deployEffect` path — guaranteed, repeating pickups with zero AI/chat dependence.
  Tunables: `-Dbang.debug.interval=<ms>` (default 5000) and `-Dbang.debug.bonus=<type>` (default
  `frontier_town/repair`).
- **Client camera-lock** (`EffectHandler`, guarded by `-Dbang.debug.effects` on the CLIENT JVM): when a
  particle effect plays, the camera centres on the affected piece via `BangBoardView.centerCameraOnPiece`
  — which this change also *un-stubbed* (it was a Phase-2 no-op; now wired through the working
  `GameCameraHandler.panCameraAbs`). So every forced burst is framed on-screen.

**How to run it (the path used to confirm BUG-12 live):**
```sh
./bin/bangserver -Dbang.debug.effects=true -Dbang.debug.interval=4000
DISPLAY=:1 ./bin/bangclient -test -autoplay -Dmute=true -Dbang.debug.effects=true \
    -Dusername=test -Dpassword=yeehaw
# then stream screenshots and pick the mid-burst frame:
for f in $(seq 1 40); do DISPLAY=:1 ./bin/shot /tmp/stream_$f.png; done
```
The interactive `/fx` chat commands also still work IF the caller is an admin player whose chat submits
(e.g. a human-played, non-autoplay game).

**Capture-timing caveat:** `bin/shot` takes ~0.7 s/frame, slightly longer than the burst's lifetime, so
you stream many frames across several pickup cycles and keep the one that catches the burst peak (a
PIL "count bright-yellow pixels" pass finds it). A future nicety: a `-Dbang.debug.holdBurst` that
lengthens the emitter lifetime, or hooking the demo to write the frame itself.

**Still TODO (deferred, not needed for BUG-12):** the fully-deterministic *board* (a pinned `TUTORIAL`
`BangConfig` + tiny fixed tutorial board so even unit placement is fixed — the unit currently wanders
with autoplay), and an *automated* frame-grabber synced to the burst. The auto-demo already removes the
pickup randomness and frames the burst, which is what BUG-12 needed.

## How it plugs into bin/qa and the goldens

- **bin/qa**: the harness emits `<outPrefix>-azNNN.png` per azimuth — exactly the input
  `bin/qa/qa_locate.py` (template-match skull/reticle/starburst, report centre + offset),
  `qa_ocr.py` (read damage numbers), and `qa_diff.py` (similarity vs baseline) consume. No new QA
  code needed; the harness is just a faster, deterministic frame source than `bin/shot` on the live
  client. (Note `qa_locate`'s "nearest unit" centroid is approximate against a plain backdrop; for an
  exact verdict, diff/overlay against a matched fork capture rather than trusting the absolute `off=`.)
- **goldens**: the harness shares `OffscreenRenderApp`'s capture path, so a chosen
  `renderEffectToPng` invocation can be added to
  `tools/j3o-converter/src/test/resources/visual/golden-manifest.txt` and gated by `verifyVisuals` /
  refreshed by `captureGoldens`, locking the effect placement against regression in CI.

## Fork side-by-side comparison

Both repos have the identical asset/source layout. Render the cutover and the fork the same way and
overlay:
1. cutover: `./gradlew :tools:j3o-converter:renderEffectToPng -PmodelType=... -Pout=/tmp/cut`
2. fork: the fork model.dat bakes to the same `.j3o` via the same converter, OR capture the live fork
   client at matched azimuths into `baseline/fork-before/`; render the cutover icon over the fork
   model, or `qa_diff.py` the two PNGs at each azimuth. Matched azimuths + matched framing make the
   icon-vs-head offset directly comparable, which is precisely what settles BUG-11.

## Settling BUG-11 with this harness

BUG-11 (open, decision pending): the floating damage number sits ~10px down-right of the unit head and
the offset **varies with camera azimuth**. Prior agents proved the effect code is byte-identical to the
fork and a screen-aligned billboard alone cannot cause azimuth drift — so the cause is that the unit
**model's visual head is horizontally offset from the sprite origin** (the billboard anchor). The open
choice: **(a) re-anchor the billboard to the model's bounding-box top at runtime** vs **accept-as-faithful**.

The prototype settles this empirically, headless, in seconds. It renders the real `shotgunner` model
with the skull icon at 8 azimuths in two anchor modes:

- `-Panchor=origin` — the **live anchor** (sprite origin + `getHeight()*TILE_SIZE`).
- `-Panchor=top` — **option (a)** (the model's world bbox top-centre).

The harness already prints the smoking gun: the shotgunner model's **bbox center is
(-1.62, -0.53, 3.56)** while the icon anchors at XY origin `(0,0)`. That ~1.6-unit horizontal X gap
between the visual mesh and the sprite origin is exactly what projects to an azimuth-dependent screen
gap between head and origin-anchored icon under perspective — confirming the diagnosis with a number,
not an eyeball. Running both modes and feeding the PNGs to `qa_locate.py` (or overlaying against a
matched fork capture) quantifies whether `anchor=top` removes the azimuth swing (validating option (a))
while `anchor=origin` retains it (matching the fork's faithful-but-drifting behaviour). That is the
data needed to make the (a)-vs-accept call.

## Implementation plan (small, landable pieces)

1. **DONE — prototype harness.** `RenderEffectToPng` (new file) + `renderEffectToPng` gradle task.
   Real baked unit + inline-reproduced `IconViz` billboard, both anchor modes, 8 azimuths → PNGs.
   Stays on the isolated `renderToPngRuntime` classpath. Verified running on `shotgunner` + `killed.png`.
2. **Real leaf builders for pixel fidelity.** Add `project(":client:shared")` to a render
   configuration; replace the inline quad with `IconConfig.createIcon(ctx, path, ICON_SIZE, ICON_SIZE)`
   and, for damage numbers, `RenderUtil.createTextQuad(ctx, BangUI.DAMAGE_FONT, color, ocolor, "12")`,
   behind a minimal `BasicContext` adapter backed by the harness `assetManager` (no board/sprite graph).
   This makes the rendered effect byte-identical to the live client, not just geometrically faithful.
3. **BUG-11 verdict.** Run both anchor modes on a few representative units (shotgunner, a mounted
   big-shot, an artillery), measure icon-vs-head with `qa_locate.py` and a fork overlay, and record the
   azimuth-swing numbers in `docs/post-cutover-bugs.md`. Decide (a) vs accept on the data.
4. **Golden-lock.** Add one or two `renderEffectToPng` lines to `golden-manifest.txt` so effect
   placement is regression-gated by `verifyVisuals`.
5. **More effect types.** Extend the harness to drive `--effect repaired|influence|get_bonus|...`
   (the icon-texture arg already generalizes; star-burst/particle effects reuse `RenderParticleToPng`).
6. **JUnit position-math test (C).** A `client:shared` test that asserts the billboard local Z equals
   `getHeight()*TILE_SIZE` and the centered-quad translation equals `(-w/2,-h/2)` — a fast guard on the
   placement math that needs no GL, locking the reticle/skull/icon-centering fixes against regression.
7. **Defer B** (scripted in-game sandbox: admin `/` debug command + fixed tutorial board) until the
   server-side damage/respawn loop or full-scene fidelity is what we actually need to test.

## Prototype usage

```sh
# the live anchor (expect azimuth drift, the bug as observed):
./gradlew :tools:j3o-converter:renderEffectToPng \
    -PmodelType=units/frontier_town/shotgunner -Picon=textures/effects/killed.png \
    -Panchor=origin -Panim=standing -Ptime=0 -Pout=/tmp/bug11-origin

# BUG-11 option (a): anchor at the model bbox top (expect azimuth-stable):
./gradlew :tools:j3o-converter:renderEffectToPng \
    -PmodelType=units/frontier_town/shotgunner -Picon=textures/effects/killed.png \
    -Panchor=top -Pout=/tmp/bug11-top

# measure / compare with the existing QA toolkit:
python3 bin/qa/qa_locate.py /tmp/bug11-origin-az90.png --template skull
python3 bin/qa/qa_diff.py /tmp/bug11-origin-az90.png --baseline /tmp/bug11-top-az90.png
```

Emits `<outPrefix>-az{0,45,90,135,180,225,270,315}.png`. Requires the baked staging tree
(`./gradlew :assets:processResources` populates `assets/build/staging/rsrc`). Icon textures live under
`assets/build/staging/rsrc/textures/effects/` (`killed.png` = skull, `damaged.png`, `repaired.png`).
