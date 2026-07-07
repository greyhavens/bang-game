# Blender authoring workflow (Epic 4 — glTF model source)

How to edit the committed models (`assets/rsrc/**/model.gltf` + `model.bin`) in Blender and get
them back into the game. The corpus imports cleanly on **Blender 4.5 LTS** (Khronos glTF I/O
v4.5.51; full-corpus headless smoke — all 310 models, zero warnings) and is **validated on
Blender 5.1** (tested 5.1.2, glTF I/O v5.1.20): the toolchain pads `_MXMLTEXCOORD` to VEC3 —
(u, v, 0) — so every custom vertex attribute shares one width, because Blender 5.1.0–5.1.2's
glTF importer crashes on spec-valid files with mixed-width custom attributes (upstream bug in
`io_scene_gltf2` `do_primitives`, list-vs-set misalignment; the corpus now sidesteps it).

The 5.1 validation covers: **all 310 models import** headless with zero warnings and zero count
drift vs the 4.5 baseline; an **export-settings API audit** confirmed the settings block (the
TL;DR export call below) is valid verbatim on 5.1 — every kwarg exists with the same
name/default/enum items as 4.5, so nothing there changed; the
**re-export preservation matrix** shows no regressions vs 4.5 (`_MXMLNORMAL`/`_MXMLTEXCOORD` and
all extras preserved value-exact); and **rebake proofs** against the render reference (at
validation time still the pre-cutover fork-wrapper oracle; see step 4 for what the reference is
today): a 5.1 Blender
re-export of the static **saloon** bakes to **meanDiff 0.000000** (re-collapsing to structural
parity), and — after the rest-extras fix — so does the animated-rigid **caboose** on both
animation frames (0.0070/0.0108 → 0.000000; locomotive likewise). See "Rest TRS of animated
nodes" below for what that fix changed.

## What you can edit today

- **Rigid/static models (props, buildings, extras): fully editable.** The whole
  import → edit → export → rebake → verify loop works now.
- **Skinned models (units): NOT yet.** Blender re-parents skinned meshes onto synthetic
  `<name>.001` nodes and rebases vertices into armature space; the reader rejects these with a
  clear error naming the node. Support is scoped (re-merge `.001` nodes ~½ day, armature-space
  rebase ~1 day) but not built — edit unit geometry via the toolchain until it is.
- **The 11 one-vertex emitter props: never.** Blender's exporter cannot write loose points, so
  the emitter mesh is silently dropped on export (the reader detects this and says so):
  `props/frontier_town/natural/{fire, dust_plume, dust_plume_small, sea_spray}`,
  `props/indian_post/natural/{ambient_drizzle, ambient_leaves, ambient_snow, falling_leaves,
  fireflies, magic_fire, mystical_ground}`. Edit their tiny `model.gltf` JSON by hand instead.

## TL;DR workflow

1. Open Blender (4.2 LTS or newer), **File → Import → glTF 2.0**, pick the model's
   `model.gltf`. Import defaults are correct. **Do not change the scene frame rate** (leave 24;
   see below).
2. Edit. Respect the don'ts below.
3. Export with **exactly** these settings (Python form; the UI checkboxes have the same names):

   ```python
   bpy.ops.export_scene.gltf(
       filepath=..., export_format='GLTF_SEPARATE',   # separate .gltf+.bin — NOT GLB
       export_extras=True,                    # the mxml metadata valve — REQUIRED
       export_attributes=True,                # _MXMLNORMAL/_MXMLTEXCOORD custom attrs — REQUIRED
       export_yup=True,                       # default
       export_all_influences=True,            # REQUIRED for skinned units (corpus uses 2 sets)
       export_animations=True, export_animation_mode='ACTIONS',
       export_force_sampling=False,           # REQUIRED: no keyframe resampling
       export_optimize_animation_size=False,  # REQUIRED: no STEP-collapse of animation curves
       export_optimize_animation_keep_anim_armature=True,
       export_optimize_animation_keep_anim_object=True,
       export_skins=True, export_materials='EXPORT',
       export_normals=True, export_texcoords=True,
       export_keep_originals=False,
   )
   ```

