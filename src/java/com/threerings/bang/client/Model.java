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
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.CloneCreator;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.model.ModelCloneCreator;
import com.jmex.model.XMLparser.JmeBinaryReader;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Contains information on a single logical "model" which may be made up
 * of multiple meshes each with various animations and whatnot.
 */
public class Model
{
    /**
     * Creates the model and loads up all of its consituent animations.
     */
    public Model (BangContext ctx, String type, String name)
    {
        _key = type + "." + name;

        String path = type + "/" + name + "/";
        Properties props = new Properties();
        try {
            InputStream pin = getClass().getClassLoader().getResourceAsStream(
                "rsrc/" + path + "model.properties");
            if (pin != null) {
                props.load(new BufferedInputStream(pin));
            } else {
                log.info("Faking model info file for " + _key + ".");
                props.setProperty("standing.meshes", name);
                props.setProperty("walking.meshes", name);
            }
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load model info " +
                    "[path=" + path + "].", ioe);
        }

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
                    models[ii] = loadModel(
                        ctx, path + "/" + mesh + ".jme", path + "/" + texture);
                }
                _anims.put(aname, models);
            }
        }
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
        ClassLoader loader = getClass().getClassLoader();
        CloneCreator cc = _meshes.get(path);
        if (cc == null) {
            JmeBinaryReader jbr = new JmeBinaryReader();
            jbr.setProperty("bound", "box");
            jbr.setProperty("texurl", loader.getResource("rsrc/" + texpath));
            InputStream in = loader.getResourceAsStream("rsrc/" + path);

            Node model;
            try {
                model = jbr.loadBinaryFormat(new BufferedInputStream(in));

                // TODO: put these bits in the config file
                model.setLocalScale(0.05f);
                model.setLocalTranslation(
                    new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0));

            } catch (IOException ioe) {
                log.log(Level.WARNING,
                        "Failed to load mesh " + path + ".", ioe);
                model = new Node("error");
                Box box = new Box(
                    "error", new Vector3f(1, 1, 0),
                    new Vector3f(TILE_SIZE-1, TILE_SIZE-1, TILE_SIZE-2));
                box.setModelBound(new BoundingBox());
                box.updateModelBound();
                model.attachChild(box);
            }

            TextureState ts = getTexture(ctx, texpath);
            if (ts != null) {
                model.setRenderState(ts);
                model.updateRenderState();
            }

            if (true /* TODO: note rotation in config file */) {
                Quaternion r1 = new Quaternion();
                r1.fromAngleAxis(-FastMath.PI/2, new Vector3f(-1,0,0));
                Quaternion r2 = new Quaternion();
                r2.fromAngleAxis(FastMath.PI/2, new Vector3f(0,1,0));
                r1.multLocal(r2);
                model.setLocalRotation(r1);
            }

            cc = new ModelCloneCreator(model);
            cc.addProperty("colors");
            cc.addProperty("texcoords");
            cc.addProperty("vertices");
            cc.addProperty("normals");
            cc.addProperty("indices");
            cc.addProperty("spatialcontroller");
            cc.addProperty("jointcontroller");
            _meshes.put(path, cc);
        }
        return cc;
    }

    protected TextureState getTexture (BangContext ctx, String texpath)
    {
        TextureState ts = _textures.get(texpath);
        if (ts == null) {
            ts = ctx.getRenderer().createTextureState();
            ts.setTexture(
                TextureManager.loadTexture(
                    getClass().getClassLoader().getResource("rsrc/" + texpath),
                    Texture.MM_LINEAR, Texture.FM_LINEAR));
            ts.setEnabled(true);
            _textures.put(texpath, ts);
        }
        return ts;
    }

    protected String _key;

    protected HashMap<String,CloneCreator> _meshes =
        new HashMap<String,CloneCreator>();

    protected HashMap<String,TextureState> _textures =
        new HashMap<String,TextureState>();

    protected HashMap<String,CloneCreator[]> _anims =
        new HashMap<String,CloneCreator[]>();
}
