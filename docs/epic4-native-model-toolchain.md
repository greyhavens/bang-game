# Epic 4 — Native model toolchain (fork eradication)

Epic 1 moved the **running game** entirely off the vendored 2005-era jME fork (the client/editor
ship and load zero fork code — confirmed: no `com.jme.*`/fork-`com.jmex.*` imports in any runtime/main
source). But the fork is **not gone** — it survives as the **build-time asset compiler**. This epic
replaces it with a jME3-native toolchain so the `jme` module can be deleted outright.

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

### Phase 2 — Native importer (scope set by Phase 1)
Build the `.mxml`→jME3 importer: either adapt the Ogre loader (if Phase 1 is promising) or write a
dedicated reader. Cover geometry, textures/materials (the existing `ModelTextureResolver` resolution
can be reused), `SkinningControl`/`Armature` + bind pose (mind the 6b skinning lessons —
inverse-bind matrices, full ancestor-chain joint transforms, model-space skin verts), and the
multi-file `*.mxml` animation clips → `AnimClip`/`AnimComposer`. Also a native `particles.jme` path
(or re-author particles as committed jME3 emitters, superseding the 6a fork bake).

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

### Phase 4 — Delete the fork
Once the native path passes Phase 3 across all 310 models (+ particles): drop `CompileModelTask` +
the fork-coupled bridge, remove the `app` `modeltool` source set's fork dep and `tools/j3o-converter`'s
fork coupling, and **delete the `jme` module** from `settings.gradle`. Update CLAUDE.md/engine-notes.

## Acceptance
A full `./gradlew deploy` compiles every model/particle from `.mxml`/source to runtime `.j3o` with
**no `jme` fork module in the build at all**, the native output matches the fork-wrapper oracle
(render + structural), and the game + editor run unchanged.
