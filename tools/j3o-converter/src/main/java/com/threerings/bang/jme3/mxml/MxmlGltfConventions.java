//
// $Id$

package com.threerings.bang.jme3.mxml;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * The ONE place for the Epic 4 Phase 2b-ii <code>.mxml</code>&harr;glTF conventions: the axis
 * convention constant + its exact inverse, and every <code>extras</code> / marker key name the
 * writer ({@code MxmlToGltf}) and the mirror reader ({@link GltfToModelDef}) share.
 *
 * <h3>Axis convention</h3>
 * <code>.mxml</code> geometry is authored in 3ds&nbsp;Max <b>Z-up right-handed</b>; glTF mandates
 * <b>Y-up right-handed</b>. The mapping is a single &minus;90&deg; rotation about X
 * ({@link #MXML_TO_GLTF}); its exact inverse is {@link #GLTF_TO_MXML}.
 *
 * <p><b>How it is applied (and why it round-trips bit-exactly).</b> Rather than perturbing every
 * mxml node's TRS by the rotation (whose quaternion has irrational components, so a
 * rotate-then-unrotate would drift at the ULP level and could flip the mesh-merge sweep's
 * 1e-4 epsilon decisions on the sensitive train models), the writer emits a single synthetic
 * <b>wrapper node</b> carrying {@link #MXML_TO_GLTF} and re-parents every mxml root node under it.
 * The mxml node transforms are therefore written <em>verbatim</em>. On import the reader discards
 * the wrapper and takes its children as the roots with their transforms untouched — which is
 * exactly "apply the inverse rotation to the roots", realised losslessly. The rotation only ever
 * multiplies data for a viewer (Blender / jME3 GltfLoader), never the round-tripped mxml values.
 * (Blender's own glTF importer then converts Y-up&rarr;Blender-Z-up, so models arrive upright.)
 *
 * <h3>Scale</h3>
 * Geometry is exported in raw mxml units (shotgunner &asymp; 130 units tall). The
 * {@code model.properties scale=} is game-side config and is deliberately NOT baked into the glTF
 * (it must survive round-trips untouched); the unit convention is recorded in {@code asset.extras}.
 */
public final class MxmlGltfConventions
{
    /** The Z-up (mxml/Max) &rarr; Y-up (glTF) root rotation: &minus;90&deg; about X. Applied to
     * the export wrapper node; see the class doc. */
    public static final Quaternion MXML_TO_GLTF =
        new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

    /** The exact inverse of {@link #MXML_TO_GLTF} (+90&deg; about X). Conceptually applied to the
     * root nodes on import; realised by discarding the wrapper (see the class doc). */
    public static final Quaternion GLTF_TO_MXML =
        new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);

    // ---- node/mesh extras + marker keys (the losslessness valve) ----

    /** Node extras key marking the synthetic axis-convention wrapper root; its value is
     * {@code true}. The reader skips this node and adopts its children as the mxml roots. */
    public static final String EXTRA_WRAPPER = "mxmlWrapper";

    /** Node extras key: the mxml spatial subtype, one of {@link #TYPE_NODE}/{@link #TYPE_TRIMESH}/
     * {@link #TYPE_SKINMESH} (so an empty {@code triMesh} — which carries no glTF mesh — is still
     * reconstructed as a {@code TriMeshDef}, not a plain node, preserving merge semantics). */
    public static final String EXTRA_MXML_TYPE = "mxmlType";

    /** Node extras key: the spatial's raw {@code parent} name string (absent for roots). Stored
     * explicitly because spatial names are NOT unique in the corpus (e.g. two meshes both named
     * "trunk"), so inferring the parent from the glTF hierarchy would be ambiguous; the reader
     * reads this verbatim to reproduce {@link ModelDef.SpatialDef#parent}. */
    public static final String EXTRA_PARENT = "parent";

    /** Node extras keys: the AUTHORED rest (bind-pose) TRS of an <b>animated</b> node, pinned so it
     * survives a Blender round-trip. Written by {@code MxmlToGltf} ONLY for nodes some animation
     * targets, and preferred over the node's own TRS by {@link GltfToModelDef} when present.
     *
     * <p><b>Why this exists.</b> Blender's glTF importer does not keep an animated node's authored
     * rest transform: it overwrites the node/bone TRS with an evaluation of the animation pose (so
     * the object sits at its frame-0 pose in the viewport). A plain re-export therefore ships the
     * anim-pose where the rest used to be. That is invisible/harmless for a viewer and for SKINNED
     * models (the bake recomputes skinning from the armature), but the mesh-merge pipeline
     * ({@code ModelDef.createTransformTree}/{@code cullDivergentTransforms}/{@code mergeSpatials})
     * keys its merge and baked-relative-transform decisions on the AUTHORED rest pose of every
     * spatial — so for an animated <em>rigid</em> model (all five frontier_town train models are of
     * this class, e.g. the caboose) a rest clobbered to the anim pose changes those decisions and
     * the rebake drifts off the oracle (caboose was 0.0070/0.0108 vs a 0.005 tolerance; 4.5 was
     * worse at 0.0446 — a pre-existing round-trip data loss, not a 5.1 regression).
     *
     * <p>Blender preserves node {@code extras} value-exact (proven for the {@code offsetTranslation}
     * float arrays), so parking the authored rest here lets the reader recover it after any
     * round-trip. <b>Scoped to animated nodes only</b> on purpose: a static node stays
     * Blender-editable through its node TRS (pinning stale rest extras on it would silently discard a
     * Blender transform edit), while an animated node's rest is not visible or editable in Blender
     * anyway (it shows the anim pose) — so pinning it costs nothing and the authoring workflow edits
     * animations through the toolchain, not by moving an animated node's rest in Blender. */
    public static final String EXTRA_REST_TRANSLATION = "restTranslation";
    public static final String EXTRA_REST_ROTATION = "restRotation";
    public static final String EXTRA_REST_SCALE = "restScale";

    public static final String TYPE_NODE = "node";
    public static final String TYPE_TRIMESH = "triMesh";
    public static final String TYPE_SKINMESH = "skinMesh";

    /** Mesh extras keys: the render flags glTF can only express lossily (they also drive
     * {@code doubleSided}/{@code alphaMode}, but are stored verbatim here so the round-trip is
     * exact regardless of how a viewer normalises the material). */
    public static final String EXTRA_SOLID = "solid";
    public static final String EXTRA_TRANSPARENT = "transparent";

    /** Mesh extras key: the RAW mxml {@code texture} string (before basename reduction). The
     * material's {@code baseColorTexture} is for viewing; this preserves the exact string the
     * mesh-merge {@code canMerge} test compares. */
    public static final String EXTRA_TEXTURE = "texture";

    /** Mesh extras key: whether the mxml mesh had texture coordinates (the per-mesh
     * {@code TriMeshDef.tcoords} flag). Distinguishes an absent tcoord from a (0,0) one. */
    public static final String EXTRA_HAS_TCOORDS = "hasTcoords";

    /** Mesh extras key: the mxml mesh's vertex count (0 for the legitimately-empty triMeshes).
     * Lets the reader detect a mesh whose geometry an authoring tool silently dropped — Blender's
     * exporter cannot write loose points, so a 1-vertex particle-emitter mesh vanishes on export
     * — including the tcoord-less ones the older {@link #EXTRA_HAS_TCOORDS}-based diagnostic is
     * blind to (that check stays as the fallback for files written before this key existed). */
    public static final String EXTRA_VERTEX_COUNT = "vertexCount";

    /** Custom vertex-attribute name carrying the RAW (non-unit) mxml normals: the standard
     * {@code NORMAL} is a normalised copy for viewers, this is what {@code GltfToModelDef} reads
     * back so the bake's normal-keyed vertex dedup is bit-exact. (Blender ignores {@code _}
     * attributes; the validator applies no unit check to them.) */
    public static final String MXML_NORMAL_ATTR = "_MXMLNORMAL";

    /** Custom vertex-attribute name carrying the RAW mxml texture coordinates: the standard
     * {@code TEXCOORD_0} is a viewer-facing copy flipped to glTF's mandated top-left UV origin
     * (u unchanged, v&prime;&nbsp;=&nbsp;1&minus;v — mxml UVs are bottom-left, the game's GL
     * convention), and this is what {@code GltfToModelDef} prefers on read: 1&minus;v is NOT
     * bit-exact-invertible in float, and {@link ModelDef.Vertex} dedup identity keys on
     * location+normal+tcoords, so a lossy flip would corrupt the bake's vertex dedup / mesh merge
     * and break structural parity. When absent (a Blender re-export that lost it, or third-party
     * glTF) the reader falls back to 1&minus;TEXCOORD_0; when both are present but disagree
     * beyond float-flip noise, TEXCOORD_0 was edited in a viewer and wins wholesale (so Blender
     * UV edits take effect on rebake). Same dual-attribute pattern as {@link #MXML_NORMAL_ATTR}.
     *
     * <p>Written as <b>VEC3</b> — (u,&nbsp;v,&nbsp;0) — not the natural VEC2, so every custom
     * attribute in a primitive shares one width/dtype (float32 VEC3, matching
     * {@link #MXML_NORMAL_ATTR}): Blender 5.1.0&ndash;5.1.2's glTF importer
     * ({@code io_scene_gltf2 blender/imp/mesh.py}, {@code do_primitives}) collects
     * custom-attribute data arrays in a list but consumes them by enumerating a set, so
     * mixed-width custom attributes misalign and the import fails with a numpy concatenation
     * ValueError. The reader accepts both VEC2 (historical files / Blender re-exports) and VEC3,
     * using only the first two components. */
    public static final String MXML_TEXCOORD_ATTR = "_MXMLTEXCOORD";

    /** Mesh extras keys: the geometry offset transform, present only when non-identity. */
    public static final String EXTRA_OFFSET_TRANSLATION = "offsetTranslation";
    public static final String EXTRA_OFFSET_ROTATION = "offsetRotation";
    public static final String EXTRA_OFFSET_SCALE = "offsetScale";

    /** Animation extras key: the mxml {@code frameRate}, for exact clip-length round-trip. */
    public static final String EXTRA_FRAME_RATE = "frameRate";

    // ---- asset.extras notes ----

    public static final String ASSET_GENERATOR = "bang-mxml-to-gltf (Epic 4 Phase 2b-ii)";
    public static final String ASSET_UNITS =
        "raw mxml (3ds Max) units; model.properties scale= is NOT baked (game-side config)";
    public static final String ASSET_AXIS =
        "source is Z-up right-handed; a synthetic wrapper root applies -90deg about X to reach " +
        "glTF Y-up. mxml node transforms are stored verbatim under the wrapper.";

    private MxmlGltfConventions () { }
}
