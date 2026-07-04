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
With Phase 3 proving the reader correct, run the one-time migration:
- Convert all 310 models (`.mxml` → glTF) and commit the glTF as the **new canonical source**;
  the build pipeline becomes `glTF → .j3o` via jME3's own `GltfLoader` + our `.j3o` bake — the
  Phase-2a `.mxml` reader retires along with the format it reads.
- **Round-trip gate:** for every model, `glTF → .j3o` must match the Phase-3 oracle renders the
  same way the direct `.mxml → .j3o` path did (Blender-agnostic check), and a spot set must
  survive an actual Blender open → re-export → bake → render-diff cycle (the real "editable"
  proof — Blender's importer/exporter normalization must not silently mangle skins or clips).
- Document the authoring workflow (docs/blender-authoring.md): scale/axis/material conventions,
  how animations map to `model.properties` actions, how to add a NEW model entirely in Blender.

### Phase 5 — Delete the fork
Drop `CompileModelTask` + the fork-coupled bridge, remove the `app` `modeltool` source set's fork
dep and `tools/j3o-converter`'s fork coupling, and **delete the `jme` module** from
`settings.gradle`. Delete the retired `.mxml`/`.dat` machinery. Update CLAUDE.md/engine-notes.

## Acceptance
A full `./gradlew deploy` compiles every **model from committed glTF source** — and every particle
effect from its own fork-free source (glTF carries no particle-system semantics, so particles land
as re-authored committed jME3 emitters or a native `particles.jme` reader per Phase 2a; Blender
editability applies to models, not particles) — to runtime `.j3o` with **no `jme` fork module in
the build at all**; the output matches the fork-wrapper oracle (render + structural); a model
edited in Blender and re-exported flows through the build and renders in-game; and the game +
editor run unchanged.
