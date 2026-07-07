//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.threerings.bang.jme3.mxml.AnimationDef.FrameDef;
import com.threerings.bang.jme3.mxml.AnimationDef.TransformDef;
import com.threerings.bang.jme3.mxml.ModelDef.BoneWeight;
import com.threerings.bang.jme3.mxml.ModelDef.NodeDef;
import com.threerings.bang.jme3.mxml.ModelDef.SkinMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.SkinVertex;
import com.threerings.bang.jme3.mxml.ModelDef.SpatialDef;
import com.threerings.bang.jme3.mxml.ModelDef.TriMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.Vertex;

/**
 * The Epic 4 glTF &rarr; {@link ModelDef} reader: reads {@code model.gltf}+{@code model.bin} and
 * reconstructs a {@link ModelDef} + {@code Map<String,AnimationDef>} that
 * {@code MxmlToJ3o.bake(props, mdef, animNames, adefs)} turns into a native {@code .j3o}. It
 * supports <b>two producer profiles</b>:
 *
 * <ol>
 *   <li><b>Our own writer {@code MxmlToGltf} — bit-exact round-trip</b> (the Phase 2b-ii
 *       contract, and the Phase-4 build path for unedited models). The reconstruction is
 *       bit-identical (for every field {@link MxmlToJ3o#bake} reads) to what
 *       {@code ModelParser}/{@code AnimationParser} produced, so the bake yields the same native
 *       {@code .j3o} as the direct {@code .mxml} path — verified across the full corpus by
 *       {@code MxmlGltfParity}.</li>
 *   <li><b>Blender re-exports (Khronos glTF Blender I/O, Blender 4.5) — render-parity rebake</b>
 *       (the Phase-4 edit loop: human edits a model in Blender and re-exports). Blender preserves
 *       our node {@code extras} and the {@code _MXMLNORMAL}/{@code _MXMLTEXCOORD} attributes
 *       (viewer UV edits are detected as a stale {@code _MXMLTEXCOORD} and the edited
 *       {@code TEXCOORD_0} adopted instead — see {@link #resolveTcoords}) but re-splits/reorders
 *       vertices, reorders nodes, uses its own buffer layouts (accessor-level {@code byteOffset},
 *       interleaving via {@code bufferView.byteStride}, UNSIGNED_BYTE/SHORT indices and joints)
 *       and drops our animation {@code extras} (the frame rate is then derived from the sampler
 *       input key spacing). Such files cannot reproduce the oracle {@code .j3o} structurally; the
 *       gate is {@code MxmlGltfParity} {@code --renderOnly} (render meanDiff &le; 0.005).
 *       <b>Not yet supported:</b> Blender's skinned-mesh restructure (the mesh moves to an
 *       extras-less {@code '&lt;name&gt;.001'} sibling of the extras-bearing empty, with POSITIONs
 *       rebased into armature space) and meshes Blender's importer drops entirely (1-vertex
 *       particle-emitter meshes) — the reader fails these with a diagnostic naming the node.</li>
 * </ol>
 *
 * <p>Bit-exactness for profile 1 rests on a few deliberate points, mirroring the writer:
 * <ul>
 *   <li><b>Axis convention:</b> the synthetic wrapper node
 *       ({@link MxmlGltfConventions#EXTRA_WRAPPER}) is discarded and its children adopted as roots
 *       — which realises {@link MxmlGltfConventions#GLTF_TO_MXML} losslessly (the mxml node TRS
 *       were stored verbatim under the wrapper). Node TRS numbers are parsed with
 *       {@code Float.parseFloat} on the original token text (not via {@code double}) so they match
 *       the parser's floats bit-for-bit — critical for the mesh-merge 1e-4 epsilon decisions.</li>
 *   <li><b>Vertices/indices:</b> restored 1:1 in stored order into a fresh
 *       {@link ModelDef.TriMeshDef} (the geometry lives as float32 in {@code model.bin}, exact);
 *       normals and texture coordinates come from the raw {@code _MXMLNORMAL}/
 *       {@code _MXMLTEXCOORD} attributes (the standard {@code NORMAL}/{@code TEXCOORD_0} are
 *       viewer-facing copies — normalised resp. v-flipped — and dedup keys on the raw values).</li>
 *   <li><b>Skinning:</b> every JOINTS_n/WEIGHTS_n set is read and non-zero influences re-added in
 *       stored order (= the parser's {@code boneWeights} map iteration order), a fixed point that
 *       reproduces the map's exact key layout and hence the identical weight-group partitioning.</li>
 *   <li><b>Animations:</b> dense per-frame TRS rebuilt from the LINEAR samplers; the
 *       {@code frameRate}/frame count come from the animation {@code extras}. Phantom targets
 *       (animation channels for nodes absent from the model) were dropped by the writer — harmless
 *       because {@code MxmlToJ3o} skips them anyway.</li>
 * </ul>
 */