4. Rebake + verify **against the current game state**:

   ```sh
   ./gradlew :tools:j3o-converter:mxmlGltfParity \
       -Pmodels=<type/path/of/model> -PgltfRoot=<dir containing <type>/model.gltf> -PrenderOnly
   ```

   **What the comparison reference is (post-cutover):** the harness diffs your re-export's bake
   against the staged `model.j3o` in `assets/build/staging/rsrc` — and since the Phase-4 glTF
   cutover that staging tree is itself baked from the **committed glTF** (refresh it with
   `./gradlew deploy` before verifying). So the diff answers "how does my edit change the model
   **vs the game as it currently ships**" — which is exactly the question an edit needs
   answered. You are NOT comparing against the old fork pipeline: the historical fork-wrapper
   oracle's job ended with the migration sign-off (its reference renders are banked off-repo,
   per the Phase-4 cutover record), and the permanent `verifyVisuals` goldens are likewise
   renders of the current glTF-baked state.

   For an **edited** model the gate is the **render diff** (≤ 0.005 mean vs that current-state
   render — for a deliberate visual edit, a diff is the point; eyeball the rendered PNGs the
   harness leaves in `tools/j3o-converter/build/mxml-gltf-parity/png/` instead of chasing the
   number): `-PrenderOnly` reports structural differences informationally without failing on
   them, because Blender re-splits vertices on export (e.g. saloon 669→703) so counts can
   legitimately change. (For *unedited* rigid models the bake's vertex dedup usually
   re-collapses the split and structural parity passes anyway — a nice extra signal, not the
   gate.) When satisfied, copy the re-exported `model.gltf`+`model.bin` over the committed
   ones and commit; CI re-validates with the Khronos validator.

## Reader constraints (the exporter settings above satisfy all of these)

- glTF Separate or embedded-base64 — not GLB.
- Indexed TRIANGLES primitives; **one material per mesh** (Blender splits multi-material
  meshes into multiple primitives, which the reader rejects — keep meshes single-material).
- Sampled LINEAR animation only (no CUBICSPLINE interpolation).
- Animation fps is derived from keyframe spacing when the `frameRate` extras are missing
  (Blender drops them on import) — verified exact on all 7 shotgunner clips including the
  3 fps `standing`. Corollary: **never change the scene fps** between import and export;
  Blender rescales keyframe times by the fps ratio and corrupts every clip.

## What survives a Blender round trip (and what doesn't)

Survives exactly (verified value-exact on the sensitive set):
- All `extras` metadata — `mxmlType`, `parent`, `solid`, `transparent`, `texture`,
  `hasTcoords`, offset transforms, the `restTranslation`/`restRotation`/`restScale` of animated
  nodes (see "Rest TRS of animated nodes" below), the `mxmlWrapper` root marker. These are the
  **authoritative** render flags and hierarchy data; the glTF material (`doubleSided`,
  `alphaMode`) is just a viewer approximation of them. Don't edit extras unless you mean it.
- `_MXMLNORMAL` and `_MXMLTEXCOORD` (the raw mxml normals and UVs, bit-exact per vertex) —
  with `export_attributes=True`. The standard `NORMAL`/`TEXCOORD_0` are viewer-facing copies
  (normalised, resp. v-flipped to glTF's top-left UV origin — textures display right-side-up
  in Blender); the bake keys on the raw attributes.
- Animation names, counts, and (with the settings above) keyframe times/values to within
  float ULP; translation/scale channels bit-exact.
- Empty `triMesh` nodes (they carry merge semantics in extras despite having no mesh).
- Texture PNG bytes (md5-identical) and basename URIs.

Lost or changed no matter the settings — the reader/bake compensates, but know about them:
- **Animation `frameRate`/`frameCount` extras**: dropped by Blender's importer (reader derives
  them, see above).
