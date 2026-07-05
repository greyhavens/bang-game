//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.threerings.jme.model.ModelTextureResolver;

import com.threerings.bang.jme3.mxml.ModelDef.SkinMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.SpatialDef;
import com.threerings.bang.jme3.mxml.ModelDef.TriMeshDef;
import com.threerings.bang.jme3.mxml.ModelDef.WeightGroupDef;

/**
 * The Epic 4 Phase 2b native <code>.mxml</code> &rarr; jME3 <code>.j3o</code> writer: reads a
 * model directory (<code>model.properties</code> + <code>model.mxml</code> + per-animation
 * <code>*.mxml</code>) through the fork-free Phase 2a parsers and builds the same jME3 scene the
 * fork-wrapper oracle bake produces (<code>CompileModelTask</code> &rarr; <code>model.dat</code>
 * &rarr; {@code ModelConverter} &rarr; <code>model.j3o</code>), with <b>no fork code on the
 * classpath</b>.
 *
 * <p>The construction happens in two stages, each a faithful port of the corresponding oracle
 * stage:
 * <ol>
 *   <li><b>Compile</b> (the fork's {@code CompileModel}/{@code ModelDef} scene tail, re-done on
 *       pure data + jME3 math): animation-aware mesh merging ({@code createTransformTree} /
 *       {@code filterTransforms} / {@code mergeSpatials}), Forsyth vertex-order optimisation,
 *       skin weight-group partitioning, per-mesh property/texture-flag resolution
 *       ({@code ModelMesh.configure}), bounding-volume vertex centering
 *       ({@code ModelMesh.centerVertices}), node hierarchy + unused-node pruning
 *       ({@code createModel}/{@code pruneUnusedNodes}), reference (bind) model transforms
 *       ({@code setReferenceTransforms}), and per-animation static/transform target
 *       classification ({@code AnimationDef.createAnimation}).</li>
 *   <li><b>Convert</b> (a transliteration of {@code ModelConverter}, sourced from the compiled
 *       representation instead of deserialized fork objects): Node/Geometry tree, materials via
 *       {@link ModelTextureResolver}, 4-influence skinning buffers over an {@link Armature} of
 *       the full bone ancestor closure (root joint pinned to identity — the fork drops the model
 *       root's local TRS from all model-space transforms), skin vertices baked into model space
 *       through the inverse bind-reference transform, {@link AnimClip}s with dual Joint+Node
 *       track targets, and the {@code bang.*} user data the runtime facade reads.</li>
 * </ol>
 *
 * <p><b>Deliberate parity choices</b> (match the oracle, not the fork-of-2005):
 * <ul>
 *   <li><b>Controllers are not compiled.</b> The current oracle compile drops every
 *       {@code model.properties} controller: the classes the properties name
 *       ({@code com.threerings.jme.model.Rotator}, {@code com.threerings.bang.game.client.sprite.
 *       *Emission}, ...) are jME3 types post-cutover, so the fork compiler's
 *       {@code (ModelController)Class.forName(...)} cast fails and {@code createController}
 *       returns null (verified: current staged {@code model.dat} contain no controller class
 *       names). Consequently controller <em>target nodes</em> are not marked referenced and can
 *       be pruned — this writer reproduces exactly that. The controller <em>declarations</em>
 *       survive in {@code bang.properties} for the runtime effects port. The one compile-time
 *       effect controllers still have — the {@code controlled} flag restricting mesh merging —
 *       IS honoured.</li>
 *   <li><b>Per-instance state is runtime's job:</b> variants, multi-texture random picks and
 *       colorization re-resolve from {@code bang.properties}/{@code bang.textures} user data at
 *       runtime (the {@code ModelCache} path), exactly as with the oracle bake.</li>
 *   <li><b>Dropped like the oracle drops them:</b> texture filter/mipmap/compress hints and the
 *       {@code translucent} depth-sort flag are parsed but not representable in the baked
 *       material (the oracle bake loses them too); animation-expanded model bounds are not
 *       stored (jME3 recomputes bounds from vertex data).</li>
 * </ul>
 *
 * <p>Usage: {@code MxmlToJ3o <src rsrc root> <model type path> <out rsrc root>} bakes
 * {@code <out>/<type>/model.j3o} from {@code <src>/<type>/model.properties}; textures resolve
 * against the src root. {@link MxmlParity} drives this in bulk and diffs against the oracle.
 */
public class MxmlToJ3o
{
    /** The outcome of one bake, mirroring {@code ModelConverter.Result}. */
    public static class Result
    {
        /** The converted jME3 scene graph root. */
        public Node root;

        /** Names of animations converted to {@link AnimClip}s. */
        public final List<String> animations = new ArrayList<>();

        /** Animations present but NOT converted (no keyframes / no live target). */
        public final List<String> droppedAnimations = new ArrayList<>();

        /** Controllers declared in model.properties and NOT compiled (parity with the oracle
         * bake, which drops all of them — see the class doc). */
        public final List<String> droppedControllers = new ArrayList<>();

        /** Number of geometries (rigid + skinned). */
        public int geometries;

        /** Number of skinned meshes converted with full skinning. */
        public int skinnedMeshes;

        /** Number of joints in the converted armature (0 if not skinned). */
        public int armatureJoints;

        /** Total TransformTracks across all converted AnimClips. */
        public int animTracks;

