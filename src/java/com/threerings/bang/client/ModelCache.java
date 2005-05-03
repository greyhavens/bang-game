//
// $Id$

package com.threerings.bang.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.scene.CloneCreator;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.model.XMLparser.Converters.MaxToJme;
import com.jme.scene.model.XMLparser.JmeBinaryReader;
import com.jme.scene.shape.Box;

import com.threerings.bang.util.BangContext;

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
        String path = "media/models/" + name + ".3ds";
        Node model = null;
        try {
            MaxToJme converter = new MaxToJme();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            InputStream min = _ctx.getResourceManager().getResource(path);
            converter.convert(new BufferedInputStream(min), bout);

            JmeBinaryReader jbr = new JmeBinaryReader();
            jbr.setProperty("bound", "box");
            model = jbr.loadBinaryFormat(
                new ByteArrayInputStream(bout.toByteArray()));
            model.setLocalScale(0.06f);
            model.setLocalTranslation(
                new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));

        } catch (IOException ioe) {
            log.log(Level.WARNING, "Error loading model '" + path + "'.", ioe);
            model = new Node("error");
            Box box = new Box(
                "error", new Vector3f(1, 1, 0),
                new Vector3f(TILE_SIZE-1, TILE_SIZE-1, TILE_SIZE-2));
            box.setModelBound(new BoundingBox());
            box.updateModelBound();
            model.attachChild(box);
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
