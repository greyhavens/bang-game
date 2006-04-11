//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.ref.WeakReference;

import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.CloneCreator;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.SharedNode;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Box;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;
import com.jmex.model.ModelCloneCreator;
import com.jmex.model.XMLparser.JmeBinaryReader;
import com.jmex.model.XMLparser.Converters.Md3ToJme;
import com.jmex.model.animation.KeyframeController;

import com.threerings.media.image.Colorization;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.client.Config;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.client.util.AnimationController;
import com.threerings.bang.client.util.ModelLoader;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains information on a single logical "model" which may be made up
 * of multiple meshes each with various animations and whatnot.
 */
public class Model
{
    /** Used for notification of the completed resolution of all of a model's
     * animations. */
    public interface ResolutionObserver
    {
        /** Called when all of a model's animations are resolved. */
        public void modelResolved (Model model);
    }

    /**
     * Used to bind an animation's meshs to a node and then later remove them.
     * The binding will take care of updating the node if the animation was not
     * fully resolved at the time that it was requested.
     */
    public static class Binding
    {
        /** Used to notify interested parties when our animation is finally
         * bound or if we are cleared prior to binding completion. */
        public interface Observer
        {
            public void wasBound (Animation anim, Binding binding);
            public void wasSkipped (Animation anim);
        }

        public Geometry getMarker (String name)
        {
            return _markers.get(name);
        }

        public void detach ()
        {
            // if we haven't completed our binding yet, we need to let our
            // observer know that we never did the deal
            if (_obs != null) {
                _obs.wasSkipped(_anim);
                _obs = null;
            }

            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.detachChild(_meshes[ii]);
            }
            _anim.clearBinding(this);
        }

        protected Binding (
            Animation anim, Node node, int random, Colorization[] zations,
            Observer obs)
        {
            _anim = anim;
            _node = node;
            _random = random;
            _zations = zations;
            _obs = obs;
            attachMeshes();
        }