        /** True if any skin vertex needed more than 4 influences (clamped + renormalised). */
        public boolean clampedInfluences;
    }

    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 3) {
            System.err.println("Usage: MxmlToJ3o <src rsrc root> <model type path> <out rsrc root>");
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

        DesktopAssetManager am = createAssetManager(srcRoot);
        MxmlToJ3o baker = new MxmlToJ3o(new ModelTextureResolver(am), typePath);
        Result result = baker.bake(modelDir);

        File outDir = new File(outRoot, typePath);
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            System.err.println("Unable to create " + outDir);
            System.exit(1);
        }
        File j3o = new File(outDir, "model.j3o");
        BinaryExporter.getInstance().save(result.root, j3o);
        System.out.println("Wrote " + j3o + " (" + j3o.length() + " bytes): " +
            result.geometries + " geometries, " + result.skinnedMeshes + " skinned, " +
            result.armatureJoints + " joints, " + result.animations.size() + " animations (" +
            result.animTracks + " tracks)" +
            (result.droppedAnimations.isEmpty() ? "" :
                ", dropped anims: " + result.droppedAnimations) +
            (result.droppedControllers.isEmpty() ? "" :
                ", uncompiled controllers: " + result.droppedControllers));
    }

    /** A headless asset manager suitable for baking (materials + textures, no GL). */
    public static DesktopAssetManager createAssetManager (File... roots)
    {
        DesktopAssetManager am = new DesktopAssetManager();
        am.registerLocator("/", ClasspathLocator.class);
        for (File root : roots) {
            am.registerLocator(root.getPath(), FileLocator.class);
        }
        am.registerLoader(J3MLoader.class, "j3m", "j3md");
        am.registerLoader(AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        am.registerLoader(com.jme3.export.binary.BinaryLoader.class, "j3o");
        return am;
    }

    /**
     * @param resolver resolves stored texture names to jME3 assets (the ModelCache-equivalent).
     * @param typePath the model's directory relative to the rsrc root; used both to resolve
     * relative texture names and as the {@code bang.type} user data.
     */
    public MxmlToJ3o (ModelTextureResolver resolver, String typePath)
    {
        _resolver = resolver;
        _typePath = typePath;
    }

    /**
     * Parses and bakes the model in the given directory. A baker instance is single-use: create
     * a fresh one per model.
     */
    public Result bake (File modelDir)
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
        return bake(props, mdef, animNames, adefs);
    }

    /** Bakes an already-parsed model (see {@link #bake(File)}). */
    public Result bake (Properties props, ModelDef mdef, String[] animNames, AnimationDef[] adefs)
    {
        Result result = new Result();

        // ---- compile stage (the fork CompileModel pipeline on pure data) ----

        // 1. preprocess: determine which compatible meshes keep a constant relative transform
        // through every animation, and merge them (fork createTransformTree/filterTransforms/
        // mergeSpatials). Uses java.util.HashMap exactly like the fork so the (iteration-order-
        // sensitive) merge outcomes match the oracle bake bit for bit.
        HashMap<String, PNode> tnodes = new HashMap<>();
        PNode troot = createTransformTree(mdef, props, tnodes);
        for (AnimationDef adef : adefs) {
            filterTransforms(adef, troot, tnodes);
        }
        mergeSpatials(mdef, tnodes);

        // 2. build the compiled node/mesh tree (fork ModelDef.createModel + configureMesh)
        CNode root = new CNode();
        root.name = props.getProperty("name", "model");
        root.scale.set(1f, 1f, 1f).multLocal(Float.parseFloat(props.getProperty("scale", "1")));

        HashMap<String, CNode> nodes = new HashMap<>();
        IdentityHashMap<SpatialDef, CNode> defNodes = new IdentityHashMap<>();
        for (SpatialDef sdef : mdef.spatials) {
            CNode cnode = createCNode(sdef, PropertiesUtil.getFilteredProperties(props, sdef.name));
            nodes.put(cnode.name, cnode);
            defNodes.put(sdef, cnode);
        }

        // resolve parents, skin bones, and root attachment (fork resolveReferences pass)
        HashSet<CNode> referenced = new HashSet<>();
        for (SpatialDef sdef : mdef.spatials) {
            CNode cnode = defNodes.get(sdef);
            CNode pnode = nodes.get(sdef.parent);
            if (pnode != null) {
                pnode.children.add(cnode);
                cnode.parent = pnode;
            } else if (sdef.parent != null) {
                System.out.println("WARNING: missing or invalid parent node [spatial=" +
                    sdef.name + ", parent=" + sdef.parent + "].");
            }
            if (cnode.mesh != null && cnode.mesh.groups != null) {
                // resolve each weight group's bone names to nodes; bones become referenced
                // (the only referenced-markers in the current oracle compile — see class doc)
                for (WGroup group : cnode.mesh.groups) {
                    group.bones = new CNode[group.boneNames.length];
                    for (int bb = 0; bb < group.boneNames.length; bb++) {
                        CNode bone = nodes.get(group.boneNames[bb]);
                        if (bone == null) {
                            System.out.println("WARNING: missing or invalid bone for bone " +
                                "weight [bone=" + group.boneNames[bb] + "].");
                        }
                        group.bones[bb] = bone;
                        referenced.add(bone);
                    }
                }
            }
            if (cnode.parent == null) {
                root.children.add(cnode);
                cnode.parent = root;
            }
        }

        // 3. controllers: NOT compiled (parity with the oracle — every controller class the
        // properties name fails the fork compiler's ModelController cast post-cutover, so the
        // oracle .dat/.j3o carry none and controller targets are NOT marked referenced).
        for (String ctrl : StringUtil.parseStringArray(props.getProperty("controllers", ""))) {
            Properties sub = PropertiesUtil.getSubProperties(props, ctrl);
            result.droppedControllers.add(ctrl + " (" + sub.getProperty("class", "?") + ")");
        }

        // 4. prune unused nodes (fork pruneUnusedNodes: a node with no mesh, no surviving
        // children and no referenced mark is dropped, and removed from the name map so
        // animations can no longer target it)
        pruneUnusedNodes(root, nodes, referenced);

        // 5. reference (bind) model transforms (fork setReferenceTransforms): the model root
        // contributes IDENTITY (its local TRS — including the props scale — is dropped from all
        // model-space transforms), and each skin mesh gets its inverse model-reference transform
        computeModelTransforms(root, new Matrix4f());
        computeInvRefTransforms(root);

        // 6. animations (fork AnimationDef.createAnimation): classify each animated node as
        // static (its local transform never diverged from bind across ALL animations — no
        // track) or a transform target (gets keyframes)
        HashMap<String, CAnim> anims = new HashMap<>();
        for (int ii = 0; ii < animNames.length; ii++) {
            anims.put(animNames[ii], createAnim(adefs[ii],
                PropertiesUtil.getSubProperties(props, animNames[ii]), nodes, tnodes));
        }

        // ---- convert stage (the ModelConverter construction, from the compiled tree) ----

        collectBoneNodes(root);
        result.root = (Node)convertSpatial(root, result);
        if (!_boneNodes.isEmpty()) {
            buildArmature(root);
            applySkinning(result);
            if (_armature != null) {
                result.armatureJoints = _armature.getJointCount();
            }
        }
        convertAnimations(anims, result);

        // preserve fork-only metadata as user data exactly like ModelConverter
        StringBuilder buf = new StringBuilder();
        for (String key : props.stringPropertyNames().stream().sorted().toList()) {
            buf.append(key).append(" = ").append(props.getProperty(key)).append('\n');
        }
        result.root.setUserData("bang.properties", buf.toString());
        result.root.setUserData("bang.animations", String.join(",",
            anims.keySet().toArray(new String[0])));
        result.root.setUserData("bang.type", _typePath);
        return result;
    }

    // ================================================================================
    // compile stage 1: merge preprocessing (fork ModelDef.TransformNode + createTransformTree +
    // AnimationDef.filterTransforms + mergeSpatials, on jME3 math)
    // ================================================================================

    /** The pure-data replacement for the fork's {@code TransformNode}: tracks a spatial def's
     * local/world transform through the animation sweep to find merge candidates. World
     * transforms compose exactly like the fork scene graph (scale, then rotate, then translate —
     * jME3's {@link Transform#combineWithParent}). */
    protected static class PNode
    {
        public SpatialDef spatial;
        public PNode parent;
        public final List<PNode> children = new ArrayList<>();

        /** True if referenced by name (as a parent or bone) — cannot be merged away. */
        public boolean referenced;

        /** True if a controller target; descendants only merge with other descendants. */
        public boolean controlled;

        /** Marks the node as having been transformed in the course of an animation. */
        public boolean transformed;

        /** The node's current local and world transforms. */
        public final Transform local = new Transform();
        public final Transform world = new Transform();
        public final Matrix4f localTransform = new Matrix4f();
        public final Matrix4f worldTransform = new Matrix4f();

        /** The bind local transform, nulled as soon as the node's local transform diverges. */
        public Matrix4f baseLocalTransform;

        /** Merge candidates: [PNode other, Matrix4f bindRelativeTransform]. */
        public List<Object[]> relativeTransforms;

        public PNode (SpatialDef spatial)
        {
            this.spatial = spatial;
            if (spatial != null) {
                setLocalTransform(spatial.translation, spatial.rotation, spatial.scale);
            }
        }

        public void setLocalTransform (float[] translation, float[] rotation, float[] scale)
        {
            local.setTranslation(translation[0], translation[1], translation[2]);
            local.setRotation(new Quaternion(
                rotation[0], rotation[1], rotation[2], rotation[3]));
            local.setScale(scale[0], scale[1], scale[2]);
            ModelDef.setTransform(local.getTranslation(), local.getRotation(),
                local.getScale(), localTransform);
        }

        /** Recomputes world transforms top-down (fork {@code updateGeometricState}); nodes not
         * reachable from the root (dangling parents) keep their initial identity world. */
        public void updateWorld ()
        {
            if (parent != null) {
                world.set(local).combineWithParent(parent.world);
            } else {
                world.set(local);
            }
            ModelDef.setTransform(world.getTranslation(), world.getRotation(),
                world.getScale(), worldTransform);
            for (PNode child : children) {
                child.updateWorld();
            }
        }

        public boolean canMerge (Properties props, PNode onode)
        {
            return !onode.referenced && spatial.canMerge(props, onode.spatial) &&
                getControlledAncestor() == onode.getControlledAncestor();
        }

        /** The nearest controlled ancestor, the root sentinel, or null for dangling chains
         * (mirrors the fork's plain-Node/parentless termination). */
        protected PNode getControlledAncestor ()
        {
            PNode ref = this;
            while (ref != null && ref.spatial != null && !ref.controlled) {
                ref = ref.parent;
            }
            return ref;
        }

        public void cullDivergentTransforms ()
        {
            if (baseLocalTransform != null &&
                !ModelDef.epsilonEquals(localTransform, baseLocalTransform)) {
                baseLocalTransform = null;
            }
            for (Iterator<Object[]> it = relativeTransforms.iterator(); it.hasNext(); ) {
                Object[] tuple = it.next();
                if (!ModelDef.epsilonEquals(
                        getRelativeTransform((PNode)tuple[0]), (Matrix4f)tuple[1])) {
                    it.remove();
                }
            }
        }

        /** The matrix taking vertices from the other node's coordinate space into this one's. */
        public Matrix4f getRelativeTransform (PNode other)
        {
            return worldTransform.invert().multLocal(other.worldTransform);
        }
    }

    /** Fork {@code ModelDef.createTransformTree}. */
    protected PNode createTransformTree (
        ModelDef mdef, Properties props, HashMap<String, PNode> nodes)
    {
        // create the nodes and map them by name
        for (SpatialDef spatial : mdef.spatials) {
            nodes.put(spatial.name, new PNode(spatial));
        }

        // resolve the parents and collect the names of the bones
        PNode root = new PNode(null);
        HashSet<String> bones = new HashSet<>();
        for (PNode node : nodes.values()) {
            if (node.spatial.parent == null) {
                root.children.add(node);
                node.parent = root;
            } else {
                PNode pnode = nodes.get(node.spatial.parent);
                if (pnode != null) {
                    pnode.children.add(node);
                    node.parent = pnode;
                    pnode.referenced = true;
                }
            }
            node.spatial.getBoneNames(bones);
        }

        // mark the bones as referenced
        for (String name : bones) {
            PNode node = nodes.get(name);
            if (node != null) {
                node.referenced = true;
            }
        }

        // mark the controlled nodes
        String[] controllers = StringUtil.parseStringArray(props.getProperty("controllers", ""));
        for (String controller : controllers) {
            Properties subProps = PropertiesUtil.getSubProperties(props, controller);
            PNode node = nodes.get(subProps.getProperty("node", controller));
            if (node != null) {
                node.referenced = node.controlled = true;
            }
        }

        // store the base transforms and relative transforms for merge candidates
        root.updateWorld();
        for (PNode node : nodes.values()) {
            node.baseLocalTransform = node.localTransform.clone();
            node.relativeTransforms = new ArrayList<>();
            for (PNode onode : nodes.values()) {
                if (node == onode || !node.canMerge(props, onode)) {
                    continue;
                }
                node.relativeTransforms.add(
                    new Object[] { onode, node.getRelativeTransform(onode) });
            }
        }
        return root;
    }

    /** Fork {@code AnimationDef.filterTransforms}: sweeps every frame, culling merge candidates
     * whose relative transform (or bind local transform) diverges. State persists across
     * animations, exactly like the fork. */
    protected void filterTransforms (
        AnimationDef adef, PNode root, HashMap<String, PNode> nodes)
    {
        for (PNode node : nodes.values()) {
            node.transformed = false;
        }
        for (AnimationDef.FrameDef frame : adef.frames) {
            for (AnimationDef.TransformDef transform : frame.transforms) {
                PNode node = nodes.get(transform.name);
                if (node != null) {
                    node.setLocalTransform(
                        transform.translation, transform.rotation, transform.scale);
                    node.transformed = true;
                }
            }
            root.updateWorld();
            for (PNode node : nodes.values()) {
                node.cullDivergentTransforms();
            }
        }
        // remove from merge candidates any pairs where one is animated and the other isn't
        for (PNode node : nodes.values()) {
            for (Iterator<Object[]> it = node.relativeTransforms.iterator(); it.hasNext(); ) {
                if (((PNode)it.next()[0]).transformed != node.transformed) {
                    it.remove();
                }
            }
        }
    }

    /** Fork {@code ModelDef.mergeSpatials}. */
    protected void mergeSpatials (ModelDef mdef, HashMap<String, PNode> nodes)
    {
        for (PNode node : nodes.values()) {
            if (!containsIdentity(mdef.spatials, node.spatial)) {
                continue;
            }
            for (Object[] tuple : node.relativeTransforms) {
                SpatialDef other = ((PNode)tuple[0]).spatial;
                if (containsIdentity(mdef.spatials, other)) {
                    node.spatial.merge(other, (Matrix4f)tuple[1]);
                    removeIdentity(mdef.spatials, other);
                }
            }
        }
    }

    /** SpatialDef has no equals override, but be explicit that merge bookkeeping is identity
     * based (as in the fork). */
    protected static boolean containsIdentity (List<SpatialDef> list, SpatialDef def)
    {
        for (SpatialDef d : list) {
            if (d == def) {
                return true;
            }
        }
        return false;
    }

    protected static void removeIdentity (List<SpatialDef> list, SpatialDef def)
    {
        for (int ii = 0, nn = list.size(); ii < nn; ii++) {
            if (list.get(ii) == def) {
                list.remove(ii);
                return;
            }
        }
    }

    // ================================================================================
    // compile stage 2: the compiled node/mesh tree (fork createSpatial/configureMesh/
    // centerVertices/pruneUnusedNodes/setReferenceTransforms)
    // ================================================================================

    /** A compiled model node (the fork's {@code ModelNode} as pure data). */
    protected static class CNode
    {
        public String name;
        public CNode parent;
        public final List<CNode> children = new ArrayList<>();
        public final Vector3f translation = new Vector3f();
        public final Quaternion rotation = new Quaternion();
        public final Vector3f scale = new Vector3f(1f, 1f, 1f);

        /** The mesh geometry hung under this node (fork: the child {@code ModelMesh}). */
        public CMesh mesh;

        /** The bind model-space transform (root = identity), fork {@code getModelTransform}. */
        public Matrix4f modelTransform;

        /** The local transform as a matrix (fork {@code JmeUtil.setTransform} of local TRS). */
        public Matrix4f localMatrix ()
        {
            return ModelDef.setTransform(translation, rotation, scale, new Matrix4f());
        }
    }

    /** A compiled mesh (the fork's configured {@code ModelMesh}/{@code SkinMesh} as pure data;
     * always named "mesh" like the fork's). */
    protected static class CMesh
    {
        public final Vector3f translation = new Vector3f();
        public final Quaternion rotation = new Quaternion();
        public final Vector3f scale = new Vector3f(1f, 1f, 1f);

        public FloatBuffer positions;
        public FloatBuffer normals;
        public FloatBuffer tcoords; // null if the mesh has no texture coordinates
        public IntBuffer indices;

        public String textureKey;
        public String[] textures;
        public boolean solid, transparent, additive, emissive, sphereMapped;
        public String emissiveMap;
        public float alphaThreshold;

        /** Skin weight groups in fork group order, or null for a rigid mesh. */
        public List<WGroup> groups;

        /** The inverse of the skin mesh's model-space reference transform. */
        public Matrix4f invRefTransform;

        public Matrix4f localMatrix ()
        {
            return ModelDef.setTransform(translation, rotation, scale, new Matrix4f());
        }
    }

    /** A compiled skin weight group (fork {@code SkinMesh.WeightGroup}). */
    protected static class WGroup
    {
        public String[] boneNames;
        public CNode[] bones;
        public int vertexCount;
        public float[] weights;
    }

    /** A compiled animation (fork {@code Model.Animation}). */
    protected static class CAnim
    {
        public int frameRate;
        public String loopMode; // jME3 LoopMode name derived from the fork repeat type
        public CNode[] transformTargets;
        public Vector3f[][] translations; // [frame][target]
        public Quaternion[][] rotations;
        public Vector3f[][] scales;
    }

    /** Fork {@code SpatialDef.getSpatial}/{@code createSpatial}: builds the compiled node (and
     * configured mesh, for mesh defs with geometry) for one spatial def. */
    protected CNode createCNode (SpatialDef sdef, Properties fprops)
    {
        CNode cnode = new CNode();
        cnode.name = sdef.name;
        cnode.translation.set(sdef.translation[0], sdef.translation[1], sdef.translation[2]);
        cnode.rotation.set(sdef.rotation[0], sdef.rotation[1], sdef.rotation[2],
            sdef.rotation[3]);
        cnode.scale.set(sdef.scale[0], sdef.scale[1], sdef.scale[2]);
        if (sdef instanceof TriMeshDef tdef && !tdef.indices.isEmpty()) {
            tdef.optimizeVertexOrder();
            cnode.mesh = configureMesh(tdef, fprops);
        }
        return cnode;
    }

    /** Fork {@code TriMeshDef.configureMesh}/{@code SkinMeshDef.configureMesh} +
     * {@code ModelMesh.configure} + {@code centerVertices}. */
    protected CMesh configureMesh (TriMeshDef tdef, Properties fprops)
    {
        CMesh mesh = new CMesh();

        // skins: group vertices by bone set and reorder them group-contiguously FIRST (the
        // buffers below must be built from the reordered vertex list)
        HashMap<Set<String>, WeightGroupDef> groups = null;
        if (tdef instanceof SkinMeshDef sdef) {
            groups = sdef.groupVertices();
        }

        // the geometry offset transform becomes the mesh's local transform
        if (tdef.offsetTranslation != null) {
            mesh.translation.set(tdef.offsetTranslation[0], tdef.offsetTranslation[1],
                tdef.offsetTranslation[2]);
        }
        if (tdef.offsetRotation != null) {
            mesh.rotation.set(tdef.offsetRotation[0], tdef.offsetRotation[1],
                tdef.offsetRotation[2], tdef.offsetRotation[3]);
        }
        if (tdef.offsetScale != null) {
            mesh.scale.set(tdef.offsetScale[0], tdef.offsetScale[1], tdef.offsetScale[2]);
        }

        // texture + render flags (fork ModelMesh.configure, with the same double-filtered
        // property lookup: "<meshName>.<key>" falls back to "<key>", then "<texture>.<key>"
        // falls back to "<key>")
        String texture = tdef.getTextureFilename();
        mesh.textureKey = texture;
        mesh.textures = (texture == null) ? null :
            StringUtil.parseStringArray(fprops.getProperty(texture, texture));
        Properties tprops = PropertiesUtil.getFilteredProperties(fprops, texture);
        mesh.sphereMapped = Boolean.parseBoolean(tprops.getProperty("sphere_map"));
        String emissive = tprops.getProperty("emissive");
        if (Boolean.parseBoolean(emissive)) {
            mesh.emissive = true;
        } else {
            mesh.emissiveMap = emissive;
        }
        mesh.additive = Boolean.parseBoolean(tprops.getProperty("additive"));
        mesh.solid = tdef.solid;
        mesh.transparent = tdef.transparent;
        String threshold = tprops.getProperty("alpha_threshold");
        mesh.alphaThreshold = (threshold == null) ?
            DEFAULT_ALPHA_THRESHOLD : Float.parseFloat(threshold);
        // (filter/mipmap/compress hints and the translucent depth-sort flag are dropped, as in
        // the oracle bake)

        // build the buffers from the (optimised, group-reordered) vertex list
        int vsize = tdef.vertices.size();
        FloatBuffer vbuf = BufferUtils.createVector3Buffer(vsize);
        FloatBuffer nbuf = BufferUtils.createVector3Buffer(vsize);
        FloatBuffer tbuf = tdef.tcoords ? BufferUtils.createVector2Buffer(vsize) : null;
        for (int ii = 0; ii < vsize; ii++) {
            tdef.vertices.get(ii).setInBuffers(vbuf, nbuf, tbuf);
        }
        IntBuffer ibuf = BufferUtils.createIntBuffer(tdef.indices.size());
        for (int ii = 0, nn = tdef.indices.size(); ii < nn; ii++) {
            ibuf.put(tdef.indices.get(ii));
        }
        vbuf.flip();
        nbuf.flip();
        if (tbuf != null) {
            tbuf.flip();
        }
        ibuf.flip();
        mesh.positions = vbuf;
        mesh.normals = nbuf;
        mesh.tcoords = tbuf;
        mesh.indices = ibuf;

        // center the vertices on the bounding-box centre, folding the offset into the mesh's
        // local translation (fork centerVertices; no model in the corpus uses bound=sphere)
        centerVertices(mesh);

        // record the weight groups (bone name order = the fork's group-key iteration order;
        // bone nodes resolve later, once every node exists)
        if (groups != null) {
            mesh.groups = new ArrayList<>();
            for (Map.Entry<Set<String>, WeightGroupDef> entry : groups.entrySet()) {
                WGroup group = new WGroup();
                group.vertexCount = entry.getValue().indices.size();
                group.boneNames = entry.getKey().toArray(new String[0]);
                group.weights = ModelDef.toArray(entry.getValue().weights);
                mesh.groups.add(group);
            }
        }
        return mesh;
    }

    /** Fork {@code ModelMesh.centerVertices}: translate the vertices so the mesh origin sits at
     * the centre of its (axis-aligned bounding box) bound, moving the local translation to
     * compensate. */
    protected static void centerVertices (CMesh mesh)
    {
        FloatBuffer vbuf = mesh.positions;
        int count = vbuf.limit() / 3;
        if (count == 0) {
            return;
        }
        float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int ii = 0; ii < count; ii++) {
            float x = vbuf.get(ii * 3), y = vbuf.get(ii * 3 + 1), z = vbuf.get(ii * 3 + 2);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        // fork: center = (min+max)/2; offset = -center; localTranslation -= offset;
        // points += offset
        float cx = (minX + maxX) * 0.5f, cy = (minY + maxY) * 0.5f, cz = (minZ + maxZ) * 0.5f;
        if (cx == 0f && cy == 0f && cz == 0f) {
            return;
        }
        mesh.translation.addLocal(cx, cy, cz);
        for (int ii = 0; ii < count; ii++) {
            vbuf.put(ii * 3, vbuf.get(ii * 3) - cx);
            vbuf.put(ii * 3 + 1, vbuf.get(ii * 3 + 1) - cy);
            vbuf.put(ii * 3 + 2, vbuf.get(ii * 3 + 2) - cz);
        }
    }

    /** Fork {@code ModelDef.pruneUnusedNodes}: recursively removes nodes with no mesh, no
     * surviving children and no referenced mark; removes them from the name map. Returns true
     * if the node should be kept from the parent's perspective. */
    protected boolean pruneUnusedNodes (
        CNode node, HashMap<String, CNode> nodes, HashSet<CNode> referenced)
    {
        boolean hasValidChildren = node.mesh != null;
        for (int ii = node.children.size() - 1; ii >= 0; ii--) {
            CNode child = node.children.get(ii);
            if (pruneUnusedNodes(child, nodes, referenced)) {
                hasValidChildren = true;
            } else {
                node.children.remove(ii);
                nodes.remove(child.name);
            }
        }
        return referenced.contains(node) || hasValidChildren;
    }

    /** Fork {@code ModelNode.updateWorldVectors} model transforms: the root contributes
     * identity; every other node is parentModel * localMatrix. */
    protected void computeModelTransforms (CNode node, Matrix4f parentModel)
    {
        node.modelTransform = (node.parent == null) ?
            new Matrix4f() : parentModel.mult(node.localMatrix());
        for (CNode child : node.children) {
            computeModelTransforms(child, node.modelTransform);
        }
    }

    /** Fork {@code SkinMesh.setReferenceTransforms}: each skin mesh's inverse model-space
     * reference transform. */
    protected void computeInvRefTransforms (CNode node)
    {
        if (node.mesh != null && node.mesh.groups != null) {
            node.mesh.invRefTransform = node.modelTransform.mult(
                node.mesh.localMatrix()).invertLocal();
        }
        for (CNode child : node.children) {
            computeInvRefTransforms(child);
        }
    }

    /** Fork {@code AnimationDef.createAnimation}: collects the animation's targets (skipping
     * pruned/merged nodes), drops the ones whose transform never diverged from bind across all
     * animations (fork "static targets"), and packs the per-frame TRS keyframes. */
    protected CAnim createAnim (
        AnimationDef adef, Properties aprops, HashMap<String, CNode> nodes,
        HashMap<String, PNode> tnodes)
    {
        LinkedHashSet<CNode> targets = new LinkedHashSet<>();
        for (AnimationDef.FrameDef frame : adef.frames) {
            for (AnimationDef.TransformDef transform : frame.transforms) {
                CNode target = nodes.get(transform.name);
                if (target == null) {
                    continue; // pruned or merged away
                }
                PNode tnode = tnodes.get(transform.name);
                if (tnode.baseLocalTransform == null) {
                    targets.add(target); // animated: gets keyframes
                }
                // else static: visible during the animation but never moves — no track,
                // exactly like the fork's staticTargets
            }
        }

        CAnim anim = new CAnim();
        anim.frameRate = adef.frameRate;
        anim.loopMode = loopMode(aprops.getProperty("repeat_type"));
        anim.transformTargets = targets.toArray(new CNode[0]);

        int frames = adef.frames.size(), ntargets = anim.transformTargets.length;
        anim.translations = new Vector3f[frames][ntargets];
        anim.rotations = new Quaternion[frames][ntargets];
        anim.scales = new Vector3f[frames][ntargets];
        for (int ff = 0; ff < frames; ff++) {
            AnimationDef.FrameDef frame = adef.frames.get(ff);
            for (int tt = 0; tt < ntargets; tt++) {
                String name = anim.transformTargets[tt].name;
                for (AnimationDef.TransformDef transform : frame.transforms) {
                    if (name.equals(transform.name)) {
                        anim.translations[ff][tt] = transform.getTranslation();
                        anim.rotations[ff][tt] = transform.getRotation();
                        anim.scales[ff][tt] = transform.getScale();
                        break;
                    }
                }
            }
        }
        return anim;
    }

    /** Fork repeat_type property to jME3 LoopMode name (JmeUtil.parseRepeatType +
     * ModelConverter.loopMode; the fork default is RT_CLAMP = DontLoop). */
    protected static String loopMode (String repeatType)
    {
        if ("wrap".equals(repeatType)) {
            return "Loop";
        } else if ("cycle".equals(repeatType)) {
            return "Cycle";
        }
        return "DontLoop";
    }

    // ================================================================================
    // convert stage: the ModelConverter construction, sourced from the compiled tree
    // ================================================================================

    /** Gathers every CNode used as a skinning bone (ModelConverter.collectBoneNodes; a
     * LinkedHashMap rather than the converter's IdentityHashMap — joint ORDER is not part of
     * the parity contract, and deterministic order beats identity-hash order). */
    protected void collectBoneNodes (CNode node)
    {
        if (node.mesh != null && node.mesh.groups != null) {
            for (WGroup group : node.mesh.groups) {
                for (CNode bone : group.bones) {
                    if (bone != null) {
                        _boneNodes.put(bone, Boolean.TRUE);
                    }
                }
            }
        }
        for (CNode child : node.children) {
            collectBoneNodes(child);
        }
    }

    /** Recursively converts a compiled node to jME3, recording the node mapping
     * (ModelConverter.convertSpatial; the compiled mesh becomes a child Geometry named "mesh",
     * mirroring the fork scene shape). */
    protected Spatial convertSpatial (CNode cnode, Result result)
    {
        Node node = new Node(cnode.name);
        if (cnode.mesh != null) {
            node.attachChild(convertMesh(cnode.mesh, result));
        }
        for (CNode child : cnode.children) {
            node.attachChild(convertSpatial(child, result));
        }
        node.setLocalTranslation(cnode.translation);
        node.setLocalRotation(cnode.rotation.clone());
        node.setLocalScale(cnode.scale);
        _nodeMap.put(cnode, node);
        return node;
    }

    /** ModelConverter.convertMesh. */
    protected Geometry convertMesh (CMesh cmesh, Result result)
    {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, cmesh.positions);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, cmesh.normals);
        if (cmesh.tcoords != null) {
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, cmesh.tcoords);
        }
        mesh.setBuffer(VertexBuffer.Type.Index, 3, cmesh.indices);

        boolean skinned = false;
        if (cmesh.groups != null) {
            skinned = addSkinningBuffers(cmesh, mesh, result);
            if (skinned) {
                // bake the (mesh-local) skin vertices into model space so jME3's root-frame
                // skinning deforms them exactly like the fork's Bcur * inverse(Bref); the
                // geometry is reparented to the root with identity transform in applySkinning
                Matrix4f toModel = cmesh.invRefTransform.invert();
                transformPositions(mesh.getFloatBuffer(VertexBuffer.Type.Position), toModel);
                transformNormals(mesh.getFloatBuffer(VertexBuffer.Type.Normal), toModel);
            }
        }

        mesh.updateBound();

        Geometry geom = new Geometry("mesh", mesh);
        geom.setLocalTranslation(cmesh.translation);
        geom.setLocalRotation(cmesh.rotation.clone());
        geom.setLocalScale(cmesh.scale);

        _resolver.applyMaterial(geom, _typePath, cmesh.textureKey, cmesh.textures,
            cmesh.solid, cmesh.additive, cmesh.transparent, cmesh.alphaThreshold,
            cmesh.emissive, cmesh.emissiveMap, cmesh.sphereMapped);

        result.geometries++;
        if (skinned) {
            result.skinnedMeshes++;
            _skinnedMeshes.add(geom);
        }
        return geom;
    }

    /** ModelConverter.addSkinningBuffers: expands the variable-width weight groups into
     * 4-influence UnsignedShort BoneIndex / Float BoneWeight buffers, recording the per-mesh
     * bone order for the armature remap. */
    protected boolean addSkinningBuffers (CMesh cmesh, Mesh mesh, Result result)
    {
        if (cmesh.groups.isEmpty()) {
            return false;
        }
        int vertexCount = mesh.getVertexCount();

        Map<CNode, Integer> boneIndex = new LinkedHashMap<>();
        List<CNode> boneOrder = new ArrayList<>();

        ShortBuffer indices = BufferUtils.createShortBuffer(vertexCount * 4);
        FloatBuffer weights = BufferUtils.createFloatBuffer(vertexCount * 4);

        int[] oi = new int[4];
        float[] ow = new float[4];

        for (WGroup group : cmesh.groups) {
            int gbones = group.bones.length;
            int[] gi = new int[gbones];
            for (int bb = 0; bb < gbones; bb++) {
                CNode node = group.bones[bb];
                if (node == null) {
                    gi[bb] = 0; // unresolved bone: pin to joint 0 (mirrors ModelConverter)
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
            float[] gw = new float[gbones];
            boolean[] used = new boolean[gbones];
            for (int vv = 0; vv < group.vertexCount; vv++) {
                for (int bb = 0; bb < gbones; bb++) {
                    gw[bb] = group.weights[vv * gbones + bb];
                }
                writeVertexInfluences(gi, gw, used, oi, ow, indices, weights, result);
            }
        }

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

    /** ModelConverter.writeVertexInfluences (verbatim): keep the 4 highest weights,
     * renormalised. */
    protected void writeVertexInfluences (
        int[] gi, float[] gw, boolean[] used, int[] oi, float[] ow,
        ShortBuffer indices, FloatBuffer weights, Result result)
    {
        int n = gi.length;
        int keep = Math.min(4, n);
        if (n > 4) {
            result.clampedInfluences = true;
        }
        Arrays.fill(used, 0, n, false);
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
        if (total <= 0f) {
            ow[0] = 1f;
            total = 1f;
        }
        for (int slot = 0; slot < 4; slot++) {
            indices.put(slot < keep ? (short)(oi[slot] & 0xFFFF) : (short)0);
            weights.put(slot < keep ? ow[slot] / total : 0f);
        }
    }

    /** ModelConverter.buildArmature: a Joint for every node that is a bone or a bone ancestor
     * (full ancestor closure up to the model root), carrying the bind local TRS — except the
     * model root itself, whose joint is pinned to IDENTITY because the fork drops the root's
     * local transform from every model-space measurement (see ModelConverter's reference-space
     * rule). */
    protected void buildArmature (CNode root)
    {
        Map<CNode, Boolean> skeletonNodes = new LinkedHashMap<>();
        for (CNode bnode : _boneNodes.keySet()) {
            for (CNode p = bnode; p != null; p = p.parent) {
                if (skeletonNodes.put(p, Boolean.TRUE) != null) {
                    break; // this node (and thus all its ancestors) already added
                }
            }
        }
        for (CNode node : skeletonNodes.keySet()) {
            Joint joint = new Joint(uniqueJointName(node.name));
            if (node.parent == null) { // the model root
                joint.setLocalTransform(new Transform());
                _rootJoint = joint;
            } else {
                joint.setLocalTransform(new Transform(
                    node.translation.clone(), node.rotation.clone(), node.scale.clone()));
            }
            _joints.put(node, joint);
        }
        for (Map.Entry<CNode, Joint> e : _joints.entrySet()) {
            CNode p = e.getKey().parent;
            if (p != null && _joints.containsKey(p)) {
                _joints.get(p).addChild(e.getValue());
            }
        }
        List<Joint> all = new ArrayList<>(_joints.values());
        _armature = new Armature(all.toArray(new Joint[0]));
        _armature.update();
        _armature.saveBindPose();
        _armature.saveInitialPose();
    }

    /** ModelConverter.applySkinning: remap bone indices to armature joint order, generate the
     * bind pose, reparent the (model-space-baked) skinned geometry directly under the root with
     * an identity transform, and prepare a single SkinningControl. */
    protected void applySkinning (Result result)
    {
        if (_armature == null) {
            return;
        }
        for (Geometry geom : _skinnedMeshes) {
            List<CNode> order = _meshBoneOrder.get(geom.getMesh());
            if (order == null) {
                continue;
            }
            remapBoneIndices(geom.getMesh(), order);
            geom.getMesh().generateBindPose();
            geom.removeFromParent();
            geom.setLocalTransform(Transform.IDENTITY.clone());
            result.root.attachChild(geom);
        }
        _skinningControl = new SkinningControl(_armature);
    }

    /** ModelConverter.remapBoneIndices. */
    protected void remapBoneIndices (Mesh mesh, List<CNode> order)
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

    /** ModelConverter.convertAnimations: AnimClips on an AnimComposer, the SkinningControl
     * attached first, and the loop-mode/frame-rate user data the runtime facade reads. */
    protected void convertAnimations (HashMap<String, CAnim> anims, Result result)
    {
        AnimComposer composer = null;
        StringBuilder loopModes = new StringBuilder();
        StringBuilder frameRates = new StringBuilder();
        for (String name : anims.keySet()) {
            CAnim anim = anims.get(name);
            AnimClip clip = (anim == null) ? null : buildClip(name, anim);
            if (clip == null) {
                result.droppedAnimations.add(name);
                continue;
            }
            if (composer == null) {
                composer = new AnimComposer();
            }
            composer.addAnimClip(clip);
            result.animations.add(name);
            result.animTracks += clip.getTracks().length;
            if (loopModes.length() > 0) {
                loopModes.append(',');
            }
            loopModes.append(name).append('=').append(anim.loopMode);
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

    /** ModelConverter.buildClip: one TransformTrack per (frame-target × jME3 target), where a
     * compiled node that is both a Joint and a converted Node gets the SAME keyframes on BOTH
     * (the homestead dual-target rule). */
    protected AnimClip buildClip (String name, CAnim anim)
    {
        int frames = anim.translations.length;
        if (frames == 0) {
            return null;
        }
        int fps = anim.frameRate > 0 ? anim.frameRate : 30;
        float[] times = new float[frames];
        for (int ff = 0; ff < frames; ff++) {
            times[ff] = (float)ff / fps;
        }

        List<AnimTrack<?>> tracks = new ArrayList<>();
        for (int tt = 0; tt < anim.transformTargets.length; tt++) {
            for (HasLocalTransform target : resolveTrackTargets(anim.transformTargets[tt])) {
                // the model-root joint is pinned to identity (see buildArmature); if a clip ever
                // animated it, honouring the keyframes would re-introduce the dropped root
                // transform on the joint — force that track to identity (Node target unaffected)
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
                    trans[ff] = anim.translations[ff][tt].clone();
                    rots[ff] = anim.rotations[ff][tt].clone();
                    scales[ff] = anim.scales[ff][tt].clone();
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

    /** ModelConverter.resolveTrackTargets: the Joint (if the node is in the armature) first,
     * then the converted Node. */
    protected List<HasLocalTransform> resolveTrackTargets (CNode target)
    {
        List<HasLocalTransform> out = new ArrayList<>(2);
        Joint joint = _joints.get(target);
        if (joint != null) {
            out.add(joint);
        }
        Spatial node = _nodeMap.get(target);
        if (node != null) {
            out.add(node);
        }
        return out;
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

    /** In-place transform of a packed xyz position buffer (full affine); see
     * ModelConverter.transformPositions. */
    protected static void transformPositions (FloatBuffer buf, Matrix4f m)
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

    /** In-place transform of a packed xyz normal buffer (rotation/scale only); see
     * ModelConverter.transformNormals. */
    protected static void transformNormals (FloatBuffer buf, Matrix4f m)
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

    protected final ModelTextureResolver _resolver;
    protected final String _typePath;

    /** Maps each compiled node to its converted jME3 node (for anim target resolution). */
    protected final Map<CNode, Spatial> _nodeMap = new IdentityHashMap<>();

    /** The set of compiled nodes used as skinning bones, in deterministic first-use order. */
    protected final Map<CNode, Boolean> _boneNodes = new LinkedHashMap<>();

    /** Maps each skeleton node to its jME3 Joint. */
    protected final Map<CNode, Joint> _joints = new LinkedHashMap<>();

    /** Per-mesh ordered bone list captured during buffer building, for armature remap. */
    protected final Map<Mesh, List<CNode>> _meshBoneOrder = new IdentityHashMap<>();

    /** Converted geometries backed by skin meshes. */
    protected final List<Geometry> _skinnedMeshes = new ArrayList<>();

    protected final Set<String> _usedJointNames = new HashSet<>();

    protected Armature _armature;
    protected SkinningControl _skinningControl;
    protected Joint _rootJoint;

    /** The fork's default alpha-test threshold. */
    protected static final float DEFAULT_ALPHA_THRESHOLD = 0.5f;
}
