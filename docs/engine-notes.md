# Engine & asset-pipeline notes

Hard-won knowledge about how Bang! Howdy's rendering, map, and asset systems actually work,
collected during the 2026 Java 21/Gradle 8 modernization and the Epic-1 jME3/LWJGL3 cutover.
Things here are non-obvious from reading any single file; each section notes the code that proves
it. (Historical fork/gdx/LWJGL2 details — the pre-cutover world — live in git history and the
Epic-1 plan `docs/jme3-cutover-plan.md`.)

## The rendering stack (post-cutover)

```
jME3 3.9 SimpleApplication (JmeApp/BangApp)  — window, input, GL context, main loop  [LWJGL3]
  └─ app/ (com.threerings.jme.*)             — scenegraph/sprite/model/camera framework
       └─ BUI (bui/)                         — in-game UI, rendered inside the jME3 scene
```

The host is a stock **`com.jme3.app.SimpleApplication`** on the **LWJGL3** backend (`jme3-lwjgl3`):
`JmeApp` (which `BangApp`/`EditorApp` extend) owns the window/context/loop. There is **no libGDX and
no LWJGL2** — the old "gdx owns the context, the jME fork renders into it" three-layer coupling is
gone. BUI renders through `bui/.../backend/Jme3RenderBackend` (+ `Jme3ImageBacking`); its input
arrives via the jME3 `InputManager` through `Jme3RootNode` (a `RawInputListener`), and keycodes are
jME3 `KeyInput.KEY_*` end-to-end (the transitional gdx keycode source was removed in Phase 8).

Sound runs on jME3/LWJGL3 audio (LWJGL3 OpenAL via `jme3-lwjgl3`); the gdx-owned-OpenAL-device
coupling is gone. Audio plays by default; `-Dmute=true` silences it for a dev session (`BangPrefs.
SILENT` → `getMusicVolume()`/`getEffectsVolume()` return 0, non-persisting).

## Threading rules

- In the game client, GL lives on the jME3 render thread (`jME3 Main`); Narya's client run queue is
  pumped from the app loop. BUI scene traversal/render happens on that render thread.
- The **editor** embeds the jME3 view in a Swing `JFrame` via a **jME3 AWT canvas**
  (`JmeContext.Type.Canvas` → `app.createCanvas()`/`startCanvas()`, bridged by
  `org.lwjglx:lwjgl3-awt`'s `AWTGLCanvas`). So two threads coexist: Swing chrome on the **AWT EDT**,
  the jME3 render loop + BUI root on the **render thread**. `EditorDesktop` builds the frame and sets
  `EditorApp.frame`/`canvas` on the EDT (`SwingUtilities.invokeAndWait`) *before* `startCanvas()`, so
  the canvas/frame are published before the GL loop runs `create()`. Cross-thread posts to Swing go
  through Narya's `RunQueue.AWT` (e.g. `setPlaceView`). Editor mouse input flows through BUI
  (canvas-relative, bottom-left origin) — no AWT↔GL coordinate flip is needed; picking rays use
  `camera.getWorldCoordinates` against the canvas-sized viewport.
- Scene-graph node constructors may touch GL/asset state at construction (e.g. `WaterNode`); construct
  them on the render thread.

## The model pipeline

Source of truth: `assets/rsrc/**/model.properties` (+ sibling `*.mxml` mesh/anim XML + textures). The
build is a **two-stage compile-then-bake** (both run by `processResources`/`compileModels`):

1. `CompileModelTask` compiles each `model.properties`/`*.mxml` → a binary `model.dat`. **This is the
   vendored `jme` fork's model compiler** (app's `modeltool` source set) — the one remaining
   build-time use of the fork (Option A; eradication is [Epic 4](epic4-native-model-toolchain.md)).
2. `ModelToJ3o`/`BakeModels` (in `tools/j3o-converter`) reads each `model.dat` *via the fork's*
   `com.jme.util.export.binary.BinaryImporter`, translates the fork objects to jME3 (`ModelConverter`),
   and writes a jME3-native `model.j3o` with **jME3's** `com.jme3.export.binary.BinaryExporter`.
   (Particles were the analogue: `particles.jme` → `ParticleConverter` → `ParticleEmitter` `.j3o` —
   until the Epic 4 Phase 4½a freeze, 2026-07-07: the final verified bake is now COMMITTED as
   `rsrc/effects/**/particles.j3o` and staged by copy, the `.jme` are deleted, and no build task
   runs the fork particle path anymore.)

Staged at `assets/build/staging/rsrc/<same path as source>/model.j3o` (**note the `rsrc/` prefix** —
it has caused real bugs; anything verifying staged outputs must include it). At **runtime** models
load through `ModelCache` → a thin fork-free **`Model` facade** over the `.j3o` (jME3 `Mesh` +
`SkinningControl`/`Armature` + `AnimComposer`) — zero fork on the runtime classpath. 310 models bake
(`./gradlew :tools:j3o-converter:verifyCorpus` asserts 310/310 fork-vs-j3o parity).

