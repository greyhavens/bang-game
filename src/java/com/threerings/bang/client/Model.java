//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.TextureIcon;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains information on a single logical "model" which may be made up
 * of multiple meshes each with various animations and whatnot.
 */
public class Model
{
    /** Information on an animation which is a collection of animated
     * meshes with a frame count and a duration. */
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

        /** The animated meshes that make up the animation. */
        protected Part[] _parts;
    }

    /** The size along each axis of the model icon. */
    public static final int ICON_SIZE = 128;

    /**
     * Creates the model and loads up all of its consituent animations.
     */
    public Model (BasicContext ctx, String type, String name)
    {
        _key = type + "/" + name;

        String path = type + "/" + name + "/";
        Properties props = new Properties();
        try {
            InputStream pin = getClass().getClassLoader().getResourceAsStream(
                "rsrc/" + path + "model.properties");
            if (pin != null) {
                props.load(new BufferedInputStream(pin));
            } else {
                log.info("Faking model info file for " + _key + ".");
                String mname = name.substring(name.lastIndexOf("/")+1);
                props.setProperty("meshes", mname);
            }
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load model info " +
                    "[path=" + path + "].", ioe);
        }

        // TODO: use a whole different system for non-animating models
        // that uses a SharedMesh

        String[] actions = getList(props, "actions", null, true);
        for (int ii = 0; ii < actions.length; ii++) {
            String action = actions[ii];

            // skip commented out actions
            if (action.equals("#")) {
                ii++;
                continue;
            } else if (action.startsWith("#")) {
                continue;
            }

            Animation anim = new Animation();
            anim.frames = BangUtil.getIntProperty(
                _key, props, action + ".frames", 8);
            anim.duration = BangUtil.getIntProperty(
                _key, props, action + ".duration", 250);

            String[] pnames = getList(props, action + ".meshes", "meshes", true);
            anim._parts = new Part[pnames.length];
            for (int pp = 0; pp < pnames.length; pp++) {
                String mesh = pnames[pp];
                Part part = (anim._parts[pp] = new Part());

                // load up this part's 3D model
                boolean trans = props.getProperty(
                    mesh + ".transparent", "false").equalsIgnoreCase("true");
                part.creator = loadModel(
                    ctx, path + action + "/" + mesh + ".jme", trans);

                // the model may have multiple textures from which we
                // select at random
                String[] tnames =
                    getList(props, mesh + ".texture", "texture", false);
                if (tnames.length == 0) {
                    continue;
                }
                part.tstates = new TextureState[tnames.length];
                for (int tt = 0; tt < part.tstates.length; tt++) {
                    part.tstates[tt] = getTexture(ctx, path + tnames[tt]);
                }
            }
            _anims.put(action, anim);
        }

        // use the first frame of the walking pose if this model has no
        // standing pose defined and use the standing pose for walking if
        // the opposite is the case
        if (!_anims.containsKey("standing") && _anims.containsKey("walking")) {
            _anims.put("standing", _anims.get("walking"));
        }
        if (!_anims.containsKey("walking") && _anims.containsKey("standing")) {
            _anims.put("walking", _anims.get("standing"));
        }

        // create the icon image for this model if it's a unit
        if (type.equals("units")) {
            try {
                createIconImage(ctx);
            } catch (Throwable t) {
                log.log(Level.WARNING, "Failed to create icon image " +
                        "[type=" + type + ", name=" + name + "].", t);
            }
        }
    }

    /**
     * Returns a pre-rendered icon version of this model.
     */
    public BIcon getIcon ()
    {
        // temporarily cope with pbuffer failures
        return (_icon == null) ?
            new BlankIcon(ICON_SIZE, ICON_SIZE) : 
            new TextureIcon(_icon, ICON_SIZE, ICON_SIZE);
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
     * specified animation does not exist.
     */
    public Animation getAnimation (String action)
    {
        Animation anim = _anims.get(action);
        if (anim == null) {
            log.warning("Requested non-existent animation [model=" + _key +
                        ", anim=" + action + "].");
            return BLANK_ANIM;
        }
        return anim;
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

        _icon = trenderer.setupTexture();
        _icon.setWrap(Texture.WM_CLAMP_S_CLAMP_T);

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

        trenderer.render(all, _icon);
        trenderer.cleanup();

        // restore the normal view camera
        ctx.getCameraHandler().getCamera().update();
    }

    protected TextureState getTexture (BasicContext ctx, String texpath)
    {
        texpath = cleanPath(texpath);
        TextureState ts = _textures.get(texpath);
        if (ts == null) {
            ts = ctx.getRenderer().createTextureState();
            Texture tex = null;
            try {
                tex = TextureManager.loadTexture(
                    getClass().getClassLoader().getResource("rsrc/" + texpath),
                    Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR);
                if (tex == null) {
                    log.warning("Missing texture [path=" + texpath + "].");
                } else {
                    tex.setWrap(Texture.WM_WRAP_S_WRAP_T);
                    ts.setTexture(tex);
                    ts.setEnabled(true);
                }

            } catch (Exception e) {
                log.log(Level.WARNING, "Failure loading texture " +
                        "[path=" + texpath + "].", e);
            }
            _textures.put(texpath, ts);
        }
        return ts;
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
    protected Texture _icon;

    protected HashMap<String,CloneCreator> _meshes =
        new HashMap<String,CloneCreator>();

    protected HashMap<String,TextureState> _textures =
        new HashMap<String,TextureState>();

    protected HashMap<String,Animation> _anims = new HashMap<String,Animation>();

    protected static final Animation BLANK_ANIM = new Animation();
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";

    static {
        BLANK_ANIM._parts = new Part[0];
        BLANK_ANIM.frames = 1;
        BLANK_ANIM.duration = 100;
    }
}
