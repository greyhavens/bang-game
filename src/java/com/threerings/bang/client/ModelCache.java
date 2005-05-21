//
// $Id$

package com.threerings.bang.client;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.CloneCreator;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jmex.model.XMLparser.Converters.MaxToJme;
import com.jmex.model.XMLparser.Converters.Md2ToJme;
import com.jmex.model.XMLparser.Converters.Md3ToJme;
import com.jmex.model.XMLparser.Converters.ObjToJme;
import com.jmex.model.XMLparser.JmeBinaryReader;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Used to load up 3D models.
 */
public class ModelCache
{
    public ModelCache (BangContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Returns a clone of the specified model, loading it into the cache
     * if necessary.
     */
    public Node getModel (String name)
    {
        CloneCreator cc = _models.get(name);
        if (cc == null) {
            _models.put(name, cc = loadModel(name));
        }

        Node copy = (Node)cc.createCopy();

        // random hackery
        if (copy.getChild(0).getControllers().size() !=0 ) {
            copy.getChild(0).getController(0).setSpeed(20);
        }

        return copy;
    }

    protected CloneCreator loadModel (String name)
    {
        Node model = loadJmeModel("media/models/" + name + ".jme");

        if (model == null) {
            model = new Node("error");
            Box box = new Box(
                "error", new Vector3f(1, 1, 0),

                new Vector3f(TILE_SIZE-1, TILE_SIZE-1, TILE_SIZE-2));
            box.setModelBound(new BoundingBox());
            box.updateModelBound();
            model.attachChild(box);

        } else {
            // temporary hackery
            if (name.equals("dirigible")) {
                model.setLocalScale(0.2f);
            } else {
                model.setLocalScale(0.05f);
            }
            model.setLocalTranslation(
                new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        }

        dump(model, "");

        CloneCreator cc = new CloneCreator(model);
        cc.addProperty("colors");
        cc.addProperty("texcoords");
        cc.addProperty("vertices");
        cc.addProperty("normals");
        cc.addProperty("indices");
        cc.addProperty("spatialcontroller");

        return cc;
    }

    protected Node loadJmeModel (String path)
    {
        try {
            JmeBinaryReader jbr = new JmeBinaryReader();
            jbr.setProperty("bound", "box");
            URL turl =
                getClass().getClassLoader().getResource("rsrc/media/textures/");
            jbr.setProperty("texurl", turl);
            InputStream in =
                getClass().getClassLoader().getResourceAsStream("rsrc/" + path);
            log.info("Loading " + path + " from " + in + ": " + in.available());
            return jbr.loadBinaryFormat(new BufferedInputStream(in));

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
            return null;
        }
    }

//     protected Node load3DSModel (String path)
//     {
//         MaxToJme converter = new MaxToJme();

//         try {
//             ByteArrayOutputStream bout = new ByteArrayOutputStream();
//             InputStream min = _ctx.getResourceManager().getResource(path);
//             converter.convert(new BufferedInputStream(min), bout);

//             JmeBinaryReader jbr = new JmeBinaryReader();
//             jbr.setProperty("bound", "box");
//             return jbr.loadBinaryFormat(
//                 new ByteArrayInputStream(bout.toByteArray()));

//         } catch (IOException ioe) {
//             log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
//             return null;
//         }
//     }

//     protected Node loadOBJModel (String path)
//     {
//         ObjToJme converter = new ObjToJme();

//         try {
//             ByteArrayOutputStream bout = new ByteArrayOutputStream();
//             URL murl = getClass().getClassLoader().getResource("rsrc/" + path);
//             converter.setProperty("mtllib", murl);

//             InputStream min = _ctx.getResourceManager().getResource(path);
//             converter.convert(new BufferedInputStream(min), bout);

//             JmeBinaryReader jbr = new JmeBinaryReader();
//             jbr.setProperty("bound", "box");
//             URL turl =
//                 getClass().getClassLoader().getResource("rsrc/media/textures/");
//             jbr.setProperty("texurl", turl);
//             return jbr.loadBinaryFormat(
//                 new ByteArrayInputStream(bout.toByteArray()));

//         } catch (IOException ioe) {
//             log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
//             return null;
//         }
//     }

//     protected Node loadMd2Model (String path)
//     {
//         Md2ToJme converter = new Md2ToJme();

//         try {
//             ByteArrayOutputStream bout = new ByteArrayOutputStream();
//             InputStream min = _ctx.getResourceManager().getResource(path);
//             converter.convert(new BufferedInputStream(min), bout);

//             JmeBinaryReader jbr = new JmeBinaryReader();
//             jbr.setProperty("bound", "box");
//             return jbr.loadBinaryFormat(
//                 new ByteArrayInputStream(bout.toByteArray()));

//         } catch (IOException ioe) {
//             log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
//             return null;
//         }
//     }

//     protected Node loadMd3Model (String path)
//     {
//         Md3ToJme converter = new Md3ToJme();

//         try {
//             ByteArrayOutputStream bout = new ByteArrayOutputStream();
//             InputStream min = _ctx.getResourceManager().getResource(path);
//             converter.convert(new BufferedInputStream(min), bout);

//             JmeBinaryReader jbr = new JmeBinaryReader();
//             jbr.setProperty("bound", "box");
//             Node model = jbr.loadBinaryFormat(
//                 new ByteArrayInputStream(bout.toByteArray()));
//             Quaternion r1 = new Quaternion();
//             r1.fromAngleAxis(-FastMath.PI/2, new Vector3f(-1,0,0));
//             Quaternion r2 = new Quaternion();
//             r2.fromAngleAxis(FastMath.PI/2, new Vector3f(0,1,0));
//             r1.multLocal(r2);
//             model.setLocalRotation(r1);
//             return model;

//         } catch (IOException ioe) {
//             log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
//             return null;
//         }
//     }

    protected void dump (Spatial spatial, String indent)
    {
        String extra = "";
        if (spatial instanceof Geometry) {
            extra = " (" + ((Geometry)spatial).getModelBound();
        }
        log.info(indent + spatial + extra);
        if (spatial instanceof Node) {
            indent += "  ";
            Node node = (Node)spatial;
            for (int ii = 0; ii < node.getQuantity(); ii++) {
                dump(node.getChild(ii), indent);
            }
        }
    }

    protected BangContext _ctx;
    protected HashMap<String,CloneCreator> _models =
        new HashMap<String,CloneCreator>();
}
