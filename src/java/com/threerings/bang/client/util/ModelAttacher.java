//
// $Id$

package com.threerings.bang.client.util;

import com.jme.scene.Node;

import com.samskivert.util.ResultListener;

import com.threerings.jme.model.Model;

/**
 * Attaches a model to a node when it becomes available.
 */
public class ModelAttacher
    implements ResultListener<Model>
{
    public ModelAttacher (Node parent)
    {
        _parent = parent;
    }
    
    // documentation inherited from interface ResultListener
    public void requestCompleted (Model model)
    {
        _parent.attachChild(model);
        model.updateRenderState();
    }
    
    // documentation inherited from interface ResultListener
    public void requestFailed (Exception cause)
    {
        // reported in ModelCache
    }
    
    /** The parent node to which the model will be attached. */
    protected Node _parent;
}
