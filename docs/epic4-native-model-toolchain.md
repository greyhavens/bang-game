# Epic 4 — Native model toolchain + Blender authoring (fork eradication)

Epic 1 moved the **running game** entirely off the vendored 2005-era jME fork (the client/editor
ship and load zero fork code — confirmed: no `com.jme.*`/fork-`com.jmex.*` imports in any runtime/main
source). But the fork is **not gone** — it survives as the **build-time asset compiler**. This epic
replaces it with a jME3-native toolchain so the `jme` module can be deleted outright.

**Scope extension (decided 2026-07-05):** the Blender content pipeline is folded into this epic
(not a standalone "Epic 6"), and the requirement is that the **legacy 310 models become editable in
Blender** — not merely replaceable by new content. That decision has one structural consequence:
**glTF 2.0 becomes the canonical committed model source** at the end of this epic. Blender's native
game interchange is glTF and jME3 ships a maintained glTF loader, but there is no path *back* from
an edited Blender model to `.mxml` — so a one-time, oracle-verified `.mxml`→glTF migration replaces
the committed sources, and `.mxml` retires with the fork. Practically this changes little in the
plan below: the same parsed in-memory model that Phase 2 builds from `.mxml` simply feeds **two
writers** (jME3 `.j3o` for the game, glTF for Blender) instead of one.

Out of scope here (tracked as ideas, not phases): Blender as a **board/map** editor (`.board` is a
heightmap+prop format with a dedicated in-game editor — improving that editor likely beats a
Blender add-on; feasibility note only) and 2D/avatar art workflows (render-to-texture conventions,
docs not code).

## Why the fork can't be deleted yet (the build-time coupling)

The committed model **source** is `*.mxml` (mesh + per-animation, e.g. `model.mxml`, `standing.mxml`,
`walking_cycle.mxml`) plus `model.properties` (310 models) — `model.dat` are **not** committed,
they're build intermediates. The pipeline today:

```
.mxml + model.properties
      │  CompileModelTask  (FORK — app `modeltool` source set, com.threerings.jme.tools.*)
      ▼
   model.dat            (fork BinaryExporter Savable graph)
      │  BangModelLoader/ModelConverter  (OUR bridge; READS .dat via the FORK's
      │                                    com.jme.util.export.binary.BinaryImporter)
      ▼  ModelToJ3o → com.jme3.export.binary.BinaryExporter  (jME3's)
   model.j3o            (jME3-native)
      │
      ▼  runtime (ModelCache, stock jME3 — no fork)
```

