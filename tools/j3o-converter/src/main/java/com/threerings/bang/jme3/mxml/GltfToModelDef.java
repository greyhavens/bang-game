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
 * The Epic 4 Phase 2b-ii glTF &rarr; {@link ModelDef} reader: the <b>exact inverse</b> of
 * {@link MxmlToGltf}. It reads {@code model.gltf}+{@code model.bin} and reconstructs a
 * {@link ModelDef} + {@code Map<String,AnimationDef>} bit-identical (for every field
 * {@link MxmlToJ3o#bake} reads) to what {@link ModelParser}/{@link AnimationParser} produced — so
 * feeding the reconstruction back through {@code MxmlToJ3o.bake(props, mdef, animNames, adefs)}
 * yields the same native {@code .j3o} as the direct {@code .mxml} path (verified by
 * {@link MxmlGltfParity}). This is also the intended <b>build-side</b> reader for the Phase-4
 * Blender cutover (so an unedited migrated model bakes identically by construction).
 *
 * <p>Reconstruction correctness rests on a few deliberate points, mirroring the writer:
 * <ul>
 *   <li><b>Axis convention:</b> the synthetic wrapper node
 *       ({@link MxmlGltfConventions#EXTRA_WRAPPER}) is discarded and its children adopted as roots
 *       — which realises {@link MxmlGltfConventions#GLTF_TO_MXML} losslessly (the mxml node TRS
 *       were stored verbatim under the wrapper). Node TRS numbers are parsed with
 *       {@code Float.parseFloat} on the original token text (not via {@code double}) so they match
 *       the parser's floats bit-for-bit — critical for the mesh-merge 1e-4 epsilon decisions.</li>
 *   <li><b>Vertices/indices:</b> restored 1:1 in stored order into a fresh
 *       {@link ModelDef.TriMeshDef} (the geometry lives as float32 in {@code model.bin}, exact).</li>
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

        // resolve the binary buffer (single buffer, uri = sibling file)
        List<Object> buffers = (List<Object>)gltf.get("buffers");
        Map<String,Object> buffer = (Map<String,Object>)buffers.get(0);
        String uri = (String)buffer.get("uri");
        _bin = Files.readAllBytes(new File(gltfFile.getParentFile(), uri).toPath());

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
        sdef.translation = readTrs(node.get("translation"), 0f, 0f, 0f);
        sdef.rotation = readTrs(node.get("rotation"), 0f, 0f, 0f, 1f);
        sdef.scale = readTrs(node.get("scale"), 1f, 1f, 1f);

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

            Object meshRef = node.get("mesh");
            if (meshRef != null) {
                readGeometry(tdef, intVal(meshRef), node, hasTc);
            } else {
                tdef.tcoords = hasTc; // empty mesh: no vertices, but preserve the flag
            }
        }
        return sdef;
    }

    @SuppressWarnings("unchecked")
    protected void readGeometry (TriMeshDef tdef, int meshIdx, Map<String,Object> node,
        boolean hasTc)
    {
        Map<String,Object> mesh = (Map<String,Object>)_meshes.get(meshIdx);
        Map<String,Object> prim =
            (Map<String,Object>)((List<Object>)mesh.get("primitives")).get(0);
        Map<String,Object> attrs = (Map<String,Object>)prim.get("attributes");

        float[] pos = readFloats(intVal(attrs.get("POSITION")));
        // read the RAW normals from _MXMLNORMAL (the standard NORMAL is a normalised copy for
        // viewers/Blender; the bake's vertex dedup / merge keys on the exact raw normal, so the
        // round-trip must use the raw values — see MxmlToGltf). Fall back to NORMAL for any glTF
        // written before _MXMLNORMAL existed.
        Object rawNrm = attrs.get("_MXMLNORMAL");
        float[] nrm = readFloats(intVal(rawNrm != null ? rawNrm : attrs.get("NORMAL")));
        float[] tc = hasTc ? readFloats(intVal(attrs.get("TEXCOORD_0"))) : null;
        int nverts = pos.length / 3;

        // skin joint names (local skin index -> bone node name)
        String[] jointNames = null;
        List<int[]> jointSets = new ArrayList<>();
        List<float[]> weightSets = new ArrayList<>();
        boolean isSkin = tdef instanceof SkinMeshDef;
        if (isSkin) {
            List<Object> joints = (List<Object>)((Map<String,Object>)_skins.get(
                intVal(node.get("skin")))).get("joints");
            jointNames = new String[joints.size()];
            for (int jj = 0; jj < joints.size(); jj++) {
                jointNames[jj] = (String)node(intVal(joints.get(jj))).get("name");
            }
            for (int set = 0; attrs.containsKey("JOINTS_" + set); set++) {
                jointSets.add(readUShorts(intVal(attrs.get("JOINTS_" + set))));
                weightSets.add(readFloats(intVal(attrs.get("WEIGHTS_" + set))));
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
        int[] idx = readUInts(intVal(prim.get("indices")));
        for (int i : idx) {
            tdef.indices.add(i);
        }
        tdef.tcoords = hasTc;
    }

    // ================================================================================
    // animations
    // ================================================================================

    @SuppressWarnings("unchecked")
    protected AnimationDef readAnimation (Map<String,Object> anim)
    {
        Map<String,Object> extras = (Map<String,Object>)anim.get("extras");
        AnimationDef adef = new AnimationDef();
        adef.frameRate = intVal(extras.get(MxmlGltfConventions.EXTRA_FRAME_RATE));
        int frameCount = intVal(extras.get("frameCount"));

        // per animated node: its translation/rotation/scale output accessors, in channel
        // first-appearance order
        List<String> order = new ArrayList<>();
        Map<String,int[]> accByNode = new LinkedHashMap<>(); // [T,R,S] accessor indices
        List<Object> channels = (List<Object>)anim.get("channels");
        List<Object> samplers = (List<Object>)anim.get("samplers");
        if (channels != null) {
            for (Object c : channels) {
                Map<String,Object> ch = (Map<String,Object>)c;
                Map<String,Object> target = (Map<String,Object>)ch.get("target");
                String nodeName = (String)node(intVal(target.get("node"))).get("name");
                String path = (String)target.get("path");
                Map<String,Object> sampler = (Map<String,Object>)samplers.get(
                    intVal(ch.get("sampler")));
                int outAcc = intVal(sampler.get("output"));
                int[] trs = accByNode.get(nodeName);
                if (trs == null) {
                    trs = new int[] { -1, -1, -1 };
                    accByNode.put(nodeName, trs);
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

        // read all output data
        Map<String,float[]> tData = new LinkedHashMap<>(), rData = new LinkedHashMap<>(),
            sData = new LinkedHashMap<>();
        for (String name : order) {
            int[] trs = accByNode.get(name);
            tData.put(name, readFloats(trs[0]));
            rData.put(name, readFloats(trs[1]));
            sData.put(name, readFloats(trs[2]));
        }

        for (int ff = 0; ff < frameCount; ff++) {
            FrameDef frame = new FrameDef();
            for (String name : order) {
                TransformDef td = new TransformDef();
                td.name = name;
                float[] t = tData.get(name), r = rData.get(name), s = sData.get(name);
                td.translation = new float[] { t[ff * 3], t[ff * 3 + 1], t[ff * 3 + 2] };
                td.rotation = new float[] { r[ff * 4], r[ff * 4 + 1], r[ff * 4 + 2], r[ff * 4 + 3] };
                td.scale = new float[] { s[ff * 3], s[ff * 3 + 1], s[ff * 3 + 2] };
                frame.addTransform(td);
            }
            adef.addFrame(frame);
        }
        return adef;
    }

    // ================================================================================
    // accessor reading (from model.bin)
    // ================================================================================

    @SuppressWarnings("unchecked")
    protected float[] readFloats (int accIdx)
    {
        Map<String,Object> acc = (Map<String,Object>)_accessors.get(accIdx);
        int comps = compsOf((String)acc.get("type"));
        int count = intVal(acc.get("count"));
        ByteBuffer bb = bufferFor(acc);
        float[] out = new float[count * comps];
        for (int ii = 0; ii < out.length; ii++) {
            out[ii] = bb.getFloat();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    protected int[] readUShorts (int accIdx)
    {
        Map<String,Object> acc = (Map<String,Object>)_accessors.get(accIdx);
        int comps = compsOf((String)acc.get("type"));
        int count = intVal(acc.get("count"));
        ByteBuffer bb = bufferFor(acc);
        int[] out = new int[count * comps];
        for (int ii = 0; ii < out.length; ii++) {
            out[ii] = bb.getShort() & 0xFFFF;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    protected int[] readUInts (int accIdx)
    {
        Map<String,Object> acc = (Map<String,Object>)_accessors.get(accIdx);
        int count = intVal(acc.get("count"));
        ByteBuffer bb = bufferFor(acc);
        int[] out = new int[count];
        for (int ii = 0; ii < out.length; ii++) {
            out[ii] = bb.getInt();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    protected ByteBuffer bufferFor (Map<String,Object> acc)
    {
        Map<String,Object> bv = (Map<String,Object>)_bufferViews.get(intVal(acc.get("bufferView")));
        int off = bv.containsKey("byteOffset") ? intVal(bv.get("byteOffset")) : 0;
        int len = intVal(bv.get("byteLength"));
        return ByteBuffer.wrap(_bin, off, len).order(ByteOrder.LITTLE_ENDIAN);
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