Pipeline gotchas, all verified the hard way:
- `CompileModelTask` **swallows per-file errors** (one observed run produced zero outputs while
  reporting BUILD SUCCESSFUL); `assets/build.gradle` has a per-source assertion that fails the build
  on any missing `model.dat`. The bake similarly reports `Baked N (M failed)`.
- The pipeline **self-mutates the source tree by design**: `updatepropheight` rewrites
  `prop.properties` heights, and `update_lists` regenerates `rsrc/terrain/codes.txt` and the
  props/units/bonuses/sounds `.txt` indexes runtime code reads (`PropConfig`/`UnitConfig`).
- The staging dir doubles as the dev-runtime resource root: `bin/` scripts point `-Dresource_dir` at
  `assets/build/staging/rsrc`. Run `./gradlew :assets:processResources` (not piecemeal bake subtasks)
  to stage models **and** particles coherently. Per-town jars are only consumed by the getdown path.
- Avatar art ships as bundles: `rsrc/avatars/metadata.jar` + per-town/sex `components.jar`, consumed
  via nenya's `ResourceManager.initBundles` → `BundledComponentRepository` → `AvatarLogic`.

## Boards (maps)

Two representations, easy to conflate:
- **On disk**, a `.board`/`.game` was historically a `BoardFile` in the jME-fork `Savable` binary
  format. The Epic-1 board converter (`tools/board-converter`) did a **one-time conversion to a Narya
  stream format**, and the runtime read-path now loads that — no fork `BinaryImporter` at runtime.
  (The converter itself is fork-coupled, run-once scaffolding and may be retired.) Files live under
  `data/boards/<town>/<players>/` relative to `server_root` plus per-town menu backdrops
  (`rsrc/menu/<town>/town.board`); the server loads them all at startup ("Loading boards...").
- **On the wire**, the board's `BoardData` streams to clients via Narya — the representation the
  client actually consumes.

What's in the Narya-streamed `BoardData`: `com.threerings.bang.*` piece/board classes,
`com.threerings.util.StreamableHashMap`, `String`, and primitive arrays. The largest single array is
the town-board heightfield: **201×201 = 40,401** (a `byte[]`, ~40 KB) — under narya 1.19's default
65,536 container cap, but worth knowing if boards grow (`com.threerings.io.maxContainerSize`). Terrain
is a splat-textured heightfield (`TerrainSplat.j3md`); water is a reflective `WaterNode`
(`BangWater.j3md`, Fresnel sphere-map, render-thread-only); `rsrc/terrain/codes.txt` maps type codes.

The board editor (`./bin/bangeditor`) is a Swing app embedding the jME3 AWT canvas (see Threading);
it runs its own in-process server and needs MySQL (`bang`/`yeehaw`).

## Narya/wire-protocol behaviors that bit us

- **Sessions survive disconnects.** Reconnecting as the same user *resumes* the server-side session
  ("Session resumed"), keeping the token ring minted at original auth. Consequence: permission grants
  (e.g. the admin token in `ooouser.users.tokens`) take effect only on a *fresh* session — restart the
  server or wait for session expiry.
- **Streaming any enum touches depot.** narya 1.19's `Streamer.create` hard-references
  `com.samskivert.depot.util.ByteEnum` under its default enum policy, so depot must be on the
  **client** runtime classpath (the `runtimeOnly` depot dep in `client/desktop/build.gradle`).
- The server enables narya's deserialization whitelist at startup (`BangServer.main` →
  `ObjectInputStream.setAllowedClassPrefixes`); a legitimate new streamed type outside
  `com.threerings./com.samskivert./java.lang./java.util.` makes logon mysteriously fail — check the
  whitelist first.
- Client logon dev path: `-Dusername`/`-Dpassword` auto-logon lives in `LogonView.progress()`;
  `-test`/`-autoplay` go through `PlayerService.playComputer`, where only the autoplay variant is
  **admin-gated server-side** (`PlayerManager`).

## Client boot flow (dev)

`bin/bangclient` → `BangDesktop` (sets up jME3 `AppSettings` + signal handlers, `app.start()`) →
`JmeApp`/`BangApp` (a `com.jme3.app.SimpleApplication` on LWJGL3; `simpleInitApp` → `BangApp.create()`)
→ `BangClient.init` → `LogonView` (auto-logon) → town view → game via `PlayerService`. A successful
dev session needs: `./gradlew deploy` outputs, staged assets under `assets/build/staging/rsrc`, the
server up on 47624 (MySQL `bang`/`yeehaw`; `etc/test/` config — `bin/devtest` auto-seeds it from
`etc/*.dist`), and a user in `ooouser.users` (admin-token grant for `-autoplay`; see README).
`bin/devtest` automates the whole cleanup→deploy→server→client→screenshot loop.
