//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.samskivert.util.StringUtil;

import com.threerings.bang.jme3.mxml.ModelDef.BoneWeight;
import com.threerings.bang.jme3.mxml.ModelDef.SkinMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.SkinVertex;
import com.threerings.bang.jme3.mxml.ModelDef.SpatialDef;
import com.threerings.bang.jme3.mxml.ModelDef.TriMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.Vertex;

/**
 * The Epic 4 Phase 2b-ii native <code>.mxml</code> &rarr; <b>glTF 2.0</b> writer: reads a model
 * directory ({@code model.properties} + {@code model.mxml} + per-animation {@code *.mxml}) through
 * the fork-free Phase 2a parsers and emits {@code model.gltf} (JSON, not GLB) + {@code model.bin},
 * so the 310-model corpus becomes Blender-editable. It is the exact counterpart of
 * {@link GltfToModelDef} — together they round-trip a {@link ModelDef}/{@link AnimationDef}[] with
 * no loss (verified by {@link MxmlGltfParity}: glTF &rarr; {@link MxmlToJ3o} reproduces the oracle
 * {@code .j3o} bit-for-bit).
 *
 * <p><b>What is stored, and why the round-trip is lossless.</b> The writer serialises the
 * <em>raw parsed</em> {@link ModelDef} (before any merge / vertex-cache optimisation / centering —
 * those are {@link MxmlToJ3o}'s compile-stage concerns and are re-run on import), so:
 * <ul>
 *   <li><b>Node hierarchy + transforms:</b> one glTF node per {@link SpatialDef}, TRS written
 *       verbatim; all mxml roots re-parented under a single wrapper node carrying
 *       {@link MxmlGltfConventions#MXML_TO_GLTF} (the Z-up&rarr;Y-up convention — see that class).
 *   <li><b>Geometry:</b> POSITION/NORMAL/(TEXCOORD_0) + UNSIGNED_INT indices, straight from the
 *       already-de-duplicated {@link TriMeshDef} vertex/index lists, as float32 in {@code model.bin}
 *       (bit-exact by construction).
 *   <li><b>Skinning:</b> the corpus has up to <b>12</b> bone influences per vertex (cavalry), so
 *       the full influence set is preserved across as many JOINTS_n/WEIGHTS_n sets as needed
 *       (ceil(max/4)); pre-capping to glTF's 4-per-set limit would shrink {@link MxmlToJ3o}'s
 *       armature and break structural parity. {@link MxmlToJ3o} does the 4-influence cap itself,
 *       at bake time, exactly as the oracle does.
 *   <li><b>Materials:</b> {@code pbrMetallicRoughness} (metallic 0, roughness 1) +
 *       {@code KHR_materials_unlit} so Blender previews match the game's unlit look;
 *       {@code baseColorTexture} references the existing PNG <em>relatively</em> (never copied);
 *       {@code alphaMode=BLEND} iff transparent, {@code doubleSided} iff !solid.
 *   <li><b>Animations:</b> one glTF animation per animation {@code .mxml}, named exactly by the
 *       action (file basename); per-joint T/R/S LINEAR samplers with dense per-frame keys
 *       (input = frame/frameRate).
 *   <li><b>{@code extras} (the losslessness valve):</b> per the {@link MxmlGltfConventions} keys —
 *       spatial subtype, {@code solid}/{@code transparent}, the raw {@code texture} string, the
 *       {@code hasTcoords} flag, the geometry offset transform (when non-identity), the animation
 *       {@code frameRate}/frame count, and the raw-units note in {@code asset.extras}. Everything
 *       else the fork bake drops lives in {@code model.properties}, which stays a committed source.
 * </ul>
 *
 * <p>Usage: {@code MxmlToGltf <src rsrc root> <model type path> <out rsrc root>} writes
 * {@code <out>/<type>/model.gltf}+{@code model.bin}.
 */