Two fork-coupled build stages, both run by `./gradlew deploy` / `:assets:compileModels`:
1. **`CompileModelTask`** (the fork's model compiler) turns `.mxml` source → `model.dat`.
2. **`ModelConverter`** (our cutover bridge) calls the **fork's `BinaryImporter`** to read `model.dat`
   into fork `com.threerings.jme.model.*` objects, translates them to jME3 `Mesh`/`Armature`/
   materials, and `ModelToJ3o` writes `.j3o` with **jME3's** `BinaryExporter`.

(Particles are the analogue: `particles.jme` → `ParticleConverter` (fork `BinaryImporter`) →
`ParticleEmitter` `.j3o`.) **Important naming note:** the `BinaryImporter` we *use* is the **fork's
own** class — we did not write it; we authored only the translation layer (`ModelConverter`/
`BangModelLoader`/`ParticleConverter`/`ModelToJ3o`) around it.

So eradicating the fork means replacing **both** stages with a native path that reads `.mxml` (and
`particles.jme`) straight to jME3, with no fork on any classpath.

## The options (Epic-1 chose A)
- **A (Epic 1's choice):** keep the fork as a walled-off **build-time-only** compiler. Runtime clean,
  models stay editable/recompilable. The fork just isn't gone.
- **B:** freeze the models — commit the baked `.j3o`, drop `.mxml`/`.dat` + fork. Fork gone, but the
  editable mesh/anim **source is discarded**. Rejected (loses source).
- **C (this epic):** write a jME3-native `.mxml`→jME3 importer, retire `CompileModelTask` + the fork
  bake, then delete the `jme` module. Keeps editable source **and** removes the fork.

## Phases

### Phase 1 — Probe the jME3 Ogre loader against `.mxml` (do FIRST — could collapse the whole epic)
**Lead/hint:** the live editor logged `Cannot find loader com.jme3.scene.plugins.ogre.MeshLoader`,
and the source meshes are `.mxml`. Three Rings' fork imported Ogre/Maya exports, so `.mxml` is
plausibly **Ogre-XML lineage**. If so, jME3's existing `jme3-plugins` Ogre loaders
(`com.jme3.scene.plugins.ogre.MeshLoader`/`MaterialLoader`/`SkeletonLoader`) may read `.mxml`
natively (or with a thin format shim / rename), turning this epic from "write a whole importer" into
"adapt an existing one." **First task:** add `jme3-plugins`, point an Ogre `MeshLoader` at a couple of
representative `.mxml` (a static prop + a skinned unit like `units/frontier_town/shotgunner`), and see
how close it gets — mesh, skin weights, `Armature`, the per-file `*.mxml` animations. Record the gap.

**Blender corollary to the same probe:** if `.mxml` really is Ogre-XML lineage, also test the
community **Blender Ogre importer add-ons** directly against a couple of `.mxml` — a working import
path into Blender may exist today for free (even a lossy one is useful for scoping Phase 2b's glTF
writer against a visual reference inside Blender).

**PHASE 1 RESULTS (2026-07-05) — Ogre refuted, but the probe found something better.**
Inspected `units/frontier_town/shotgunner/{model,dying}.mxml` directly:

- **`.mxml` is NOT Ogre XML.** It is a custom Three Rings format: root `<model>` containing
  `<skinMesh name= translation= rotation= scale= texture= …>` elements whose children are inline
  triangle-soup `<vertex location= normal= tcoords=>` entries with nested
  `<boneWeight bone= weight=/>`; animations are a separate root `<animation frameRate=30>` of
  `<frame>` × N, each a flat list of per-bone `<transform name= translation= rotation= scale=/>`
  (sampled TRS keyframes). Bone names (`Bip01 …`) are 3ds Max Biped; the format's producer is the
  committed MaxScript exporter **`jme/src/main/ms/TRModelExporter.mcr`** — the authoritative
  format spec from the write side. The jME3 Ogre-loader path and the Blender-Ogre-importer path
  are both **refuted**.
- **The consolation collapses Phase 2a instead:** the fork tree already contains a complete,
  almost-fork-free parser for the format. `tools/xml/ModelParser.java` (109 lines, digester-based)
  and `tools/xml/AnimationParser.java` have **zero** fork imports; the `ModelDef` intermediate
  (1,029 lines) has exactly **9** fork imports, all math/scene types with direct jME3 equivalents
  (`Vector3f`, `Quaternion`, `Matrix4f`, `FastMath`, `BoundingBox/Sphere`, `Node`, `Spatial`,
  `BufferUtils`). Phase 2a is therefore a **mechanical port** of ~1,200 lines to jME3 types, not a
  from-scratch importer. (The `tools/model/*` classes behind `ModelDef` are the fork-typed layer
  the port replaces.)

### Phase 2 — Native importer, one parser / two writers (scope set by Phase 1)
**2a — the reader:** build the `.mxml` parser into a neutral in-memory model: either adapt the Ogre
loader (if Phase 1 is promising) or write a dedicated reader. Cover geometry, textures/materials
(the existing `ModelTextureResolver` resolution can be reused), skinning + bind pose (mind the 6b
skinning lessons — inverse-bind matrices, full ancestor-chain joint transforms, model-space skin
verts), and the multi-file `*.mxml` animation clips. Also a native `particles.jme` path (or
re-author particles as committed jME3 emitters, superseding the 6a fork bake).

**2b — the writers:** from that one parsed representation, emit
- **jME3 `.j3o`** (`Mesh`/`Armature`/`AnimClip` via jME3's `BinaryExporter`) — the game path; and
- **glTF 2.0** — the Blender path. Define the conventions here once: axis/scale mapping, material
  naming, animation-clip naming (must round-trip the `model.properties` action names), texture
  reference style. Blender must open the result with mesh, skin, and every animation intact.

*(2a landed in #44: `com.threerings.bang.jme3.mxml` in `tools/j3o-converter`, full-corpus probe
`PARSED 310 models, 202 animations, 0 failures`. 2b-i, the `.j3o` writer + parity harness, landed
2026-07-05 — results below. The 2b-ii section after it is the upfront design for the glTF writer.)*

#### 2b-i — the `.j3o` writer: RESULTS (2026-07-05, full-corpus parity)
`MxmlToJ3o` (same `mxml` package) bakes `model.properties` + `model.mxml` + animation `.mxml`
straight to `model.j3o` with **no fork code on any classpath**, reproducing both oracle stages:
the fork *compile* (animation-aware mesh merging via the `TransformNode` sweep, Forsyth
vertex-cache optimisation, skin weight-group partitioning, per-mesh property/texture flags,
bounding-box vertex centering, unused-node pruning, bind/reference transforms,
static-vs-transform animation-target classification) and the *fork→jME3 conversion* (a
transliteration of `ModelConverter`: materials via `ModelTextureResolver`, 4-influence skinning
over the full bone-ancestor-closure `Armature` with the identity-pinned root joint,
model-space-baked skin vertices, dual Joint+Node track targets, the `bang.*` user data).

The parity harness `MxmlParity` (`./gradlew :tools:j3o-converter:mxmlParity -PallModels`, or
`-Pmodels=a,b,c`; `-Prender=false` for the fast structural-only pass) bakes each model natively
and compares it to the staged oracle `.j3o` — structurally (geometry count; per-mesh
vertex/triangle counts + diffuse/glow texture paths + blend/cull/bucket/skin-width flags as an
order-insensitive multiset; armature joint-name multiset; clip names/durations/frame/track
counts; `bang.animations`) and by render (forked `RenderModelToPng` JVMs — oracle tree vs. a
native overlay tree whose textures fall through to staging — diffed with the `SnapshotDiff`
metric; animated models posed at frame 0 AND clip midpoint of the alphabetically-first clip).
**Result: 310/310 structural parity and 310/310 render parity, meanDiff ≤ 0.000001 (typically
0.000000, pixel-exact) at tolerance 0.005.**

Three bugs surfaced and fixed on the way — the interesting two are **fork-vs-jME3
float-behavior divergences** (the fork's math classes are jME3's ancestors, but not
bitwise-identical; every other op on the decision path — `Matrix4f.invert`/`mult`,
`Quaternion.mult`, `Matrix4f.mult(Vector3f)`, TRS world composition — was verified identical
by source diff):
- `ModelDef.setTransform` now transliterates the **fork's** `Quaternion.toRotationMatrix`
  float-for-float instead of calling jME3's `setRotationQuaternion`: jME3's version *preserves
  the target matrix's existing scale* (compounding scale when a result matrix is reused, which
  broke the merge sweep outright), and it normalises with `2/norm` where the fork uses
  `invSqrt(norm)` — a ULP-level difference that flipped real epsilon-boundary (1e-4) merge
  decisions on the `extras/frontier_town/train/*` models (both directions: coal/freight
  under-merged, locomotive over-merged, vs. the oracle).
- `ModelDef.toRotationQuat` (used by `TriMeshDef.merge` to rotate merged-in normals)
  transliterates the fork's trace-method `Quaternion.fromRotationMatrix`; jME3's normalises the
  matrix columns first, and on the scaled merge matrices the differing quaternion shaded merged
  normals visibly off (bonuses/ghost_town/plague: meanDiff 0.004 → 0.000 with the fork method).
- `ModelParser`/`AnimationParser` now close their input streams (the #44 review note).

Pre-existing **content** bug found (not fixed — parity preserved): `bonuses/ghost_town/plague`'s
`pistol.png = ../../units/gunslinger/pistol.png` alias resolves to the nonexistent
`bonuses/units/gunslinger/pistol.png`; oracle and native bakes both leave the mesh untextured
(the real texture lives at `units/frontier_town/gunslinger/pistol.png`).

**2b-i gap list — what the native writer deliberately does NOT bake.** Each item verified to
match the *current oracle* bake (none is a Phase 2b regression; "fork-of-2005" behaviors the
oracle already dropped are inherited knowingly):
- **Controllers/emissions are not compiled** (`controllers = …`; 61 models — e.g. steamgunman's
  Gunshot/DudShot/Misfire/SmokePlume emitters, windmill/steamgear `Rotator`s,
  `BillboardController` glows, `TextureTranslator` flame). The *current* oracle compile drops
  every one of them: post-cutover the classes the properties name are jME3 types, so the fork
  compiler's `(ModelController)Class.forName(…)` cast throws and `createController` returns null
  — verified empirically (no controller class names appear in any staged `model.dat`).
  Consequently controller *target* nodes (empty emitter locators) are not marked referenced and
  are pruned from oracle and native alike when nothing else keeps them. Controller
  *declarations* survive in `bang.properties` for the runtime/effects port. The one compile-time
  effect controllers still have — the `controlled` flag restricting which meshes may merge — IS
  honoured.
- **Texture `filter`/`mipmap`/`compress` hints and the `translucent` per-triangle depth-sort
  flag**: parsed but not representable in the baked placeholder material — the oracle
  `ModelConverter` drops them identically. (All of these live in `model.properties`, which stays
  committed — nothing is lost from source.)
- **Animation-expanded skin bounds** (fork `Model.addAnimation` grew each skin mesh's model
  bound across every animation frame): not stored; jME3 recomputes bounds from bind-pose vertex
  data, exactly as the oracle `.j3o` path does.
- **Variants / multi-texture random picks / colorization**: per-instance runtime concerns
  re-resolved from `bang.properties`/`bang.textures` user data (the `ModelCache` path); the
  oracle bakes a single base `.j3o` too.
- **`bound = sphere`** (fork `BoundingSphere` model bounds): only bounding-box centering is
  implemented; no corpus `model.properties` uses `sphere` (grep-verified). Add if content ever
  does.
- **Byte-level (non-semantic) differences from the oracle**: skin vertex floats differ at ~1 ULP
  (the oracle round-trips its buffers through one bind-pose deformation before serialising;
  renders are pixel-identical); armature joint order (oracle: `IdentityHashMap` order — itself
  run-dependent; native: deterministic first-use order), per-clip track order, and geometry
  attach order may differ — compared as multisets per Phase 3's "semantic, not byte-wise" rule.

#### 2b-ii — the glTF writer: designed decisions
**Ground truth this design rests on (verified against shotgunner):** `model.mxml` contains the
full node hierarchy (`<node name= parent=>`, 46 nodes incl. the whole Bip01 skeleton), so bind
pose and armature come straight from `ModelDef`; `model.properties` carries `scale` (e.g. 0.04),
the `animations`/`sequences` lists, repeat types, and all controllers as **game-class config bound
to named nodes** (`emitter_L_barrel.class = …GunshotEmission`). Controllers therefore do NOT need
glTF representation — `model.properties` remains a separate committed source; glTF only has to
**preserve node names**, which it does natively.

- **Output layout:** `model.gltf` (JSON) + `model.bin` sibling to today's sources, textures
  referenced *relatively* to the existing PNGs (untouched). JSON form (not GLB) on purpose:
  diffable in review, and the PNGs stay shared with the avatar/etc. pipelines. Sizes are modest
  (model.dat ≈ 100 KB) — no LFS needed.
- **Library:** default to `de.javagl:jgltf-model` (+`jgltf-model-builder` if helpful; MIT, on
  Central) for buffer/accessor layout; if its API fights, hand-rolling is acceptable (glTF 2.0
  skinned+animated is well-specced) but budget the extra care for accessor min/max, alignment,
  and componentType rules. Validate every emitted file with the Khronos **glTF-Validator** in CI
  for this task (`npx gltf-validator` or the Java port) — non-negotiable, catches layout bugs
  render tests can't.
- **Axes (the big convention):** `.mxml` data is 3ds Max **Z-up right-handed**; glTF mandates
  **Y-up right-handed**. Define ONE constant (`MxmlGltfConventions.MXML_TO_GLTF` = −90° about X)
  applied to root-level nodes on export, with the exact inverse applied on import — never
  per-vertex, never in two places. Blender's own glTF importer converts Y-up→Blender-Z-up, so
  models arrive upright in Blender with no manual rotation.
- **Scale:** export in raw mxml units (shotgunner ≈ 130 units tall). Do NOT bake
  `model.properties scale=` into the glTF — it is game-side config and must survive round-trips
  untouched. Record the unit convention in `asset.extras`.
- **Meshes:** Phase-2a `ModelDef` already dedupes the triangle soup into indexed vertices —
  emit POSITION/NORMAL/TEXCOORD_0 + indices directly. Skinned meshes add JOINTS_0/WEIGHTS_0
  (ubyte/float4). **Probe first: max bone influences per vertex across the corpus** (one-line
  MxmlProbe extension) — glTF caps a vertex at 4 per set; if any model exceeds 4, cap+renormalize
  and record the affected models (render oracle will show whether it matters).
- **Normals — dual, deliberately (the glTF-Validator gate forced this).** The mxml normals are raw
  **non-unit** (a 3ds Max export artifact, magnitudes ~1.8–2.9), and glTF's `NORMAL` MUST be unit
  length (`ACCESSOR_VECTOR3_NON_UNIT` — 7184 errors on the first validator run). But the bake's
  `Vertex` identity keys on the normal, so renormalising before dedup/merge changes vertex counts
  (saloon's wood mesh 669→667) and breaks the round-trip. So emit **both**: a normalised copy as the
  spec-valid, correctly-shaded standard `NORMAL` (what viewers/Blender read — Blender ignores the
  custom attribute; the game shaders normalise anyway), and the raw values as a custom
  `_MXMLNORMAL` accessor that `GltfToModelDef` reads back to reproduce the oracle bake bit-for-bit.
- **Materials:** per-mesh `pbrMetallicRoughness` with `baseColorTexture` = the mxml `texture`
  PNG, metallic 0/roughness 1, plus **`KHR_materials_unlit`** so Blender previews match the
  game's unlit look; `alphaMode=BLEND` iff `transparent`, `doubleSided` iff `!solid`.
- **Skins:** `skin.joints` = the bone nodes referenced by weights plus their ancestors;
  `inverseBindMatrices` computed from the bind-pose node world transforms — reuse the exact math
  the 2b-i `.j3o` writer uses (same source of truth, or they will disagree with each other).
- **Animations:** one glTF `animation` per animation `.mxml`, **named exactly by the action name**
  (file basename — `model.properties` refers to these). Channels: per-joint T/R/S with LINEAR
  samplers; input times = frame/frameRate (dense per-frame keys — lossless; no keyframe reduction
  in v1). Store `frameRate` in the animation's `extras` for exact round-trip.
- **`extras` (the losslessness valve):** anything the fork bake honors that glTF can't express
  natively rides in spec-sanctioned `extras`: per-mesh `{solid, transparent, offset*}` if the
  offset transform is non-identity, per-animation `{frameRate}`. The landed 2b-i gap list
  confirms nothing further is needed: every other dropped/deferred feature (controllers, texture
  filter/mipmap/compress hints, `translucent`, variants, bounds mode) lives in
  `model.properties`, which stays a separately committed source.
- **Verification for 2b-ii (before any cutover):** (1) **round-trip identity** — a
  `GltfToModelDef` reader (exact mirror of the writer) must reproduce a `ModelDef` whose
  `MxmlToJ3o` output matches the mxml-path output across all 310 (this is cheap once 2b-i's
  parity harness exists: same harness, different front end); (2) glTF-Validator clean on all 310;
  (3) **Blender smoke** — headless `blender --background --python` script importing the spot set
  and asserting mesh/bone/animation counts (no human in the loop; the human edit test comes in
  Phase 4).

#### 2b-ii — the glTF writer: RESULTS (2026-07-05, full-corpus round-trip parity)
Landed as `com.threerings.bang.jme3.mxml.{MxmlGltfConventions, MxmlToGltf, GltfToModelDef,
MxmlGltfParity}` in `tools/j3o-converter`, plus the `mxmlGltfParity`/`mxmlToGltf` Gradle tasks.

- **Library: hand-rolled, not jgltf.** The round-trip must be *bit-exact* (the mesh-merge sweep's
  1e-4 epsilon on node transforms is unforgiving — see the 2b-i train-model note), so the writer
  emits its own glTF JSON with **lossless float text** (`Float.toString`, the shortest decimal that
  reparses to the identical `float`) and packs all bulk arrays as float32 LE in `model.bin` (exact
  by construction); the reader parses numbers as **raw token text** and `Float.parseFloat`s them
  (never via `double`, which would double-round node TRS and could flip a merge decision). A
  general glTF library gave no control over either and added a dependency fetch; the format subset
  needed (indexed meshes, multi-set skins, TRS-node hierarchy, LINEAR TRS animations) is small and
  well-specced. The self-check "re-read the file after writing" is `GltfToModelDef` itself (the
  parity harness re-reads every emitted file).
- **Output:** `model.gltf` (JSON, diffable) + `model.bin`, textures referenced *relatively* to the
  existing PNGs (never copied). `pbrMetallicRoughness` (metallic 0/roughness 1) +
  `KHR_materials_unlit`; `alphaMode=BLEND` iff transparent, `doubleSided` iff `!solid`.
- **Axes:** the `-90°`-about-X convention lives in `MxmlGltfConventions.MXML_TO_GLTF` and is applied
  via a single **synthetic wrapper root node** that every mxml root hangs under. The mxml node
  transforms are therefore stored *verbatim*; the reader discards the wrapper (realising the exact
  inverse **losslessly** — a numeric rotate-then-unrotate of the irrational-component quaternion was
  rejected precisely because it could drift the train-model merge decisions). Scale is raw mxml
  units; `model.properties scale=` is deliberately NOT baked (recorded in `asset.extras`).
- **Skinning — the sharp edge:** the corpus goes up to **12 bone influences per vertex** (cavalry;
  probe added), well past glTF's 4-per-set cap. Pre-capping to 4 in the glTF would shrink
  `MxmlToJ3o`'s armature (fewer weight-referenced bones ⇒ fewer joints) and break structural
  parity, so the writer preserves the **full** influence set across `ceil(max/4)` JOINTS_n/WEIGHTS_n
  sets; `MxmlToJ3o` still does the top-4 cap+renormalise itself at bake time, exactly as the oracle.
  `inverseBindMatrices` are computed from the SAME fork-exact bind-pose math `MxmlToJ3o` uses
  (`ModelDef.setTransform`; `IBM = inverse(globalBind_j)·meshToModel`, the wrapper rotation cancels
  so POSITION stays raw mxml-local and the round-trip is exact).
- **Animations:** one glTF animation per animation `.mxml`, named exactly by the action (basename);
  per-joint T/R/S LINEAR samplers, dense per-frame keys (input = frame/frameRate). `frameRate` +
  frame count ride in the animation `extras`. Phantom targets (channels for nodes absent from the
  model) are dropped — harmless, since `MxmlToJ3o.filterTransforms`/`createAnim` skip them anyway.
- **What rides in `extras` (the losslessness valve):** on each spatial's **node** extras (so the 5
  empty `triMesh`es — two in the merge-sensitive train models — keep them despite carrying no glTF
  mesh): the spatial subtype, `solid`/`transparent`, the raw `texture` string (what `canMerge`
  compares), the `hasTcoords` flag, and the geometry offset transform when non-identity; on the
  animation, `frameRate`+frame count; the raw-units/axis notes on `asset.extras`. Everything else
  the bake drops (controllers, filter/mipmap/compress hints, `translucent`, variants, bounds mode)
  stays in the committed `model.properties`.
- **Result: 310/310 structural parity AND 310/310 render parity, meanDiff ≤ 0.000001 (typically
  0.000000, pixel-exact) at tolerance 0.005** — the round-trip `.j3o` matches the oracle exactly as
  the direct `.mxml`→`.j3o` path did. Run `./gradlew :tools:j3o-converter:mxmlGltfParity -PallModels`
  (add `-Prender=false` for the fast structural-only pass).
- **glTF-Validator — now a CI gate (`glTF validity` job in `ci.yml`).** `MxmlToGltf --all` writes
  the whole corpus's glTF, the PNGs are co-located so image URIs resolve, and the official Khronos
  **`gltf-validator`** (`deploy/ci/gltf-validate/`) validates every file, failing the build on any
  Error-severity issue. This is the automated substitute for a viewer opening the file: the
  round-trip parity proves losslessness (our reader mirrors our writer) but is blind to spec
  violations only a validator/Blender rejects — three such (raw texture path, empty-channel
  animation, unencoded URI) were caught by hand during #48 review, which is exactly why this gate
  exists. It cannot run on the dev box (no Node) — the CI runners have it.
- **Blender headless smoke — still a Phase-4 gate.** A `blender --background --python` import test
  (mesh/bone/animation counts, and the real "does it edit + re-export" cycle) needs Blender, which
  isn't on the dev box or the CI runners; it lands with the Phase-4 cutover. (`inverseBindMatrices`
  correctness for *viewing* is likewise unverified until then — but it does not affect the
  round-trip, which re-derives skinning from the reconstructed `ModelDef`.)

### Phase 3 — Verify against the fork-wrapper oracle
The current fork-wrapper `.j3o` are **verified-correct** (310/310 `verifyCorpus` parity + visually
correct after the 6b skinning fix + the game runs on them), so they're a trustworthy reference.
Compare the native importer's output to them — **semantically, not byte-wise** (two correct importers
produce structurally-different-but-equivalent `.j3o`: vertex/index/bone/material ordering, float
precision, and Savable field order all legitimately differ, so a binary diff gives false failures):
- **Render-and-diff (primary):** `RenderModelToPng` both versions and `SnapshotDiff` them (the
  Phase-7d golden harness). Models render bit-exact run-to-run, so tolerances stay tight. Freeze the
  current renders as goldens; the native importer must match.
- **Structural assertions (complement, extend `VerifyCorpus`):** mesh vertex/index counts, `Armature`
  bone hierarchy, `AnimClip` names + durations, bounding boxes, material params.
Keep the fork wrapper as the live oracle **throughout** — retire it only once the native importer
matches across the full corpus. Never a blind swap.

### Phase 4 — glTF becomes the source (the Blender cutover)
With Phase 3 proving the writers correct, run the one-time migration:
- **Design decision (2026-07-05): the build-side glTF reader is OUR `GltfToModelDef`** (the exact
  mirror of the 2b-ii writer) feeding the SAME `MxmlToJ3o` — so an *unedited* migrated model
  produces an identical `.j3o` **by construction**, making migration verification near-trivial.
  jME3's own `GltfLoader` (note: that IS the class name; a review bot once "corrected" it to
  GltfModelLoader — wrong) is used only as a cross-validation in tests, never as the build path:
  we keep total control of conventions and don't inherit its material/armature construction
  choices into the oracle-matched bake.
