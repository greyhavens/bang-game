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
`PARSED 310 models, 202 animations, 0 failures`. 2b-i, the `.j3o` writer + parity harness, is in
flight. The rest of this section is the upfront design for 2b-ii, written 2026-07-05 so execution
can proceed without re-deriving it.)*

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
  offset transform is non-identity, per-animation `{frameRate}`, plus whatever the 2b-i gap list
  surfaces as actually-baked-into-`.j3o` (SLOT: fill from the 2b-i report when it lands).
- **Verification for 2b-ii (before any cutover):** (1) **round-trip identity** — a
  `GltfToModelDef` reader (exact mirror of the writer) must reproduce a `ModelDef` whose
  `MxmlToJ3o` output matches the mxml-path output across all 310 (this is cheap once 2b-i's
  parity harness exists: same harness, different front end); (2) glTF-Validator clean on all 310;
  (3) **Blender smoke** — headless `blender --background --python` script importing the spot set
  and asserting mesh/bone/animation counts (no human in the loop; the human edit test comes in
  Phase 4).

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
- **2b-i gap list** (SLOT — fill when the in-flight `.j3o` writer report lands): whatever
  CompileModelTask semantics the writer had to special-case get an explicit `extras` mapping +
  a line here.

## Acceptance
A full `./gradlew deploy` compiles every **model from committed glTF source** — and every particle
effect from its own fork-free source (glTF carries no particle-system semantics, so particles land
as re-authored committed jME3 emitters or a native `particles.jme` reader per Phase 2a; Blender
editability applies to models, not particles) — to runtime `.j3o` with **no `jme` fork module in
the build at all**; the output matches the fork-wrapper oracle (render + structural); a model
edited in Blender and re-exported flows through the build and renders in-game; and the game +
editor run unchanged.
