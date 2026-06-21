//
// $Id$

package com.threerings.bang.jme3.model;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import com.threerings.jme.tools.model.Model;
import com.threerings.jme.model.ModelTextureResolver;
import com.threerings.jme.tools.model.ModelAnimAccess;
import com.threerings.jme.tools.model.ModelMesh;
import com.threerings.jme.tools.model.ModelMeshAccess;
import com.threerings.jme.tools.model.ModelNode;
import com.threerings.jme.tools.model.SkinMesh;

/**
 * Converts a loaded fork {@link Model} (the vendored jME-fork scenegraph deserialized from
 * a {@code model.dat}) into a jMonkeyEngine&nbsp;3 {@link Spatial} tree.
 *
 * <p>This is the shared conversion engine used both by the runtime {@link BangModelLoader}
 * (an {@code AssetManager} loader) and by the {@code ModelToJ3o} build-time exporter. It is
 * deliberately self-contained and headless: it never touches GL state, so it runs against a
 * {@code DummyDisplaySystem} just like the model compiler.
 *
 * <p>What it converts:
 * <ul>
 *   <li><b>Geometry</b> — position/normal/single-UV/index buffers copied straight from each
 *       {@link ModelMesh} (the fork already Forsyth-optimised them).</li>
 *   <li><b>Hierarchy + transforms</b> — every {@link ModelNode} becomes a jME3 {@link Node}
 *       carrying the fork local TRS.</li>
 *   <li><b>Materials/textures</b> — a placeholder {@code Lighting.j3md} material with the
 *       default (first) texture resolved through {@link ModelTextureResolver}; the full
 *       multi-valued texture list and render flags are preserved (see that class).</li>
 *   <li><b>Rigid keyframe animation</b> — each fork {@link Model.Animation} becomes one
 *       {@link AnimClip} of {@link TransformTrack}s targeting the moving {@link Node}s, on an
 *       {@link AnimComposer} attached to the root.</li>
 *   <li><b>Skinning</b> — {@link SkinMesh} weight groups become 4-influence
 *       {@code BoneIndex}/{@code BoneWeight} buffers over an {@link Armature} of {@link Joint}s
 *       (the referenced bone {@link ModelNode}s), driven by a {@link SkinningControl}; the same
 *       animation clips drive the joints.</li>
 * </ul>
 *
 * <p>What it reports but does not convert: procedural/emission controllers (deferred to the
 * effects port) and non-mesh procedural geometry (e.g. {@code ParticleMesh}).
 */
public class ModelConverter
{
    /** Per-geometry statistics used for parity verification. */
    /**
     * Per-geometry fidelity snapshot used for round-trip parity. Beyond mesh counts it captures
     * the material/skinning state that survives the {@code .j3o} round trip, so a dropped glow
     * map, flipped cull/blend flag, lost transparent bucket, or missing skinning buffer is caught
     * (the earlier count-and-base-texture-only check passed all of those silently).
     */
    public record GeoStats (String path, int vertices, int triangles, String texture,
                            String glowTexture, String blend, boolean cullOff,
                            boolean transparentBucket, int maxWeights) { }

    /** Extracts a {@link GeoStats} from a jME3 geometry — used identically on the freshly
     * converted graph and on the re-imported {@code .j3o}, so parity compares like with like. */
    public static GeoStats statsOf (String path, Geometry geom)
    {
        Mesh m = geom.getMesh();
        Material mat = geom.getMaterial();
        return new GeoStats(path, m.getVertexCount(), m.getTriangleCount(),
            texName(mat, "DiffuseMap"), texName(mat, "GlowMap"),
            mat.getAdditionalRenderState().getBlendMode().name(),
            mat.getAdditionalRenderState().getFaceCullMode() == FaceCullMode.Off,
            geom.getLocalQueueBucket() == Bucket.Transparent,
            m.getMaxNumWeights());
    }

    private static String texName (Material mat, String param)
    {
        MatParamTexture p = mat.getTextureParam(param);
        return (p == null || p.getTextureValue() == null || p.getTextureValue().getKey() == null)
            ? null : p.getTextureValue().getKey().getName();
    }

    /** The outcome of one conversion, with everything a caller needs to verify coverage. */
    public static class Result
    {
        /** The converted jME3 scene graph root. */
        public Node root;

        /** Per-geometry stats in traversal order (for parity checks). */
        public final List<GeoStats> stats = new ArrayList<>();

        /** Names of animations converted to {@link AnimClip}s. */
        public final List<String> animations = new ArrayList<>();

        /** Animations present in the model but NOT converted (no transforms / no live target). */
        public final List<String> droppedAnimations = new ArrayList<>();

