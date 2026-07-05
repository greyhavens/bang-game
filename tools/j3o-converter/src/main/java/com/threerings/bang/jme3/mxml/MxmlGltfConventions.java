//
// $Id$

package com.threerings.bang.jme3.mxml;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * The ONE place for the Epic 4 Phase 2b-ii <code>.mxml</code>&harr;glTF conventions: the axis
 * convention constant + its exact inverse, and every <code>extras</code> / marker key name the
 * writer ({@link MxmlToGltf}) and the mirror reader ({@link GltfToModelDef}) share.
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