public class GltfToModelDef
{
    /** The reconstructed model plus its animations, keyed by action name. */
    public static class Parsed
    {
        public ModelDef model;
        public final Map<String,AnimationDef> animations = new LinkedHashMap<>();
    }

    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1) {
            System.err.println("Usage: GltfToModelDef <model.gltf>");
            System.exit(1);
        }
        Parsed p = new GltfToModelDef().read(new File(args[0]));
        System.out.println("Reconstructed " + p.model.spatials.size() + " spatials, " +
            p.animations.size() + " animations: " + p.animations.keySet());
    }

    /** Reads a {@code model.gltf} (with its sibling {@code model.bin}). */
    @SuppressWarnings("unchecked")
    public Parsed read (File gltfFile)
        throws IOException
    {
        String json = new String(Files.readAllBytes(gltfFile.toPath()), StandardCharsets.UTF_8);
        Map<String,Object> gltf = (Map<String,Object>)new Json(json).parse();

        // resolve the binary buffer: a single buffer whose uri is a sibling file (our writer:
        // model.bin) or a base64 data: uri (Blender's "glTF Embedded" flavor). A file with no
        // buffer at all is legal (a mesh-less, animation-less model carries no binary data).
        List<Object> buffers = (List<Object>)gltf.get("buffers");
        if (buffers == null || buffers.isEmpty()) {
            _bin = new byte[0];
        } else {
            Map<String,Object> buffer = (Map<String,Object>)buffers.get(0);
            String uri = (String)buffer.get("uri");
            if (uri == null) {
                throw new IOException("buffer 0 has no uri (GLB-embedded binary chunks are not " +
                    "supported; export as glTF Separate or Embedded)");
            }
            if (uri.startsWith("data:")) {
                int comma = uri.indexOf(',');
                _bin = java.util.Base64.getDecoder().decode(uri.substring(comma + 1));
            } else {
                _bin = Files.readAllBytes(new File(gltfFile.getParentFile(), uri).toPath());
            }
        }

        _accessors = (List<Object>)gltf.get("accessors");
        _bufferViews = (List<Object>)gltf.get("bufferViews");
        _nodes = (List<Object>)gltf.get("nodes");
        _meshes = (List<Object>)gltf.getOrDefault("meshes", new ArrayList<>());
        _skins = (List<Object>)gltf.getOrDefault("skins", new ArrayList<>());

        // locate the synthetic axis-convention wrapper (skipped; its transform is discarded)
        int nn = _nodes.size();
        int wrapper = -1;
        for (int ii = 0; ii < nn; ii++) {
            Map<String,Object> extras = (Map<String,Object>)node(ii).get("extras");
            if (extras != null && Boolean.TRUE.equals(extras.get(
                    MxmlGltfConventions.EXTRA_WRAPPER))) {
                wrapper = ii;
                break;
            }
        }

        Parsed parsed = new Parsed();
        ModelDef model = new ModelDef();
        parsed.model = model;

        // reconstruct spatials in node index order (== the original mdef.spatials order, since the
        // writer emitted one node per spatial in that order after the wrapper)
        for (int ii = 0; ii < nn; ii++) {
            if (ii == wrapper) {
                continue;
            }
            model.spatials.add(readSpatial(ii));
        }

        // animations
        List<Object> anims = (List<Object>)gltf.get("animations");
        if (anims != null) {
            for (Object a : anims) {
                AnimationDef adef = readAnimation((Map<String,Object>)a);
                parsed.animations.put((String)((Map<String,Object>)a).get("name"), adef);
            }
        }
        return parsed;
    }

    @SuppressWarnings("unchecked")
    protected SpatialDef readSpatial (int idx)
    {
        Map<String,Object> node = node(idx);
        Map<String,Object> extras = (Map<String,Object>)node.get("extras");
        if (extras == null) {
            throw new IllegalStateException("node " + idx + " ('" + node.get("name") + "') has " +
                "no mxml extras, so it was not written by MxmlToGltf (nor preserved from it). If " +
                "this is a Blender re-export of a SKINNED model: Blender moves each skinned mesh " +
                "onto a new extras-less '<name>.001' sibling of the original (extras-bearing) " +
                "node — a restructure this reader does not support yet; skinned models are not " +
                "Blender-editable until it does.");
        }
        String type = (String)extras.get(MxmlGltfConventions.EXTRA_MXML_TYPE);

        SpatialDef sdef;
        boolean skin = MxmlGltfConventions.TYPE_SKINMESH.equals(type);
        boolean tri = MxmlGltfConventions.TYPE_TRIMESH.equals(type);
        if (skin) {
            sdef = new SkinMeshDef();
        } else if (tri) {
            sdef = new TriMeshDef();
        } else {
            sdef = new NodeDef();
        }

        sdef.name = (String)node.get("name");
        // parent comes from the explicit extras string (names are not unique — see the writer),
        // not the glTF hierarchy
        sdef.parent = (String)extras.get(MxmlGltfConventions.EXTRA_PARENT);
        // an animated node carries its AUTHORED rest TRS in extras (the writer pins it there because
        // Blender's importer overwrites an animated node's node TRS with an anim-pose evaluation —
        // see MxmlGltfConventions EXTRA_REST_*). Prefer it over the node TRS when present so the
        // mesh-merge pipeline sees the true rest after a Blender round-trip; static nodes (no rest
        // extras) fall through to their node TRS, which stays Blender-editable. Mirrors the
        // offsetTranslation-style extras handling; readTrs reads the same JSON float-list form.
        Object restT = extras.get(MxmlGltfConventions.EXTRA_REST_TRANSLATION);
        Object restR = extras.get(MxmlGltfConventions.EXTRA_REST_ROTATION);
        Object restS = extras.get(MxmlGltfConventions.EXTRA_REST_SCALE);
        sdef.translation = readTrs(restT != null ? restT : node.get("translation"), 0f, 0f, 0f);
        sdef.rotation = readTrs(restR != null ? restR : node.get("rotation"), 0f, 0f, 0f, 1f);
        sdef.scale = readTrs(restS != null ? restS : node.get("scale"), 1f, 1f, 1f);

        if (sdef instanceof TriMeshDef tdef) {
            tdef.solid = Boolean.TRUE.equals(extras.get(MxmlGltfConventions.EXTRA_SOLID));
            tdef.transparent =
                Boolean.TRUE.equals(extras.get(MxmlGltfConventions.EXTRA_TRANSPARENT));
            tdef.texture = (String)extras.get(MxmlGltfConventions.EXTRA_TEXTURE);
            Object ot = extras.get(MxmlGltfConventions.EXTRA_OFFSET_TRANSLATION);
            if (ot != null) tdef.offsetTranslation = readTrs(ot, 0f, 0f, 0f);
            Object orr = extras.get(MxmlGltfConventions.EXTRA_OFFSET_ROTATION);
            if (orr != null) tdef.offsetRotation = readTrs(orr, 0f, 0f, 0f, 1f);
            Object os = extras.get(MxmlGltfConventions.EXTRA_OFFSET_SCALE);
            if (os != null) tdef.offsetScale = readTrs(os, 1f, 1f, 1f);
            boolean hasTc = Boolean.TRUE.equals(extras.get(MxmlGltfConventions.EXTRA_HAS_TCOORDS));
            // the declared mxml vertex count; -1 when the key is absent (glTF written before it
            // existed — the hasTcoords heuristic below remains the fallback for those)
            Object vcRef = extras.get(MxmlGltfConventions.EXTRA_VERTEX_COUNT);
            int vertexCount = (vcRef != null) ? intVal(vcRef) : -1;

            Object meshRef = node.get("mesh");
            if (meshRef != null) {
                readGeometry(tdef, intVal(meshRef), node, hasTc);
            } else if (skin && (hasTc || vertexCount > 0)) {
                // Blender re-parents each skinned mesh onto a NEW extras-less '<name>.001' node
                // (child of the armature, POSITIONs rebased into armature space) and leaves the
                // original node as this extras-bearing empty — a restructure the reader does not
                // support yet
                throw new IllegalStateException("node " + idx + " ('" + sdef.name + "'): " +
                    "skinMesh node carries no glTF mesh. In a Blender re-export the skinned mesh " +
                    "moves to a new extras-less '" + sdef.name + ".001' sibling with positions " +
                    "rebased into armature space — a restructure this reader does not support " +
                    "yet; skinned models are not Blender-editable until it does.");
            } else if (vertexCount > 0) {
                // the definitive dropped-geometry tripwire: the node declares its mxml mesh had
                // vertices but carries no glTF mesh, so an authoring tool dropped the geometry
                // (Blender's exporter cannot write loose points, so 1-vertex particle-emitter
                // meshes vanish silently on export — including tcoord-less ones the hasTcoords
                // heuristic below cannot see) — fail loudly rather than bake the mesh away
                throw new IllegalStateException("node " + idx + " ('" + sdef.name + "'): triMesh " +
                    "declares vertexCount=" + vertexCount + " but carries no glTF mesh — its " +
                    "geometry was dropped in the authoring round-trip (Blender's exporter cannot " +
                    "write loose points, so 1-vertex particle-emitter meshes vanish on export; " +
                    "models with such emitters are not Blender-editable). Restore the mesh from " +
                    "the committed model.gltf.");
            } else if (hasTc && vertexCount < 0) {
                // fallback for files without the vertexCount key: every legitimately-empty
                // triMesh in the corpus has hasTcoords=false, so a meshless triMesh node that HAD
                // texture coordinates means an authoring tool dropped the geometry
                throw new IllegalStateException("node " + idx + " ('" + sdef.name + "'): triMesh " +
                    "with hasTcoords=true carries no glTF mesh — its geometry was dropped in the " +
                    "authoring round-trip (Blender's importer discards 1-vertex particle-emitter " +
                    "meshes, so models with such emitters are not Blender-editable). Restore the " +
                    "mesh from the committed model.gltf.");
            } else {
                tdef.tcoords = false; // empty mesh: no vertices (legitimate, e.g. train dummies)
            }
        }
        return sdef;
    }

    @SuppressWarnings("unchecked")
    protected void readGeometry (TriMeshDef tdef, int meshIdx, Map<String,Object> node,
        boolean hasTc)
    {
        Map<String,Object> mesh = (Map<String,Object>)_meshes.get(meshIdx);
        String where = "mesh " + meshIdx + " ('" + mesh.get("name") + "', node '" + tdef.name +
            "')";
        List<Object> prims = (List<Object>)mesh.get("primitives");
        if (prims.size() != 1) {
            throw new IllegalStateException(where + " has " + prims.size() + " primitives; " +
                "expected exactly 1 (Blender splits a mesh into one primitive per material — " +
                "keep a single material per mesh)");
        }
        Map<String,Object> prim = (Map<String,Object>)prims.get(0);
        Object mode = prim.get("mode");
        if (mode != null && intVal(mode) != 4) {
            throw new IllegalStateException(
                where + ": primitive mode " + intVal(mode) + " is not TRIANGLES (4)");
        }
        Map<String,Object> attrs = (Map<String,Object>)prim.get("attributes");

        float[] pos = readFloats(requireAttr(attrs, "POSITION", where), where + " POSITION");
        // read the RAW normals from _MXMLNORMAL (the standard NORMAL is a normalised copy for
        // viewers/Blender; the bake's vertex dedup / merge keys on the exact raw normal, so the
        // round-trip must use the raw values — see MxmlToGltf). Fall back to NORMAL for any glTF
        // written before _MXMLNORMAL existed.
        Object rawNrm = attrs.get(MxmlGltfConventions.MXML_NORMAL_ATTR);
        float[] nrm = (rawNrm != null)
            ? readFloats(intVal(rawNrm), where + " " + MxmlGltfConventions.MXML_NORMAL_ATTR)
            : readFloats(requireAttr(attrs, "NORMAL", where), where + " NORMAL");
        // texture coordinates are dual too: TEXCOORD_0 is the viewer-facing copy flipped to
        // glTF's top-left origin (v' = 1-v; mxml is bottom-left), _MXMLTEXCOORD the raw mxml
        // values the bake's vertex dedup keys on — resolved by resolveTcoords below
        float[] tcView = hasTc
            ? readFloats(requireAttr(attrs, "TEXCOORD_0", where), where + " TEXCOORD_0") : null;
        Object rawTc = hasTc ? attrs.get(MxmlGltfConventions.MXML_TEXCOORD_ATTR) : null;
        float[] tcRaw = (rawTc != null)
            ? readFloats(intVal(rawTc), where + " " + MxmlGltfConventions.MXML_TEXCOORD_ATTR)
            : null;
        int nverts = pos.length / 3;
        // _MXMLTEXCOORD comes in TWO widths and both must be accepted: the toolchain writes
        // VEC3 — (u, v, 0), padded so all custom attributes share one width, sidestepping the
        // Blender 5.1.0-5.1.2 importer bug with mixed-width custom attributes (see MxmlToGltf)
        // — while pre-pad files and Blender re-exports of them still carry VEC2. Only (u, v)
        // are meaningful either way; narrow VEC3 to VEC2 here (keyed off the accessor's
        // declared type, not the array length) so all downstream logic (count checks,
        // resolveTcoords stale detection, the bake) is width-agnostic.
        if (tcRaw != null && accessorComps(intVal(rawTc)) == 3) {
            int ntc = tcRaw.length / 3;
            float[] narrowed = new float[ntc * 2];
            for (int ii = 0; ii < ntc; ii++) {
                narrowed[ii * 2] = tcRaw[ii * 3];
                narrowed[ii * 2 + 1] = tcRaw[ii * 3 + 1];
            }
            tcRaw = narrowed;
        }
        if (nrm.length != nverts * 3 || (hasTc && tcView.length != nverts * 2) ||
                (tcRaw != null && tcRaw.length != nverts * 2)) {
            throw new IllegalStateException(where + ": vertex attribute counts disagree " +
                "(POSITION " + nverts + ", NORMAL " + (nrm.length / 3) +
                (hasTc ? (", TEXCOORD_0 " + (tcView.length / 2)) : "") +
                (tcRaw != null ? (", " + MxmlGltfConventions.MXML_TEXCOORD_ATTR + " " +
                    (tcRaw.length / 2)) : "") + ")");
        }
        float[] tc = hasTc ? resolveTcoords(tcView, tcRaw, where) : null;

        // skin joint names (local skin index -> bone node name)
        String[] jointNames = null;
        List<int[]> jointSets = new ArrayList<>();
        List<float[]> weightSets = new ArrayList<>();
        boolean isSkin = tdef instanceof SkinMeshDef;
        if (isSkin) {
            Object skinRef = node.get("skin");
            if (skinRef == null) {
                throw new IllegalStateException(where + " is a skinMesh but its node carries no " +
                    "glTF skin");
            }
            List<Object> joints = (List<Object>)((Map<String,Object>)_skins.get(
                intVal(skinRef))).get("joints");
            jointNames = new String[joints.size()];
            for (int jj = 0; jj < joints.size(); jj++) {
                jointNames[jj] = (String)node(intVal(joints.get(jj))).get("name");
            }
            for (int set = 0; attrs.containsKey("JOINTS_" + set); set++) {
                int[] ji = readJoints(intVal(attrs.get("JOINTS_" + set)),
                    where + " JOINTS_" + set);
                float[] jw = readFloats(requireAttr(attrs, "WEIGHTS_" + set, where),
                    where + " WEIGHTS_" + set);
                if (ji.length != nverts * 4 || jw.length != nverts * 4) {
                    throw new IllegalStateException(where + ": JOINTS_" + set + "/WEIGHTS_" + set +
                        " counts (" + (ji.length / 4) + "/" + (jw.length / 4) +
                        ") disagree with POSITION (" + nverts + " vertices)");
                }
                jointSets.add(ji);
                weightSets.add(jw);
            }
        }

        for (int ii = 0; ii < nverts; ii++) {
            Vertex v = isSkin ? new SkinVertex() : new Vertex();
            v.location = new float[] { pos[ii * 3], pos[ii * 3 + 1], pos[ii * 3 + 2] };
            v.normal = new float[] { nrm[ii * 3], nrm[ii * 3 + 1], nrm[ii * 3 + 2] };
            v.tcoords = hasTc ? new float[] { tc[ii * 2], tc[ii * 2 + 1] } : null;
            if (isSkin) {
                SkinVertex sv = (SkinVertex)v;
                for (int set = 0; set < jointSets.size(); set++) {
                    int[] ji = jointSets.get(set);
                    float[] jw = weightSets.get(set);
                    for (int slot = 0; slot < 4; slot++) {
                        float w = jw[ii * 4 + slot];
                        if (w != 0f) {
                            BoneWeight bw = new BoneWeight();
                            bw.bone = jointNames[ji[ii * 4 + slot]];
                            bw.weight = w;
                            sv.addBoneWeight(bw);
                        }
                    }
                }
            }
            tdef.vertices.add(v);
        }
        Object idxRef = prim.get("indices");
        if (idxRef == null) {
            throw new IllegalStateException(where + ": non-indexed primitive (no 'indices'); " +
                "export with indexed geometry");
        }
        int[] idx = readIndices(intVal(idxRef), where + " indices");
        for (int i : idx) {
            tdef.indices.add(i);
        }
        tdef.tcoords = hasTc;
    }

    /**
     * Picks the mxml (bottom-left-origin) texture coordinates from the dual UV attributes:
     * <ul>
     *   <li>{@code _MXMLTEXCOORD} present and consistent with {@code TEXCOORD_0} &rarr; the raw
     *       values (the bit-exact round-trip path — the bake's vertex dedup keys on them);</li>
     *   <li>{@code _MXMLTEXCOORD} absent (a Blender re-export that lost the custom attribute, or
     *       third-party glTF) &rarr; {@code TEXCOORD_0} flipped back (u unchanged, v = 1-v');</li>
     *   <li>both present but <b>inconsistent</b> &rarr; the UVs were edited in a viewer, which
     *       rewrites only {@code TEXCOORD_0} and leaves {@code _MXMLTEXCOORD} stale: adopt the
     *       edited {@code TEXCOORD_0} wholesale (flipped back, per-mesh note printed) so the edit
     *       actually takes effect on rebake instead of being silently ignored.</li>
     * </ul>
     * "Consistent" means max |u' - u| and max |(1-v') - v| &le; {@link #TC_STALE_TOLERANCE} over
     * all vertices.
     */
    protected static float[] resolveTcoords (float[] tcView, float[] tcRaw, String where)
    {
        if (tcRaw == null) {
            return unflipTcoords(tcView);
        }
        float maxDev = 0f;
        for (int ii = 0; ii < tcView.length; ii += 2) {
            maxDev = Math.max(maxDev, Math.abs(tcView[ii] - tcRaw[ii]));                // u
            maxDev = Math.max(maxDev, Math.abs((1f - tcView[ii + 1]) - tcRaw[ii + 1])); // v
        }
        if (maxDev > TC_STALE_TOLERANCE) {
            System.out.println("  note: " + where + ": TEXCOORD_0 disagrees with " +
                MxmlGltfConventions.MXML_TEXCOORD_ATTR + " (max deviation " + maxDev + ") — " +
                "the UVs were edited in a viewer; using the edited TEXCOORD_0 (v flipped back)");
            return unflipTcoords(tcView);
        }
        return tcRaw;
    }

    /** Maps viewer-facing (top-left-origin) texture coordinates back to the mxml bottom-left
     * convention: u unchanged, v = 1-v'. Lossy at the float-ULP level, so only used when the raw
     * {@code _MXMLTEXCOORD} is absent or stale (see {@link #resolveTcoords}). */
    protected static float[] unflipTcoords (float[] tcView)
    {
        float[] out = tcView.clone();
        for (int ii = 1; ii < out.length; ii += 2) {
            out[ii] = 1f - out[ii];
        }
        return out;
    }

    /** The stale-UV detection tolerance for {@link #resolveTcoords}. Must sit well ABOVE the
     * float32 noise of the v &rarr; 1-v &rarr; 1-(1-v) flip cycle on unedited data (measured max
     * 2.4e-7 across all 1434 textured corpus meshes, whose UVs tile out to &plusmn;130) — a false
     * "stale" verdict would swap ULP-perturbed values into the bake and break bit-exact parity —
     * and well BELOW the smallest meaningful UV edit (one texel of a 1024px texture &asymp;
     * 9.8e-4). 1e-6 gives ~4x noise headroom and ~1000x edit margin. */
    protected static final float TC_STALE_TOLERANCE = 1e-6f;

    /** Resolves a required primitive attribute to its accessor index, or fails naming the mesh
     * and the missing attribute. */
    protected static int requireAttr (Map<String,Object> attrs, String name, String where)
    {
        Object ref = attrs.get(name);
        if (ref == null) {
            throw new IllegalStateException(where + " primitive has no " + name + " attribute");
        }
        return intVal(ref);
    }

    // ================================================================================
    // animations
    // ================================================================================

    @SuppressWarnings("unchecked")
    protected AnimationDef readAnimation (Map<String,Object> anim)
    {
        String name = (String)anim.get("name");
        AnimationDef adef = new AnimationDef();

        // per animated node: its translation/rotation/scale output accessors, in channel
        // first-appearance order (plus the node index, for the static-TRS fallback below)
        List<String> order = new ArrayList<>();
        Map<String,int[]> accByNode = new LinkedHashMap<>(); // [T,R,S] accessor indices
        Map<String,Integer> nodeByName = new LinkedHashMap<>();
        List<Object> channels = (List<Object>)anim.get("channels");
        List<Object> samplers = (List<Object>)anim.get("samplers");
        int firstInput = -1;
        if (channels != null) {
            for (Object c : channels) {
                Map<String,Object> ch = (Map<String,Object>)c;
                Map<String,Object> target = (Map<String,Object>)ch.get("target");
                int nodeIdx = intVal(target.get("node"));
                String nodeName = (String)node(nodeIdx).get("name");
                String path = (String)target.get("path");
                Map<String,Object> sampler = (Map<String,Object>)samplers.get(
                    intVal(ch.get("sampler")));
                if ("CUBICSPLINE".equals(sampler.get("interpolation"))) {
                    throw new IllegalStateException("animation '" + name + "': CUBICSPLINE " +
                        "sampler (node '" + nodeName + "', path '" + path + "') is not " +
                        "supported; re-export with sampled LINEAR keyframes");
                }
                int outAcc = intVal(sampler.get("output"));
                if (firstInput < 0) {
                    firstInput = intVal(sampler.get("input"));
                }
                int[] trs = accByNode.get(nodeName);
                if (trs == null) {
                    trs = new int[] { -1, -1, -1 };
                    accByNode.put(nodeName, trs);
                    nodeByName.put(nodeName, nodeIdx);
                    order.add(nodeName);
                }
                switch (path) {
                    case "translation": trs[0] = outAcc; break;
                    case "rotation": trs[1] = outAcc; break;
                    case "scale": trs[2] = outAcc; break;
                    default: break;
                }
            }
        }

        // frame grid: our writer records the exact mxml frameRate/frameCount in the animation
        // extras; Blender's importer drops them, so for a re-export derive both from the (dense)
        // sampler input — key count and 1/spacing (Blender resamples on exactly that grid)
        Map<String,Object> extras = (Map<String,Object>)anim.get("extras");
        int frameCount;
        if (extras != null && extras.get(MxmlGltfConventions.EXTRA_FRAME_RATE) != null) {
            adef.frameRate = intVal(extras.get(MxmlGltfConventions.EXTRA_FRAME_RATE));
            frameCount = intVal(extras.get("frameCount"));
        } else if (firstInput >= 0) {
            float[] times = readFloats(firstInput, "animation '" + name + "' sampler input");
            frameCount = times.length;
            adef.frameRate = deriveFrameRate(times);
        } else {
            throw new IllegalStateException("animation '" + name + "' has no frameRate/" +
                "frameCount extras and no channels to derive them from");
        }

        // read all output data; a node whose channel for some path was omitted (producers may
        // drop constant tracks) holds its static (rest) value on that path for every frame
        Map<String,float[]> tData = new LinkedHashMap<>(), rData = new LinkedHashMap<>(),
            sData = new LinkedHashMap<>();
        for (String nn : order) {
            int[] trs = accByNode.get(nn);
            Map<String,Object> node = node(nodeByName.get(nn));
            // the static fallback must prefer the authored rest extras over the node TRS, exactly
            // as readSpatial does: for Blender re-exports an animated node's TRS is clobbered with
            // a pose evaluation (possibly of a DIFFERENT clip), which would silently bake a wrong
            // "constant" value here (#50 review finding). Our own writer emits dense T/R/S
            // channels for every targeted node, so this fallback never fires on its files.
            @SuppressWarnings("unchecked")
            Map<String,Object> nx = (Map<String,Object>)node.get("extras");
            Object restT = nx == null ? null : nx.get(MxmlGltfConventions.EXTRA_REST_TRANSLATION);
            Object restR = nx == null ? null : nx.get(MxmlGltfConventions.EXTRA_REST_ROTATION);
            Object restS = nx == null ? null : nx.get(MxmlGltfConventions.EXTRA_REST_SCALE);
            tData.put(nn, trs[0] >= 0
                ? readFloats(trs[0], "animation '" + name + "' node '" + nn + "' translation")
                : repeatStatic(readTrs(restT != null ? restT : node.get("translation"),
                    0f, 0f, 0f), frameCount));
            rData.put(nn, trs[1] >= 0
                ? readFloats(trs[1], "animation '" + name + "' node '" + nn + "' rotation")
                : repeatStatic(readTrs(restR != null ? restR : node.get("rotation"),
                    0f, 0f, 0f, 1f), frameCount));
            sData.put(nn, trs[2] >= 0
                ? readFloats(trs[2], "animation '" + name + "' node '" + nn + "' scale")
                : repeatStatic(readTrs(restS != null ? restS : node.get("scale"),
                    1f, 1f, 1f), frameCount));
            checkTrack(name, nn, "translation", tData.get(nn), frameCount, 3);
            checkTrack(name, nn, "rotation", rData.get(nn), frameCount, 4);
            checkTrack(name, nn, "scale", sData.get(nn), frameCount, 3);
        }

        for (int ff = 0; ff < frameCount; ff++) {
            FrameDef frame = new FrameDef();
            for (String nn : order) {
                TransformDef td = new TransformDef();
                td.name = nn;
                float[] t = tData.get(nn), r = rData.get(nn), s = sData.get(nn);
                td.translation = new float[] { t[ff * 3], t[ff * 3 + 1], t[ff * 3 + 2] };
                td.rotation = new float[] { r[ff * 4], r[ff * 4 + 1], r[ff * 4 + 2], r[ff * 4 + 3] };
                td.scale = new float[] { s[ff * 3], s[ff * 3 + 1], s[ff * 3 + 2] };
                frame.addTransform(td);
            }
            adef.addFrame(frame);
        }
        return adef;
    }

    /** Derives an integer frames-per-second from dense sampler input times (the mean key
     * spacing): the fallback when a producer (Blender) dropped our frameRate extras. */
    protected static int deriveFrameRate (float[] times)
    {
        if (times.length < 2) {
            return com.threerings.jme.model.ModelMetadata.DEFAULT_FRAME_RATE;
        }
        double spacing = (double)(times[times.length - 1] - times[0]) / (times.length - 1);
        return (spacing > 0) ? Math.max(1, (int)Math.round(1 / spacing))
            : com.threerings.jme.model.ModelMetadata.DEFAULT_FRAME_RATE;
    }

    /** Expands one static TRS value to a dense per-frame track (for a channel a producer omitted
     * because it was constant: the node holds its rest value). */
    protected static float[] repeatStatic (float[] value, int frames)
    {
        float[] out = new float[value.length * frames];
        for (int ff = 0; ff < frames; ff++) {
            System.arraycopy(value, 0, out, ff * value.length, value.length);
        }
        return out;
    }

    protected static void checkTrack (
        String anim, String node, String path, float[] data, int frames, int comps)
    {
        if (data.length != frames * comps) {
            throw new IllegalStateException("animation '" + anim + "' node '" + node + "' " +
                path + " track has " + (data.length / comps) + " keys; expected " + frames +
                " (all channels must share the animation's dense frame grid)");
        }
    }

    // ================================================================================
    // accessor reading (from model.bin)
    // ================================================================================

    // glTF componentType codes
    protected static final int COMP_UNSIGNED_BYTE = 5121;
    protected static final int COMP_UNSIGNED_SHORT = 5123;
    protected static final int COMP_UNSIGNED_INT = 5125;
    protected static final int COMP_FLOAT = 5126;

    /**
     * The resolved byte layout of one accessor: where element 0 lives in {@code model.bin} and
     * how far apart consecutive elements sit. Generalised beyond {@code MxmlToGltf}'s own output
     * (tightly-packed, one accessor per bufferView, no offsets) to the layouts other producers —
     * Blender's exporter in particular — emit: an accessor-level {@code byteOffset}, a
     * bufferView-level {@code byteOffset}, and interleaved vertex attributes via
     * {@code bufferView.byteStride}. All bounds are validated up front so a malformed or
     * truncated file fails with a message naming the accessor and bufferView instead of a bare
     * {@code BufferUnderflowException}.
     */
    protected class Layout
    {
        final ByteBuffer bb;    // all of model.bin, little-endian (absolute reads only)
        final int comps;        // components per element (VEC3 -> 3, ...)
        final int count;        // element count
        final int componentType;
        final int base;         // absolute byte offset of element 0
        final int stride;       // bytes between consecutive elements

        @SuppressWarnings("unchecked")
        Layout (int accIdx, String use, int... allowedComponentTypes)
        {
            if (accIdx < 0 || accIdx >= _accessors.size()) {
                throw new IllegalStateException(use + ": accessor index " + accIdx +
                    " out of range (file has " + _accessors.size() + " accessors)");
            }
            Map<String,Object> acc = (Map<String,Object>)_accessors.get(accIdx);
            comps = compsOf((String)acc.get("type"));
            count = intVal(acc.get("count"));
            componentType = intVal(acc.get("componentType"));
            boolean allowed = false;
            for (int ct : allowedComponentTypes) {
                allowed |= (ct == componentType);
            }
            if (!allowed) {
                StringBuilder names = new StringBuilder();
                for (int ct : allowedComponentTypes) {
                    if (names.length() > 0) names.append('/');
                    names.append(componentTypeName(ct));
                }
                throw new IllegalStateException(use + ": accessor " + accIdx +
                    " has unsupported componentType " + componentTypeName(componentType) +
                    "; expected " + names);
            }
            Object bvRef = acc.get("bufferView");
            if (bvRef == null) {
                throw new IllegalStateException(use + ": accessor " + accIdx +
                    " has no bufferView (sparse/zero-filled accessors are not supported)");
            }
            int bvIdx = intVal(bvRef);
            if (bvIdx < 0 || bvIdx >= _bufferViews.size()) {
                throw new IllegalStateException(use + ": accessor " + accIdx +
                    " references bufferView " + bvIdx + " but the file has " +
                    _bufferViews.size());
            }
            Map<String,Object> bv = (Map<String,Object>)_bufferViews.get(bvIdx);
            // only buffers[0] is loaded into _bin (both supported producers emit a single
            // buffer); a multi-buffer file must fail loudly, not silently read buffer 0
            int bufIdx = bv.containsKey("buffer") ? intVal(bv.get("buffer")) : 0;
            if (bufIdx != 0) {
                throw new IllegalStateException(use + ": bufferView " + bvIdx +
                    " references buffer " + bufIdx + " — only single-buffer glTF is supported");
            }
            int bvOff = bv.containsKey("byteOffset") ? intVal(bv.get("byteOffset")) : 0;
            int bvLen = intVal(bv.get("byteLength"));
            int accOff = acc.containsKey("byteOffset") ? intVal(acc.get("byteOffset")) : 0;
            int elemSize = comps * componentSize(componentType);
            stride = bv.containsKey("byteStride") ? intVal(bv.get("byteStride")) : elemSize;
            base = bvOff + accOff;
            int end = (count == 0) ? accOff : accOff + (count - 1) * stride + elemSize;
            if (stride < elemSize || end > bvLen || bvOff + bvLen > _bin.length) {
                throw new IllegalStateException(use + ": accessor " + accIdx + " (" +
                    acc.get("type") + " x" + count + " of " +
                    componentTypeName(componentType) + ") overruns bufferView " + bvIdx +
                    " (byteOffset=" + bvOff + ", byteLength=" + bvLen + ", byteStride=" +
                    (bv.containsKey("byteStride") ? String.valueOf(stride) : "none") +
                    ", accessor byteOffset=" + accOff + ", bin=" + _bin.length + " bytes)");
            }
            bb = ByteBuffer.wrap(_bin).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /** Reads a FLOAT accessor (vertex attributes, weights, animation data) into a flat
     * {@code float[count * comps]}, honoring interleaving/offsets via {@link Layout}. */
    protected float[] readFloats (int accIdx, String use)
    {
        Layout l = new Layout(accIdx, use, COMP_FLOAT);
        float[] out = new float[l.count * l.comps];
        for (int e = 0; e < l.count; e++) {
            int p = l.base + e * l.stride;
            for (int c = 0; c < l.comps; c++) {
                out[e * l.comps + c] = l.bb.getFloat(p + c * 4);
            }
        }
        return out;
    }

    /** Reads a JOINTS_n accessor: UNSIGNED_SHORT (our writer) or UNSIGNED_BYTE (Blender for
     * &le;255 joints). */
    protected int[] readJoints (int accIdx, String use)
    {
        Layout l = new Layout(accIdx, use, COMP_UNSIGNED_BYTE, COMP_UNSIGNED_SHORT);
        int[] out = new int[l.count * l.comps];
        boolean bytes = (l.componentType == COMP_UNSIGNED_BYTE);
        for (int e = 0; e < l.count; e++) {
            int p = l.base + e * l.stride;
            for (int c = 0; c < l.comps; c++) {
                out[e * l.comps + c] = bytes
                    ? (l.bb.get(p + c) & 0xFF) : (l.bb.getShort(p + c * 2) & 0xFFFF);
            }
        }
        return out;
    }

    /** Reads an index accessor: UNSIGNED_INT (our writer) or UNSIGNED_SHORT/UNSIGNED_BYTE
     * (Blender sizes indices to the vertex count). */
    protected int[] readIndices (int accIdx, String use)
    {
        Layout l = new Layout(
            accIdx, use, COMP_UNSIGNED_BYTE, COMP_UNSIGNED_SHORT, COMP_UNSIGNED_INT);
        int[] out = new int[l.count * l.comps];
        for (int e = 0; e < l.count; e++) {
            int p = l.base + e * l.stride;
            for (int c = 0; c < l.comps; c++) {
                switch (l.componentType) {
                    case COMP_UNSIGNED_BYTE: out[e * l.comps + c] = l.bb.get(p + c) & 0xFF; break;
                    case COMP_UNSIGNED_SHORT:
                        out[e * l.comps + c] = l.bb.getShort(p + c * 2) & 0xFFFF; break;
                    default: out[e * l.comps + c] = l.bb.getInt(p + c * 4); break;
                }
            }
        }
        return out;
    }

    /** The component count an accessor declares (its {@code type} field: VEC2 &rarr; 2, ...). */
    @SuppressWarnings("unchecked")
    protected int accessorComps (int accIdx)
    {
        return compsOf((String)((Map<String,Object>)_accessors.get(accIdx)).get("type"));
    }

    protected static int compsOf (String type)
    {
        switch (type) {
            case "SCALAR": return 1;
            case "VEC2": return 2;
            case "VEC3": return 3;
            case "VEC4": return 4;
            case "MAT4": return 16;
            default: throw new IllegalArgumentException("unhandled accessor type " + type);
        }
    }

    protected static int componentSize (int componentType)
    {
        switch (componentType) {
            case COMP_UNSIGNED_BYTE: return 1;
            case COMP_UNSIGNED_SHORT: return 2;
            case COMP_UNSIGNED_INT: case COMP_FLOAT: return 4;
            default:
                throw new IllegalArgumentException("unhandled componentType " + componentType);
        }
    }

    protected static String componentTypeName (int componentType)
    {
        switch (componentType) {
            case COMP_UNSIGNED_BYTE: return "5121/UNSIGNED_BYTE";
            case COMP_UNSIGNED_SHORT: return "5123/UNSIGNED_SHORT";
            case COMP_UNSIGNED_INT: return "5125/UNSIGNED_INT";
            case COMP_FLOAT: return "5126/FLOAT";
            default: return String.valueOf(componentType);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String,Object> node (int idx)
    {
        return (Map<String,Object>)_nodes.get(idx);
    }

    /** Reads a JSON number array (Num tokens) into a float[] with per-component defaults if the
     * array is absent. Uses {@code Float.parseFloat} on the ORIGINAL token text (never via double)
     * so values match the mxml parser's floats bit-for-bit. */
    @SuppressWarnings("unchecked")
    protected static float[] readTrs (Object arr, float... defaults)
    {
        if (arr == null) {
            return defaults.clone();
        }
        List<Object> list = (List<Object>)arr;
        float[] out = new float[list.size()];
        for (int ii = 0; ii < out.length; ii++) {
            out[ii] = floatVal(list.get(ii));
        }
        return out;
    }

    protected static float floatVal (Object o)
    {
        return Float.parseFloat(((Json.Num)o).raw);
    }

    protected static int intVal (Object o)
    {
        String raw = ((Json.Num)o).raw;
        // structural ints are emitted as plain integers; be tolerant of a trailing ".0"
        int dot = raw.indexOf('.');
        return Integer.parseInt(dot < 0 ? raw : raw.substring(0, dot));
    }

    protected byte[] _bin;
    protected List<Object> _accessors, _bufferViews, _nodes, _meshes, _skins;

    // ================================================================================
    // minimal JSON parser: numbers are kept as raw token text (Json.Num) so the reader can
    // Float.parseFloat them losslessly; objects are LinkedHashMap (insertion order preserved)
    // ================================================================================

    protected static final class Json
    {
        /** A JSON number, kept as its original token text for lossless float reconstruction. */
        static final class Num
        {
            final String raw;
            Num (String raw) { this.raw = raw; }
            @Override public String toString () { return raw; }
        }

        Json (String s) { _s = s; _n = s.length(); }

        Object parse ()
        {
            Object v = value();
            skipWs();
            if (_i != _n) {
                throw new IllegalStateException("trailing JSON at offset " + _i);
            }
            return v;
        }

        Object value ()
        {
            skipWs();
            char c = _s.charAt(_i);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return number();
            }
        }

        Map<String,Object> object ()
        {
            Map<String,Object> m = new LinkedHashMap<>();
            _i++; // {
            skipWs();
            if (_s.charAt(_i) == '}') { _i++; return m; }
            while (true) {
                skipWs();
                String key = string();
                skipWs();
                expectChar(':');
                m.put(key, value());
                skipWs();
                char c = _s.charAt(_i++);
                if (c == '}') break;
                if (c != ',') throw new IllegalStateException("expected , or } at " + (_i - 1));
            }
            return m;
        }

        List<Object> array ()
        {
            List<Object> list = new ArrayList<>();
            _i++; // [
            skipWs();
            if (_s.charAt(_i) == ']') { _i++; return list; }
            while (true) {
                list.add(value());
                skipWs();
                char c = _s.charAt(_i++);
                if (c == ']') break;
                if (c != ',') throw new IllegalStateException("expected , or ] at " + (_i - 1));
            }
            return list;
        }

        String string ()
        {
            expectChar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = _s.charAt(_i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = _s.charAt(_i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            sb.append((char)Integer.parseInt(_s.substring(_i, _i + 4), 16));
                            _i += 4;
                            break;
                        default: throw new IllegalStateException("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Num number ()
        {
            int start = _i;
            while (_i < _n) {
                char c = _s.charAt(_i);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' ||
                    (c >= '0' && c <= '9')) {
                    _i++;
                } else {
                    break;
                }
            }
            return new Num(_s.substring(start, _i));
        }

        void expect (String lit)
        {
            if (!_s.startsWith(lit, _i)) {
                throw new IllegalStateException("expected " + lit + " at " + _i);
            }
            _i += lit.length();
        }

        void expectChar (char c)
        {
            if (_s.charAt(_i++) != c) {
                throw new IllegalStateException("expected " + c + " at " + (_i - 1));
            }
        }

        void skipWs ()
        {
            while (_i < _n) {
                char c = _s.charAt(_i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    _i++;
                } else {
                    break;
                }
            }
        }

        private final String _s;
        private final int _n;
        private int _i;
    }
}