- Convert all 310 models (`mxmlToGltf` gradle task) and commit the glTF as the **new canonical
  source**; flip `assets:compileModels` input to glTF; delete `.mxml` **in the same PR** that
  flips (git history preserves them; a dual-source window invites drift). `model.properties`
  is untouched throughout — it remains the game-config source (scale, sequences, controllers).
- **Round-trip gate:** (already proven Blender-agnostically in 2b-ii) plus the Blender cycle —
  scripted headless `blender --background --python roundtrip.py` on the spot set: import glTF,
  re-export, rebake, render-diff ≤ 0.005 mean; then ONE human edit test (Tim edits a mesh in
  Blender, re-exports, model renders edited in-game) as the final "actually editable" proof.
- Document the authoring workflow (docs/blender-authoring.md): scale/axis/material conventions,
  how animations map to `model.properties` actions, how to add a NEW model entirely in Blender,
  and the don'ts (don't rename emitter/attachment nodes — controllers bind to them by name;
  don't resample/retime animations unless you mean it).

### Phase 4½ — particles end-state (decision needed from Tim)
glTF carries no particle-system semantics, and fork deletion (Phase 5) needs `particles.jme`
(fork binary) off the build path too. Options:
- **(a) RECOMMENDED — freeze:** run the existing verified `ParticleConverter` bake one final time
  and commit the resulting 60 particle `.j3o` as source artifacts; delete `particles.jme`. Loses
  "editability" that in practice died with the fork's particle editor years ago; effects are
  stable and the render goldens cover the visible ones. Cheap, safe, reversible (re-authoring
  any single effect as a jME3 emitter later is localized work).
