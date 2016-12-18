//
// $Id$

package com.threerings.bang.client.util;

import com.jme.scene.Node;
import com.jme.scene.Spatial;

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
        _parent.attachChild(result);
        result.updateRenderState();
    }
    
    // documentation inherited from interface ResultListener
    public void requestFailed (Exception cause)
    {
        // reported in SpatialCache
    }
    
    /** The parent node to which the result will be attached. */
    protected Node _parent;
}
