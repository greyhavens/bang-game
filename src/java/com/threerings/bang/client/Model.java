//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

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
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;
import com.jmex.model.ModelCloneCreator;
import com.jmex.model.XMLparser.JmeBinaryReader;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.TextureIcon;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.client.util.ModelLoader;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains information on a single logical "model" which may be made up
 * of multiple meshes each with various animations and whatnot.
 */
public class Model
{
    /**
     * Used to bind an animation's meshs to a node and then later remove them.
     * The binding will take care of updating the node if the animation was not
     * fully resolved at the time that it was requested.
     */
    public static class Binding
    {
        public void detach ()
        {
            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.detachChild(_meshes[ii]);
            }
            _anim.clearBinding(this);
        }

        protected Binding (Animation anim, Node node, int random)
        {
            _anim = anim;
            _node = node;
            _random = random;
            _meshes = _anim.getMeshes(_random);
            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.attachChild(_meshes[ii]);
                _meshes[ii].updateRenderState();
            }
        }

        protected void update ()
        {
            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.detachChild(_meshes[ii]);
            }
            _meshes = _anim.getMeshes(_random);
            for (int ii = 0; ii < _meshes.length; ii++) {
                _node.attachChild(_meshes[ii]);
                _meshes[ii].updateRenderState();
            }
        }

        protected Animation _anim;
        protected Node _node;
        protected int _random;
        protected Node[] _meshes;
    }

    /** Information on an animation which is a collection of animated meshes
     * with a frame count and a duration. */
    public static class Animation
    {
        /** The number of frames in this animation. */
        public int frames;

        /** The duration of one cycle of the animation in milliseconds. */
        public int duration;

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
         * Returns true if this animation is resolved or has at least been
         * queued up to be resolved.
         */
        public boolean isResolved ()
        {
            return _parts != null;
        }

        /**
         * Binds this animation to the specified node. <em>Note:</em> the
         * binding <em>must</em> be cleared later via {@link Binding#detach}.
         *
         * @see #getMeshes
         */
        public Binding bind (Node node, int random)
        {
            Binding binding = new Binding(this, node, random);
            _bindings.add(binding);
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
         */
        public Node[] getMeshes (int random)
        {
            // return an empty set of meshes if our parts are not resolved
            if (_parts == null) {
                return new Node[0];
            }

            Node[] nodes = new Node[_parts.length];
            for (int ii = 0; ii < nodes.length; ii++) {
                nodes[ii] = (Node)_parts[ii].creator.createCopy();
                // select a random texture state
                if (_parts[ii].tstates != null) {
                    TextureState tstate =
                        _parts[ii].tstates[random % _parts[ii].tstates.length];
                    nodes[ii].setRenderState(tstate);
                    nodes[ii].updateRenderState();
                }
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
            for (Binding binding : _bindings) {
                binding.update();
            }
        }

        /**
         * Clears the specified binding.
         */
        protected void clearBinding (Binding binding)
        {
            _bindings.remove(binding);
        }

        /** The animated meshes that make up the animation. */
        protected Part[] _parts;

        /** Use to track the nodes to which this animation is bound. */
        protected ArrayList<Binding> _bindings = new ArrayList<Binding>();
    }

    /** The size along each axis of the model icon. */
    public static final int ICON_SIZE = 128;

    /**
     * Creates the model and loads up all of its consituent animations.
     */
    public Model (BasicContext ctx, String type, String name)
    {
        _key = type + "/" + name;

        _props = new Properties();
        String path = "rsrc/" + _key + "/model.properties";
        try {
            InputStream pin =
                getClass().getClassLoader().getResourceAsStream(path);
            if (pin != null) {
                _props.load(new BufferedInputStream(pin));
            } else {
                log.info("Faking model info file for " + _key + ".");
                String mname = name.substring(name.lastIndexOf("/")+1);
                _props.setProperty("meshes", mname);
            }
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load model info " +
                    "[path=" + path + "].", ioe);
        }

        // TODO: use a whole different system for non-animating models
        // that uses a SharedMesh

        // start up our background loader thread if it hasn't been
        if (_loader == null) {
            _loader = new ModelLoader(ctx);
            _loader.start();
        }

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
            _anims.put(action, anim);
        }

        // TEMP: use the first frame of the walking pose if this model has no
        // standing pose defined
        if (!_anims.containsKey("standing") && _anims.containsKey("walking")) {
            _anims.put("standing", _anims.get("walking"));
        }

        // load up the action we need to create our icon image
        String iaction = _props.getProperty("icon");
        if (iaction != null && (_ianim = _anims.get(iaction)) != null) {
            _loader.queueAction(this, iaction);
        }
    }

    /**
     * Returns a pre-rendered icon version of this model.
     */
    public BIcon getIcon ()
    {
        return _icon;
    }

    /**
     * Returns true if we have meshes for the specified action.
     */
    public boolean hasAnimation (String action)
    {
        return _anims.containsKey(action);
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
            anim.setParts(new Part[0]);
            _loader.queueAction(this, action);
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
                anim.setParts(new Part[0]);
                _loader.queueAction(this, action);
            }
        }
    }

    /**
     * Resolves the mesh and texture data associated with the specified
     * action. <em>Note:</em> this is called on the background model loader
     * thread and must be very careful not to use non-thread-safe services from
     * the supplied context.
     */
    public void resolveAction (final BasicContext ctx, String action)
    {
        final Animation anim = _anims.get(action);
        anim.frames = BangUtil.getIntProperty(
            _key, _props, action + ".frames", 8);
        anim.duration = BangUtil.getIntProperty(
            _key, _props, action + ".duration", 250);

        String path = _key + "/";
        String[] pnames = getList(_props, action + ".meshes", "meshes", true);
        final Part[] parts = new Part[pnames.length];
        final Texture[][] textures = new Texture[pnames.length][];
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
            part.creator = loadModel(
                ctx, path + action + "/" + mesh + ".jme", trans);

            // the model may have multiple textures from which we
            // select at random
            String[] tnames =
                getList(_props, mesh + ".texture", "texture", false);
            if (tnames.length == 0) {
                continue;
            }
            textures[pp] = new Texture[tnames.length];
            for (int tt = 0; tt < tnames.length; tt++) {
                // we load the texture data on the background thread, but the
                // creation of the texture states must take place on the main
                // thread as it does OpenGL things
                textures[pp][tt] = ctx.getTextureCache().getTexture(
                    // TODO: make the texture cache thread safe
                    cleanPath(path + tnames[tt]));
            }
        }

        // now finish our resolution on the main thread
        ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                finishResolution(ctx, anim, parts, textures);
            }
        });
    }

    /**
     * Finishes the resolution of a model animation. <em>Note:</em> this is
     * called on the main thread where we can safely access OpenGL.
     */
    protected void finishResolution (BasicContext ctx, Animation anim,
                                     Part[] parts, Texture[][] textures)
    {
        for (int ii = 0; ii < parts.length; ii++) {
            if (textures[ii] == null) {
                continue;
            }
            Part part = parts[ii];
            part.tstates = new TextureState[textures[ii].length];
            for (int tt = 0; tt < part.tstates.length; tt++) {
                part.tstates[tt] = ctx.getRenderer().createTextureState();
                textures[ii][tt].setWrap(Texture.WM_WRAP_S_WRAP_T);
                part.tstates[tt].setTexture(textures[ii][tt]);
                part.tstates[tt].setEnabled(true);
            }
        }
        anim.setParts(parts);

        // create the icon image for this model if it wants one
        if (anim == _ianim) {
            try {
                createIconImage(ctx);
            } catch (Throwable t) {
                log.log(Level.WARNING, "Failed to create icon image " +
                        "[key=" + _key + "].", t);
            }
        }
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
        return _key + " m:" + _meshes.size() + " t:" + _textures.size() +
            " a:" + _anims.size() + ")";
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

    protected CloneCreator loadModel (
        BasicContext ctx, String path, boolean trans)
    {
        path = cleanPath(path);
        ClassLoader loader = getClass().getClassLoader();
        CloneCreator cc = _meshes.get(path);
        if (cc == null) {
            JmeBinaryReader jbr = new JmeBinaryReader();
            jbr.setProperty("bound", "box");
            jbr.setProperty("texurl", loader.getResource("rsrc/" + path));
            InputStream in = loader.getResourceAsStream("rsrc/" + path);

            Node model = null;
            if (in != null) {
                try {
                    model = jbr.loadBinaryFormat(new BufferedInputStream(in));
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
                    }

                } catch (IOException ioe) {
                    log.log(Level.WARNING,
                            "Failed to load mesh " + path + ".", ioe);
                }
            } else {
                log.warning("Missing model " + path + ".");
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

            cc = new ModelCloneCreator(model);
            // these define what we want to "shallow" copy
            cc.addProperty("colors");
            cc.addProperty("texcoords");
            cc.addProperty("indices");
            _meshes.put(path, cc);
        }
        return cc;
    }

    protected void createIconImage (BasicContext ctx)
    {
        TextureRenderer trenderer =
            ctx.getDisplay().createTextureRenderer(
                ICON_SIZE, ICON_SIZE, false, true, false, false,
                TextureRenderer.RENDER_TEXTURE_2D, 0);
        trenderer.setBackgroundColor(new ColorRGBA(.9f, .9f, .9f, 0f));

        Vector3f loc = new Vector3f(TILE_SIZE/2, -TILE_SIZE, TILE_SIZE);
        Camera cam = trenderer.getCamera();
        cam.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/2, cam.getLeft());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());
        rotm.fromAngleAxis(FastMath.PI/6, cam.getUp());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());
        rotm.fromAngleAxis(FastMath.PI/6, cam.getLeft());
        rotm.mult(cam.getDirection(), cam.getDirection());
        rotm.mult(cam.getUp(), cam.getUp());
        rotm.mult(cam.getLeft(), cam.getLeft());
        cam.update();

        Texture icon = trenderer.setupTexture();
        icon.setWrap(Texture.WM_CLAMP_S_CLAMP_T);

        Node all = new Node("all");
        all.setRenderQueueMode(Renderer.QUEUE_SKIP);

        all.attachChild(new Box("origin", new Vector3f(-0.01f, -0.01f, -0.01f),
                                new Vector3f(.01f, .01f, .01f)));

        PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        light.setAmbient(new ColorRGBA(0.75f, 0.75f, 0.75f, 1.0f));
        light.setLocation(new Vector3f(100, 100, 100));
        light.setAttenuate(true);
        light.setConstant(0.25f);
        light.setEnabled(true);

        LightState lights = ctx.getRenderer().createLightState();
        lights.setEnabled(true);
        lights.attach(light);
        all.setRenderState(lights);

        Node[] meshes = getAnimation("standing").getMeshes(0);
        for (int ii = 0; ii < meshes.length; ii++) {
            all.attachChild(meshes[ii]);
        }

        ZBufferState buf = ctx.getRenderer().createZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.CF_LEQUAL);
        all.setRenderState(buf);

        all.updateGeometricState(0, true);
        all.updateRenderState();

        trenderer.render(all, icon);
        trenderer.cleanup();

        // restore the normal view camera
        ctx.getCameraHandler().getCamera().update();

        // configure our icon with the rendered texture
        _icon.setTexture(icon);
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
    protected static class Part
    {
        /** Used to create a clone of the model. */
        public CloneCreator creator;

        /** A list of texture states from which to select randomly. */
        public TextureState[] tstates;
    }

    protected String _key;
    protected TextureIcon _icon = new TextureIcon(ICON_SIZE, ICON_SIZE);
    protected Properties _props;
    protected Animation _ianim;

    protected HashMap<String,CloneCreator> _meshes =
        new HashMap<String,CloneCreator>();

    protected HashMap<String,TextureState> _textures =
        new HashMap<String,TextureState>();

    protected HashMap<String,Animation> _anims = new HashMap<String,Animation>();

    /** This asynchronous thread loads our models in the background. */
    protected static ModelLoader _loader;

    protected static final Animation BLANK_ANIM = new Animation();
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";

    static {
        BLANK_ANIM.frames = 1;
        BLANK_ANIM.duration = 100;
        BLANK_ANIM.setParts(new Part[0]);
    }
}