        /** Number of {@link SkinMesh}es converted with full skinning. */
        public int skinnedMeshes;

        /** Number of geometries in the model (rigid + skinned + static). */
        public int geometries;

        /** Controller class names detected and NOT converted (deferred to effects port). */
        public final List<String> droppedControllers = new ArrayList<>();

        /** Non-mesh procedural spatials skipped (e.g. ParticleMesh). */
        public final List<String> skippedSpatials = new ArrayList<>();

        /** True if any SkinMesh vertex needed >4 influences (clamped + renormalised). */
        public boolean clampedInfluences;

        /** Number of joints in the converted armature (0 if not skinned). */
        public int armatureJoints;

        /** Total TransformTracks across all converted AnimClips. */
        public int animTracks;

        /** Names of nodes that are BOTH an armature Joint AND a converted Node carrying rigid
         * geometry descendants (the homestead-float at-risk pattern: such a node needs its
         * keyframes to drive both targets, which {@link #resolveTrackTargets} now does). Reported
         * for the cross-model audit. */
        public final List<String> dualTargetGeoNodes = new ArrayList<>();

        public boolean isAnimated () { return !animations.isEmpty(); }
        public boolean isSkinned () { return skinnedMeshes > 0; }
    }

    /**
     * @param resolver resolves stored texture names to jME3 assets (the ModelCache-equivalent).
     * @param typePath the model's directory relative to the rsrc root (e.g.
     * {@code units/frontier_town/gunslinger}); used to resolve relative texture names.
     */
    public ModelConverter (ModelTextureResolver resolver, String typePath)
    {
        _resolver = resolver;
        _typePath = typePath;
    }

    /**
     * Converts the supplied fork model into a jME3 scene graph, returning a {@link Result}
     * describing exactly what was and was not converted.
     */
    public Result convert (Model model)
    {
        Result result = new Result();

        // 1. identify the bone nodes (those referenced by any SkinMesh weight group) so we
        // know which ModelNodes also need a Joint mirror in the armature.
        collectBoneNodes(model);

        // 2. build the scene-graph Node/Geometry tree, recording the fork-node -> jME3-node
        // mapping so animation tracks and skinning can resolve their targets.
        result.root = (Node)convertSpatial(model, "", result);

        // 3. build the armature (if any bones) and skinning controls.
        if (!_boneNodes.isEmpty()) {
            buildArmature();
            applySkinning(result);
            if (_armature != null) {
                result.armatureJoints = _armature.getJointCount();
            }
        }

        // 4. convert animations to AnimClips on an AnimComposer.
        convertAnimations(model, result);

        // 5. detect (but do not convert) procedural controllers.
        for (Object ctrl : model.getControllers()) {
            result.droppedControllers.add(ctrl.getClass().getName());
        }

        // 6. preserve fork-only metadata as user data (round-trip safety / runtime sprites).
        Properties props = model.getProperties();
        StringBuilder buf = new StringBuilder();
        for (String key : props.stringPropertyNames().stream().sorted().toList()) {
            buf.append(key).append(" = ").append(props.getProperty(key)).append('\n');
        }
        result.root.setUserData("bang.properties", buf.toString());
        result.root.setUserData("bang.animations", String.join(",", model.getAnimationNames()));
        result.root.setUserData("bang.type", _typePath);

        // audit: flag nodes that are BOTH a Joint AND a converted Node with rigid geometry
        // descendants (the homestead-float at-risk pattern).
        for (ModelNode bn : _joints.keySet()) {
            Spatial node = _nodeMap.get(bn);
            if (node != null && hasGeometryDescendant(node)) {
                result.dualTargetGeoNodes.add(bn.getName());
            }
        }

        return result;
    }