- (b) Native fork-binary reader for `particles.jme`: high effort (fork Savable wire format),
  near-zero added value over (a).
- (c) Hand re-author all 60 as jME3 emitter definitions now: regains real editability, medium
  effort, verifiable against the same render goldens — fine as an *incremental follow-up* to (a),
  effect by effect, but shouldn't gate fork deletion.

### Phase 5 — Delete the fork (checklist)
Only after Phase 3 full-corpus sign-off + Phase 4 cutover + a particles decision:
- `settings.gradle`: remove `jme`; delete the `jme/` directory (the MaxScript exporter
  `TRModelExporter.mcr` moves to `docs/legacy/` first — it's the historical format spec).
- `app/build.gradle`: delete the `modeltool` source set + `CompileModelTask` wiring; delete
  `app/src/modeltool/` (its parsers were superseded by #44's ports, its compiler by the writers).
- `assets/build.gradle`: `compileModels` becomes the glTF→`.j3o` bake; remove the `.dat`
  intermediate stage entirely.
- `tools/j3o-converter`: remove fork deps + retire `ModelConverter`/`BangModelLoader`/
  `ParticleConverter`/`ModelToJ3o` (they are the ORACLE generators — retire only after Phase 3
  sign-off, and note the oracle renders themselves are kept as goldens).
- Docs: CLAUDE.md module list + build notes, engine-notes.md pipeline section, epics.md Epic 1/4
  status lines.
- Final verification: full `./gradlew deploy` from clean, corpus parity harness green,
  `verifyVisuals` 22/23 (fireflies pre-existing, #66), bot game PASS, editor boots, client
  screenshot — the same battery every risky change in this repo gets.

### Known risks / probes (pre-answered where possible)
- **>4 bone influences per vertex** (glTF hard cap per attribute set): probe the corpus before
  writing the exporter; cap+renormalize if rare, add JOINTS_1/WEIGHTS_1 only if actually needed.
- **Winding/normals after the −90° X convention rotation**: the render oracle catches handedness
  mistakes immediately — trust it, don't hand-verify.
- **Non-uniform bind-pose scales**: legal in both formats, but inverse-bind-matrix math must be
  the SAME code path as the `.j3o` writer (single Conventions class, no duplicated math).
- **Blender bone-orientation cosmetics**: Blender infers bone tails/rolls on import (glTF joints
  are just nodes) — armatures may LOOK odd while animating correctly; document in
  blender-authoring.md so nobody "fixes" rolls and breaks re-export.
- **Texture path casing** (Linux is case-sensitive; Blender may rewrite relative URIs on
  re-export): round-trip gate asserts URIs unchanged.
- **2b-i gap list** (landed — see the 2b-i results above): the only CompileModelTask semantics
  the writer special-cases are `.mxml`-side (`solid`/`transparent`/offset transforms/frameRate),
  which map to the `extras` bullet above; everything else dropped by the bake lives in the
  still-committed `model.properties`. One genuinely sharp edge for 2b-ii: the mesh-merge sweep's
  1e-4 epsilon decisions require the FORK's exact `Quaternion.toRotationMatrix` float sequence
  (see `ModelDef.setTransform`) — `GltfToModelDef`→`MxmlToJ3o` inherits this for free, but any
  alternative path re-deriving transforms will diverge on the train models.

## Acceptance
A full `./gradlew deploy` compiles every **model from committed glTF source** — and every particle
effect from its own fork-free source (glTF carries no particle-system semantics, so particles land
as re-authored committed jME3 emitters or a native `particles.jme` reader per Phase 2a; Blender
editability applies to models, not particles) — to runtime `.j3o` with **no `jme` fork module in
the build at all**; the output matches the fork-wrapper oracle (render + structural); a model
edited in Blender and re-exported flows through the build and renders in-game; and the game +
editor run unchanged.