public class MxmlToGltf
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 3) {
            System.err.println(
                "Usage: MxmlToGltf <src rsrc root> <model type path> <out rsrc root>");
            System.exit(1);
        }
        File srcRoot = new File(args[0]).getAbsoluteFile();
        String typePath = args[1];
        File outRoot = new File(args[2]).getAbsoluteFile();
        File modelDir = new File(srcRoot, typePath);
        if (!new File(modelDir, "model.properties").isFile()) {
            System.err.println("No model.properties in " + modelDir);
            System.exit(1);
        }
        File outDir = new File(outRoot, typePath);
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            System.err.println("Unable to create " + outDir);
            System.exit(1);
        }
        Result r = new MxmlToGltf().writeFromDir(modelDir, outDir);
        System.out.println("Wrote " + new File(outDir, "model.gltf") + ": " + r.summary());
    }

    /** A small summary of one export. */
    public static class Result
    {
        public int nodes, meshes, skins, animations;
        public int maxInfluences;
        public String summary ()
        {
            return nodes + " nodes, " + meshes + " meshes, " + skins + " skins, " +
                animations + " animations, maxInfluences=" + maxInfluences;
        }
    }

    /** Parses the model in {@code modelDir} and writes {@code model.gltf}+{@code model.bin} into
     * {@code outDir}. */
    public Result writeFromDir (File modelDir, File outDir)
        throws Exception
    {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(new File(modelDir, "model.properties"))) {
            props.load(in);
        }
        ModelDef mdef = new ModelParser().parseModel(
            new File(modelDir, "model.mxml").getPath());
        String[] animNames = StringUtil.parseStringArray(props.getProperty("animations", ""));
        AnimationParser aparser = new AnimationParser();
        AnimationDef[] adefs = new AnimationDef[animNames.length];
        for (int ii = 0; ii < animNames.length; ii++) {
            adefs[ii] = aparser.parseAnimation(
                new File(modelDir, animNames[ii] + ".mxml").getPath());
        }
        return write(mdef, animNames, adefs, outDir);
    }

    /** Writes an already-parsed model to {@code outDir/model.gltf}+{@code model.bin}. */
    public Result write (ModelDef mdef, String[] animNames, AnimationDef[] adefs, File outDir)
        throws IOException
    {
        Result result = new Result();
        _bin = new ByteArrayOutputStream();
        _accessors = new ArrayList<>();
        _bufferViews = new ArrayList<>();
        _meshes = new ArrayList<>();
        _materials = new ArrayList<>();
        _textures = new ArrayList<>();
        _images = new ArrayList<>();
        _samplers = new ArrayList<>();
        _skins = new ArrayList<>();
        _animations = new ArrayList<>();
        _textureByUri = new LinkedHashMap<>();
        _defaultSampler = -1;

        // index every spatial by name, and compute bind-pose model transforms (fork-exact math,
        // the same source of truth MxmlToJ3o uses for its skinning) up front for inverse-binds
        _byName = new LinkedHashMap<>();
        for (SpatialDef s : mdef.spatials) {
            _byName.put(s.name, s);
        }
        _globalBind = new LinkedHashMap<>();

        // ---- nodes: a wrapper root (axis convention) + one node per spatial ----
        // node index layout: [0] = wrapper; [base+i] = mdef.spatials.get(i). Node identity is the
        // INDEX, never the name — the corpus has duplicate spatial names (e.g. two meshes both
        // "trunk" in props/indian_post/natural/tree_pine_*), which a name->index map would collapse
        // (dropping one mesh); the reader reconstructs each spatial's `parent` from an explicit
        // extras field rather than inferring it from the (name-ambiguous) hierarchy.
        int base = 1;
        List<Map<String,Object>> nodes = new ArrayList<>();
        Map<String,Object> wrapper = new LinkedHashMap<>();
        wrapper.put("name", "mxml-root");
        wrapper.put("rotation", quatArray(MxmlGltfConventions.MXML_TO_GLTF));
        Map<String,Object> wrapExtras = new LinkedHashMap<>();
        wrapExtras.put(MxmlGltfConventions.EXTRA_WRAPPER, Boolean.TRUE);
        wrapper.put("extras", wrapExtras);
        nodes.add(wrapper); // index 0

        // name -> node index for parent/anim/skin resolution (LAST wins, mirroring the HashMap the
        // fork bake keys nodes by, so name-ambiguous lookups resolve the same way it does)
        Map<String,Integer> nameToIndex = new LinkedHashMap<>();
        for (int ii = 0; ii < mdef.spatials.size(); ii++) {
            nameToIndex.put(mdef.spatials.get(ii).name, base + ii);
        }
        _pendingNodeIndex = nameToIndex;

        for (int ii = 0; ii < mdef.spatials.size(); ii++) {
            SpatialDef s = mdef.spatials.get(ii);
            Map<String,Object> node = new LinkedHashMap<>();
            node.put("name", s.name);
            node.put("translation", floatArray(s.translation));
            node.put("rotation", floatArray(s.rotation));
            node.put("scale", floatArray(s.scale));
            Map<String,Object> extras = new LinkedHashMap<>();
            extras.put(MxmlGltfConventions.EXTRA_MXML_TYPE, typeOf(s));
            if (s.parent != null) {
                extras.put(MxmlGltfConventions.EXTRA_PARENT, s.parent);
            }
            // mesh metadata rides on the NODE extras (not the glTF mesh) so it survives even for
            // the 5 empty triMeshes that carry no glTF mesh — two of which live in the
            // merge-sensitive train models, where canMerge compares exactly these flags
            if (s instanceof TriMeshDef tdef) {
                putMeshExtras(extras, tdef);
            }
            node.put("extras", extras);
            nodes.add(node); // index base+ii
            result.nodes++;
        }

        // glTF display hierarchy: attach each spatial's node under its parent's node (by name,
        // last-wins), roots under the wrapper. Only for viewing — the round-trip uses EXTRA_PARENT.
        Map<Integer,List<Integer>> childrenOf = new LinkedHashMap<>();
        List<Integer> wrapperChildren = new ArrayList<>();
        for (int ii = 0; ii < mdef.spatials.size(); ii++) {
            SpatialDef s = mdef.spatials.get(ii);
            Integer pIdx = (s.parent == null) ? null : nameToIndex.get(s.parent);
            if (pIdx == null) {
                wrapperChildren.add(base + ii);
            } else {
                childrenOf.computeIfAbsent(pIdx, k -> new ArrayList<>()).add(base + ii);
            }
        }
        for (Map.Entry<Integer,List<Integer>> e : childrenOf.entrySet()) {
            nodes.get(e.getKey()).put("children", e.getValue());
        }
        if (!wrapperChildren.isEmpty()) {
            wrapper.put("children", wrapperChildren);
        }

        // ---- meshes, materials, skins (assigned to node base+i by INDEX) ----
        for (int ii = 0; ii < mdef.spatials.size(); ii++) {
            SpatialDef s = mdef.spatials.get(ii);
            if (!(s instanceof TriMeshDef tdef) || tdef.indices.isEmpty()) {
                continue; // NodeDef, or an empty triMesh (carries no glTF mesh; type is in extras)
            }
            Map<String,Object> node = nodes.get(base + ii);
            int meshIdx = buildMesh(tdef, result);
            node.put("mesh", meshIdx);
            if (tdef instanceof SkinMeshDef sdef) {
                int skinIdx = buildSkin(sdef, nameToIndex, result);
                node.put("skin", skinIdx);
                result.skins++;
            }
            result.meshes++;
        }

        // ---- animations ----
        for (int ii = 0; ii < animNames.length; ii++) {
            buildAnimation(animNames[ii], adefs[ii]);
            result.animations++;
        }

        // ---- assemble the glTF JSON ----
        Map<String,Object> gltf = new LinkedHashMap<>();
        Map<String,Object> asset = new LinkedHashMap<>();
        asset.put("version", "2.0");
        asset.put("generator", MxmlGltfConventions.ASSET_GENERATOR);
        Map<String,Object> assetExtras = new LinkedHashMap<>();
        assetExtras.put("units", MxmlGltfConventions.ASSET_UNITS);
        assetExtras.put("axis", MxmlGltfConventions.ASSET_AXIS);
        asset.put("extras", assetExtras);
        gltf.put("asset", asset);

        List<String> used = new ArrayList<>();
        used.add("KHR_materials_unlit");
        gltf.put("extensionsUsed", used);

        gltf.put("scene", 0);
        Map<String,Object> scene = new LinkedHashMap<>();
        scene.put("nodes", List.of(0));
        gltf.put("scenes", List.of(scene));
        gltf.put("nodes", nodes);
        if (!_meshes.isEmpty()) gltf.put("meshes", _meshes);
        if (!_materials.isEmpty()) gltf.put("materials", _materials);
        if (!_textures.isEmpty()) gltf.put("textures", _textures);
        if (!_images.isEmpty()) gltf.put("images", _images);
        if (!_samplers.isEmpty()) gltf.put("samplers", _samplers);
        if (!_skins.isEmpty()) gltf.put("skins", _skins);
        if (!_animations.isEmpty()) gltf.put("animations", _animations);
        gltf.put("accessors", _accessors);
        gltf.put("bufferViews", _bufferViews);
        byte[] binBytes = _bin.toByteArray();
        Map<String,Object> buffer = new LinkedHashMap<>();
        buffer.put("byteLength", binBytes.length);
        buffer.put("uri", "model.bin");
        gltf.put("buffers", List.of(buffer));

        // write model.bin then model.gltf
        try (FileOutputStream bout = new FileOutputStream(new File(outDir, "model.bin"))) {
            bout.write(binBytes);
        }
        StringBuilder sb = new StringBuilder();
        writeJson(gltf, sb, 0);
        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(new File(outDir, "model.gltf")), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        return result;
    }

    protected static String typeOf (SpatialDef s)
    {
        if (s instanceof SkinMeshDef) return MxmlGltfConventions.TYPE_SKINMESH;
        if (s instanceof TriMeshDef) return MxmlGltfConventions.TYPE_TRIMESH;
        return MxmlGltfConventions.TYPE_NODE;
    }

    // ================================================================================
    // meshes
    // ================================================================================

    protected int buildMesh (TriMeshDef tdef, Result result)
    {
        int nverts = tdef.vertices.size();

        // POSITION + NORMAL + (TEXCOORD_0)
        float[] pos = new float[nverts * 3];
        float[] nrm = new float[nverts * 3];
        boolean hasTc = tdef.tcoords;
        float[] tc = hasTc ? new float[nverts * 2] : null;
        float[] min = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY };
        float[] max = { Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        for (int ii = 0; ii < nverts; ii++) {
            Vertex v = tdef.vertices.get(ii);
            for (int k = 0; k < 3; k++) {
                float p = v.location[k];
                pos[ii * 3 + k] = p;
                nrm[ii * 3 + k] = v.normal[k];
                if (p < min[k]) min[k] = p;
                if (p > max[k]) max[k] = p;
            }
            if (hasTc) {
                // a vertex with no tcoords in a textured mesh (none exist in the corpus, but be
                // faithful to setInBuffers, which writes (0,0)); reconstruction keys off hasTcoords
                tc[ii * 2] = (v.tcoords != null) ? v.tcoords[0] : 0f;
                tc[ii * 2 + 1] = (v.tcoords != null) ? v.tcoords[1] : 0f;
            }
        }
        int posAcc = addFloatAccessor(pos, 3, "VEC3", min, max, ARRAY_BUFFER);
        int nrmAcc = addFloatAccessor(nrm, 3, "VEC3", null, null, ARRAY_BUFFER);
        Integer tcAcc = hasTc ? addFloatAccessor(tc, 2, "VEC2", null, null, ARRAY_BUFFER) : null;

        // indices
        int nidx = tdef.indices.size();
        int[] idx = new int[nidx];
        for (int ii = 0; ii < nidx; ii++) {
            idx[ii] = tdef.indices.get(ii);
        }
        int idxAcc = addIntAccessor(idx, ELEMENT_ARRAY_BUFFER);

        Map<String,Object> attributes = new LinkedHashMap<>();
        attributes.put("POSITION", posAcc);
        attributes.put("NORMAL", nrmAcc);
        if (tcAcc != null) attributes.put("TEXCOORD_0", tcAcc);

        // skinning attributes (may span multiple sets)
        if (tdef instanceof SkinMeshDef sdef) {
            int sets = addSkinAttributes(sdef, attributes, result);
            result.maxInfluences = Math.max(result.maxInfluences, sets * 4);
        }

        Map<String,Object> primitive = new LinkedHashMap<>();
        primitive.put("attributes", attributes);
        primitive.put("indices", idxAcc);
        primitive.put("mode", 4); // TRIANGLES
        primitive.put("material", buildMaterial(tdef));

        Map<String,Object> mesh = new LinkedHashMap<>();
        mesh.put("name", tdef.name);
        mesh.put("primitives", List.of(primitive));
        _meshes.add(mesh);
        return _meshes.size() - 1;
    }

    /** The lossless valve: mxml render flags / raw texture / tcoord presence / offset transform,
     * written onto the spatial's NODE extras (so empty triMeshes — which have no glTF mesh — keep
     * them too). */
    protected static void putMeshExtras (Map<String,Object> extras, TriMeshDef tdef)
    {
        extras.put(MxmlGltfConventions.EXTRA_SOLID, tdef.solid);
        extras.put(MxmlGltfConventions.EXTRA_TRANSPARENT, tdef.transparent);
        extras.put(MxmlGltfConventions.EXTRA_HAS_TCOORDS, tdef.tcoords);
        if (tdef.texture != null) {
            extras.put(MxmlGltfConventions.EXTRA_TEXTURE, tdef.texture);
        }
        if (tdef.offsetTranslation != null && !isZero(tdef.offsetTranslation)) {
            extras.put(MxmlGltfConventions.EXTRA_OFFSET_TRANSLATION,
                floatArray(tdef.offsetTranslation));
        }
        if (tdef.offsetRotation != null && !isIdentityQuat(tdef.offsetRotation)) {
            extras.put(MxmlGltfConventions.EXTRA_OFFSET_ROTATION, floatArray(tdef.offsetRotation));
        }
        if (tdef.offsetScale != null && !isOne(tdef.offsetScale)) {
            extras.put(MxmlGltfConventions.EXTRA_OFFSET_SCALE, floatArray(tdef.offsetScale));
        }
    }

    /** Emits JOINTS_n/WEIGHTS_n covering EVERY influence of every vertex (up to 12 in the corpus),
     * in ceil(maxInfluences/4) sets. Returns the number of sets. The joint indices are into this
     * mesh's skin.joints, resolved in {@link #buildSkin}. */
    protected int addSkinAttributes (
        SkinMeshDef sdef, Map<String,Object> attributes, Result result)
    {
        // per-vertex ordered influence lists (bone name + weight), in the vertex's boneWeights
        // iteration order (a HashMap: deterministic for a given key set — matches the parser)
        int nverts = sdef.vertices.size();
        int maxInf = 0;
        List<List<String>> vbones = new ArrayList<>(nverts);
        List<List<Float>> vweights = new ArrayList<>(nverts);
        for (int ii = 0; ii < nverts; ii++) {
            SkinVertex sv = (SkinVertex)sdef.vertices.get(ii);
            List<String> bn = new ArrayList<>();
            List<Float> bw = new ArrayList<>();
            for (Map.Entry<String,BoneWeight> e : sv.boneWeights.entrySet()) {
                bn.add(e.getKey());
                bw.add(e.getValue().weight);
            }
            vbones.add(bn);
            vweights.add(bw);
            maxInf = Math.max(maxInf, bn.size());
        }
        // this mesh's skin joint order: weighted bones (first-appearance) then their ancestors
        LinkedHashSet<String> jointNames = new LinkedHashSet<>();
        for (List<String> bn : vbones) {
            for (String b : bn) {
                addWithAncestors(b, jointNames);
            }
        }
        // record the ordered joint names for buildSkin (called right after)
        _pendingSkinJoints = new ArrayList<>(jointNames);
        Map<String,Integer> jointOrder = new LinkedHashMap<>();
        for (String jn : _pendingSkinJoints) {
            jointOrder.put(jn, jointOrder.size());
        }

        int sets = Math.max(1, (maxInf + 3) / 4);
        for (int set = 0; set < sets; set++) {
            int[] ji = new int[nverts * 4];
            float[] jw = new float[nverts * 4];
            for (int ii = 0; ii < nverts; ii++) {
                List<String> bn = vbones.get(ii);
                List<Float> bw = vweights.get(ii);
                for (int slot = 0; slot < 4; slot++) {
                    int inf = set * 4 + slot;
                    if (inf < bn.size()) {
                        Integer jo = jointOrder.get(bn.get(inf));
                        ji[ii * 4 + slot] = (jo != null) ? jo : 0;
                        jw[ii * 4 + slot] = bw.get(inf);
                    } else {
                        ji[ii * 4 + slot] = 0;
                        jw[ii * 4 + slot] = 0f;
                    }
                }
            }
            int jiAcc = addUShortAccessor(ji, ARRAY_BUFFER);
            int jwAcc = addFloatAccessor(jw, 4, "VEC4", null, null, ARRAY_BUFFER);
            attributes.put("JOINTS_" + set, jiAcc);
            attributes.put("WEIGHTS_" + set, jwAcc);
        }
        return sets;
    }

    /** Builds a glTF skin for a skin mesh: joints = the ordered names captured by
     * {@link #addSkinAttributes}, inverseBindMatrices from the SAME bind-pose math MxmlToJ3o uses
     * (IBM_j = inverse(globalBind_j) * meshToModel; the wrapper rotation cancels, so POSITION stays
     * raw mxml-local and the round-trip is exact — the IBMs matter only to a glTF viewer). */
    protected int buildSkin (SkinMeshDef sdef, Map<String,Integer> nodeIndex, Result result)
    {
        List<String> jointNames = _pendingSkinJoints;
        List<Integer> joints = new ArrayList<>();
        for (String jn : jointNames) {
            Integer idx = nodeIndex.get(jn);
            joints.add(idx != null ? idx : 0);
        }
        Matrix4f meshToModel = globalBind(sdef.name).mult(offsetMatrix(sdef));
        float[] ibm = new float[jointNames.size() * 16];
        for (int jj = 0; jj < jointNames.size(); jj++) {
            Matrix4f jb = globalBind(jointNames.get(jj));
            Matrix4f m = jb.invert().multLocal(meshToModel);
            columnMajor(m, ibm, jj * 16);
        }
        int ibmAcc = addFloatAccessor(ibm, 16, "MAT4", null, null, null);

        Map<String,Object> skin = new LinkedHashMap<>();
        skin.put("inverseBindMatrices", ibmAcc);
        skin.put("joints", joints);
        _skins.add(skin);
        return _skins.size() - 1;
    }

    protected void addWithAncestors (String bone, LinkedHashSet<String> out)
    {
        // add the bone and every ancestor up to a root (mirrors MxmlToJ3o's ancestor closure)
        for (SpatialDef s = _byName.get(bone); s != null; s = _byName.get(s.parent)) {
            if (!out.add(s.name)) {
                // already present -> its ancestors are too
            }
            if (s.parent == null) break;
        }
    }

    /** The bind-pose model transform of a spatial (root contributes its own local; ancestors
     * compose down), using {@link ModelDef#setTransform}'s fork-exact quaternion math. */
    protected Matrix4f globalBind (String name)
    {
        Matrix4f cached = _globalBind.get(name);
        if (cached != null) {
            return cached.clone();
        }
        SpatialDef s = _byName.get(name);
        Matrix4f local = ModelDef.setTransform(
            new Vector3f(s.translation[0], s.translation[1], s.translation[2]),
            new Quaternion(s.rotation[0], s.rotation[1], s.rotation[2], s.rotation[3]),
            new Vector3f(s.scale[0], s.scale[1], s.scale[2]), new Matrix4f());
        Matrix4f world;
        if (s.parent != null && _byName.containsKey(s.parent)) {
            world = globalBind(s.parent).mult(local);
        } else {
            world = local;
        }
        _globalBind.put(name, world);
        return world.clone();
    }

    protected static Matrix4f offsetMatrix (TriMeshDef tdef)
    {
        Vector3f t = new Vector3f(), sc = new Vector3f(1f, 1f, 1f);
        Quaternion r = new Quaternion();
        if (tdef.offsetTranslation != null) {
            t.set(tdef.offsetTranslation[0], tdef.offsetTranslation[1], tdef.offsetTranslation[2]);
        }
        if (tdef.offsetRotation != null) {
            r.set(tdef.offsetRotation[0], tdef.offsetRotation[1], tdef.offsetRotation[2],
                tdef.offsetRotation[3]);
        }
        if (tdef.offsetScale != null) {
            sc.set(tdef.offsetScale[0], tdef.offsetScale[1], tdef.offsetScale[2]);
        }
        return ModelDef.setTransform(t, r, sc, new Matrix4f());
    }

    protected static void columnMajor (Matrix4f m, float[] out, int off)
    {
        // glTF matrices are column-major
        float[] rows = new float[16];
        m.get(rows, true); // row-major
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                out[off + c * 4 + r] = rows[r * 4 + c];
            }
        }
    }

    // ================================================================================
    // materials / textures (for viewing; not part of the round-trip contract)
    // ================================================================================

    protected int buildMaterial (TriMeshDef tdef)
    {
        Map<String,Object> mat = new LinkedHashMap<>();
        mat.put("name", tdef.name + "-mat");
        Map<String,Object> pbr = new LinkedHashMap<>();
        pbr.put("metallicFactor", 0);
        pbr.put("roughnessFactor", 1);
        if (tdef.texture != null) {
            Map<String,Object> bct = new LinkedHashMap<>();
            bct.put("index", textureFor(tdef.getTextureFilename()));
            pbr.put("baseColorTexture", bct);
        }
        mat.put("pbrMetallicRoughness", pbr);
        Map<String,Object> ext = new LinkedHashMap<>();
        ext.put("KHR_materials_unlit", new LinkedHashMap<String,Object>());
        mat.put("extensions", ext);
        mat.put("alphaMode", tdef.transparent ? "BLEND" : "OPAQUE");
        mat.put("doubleSided", !tdef.solid);
        _materials.add(mat);
        return _materials.size() - 1;
    }

    protected int textureFor (String filename)
    {
        // the URI is the texture *basename* (TriMeshDef.getTextureFilename, the same reduction the
        // real bake applies): the PNG sits next to model.mxml, and a few mxml sources still carry
        // leftover absolute authoring-machine paths (C:\projects\... with spaces/backslashes) in
        // their raw texture attribute that would be an invalid, non-resolvable URI. All 262 corpus
        // basenames are URI-safe (verified: [A-Za-z0-9._-] only). Reuse identical URIs so viewers
        // de-dupe. (The RAW texture string still rides EXTRA_TEXTURE for the lossless round-trip.)
        Integer existing = _textureByUri.get(filename);
        if (existing != null) {
            return existing;
        }
        Map<String,Object> image = new LinkedHashMap<>();
        image.put("uri", filename);
        _images.add(image);
        int imageIdx = _images.size() - 1;
        if (_defaultSampler < 0) {
            Map<String,Object> sampler = new LinkedHashMap<>();
            sampler.put("wrapS", 10497); // REPEAT
            sampler.put("wrapT", 10497);
            _samplers.add(sampler);
            _defaultSampler = _samplers.size() - 1;
        }
        Map<String,Object> tex = new LinkedHashMap<>();
        tex.put("source", imageIdx);
        tex.put("sampler", _defaultSampler);
        _textures.add(tex);
        int texIdx = _textures.size() - 1;
        _textureByUri.put(filename, texIdx);
        return texIdx;
    }

    // ================================================================================
    // animations
    // ================================================================================

    protected void buildAnimation (String name, AnimationDef adef)
    {
        int frames = adef.frames.size();
        int fps = adef.frameRate > 0 ? adef.frameRate : ModelMetadataFps();
        // dense per-frame times
        float[] times = new float[frames];
        for (int ff = 0; ff < frames; ff++) {
            times[ff] = (float)ff / fps;
        }

        // the union of animated node names, in first-appearance order, restricted to nodes that
        // exist in the model (phantom targets are skipped by both MxmlToJ3o.filterTransforms and
        // createAnim, so dropping them here is lossless — see GltfToModelDef)
        LinkedHashSet<String> animNodes = new LinkedHashSet<>();
        for (AnimationDef.FrameDef frame : adef.frames) {
            for (AnimationDef.TransformDef t : frame.transforms) {
                if (_byName.containsKey(t.name)) {
                    animNodes.add(t.name);
                }
            }
        }

        List<Map<String,Object>> channels = new ArrayList<>();
        List<Map<String,Object>> samplers = new ArrayList<>();
        int inputAcc = -1;
        if (frames > 0 && !animNodes.isEmpty()) {
            float[] tmin = { times[0] }, tmax = { times[frames - 1] };
            inputAcc = addFloatAccessor(times, 1, "SCALAR", tmin, tmax, null);
        }

        for (String nodeName : animNodes) {
            // gather per-frame TRS for this node (dense: present in every frame)
            float[] tr = new float[frames * 3];
            float[] ro = new float[frames * 4];
            float[] sc = new float[frames * 3];
            for (int ff = 0; ff < frames; ff++) {
                AnimationDef.TransformDef td = findTransform(adef.frames.get(ff), nodeName);
                if (td == null) {
                    throw new IllegalStateException("animation '" + name + "' is not dense: node '" +
                        nodeName + "' absent from frame " + ff);
                }
                tr[ff * 3] = td.translation[0];
                tr[ff * 3 + 1] = td.translation[1];
                tr[ff * 3 + 2] = td.translation[2];
                ro[ff * 4] = td.rotation[0];
                ro[ff * 4 + 1] = td.rotation[1];
                ro[ff * 4 + 2] = td.rotation[2];
                ro[ff * 4 + 3] = td.rotation[3];
                sc[ff * 3] = td.scale[0];
                sc[ff * 3 + 1] = td.scale[1];
                sc[ff * 3 + 2] = td.scale[2];
            }
            int node = _pendingNodeIndex.get(nodeName);
            addChannel(channels, samplers, inputAcc, node, "translation",
                addFloatAccessor(tr, 3, "VEC3", null, null, null));
            addChannel(channels, samplers, inputAcc, node, "rotation",
                addFloatAccessor(ro, 4, "VEC4", null, null, null));
            addChannel(channels, samplers, inputAcc, node, "scale",
                addFloatAccessor(sc, 3, "VEC3", null, null, null));
        }

        if (channels.isEmpty()) {
            // an animation whose every target is a phantom node (absent from the model), or which
            // has no frames, produces no keyframe data. glTF's schema requires animation.channels
            // with minItems:1, so emitting such an object yields a spec-invalid file a validator /
            // Blender rejects. Drop it — MxmlToJ3o drops the same clip (droppedAnimations), so this
            // matches the oracle. (Not triggered by the current corpus; a future/edited model with
            // an all-phantom animation would surface here as a round-trip animName mismatch.)
            System.out.println("  note: dropped empty (no-channel) animation '" + name + "'");
            return;
        }
        Map<String,Object> anim = new LinkedHashMap<>();
        anim.put("name", name);
        anim.put("channels", channels);
        anim.put("samplers", samplers);
        Map<String,Object> extras = new LinkedHashMap<>();
        extras.put(MxmlGltfConventions.EXTRA_FRAME_RATE, adef.frameRate);
        extras.put("frameCount", frames);
        anim.put("extras", extras);
        _animations.add(anim);
    }

    protected static int ModelMetadataFps ()
    {
        return com.threerings.jme.model.ModelMetadata.DEFAULT_FRAME_RATE;
    }

    protected static AnimationDef.TransformDef findTransform (
        AnimationDef.FrameDef frame, String name)
    {
        for (AnimationDef.TransformDef t : frame.transforms) {
            if (name.equals(t.name)) {
                return t;
            }
        }
        return null;
    }

    protected void addChannel (
        List<Map<String,Object>> channels, List<Map<String,Object>> samplers,
        int inputAcc, int node, String path, int outputAcc)
    {
        Map<String,Object> sampler = new LinkedHashMap<>();
        sampler.put("input", inputAcc);
        sampler.put("output", outputAcc);
        sampler.put("interpolation", "LINEAR");
        samplers.add(sampler);
        Map<String,Object> channel = new LinkedHashMap<>();
        channel.put("sampler", samplers.size() - 1);
        Map<String,Object> target = new LinkedHashMap<>();
        target.put("node", node);
        target.put("path", path);
        channel.put("target", target);
        channels.add(channel);
    }

    // ================================================================================
    // accessor / bufferView / bin plumbing
    // ================================================================================

    protected int addFloatAccessor (
        float[] data, int comps, String type, float[] min, float[] max, Integer target)
    {
        ByteBuffer bb = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : data) {
            bb.putFloat(f);
        }
        int bv = addBufferView(bb.array(), target);
        Map<String,Object> acc = new LinkedHashMap<>();
        acc.put("bufferView", bv);
        acc.put("componentType", 5126); // FLOAT
        acc.put("count", data.length / comps);
        acc.put("type", type);
        if (min != null) acc.put("min", min);
        if (max != null) acc.put("max", max);
        _accessors.add(acc);
        return _accessors.size() - 1;
    }

    protected int addUShortAccessor (int[] data, Integer target)
    {
        ByteBuffer bb = ByteBuffer.allocate(data.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int v : data) {
            bb.putShort((short)(v & 0xFFFF));
        }
        int bv = addBufferView(bb.array(), target);
        Map<String,Object> acc = new LinkedHashMap<>();
        acc.put("bufferView", bv);
        acc.put("componentType", 5123); // UNSIGNED_SHORT
        acc.put("count", data.length / 4);
        acc.put("type", "VEC4");
        _accessors.add(acc);
        return _accessors.size() - 1;
    }

    protected int addIntAccessor (int[] data, Integer target)
    {
        ByteBuffer bb = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int v : data) {
            bb.putInt(v);
        }
        int bv = addBufferView(bb.array(), target);
        Map<String,Object> acc = new LinkedHashMap<>();
        acc.put("bufferView", bv);
        acc.put("componentType", 5125); // UNSIGNED_INT
        acc.put("count", data.length);
        acc.put("type", "SCALAR");
        _accessors.add(acc);
        return _accessors.size() - 1;
    }

    protected int addBufferView (byte[] bytes, Integer target)
    {
        // align every bufferView to 4 bytes (covers float/uint/ushort component alignment)
        while ((_bin.size() & 3) != 0) {
            _bin.write(0);
        }
        int offset = _bin.size();
        _bin.writeBytes(bytes);
        Map<String,Object> bv = new LinkedHashMap<>();
        bv.put("buffer", 0);
        bv.put("byteOffset", offset);
        bv.put("byteLength", bytes.length);
        if (target != null) {
            bv.put("target", target);
        }
        _bufferViews.add(bv);
        return _bufferViews.size() - 1;
    }

    // ================================================================================
    // tiny JSON serializer (lossless float formatting; no external dependency)
    // ================================================================================

    protected static void writeJson (Object o, StringBuilder sb, int indent)
    {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Map<?,?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?,?> e : m.entrySet()) {
                pad(sb, indent + 1);
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                writeJson(e.getValue(), sb, indent + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append('}');
        } else if (o instanceof List<?> list) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            // inline short numeric arrays for readability + smaller files
            boolean scalarish = list.stream().allMatch(
                x -> x instanceof Number || x instanceof Boolean || x instanceof String);
            if (scalarish && list.size() <= 16) {
                sb.append('[');
                for (int j = 0; j < list.size(); j++) {
                    if (j > 0) sb.append(", ");
                    writeJson(list.get(j), sb, indent);
                }
                sb.append(']');
            } else {
                sb.append("[\n");
                for (int j = 0; j < list.size(); j++) {
                    pad(sb, indent + 1);
                    writeJson(list.get(j), sb, indent + 1);
                    if (j + 1 < list.size()) sb.append(',');
                    sb.append('\n');
                }
                pad(sb, indent);
                sb.append(']');
            }
        } else if (o instanceof float[] fa) {
            sb.append('[');
            for (int j = 0; j < fa.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(fmt(fa[j]));
            }
            sb.append(']');
        } else if (o instanceof int[] ia) {
            sb.append('[');
            for (int j = 0; j < ia.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(ia[j]);
            }
            sb.append(']');
        } else if (o instanceof Float f) {
            sb.append(fmt(f));
        } else if (o instanceof Double d) {
            sb.append(fmt(d.floatValue()));
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o.toString());
        } else if (o instanceof String s) {
            writeString(s, sb);
        } else {
            throw new IllegalArgumentException("cannot serialize " + o.getClass());
        }
    }

    /** Lossless float text: Float.toString produces the shortest decimal that parses back to the
     * identical float (so node TRS survive the round-trip bit-for-bit). */
    protected static String fmt (float f)
    {
        if (Float.isNaN(f) || Float.isInfinite(f)) {
            throw new IllegalArgumentException("non-finite float cannot be JSON-encoded: " + f);
        }
        return Float.toString(f);
    }

    protected static void writeString (String s, StringBuilder sb)
    {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    protected static void pad (StringBuilder sb, int indent)
    {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    protected static boolean isZero (float[] v)
    {
        for (float f : v) if (f != 0f) return false;
        return true;
    }

    protected static boolean isOne (float[] v)
    {
        for (float f : v) if (f != 1f) return false;
        return true;
    }

    protected static boolean isIdentityQuat (float[] q)
    {
        return q.length == 4 && q[0] == 0f && q[1] == 0f && q[2] == 0f && q[3] == 1f;
    }

    protected static float[] floatArray (float[] a)
    {
        return a.clone();
    }

    protected static float[] quatArray (Quaternion q)
    {
        return new float[] { q.getX(), q.getY(), q.getZ(), q.getW() };
    }

    // per-write state
    protected ByteArrayOutputStream _bin;
    protected List<Map<String,Object>> _accessors, _bufferViews, _meshes, _materials, _textures,
        _images, _samplers, _skins, _animations;
    protected Map<String,SpatialDef> _byName;
    protected Map<String,Matrix4f> _globalBind;
    protected Map<String,Integer> _textureByUri = new LinkedHashMap<>();
    protected int _defaultSampler = -1;
    protected List<String> _pendingSkinJoints;
    protected Map<String,Integer> _pendingNodeIndex;

    static final int ARRAY_BUFFER = 34962;
    static final int ELEMENT_ARRAY_BUFFER = 34963;
}
