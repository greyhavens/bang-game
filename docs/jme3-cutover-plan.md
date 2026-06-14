# Epic 1 — jME3 cutover campaign

_(Part of the [Epics](epics.md) roadmap. This is Epic 1; Three Rings code consolidation is
[Epic 2](epic2-threerings-consolidation.md).)_

The execution plan for Phase 5's **atomic** step: move the running game off the vendored 2005-era
jME fork + LWJGL2 + libGDX onto jMonkeyEngine 3.9.0 + LWJGL3. Unlike the pre-cutover groundwork
(migration map, model loader, board converter, BUI seam — all merged to master), this work
**breaks the client until it lands**, so it runs as ordered phases on the single `JRECutover`
branch with compile/runtime checkpoints — not parallel worktrees that can't compile-verify against
a half-migrated base.

Steering docs (on master): `docs/jme3-migration-map.md` (the per-class DIRECT/ADAPT/REBUILD/DROP
table — the authority for every type move), `docs/jme3-model-loader.md`, `docs/jme3-board-conversion.md`,
`docs/jme3-bui-host.md`, `docs/engine-notes.md`.

## What already exists (reuse, don't rebuild)
- **Runtime model loader** — `tools/j3o-converter` `BangModelLoader`/`ModelConverter` load `model.dat`
  → jME3 `Spatial` (geometry, anim, skinning). Phase 3 wires it into `ModelCache`.
- **Board/bounty Narya format** — `tools/board-converter` converts the 364 fork-Savable
  `.board`/`.game` files to Narya streams; the runtime read-path swap is ~4 methods (see its doc §5).
- **BUI jME3 backend** — `bui/.../backend/Jme3RenderBackend`+`Jme3ImageBacking`, proven to render by
  `tools/jme3-host`. Phase 3 installs it and adds the input path.

## Cutover seam
The LWJGL2→LWJGL3 switch is a single atomic flip at the app-host boundary
(`BangDesktop`/`BangApp`/`EditorDesktop`). It **drags sound with it**: retiring gdx removes the
OpenAL device owner, so the vendored `com.threerings.openal` package moves to LWJGL3/jME3 audio in
the same phase. `com.jme.*` and `com.jme3.*` can coexist at compile time, so lower modules can be
migrated before the host flips — but nothing renders until Phase 3.

## Phases (sequential on `JRECutover`)

### Phase 1 — Build foundation + `app`/`bui` type migration
- Add jme3-core/jme3-lwjgl3/jme3-desktop/jme3-effects/jme3-terrain (root `ext jme3Version`) to the
  modules that will need them; begin retiring the `jme` fork module.
- Migrate `app` (`com.threerings.jme.*`) and `bui` from `com.jme.*` to `com.jme3.*` per the map:
  math/bounding (DIRECT renames), `Spatial`/`Node`/`Geometry`, `Controller`→`Control`,
  render-state objects → `Material`. BUI's seam already abstracts rendering; finish its type-token
  swap (`Renderer`→`RenderManager`, fork `ColorRGBA`→jME3, `BImage extends Quad`).
- **Checkpoint:** `app` and `bui` compile against jme3-core (game not yet runnable).

**Status (2026-06-13): `bui` DONE; `app` DONE — `./gradlew :app:compileJava` passes against
jme3-core with the fork `jme` module off app's main compile path. See the app model-pipeline
decision + execution status below.**
`./gradlew :bui:compileJava` passes against jme3-core with the fork `jme` module off bui's
compileClasspath (only jme3-core + gdx remain; gdx stays as a GL-free keycode-constant source
until Phase 3). jME3 is now BUI's only render backend: `BackendProvider` requires an explicit
`Jme3RenderBackend` install (it needs an `AssetManager`, so no lazy default), the fork backend
(`JmeRenderBackend`/`JmeImageBacking`) is deleted, the `BImageBacking` seam takes a jME3
`com.jme3.texture.Image`, `BImage` no longer extends the fork `Quad` (builds an ABGR8/BGR8 jME3
Image from its AWT raster), and `BRootNode` no longer extends the fork `Geometry` (the fork scene
hooks became engine-neutral `updateRootState(float)` + `renderWindows(RenderManager)`). `BGeomView`
is retyped to jME3 `Spatial` with its 3D render deferred to a Phase-3 ViewPort; `BCursor` keeps its
`BufferedImage` API with the native install deferred to Phase 3; `PolledRootNode` (fork/LWJGL2/gdx
input glue) was deleted in favour of the Phase-3 `Jme3RootNode`. The `tools:jme3-host` proof harness
was updated to the new API and still compiles.

`app` was initially recorded as blocked: its model framework (`Model`/`ModelMesh`/`SkinMesh`/
`ModelNode` + controllers, ~4.5k LOC, plus `CompileModelTask`) is the per-class table's REBUILD
risk #1 (fork `TriMesh`/batches/`GLSLShaderObjectsState` hardware skinning + embedded
`SpriteEmission` Savables). The resolution — recorded as the decision below — was to recognise that
this fork-format machinery is **build-time-only** (it compiles XML→`model.dat`; nothing runtime
needs to read `model.dat` once the Phase-3 bake produces `.j3o`), so it was split off app's
fork-free `main` into a `modeltool` source set rather than rebuilt. app `main` is now migrated and
compiles against jme3-core; see the decision + execution status below.

#### app model-pipeline rebuild — architecture decision (2026-06-13)

**Decision: RETIRE app's bespoke model classes + bake `model.dat` → `.j3o` at build time,
behind a thin app-side `Model`/`ModelNode` jME3 facade that localizes Phase-2 churn.**

The investigation that forced this (STEP 0):

- **The fork reader cannot stay on the runtime classpath.** `tools/j3o-converter`'s
  `ModelConverter`/`BangModelLoader` `import com.threerings.jme.model.{Model,ModelMesh,ModelNode,
  SkinMesh}` — i.e. they READ the *fork* model classes (via the fork `BinaryImporter` reading
  `model.dat`) and convert to jME3. They are a fork→jME3 *bridge*, not a fork-free runtime loader.
  The documented runtime loader (`docs/jme3-model-loader.md`) keeps the fork reader on the
  *runtime* classpath. That directly conflicts with the Phase-1 checkpoint (app must compile with
  the fork `jme` module OFF its classpath) — you cannot host that loader inside a fork-free `app`.
- **The 310 `model.dat` are fork-`BinaryExporter` Savable graphs**, regenerated each build by the
  project-owned `CompileModelTask`. So the resolution is to move the fork-format read to
  **build time**: bake `model.dat` → jME3-native `.j3o` once per build, and let runtime `app`
  load `.j3o` through stock jME3 with zero fork dependency. `ModelToJ3o` already does exactly this
  bake on top of the shared `ModelConverter`, corpus-verified 310/310.

So the model classes split three ways:

1. **Build-time (stays fork-coupled, lives in `tools/j3o-converter`):** the fork reader + the
   `ModelConverter`/`ModelToJ3o`/`ModelTextureResolver` bridge. `CompileModelTask`'s role
   **shrinks to staging `model.dat`** (unchanged); a new bake step (`ModelToJ3o`, wired into
   `assets:` in Phase 3) turns each `model.dat` into a sibling `model.j3o`. No runtime fork dep.
2. **Runtime (fork-free, lives in `app`):** a thin **`Model` facade** — a jME3 `Node` subclass
   that wraps the loaded `.j3o` `Spatial` and re-exposes the client-facing API shape
   (`createInstance`/`putClone` → jME3 `clone()`; `startAnimation`/`stopAnimation`/`hasAnimation`/
   `getAnimationNames`/observers → drive the `.j3o`'s `AnimComposer`/`SkinningControl`;
   `getEmissionNode`; `getControllers`; `resolveTextures(TextureProvider)` → re-resolve the
   `bang.textures` user-data via the resolver). This keeps `ModelCache.getModel(...)`'s return
   type and the ~29 client sprite/effect call sites' shape stable, so Phase-2 churn over the model
   API is localized to method-body retargeting, not a type-wide `Model`→`Spatial` sweep.
3. **DELETED outright:** the fork-coupled `ModelMesh`/`SkinMesh`/`ModelSpatial` (TriMesh/batch/
   GLSL-skinning machinery — the `.j3o` carries jME3 `Mesh`+`SkinningControl` instead), and the
   fork serialization (`read`/`write`/`putClone`-over-RenderState) bodies of `ModelNode`/`Model`.
   The procedural controllers (`Rotator`/`Translator`/`Billboard`/`Texture*`) become small jME3
   `AbstractControl`s; the emission controllers (`EmissionController` + game `*Emission`) are the
   effects port (Phase 4/§3.3 of the loader doc) and stay client-side.

