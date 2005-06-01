//
// $Id$

package com.threerings.bang.client;

import java.util.HashMap;

import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of resolved 3D models.
 */
public class ModelCache
{
    public ModelCache (BangContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Loads up the specified model and all of its associated animations.
     */
    public Model getModel (String type, String name)
    {
        String key = type + "." + name;
        Model model = _models.get(key);
        if (model == null) {
            _models.put(key, model = new Model(_ctx, type, name));
        }
        return model;
    }

    protected void dump (Spatial spatial, String indent)
    {
        spatial.updateWorldBound();
        String extra = " (" + spatial.getWorldBound() + ")";
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
    protected HashMap<String,Model> _models = new HashMap<String,Model>();
}