        protected void update ()
        {
            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.detachChild(_meshes[ii]);
            }
            attachMeshes();
        }

        protected void attachMeshes ()
        {
            _meshes = _anim.getMeshes(_random, _zations);
            _markers = new HashMap<String, Geometry>();
            for (int ii = 0; ii < _meshes.length; ii++) {
                if (_meshes[ii].getCullMode() == Node.CULL_ALWAYS) {
                    _markers.put(_meshes[ii].getName(),
                        getGeometry(_meshes[ii]));
                }
                _node.attachChild(_meshes[ii]);
                _meshes[ii].updateRenderState();
            }

            // configure the animation speed and repeat type
            Sprite.setAnimationSpeed(
                _node, Config.animationSpeed * _anim.getSpeed());
            Sprite.setAnimationRepeatType(_node, _anim.repeatType);

            // the first time through we'll be resolving and won't call this,
            // then we'll call it when the resolution is done; subsequent
            // bindings will be resolved the first time through and will report
            // binding completion immediately
            if (!_anim.isResolving() && _obs != null) {
                _obs.wasBound(_anim, this);
                _obs = null;
            }
        }

        protected Geometry getGeometry (Node mesh)
        {
            for (int ii = 0, nn = mesh.getQuantity(); ii < nn; ii++) {
                Spatial child = mesh.getChild(ii);
                if (child instanceof Geometry) {
                    return (Geometry)child;

                } else if (child instanceof Node) {
                    Geometry geom = getGeometry((Node)child);
                    if (geom != null) {
                        return geom;
                    }
                }
            }
            return null;
        }

        protected Animation _anim;
        protected Node _node;
        protected int _random;
        protected Colorization[] _zations;
        protected Observer _obs;
        protected Node[] _meshes;
        protected HashMap<String, Geometry> _markers;
    }

    /** Information on an animation which is a collection of animated meshes
     * with a frame count and a duration. */
    public static class Animation
    {
        /** The name of this animation. */
        public String action;

        /** The number of frames in this animation. */
        public int frames;

        /** The duration of one cycle of the animation in milliseconds. */
        public int duration;

        /** The repeat type of the animation (clamp, cycle, wrap). */
        public int repeatType;

        /** The emitters used in the animation. */
        public Emitter[] emitters;

        /**
         * Returns the "speed" at which this animation's controller should
         * be run.
         */
        public float getSpeed ()
        {
            return 1000f * frames / duration;
        }

        /**
         * Returns the duration of this animation in seconds.
         */
        public float getDuration ()
        {
            return duration / 1000f;
        }

        /**
         * Gets an emitter by name.
         */
        public Emitter getEmitter (String name)
        {
            for (int ii = 0; ii < emitters.length; ii++) {
                if (emitters[ii].name.equals(name)) {
                    return emitters[ii];
                }
            }
            return null;
        }

        /**
         * Returns true if this animation is resolved or has at least been
         * queued up to be resolved.
         */
        public boolean isResolved ()
        {
            return _parts != null || _bindings != null;
        }

        /**
         * Returns true if this animation is in the process of resolving.
         */
        public boolean isResolving ()
        {
            return _bindings != null;
        }

        /**
         * Configures this animation as "being resolved" so that we don't queue
         * it up for resolution more than once.
         */
        public void setIsResolving ()
        {
            _bindings = new ArrayList<Binding>();
        }

        /**
         * Binds this animation to the specified node. <em>Note:</em> the
         * binding <em>must</em> be cleared later via {@link Binding#detach}.
         * When the binding is finally bound, the optional observer will be
         * notified.
         *
         * @see #getMeshes
         */
        public Binding bind (Node node, int random, Colorization[] zations,
            Binding.Observer obs)
        {
            Binding binding = new Binding(this, node, random, zations, obs);
            if (_bindings != null) {
                _bindings.add(binding);
            }
            return binding;
        }

        /**
         * Returns the meshes for the specified action or a zero-length array
         * if we have no meshes for said action.
         *
         * @param random a random number used to select a texture state
         * for models that support random textures. To allow the same
         * texture to be used for different animations, the caller must
         * select a random number for the "instance" and then supply it to
         * the model when obtaining animated meshes for that particular
         * instance.
         * @param zations the colorizations to apply to the texture, or
         * <code>null</code> for none
         */
        public Node[] getMeshes (int random, Colorization[] zations)
        {
            // return an empty set of meshes if our parts are not resolved
            if (_parts == null) {
                return new Node[0];
            }

            Node[] nodes = new Node[_parts.length + emitters.length];
            for (int ii = 0; ii < _parts.length; ii++) {
                nodes[ii] = _parts[ii].createInstance(random, zations);
            }
            for (int ii = 0; ii < emitters.length; ii++) {
                int idx = _parts.length + ii;
                nodes[idx] = (Node)emitters[ii].creator.createCopy();
                nodes[idx].setName(emitters[ii].name);
                nodes[idx].setIsCollidable(false);
                nodes[idx].setCullMode(Node.CULL_ALWAYS);
            }
            return nodes;
        }

        /**
         * Configures this animation with its underlying meshes.
         */
        public void setParts (Part[] parts)
        {
            _parts = parts;

            // update any extant bindings
            ArrayList<Binding> obindings = _bindings;
            _bindings = null;
            for (Binding binding : obindings) {
                binding.update();
            }
        }

        /**
         * Clears the specified binding if it's being tracked.
         */
        protected void clearBinding (Binding binding)
        {
            if (_bindings != null) {
                _bindings.remove(binding);
            }
        }

        /** The animated meshes that make up the animation. */
        protected Part[] _parts;

        /** Use to track the nodes to which this animation is bound while the
         * animation is in the process of resolving. */
        protected ArrayList<Binding> _bindings;
    }

    /** Contains information on an effect emitter. */
    public static class Emitter
    {
        /** The name of the emitter as defined in the model properties. */
        public String name;

        /** The configuration of the emitter. */
        public Properties props;

        /** Used to create a clone of the emitter marker geometry (only valid
         * once the animation has been resolved). */
        public CloneCreator creator;
    }

    /**
     * Returns the background loader thread used to asynchronously load models.
     */
    public static ModelLoader getLoader ()
    {
        // start up our background loader thread if it hasn't been
        if (_loader == null) {
            _loader = new ModelLoader();
            _loader.start();
        }
        return _loader;
    }

    /**
     * Creates the model and loads up all of its constituent animations from
     * a local file.
     */
    public Model (BasicContext ctx, File path)
    {
        _ctx = ctx;
        _local = true;
        _key = path.toString();
        int sep = _key.lastIndexOf(File.separator);
        _key = (sep == -1) ? "" : _key.substring(0, sep);
        FileInputStream pin = null;
        try {
            pin = new FileInputStream(path);
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Couldn't find model properties file " +
                "[path=" + path + "].");
        }
        loadProperties(pin, _key);
    }
    
    /**
     * Creates the model and loads up all of its consituent animations.
     */
    public Model (BasicContext ctx, String type, String name)
    {
        _ctx = ctx;
        _key = type + "/" + name;
        String path = "rsrc/" + _key + "/model.properties";
        loadProperties(getClass().getClassLoader().getResourceAsStream(path),
            path);
    }

    /**
     * Returns a reference to the configuration properties of this model.
     */
    public Properties getProperties ()
    {
        return _props;
    }

    /**
     * Returns the names of all this model's actions.
     */
    public String[] getActions ()
    {
        if (_actions == null) {
            _actions = _anims.keySet().toArray(new String[_anims.size()]);
        }
        return _actions;
    }
    
    /**
     * Returns true if we have meshes for the specified action.
     */
    public boolean hasAnimation (String action)
    {
        return _anims.containsKey(action);
    }

    /**
     * Adds a resolution observer to this model.
     */
    public void addResolutionObserver (ResolutionObserver observer)
    {
        if (allAnimationsResolved()) {
            observer.modelResolved(this);
        } else {
            _observers.add(observer);
        }
    }

    /**
     * Clears a resolution observer from this model.
     */
    public void clearResolutionObserver (ResolutionObserver observer)
    {
        _observers.remove(observer);
    }

    /**
     * Returns the specified named animation or a blank animation if the
     * specified animation does not exist. If the specified animation is not
     * yet resolved, it will be queued up for resolution.
     */
    public Animation getAnimation (String action)
    {
        Animation anim = _anims.get(action);
        if (anim == null) {
            log.warning("Requested non-existent animation [model=" + _key +
                        ", anim=" + action + "].");
            return BLANK_ANIM;
        } else if (!anim.isResolved()) {
            anim.setIsResolving();
            getLoader().queueAction(this, action);
        }
        return anim;
    }

    /**
     * Requests that all of this model's actions be resolved.
     */
    public void resolveActions ()
    {
        Iterator<Map.Entry<String,Animation>> iter =
            _anims.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Animation> entry = iter.next();
            String action = entry.getKey();
            Animation anim = entry.getValue();
            if (!anim.isResolved()) {
                anim.setIsResolving();
                getLoader().queueAction(this, action);
            }
        }
    }

    /**
     * Resolves the mesh and texture data associated with the specified
     * action. <em>Note:</em> this is called on the background model loader
     * thread and must be very careful not to use non-thread-safe services from
     * the supplied context.
     *
     * @return the number of bytes loaded from disk in resolving this action.
     */
    public int resolveAction (String action)
    {
        final Animation anim = _anims.get(action);
        boolean isStatic = _props.getProperty(
            anim.action + ".static", "false").equalsIgnoreCase("true");
        anim.frames = BangUtil.getIntProperty(
            _key, _props, anim.action + ".frames", 8);
        anim.duration = BangUtil.getIntProperty(
            _key, _props, anim.action + ".duration", 250);

        String repeatType = _props.getProperty(anim.action + ".repeat_type");
        if ("clamp".equals(repeatType)) {
            anim.repeatType = Controller.RT_CLAMP;
        } else if ("cycle".equals(repeatType)) {
            anim.repeatType = Controller.RT_CYCLE;
        } else {
            anim.repeatType = Controller.RT_WRAP;
        }

        // used to track total number of bytes read
        int[] loaded = new int[1];

        String path = _key + "/";
        String[] pnames = getList(
            _props, anim.action + ".meshes", "meshes", true);
        final Part[] parts = new Part[pnames.length];
        for (int pp = 0; pp < pnames.length; pp++) {
            String mesh = pnames[pp];
            Part part = (parts[pp] = new Part());

            // load up this part's 3D model; we can safely do this on the
            // background thread because our models are super simple and don't
            // contain embedded texture states or other bits which would cause
            // the JmeBinaryLoader to try to do OpenGL things on the wrong
            // thread and we'd be hosed
            boolean trans = _props.getProperty(
                mesh + ".transparent", "false").equalsIgnoreCase("true");
            boolean solid = _props.getProperty(
                mesh + ".solid", "true").equalsIgnoreCase("true");
            String mpath = path + anim.action + "/" + mesh + ".jme",
                amesh = anim.action + "." + mesh,
                bound = _props.getProperty(amesh + ".bound", "box");
            if (isStatic) {
                part.target = loadModel(mpath, trans, solid, bound, loaded);
            } else {
                part.creator = loadModelCreator(
                    mpath, trans, solid, bound, loaded);
            }

            // the model may have multiple textures from which we
            // select at random
            String[] tnames =
                getList(_props, mesh + ".texture", "texture", false);
            if (tnames.length == 0) {
                continue;
            }
            part.tpaths = new String[tnames.length];
            for (int tt = 0; tt < tnames.length; tt++) {
                part.tpaths[tt] = cleanPath(path + tnames[tt]);
                if (!_local) {
                    // preload the textures so they're cached when we need them
                    // TODO: make the texture cache thread safe
                    _ctx.getTextureCache().getTexture(part.tpaths[tt]);
                }
            }
            
            // possibly create a programmatic animation controller
            String panim = _props.getProperty(amesh + ".anim");
            if (panim != null) {
                part.animctrl = AnimationController.create(panim,
                    PropertiesUtil.getSubProperties(_props, amesh));
            }
        }

        // load the emitter marker geometries
        for (int ee = 0; ee < anim.emitters.length; ee++) {
            String mesh = anim.emitters[ee].name;
            anim.emitters[ee].creator = loadModelCreator(
                path + anim.action + "/" + mesh + ".jme", false,
                false, "box", loaded);
        }

        // now finish our resolution on the main thread
        _ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                finishResolution(anim, parts);
            }
        });

        return loaded[0];
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return _key.equals(((Model)other)._key);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _key.hashCode();
    }

    @Override // documentation inherited
    public String toString ()
    {
        return _key + " m:" + _meshes.size() + " a:" + _anims.size();
    }

    /**
     * Loads the model properties from the given stream.
     */
    protected void loadProperties (InputStream pin, String path)
    {
        _props = new Properties();
        if (pin != null) {
            try {
                _props.load(new BufferedInputStream(pin));
            } catch (IOException ioe) {
                log.log(Level.WARNING, "Failed to load model info " +
                    "[path=" + path + "].", ioe);
            }
        }
        if (_props.size() == 0) {
            log.info("Faking model info file for " + _key + ".");
            String mname = _key.substring(_key.lastIndexOf("/")+1);
            _props.setProperty("meshes", mname);
        }

        // TODO: use a whole different system for non-animating models
        // that uses a SharedMesh

        String[] actions = getList(_props, "actions", null, true);
        for (int ii = 0; ii < actions.length; ii++) {
            String action = actions[ii];
            // skip commented out actions
            if (action.equals("#")) {
                ii++;
                continue;
            } else if (action.startsWith("#")) {
                continue;
            }

            // create a placeholder animation record for this action
            Animation anim = new Animation();
            anim.action = action;
            anim.emitters = getEmitters(action);
            _anims.put(action, anim);
        }

        // TEMP: use the first frame of the walking pose if this model has no
        // standing pose defined
        if (!_anims.containsKey("standing") && _anims.containsKey("walking")) {
            _anims.put("standing", _anims.get("walking"));
        }
    }
    
    /**
     * Loads the names and properties of the emitters for the specified action.
     * Does not load the marker geometries (that happens on the loader thread).
     */
    protected Emitter[] getEmitters (String action)
    {
        String[] enames = getList(_props, action + ".emitters", "emitters",
            false);
        Emitter[] emitters = new Emitter[enames.length];
        for (int ee = 0; ee < enames.length; ee++) {
            Emitter emitter = (emitters[ee] = new Emitter());
            emitter.name = enames[ee];
            emitter.props = PropertiesUtil.getSubProperties(_props,
                action + "." + emitter.name, emitter.name);
        }
        return emitters;
    }

    /**
     * Finishes the resolution of a model animation. <em>Note:</em> this is
     * called on the main thread where we can safely access OpenGL.
     */
    protected void finishResolution (Animation anim, Part[] parts)
    {
        anim.setParts(parts);

        // if all of our animations are now resolved, notify our observers
        if (allAnimationsResolved()) {
            for (ResolutionObserver observer : _observers) {
                observer.modelResolved(this);
            }
            _observers.clear();
        }
    }

    /**
     * Helper function for {@link #finishResolution}.
     */
    protected boolean allAnimationsResolved ()
    {
        for (Animation anim : _anims.values()) {
            if (anim.isResolving()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a comma separated list from the supplied properties file,
     * using the specified "parent" property if the main property is not
     * set.
     */
    protected String[] getList (
        Properties props, String key, String fallback, boolean warn)
    {
        String value = props.getProperty(key);
        if (StringUtil.isBlank(value) && fallback != null) {
            value = props.getProperty(fallback);
        }
        if (StringUtil.isBlank(value)) {
            if (warn) {
                log.warning("Missing model list [model=" + _key +
                            ", key=" + key + ", fallback=" + fallback + "].");
            }
            return new String[0];
        }
        StringTokenizer ttok = new StringTokenizer(value, ", ");
        ArrayList<String> values = new ArrayList<String>();
        while (ttok.hasMoreTokens()) {
            String val = ttok.nextToken().trim();
            if (!StringUtil.isBlank(val)) {
                values.add(val);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    protected CloneCreator loadModelCreator (
        String path, boolean trans, boolean solid, String bound, int[] loaded)
    {
        ModelCloneCreator cc = new ModelCloneCreator(
            loadModel(path, trans, solid, bound, loaded));
        // these define what we want to "shallow" copy
        cc.addProperty("colors");
        cc.addProperty("texcoords");
        cc.addProperty("indices");
        return cc;
    }

    protected Node loadModel (
        String path, boolean trans, boolean solid, String bound, int[] loaded)
    {
        path = cleanPath(path);
        ClassLoader loader = getClass().getClassLoader();
        Node model = _meshes.get(path);
        if (model == null || _local) {
            JmeBinaryReader jbr = new JmeBinaryReader();
            jbr.setProperty("bound", bound);

            // our media is loaded from unpacked files
            if (_local) {
                // (re-)convert the source model
                attemptModelConversion(path);
            }
            File file = _local ? new File(path) : 
                _ctx.getResourceManager().getResourceFile(path);
            if (file.exists()) {
                try {
                    jbr.setProperty("texurl", file.toURL());
                } catch (Exception e) {
                    log.warning("Failed to set texture URL [file=" + file +
                                ", err=" + e + "].");
                }
                try {
                    model = jbr.loadBinaryFormat(
                        new BufferedInputStream(new FileInputStream(file)));

                    // note the number of bytes we loaded
                    loaded[0] += file.length();

                    // TODO: put this in the model config file
                    if (path.indexOf("units") != -1) {
                        model.setLocalScale(0.04f);
                    } else {
                        model.setLocalScale(0.05f);
                    }

                    // configure transparent models specially
                    if (trans) {
                        model.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
                        model.setRenderState(RenderUtil.blendAlpha);
                        model.setRenderState(RenderUtil.overlayZBuf);
                        centerOrigins(model);   
                    }
                    
                    // enable back-face culling for solid models
                    if (solid) {
                        model.setRenderState(RenderUtil.frontCull);
                    }

                } catch (IOException ioe) {
                    log.log(Level.WARNING,
                            "Failed to load mesh " + path + ".", ioe);
                }
            } else {
                log.warning("Missing model " + file + ".");
            }

            if (model == null) {
                model = new Node("error");
                Box box = new Box(
                    "error", new Vector3f(1, 1, 0),
                    new Vector3f(TILE_SIZE-1, TILE_SIZE-1, TILE_SIZE-2));
                box.setModelBound(new BoundingBox());
                box.updateModelBound();
                model.attachChild(box);
            }

            _meshes.put(path, model);
        }
        return model;
    }

    /**
     * Attempts to locate a non-JME model corresponding to the given path
     * and convert it to a JME model.
     */
    protected void attemptModelConversion (String path)
    {
        String root = path.substring(0, path.lastIndexOf('.')+1);
        File test = new File(root + "MD3");
        if (test.exists()) {
            new Md3ToJme().attemptFileConvert(
                new String[] { test.toString(), path });
        }
    }
    
    /**
     * Recursively descends the scene graph, making sure that the origins of
     * each {@link Geometry} object encountered are located at the object's
     * bounding volume center.
     */
    protected void centerOrigins (Spatial spatial)
    {
        if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                centerOrigins(node.getChild(ii));
            }
        } else if (spatial instanceof Geometry) {
            Geometry geom = (Geometry)spatial;
            Vector3f mcenter = geom.getModelBound().getCenter(),
                offset = geom.getLocalTranslation().subtract(mcenter);
            if (!offset.equals(Vector3f.ZERO)) {
                geom.getLocalTranslation().subtractLocal(offset);
                geom.getModelBound().setCenter(mcenter.addLocal(offset));
                geom.getBatch().translatePoints(offset);
            }
        }
    }
    
    protected String cleanPath (String path)
    {
        // non-file URLs don't handle blah/foo/../bar so we make those path
        // adjustments by hand
        String npath = path.replaceAll(PATH_DOTDOT, "");
        while (!npath.equals(path)) {
            path = npath;
            npath = path.replaceAll(PATH_DOTDOT, "");
        }
        return npath;
    }

    /** Contains information on one part of a model. */
    protected class Part
    {
        /** Used to create a clone of non-static models. */
        public CloneCreator creator;

        /** Used as a target for static models. */
        public Node target;

        /** The paths of the textures from which to select randomly. */
        public String[] tpaths;

        /** The animation controller to clone for this part, if any. */
        public AnimationController animctrl;

        /** Maps indices/colorizations to precreated texture states. */
        public HashMap<TextureKey, WeakReference<TextureState>> tstates =
            new HashMap<TextureKey, WeakReference<TextureState>>();

        /**
         * Creates either a copy of the original part node or a
         * {@link SharedNode} reference to a locked target, depending on
         * whether or not the part is static.
         *
         * @param random a random number used to determine which texture to
         * use
         * @param zations the colorizations to apply to the texture, or
         * <code>null</code> for none
         */
        public Node createInstance (int random, Colorization[] zations)
        {
            Node instance;
            if (creator != null) {
                instance = (Node)creator.createCopy();

            } else { // target != null
                if (!_tinit) {
                    // in order to ensure that texture coords are sent when
                    // compiling the shared geometry to a display list, we must
                    // include a dummy texture state
                    Renderer renderer = DisplaySystem.getDisplaySystem().
                        getRenderer();
                    TextureState tstate = renderer.createTextureState();
                    tstate.setTexture(null, 0);
                    target.setRenderState(tstate);

                    if (Config.useVBOs && renderer.supportsVBO()) {
                        setVBOInfos(target);
                        
                    } else if (Config.useDisplayLists) {
                        target.lockMeshes();
                    }
                    target.lockBounds();
                    
                    target.clearRenderState(RenderState.RS_TEXTURE);

                    target.updateCollisionTree();

                    // we create a SharedNode of a SharedNode because rendering
                    // each SharedMesh modifies the transform of its target,
                    // would then be copied by SharedMeshes created afterwards
                    target = new SharedNode("shared", target, false);

                    _tinit = true;
                }
                instance = new SharedNode("shared", target, false);
            }
            if (tpaths != null) {
                TextureKey tkey = new TextureKey(random % tpaths.length,
                    zations);
                WeakReference<TextureState> tref = tstates.get(tkey);
                TextureState tstate;
                if (tref == null || (tstate = tref.get()) == null) {
                    tstate = _ctx.getRenderer().createTextureState();
                    Texture tex;
                    if (_local) {
                        tex = RenderUtil.loadTexture(_ctx, tpaths[tkey.idx],
                            zations);
                    } else {
                        tex = _ctx.getTextureCache().getTexture(
                            tpaths[tkey.idx], zations);
                    }
                    tex.setWrap(Texture.WM_WRAP_S_WRAP_T);
                    tstate.setTexture(tex);
                    tstates.put(tkey, new WeakReference<TextureState>(tstate));
                }
                instance.setRenderState(tstate);
            }
            if (animctrl != null) {
                AnimationController ctrl =
                    (AnimationController)animctrl.clone();
                ctrl.setTarget(instance);
                instance.addController(ctrl);
            }
            return instance;
        }

        /**
         * Recursively descends the scene graph rooted at the supplied spatial,
         * setting the {@link VBOInfo}s of any encountered {@link Geometry}
         * nodes.
         */
        protected void setVBOInfos (Spatial spatial)
        {
            if (spatial instanceof Geometry) {
                VBOInfo vboinfo = new VBOInfo(true);
                vboinfo.setVBOIndexEnabled(true);
                ((Geometry)spatial).setVBOInfo(vboinfo);

            } else if (spatial instanceof Node) {
                Node node = (Node)spatial;
                for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                    setVBOInfos(node.getChild(ii));
                }
            }
        }

        /** Whether or not the target has been initialized. */
        protected boolean _tinit;
    }

    /** Identifies a specific part texture by index and colorizations. */
    protected static class TextureKey
    {
        public int idx;
        public Colorization[] zations;

        public TextureKey (int idx, Colorization[] zations)
        {
            this.idx = idx;
            this.zations = zations;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return idx ^ Arrays.hashCode(zations);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            TextureKey okey = (TextureKey)other;
            return idx == okey.idx && Arrays.equals(zations, okey.zations);
        }
    }

    protected BasicContext _ctx;
    protected String _key;
    protected Properties _props;
    protected Animation _ianim;
    protected ArrayList<ResolutionObserver> _observers =
        new ArrayList<ResolutionObserver>();

    protected HashMap<String,Node> _meshes =
        new HashMap<String,Node>();
    protected HashMap<String,Animation> _anims =
        new HashMap<String,Animation>();

    protected boolean _local;
    protected String[] _actions;
    
    /** This asynchronous thread loads our models in the background. */
    protected static ModelLoader _loader;

    protected static final Animation BLANK_ANIM = new Animation();
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";

    static {
        BLANK_ANIM.frames = 1;
        BLANK_ANIM.duration = 100;
        BLANK_ANIM.emitters = new Emitter[0];
    }
}
