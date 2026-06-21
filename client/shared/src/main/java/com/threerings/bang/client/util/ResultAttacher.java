//
// $Id$

package com.threerings.bang.client.util;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.samskivert.util.ResultListener;

/**
 * Attaches a {@link Spatial} to a node when it becomes available.
 */
public class ResultAttacher<T extends Spatial>
    implements ResultListener<T>
{
    public ResultAttacher (Node parent)
    {
        _parent = parent;
    }
    
    // documentation inherited from interface ResultListener
    public void requestCompleted (T result)
    {
        // jME3 cutover: the fork required an explicit updateRenderState() after attach; jME3
        // refreshes material/light state automatically during its update pass.
        _parent.attachChild(result);
    }
    
    // documentation inherited from interface ResultListener
    public void requestFailed (Exception cause)
    {
        // reported in SpatialCache
    }
    
    /** The parent node to which the result will be attached. */
    protected Node _parent;
}