Why not (a) pure RETIRE (ModelCache returns a bare jME3 `Spatial`): it pushes a type-wide
`Model`→`Spatial` rewrite plus an anim-API rewrite into ~29 Phase-2 files with no compile anchor.
Why not (b') keep the runtime fork reader + convert at load: it violates the fork-off-classpath
checkpoint and ships LWJGL2-era fork code into the LWJGL3 runtime. The chosen split is the only
one that (i) reuses the corpus-verified converter unchanged, (ii) gets app fork-free, and
(iii) keeps the client API shape for Phase 2.

#### app Phase-1 execution status (2026-06-13)

**Checkpoint passing command:** `./gradlew :app:compileJava` → BUILD SUCCESSFUL, fork `jme`
verified off app's `compileClasspath` (only `jme3-core` + the gdx keycode source from bui remain).
`./gradlew :app:compileModeltoolJava` also passes (the relocated fork build-time compiler).

**Module split implemented.** app gained a `modeltool` source set (`app/src/modeltool/java`,
`modeltoolImplementation project(":jme")`, off `main`'s classpath). Moved there, fork-format and
build-time-only, relocated to package `com.threerings.jme.tools.*`:
- the model compiler: `tools/{CompileModelTask,CompileModel,ModelDef,AnimationDef,BuildSphereMap,
  BuildSphereMapTask}`, `tools/xml/{ModelParser,AnimationParser}`;
- the fork model classes: `tools/model/{Model,ModelNode,ModelMesh,SkinMesh,ModelSpatial,
  ModelController,BillboardController,EmissionController,Rotator,Translator,TextureController,
  TextureAnimator,TextureTranslator,TextureProvider}`;
- the fork-only util: `util/{ShaderCache,ShaderConfig,BatchVisitor}` + fork-typed copies of
  `util/{JmeUtil,SpatialVisitor}` (their jME3 versions live in `main`).
`assets:compileModels`'s ant taskdef classpath was extended with
`project(":app").sourceSets.modeltool.runtimeClasspath`, so the XML→`model.dat` pipeline is
unchanged. **CompileModelTask's role is unchanged** (XML→fork `model.dat`); the *runtime* side
shrinks: nothing runtime reads `model.dat` anymore — the Phase-3 bake (`ModelToJ3o`) turns each
`model.dat` into a sibling `.j3o` that the client `AssetManager` loads natively.

**app `main` migrated onto jME3** (compiles): `util` (JmeUtil/SpatialVisitor/ImageCache —
ImageCache builds jME3 `texture.Image` RGBA8/RGB8), `sprite` (Path→`AbstractControl` + new
`AnimationController` speed/active/repeat seam; Sprite→`addControl`; path subclasses DIRECT),
`camera` (CameraHandler→jME3 `Camera`/`setAxes`/`Plane.getNormal`; GodViewHandler→jME3
`AnalogListener` with a Phase-3 `registerWith(InputManager)`), `effect` (FadeInOutEffect/
WindowSlider→`Geometry`+`Unshaded` Material in `Bucket.Gui`, `AbstractControl` stepping, screen
size passed in), `chat` (ColorRGBA move), and the `JmeContext`/`JmeApp` host seam (retyped to
`AssetManager`/`RenderManager`/`Camera`; `getRenderer`→`getRenderManager`, `getDisplay` dropped;
the live LWJGL3 host loop/input/BUI-install is the Phase-3 flip — `JmeApp` is a jME3-typed
skeleton until then).

**NOT done (left for Phase 2/3, by design):**
- The fork-free app-side **`Model` facade** is *not* written. The decision is to wrap the loaded
  `.j3o` `Spatial` and re-expose the client API, but the loader (`BangModelLoader`/`ModelConverter`)
  is fork-coupled and lives in `tools/j3o-converter`; the facade has no compile anchor until
  `client/shared` is on jME3 (Phase 2) and the `.j3o` bake/load path is wired (Phase 3). Writing
  it now would be speculative against code that cannot yet compile. **It is Phase 2's first task**
  (shapes below).
- The procedural controllers (`Rotator`/`Translator`/`BillboardController`/`Texture*`) and the
  `EmissionController` base remain only in `modeltool` (fork). The jME3 procedural-control
  equivalents are a cheap follow-up; the emission controllers are the effects port (Phase 4).

> **Deferred validation (gated on Phase 2):** `assets:compileModels` — the fork→`model.dat`
> pipeline now driven by app's `modeltool` source set — cannot run until client/shared compiles:
> its taskdef classpath pulls `configurations.tools → :tools → :server → :client:shared`. So the
> model pipeline (and a full `assets:deploy`) is re-verified at the END of Phase 2, not now. This
> was always the case (the tools→server→client chain predates the cutover); it just surfaces now
> because client/shared is mid-migration.

### Phase 2 — `client/shared` migration (the bulk: 164 fork-using files)

**STATUS (2026-06-13): DONE.** `:client:shared:compileJava`, `:server:compileJava`, and `:assets:compileModels` (310/310 model.dat) are all GREEN; zero fork imports remain outside the deferred-to-Phase-3 vendored `com.threerings.openal` package. `:client:desktop` fails only on the 2 gdx-host-wiring errors (BangDesktop/EditorDesktop) = the Phase-3 flip. Done via a foundation pass + 5 parallel fan-out slices + an integration drive-to-green (50 commits).

**Concrete starting point (the app `Model` facade + the seam edits app's migration forces):**

1. **Write the app `Model` facade** (`com.threerings.jme.model`, fork-free, in app `main`): a jME3
   `Node` that wraps the `.j3o`-loaded content + its `AnimComposer`/`SkinningControl`, re-exposing
   the surface client uses (measured): `getEmissionNode` (12), `getProperties` (8), `getAnimation`
   (7, returns an `Animation` carrying `frameRate` + `getDuration()`), `pauseAnimation` (5),
   `getControllers` (4), `startAnimation`/`stopAnimation`/`resolveTextures`/`createInstance` (3 each
   — `createInstance`/`putClone`→jME3 `clone()`; `resolveTextures(TextureProvider)` re-resolves the
   `bang.textures` user-data the converter preserves), `lockInstance`/`hasAnimation` (2 each),
   `setAnimationMode`/`reverseAnimation`/`getVariantNames`/`fastForwardAnimation`/`createPrototype`
   (1 each). Nested API shapes used: `Model.Animation` (`.frameRate`, `.getDuration()`),
   `Model.CloneCreator` (passed to emission controllers' `putClone`), `Model.AnimationMode`,
   `Model.AnimationObserver`. `ModelCache.getModel(...)` keeps returning this `Model`.
2. **Re-point `client/util/ModelCache`** at `assetManager.loadModel(".j3o")` → wrap in the facade;
   thread `variantIndex`/`Colorization[]`/detail into `ModelTextureResolver` (the indirection shape
   exists in tools/j3o-converter — see docs/jme3-model-loader.md §5).
3. **Edits app's `main` migration forces on client (the seam churn this commit created):**
   - `SpatialVisitor<ModelMesh>` sites (`ModelCache`, `effect/HeroInfluenceViz`,
     `effect/IronPlateViz`) → `SpatialVisitor<Geometry>` (the `.j3o` yields jME3 `Geometry`, not
     `ModelMesh`).
   - `JmeContext` consumers: `getRenderer()`→`getRenderManager()` (93 sites), `getDisplay()`
     removed (31 sites — re-point to `getCamera()`/`getAssetManager()` or a screen-size accessor);
     `getInputHandler()` removed (12 sites — Phase 3 input). Implementers `BangApp`/`EditorApp`/
     `BasicContext` re-fit to the new seam.
   - `IconConfig`/`ParticleUtil` fork `Controller.RT_*` → `com.threerings.jme.util.JmeUtil.RT_*`.
   - `FadeInOutEffect`/`WindowSlider` constructors now take `AssetManager`/screen-size (were
     `DisplaySystem`-global); their callers (`WindowFader` etc.) pass them through `BasicContext`.
   - `GodViewHandler` is now an `AnalogListener`; `GameInputHandler`/`EditorController` wire it via
     `registerWith(InputManager)` at the Phase-3 host.

Then proceed with the rest of the per-class table (scene graph, render-state→Material across all
sites, picking, camera, the `*Sprite`/effect framework, `BangBoardView`/`WaterNode`/sky).
- Drive off the map's per-class table: scene graph, render-state→Material across all sites,
  picking/intersection, camera, the `*Sprite`/effect framework, `BangBoardView`/`WaterNode`/sky.
- Regenerate nothing; this is hand work. **Checkpoint:** `client:shared` compiles against jme3.

#### Phase 2 FOUNDATION pass — DONE (2026-06-13): what landed + the fan-out assignment

The shared foundation + the mechanical bulk landed on `JRECutover` (4 commits). `client:shared`
does **not** compile (expected — single compile unit; the REBUILD clusters below remain). What is
done and compile-clean:

- **app `Model` facade** (`app/.../jme/model/Model.java`, fork-free) — a jME3 `Node` wrapping the
  `.j3o` content + its `AnimComposer`/`SkinningControl`, re-exposing the full measured client
  surface (`getEmissionNode`/`getProperties`/`getAnimation`/`start|stop|pause|reverse|
  fastForwardAnimation`/`getControllers`/`createInstance`/`createPrototype`/`lockInstance`/
  `hasAnimation`/`setAnimationMode`/`getVariantNames`/`resolveTextures`) + nested
  `Animation`(`.frameRate`,`getDuration()`)/`CloneCreator`/`AnimationMode`/`AnimationObserver`.
  **`:app:compileJava` is GREEN.** `ModelTextureResolver` was promoted from `tools/j3o-converter`
  into app `main` (`com.threerings.jme.model`) as the single home shared by `ModelCache` and the
  converter; a fork-free `TextureProvider` seam (`getTexture(Geometry, name)→Texture`) was added;
  the converter now writes `bang.frameRates` user data so the facade re-exposes `frameRate`.
- **`ModelCache`** re-pointed at `assetManager.loadModel(type+"/model.j3o")` wrapped in the facade;
  fork VBO/display-list/shader bookkeeping deleted (jME3 owns it); variant/colorization threaded
  through `ModelTextureResolver`. Returns the facade `Model` (call-site type stable).
- **Seam edits applied:** `ctx/_ctx.getRenderer()→getRenderManager()` (~80 context sites);
  `SpatialVisitor<ModelMesh>→<Geometry>` (ModelCache, IronPlate/HeroInfluence viz, TreeBedSprite);
  fork `Controller.RT_*→JmeUtil.RT_*`; `ColorRGBA.white/black/...→.White/.Black/...` (jME3
  capitalizes; ~100 sites); `FadeInOutEffect`/`WindowSlider` callers thread
  `getAssetManager()`/`getCamera().getWidth()/getHeight()`; `BasicContext.loadParticles` Spatial
  type-token → jME3.
- **Mechanical DIRECT renames** (104 files): math (`Vector*/Quaternion/Matrix*/FastMath/Ray/
  Plane`), `ColorRGBA` (renderer→math), `bounding.*`, `scene.Node`, `geom.BufferUtils`→
  `util.BufferUtils`. **BUI render-hook param `Renderer→RenderManager`** in 33 view/icon/background
  files whose only fork dep was overriding `render*(Renderer)` (NOT rebuild — BUI's hook type).

Result: fork-importing files in client/shared dropped 132→**99**. The remaining 99 are the
parallel fan-out. javac caps at 100 errors so the assignment is keyed on remaining fork imports
(the true work surface), not the truncated error count.

**FAN-OUT ASSIGNMENT (remaining-error breakdown by subsystem, with the dominant fork API each
cluster still needs).** All paths under `client/shared/.../com/threerings/bang/`.

1. **`*Sprite`/sprite framework — 31 files** (`game/client/sprite/`). Dominant fork API:
   `scene.Spatial`(ADAPT: setRenderState→Material, queue/cull hints, draw hooks gone),
   `scene.state.{TextureState,LightState,MaterialState}`→**Material** (REBUILD),
   `scene.Controller`→`control.AbstractControl` (Path/Spinner/Bouncer + the `*Emission`),
   `scene.SharedMesh`→shared-Mesh Geometry, `scene.BillboardNode`→`BillboardControl`,
   `scene.shape.Quad`→Mesh, `image.Texture`→`texture.Texture2D`,
   `util.export.*`(Savable embedded in models — the emission Savables), `jmex.effects.particles.*`
   →`ParticleEmitter`. Files: ActiveSprite, MobileSprite, PieceSprite, UnitSprite, PropSprite,
   BonusSprite, BreakableSprite, CowSprite, BisonSprite, OneArmedBanditSprite, WendigoSprite,
   ViewpointSprite, MarkerSprite, SafeMarkerSprite, CounterSprite, GenericCounterNode,
   PieceTarget, PieceStatus, UnitStatus, ShotSprite, FireworksSprite, TreeBedSprite, Spinner,
   Bouncer, and the emission controllers (FrameEmission, GunshotEmission, MisfireEmission,
   DudShotEmission, SmokePlumeEmission, ParticleEmission, TransientParticleEmission). **Theme:
   render-state→Material + Controller→AbstractControl + particles.** The `*Emission` classes are
   the effects port (Phase 4) and code their `putClone(Controller, Model.CloneCreator)` /
   `resolveTextures(TextureProvider)` against the facade's now-defined `CloneCreator`/
   `TextureProvider`.
2. **Effect viz — 13 files** (`game/client/effect/`). Dominant: `scene.Controller`(REBUILD→
   AbstractControl), `jmex.effects.particles.*`(REBUILD→ParticleEmitter), render-state→Material,
   `scene.BillboardNode`, `scene.shape.Quad`. Files: ExplosionViz, RepairViz, WreckViz,
   HealHeroViz, DamageIconViz, IconViz, IconInfluenceViz, HeroInfluenceViz, IronPlateViz,
   NoncorporealViz, ParticleEffectViz, ParticleInfluenceViz, ParticlePool. **Theme: particle
   re-author + render-state→Material overlay.**
3. **Board renderer + sky/water/terrain (the REBUILD critical path) — 5 files**
   (`game/client/`): `BoardView` (picking: `intersection.PickResults/TrianglePickResults`→
   `collideWith`/`CollisionResults`; lights; TextureRenderer), `BangBoardView`, `TerrainNode`
   (splat combine→custom MatDef), `WaterNode` (reflection FBO + GL-thread init), `SkyNode`
   (Dome + gradient). **Theme: render-state→Material, multi-texture splat MatDefs, RTT→FrameBuffer,
   picking→collideWith.** Highest fidelity risk (map §4 risks #3/#4).
4. **Game handlers + in-game views — 14 files** (`game/client/`): EffectHandler,
   BallisticShotHandler, RocketHandler, HoldHandler, RobotWaveHandler, AreaDamageHandler,
   WendigoHandler, GameInputHandler (`input.InputHandler`→Phase-3 InputManager/AnalogListener),
   GameCameraHandler (`renderer.Camera` ADAPT), GridNode (`scene.Line`→Lines Mesh), BangView,
   PlayerStatusView, HeroBuildingView, BountyGameOverView. **Theme: Camera/Spatial ADAPT + a few
   render-state sites; GameInputHandler is the Phase-3 input seam (`GodViewHandler.registerWith`).**
5. **Game data Savables — 10 files** (`game/data/`, `game/util/BoardFile`): `util.export.*`
   (Savable/InputCapsule/OutputCapsule/JMEImporter/JMEExporter) + `binary.BinaryImporter/Exporter`.
   Files: Piece, Prop, Marker, Track, Viewpoint, BangBoard, BoardData, BangConfig, Criterion,
   BoardFile. **Theme: `com.jme.util.export.*`→`com.jme3.export.*` (ADAPT, case rename JmeImporter/
   JmeExporter) for the in-memory classes; the on-disk `.board`/`.game` migration is the offline
   board-converter (Phase 3), NOT a code change here.** Mechanical-ish but touches the wire/save
   format — pairs with the board-converter doc.
6. **Central render-state factory + RTT + particle/texture caches — 9 files**: `util/RenderUtil`
   (587-ln state factory → shared-**Material** library — gates clusters 1-3), `util/IconConfig`,
   `util/ParticleUtil`, `util/BackTextureRenderer` (`renderer.TextureRenderer`→FrameBuffer),
   `client/util/{TextureCache,TexturePool,ParticleCache,ResultAttacher}` (`image.{Texture,Image}`,
   `util.TextureKey/TextureManager`, `scene.state.gdx.records.TextureStateRecord` DROP,
   `jmex.effects.particles.*`). **Theme: the render-state→Material library + texture/particle
   loading off fork TextureState onto `Texture2D`/AssetManager.** RenderUtil is the keystone —
   do it first; clusters 1-3 draw from it.
7. **Core client/views — 8 files** (`client/`): BangApp, BasicClient (`renderer.Renderer` host
   wiring → Phase-3), BangUI, BangPrefs, GlobalKeyManager, OptionsView, TownView,
   `client/bui/WindowFader` (`scene.Controller`→AbstractControl). **Theme: mostly host-seam
   (`DisplaySystem`/`InputHandler` removal, Phase-3) + a couple Controller/Renderer sites.**
8. **Editor — 3 files** (`editor/`): EditorBoardView, CameraDolly (`renderer.Camera`),
   CrossStatus (`scene.Line`/`SharedMesh`), + `jmex.terrain.util.*` heightmaps (ADAPT→jme3-terrain),
   `scene.state.WireframeState`→`material.RenderState.setWireframe`, `geom.Debugger`. **Editor-only,
   Phase-5 deferrable.**
9. **Misc place views — 6 files**: `ranch/client/UnitView` (RTT portrait), `saloon/client/PaperView`,
   `chat/client/SystemChatView`, `bounty/client/BountyGameEditor`, `bounty/data/{RankCriterion,
   IntStatCriterion}` (Savable). **Theme: a few render-state/Savable sites; small.**

**Facade/seam API the fan-out codes against (decisions to honour):**
- `com.threerings.jme.model.Model` (app) is the model handle `ModelCache` returns; it is a jME3
  `Node`. `getEmissionNode()→Node`, `getControllers()→List<com.jme3.scene.control.Control>` (the
  effects port populates this), `getAnimation(name)→Model.Animation` (`.frameRate` int +
  `getDuration()` float). `createInstance()/createPrototype(variant)` clone the content.
- `com.threerings.jme.model.TextureProvider` is **fork-free**: `Texture getTexture(Geometry, String)`
  (NOT the fork `TextureState getTexture(String)`). Emission controllers' `resolveTextures` recode
  to this. `ModelTextureResolver` (app `com.threerings.jme.model`) is the shared re-resolution
  engine; `textureAssetPath(typePath, name)` is public.
- `JmeContext`/`BasicContext` seam: `getRenderManager()` (not `getRenderer()`), `getAssetManager()`,
  `getCamera()` (jME3 `Camera`, has `getWidth()/getHeight()` for screen size — there is **no**
  `getDisplay()` and **no** `getInputHandler()`). Input is Phase-3 (`registerWith(InputManager)`).
- `JmeUtil.RT_{CLAMP,WRAP,CYCLE}` replace fork `Controller.RT_*`.
- `FadeInOutEffect`/`WindowSlider` ctors take `AssetManager` + screen `width,height` up front.

### Phase 3 — Atomic app-host cutover (the flip)
- Rewrite `BangDesktop`/`BangApp`/`EditorDesktop` as a jME3 `SimpleApplication` (LWJGL3 context).
- `Jme3RootNode` + `RawInputListener` → BUI input; install `Jme3RenderBackend`.
- Move `com.threerings.openal` to LWJGL3/jME3 audio; drop gdx + LWJGL2 + the vendored-for-LWJGL2 copy.
- Wire `BangModelLoader` into `ModelCache`; switch board/bounty load to the Narya format (run the
  board-converter once to produce the shipped `.nboard`/`.ngame`).
- **Checkpoint:** client launches a jME3 window and reaches the login/town view.

### Phase 4 — Board renderer + effects
Split into sub-phases as the work fell out (checkpoint: a board renders in-game with fidelity
approaching the fork baselines in `baseline/fork-before/`):
- **4a — DONE:** multi-texture terrain splat (custom `TerrainSplat.j3md`), board background/fog
  color to the host ViewPort, + two real bugs fixed along the way: BUI textured-image channel
  scramble (the pink UI cast — ABGR8 endianness) and instant idle-out (`Jme3RootNode` tick stamp
  uninitialized → passive clients logged out in ~10s). Units, fences/small props, combat-effect
  icons, shadows, and the HUD render correctly.
- **4b — DONE (non-bug):** investigated the untextured-white-buildings report; the bake/resolve/
  render path is actually correct — 272/272 baked building geometries carry a bound DiffuseMap +
  normals, and the live frontier-town view (`baseline/jme3-phase4/building-town-after.png`) shows
  all buildings fully textured (brown wood, signs, windows), matching the fork baseline. The white
  shot was stale pre-4a state. Deliverable: a headless **`RenderToPng`** offscreen harness (no
  window; LWJGL3 FrameBuffer through the real load/resolve/clone path) — the Phase-7 #1 tool, seeded
  early. Caveats: the offscreen harness's own lighting has a blue tint to calibrate (cosmetic,
  harness-only); the in-game *game* board (vs town view) wasn't re-captured headlessly (autoplay
  doesn't drive BUI big-shot selection) — high confidence it's fine given same `BoardView` renderer.
- **4c — DONE:** water Fresnel sphere-map reflection verified and fixed. The shader/material
  approach is correct (animated, reflective, sphere-environment-mapped — reproduces the fork's
  EM_SPHERE water<->sky Fresnel blend), confirmed by a headless offscreen render of the actual
  `BangWater.j3md` material + the same Fresnel sphere map + a wave-displaced surface (new
  `tools/j3o-converter` `RenderWaterToPng` harness; `baseline/jme3-phase4/water-{before,after}.png`).
  **Root-cause bug fixed:** the Fresnel sphere map was tagged `ColorSpace.sRGB`, which the
  gamma-correct live pipeline (jME3 default `GammaCorrection=true`, inherited by `BangDesktop`)
  sRGB-decodes on sample, crushing the baked water/sky colors to a dark, near-uniform sheet (the
  classic "flat solid color" water failure). The fork displayed these baked colors verbatim with no
  gamma management, so the sphere map is now tagged `ColorSpace.Linear` in `WaterNode.refreshColors`
  — the surface renders at full color with the reflective variation reading as water. Animation is
  confirmed (the FFT-driven normals change each frame; two harness frames diff across the whole
  surface). Also fixed the Phase-4b harness blue-tint caveat: it was an RGBA-vs-BGRA channel swap in
  the framebuffer readback (`Screenshots.convertScreenShot` assumes BGRA) plus a missing sRGB encode
  on the offscreen color target — `RenderWaterToPng` reads RGBA directly into the image and uses an
  sRGB color target, so its colors are now neutral/correct.

  **LIVE-BOARD VERIFICATION (2026-06-13, this pass):** the water was rendered on a real shipped
  `.board` through the full live client+server (not the harness) and visually confirmed to match
  the fork reference (`baseline/fork-before/water.png`): saturated, reflective, reads-as-water teal.
  Board used: **Thunderbird Rock** (`data/boards/indian_post/4/thunderbird_rock.board`, water
  `#003232`, sky-overhead `#00FF99`, ~65% of tiles below the water plane — a coastal board like the
  fork shot). Reached via `bangserver indian_post` + autoplay client `-Dboard="Thunderbird Rock"
  -Dtest=true -Dautoplay=true`. Live render shows teal coastal water around a sandy island with
  rope bridges; sampled live water avg `#347F7E` vs the fork shot's `#255245` — same saturated
  green-teal hue family (different specific board, so not pixel-identical, but the fidelity bar —
  saturated/reflective/animated, not pale-flat or dark-muddy — is met). Deliverables:
  `baseline/jme3-phase4/water-live-{before,after}.png` (+ `water-live-after-frame2.png`). **No code
  tuning was needed** — the prior sRGB->Linear sphere-map fix produces correct color on a real board;
  this pass only verified it. `refreshColors` water/sky colors are board-driven (`getWaterColor()`/
  `getSkyOverheadColor()`), identical to the fork, so any board's stored teal reproduces verbatim.

  **Two non-water findings from this pass:**
  - *Phase-3 board read is HEALTHY.* The live client+server read shipped `.board` files (now pure
    Narya streaming) with zero errors — server "Loading boards..." clean, client "Downloading board"
    + full board render. A standalone scan of all 165 boards via `BoardFile.loadFrom` also read every
    `BangBoard` (incl. water fields) fine. (A bare scan hits a `Prop.init` NPE only when the
    `PropConfig` registry isn't initialized — a harness-classpath artifact, not a format bug; it
    vanishes with the staging `rsrc/` on the classpath, as the live client/server have.)
  - *`bangserver indian_post` registered NO indian_post boards* until fixed: `etc/test/
    server.properties` left `indian_post.town_id` commented out, so `ServerConfig.townId` fell back
    to `frontier_town` (townIndex 0) and `BoardManager.mapBoard` skipped every board with
    `getMinimumTownIndex() > 0` (i.e. all indian_post boards) → `getBoard()` returns null →
    `BangManager.startRound` NPE at `_rounds[roundId].board.name`. Set `indian_post.town_id =
    indian_post` (local `etc/test/`, gitignored) to serve indian_post boards on that node. Frontier
    boards were unaffected throughout.
- **4d — DONE (test tooling):** `bin/devtest` — a standard live-run "run sheet" that ends the
  recurring setup tax behind most agent run failures. It (1) kills stale Bang processes + frees
  47624, (2) verifies MySQL up and `bang`/`yeehaw` connects, (3) `./gradlew deploy` (green;
  `--no-deploy` to skip), (4) starts the server and **polls the log until "Server listening"**,
  (5) launches the client with standard flags (muted by default; `-test`/`--board` `-autoplay`
  creds), (5b) **clicks into the window via python-xlib `warp_pointer`+XTest — REQUIRED to start the
  autoplay game** (without a click you only ever capture the static pre-game screen; the #1 thing
  that was silently tripping agents), (6) optional `--shot` screenshot, (7) trap-based teardown that
  waits for process death (with `-9` escalation) and frees the port. Validated end-to-end
  (cleanup→mysql→server-wait→click→autoplay board→shot→clean teardown). Two gotchas learned and
  baked in: match the game window on **"howdy"** (not "bang" — that hits a "bang-game" terminal
  title) and click via window-relative `warp_pointer` (root-coord `translate_coords` returns bogus
  negatives on this multi-monitor display). Runbook: `docs/running-the-game.md` — every future "run
  the app" agent brief points at it. Pairs with the Phase-5 offscreen harnesses
  (`RenderToPng`/`RenderWaterToPng`) for deterministic windowless model/water shots.
_(Phase 4 core rendering is done at 4a–4d. The remaining fidelity items moved out to **Phase 6**:
particles → 6a, live-render/input defects → 6b — they follow the Phase-5 testing harnesses, which
accelerate diagnosing them.)_

### Phase 5 — Agent testability (brought forward — ahead of the fidelity/editor work)

Brought forward (was originally last): these harnesses directly accelerate the Phase-6 fidelity
defects and the Phase-7 editor regression, so they come first. This is a real-time 3D OpenGL
networked game — the hard part of agent-driven work is that an agent can neither reliably *see* the
rendered output (X-grabs off `DISPLAY=:1` are flaky) nor deterministically *drive* gameplay; there is
no Playwright equivalent (an OpenGL framebuffer has no DOM). `bin/devtest` (4d) already covers the
live-client path. JDK 25 and a JUnit 4→5 bump do **not** help (JDK 21 already has JFR + helpful NPEs;
the bottleneck is coverage of a hard-to-assert app, not the runner). The two harnesses to finish:

1. **Headless offscreen render-to-PNG (highest value; seed already built).** Generalize the existing
   `tools/j3o-converter` `RenderToPng`/`RenderWaterToPng` into a board/scene/model render-to-PNG that
   runs windowless on a `FrameBuffer`, so agents diff deterministic snapshots against
   `baseline/fork-before/` instead of flaky X-grabs. Feeds CI visual regression and per-change checks
   (incl. the Phase-6 sprite defects + the Phase-7 editor regression).
   _**DONE.** Factored the two seeds onto a shared `OffscreenRenderApp` base (AppSettings +
   `OffscreenSurface` launch, color-texture `FrameBuffer` + one-shot `CaptureProcessor` PNG capture
   with the correct RGBA/Y-flip readback, `BoardView`-style lighting, **Z-up** bounding-box framing —
   fixing the seed's Y-up framing that laid Z-up character models on their side). New render modes,
   all on the isolated jME3 + LWJGL3 + `project(":app")` `renderToPngRuntime` classpath (no fork, no
   LWJGL2 sealing clash): (1) `RenderModelToPng` — a unit/big-shot model with skin + a posed
   `AnimComposer`/`SkinningControl` animation frame (`Models.loadModel`/`poseAnimation` select a named
   clip and `setTime` it, then `updateLogicalState` so the skinned mesh deforms before capture);
   (2) `RenderSceneToPng` — N `modelType[@anim[:time]]` specs posed on a ground-grid scene; (3)
   `SnapshotDiff` — GL-free mean-per-channel PNG diff that writes a red heat-map and exits non-zero
   past `-Ptolerance` (default 0.015), the regression gate. Gradle tasks `renderToPng`,
   `renderWaterToPng`, `renderModelToPng`, `renderSceneToPng`, `snapshotDiff`; run sheet in
   `docs/running-the-game.md`. Files: `tools/j3o-converter/.../tools/j3o/{OffscreenRenderApp,Models,
   RenderModelToPng,RenderSceneToPng,SnapshotDiff}.java` (new) + refactored `RenderToPng`/
   `RenderWaterToPng` + `build.gradle`. Verified rendering: `props/frontier_town/buildings/saloon`
   (static, upright + fully textured), `units/frontier_town/shotgunner` & `units/frontier_town/cavalry`
   & `units/indian_post/buffalo_rider` (skin + textures resolve, anims list, hardware skinning engages
   — and they **reproduce the Phase-6 defect deterministically**: textures are correct but the posed
   skinned armature is contorted, pointing the diagnosis at the skin/joint bind, not texture resolve),
   `units/wreckage/wheel_big` (0-anim, falls through to bind pose cleanly), water surface, and the
   diff gate (PASS on identical, FAIL on differing)._
2. **Headless Narya bot client + server-side dobj assertions.** A rendering-free client that logs in,
   drives gameplay through the service API, and asserts on distributed-object state — the closest
   thing to "Playwright for this game." Leans on the existing AI/`-autoplay`/bot + the Narya wire.
   - **DONE.** New leaf module `tools:bot-client` (`tools/bot-client/`), entry point
     `com.threerings.bang.tools.bot.BangBotClient`. It is a pure Presents network client: a bare
     `new Client(BangCredentials, BasicRunQueue)` + a minimal hand-rolled `ParlorContext`
     (Location/Occupant/Chat/Parlor directors only) driven by `rqueue.run()` — **no jME3/LWJGL/GL/
     BUI**; it never touches `BangClient`/`BasicClient`/`JmeApp`. (jME3 jars sit inertly on the
     classpath via `:server`→`:client:shared`, but no display is ever initialized.) Modelled on
     narya's own `presents/crowd` `TestClient`s. How it works: logs in `test`/`yeehaw` (mirrors
     `BangClient.createCredentials` incl. the `IdentUtil` machine ident — the server rejects a blank
     one), calls `PlayerService.playComputer(players, scenarios, board, autoplay=true)` to spawn an
     all-AI game (needs the admin token, which test/yeehaw holds), intercepts the `gameIsReady`
     notification via a `GameReadyObserver`, and enters the place through the authorized
     `LocationDirector.moveTo` path — but a subclassed `LocationDirector` hands back a trivial
     headless `PlaceController` (null view) instead of the rendering `BangController`. It then
     reproduces BangManager's all-AI pre-game handshake purely from dobj state
     (`manager.invoke("playerReady")`, then `playerReadyFor(SKIP_SELECT_PHASE)`/`(IN_PLAY)` as the
     state advances — for an all-AI game the *observing* client drives phase progression; the server
     runs both `BangAI`s and ticks autonomously, no per-tick input or window-click needed).
   - **Asserts (fail loudly, non-zero exit → CI-ready):** the `BangObject`/`GameObject` transitions
     `PRE_GAME → SELECT_PHASE → IN_PLAY → GAME_OVER`; ticks advance past 0 (board simulated);
     `players[]` has the expected count; a winner is flagged in `winners[]` at game over; and
     `points[]` sums to a non-zero total (the AIs actually played, captured/scored). A hard
     wall-clock watchdog fails the run if the game stalls.
   - **Verified end-to-end** against a `bin/devtest --no-client` server (Cattle Rustling, 2 AIs):
     reached `GAME_OVER`, `last tick = 47`, `winners = [true, false]`, `points = [291, 290]`,
     "ALL DOBJ ASSERTIONS PASSED". Run: start the server (`bin/devtest --no-client`), then
     `./gradlew :tools:bot-client:run` (tunables: `-Pplayers= -Pscenario= -Pboard= -Ptimeout=
     -Phost=`; the run task `dependsOn :assets:processResources` so the staging `rsrc/` configs are
     on the classpath — units/props/i18n must resolve as pieces stream in, exactly as
     `bin/bangserver` puts staging on the server classpath).
   - **Deferred:** per-piece (unit) tracking is *not* asserted. The server writes `BangObject.pieces`
     as a direct field assignment (no dobj event) and the live client rebuilds the piece set from the
     loaded `.board` file (rendering-adjacent, out of scope here), so `pieces` stays empty
     client-side and its element-update events are dropped. The bot instead asserts on the
     wire-synced attribute state (phases/ticks/winners/points) — `points > 0` is the attribute-backed
     proxy that units fought and scored. A future extension could load the board via `BoardCache` to
     get full per-unit assertions. Also: only the frontier_town node is exercised by default
     (`-Pplayers`/`-Pscenario` cover the rest; indian_post needs the node running).

Supporting, lower priority: fold the converter harnesses (j3o 310/310, board 364/364) + the 3 unit
tests into a JUnit + CI gate (AssertJ for ergonomics; JUnit 5 optional, not the point); JFR for
frame-timing/perf telemetry. Not worth doing for tooling reasons: JDK 25, the JUnit major bump.

### Phase 6 — Remaining fidelity & polish

Reproduce/diagnose with the Phase-5 harnesses + the `bin/devtest` run sheet.

**6a — particles. [DONE 2026-06-14]** The 60 fork-format `effects/<key>/particles.jme`
(`ParticleGeometry`/`ParticleMesh` graphs) are re-authored as jME3 `ParticleEmitter`s by a
build-time fork→j3o bake, mirroring the `model.dat`→`.j3o` pipeline. The runtime `ParticleCache`
load path (Phase 2) lights up unchanged: it loads `effects/<key>/particles.j3o` through the
`AssetManager`, applies the `particles.properties` scale + Z-up rotation, clones per instance.

What landed:
- **`tools/j3o-converter/.../jme3/model/ParticleConverter.java`** — the fork→jME3 bridge
  (`com.jmex.effects.particles.ParticleGeometry` → `com.jme3.effect.ParticleEmitter`), on the
  fork-coupled build-time classpath alongside `ModelConverter`. A `particles.jme` with multiple
  particle nodes becomes a `Node` of multiple `ParticleEmitter`s, transforms preserved.
- **`tools/j3o-converter/.../tools/j3o/ParticlesToJ3o.java`** — the batch baker (analogue of
  `BakeModels`): walks staged `effects/**`, imports each `particles.jme` via the fork
  `BinaryImporter` (with the same `TextureKey` location override the fork `ParticleCache` used so
  embedded `TextureState`s resolve), converts, writes the sibling `particles.j3o`, and reports
  coverage. `-Peffect=<town>/<effect>` bakes + verbosely reports one effect.
- **Gradle wiring:** `:tools:j3o-converter:bakeParticles` (+ `renderParticleToPng`); `assets`
  runs it `finalizedBy :assets:copyTownBits` (which stages the `.jme`+textures), so a
  `./gradlew deploy` / `:assets:compileModels --rerun-tasks` bakes all 60 into
  `assets/build/staging/rsrc`.

Param mapping (fork → `ParticleEmitter`): count→`setNumParticles`. **Emission mode is keyed on the
fork controller's repeat type** (`RT_CLAMP` = one-shot burst → `setParticlesPerSec(0)`, the spawn
path fires the single burst; `RT_WRAP`/`RT_CYCLE` = ambient → continuous `setParticlesPerSec`: the
fork `releaseRate` when `controlFlow` is set, else a refill at count/mean-life, clamped to
count/mean-life so it can't blow out). _(Corrected in Phase 8 — the original "every non-controlFlow
effect → continuous count/mean refill" mapping made one-shot bursts emit forever, producing a
runaway additive bloom in the live game; see the Phase 8 emission-mode bullet for the full
root-cause/fix.)_ start/end size→`setStartSize`/`setEndSize`; start/end
color→`setStartColor`/`setEndColor`; min/max lifetime (fork ms) →`setLowLife`/`setHighLife` (÷1000);
`emissionDirection`×`initialVelocity` →`influencer.setInitialVelocity`, `maximumAngle`
cone→`setVelocityVariation` (1.0 at PI/2); `particleSpinSpeed`→`setRotateSpeed`+`setRandomAngle`;
`velocityAligned`→`setFacingVelocity`; emit type point/rect/ring/line→`EmitterPoint`/`Box`/`Sphere`/
thin-`Box` shape; texture (fork `TextureState` first texture's image location, incl. cross-effect
shared textures resolved by the rsrc-relative path) → a `Common/MatDefs/Misc/Particle.j3md` material
with `ImagesX/Y=1` (the fork particle system has no sprite grid); blend mode additive
(`DB_ONE`)→`Additive` else `Alpha`; `BasicGravity`→`setGravity` (negated to jME3's subtract
convention), `BasicWind` folded into gravity. **Fork params with no jME3 equivalent (recorded as
dropped, effect still renders without that secondary motion):** `BasicDrag`, `BasicVortex`,
`SwarmInfluence`, `WanderInfluence`; `alphaFalloff` (distance fade) and `releaseVariance` have no
direct emitter setter.

Coverage: **60/60 bake (0 failed).** 26 convert fully clean; 34 are "degraded/lossy" — mostly one
unmappable influence dropped (drag/vortex/swarm/wander) but a working emitter still produced; only
**3** have an unresolved texture (`indian_post/rain` + `ambient_drizzle` are untextured `PT_LINE`
rain streaks with no fork texture; `boom_town/buggy_logic` Sparks references a content path that
doesn't exist — `boom_town/resurrection/spark.png`). Model corpus unaffected:
`:tools:j3o-converter:verifyCorpus` still **310/310** parity.

Verify (offscreen, deterministic): `./gradlew :tools:j3o-converter:renderParticleToPng
-Peffect=frontier_town/explosion -Pframes=10 -Pout=/tmp/x.png` — steps the emitter N frames
(`OffscreenRenderApp` gained a warmup hook) then snapshots through the isolated jME3+LWJGL3 render
classpath. Confirmed rendering: `frontier_town/explosion` (textured fireball + alpha smoke + rotating
quads), `frontier_town/fire` (orange flame tongues + smoke), `frontier_town/dust`,
`boom_town/barrel_explosion`. Tight point-clouds (`fireflies`) saturate at the bbox framing — a
harness zoom artifact on a tiny bound, not a converter defect; the live board view (camera far back)
shows them as discrete dots. The `*Emission` controllers (Phase 2 `AbstractControl`s) now have real
emitters to drive. NOTE: a live `bin/devtest` run in the agent's worktree was blocked by a
server-bootstrap NPE — **a worktree-setup artifact, not a code regression** (the canonical checkout
bootstraps fine; the Phase-5 bot agent played a full live game). Root cause: `etc/test/` is
gitignored, so a freshly-created worktree has no local config; the server's `server_auth` then bakes
in blank, `ServerConfig.getAuthenticator()` returns null on the blank key, and `BangServer$Module`'s
`bind(Authenticator.class).to(null)` (BangServer:116) throws an opaque Guice NPE far from the cause.
Addressed in Phase 6z (fail-fast on the misconfig + auto-seed `etc/test/` in `bin/devtest`).

**6b — remaining live-render & input defects** (observed on live boards 2026-06-13; cluster-1/3
fidelity + Phase-3 input):

- **Claim/counter numbers render sideways (rotated ~90°). [FIXED 2026-06-14]** The original
  hypothesis (billboard `Alignment.Screen` Y-up vs. board Z-up) was **wrong**: `Screen` mode aligns
  the quad's local +Y to `camera.getUp()` directly (screen-up), which is correct regardless of the
  board's up-axis, and the icon billboards (`PieceTarget`, `OneArmedBanditSprite`, `IconViz`,
  `IconInfluenceViz`) were never rotated. The real root cause was a **transposed UV assignment** in
  `RenderUtil.createTextTexture`: the ported `tcoords` were `(0,0),(0,h),(w,h),(w,0)` assigned to the
  jME3 `com.jme3.scene.shape.Quad` vertex order (BL, BR, TR, TL). That maps the texture's U axis (the
  text's reading direction) onto the quad's **+Y** and V onto **+X** → every glyph rotated 90°. jME3's
  `Quad` winds BL/BR/TR/TL with canonical UVs `(0,0),(1,0),(1,1),(0,1)` (verified against engine
  source `Quad.updateGeometry`); the working `IconConfig.createIcon`/`FrameControl` paths already use
  that winding. **Fix:** reorder the `tcoords` in `RenderUtil.createTextTexture` to
  `(0,0),(w,0),(w,h),(0,h)` so U→local X and V→local Y. This is a single central fix that corrects
  **every** text billboard at once: `CounterSprite`, `GenericCounterNode`, `HeroInfluenceViz` (level
  count), and `DamageIconViz` — all route through `createTextTexture`/`createTextQuad`. The
  `BillboardControl` `Alignment.Screen` on those sites was left as-is (it is correct). Audited the
  icon billboards too (`PieceTarget`, `OneArmedBanditSprite`, `IconViz`, `IconInfluenceViz`): they use
  plain `Quad` default UVs / `FrameControl`'s canonical-winding UVs and are not affected. File changed:
  `client/shared/.../util/RenderUtil.java` (`createTextTexture` tcoords). Verified: jME3 `Quad` UV
  winding confirmed from engine source and matched against the corrected ordering; live `bin/devtest`
  gold-rush (`-Dscenario=gr`) board rendered with the fix in place (the dev `-mx128M` autoplay heap
  OOMs before a claim counter could be deterministically framed, so the conclusive proof is the
  source-level winding match plus the identical-convention icon path rendering upright live).
- **[SUPERSEDED — root-caused & fixed]** The mounted/horse big-shot and shotgun-unit model-render
  defects below were the same **posed skinning reference-space** bug — see the Phase-8 "POSED
  contortion — the *real* root cause" note in the 6b section above (model-root joint must be
  identity to match the fork's root-as-identity model frame). Fixed in `ModelConverter`; coherent
  posed renders verified offscreen for shotgunner/gunslinger/tactician, with new posed goldens.
  The original triage text is kept below for history.
- **Mounted/horse big-shot character model renders incorrectly.** The horse (mounted unit big-shot)
  model is wrong on the live board — needs a closer look to classify: skinning/anim (SkinningControl
  / AnimComposer mis-bind from the `.j3o` bake), a wrong variant/clone via the Phase-2 `Model` facade
  `createInstance`/`resolveTextures` path, or a bad mesh in the `model.dat`→`.j3o` conversion. Start
  by loading the offending unit through `ModelCache` in the `RenderToPng` harness and comparing to the
  fork. Likely lives in the model-pipeline cluster (app `Model` facade + `tools/j3o-converter`
  `ModelToJ3o`), not in board rendering. Capture which specific unit (e.g. a frontier_town/indian_post
  mounted big-shot) when reproducing.
- **Shotgun unit sprite renders incorrectly** (observed live, 2026-06-13). The "shotgun dude" unit
  (the shotgunner) sprite is wrong on the board. Same model-pipeline-vs-sprite classification needed
  as the horse item — diagnose via `RenderToPng` on that unit's model through `ModelCache` and compare
  to the fork; check the unit's `model.dat`→`.j3o` bake (mesh/skin/anim) and its `UnitSprite`/
  `ActiveSprite` material/animation binding. Probably shares a root cause with the horse big-shot.

  **Phase-5 harness finding (2026-06-14, narrows both the shotgun and horse items above):** the
  shotgun-dude and the mounted big-shots (`units/frontier_town/cavalry`, `units/indian_post/buffalo_rider`)
  reproduce **deterministically** in `RenderModelToPng` (`-Panim=<clip>`). Textures resolve correctly
  and the right clips list — but the **posed skinned armature is badly contorted**; static props render
  fine. So the root cause is in the **skinning / joint-bind path** (`SkinningControl`/`Armature` +
  `AnimComposer`, or the `model.dat`→`.j3o` skin bake), **not** texture resolution or the `Model`-facade
  clone. Next step: inspect the baked `.j3o`'s `Armature`/skin weights vs. the fork rig (compare a
  rest-pose `-Ptime=0` render to a posed frame; check bone bind matrices / weight assignment in
  `ModelConverter`/`ModelToJ3o`).

  **FIXED (2026-06-14, Phase 6b).** Root cause was the skin bake in
  `tools/j3o-converter/.../ModelConverter.java` — three compounding bugs, all confirmed by the
  rest-pose (`-Ptime=0`) render being contorted (so: bind-matrix bugs, not anim tracks):
  1. **Inverse-bind matrices were never computed.** The armature called only
     `Armature.saveInitialPose()`, which sets the animation *reset* pose but does **not** fill
     `Joint.inverseModelBindMatrix` — those defaulted to identity, so jME3's skinning offset
     (`jointModel * inverseBind`) collapsed to the raw joint model transform and shattered the rig.
     Now we call `armature.update(); armature.saveBindPose(); armature.saveInitialPose()` so the
     inverse-bind matrices are `inverse(jointModelBind)` taken at the reference pose.
  2. **Intermediate (non-bone) node transforms were dropped.** The armature parented each bone
     under its *nearest bone ancestor* using the bone's *local* transform, but the fork accumulates
     a bone's model transform through the **whole** ancestor chain (`ModelNode.updateWorldVectors`),
     including non-bone nodes. We now build a `Joint` for every node in the bones' full ancestor
     closure with the true parent links, so joint model transforms equal the fork node model
     transforms.
  3. **Skin vertices were in the wrong space.** The fork stores skin vertices in mesh-local space
     and frames bone matrices by the mesh's inverse model-ref transform; jME3 skins in the
     armature/model (root) frame. We now bake the skin vertices into model space
     (`vertex' = inverse(skin._invRefTransform) * vertex`) and reparent the skinned `Geometry` to
     the model root with an identity transform, so the geometry's own node-chain transform is not
     re-applied on top of the already-model-space skinning.

  Files changed: `tools/j3o-converter/.../jme3/model/ModelConverter.java` (the three fixes above),
  `tools/j3o-converter/.../jme/tools/model/ModelMeshAccess.java` (expose `SkinMesh._invRefTransform`),
  and the two parity verifiers `ModelToJ3o.java` / `VerifyCorpus.java` (match skinned geometry by
  content rather than scene-graph path, since the reparenting deliberately changes the path).
  Verified: full-corpus parity now 310/310 PASS (was 283 PASS / 27 FAIL — all 27 were the skinned
  models). Rest + posed (`standing`, `walking_cycle`, `shooting`) renders of
  `units/frontier_town/shotgunner`, `units/frontier_town/cavalry`, and
  `units/indian_post/buffalo_rider` all show coherent, correctly-deformed armatures (cowboy /
  mounted cowboy / buffalo + rider) instead of the previous contortion. Repro:
  `./gradlew :assets:compileModels --rerun-tasks` then
  `./gradlew :tools:j3o-converter:renderModelToPng -PmodelType=units/frontier_town/shotgunner -Panim=standing -Ptime=0 -Pout=/tmp/rest.png`.

  **POSED contortion — the *real* root cause (FIXED 2026-06-14, Phase 8).** Phase 6b only fixed the
  **bind** pose: with the fixed *posed* render harness (`Models.poseAnimation` double-ticks so the
  skin actually deforms at time t), every skinned unit still rendered **clean at frame 0 (bind) but
  progressively contorted as a clip advanced** — the error grew with rotation-away-from-bind, was
  identical under forced software skinning, with clean unit-quaternion keyframes. (Bind-only goldens
  never caught it.) The bug was a **reference-space mismatch between the baked skin vertices and the
  joint inverse-bind**, both supposed to live in the *same* model space:
  - jME3 skins as `v' = Σ wⱼ · jointModelⱼ · inverseBindⱼ · v_bind`. For this to equal the fork's
    posed result, the baked vertices, `jointModel`, and `inverseBind` must all be in **one** model
    frame. The fork's model frame is defined by `ModelNode.updateWorldVectors()`, which sets a node's
    model transform to `parentModel · localTRS` **only when the parent is itself a `ModelNode`**;
    for the **topmost ModelNode (the `Model` root)** it loads **IDENTITY**, *dropping that node's own
    local TRS*. So the fork's `Bref`/`Bcur` and the skin's `_invRefTransform` (hence the baked
    model-space vertices) all live in **"root-as-identity"** model space.
  - Phase-6b's `buildArmature()` built a `Joint` for the Model root carrying the Model's **local
    TRS**. jME3 accumulates a joint's model transform from the armature root down, **including** the
    root joint's local transform — so jME3's `jointModel` was `R · …` where `R` = the Model root's
    dropped TRS. The skinning offset `jointModel(t) · inverse(jointModel(bind))` then became
    `R · X(t) · inverse(X_bind) · inverse(R)` — a **conjugation of every deformation by `R`**. At
    bind, `X(t)=X_bind` so the offset is identity *regardless of `R`* (bind always looked fine);
    off-bind, the conjugation by `R` shattered the rig, growing with the joint's rotation.
  - **Fix (`ModelConverter.buildArmature` + `buildClip`):** the model-root skeleton joint (parent not
    a `ModelNode`) now gets an **IDENTITY** local transform, exactly mirroring the fork's root model
    transform. Then jME3 `jointModel` equals the fork node model transform at *every* joint, so
    `jointModel · inverseBind == Bcur · inverse(Bref)` and the baked root-as-identity vertices are
    deformed in the matching frame. If a clip *animates* the root joint, its `TransformTrack` is
    forced to identity for all frames too (the fork's root model transform is identity for the bind
    pose AND every animated frame). Bind-pose output is byte-unchanged (the offset is identity at
    bind no matter what the root joint carries), so the `standing@0` goldens stay green.

  Verified (all offscreen, fixed posed harness): `shotgunner`/`gunslinger`/`tactician` (the umbrella
  big-shot, whose `shooting`/`blocking` clips are **joint-only**) render coherent, recognizable
  figures across `shooting`/`blocking`/`walking_cycle` at t=0.1/0.3/0.4 — upright cowboys/gentleman
  aiming/holding their weapons, articulation matching the fork golden
  `docs/fidelity-reference/attackAndDeathAnimation.png` — where before t=0.3 was a shattered ribbon
  of limbs. `verifyCorpus` stayed **310/310 PASS**; `verifyVisuals` green with the existing bind
  goldens **byte-unchanged** plus **two new POSED regression goldens** added to
  `golden-manifest.txt` (`ft_unit_shotgunner_shooting` = `units/frontier_town/shotgunner shooting
  0.3`; `ft_unit_tactician_shooting` = `units/frontier_town/tactician shooting 0.3`) so this
  contortion class can never again hide behind a bind-only check. Files changed:
  `tools/j3o-converter/.../jme3/model/ModelConverter.java` (root-joint identity rule + root-track
  identity), `golden-manifest.txt` + the two new golden PNGs. **Still needs final live/in-game
  confirmation** (deferred to parent): the offscreen harness loads through the real runtime
  `Model`/`ModelTextureResolver`/`createInstance` path, but in-game `UnitSprite`/`ActiveSprite`
  animation driving on a live board has not been re-checked.
- **WASD camera panning doesn't work at all** (functional, not cosmetic). **[FIXED 2026-06-14]** The
  original diagnosis (`GodViewHandler.registerWith(InputManager)` never called) was **stale**: the
  Phase-3 host flip commit (`8fdb90453`) already calls `_inputHandler.registerWith(inputManager)` in
  `JmeApp.simpleInitApp`, and `GameInputHandler.registerWith` maps W/A/S/D → FORWARD/BACKWARD/LEFT/
  RIGHT and adds the listener. The actual bug was in the **BUI raw-input bridge**: `Jme3RootNode.
  onKeyEvent` called `kie.setConsumed()` on **every** key event unconditionally. jME3 runs raw-input
  listeners before mapping/trigger dispatch, so a consumed event never reaches the `InputManager`
  mappings — BUI was swallowing every keystroke before the camera `AnalogListener` could see it.
  **Fix:** only consume the key event when a BUI component actually has focus (`_focus != null`, i.e.
  text entry / chat); otherwise let it fall through to the camera mappings. File changed:
  `bui/.../Jme3RootNode.java` (`onKeyEvent`). Added a one-shot verification log in `GodViewHandler`
  (`registerWith` + first `onAnalog`). **Verified live** (`bin/devtest`, no `--shot`, held open): drove
  a W keypress burst via python-xlib XTest into the focused client window — the client log emitted
  `GodViewHandler received camera input. [action=forward, ...]`, and before/after screenshots show the
  board pan (camera location moved). A-key likewise pans. Without focus, WASD now reaches the camera;
  with a focused text field, BUI still consumes keys (correct).

### Phase 6z — Dev-loop robustness (surfaced by the Phase-6a live-run NPE)
Two small fixes that came out of the Phase-6a server-bootstrap NPE (see the 6a note above): the NPE
was a worktree-setup artifact (gitignored `etc/test/` missing → blank `server_auth`), made worse by
a silent-null misconfig path. Harden both so a fresh checkout/worktree "just runs".

- **#1 — fail-fast on a misconfigured authenticator. [DONE 2026-06-14]** `ServerConfig.getAuthenticator()`
  (`server/.../ServerConfig.java`) used to return `null` when `server_auth` is blank (or the class
  fails to load), and `BangServer$Module.configure` (`BangServer:116`) does `bind(Authenticator.class)
  .to(null)` → an opaque Guice NPE far from the cause. **Fix:** `getAuthenticator()` now throws an
  `IllegalStateException` up front: on a blank `server_auth`, the message is *"server_auth is not
  configured. Copy etc/*.dist to etc/test/ (see README / docs/running-the-game.md), or set server_auth
  in etc/test/server.properties."*; on a `Class.forName` failure it rethrows
  `"Failed to load authenticator class '<class>' (configured as server_auth)."` with the original
  cause chained (no longer swallowed/logged-and-nulled). The success path is unchanged — a valid
  `server_auth = com.threerings.bang.server.ooo.OOOAuthenticator` binds exactly as before. The throw
  fires during Guice module `configure()`, before any DB contact. File changed:
  `server/src/main/java/com/threerings/bang/server/ServerConfig.java`. Verified: `:server:compileJava`
  clean; live `bin/devtest --no-client` with `server_auth` blanked in `etc/test/server.properties`
  surfaces the clear fail-fast message in the server log instead of the bare Guice NPE, and restoring
  it returns to normal "Server listening" startup.
- **#2 — auto-seed `etc/test/` in `bin/devtest`. [DONE 2026-06-14]** `etc/test/` is gitignored, so
  fresh worktrees (every background agent) had no local config and tripped #1. **Fix:** `bin/devtest`
  gained a `seed_etc_test` function (called after process cleanup, before the MySQL check and the
  `./gradlew deploy` step — deploy bakes `etc/test/` into the jars, so seeding must precede it). For
  each `etc/*.dist` it copies to `etc/test/<name-without-.dist>` only when the target is absent (never
  overwrites), logs each file seeded with a `[devtest] seeded etc/test/<name> from <name>.dist` line,
  and stays quiet when everything is already present. Idempotent / safe to re-run. The comment + a
  closing log line note that the seeded `server.properties` still needs correct `db.*` MySQL
  credentials — seeding removes the *missing-file* failure, it does not provision MySQL. File changed:
  `bin/devtest`. Verified: in a fresh worktree (only `etc/test/README` present), running `bin/devtest`
  seeded `blosxom.conf, build_settings.properties, deployment.properties, server.conf,
  server.properties`; a second run seeded nothing (idempotent); the server then deployed and reached
  "Server listening" (MySQL was up as `bang`/`yeehaw`).

### Phase 7 — Editor + visual regression

Two largely separate efforts: (A) restore `bangeditor` as a usable Swing tool with the jME3 render
embedded in an AWT canvas; (B) a per-town visual-regression harness on the Phase-5 offscreen
renderer. (A) is gated by one keystone unknown, so it's sequenced 7a→7b→7c; (B) = 7d is independent
and can run in parallel.

**Current state (recon 2026-06-14, 7a complete):** the editor *compiles* and starts its in-JVM
`EditorServer` cleanly, then **crashes its jME3 init thread with an NPE** — it does NOT boot to a
usable bare window. `EditorDesktop` calls `app.start()` (default `JmeContext.Type.Display`, a
LWJGL3 window); on the `jME3 Main` thread `JmeApp.simpleInitApp` → `EditorApp.create()` →
`EditorClient.init(this, frame)` with **`frame == null`**, and `EditorClient.init` line 64 does
`_frame.setJMenuBar(...)` → `NullPointerException`. `LegacyApplication.handleError` logs it and the
render loop dies; the JVM stays alive only because `EditorServer`'s non-daemon `presents.Invoker`
keeps it up, so the process lingers with **no window and no render loop** (verified via screenshot:
empty desktop, and a thread dump showing `DestroyJavaVM` waiting, no `jME3 Main`/render thread). The
log line:

```
SEVERE LegacyApplication.handleError: Uncaught exception thrown in Thread[#44,jME3 Main,5,main]
java.lang.NullPointerException: Cannot invoke "...JFrame.setJMenuBar(...)" because "this._frame" is null
  at com.threerings.bang.editor.EditorClient.init(EditorClient.java:64)
  at com.threerings.bang.editor.EditorApp.create(EditorApp.java:43)
  at com.threerings.jme.JmeApp.simpleInitApp(JmeApp.java:176)
```

**Editor UI architecture (key for 7b).** The editor is a **Swing `JFrame` shell wrapping a GL
canvas, with the board view drawn as a BUI window inside that canvas** — exactly the old
gdx-`LwjglCanvas`-in-`JFrame` shape:
- *Swing layer (the `JFrame` content pane):* a `JMenuBar` + a `SOUTH` status panel (`EditorClient`),
  and `EditorPanel`/`ToolPanel`/`BoardInfoPanel` — all `javax.swing.JPanel`s — laid out around the
  GL canvas in the center. These need the AWT `Canvas` from the jME3 context.
- *BUI layer (drawn inside the GL canvas, already Phase-5-ported):* `EditorPanel` builds a BUI
  `BWindow` (`_vwin`) holding the `EditorBoardView` (`extends BoardView`, so it rides the Phase-4
  jME3 render path), adds it via `_ctx.getRootNode().addWindow(_vwin)`, and **sizes it to the AWT
  canvas** via `((EditorApp)ctx.getApp()).getCanvas()` + a `ComponentListener` (`sizeToCanvas`). So
  `EditorApp.getCanvas()` MUST return the real embedded AWT canvas for the BUI view to size itself.

So the missing piece is purely the **embedding seam**: build a jME3 `Canvas` context, put its
`java.awt.Canvas` in the `JFrame` and into `EditorApp.canvas`, set `EditorApp.frame`, and call
`EditorClient.init(app, frame)` with a real frame. Rendering and the BUI tool UI are already done.

**Keystone result — jME3-in-Swing on LWJGL3 WORKS (spike proven 2026-06-14).** A throwaway spike
(`tools/jme3-host/CanvasSpike`, run task `:tools:jme3-host:runCanvasSpike`) renders a spinning cube
into a Swing `JFrame` (Swing tool panel on the left + GL canvas in the center) on the LWJGL3 backend.
Screenshot-confirmed: both the X11 grab of the live `JFrame` and the GL framebuffer screenshot show
the cube + jME3 stats overlay inside the canvas. **The working API is `JmeContext.Type.Canvas`:**

```java
AppSettings settings = new AppSettings(true);
settings.setRenderer(AppSettings.LWJGL_OPENGL2);  // LWJGL3 backend on this branch
app.setSettings(settings);
app.createCanvas();                                       // builds the Canvas context (no GL loop yet)
JmeCanvasContext ctx = (JmeCanvasContext) app.getContext();
java.awt.Canvas canvas = ctx.getCanvas();                // -> add to a Swing JFrame
// ... add `canvas` to the JFrame on the EDT, then:
app.startCanvas();                                        // spins the GL render loop against the canvas
```

On LWJGL3 the canvas context is `com.jme3.system.lwjgl.LwjglCanvas` (in `jme3-lwjgl3`, implements
`com.jme3.system.JmeCanvasContext`), which bridges to `org.lwjgl.opengl.awt.AWTGLCanvas` from
**`org.lwjglx:lwjgl3-awt:0.2.3`**. The earlier recon note ("`lwjgl3awt:0.2.3` on the classpath but
used nowhere") was off on one point: the artifact is `org.lwjglx:lwjgl3-awt:0.2.3` and it was **not**
declared in any gradle file — only cached. 7b must add it. The `AwtPanelsContext`/`AwtPanel` path
also exists (`jme3-desktop`) as a fallback but is not needed; `Type.Canvas` matches the editor's
existing `EditorApp.canvas`/`getCanvas()` field shape directly.

**Gradle gotcha (multi-platform).** `lwjgl3-awt:0.2.3`'s POM declares an lwjgl dependency with an
unresolved `${lwjgl.natives}` native classifier, which breaks resolution. Pull it in with
`transitive = false` — `jme3-lwjgl3` already supplies the matching lwjgl 3.3.x core + the
per-OS natives (Linux/Windows/macOS), so we only need lwjgl3-awt's `org.lwjgl.opengl.awt.*` classes
(platform-specific glue: `PlatformLinuxGLCanvas`/`PlatformWin32GLCanvas`/`PlatformMacOSXGLCanvas` all
ship in the one jar). The canvas path is cross-platform; the spike ran on Linux/X11. **Windows
note:** `Type.Canvas`/`AWTGLCanvas` is the standard cross-platform jME3 embedding and works on
Windows; macOS historically needs the AWT thread to be the main thread (`-XstartOnFirstThread`),
which is the one platform caveat to validate if a Mac build is targeted.

- **7a — Editor recon + AWT-canvas spike (keystone). DONE.** See above: as-is editor crashes on a
  null `JFrame`; the jME3-in-Swing-Canvas-on-LWJGL3 keystone is proven (`Type.Canvas` +
  `org.lwjglx:lwjgl3-awt:0.2.3`, `transitive=false`). Spike at `tools/jme3-host/CanvasSpike.java`.
- **7b — Canvas embedding + Swing chrome. DONE (2026-06-14).** The editor now opens as a real
  windowed Swing tool with the jME3 board view rendering inside an embedded AWT canvas, and resizes
  correctly. **What landed:**
  1. **Dep:** `org.lwjglx:lwjgl3-awt:0.2.3` (`transitive = false`) added to `client/desktop`'s
     runtime classpath (mirrors the spike's declaration). `EditorApp`/`EditorClient`/`EditorPanel`
     resolve in `client/shared`, but the canvas is constructed in `EditorDesktop` (in `client/desktop`)
     so that one module is where the AWT-canvas dep belongs. **Also bumped `jme3-desktop` from
     `runtimeOnly` → `implementation` in `client/desktop`** — `com.jme3.system.JmeCanvasContext` lives
     in jme3-desktop (NOT jme3-core), and `EditorDesktop` references it at *compile* time to grab the
     embedded canvas; without it on the compile classpath the build fails `cannot find symbol`.
  2. **`EditorDesktop.main`** rewritten off `app.start()` onto the Canvas path (matches `CanvasSpike`):
     LWJGL3 `AppSettings` (`setRenderer(AppSettings.LWJGL_OPENGL2)`), `app.createCanvas()`, grab
     `canvas = ((JmeCanvasContext)app.getContext()).getCanvas()`, `app.canvas = canvas`, then build the
     `JFrame` on the EDT via **`SwingUtilities.invokeAndWait`** (canvas in `BorderLayout.CENTER`,
     `setVisible(true)`), `app.frame = frame`, and finally `app.startCanvas()`. **Ordering is the
     keystone:** `invokeAndWait` guarantees both `app.frame` and `app.canvas` are published *and the
     frame is realized/sized* before `startCanvas()` spins the GL loop — `EditorApp.create()` (→
     `EditorClient.init(this, frame)` + `_client.start()`) runs on the jME3 init thread once the loop
     starts and dereferences `app.frame`, so a null there is the original crash this fixes.
  3. **`EditorClient.init(app, app.frame)`** now receives the real (non-null) frame and installs the
     `JMenuBar` + SOUTH status panel (x/y coords) — verified rendered. `JPopupMenu
     .setDefaultLightWeightPopupEnabled(false)` already present (and also set in `EditorDesktop`).
  4. **Swing chrome layout:** canvas CENTER (`EditorDesktop`), menu bar + SOUTH status bar
     (`EditorClient.init`), and the `EditorPanel` chrome (title, `BoardInfoPanel`, `ToolPanel` —
     scenario checkboxes, prop/tool dropdowns) EAST via `EditorContextImpl.setPlaceView` (posted to the
     EDT through `RunQueue.AWT`). `EditorPanel.willEnterPlace`'s `ComponentListener` on
     `EditorApp.getCanvas()` (now non-null) fires `sizeToCanvas` so the BUI `_vwin` board view fills +
     tracks the canvas.
  5. **Verified live** (`bin/bangeditor`, DISPLAY=:1, screenshots): the JFrame shows full Swing chrome
     AND the default empty `BangBoard` terrain grid renders inside the GL canvas (no NPE). Programmatic
     EWMH resize 1252×797 → 1528×1006 confirmed the board view expands to fill the larger canvas and the
     GL viewport reshapes cleanly. `editor.log`/stdout clean — only the benign
     `Cannot find loader com.jme3.scene.plugins.ogre.MeshLoader` WARNING (no ogre assets used); logon +
     `Entering game data.BangObject(editor)` succeed.
  **EDT/render-thread note (the flagged risk, resolved):** the Swing mutations inside
  `EditorClient.init` (`setJMenuBar`, add SOUTH panel) run on the jME3 init thread (the fork did the
  same), but they are one-shot setup-time mutations before the frame is interacted with, so no EDT
  violation surfaced. The per-frame BUI root traversal stays on the render thread (`JmeApp.BuiProcessor`
  on the GUI viewport); the only cross-thread post is `setPlaceView`, already routed to the EDT via
  `RunQueue.AWT`. No threading defects observed across launch + resize.
- **7c — Editor interaction. DONE (2026-06-14).** The editor is now *usable*: picking, the core
  tools, the environment dialogs, and board save/load all respond to canvas input, verified live on
  `bin/bangeditor` (DISPLAY=:1) driven by python-xlib XTest with before/after screenshots Read back.

  **Picking — the crux, and it needed NO coordinate fix (the 7b wiring already handles the origin).**
  The key finding: in Canvas mode editor mouse input flows entirely through **BUI**, not raw AWT.
  jME3's `LwjglCanvas` feeds the `InputManager` mouse events that are **already canvas-relative and
  bottom-left-origin** (y up); `Jme3RootNode.onMouseMotionEvent/onMouseButtonEvent` forward
  `mme.getX()/getY()` verbatim into BUI `MouseEvent`s (no flip — BUI is also bottom-left origin, as the
  node's own comment notes). The editor tools (`EditorTool` subclasses) receive those BUI events via
  `ToolPanel.EventDispatcher` attached to `EditorBoardView` (`view.addListener(_dispatcher)`), and the
  picking ray is built in `BoardView.getGroundIntersect(e,…)` from `camera.getWorldCoordinates(new
  Vector2f(e.getX(), e.getY()), 0)`. Because (i) BUI hands over canvas-relative bottom-left pixels and
  (ii) the camera's viewport is sized to the canvas, `getWorldCoordinates` already consumes exactly the
  coords it expects — **no top-left↔bottom-left or frame↔canvas-offset correction was required.**
  The EAST/SOUTH chrome offset never enters the math because the canvas (CENTER) has its own GL
  context/viewport with a (0,0)-origin at the canvas corner, not the frame corner. Verified by the
  SOUTH status bar's live `x:,y:` readout: at the four canvas corners the tile coords map correctly and
  consistently (x grows left→right, y grows bottom→top on a 40×40 default board — TL≈(7,27),
  TR≈(30,27), BL≈(7,12), BR≈(30,12)), and a click lands on the tile under the cursor (a placed piece
  appears exactly where clicked).

  **Tools verified live:** `PiecePlacer` (+`PieceChooser`) — placed a Viewpoint and two Frontier-Town
  cacti, each on the clicked tile; `TerrainBrush` — paint stroke fires `paintTerrain` + alpha-splat
  refresh; `HeightfieldBrush` — "Add Noise" roughened the whole heightfield (visible surface change);
  `CameraDolly` — left-drag orbits/tilts the camera (delta-based, origin-independent) and View→Recenter
  Camera fires. `ViewpointEditor`/`TrackLayer` share the identical BUI-event + `getGroundIntersect`
  seam (same dispatcher, same picking path) so they ride the same verified plumbing.

  **Environment dialogs verified:** `SkyDialog` and `WaterDialog` open as heavyweight `JDialog`s over
  the GL canvas, read the board's current params (Sky showed the live horizon/overhead colors +
  falloff), and **apply to the live board** — raising the Water "Level" slider flooded the board with a
  visible water surface (`EditorBoardView.setWaterParams` → `_wnode.refreshSurface`). `LightDialog`/
  `BoardPropertiesDialog`/`NewBoardDialog`/`EnvironmentDialog` open through the same
  `handleEdit*`→`fromBoard`→`setVisible` path on `_ctx.getFrame()`; NewBoardDialog opened and "Create
  Board" reset the board to a clean grid (no exception). Heavyweight popups are on, so the dialogs paint
  over the canvas correctly.

  **Board save/load round-trip verified — Narya format (Phase 3).** `EditorController.handleSaveBoard`/
  `loadBoard` use `BoardFile.saveTo`/`loadFrom`, which are pure `com.threerings.io.ObjectOutputStream`/
  `ObjectInputStream` (header + `BoardData` graph), NOT the retired fork `Savable`. Drove File→Save to
  `/tmp/roundtrip.board` (78 KB written, status "Saved …"), then File→New→Create Board (board cleared
  to empty grid), then File→Load of the same file — the two cacti, the viewpoint, and the noise terrain
  all came back at their saved positions (status "Loaded …"). Clean round-trip.

  **Place-view swap — fixed a latent magic-index bug.** `EditorContextImpl.setPlaceView`/
  `clearPlaceView` indexed the content pane by component count (`>2` / `>1`), which assumes a specific
  add order; with the canvas in CENTER and the status panel SOUTH, `clearPlaceView`'s `remove(1)` would
  have removed the **SOUTH status bar**, not the EAST place view. Re-pointed both to target the EAST
  slot explicitly via `BorderLayout.getLayoutComponent(pane, EAST)` (setPlaceView removes whatever
  holds EAST then installs the new view; clearPlaceView removes the passed-in view only when it is the
  EAST component). The editor has a single place so the bug never fired in practice, but the fix is
  order-independent. Re-launched after the change: full chrome intact, status bar still live.

  **Files changed:** `client/shared/.../editor/EditorClient.java` (the place-view EAST-slot fix). No
  other product code needed changing — picking/tools/dialogs/save-load were already correctly wired
  for the canvas by 7b; 7c is mostly a *verification* phase plus that one hardening fix.

  **EDT/render-thread:** no threading defects observed. Tool mouse/key handling runs on the render
  thread via the BUI dispatcher (same as in-game BUI); Swing menu/dialog actions run on the EDT (the
  controller posts via `RunQueue.AWT`). The only stray log line during water apply was a benign jME3
  bounding-volume debug hint ("Problem spatial name: Root Node"), not an error.

  **Deferred / minor (for a 7c-follow-up, not blocking):** (a) the `TerrainNode.Cursor` brush ring for
  Terrain/Heightfield brushes does not render visibly (the edit still applies; this is a Phase-4/5
  cursor-geometry rendering gap, not a picking issue); (b) the `TerrainSelector` combobox in the Terrain
  Brush options renders with near-zero height (a Swing layout quirk in the tool-options scrollpane —
  painting still works against the default terrain); (c) `View→Recenter Camera` resets only the camera
  location, not its orbit/tilt (pre-existing `CameraDolly.recenter` behavior). Depends on 7b's
  embedding (done).
- **7d — Per-town visual regression (independent; parallelizable). DONE (2026-06-14).** A
  deterministic, CI-ready **golden-image** regression suite built on the Phase-5/6 offscreen harness
  (`RenderModelToPng`/`RenderSceneToPng`/`RenderParticleToPng`) + the `SnapshotDiff` metric. Does
  **not** need the editor or a live server — it renders baked `.j3o` headlessly on the isolated
  `renderToPngRuntime` classpath (jme3 + app only, no fork/gdx, no `org.lwjgl` sealing clash).

  **What landed:**
  - **A golden manifest** — `tools/j3o-converter/src/test/resources/visual/golden-manifest.txt` —
    declaring 20 deterministic renders (pipe-separated `name | kind | tolerance | args`; kinds
    `model`/`prop`/`scene`/`particle`), **10 per town**: per town 3 units (posed `standing@0`), 2
    buildings, 2 natural props, 1 composed scene (2 units + a building), and 2 particle effects.
    (frontier_town: shotgunner/cavalry/gunslinger, saloon/bank, tree_oak/cactus1, a town scene,
    explosion+dust. indian_post: dogsoldier/revolutionary/stormcaller, saloon/sheriff,
    pine/totem, a town scene, fireflies+rain.)
  - **The committed goldens** — `tools/j3o-converter/src/test/resources/visual/golden/*.png` (20
    PNGs, 800×600). They live under `src/test/resources` because `baseline/` is **gitignored** and
    would not persist.
  - **A driver** `VerifyVisuals` + two Gradle tasks. The gate:
    `./gradlew :tools:j3o-converter:verifyVisuals` re-renders each manifest entry (each in a forked
    JVM, the same main that captured it) and diffs against its golden, exiting **non-zero** on any
    regression over the per-entry tolerance or a missing golden. To seed/refresh:
    `./gradlew :tools:j3o-converter:captureGoldens` (then commit the PNGs). Both need the baked
    staging tree — run `./gradlew :assets:processResources` first (stages all 310 `model.j3o` + 60
    `particles.j3o` coherently; piecemeal bake sub-tasks can leave one set un-staged).
  - **Demonstrated:** PASS on the committed goldens (exit 0; models/props/scenes bit-exact
    `meanDiff=0.000000`); a deliberately perturbed render FAILS with exit 1
    (`meanDiff=0.052 -> FAIL`). `:tools:j3o-converter:verifyCorpus` still 310/310.

  **Determinism finding (drove the tolerance design):** model/prop/scene renders are **bit-exact**
  across runs (tight tolerance 0.005, mostly to absorb driver float noise). jME3 `ParticleEmitter`
  uses an **unseeded RNG**, so *active* emitters vary run-to-run by ~0.01 mean diff (explosion
  ~0.010, dust ~0.0006) — these get a loose tolerance (0.030) that still catches gross regressions
  (effect missing / broken material / wrong framing); *steady* ambient emitters are effectively
  deterministic (fireflies ~0.00004, kept tight at 0.005).

  **Seeding follow-up (post-review):** the unseeded-RNG nondeterminism above is a flaky-gate risk, so
  `RenderParticleToPng` now **seeds** `FastMath.rand` to a fixed value before warmup — particle
  renders are reproducible run-to-run. ⚠️ **The committed particle goldens (`*_fx_*.png`) are stale**
  (captured pre-seed) and will not match the seeded render; they must be re-captured
  (`captureGoldens`) before `verifyVisuals` is wired into CI, after which the loose 0.030 particle
  tolerances can be tightened. Tracked in **Phase 8 cleanup** below.

  **Baseline-alignment finding (fork-before):** `baseline/fork-before/` holds **6 live game
  screenshots**, not harness-reproducible references — `login.png` (1024×768, UI only),
  `editor-frontier-town.png` (1252×797, editor chrome), `p3-{game-1,play-1,play-2}.png`
  (4480×1440, ultra-wide live gameplay), `water.png` (1018×786, live board). None is 800×600, none
  is an isolated-model framing, and all carry arbitrary live framing/lighting/UI, so direct
  `SnapshotDiff` against them is **not apples-to-apples** (it fails immediately on the size
  mismatch). The golden suite is the real deliverable; the fork-before shots remain useful only as
  human eyeball references.

  **Deferred:** full live-board per-town regression (`bin/devtest --shot`) — feasible but out of
  scope here; full-board offscreen rendering would drag client/shared onto the render classpath
  (re-introducing the LWJGL2/`org.lwjgl` sealing clash), so the model/scene/golden approach is
  preferred.

### Phase 8 — Cleanup
- **Fork: keep as build-time-only (Option A — decided 2026-06-14).** Do NOT delete the `jme` module
  in Epic 1. Recon confirmed no runtime/main code references the fork — but it's still the build-time
  asset compiler (`.mxml` → `model.dat` via the fork `CompileModelTask`, then `model.dat` → `.j3o`
  via `ModelConverter`'s use of the fork `BinaryImporter`). So instead of deleting it: **confirm the
  fork is walled off from every runtime/main classpath** (it is — only `app:modeltoolImplementation`
  + `tools/j3o-converter` reference it, both build-time) and leave it as the isolated build-time
  model/particle compiler. Full eradication (a native `.mxml`→jME3 importer) is deferred to
  **[Epic 4](epic4-native-model-toolchain.md)**.
- **Bare `gdx` dropped — DONE (2026-06-14).** libGDX (`com.badlogicgames.gdx:gdx:1.5.4`) was kept
  through the cutover *only* as a GL-free source of keyboard keycode constants
  (`com.badlogic.gdx.Input.Keys.*`) for BUI. It is now gone from every runtime/main classpath; the
  BUI keycode space is **jME3 `com.jme3.input.KeyInput.KEY_*` end-to-end**.
  - **Keycode-VALUE consistency (the crux, not a symbol sed):** `Jme3RootNode` previously received
    jME3 `KeyInput` codes from the `InputManager` and *translated* them to gdx `Input.Keys` codes via
    a 256-entry `KEYMAP` table (gdx and jME3 use **different integer values** for the same physical
    key), then the rest of BUI compared `event.getKeyCode()` against gdx `Keys.*`. That whole
    translation layer was **removed** rather than re-symboled: `Jme3RootNode.onKeyEvent` now forwards
    the raw jME3 `KeyInput` code unchanged (`toBuiKeyCode`/`KEYMAP`/the `m()` helper deleted), and
    every consumer compares against `KeyInput.KEY_*`. Producer and consumer now share one constant
    space, so a value mismatch is impossible by construction.
  - **Files migrated `Keys.*` → `KeyInput.KEY_*`:** bui `Jme3RootNode` (translation removed;
    `KEY_MODIFIER_MAP` retyped to `KEY_LSHIFT/RSHIFT/LCONTROL/RCONTROL/LMENU/RMENU`), `BComponent`
    (`KEY_TAB`), `event/KeyEvent` (numpad char hack → `KEY_NUMPAD0..9`), `text/DefaultKeyMap` (text
    editing map); client `StatusView`, `FKeyPopups` (F-key popups), `bui/IconPalette` (arrow nav),
    `BangClient` (`KEY_ESCAPE`), `game/client/BangController` (in-game hotkeys), editor `CameraDolly`,
    `PiecePlacer`, `ViewpointEditor`. **Dead gdx-canvas class `bui/CanvasRootNode` deleted** (the
    abandoned gdx `LwjglCanvas`-editor root node; nothing instantiates it — the editor uses
    `Jme3RootNode` since Phase 7b).
  - **Non-obvious name differences applied:** gdx `Keys.NUM_0..9` (top-row digits) → jME3 `KEY_0..9`;
    `Keys.NUMPAD_N` → `KEY_NUMPADN`; `Keys.ENTER` → `KEY_RETURN`; `Keys.BACKSPACE` → `KEY_BACK`;
    `Keys.FORWARD_DEL` → `KEY_DELETE`; `Keys.PAGE_UP/PAGE_DOWN` → `KEY_PRIOR/KEY_NEXT`;
    `Keys.SHIFT_LEFT/CONTROL_LEFT/ALT_LEFT` → `KEY_LSHIFT/KEY_LCONTROL/KEY_LMENU` (and the right-hand
    variants); `Keys.LEFT_BRACKET/RIGHT_BRACKET` → `KEY_LBRACKET/KEY_RBRACKET`. Letters, F-keys,
    arrows, `ESCAPE/TAB/SPACE/HOME/END/INSERT` and most punctuation are same-name. (Edge: numpad-Enter
    used to be folded onto `Keys.ENTER` by the table; with the table gone it now flows as its own
    `KEY_NUMPADENTER` code — `DefaultKeyMap` gained an explicit `KEY_NUMPADENTER → ACTION` mapping so
    text-field submit on numpad-Enter is preserved.)
  - **Dependency:** removed `api "com.badlogicgames.gdx:gdx"` from `bui/build.gradle` (the only
    runtime declaration). The `jme` fork module keeps its own `gdx`/LWJGL2 deps (`jme/build.gradle`,
    untouched per Option A) — it is build-time-only (model/particle compile) and walled off every
    runtime/main classpath. Proof: `./gradlew :client:desktop:dependencies --configuration
    runtimeClasspath | grep -i gdx` → empty. Builds green: `:bui:`/`:client:shared:`/`:client:desktop:`
    /`:app:`/`:server:`/`:jme:`/`:tools:j3o-converter: compileJava` all SUCCESSFUL. (The unrelated,
    pre-existing `:tools:board-converter:compileJava` break — stale fork `com.jme.util.export.*`
    refs in a run-once-then-delete converter — predates this work and is out of scope.)
- Any lingering LWJGL2 bits: still owned by the (retained) `jme` fork module only; not on a runtime
  classpath.
- **Revert the dev mute:** `BangPrefs.SILENT` is muted-by-default on this branch (`-Dsound=true` to
  hear) — restore to audible-by-default before ship. **DONE (Phase 8):** flipped to audible by
  default with an opt-in mute — `BangPrefs.SILENT = Boolean.getBoolean("mute")` (pass `-Dmute=true`
  to silence; `getMusicVolume`/`getEffectsVolume` still return 0 when SILENT, non-persisting). Class
  doc + `bin/devtest` updated (`--sound` → `--mute`, default now audible). The flag is documented in
  CLAUDE.md (Build/run "dev flags") and docs/running-the-game.md (audio gotcha).
- Remove the now-vestigial jME3 `Savable` impls left on the board/config data classes (they stream
  via Narya now; the Savable side is unused). **DONE (Phase 8):** verified unused first — the
  classes implement `com.jme3.export.Savable`, but the only would-be consumers use the *fork*
  `com.jme.util.export.binary.BinaryImporter`/`Savable` (a different interface): `BoardConverter`
  reads legacy `.game` via the fork importer (and no longer even compiles — the fork is build-time-
  only/off the classpath, Option A), and the `BountyGameSaveUnitTest` likewise referenced the dead
  fork API and didn't compile. Runtime board/config/piece persistence is pure Narya `Streamable`
  (`BoardData.toBytes`/`fromBytes`); jME3 `com.jme3.export.binary.BinaryExporter` is used only in
  `tools/j3o-converter` against scene-graph `Spatial`s, never these data types. Removed the
  `com.jme3.export.Savable` impl (interface + `read`/`write`/`getClassTag` + imports) from the whole
  connected hierarchy — `BangBoard`, `BangConfig` (incl. nested `Round`/`Player`), `Criterion`,
  `Piece` and its overriding subclasses `Prop`/`Marker`/`Viewpoint`/`Track` (which called
  `super.read/write`), and `IntStatCriterion`/`RankCriterion`. Deleted the dead
  `BountyGameSaveUnitTest`. `:client:shared:compileJava`/`compileTestJava` green.
- Remove the Phase-6b WASD-verification log lines in `app/.../camera/GodViewHandler.java` (the
  one-shot `registerWith` + first-`onAnalog` logging added to prove the input fix — harmless but
  host-code noise once the fix is trusted). **DONE (Phase 8):** removed the `registerWith` "mappings
  registered" log, the first-`onAnalog` "received camera input" log + its `_loggedFirstAnalog`
  guard, and the now-unused `com.threerings.jme.Log.log` import. Input wiring (mappings + listener)
  left intact; `:app:compileJava` green.
- **Verify the Phase-6a particle emission-mode mapping in a *live* render** (not just the warmed
  snapshot). **DONE (Phase 8, 2026-06-14) — this was a real defect; fixed.** The live Epic-1
  acceptance smoke showed a **giant blown-out white/orange particle bloom** swallowing a large area
  of the screen and *persisting* (not firing and fading); the rest of the game rendered fine. The
  offscreen single-frame render hid it because `RenderParticleToPng` never fired the spawn-path
  burst.
  - **Root cause (confirmed):** `ParticleConverter` baked, for every non-`controlFlow` fork effect,
    `setParticlesPerSec(count / mean-lifetime)` — turning the baked `.j3o` prototype into a
    *continuous* emitter. That is right for genuinely ambient effects but **wrong for one-shot
    bursts** (explosion/impact/death-puff): the runtime spawn path
    (`ParticleEffectViz.displayEffect` → `ParticlePool.getParticles` → `ParticleUtil.forceRespawn`
    → `emitAllParticles`, with a `ParticleUtil.ParticleRemover` that detaches the effect once it is
    idle) *itself* fires the single burst, so a baked positive rate makes the emitter emit forever,
    the remover's `getParticlesPerSec() > 0` guard never trips, and a dense additive burst
    accumulates into the runaway bloom (e.g. `frontier_town/explosion` emitter[1]: 75 particles over
    a ~0.44 s mean life → ~170 particles/sec *forever*).
  - **Burst-vs-ambient signal:** the fork **controller's repeat type** (`ParticleController` /
    `Controller.RT_*`, read via `geom.getParticleController().getRepeatType()`). `RT_CLAMP` = a dead
    particle stays dead and the controller goes inactive once all particles have died → a single
    burst that fades; `RT_WRAP`/`RT_CYCLE` recreate particles forever → a continuous/ambient
    population. A corpus dump confirmed the split matches intent: CLAMP = explosion,
    barrel_explosion, hit_flash, mushroom_cloud, resurrection, fireworks_explosion, hero_death,
    storm_caller attacks, teleporter activates, etc.; WRAP = dust, fire, fireflies, rain, ambient_*,
    sea_spray, teleporter ambient, dust_trail, etc. (`controlFlow` is *not* the signal — many CLAMP
    bursts have `controlFlow=true` with a `releaseRate`, but they must still fire once and stop.)
  - **Fix (in `ParticleConverter.convertEmitter`):** classify on repeat type. **One-shot (CLAMP)** →
    `setParticlesPerSec(0)` + `setNumParticles(count)`; the spawn code's `emitAllParticles()` fires
    the single pool-sized burst, it fades over its lifetime, and the `ParticleRemover` detaches it.
    **Ambient (WRAP/CYCLE)** → keep a sane continuous rate: the fork `releaseRate` when `controlFlow`
    is set, else a refill at `count/mean-life`, **clamped** to `count/mean-life` so even an ambient
    effect can't blow out. No spawn-path change was needed — the lifecycle was already correct once
    the prototype stopped carrying a runaway rate. (Verified Wreck/Repair/HealHero use the
    code-built pooled emitters with their own explicit rates, not the baked `.j3o` prototypes, so
    they were unaffected.)
  - **Also fixed the offscreen harness** (`RenderParticleToPng`) to mirror the live spawn path: it
    now fires `emitAllParticles()` on every emitter before stepping, so a one-shot effect actually
    renders its burst (and then fades) offscreen instead of showing an empty cloud — this is the
    thing whose absence let the defect ship.
  - **Verified — live + offscreen.** Offscreen: `frontier_town/explosion` renders a contained
    fireball at frame ~6 and is **fully faded by frame 90** (fire-and-fade); `boom_town/barrel_explosion`
    same; ambient `fire`/`dust`/`fireflies` still emit continuously at frame 40 (continuous emission
    not killed). **Live** (`bin/devtest --mute --no-deploy --shot` + a sequence of `bin/shot` grabs
    over ~60 s of an autoplay game): the board, units, fences and HUD render cleanly with **no
    runaway bloom**; combat bursts (muzzle flashes / explosions) appear *contained and localized* and
    *differ frame-to-frame* (they fire and fade, they do not accumulate or persist). `verifyVisuals`
    20/20 PASS, `verifyCorpus` 310/310.
- **Re-capture the stale Phase-7d particle goldens.** `RenderParticleToPng` now seeds the particle
  RNG (`FastMath.rand`), but the committed `golden/*_fx_*.png` were captured pre-seed and no longer
  match. Run `./gradlew :tools:j3o-converter:captureGoldens`, commit the refreshed PNGs, then tighten
  the loose 0.030 particle tolerances in `golden-manifest.txt` now that the renders are deterministic
  — do this before `verifyVisuals` is wired into CI (Epic 3 Phase 3). **DONE (Phase 8):** ran
  `:assets:processResources` then `:tools:j3o-converter:captureGoldens` — all 4 `*_fx_*.png`
  (`ft_fx_explosion`, `ft_fx_dust`, `ip_fx_fireflies`, `ip_fx_rain`) re-captured against the seeded
  RNG and committed. Tightened the three loose `0.030` particle tolerances
  (`ft_fx_explosion`/`ft_fx_dust`/`ip_fx_rain`) to `0.005`, matching the model/prop/scene tolerance
  (and the already-tight `ip_fx_fireflies`). `:tools:j3o-converter:verifyVisuals` is green — all 20
  goldens PASS with `meanDiff=0.000000` (the seeded particle renders are bit-exact), and
  `:tools:j3o-converter:verifyCorpus` is still 310/310 parity. Updated the manifest header comment
  (dropped the STALE-goldens warning). **Re-refreshed (2026-06-14)** after the emission-mode fix
  above: the harness now fires `emitAllParticles()` and one-shot effects bake at 0 particles/sec, so
  the `ft_fx_explosion` golden was moved to **frame 8** (mid-burst) — at the old frame 40 the
  one-shot has fully faded and would golden an empty frame — and all `*_fx_*.png` re-captured; both
  gates green again (20/20 PASS, 310/310).
- **Add a `bin/shot` window-capture helper + a screenshot convention.** Live verification grabs the
  whole `DISPLAY=:1` (`PIL ImageGrab`), which captures the entire multi-monitor desktop — leaking
  whatever else is on screen (Discord, the other monitor) into shared/committed images, and needing a
  lossy AI-crop afterward (see the one-off `baseline/fork-before/*-cropped` salvage). Fix it at
  capture time: a small `bin/shot <out.png>` that finds the Bang window and grabs **only that
  window's pixmap** (python-xlib `get_image`, or `xwd`/`import -window <id>`) — pixel-exact, no crop,
  no desktop. Match the window **precisely**: exact title `Bang Editor`, or title starting with
  `Bang! Howdy`, **and** WM_CLASS = the jME/Java app — and explicitly exclude browsers/terminals (a
  loose `'bang! howdy' in title` substring match grabbed a Firefox tab titled with the repo
  description during dev — don't repeat that). Then point `bin/devtest --shot` at it instead of the
  full-display grab, and add the convention to CLAUDE.md / docs/running-the-game.md: **screenshots
  capture only the Bang window, never a full-display grab** (privacy + relevance). Prefer the
  offscreen render harness where a live window isn't needed (it's already window-only/deterministic).
  **DONE (Phase 8):** added `bin/shot <out.png> [DISPLAY]` — grabs only the window pixmap via
  python-xlib `window.get_image` (PNG encoded inline), with an ImageMagick `import -window <id>`
  fallback. Match logic: title `== "Bang Editor"` OR `startswith("Bang! Howdy")`, AND WM_CLASS not on
  a browser/terminal/editor denylist (firefox/chrome/edge/discord/wezterm/code/...), preferring a
  WM_CLASS that positively looks like the jME/Java/LWJGL app (java/jvm/lwjgl/glfw/bang/...). Pointed
  `bin/devtest --shot` at `bin/shot` (replacing the full-display `PIL ImageGrab`). Verified live:
  launched `bin/bangeditor`, ran `bin/shot /tmp/shot-test.png` → a 1224x731 PNG that is exactly the
  editor window (menu bar, "Bang! Editor" header, board/scenario panel, grid, camera tools) — no
  desktop, no other windows; the WM_CLASS was `com-threerings-bang-editor-EditorDesktop` and the
  browsers/terminals on the same `:1` were correctly excluded. Convention documented in CLAUDE.md +
  docs/running-the-game.md.
- Update CLAUDE.md + docs/engine-notes.md for the new jME3 / LWJGL3 stack.
- **Epic-1 live-smoke render defects — offscreen triage (2026-06-14).** Three defects flagged on the
  live board (ref shot `baseline/jme3-phase8/p8-render-defects-tactician-gunslinger-counters.png`,
  not committed to this worktree) triaged entirely offscreen via `RenderModelToPng` + a standalone
  `createTextTexture` AWT check (live runs deferred — the human was using the single-bind server).
  - **Harness bug found + fixed first (it masked the other two).** `Models.poseAnimation`
    (`tools/j3o-converter/.../tools/j3o/Models.java`) ran a **single** `updateLogicalState(0f)` to
    apply a posed clip. But the bake attaches the `SkinningControl` **before** the `AnimComposer`, so
    in one tick the skin deforms from the *previous* (reset) armature pose and only *then* does the
    composer write the time-t joint transforms — leaving the rendered skin **frozen at the bind pose**
    for any clip whose tracks are joint-only (no rigid spatial tracks). Symptom: the umbrella
    tactician rendered byte-identical at `standing@0`, `shooting@mid` and `dying@end` (md5-equal),
    while the gunslinger *appeared* to animate only because its 4 rigid pistol **spatial** tracks
    moved. Fix: pin the time and tick **twice** so the `SkinningControl` consumes the posed armature.
    `verifyVisuals` stays 20/20 (every golden is `standing 0`, where both ticks reproduce the bind
    pose, so goldens are byte-unchanged) and `verifyCorpus` 310/310.
  - **Defect 3 — claim/gold counter numbers illegible: OFFSCREEN-CLEAN (likely already fixed).**
    Distinct from the Phase-6b billboard *orientation* fix. Replicated `RenderUtil.createTextTexture`'s
    exact AWT image-gen for the counter path (`Old Town` bold 72, gray fill + dark-gray 1px outline,
    the colours `CounterSprite`/`GenericCounterNode` pass) and rendered "12": **legible, upright,
    good-contrast digits** (`width=48 height=59 → 64²` POT texture, correct `tcoords` U→X / V→Y from
    the Phase-6b winding fix). Counter colours (`BangMetrics.JPIECE_COLORS` + `DARKER_COLORS`) are
    high-contrast. No offscreen defect reproduced — the Phase-6b `createTextTexture` tcoords fix
    appears to also be what the smoke needed. **Needs live confirmation** on a real gold-rush board
    (a deterministic in-board counter frame couldn't be captured headlessly).
  - **Defects 1 & 2 — tactician umbrella + gunslinger: a single posed-skin issue, NEEDS LIVE.** With
    the harness fix, **`standing@0` and the very first keyframe of every clip render cleanly** for the
    gunslinger, shotgunner, *and* tactician (coherent armatures; gunslinger's holstered/drawn pistols
    all correct; tactician's umbrella base joint `bone umb base` — a root-level single-bone weight,
    which is only positioned *by animation*, its bind pose far from the body — tracks correctly to the
    right hand, `umb.model≈Rhand.model`). **But every model contorts as a clip advances past frame 0**
    (e.g. shotgunner `shooting`: frame 0 clean, frame 1 clean, frame 2 the right arm + right leg
    stretch to spikes; the umbrella likewise distorts off the hand). The distortion **grows with
    rotation-magnitude-from-bind** and is identical under forced **software** skinning, so it is in the
    **armature bind / inverse-bind math**, not HW skinning, not the keyframes: every track keyframe is
    a clean unit quaternion with scale 1, smoothly varying; the posed joint **model** transforms are
    sane (not exploded); no intermediate joint is missing a track. The signature (identity offset at
    bind, error growing with rotation) points at the Phase-6b skin bake — most likely a mismatch
    between the per-mesh reference used to bake skin vertices into model space
    (`inverse(skin._invRefTransform)`, fix #3) and the joint **inverse-bind** captured by
    `armature.saveBindPose()` (fix #1) in `ModelConverter` — i.e. the two are consistent only at the
    bind pose. **This was not caught earlier because the old single-tick harness froze the skin at
    bind and every golden/`verifyCorpus` case is bind/`standing@0`.** It affects all skinned units, so
    in-game it would surface most on big, rigid-looking props like the tactician umbrella (defect 1)
    and on wide-swing clips. **Deferred to live:** confirm against the live board / the fork that the
    posed-skin contortion is the real in-game tactician+gunslinger defect, then fix the bind/
    inverse-bind reference mismatch in `ModelConverter` (validate with the now-correct posed harness:
    `renderModelToPng -Panim=shooting -Ptime=0.1` on shotgunner/gunslinger/tactician must stay
    coherent), keeping `verifyCorpus`/`verifyVisuals` green and adding a **posed** (non-`standing@0`)
    golden so it can't regress unseen. No `ModelConverter` change was shipped — fixing it unvalidated
    (no fork reference render available headless, live off-limits) risked regressing the 310/310 bake.

### Phase 8 — Epic-1 final live verification (2026-06-14)

Final close-out punch-list verified against the fork golden corpus
(`docs/fidelity-reference/video-catalog.md` clips 1-19 + `docs/game-reference/`). Offscreen
pre-checks via `:tools:j3o-converter` render harnesses; live via `bin/devtest --mute` +
`bin/shot` window-only captures, frames cropped/upscaled with `ffmpeg` for analysis (board is
zoomed-out at autoplay scale, so transient effects are at the resolution floor — cropping was
needed to read them).

**Offscreen pre-checks (gates):**
- `verifyVisuals` **22/22 PASS**, `verifyCorpus` **310/310 PASS** (deployed bake good).
- **Umbrella `blocking` (marquee) — PASS.** `tactician -Panim=blocking` at t=0.0/0.15/0.3/0.5:
  canopy clearly **inflates from a closed tube → partial fan → fully open disc held overhead**
  (the rib-bone skinMesh `bone umb base`+`Bone01..33` skins correctly through the clip).
  Matches `TacticianBlocksArtillery` (clip 11) spec. PNG sizes grow monotonically with the
  open canopy. The defect the catalog flagged as a likely bug **does not reproduce** on the
  JRECutover tip.
- **Posed-skin-in-motion — PASS (prior deferred defect resolved).** The Phase-8 offscreen
  triage above deferred-to-live a "contortion past frame 0" (arms/legs spike, umbrella
  distorts) suspected in the `ModelConverter` bind/inverse-bind math. On the current tip it is
  **fixed**: shotgunner `shooting` at t=0.1/0.3/0.6/0.9 (deep into the clip, high
  rotation-from-bind) is fully coherent — no spiked limbs; gunslinger `shooting`, cavalry
  mounted `shooting` (rider sits correctly on the horse), shotgunner `dying` all coherent.

**Live game (`bin/devtest --mute`, frontier_town autoplay, multiple full rounds):**

| # | Item | Result | Evidence (vs corpus) |
|---|------|--------|----------------------|
| 1 | Skinning in motion (units animate coherently) | **PASS** | Live units render/animate cleanly through the match; corroborated by the offscreen t=0.6/0.9 coherence above. vs `Tactician*`/`*Gunslinger`/`Cavalry*`. |
| 2 | Particles: explosion warm flash, **neutral (not gang-tinted)**, **no runaway bloom**; steam plume continuous | **PASS** | Caught a warm orange **explosion flash** mid-kill (`zr_s7_3`, `zd_kk5_3`) — localized, warm/neutral, **not** gang-tinted, **no bloom/accumulation** anywhere across ~80 captures (Phase-8 bloom fix holds). Steam-unit `SmokePlumeEmission` renders **continuous** (curling plume on the wheeled steam units, `z_live_17`). No `SEVERE`/particle errors in `devtest-client.log`. vs `ArtilleryKillShotgunner` (4), `*SteamGunman` (3/10). |
| 3 | Movement dust under moving units | **PARTIAL (code-correct, faint at board scale)** | `detail_level` defaults to **HIGH** (`BangPrefs`), so `MobileSprite.createDustManager` does **not** early-return — the emitter is active and wired (`ParticleEmitter` "dust", per-frame recolour from terrain). Not crisply isolable at autoplay zoom in stills; the Phase-4-deferred emitter tuning (lifetimes/velocity-cone) is the only outstanding piece. Not a gap — see follow-up. vs `DustAnimation` (12). |
| 4 | Counters legible+upright; damage numbers gang-tinted; skull death marker gang-tinted | **PASS** (counters+damage); **PASS-by-code** (skull) | Blue/gold **gold-stash counters ("2","3","5")** render **upright + legible + gang-tinted** (the Phase-6b billboard fix holds; `zr_s7_2`, `zs_s2_*`). **Damage numbers tinted by target gang colour** caught live: blue **"20"** and yellow glyphs (`zr_s7_2/3`). Skull death-marker: its gang-tint path is verified in `IconViz.didInit` (`getJPieceColor(owner)`); the marker itself is too brief/off-center to freeze at board zoom. vs `CavalryPicksupGoldKillsSteamGunman` (5), `ArtilleryKillShotgunner` (4). |
| 5 | Death drop (killed unit leaves a gold nugget) | **PASS** | `NuggetEffect ... dropping=true` on every kill in the log; dropped gold nuggets visible on tiles (`zs_s2_1`, lower-left gold pile). Killed units leave **prone dead bodies** then a nugget. |
| 6 | Camera: scroll-wheel rotate, zoom, WASD pan | **PASS-by-code** (wheel plumbing verified end-to-end) | The BUI wheel→jME3 path is **intact**: `Jme3RootNode.onMouseMotionEvent` reads `MouseMotionEvent.getDeltaWheel()` and dispatches a `MOUSE_WHEELED` BUI event → `GameInputHandler._swingListener.mouseWheeled` → `swingCamera(±PI/2)`. WASD pan is the prior 6b `GodViewHandler` fix. Headless autoplay generates no real wheel/key input, so not exercised by injected events this run; the wiring is present and correct. vs `ScrollwheelRotatesCamera` (17), `StartGameAndZoomOut` (18). |

**Small fix applied:** `bin/bangclient` `CLIENT_MEMORY` **128M → 768M**. 128M was the 2004-era
LWJGL2/gdx default; under the jME3/LWJGL3 scene graph the autoplay client **OOM'd**
(`OutOfMemoryError`) after ~2 rounds at 128M and again at 512M. At 768M a full multi-round
match runs to the post-game standings screen with **0 OOM**. Low-risk dev-launch tuning only
(production Getdown sets its own heap; this is the dev script).

**Follow-ups (not hacked under time pressure):**
- **Client heap headroom / possible slow growth.** 768M is comfortable for a full game, but the
  client OOM'd at both 128M and 512M under sustained autoplay (`respawnUnits=true`). Worth a
  focused check that footprint is *stable* (high-but-bounded) and not a slow sprite/particle
  leak — instrument with `-verbose:gc` over a long autoplay. Files: `bin/bangclient`,
  `MobileSprite`/`ParticlePool` sprite lifecycle. Likely just endurance, not a defect.
- **Movement-dust emitter tuning (Phase-4 deferred).** `MobileSprite.createDustManager` is wired
  and active at HIGH detail but the ms-lifetime/velocity-cone/spin port is approximate; tune
  against the `DustAnimation` golden (small/soft/ground-hugging/terrain-coloured). In-scope-small
  but needs the golden side-by-side. File: `client/shared/.../game/client/sprite/MobileSprite.java`.
- **Skull-and-crossbones death marker — needs a deterministic capture.** Code path
  (`IconViz` gang-tint) is correct; visually confirm the rising marker tint live with a tighter
  capture (tail the log for `removed by effect` and burst-shoot at higher zoom, or add an
  offscreen `IconViz` render). Not a known defect — verification gap only.

## Acceptance
Client + editor run on jME3 3.9 / LWJGL3 with gameplay and per-town visuals preserved
(the Phase 3 acceptance bar from UPGRADE_PLAN, re-verified on the new engine).