- **Rest TRS of animated nodes**: Blender's importer overwrites an animated node's rest
  transform with an animation-pose evaluation (it sits at frame 0 in the viewport). This is
  harmless for a viewer and for **skinned** models (the bake recomputes skinning from the
  armature), but it is **not** harmless for animated **rigid** models: the mesh-merge pipeline
  keys its merge and baked-relative-transform decisions on the AUTHORED rest pose, so a clobbered
  rest silently broke their rebake (caboose 0.0070/0.0108 against a 0.005 tolerance; the five
  train models are the at-risk class — a pre-existing round-trip data loss, worse on 4.5 at
  0.0446). Fixed by pinning the authored rest into the node extras
  (`restTranslation`/`restRotation`/`restScale`) for every animation-targeted node — Blender
  preserves extras value-exact, and the reader prefers them over the clobbered node TRS.
  Consequence for editing: an animated node's rest is now carried in extras and is **not**
  Blender-editable (moving such a node in the viewport only changes its display TRS, which the
  reader ignores in favour of the rest extras). **Edit animations through the toolchain, not by
  posing animated nodes in Blender.** Static nodes are unaffected — they carry no rest extras and
  stay fully Blender-editable through their node TRS.
- **Duplicate node names**: Blender renames to `name.001`. Controllers bind to emitter and
  attachment nodes **by name** — don't rename those nodes, and don't introduce name
  collisions that force Blender to.
- `asset.extras` provenance notes are replaced by Blender's generator string. Harmless.

## Other don'ts

- Don't rename emitter/attachment nodes — controllers in `model.properties` bind by name.
- Don't retime/resample animations unless you mean it; clip timing feeds gameplay.
- Don't "fix" bone rolls/tails: glTF joints are bare nodes, Blender invents tails and rolls
  for display. The armature may LOOK odd while animating correctly; re-authoring rolls breaks
  re-export.
- Don't normalize or recalculate normals expecting it to stick: the bake reads
  `_MXMLNORMAL` (raw), not the display `NORMAL`. To intentionally change shading normals you
  must change `_MXMLNORMAL` (or regenerate the model through the toolchain).
- **UV edits, by contrast, ARE honored on rebake** (stale-detection): editing UVs in Blender
  rewrites only the display `TEXCOORD_0`, leaving `_MXMLTEXCOORD` stale — the reader spots the
  disagreement (beyond 1e-6 after un-flipping v) and adopts your edited `TEXCOORD_0` wholesale,
  printing a per-mesh note. An unedited mesh keeps its raw `_MXMLTEXCOORD` and rebakes
  bit-exact. Remember `TEXCOORD_0` is v-flipped relative to the mxml values (glTF top-left vs
  game bottom-left origin) — edit UVs visually against the texture and let the reader handle
  the convention; don't hand-copy values between the two attributes.
- `model.properties` stays the game-config source (scale, action sequences, controllers,
  variants) — model edits never touch it, and it is never baked into the glTF.
- Runtime-composited textures (~98 colorized/variant models, e.g. `bugle.png`) intentionally
  have **no** `baseColorTexture` — the mesh shows untextured in Blender because the game
  composites the real texture at runtime (decision 2026-07-06: no baked composites).

## Scale/axis conventions

Geometry is in raw mxml units (a humanoid unit ≈ 130 units tall; `model.properties scale=`
maps to world size at load). Source data is Z-up; the synthetic `mxml-root` wrapper node
carries the −90°-about-X rotation to glTF Y-up, and Blender's importer converts to its own
Z-up on top — models arrive upright. Leave the wrapper node alone.

## Adding a NEW model entirely in Blender

Possible in principle (author under a wrapper-equivalent hierarchy, supply the extras keys,
write a `model.properties`), but not yet exercised end-to-end — do the first one alongside
the toolchain owner and document the recipe here when it exists.