    /** Walks the model gathering every ModelNode used as a skinning bone. */
    protected void collectBoneNodes (com.jme.scene.Spatial spatial)
    {
        if (spatial instanceof SkinMesh skin) {
            SkinMesh.WeightGroup[] groups = ModelMeshAccess.weightGroups(skin);
            if (groups != null) {
                for (SkinMesh.WeightGroup group : groups) {
                    for (SkinMesh.Bone bone : group.bones) {
                        if (bone != null && bone.node != null) {
                            _boneNodes.put(bone.node, Boolean.TRUE);
                        }
                    }
                }
            }
        } else if (spatial instanceof com.jme.scene.Node node) {
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                collectBoneNodes(node.getChild(ii));
            }
        }
    }

    /** Recursively converts a fork spatial to jME3, recording the node mapping. */
    protected Spatial convertSpatial (
        com.jme.scene.Spatial spatial, String path, Result result)
    {
        String name = spatial.getName();
        String childPath = path.isEmpty() ? name : (path + "/" + name);
        Spatial converted;
        if (spatial instanceof ModelMesh mesh) {
            converted = convertMesh(mesh, childPath, result);
        } else if (spatial instanceof com.jme.scene.Node fnode) {
            Node node = new Node(name);
            for (int ii = 0, nn = fnode.getQuantity(); ii < nn; ii++) {
                Spatial child = convertSpatial(fnode.getChild(ii), childPath, result);
                if (child != null) {
                    node.attachChild(child);
                }
            }
            converted = node;
        } else {
            // procedural effect geometry (ParticleMesh, etc.); reproduced at runtime from
            // the model's controllers, not stored as convertible mesh data
            result.skippedSpatials.add(childPath + " (" + spatial.getClass().getName() + ")");
            return null;
        }
        copyTransform(spatial, converted);
        _nodeMap.put(spatial, converted);
        return converted;
    }

    /** Converts one fork mesh to a jME3 Geometry with a placeholder lit material. */
    protected Geometry convertMesh (ModelMesh fmesh, String path, Result result)
    {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, prepare(fmesh.getVertexBuffer(0)));
        FloatBuffer nbuf = fmesh.getNormalBuffer(0);
        if (nbuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, prepare(nbuf));
        }
        FloatBuffer tbuf = fmesh.getTextureBuffer(0, 0);
        if (tbuf != null) {
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, prepare(tbuf));
        }
        mesh.setBuffer(VertexBuffer.Type.Index, 3, copy(fmesh.getIndexBuffer(0)));

        boolean skinned = false;
        if (fmesh instanceof SkinMesh skin) {
            skinned = addSkinningBuffers(skin, mesh, result);
            if (skinned) {
                // The fork stores skin vertices in mesh-local space and deforms them with bone
                // matrices framed by the mesh's inverse model-ref transform; jME3 skins vertices
                // in the armature/model (root) frame. Bake the vertices into model space here
                // (vertex' = inverse(meshInvRef) * vertex) and let the geometry sit at the root
                // with an identity transform (see applySkinning) so the joint model * inverseBind
                // offset matrices deform them directly — matching the fork's posed result.
                com.jme.math.Matrix4f invRef = ModelMeshAccess.invRefTransform(skin);
                if (invRef != null) {
                    com.jme.math.Matrix4f toModel = invRef.invert();
                    transformPositions(mesh.getFloatBuffer(VertexBuffer.Type.Position), toModel);
                    transformNormals(mesh.getFloatBuffer(VertexBuffer.Type.Normal), toModel);
                }
            }
        }

        mesh.updateBound();

        Geometry geom = new Geometry(fmesh.getName(), mesh);

        // material: placeholder Lighting.j3md, textures resolved via the ModelCache-equivalent
        String[] textures = ModelMeshAccess.textures(fmesh);
        _resolver.applyMaterial(
            geom, _typePath, ModelMeshAccess.textureKey(fmesh), textures,
            ModelMeshAccess.solid(fmesh), ModelMeshAccess.additive(fmesh),
            ModelMeshAccess.transparent(fmesh), ModelMeshAccess.alphaThreshold(fmesh),
            ModelMeshAccess.emissive(fmesh), ModelMeshAccess.emissiveMap(fmesh),
            ModelMeshAccess.sphereMapped(fmesh));

        result.geometries++;
        result.stats.add(statsOf(path, geom));
        if (skinned) {
            result.skinnedMeshes++;
            _skinnedMeshes.add(geom);
        }
        return geom;
    }

    /**
     * Builds the 4-influence {@code BoneIndex}/{@code BoneWeight} buffers for a skin mesh from
     * the fork's variable-width weight groups, and registers the mesh's bone-index ordering so
     * the armature can be wired. Returns true if the mesh was given skinning buffers.
     *
     * <p>The fork stores vertices pre-sorted into contiguous runs (weight groups), each group
     * naming its own bone set with interleaved weights. The deformer walks groups in order and
     * the vertex buffer is laid out in that same order, so weight-group vertex N maps to mesh
     * vertex N linearly. We expand each group's variable bone count into jME3's fixed 4 slots
     * (keeping the 4 highest weights, renormalised) and index into a per-mesh global bone list.
     */
    protected boolean addSkinningBuffers (SkinMesh skin, Mesh mesh, Result result)
    {
        SkinMesh.WeightGroup[] groups = ModelMeshAccess.weightGroups(skin);
        if (groups == null || groups.length == 0) {
            return false;
        }
        int vertexCount = mesh.getVertexCount();

        // per-mesh ordered bone list (ModelNode -> local index), captured for armature wiring
        Map<ModelNode, Integer> boneIndex = new LinkedHashMap<>();
        List<ModelNode> boneOrder = new ArrayList<>();

        // UnsignedShort (not UnsignedByte) so armatures with >255 joints don't wrap mod 256;
        // current content tops out ~31 bones/mesh, but jME3 supports short bone indices natively
        ShortBuffer indices = BufferUtils.createShortBuffer(vertexCount * 4);
        FloatBuffer weights = BufferUtils.createFloatBuffer(vertexCount * 4);

        int[] oi = new int[4];   // top-4 index/weight scratch, reused across all vertices
        float[] ow = new float[4];

        int vert = 0;
        for (SkinMesh.WeightGroup group : groups) {
            int gbones = group.bones.length;
            // a group's bone set (and thus its global bone indices) is constant for every
            // vertex in the group, so resolve it once instead of per vertex
            int[] gi = new int[gbones];
            for (int bb = 0; bb < gbones; bb++) {
                // mirror collectBoneNodes' guard: ModelDef.resolveReferences can leave a
                // Bone (or its node) null for an unresolved bone name; pin to joint 0
                // rather than NPEing or polluting the bone order with null
                SkinMesh.Bone bone = group.bones[bb];
                ModelNode node = (bone != null) ? bone.node : null;
                if (node == null) {
                    gi[bb] = 0;
                    continue;
                }
                Integer idx = boneIndex.get(node);
                if (idx == null) {
                    idx = boneOrder.size();
                    boneIndex.put(node, idx);
                    boneOrder.add(node);
                }
                gi[bb] = idx;
            }
            float[] gw = new float[gbones];      // scratch, refilled per vertex in this group
            boolean[] used = new boolean[gbones]; // selection-sort scratch, reused per vertex
            for (int vv = 0; vv < group.vertexCount; vv++, vert++) {
                for (int bb = 0; bb < gbones; bb++) {
                    gw[bb] = group.weights[vv * gbones + bb];
                }
                writeVertexInfluences(gi, gw, used, oi, ow, indices, weights, result);
            }
        }

        // jME3 wants weights normalised and a declared max influence count
        indices.flip();
        weights.flip();
        mesh.setMaxNumWeights(4);

        VertexBuffer ib = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        ib.setupData(VertexBuffer.Usage.CpuOnly, 4, VertexBuffer.Format.UnsignedShort, indices);
        mesh.setBuffer(ib);
        VertexBuffer wb = new VertexBuffer(VertexBuffer.Type.BoneWeight);
        wb.setupData(VertexBuffer.Usage.CpuOnly, 4, VertexBuffer.Format.Float, weights);
        mesh.setBuffer(wb);

        _meshBoneOrder.put(mesh, boneOrder);
        return true;
    }

    /**
     * Reduces a vertex's variable-width influences to jME3's fixed 4 (highest weights,
     * renormalised) and appends them to the index/weight buffers.
     */
    protected void writeVertexInfluences (
        int[] gi, float[] gw, boolean[] used, int[] oi, float[] ow,
        ShortBuffer indices, FloatBuffer weights, Result result)
    {
        int n = gi.length;
        // selection sort the top-4 by descending weight (n is tiny)
        int keep = Math.min(4, n);
        if (n > 4) {
            result.clampedInfluences = true;
        }
        java.util.Arrays.fill(used, 0, n, false);
        float total = 0f;
        for (int slot = 0; slot < keep; slot++) {
            int best = -1;
            float bestW = -1f;
            for (int kk = 0; kk < n; kk++) {
                if (!used[kk] && gw[kk] > bestW) {
                    bestW = gw[kk];
                    best = kk;
                }
            }
            used[best] = true;
            oi[slot] = gi[best];
            ow[slot] = gw[best];
            total += gw[best];
        }
        // renormalise so the 4 (or fewer) kept weights sum to 1
        if (total <= 0f) {
            ow[0] = 1f; // degenerate: pin to first bone
            total = 1f;
        }
        for (int slot = 0; slot < 4; slot++) {
            // unused slots are written explicitly (oi/ow are reused scratch and may be stale)
            indices.put(slot < keep ? (short)(oi[slot] & 0xFFFF) : (short)0);
            weights.put(slot < keep ? ow[slot] / total : 0f);
        }
    }

    /**
     * Builds a jME3 {@link Armature} that reproduces the fork bone nodes' <em>full model-space
     * transforms</em>. The fork skin deformer accumulates a bone's model transform through the
     * whole ancestor chain (including non-bone {@link ModelNode}s) — see
     * {@link ModelNode#updateWorldVectors}. So a bone-to-nearest-bone joint hierarchy (as an
     * earlier cut did) silently drops every intermediate node's transform, contorting the rig.
     *
     * <p>Instead we build a {@link Joint} for every {@link ModelNode} that is a bone <em>or an
     * ancestor of a bone</em> (up to the model root), preserving the true parent links and each
     * node's fork local TRS. Joint model transforms then equal the fork node model transforms,
     * so jME3's {@code jointModel * inverseBind} offset matrices match the fork's
     * {@code Bcur * inverse(Bref)} skinning (the per-mesh model-space framing is handled by
     * pre-transforming the skin vertices into model space; see {@link #addSkinningBuffers}).
     */
    protected void buildArmature ()
    {
        // expand the bone set to its full ancestor closure so intermediate (non-bone) node
        // transforms are not dropped from the accumulated joint model transforms
        Map<ModelNode, Boolean> skeletonNodes = new LinkedHashMap<>();
        for (ModelNode bnode : _boneNodes.keySet()) {
            for (com.jme.scene.Spatial p = bnode; p instanceof ModelNode mn; p = p.getParent()) {
                if (skeletonNodes.put(mn, Boolean.TRUE) != null) {
                    break; // this node (and thus all its ancestors) already added
                }
            }
        }

        // create a Joint per skeleton node, carrying the fork local transform (so the joint
        // model transform, accumulated through the true hierarchy, equals the fork node's).
        //
        // CRITICAL reference-space rule: the fork measures all bone model transforms (and the skin
        // mesh's _invRefTransform used to bake the vertices into model space) in a frame where the
        // *model root's own local transform is dropped*. ModelNode.updateWorldVectors() sets the
        // model transform to parentModelTransform * localTransform only when the parent is itself a
        // ModelNode; for the topmost ModelNode (the Model root, whose parent is the scene/null) it
        // loads IDENTITY — discarding that node's local TRS. So Bref/Bcur and the baked skin
        // vertices both live in "root-as-identity" model space. jME3 accumulates a joint's model
        // transform from the armature root down, INCLUDING the root joint's own local transform, so
        // giving the root joint the Model's local TRS would conjugate every off-bind deformation by
        // that transform (jointModel(t) * inverse(jointModel(bind)) = R*X(t)*inverse(X_bind)*inv(R))
        // — coherent only at bind (offset == identity there), shattering as a clip advances.
        // Fix: a model-root skeleton node (parent not a ModelNode) gets an IDENTITY joint local
        // transform, exactly mirroring the fork. Then jME3's joint model transforms equal the fork
        // node model transforms at every joint, so jointModel*inverseBind == fork Bcur*inverse(Bref)
        // and the baked model-space vertices (root-as-identity) are deformed in the same frame.
        for (ModelNode node : skeletonNodes.keySet()) {
            Joint joint = new Joint(uniqueJointName(node.getName()));
            boolean isModelRoot = !(node.getParent() instanceof ModelNode);
            if (isModelRoot) {
                // fork: getModelTransform() == IDENTITY for the root ModelNode (local TRS dropped)
                joint.setLocalTransform(new com.jme3.math.Transform());
                _rootJoint = joint;
            } else {
                com.jme.math.Vector3f t = node.getLocalTranslation();
                com.jme.math.Quaternion r = node.getLocalRotation();
                com.jme.math.Vector3f s = node.getLocalScale();
                joint.setLocalTransform(new com.jme3.math.Transform(
                    new Vector3f(t.x, t.y, t.z),
                    new Quaternion(r.x, r.y, r.z, r.w),
                    new Vector3f(s.x, s.y, s.z)));
            }
            _joints.put(node, joint);
        }
        // parent each joint under its actual parent skeleton node (the true fork hierarchy)
        for (Map.Entry<ModelNode, Joint> e : _joints.entrySet()) {
            com.jme.scene.Spatial p = e.getKey().getParent();
            if (p instanceof ModelNode mn && _joints.containsKey(mn)) {
                _joints.get(mn).addChild(e.getValue());
            }
        }
        List<Joint> all = new ArrayList<>(_joints.values());
        _armature = new Armature(all.toArray(new Joint[0]));
        // The joints are positioned at the bind (reference) pose. saveBindPose() computes each
        // joint's inverseModelBindMatrix = inverse(jointModelTransform) — WITHOUT this they
        // default to identity and the skinning offset (jointModel * inverseBind) collapses to
        // the raw joint model transform, grossly contorting the rig. saveInitialPose() then
        // makes that same pose the animation reset pose.
        _armature.update();
        _armature.saveBindPose();
        _armature.saveInitialPose();
    }

    /** Attaches a SkinningControl with the armature to each skinned mesh's geometry. */
    protected void applySkinning (Result result)
    {
        if (_armature == null) {
            return;
        }
        for (Geometry geom : _skinnedMeshes) {
            List<ModelNode> order = _meshBoneOrder.get(geom.getMesh());
            if (order == null) {
                continue;
            }
            // remap the mesh's local bone indices to armature joint indices
            remapBoneIndices(geom.getMesh(), order);
            geom.getMesh().generateBindPose();
            // the skin vertices were baked into model (root/armature) space (see convertMesh),
            // so reparent the geometry directly under the root with an identity transform —
            // otherwise its fork mesh-local transform (and parent node transforms) would be
            // applied on top of the already model-space skinning, double-transforming it
            geom.removeFromParent();
            geom.setLocalTransform(com.jme3.math.Transform.IDENTITY.clone());
            result.root.attachChild(geom);
        }
        // a single SkinningControl on the root drives all skinned geometries beneath it
        SkinningControl skinning = new SkinningControl(_armature);
        _skinningControl = skinning;
    }

    /** Rewrites a mesh's BoneIndex buffer from per-mesh bone order to armature joint indices. */
    protected void remapBoneIndices (Mesh mesh, List<ModelNode> order)
    {
        List<Joint> jointList = _armature.getJointList();
        Map<Joint, Integer> jointIndex = new IdentityHashMap<>();
        for (int ii = 0; ii < jointList.size(); ii++) {
            jointIndex.put(jointList.get(ii), ii);
        }
        int[] remap = new int[order.size()];
        for (int ii = 0; ii < order.size(); ii++) {
            Joint j = _joints.get(order.get(ii));
            remap[ii] = (j != null) ? jointIndex.get(j) : 0;
        }
        VertexBuffer ib = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        ShortBuffer data = (ShortBuffer)ib.getData();
        data.clear();
        for (int ii = 0; ii < data.capacity(); ii++) {
            int local = data.get(ii) & 0xFFFF;
            data.put(ii, (short)(remap[local] & 0xFFFF));
        }
        ib.updateData(data);
    }

    /**
     * Converts every fork {@link Model.Animation} into a jME3 {@link AnimClip} of
     * {@link TransformTrack}s, attaching an {@link AnimComposer} to the root (and a
     * {@link SkinningControl} if the model is skinned).
     */
    protected void convertAnimations (Model model, Result result)
    {
        String[] names = model.getAnimationNames();
        AnimComposer composer = null;
        StringBuilder loopModes = new StringBuilder();
        StringBuilder frameRates = new StringBuilder();
        for (String name : names) {
            Model.Animation anim = model.getAnimation(name);
            if (anim == null || anim.transforms == null || anim.transformTargets == null) {
                result.droppedAnimations.add(name); // no keyframe data to convert
                continue;
            }
            AnimClip clip = buildClip(name, anim);
            if (clip == null) {
                result.droppedAnimations.add(name); // no track resolved to a converted target
                continue;
            }
            if (composer == null) {
                composer = new AnimComposer();
            }
            composer.addAnimClip(clip);
            result.animations.add(name);
            result.animTracks += clip.getTracks().length;
            // jME3 carries loop mode on the playback action, not the clip; preserve the fork's
            // repeatType as user data so the cutover can set LoopMode when it builds actions
            if (loopModes.length() > 0) {
                loopModes.append(',');
            }
            loopModes.append(name).append('=').append(loopMode(anim.repeatType));
            // the runtime app-side Model facade re-exposes the fork Animation.frameRate (sprite
            // code computes per-frame durations as 1f/frameRate); jME3 clips only carry absolute
            // times, so preserve the source frame rate per animation as user data.
            if (frameRates.length() > 0) {
                frameRates.append(',');
            }
            frameRates.append(name).append('=').append(anim.frameRate > 0 ? anim.frameRate : 30);
        }
        if (_skinningControl != null) {
            result.root.addControl(_skinningControl);
        }
        if (composer != null) {
            result.root.addControl(composer);
            result.root.setUserData("bang.loopModes", loopModes.toString());
            result.root.setUserData("bang.frameRates", frameRates.toString());
        }
    }

    /** Maps the fork {@code Controller} repeat type to the jME3 {@code LoopMode} name. */
    protected static String loopMode (int repeatType)
    {
        switch (repeatType) {
        case com.jme.scene.Controller.RT_WRAP:  return "Loop";
        case com.jme.scene.Controller.RT_CYCLE: return "Cycle";
        default:                                return "DontLoop"; // RT_CLAMP
        }
    }

    /** Builds one AnimClip from a fork Animation's packed per-frame TRS keyframes. */
    protected AnimClip buildClip (String name, Model.Animation anim)
    {
        int frames = anim.transforms.length;
        if (frames == 0) {
            return null;
        }
        int fps = anim.frameRate > 0 ? anim.frameRate : 30;
        float[] times = new float[frames];
        for (int ff = 0; ff < frames; ff++) {
            times[ff] = (float)ff / fps;
        }

        List<AnimTrack<?>> tracks = new ArrayList<>();
        com.jme.scene.Spatial[] targets = anim.transformTargets;
        for (int tt = 0; tt < targets.length; tt++) {
            // A fork node may map to MORE THAN ONE jME3 track target: it is always converted to
            // a Node (carrying its rigid mesh/child descendants), and if it is also a skinning
            // bone (or a bone ancestor) it ALSO has an armature Joint. The fork drives the single
            // ModelNode and both its rigid children and the skin follow; jME3 splits these, so the
            // SAME keyframes must drive BOTH the converted Node and the Joint. Animating only the
            // Joint (the earlier behaviour) froze the rigid children at the bind pose — e.g. the
            // homestead's "Homestead" node is a flag-bone ancestor that also carries the house
            // mesh, so the house floated at its bind Z while the flag rig posed correctly.
            for (HasLocalTransform target : resolveTrackTargets(targets[tt])) {
                // The model-root joint is pinned to identity (see buildArmature: the fork's
                // getModelTransform() always returns IDENTITY for the root ModelNode, dropping its
                // TRS — for the bind pose AND every animated frame). If a clip animates the root,
                // honouring those keyframes would re-introduce the dropped transform on the joint
                // and re-contort the skin off-bind, so force the root-joint track to identity to
                // match. (This only applies to the Joint target; the converted Node, if any, still
                // gets the real keyframes so its rigid descendants animate.)
                boolean rootJoint = (target == _rootJoint);
                Vector3f[] trans = new Vector3f[frames];
                Quaternion[] rots = new Quaternion[frames];
                Vector3f[] scales = new Vector3f[frames];
                for (int ff = 0; ff < frames; ff++) {
                    if (rootJoint) {
                        trans[ff] = new Vector3f();
                        rots[ff] = new Quaternion();
                        scales[ff] = new Vector3f(1f, 1f, 1f);
                        continue;
                    }
                    Model.Transform xf = anim.transforms[ff][tt];
                    com.jme.math.Vector3f t = ModelAnimAccess.translation(xf);
                    com.jme.math.Quaternion r = ModelAnimAccess.rotation(xf);
                    com.jme.math.Vector3f s = ModelAnimAccess.scale(xf);
                    trans[ff] = new Vector3f(t.x, t.y, t.z);
                    rots[ff] = new Quaternion(r.x, r.y, r.z, r.w);
                    scales[ff] = new Vector3f(s.x, s.y, s.z);
                }
                tracks.add(new TransformTrack(target, times, trans, rots, scales));
            }
        }
        if (tracks.isEmpty()) {
            return null;
        }
        AnimClip clip = new AnimClip(name);
        clip.setTracks(tracks.toArray(new AnimTrack<?>[0]));
        return clip;
    }

    /**
     * Resolves an animation target (a fork Spatial) to every jME3 track target it drives.
     *
     * <p>A fork {@link ModelNode} is always converted to a jME3 {@link Node} (carrying its rigid
     * mesh/child descendants); if it is also a skinning bone or a bone ancestor it ALSO has an
     * armature {@link Joint}. The fork drives the single ModelNode and both its rigid children and
     * the skin follow. jME3 splits these into a Node (positions the rigid descendants) and a Joint
     * (deforms the skin), so the SAME keyframes must drive BOTH — otherwise a node that is a bone
     * ancestor yet also carries geometry (e.g. the homestead's "Homestead" node: a flag-bone
     * ancestor that holds the house mesh) leaves its rigid descendants frozen at the bind pose
     * while only the skin poses, floating the geometry.
     *
     * <p>Returns at most two targets (Node and/or Joint), the Joint first. A non-{@link ModelNode}
     * target (e.g. a rigid {@link com.jme.scene.TriMesh} converted to a {@link Geometry}) yields
     * its single converted spatial.
     */
    protected List<HasLocalTransform> resolveTrackTargets (com.jme.scene.Spatial forkTarget)
    {
        List<HasLocalTransform> out = new ArrayList<>(2);
        if (forkTarget instanceof ModelNode mn) {
            Joint joint = _joints.get(mn);
            if (joint != null) {
                out.add(joint);
            }
        }
        Spatial node = _nodeMap.get(forkTarget);
        if (node instanceof HasLocalTransform hlt) {
            out.add(hlt);
        }
        return out;
    }

    /** True if the converted spatial is, or contains, a {@link Geometry} (rigid mesh). */
    protected static boolean hasGeometryDescendant (Spatial sp)
    {
        if (sp instanceof Geometry) {
            return true;
        }
        if (sp instanceof Node n) {
            for (Spatial child : n.getChildren()) {
                if (hasGeometryDescendant(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected String uniqueJointName (String base)
    {
        String name = base != null ? base : "joint";
        String candidate = name;
        int suffix = 1;
        while (_usedJointNames.contains(candidate)) {
            candidate = name + "$" + (suffix++);
        }
        _usedJointNames.add(candidate);
        return candidate;
    }

    /** Copies the local transform from a fork spatial to a jME3 spatial. */
    protected static void copyTransform (com.jme.scene.Spatial from, Spatial to)
    {
        com.jme.math.Vector3f t = from.getLocalTranslation();
        com.jme.math.Quaternion r = from.getLocalRotation();
        com.jme.math.Vector3f s = from.getLocalScale();
        to.setLocalTranslation(t.x, t.y, t.z);
        to.setLocalRotation(new Quaternion(r.x, r.y, r.z, r.w));
        to.setLocalScale(s.x, s.y, s.z);
    }

    /** Defensive copy of a fork float buffer into a fresh jME3-owned buffer. The fork's live
     * buffers must not be aliased into the jME3 mesh: a runtime loader may load a model more
     * than once, and the fork model can be read again afterward — sharing (and clear()-ing)
     * its buffers would corrupt those readers. */
    protected static FloatBuffer prepare (FloatBuffer buf)
    {
        buf.clear();
        FloatBuffer out = BufferUtils.createFloatBuffer(buf.remaining());
        out.put(buf);
        out.flip();
        return out;
    }

    /** In-place transform of a packed xyz position buffer by a fork {@link com.jme.math.Matrix4f}
     * (full affine: rotation/scale + translation). Used to bake skin vertices into model space. */
    protected static void transformPositions (FloatBuffer buf, com.jme.math.Matrix4f m)
    {
        if (buf == null) {
            return;
        }
        for (int ii = 0; ii + 2 < buf.limit(); ii += 3) {
            float x = buf.get(ii), y = buf.get(ii + 1), z = buf.get(ii + 2);
            buf.put(ii,     x*m.m00 + y*m.m01 + z*m.m02 + m.m03);
            buf.put(ii + 1, x*m.m10 + y*m.m11 + z*m.m12 + m.m13);
            buf.put(ii + 2, x*m.m20 + y*m.m21 + z*m.m22 + m.m23);
        }
    }

    /** In-place transform of a packed xyz normal buffer by a fork matrix (rotation/scale only,
     * no translation; matches the fork's normal handling in {@code SkinMesh.updateWorldData}). */
    protected static void transformNormals (FloatBuffer buf, com.jme.math.Matrix4f m)
    {
        if (buf == null) {
            return;
        }
        for (int ii = 0; ii + 2 < buf.limit(); ii += 3) {
            float x = buf.get(ii), y = buf.get(ii + 1), z = buf.get(ii + 2);
            buf.put(ii,     x*m.m00 + y*m.m01 + z*m.m02);
            buf.put(ii + 1, x*m.m10 + y*m.m11 + z*m.m12);
            buf.put(ii + 2, x*m.m20 + y*m.m21 + z*m.m22);
        }
    }

    /** Defensive copy of a fork index buffer (see {@link #prepare}). */
    protected static IntBuffer copy (IntBuffer buf)
    {
        buf.clear();
        IntBuffer out = BufferUtils.createIntBuffer(buf.remaining());
        out.put(buf);
        out.flip();
        return out;
    }

    protected final ModelTextureResolver _resolver;
    protected final String _typePath;

    /** Maps each fork spatial to its converted jME3 spatial (for anim target resolution). */
    protected final Map<com.jme.scene.Spatial, Spatial> _nodeMap = new IdentityHashMap<>();

    /** The set of ModelNodes used as skinning bones (identity set). */
    protected final Map<ModelNode, Boolean> _boneNodes = new IdentityHashMap<>();

    /** Maps each bone ModelNode to its jME3 Joint. */
    protected final Map<ModelNode, Joint> _joints = new LinkedHashMap<>();

    /** Per-mesh ordered bone list captured during buffer building, for armature remap. */
    protected final Map<Mesh, List<ModelNode>> _meshBoneOrder = new IdentityHashMap<>();

    /** Converted geometries backed by SkinMeshes. */
    protected final List<Geometry> _skinnedMeshes = new ArrayList<>();

    protected final java.util.Set<String> _usedJointNames = new java.util.HashSet<>();

    protected Armature _armature;
    protected SkinningControl _skinningControl;

    /** The armature's model-root joint (parent not a ModelNode). Pinned to identity to mirror the
     * fork's root model transform; its animation track (if any) is forced to identity too. */
    protected Joint _rootJoint;
}
