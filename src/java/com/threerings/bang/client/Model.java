//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;
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

import com.jme.bui.BIcon;
import com.jme.bui.BTextureIcon;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains information on a single logical "model" which may be made up
 * of multiple meshes each with various animations and whatnot.
 */
public class Model
{
    /** The size along each axis of the model icon. */
    public static final int ICON_SIZE = 128;

    /**
     * Creates the model and loads up all of its consituent animations.
     */
    public Model (BangContext ctx, String type, String name)
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
                props.setProperty("standing.meshes", mname);
                props.setProperty("walking.meshes", mname);
            }
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load model info " +
                    "[path=" + path + "].", ioe);
        }

        // TODO: use a whole different system for non-animating models
        // that uses a SharedMesh

        Enumeration kenum = props.propertyNames();
        while (kenum.hasMoreElements()) {
            String pkey = (String)kenum.nextElement();
            String pval = props.getProperty(pkey);
            int midx;
            if ((midx = pkey.indexOf(".meshes")) != -1) {
                String aname = pkey.substring(0, midx);
                StringTokenizer tok = new StringTokenizer(pval, ", ");
                CloneCreator[] models = new CloneCreator[tok.countTokens()];
                for (int ii = 0; ii < models.length; ii++) {
                    String mesh = tok.nextToken();
                    String texture = props.getProperty(mesh + ".texture");
                    if (texture == null) {
                        texture = props.getProperty("texture");
                    }
                    if (texture != null) {
                        texture = path + texture;
                    }
                    models[ii] = loadModel(ctx, path + mesh + ".jme", texture);
                }
                _anims.put(aname, models);
            }
        }

        // create the icon image for this model if it's a unit
        if (type.equals("units")) {
            TextureRenderer trenderer =
                ctx.getDisplay().createTextureRenderer(
                    ICON_SIZE, ICON_SIZE, false, true, false, false,
                    TextureRenderer.RENDER_TEXTURE_2D, 0);
            trenderer.setBackgroundColor(new ColorRGBA(.667f, .667f, .851f, 0f));

            Vector3f loc = new Vector3f(TILE_SIZE/2, -TILE_SIZE, TILE_SIZE);
            trenderer.getCamera().setLocation(loc);
            Matrix3f rotm = new Matrix3f();
            rotm.fromAngleAxis(-FastMath.PI/2, trenderer.getCamera().getLeft());
            rotm.mult(trenderer.getCamera().getDirection(),
                      trenderer.getCamera().getDirection());
            rotm.mult(trenderer.getCamera().getUp(),
                      trenderer.getCamera().getUp());
            rotm.mult(trenderer.getCamera().getLeft(),
                      trenderer.getCamera().getLeft());
            rotm.fromAngleAxis(FastMath.PI/6, trenderer.getCamera().getUp());
            rotm.mult(trenderer.getCamera().getDirection(),
                      trenderer.getCamera().getDirection());
            rotm.mult(trenderer.getCamera().getUp(),
                      trenderer.getCamera().getUp());
            rotm.mult(trenderer.getCamera().getLeft(),
                      trenderer.getCamera().getLeft());
            rotm.fromAngleAxis(FastMath.PI/6, trenderer.getCamera().getLeft());
            rotm.mult(trenderer.getCamera().getDirection(),
                      trenderer.getCamera().getDirection());
            rotm.mult(trenderer.getCamera().getUp(),
                      trenderer.getCamera().getUp());
            rotm.mult(trenderer.getCamera().getLeft(),
                      trenderer.getCamera().getLeft());
            trenderer.getCamera().update();

            _icon = trenderer.setupTexture();
            _icon.setWrap(Texture.WM_CLAMP_S_CLAMP_T);

            Node all = new Node("all");
            all.setRenderQueueMode(Renderer.QUEUE_SKIP);

            all.attachChild(new Box("origin", new Vector3f(0.01f, .01f, .01f),
                                    new Vector3f(.02f, .02f, .02f)));

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

            Node[] meshes = getMeshes("standing");
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
            ctx.getCamera().update();
        }
    }

    /**
     * Returns a pre-rendered icon version of this model.
     */
    public BIcon getIcon ()
    {
        return new BTextureIcon(_icon, ICON_SIZE, ICON_SIZE);
    }

    public Node[] getMeshes (String action)
    {
        CloneCreator[] cc = _anims.get(action);
        if (cc == null) {
            log.warning("Requested unknown action [model=" + _key +
                        ", action=" + action + "].");
            return new Node[0];
        }
        Node[] nodes = new Node[cc.length];
        for (int ii = 0; ii < cc.length; ii++) {
            nodes[ii] = (Node)cc[ii].createCopy();
        }
        return nodes;
    }

    public boolean equals (Object other)
    {
        return _key.equals(((Model)other)._key);
    }

    public int hashCode ()
    {
        return _key.hashCode();
    }

    public String toString ()
    {
        return _key + " m:" + _meshes.size() + " t:" + _textures.size() +
            " a:" + _anims.size() + ")";
    }

    protected CloneCreator loadModel (
        BangContext ctx, String path, String texpath)
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

            if (texpath != null) {
                TextureState ts = getTexture(ctx, texpath);
                if (ts != null) {
                    model.setRenderState(ts);
                    model.updateRenderState();
                }
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

    protected TextureState getTexture (BangContext ctx, String texpath)
    {
        texpath = cleanPath(texpath);
        TextureState ts = _textures.get(texpath);
        if (ts == null) {
            ts = ctx.getRenderer().createTextureState();
            ts.setTexture(
                TextureManager.loadTexture(
                    getClass().getClassLoader().getResource("rsrc/" + texpath),
                    Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR));
            ts.setEnabled(true);
            _textures.put(texpath, ts);
        }
        return ts;
    }

    protected String cleanPath (String path)
    {
        // non-file URLs don't handle blah/foo/../bar so we make those
        // path adjustments by hand
        return path.replaceAll("/[^/]+/\\.\\.", "");
    }

    protected String _key;
    protected Texture _icon;

    protected HashMap<String,CloneCreator> _meshes =
        new HashMap<String,CloneCreator>();

    protected HashMap<String,TextureState> _textures =
        new HashMap<String,TextureState>();

    protected HashMap<String,CloneCreator[]> _anims =
        new HashMap<String,CloneCreator[]>();
}
