//
// $Id$

package com.threerings.bang.client.util;

import java.util.Properties;

import com.jme.scene.Controller;
import com.jme.scene.Node;

import com.threerings.bang.client.Model;

import static com.threerings.bang.Log.*;

/**
 * Defines a programmatic animation for part of a {@link Model}.
 */
public abstract class AnimationController extends Controller
    implements Cloneable
{
    /** A rotation at constant angular velocity. */
    public static final String ROTATION = "rotation";
    
    /**
     * Creates and returns an animation instance.
     *
     * @param type the type of animation desired
     * @param props the properties containing the configuration of the
     * animation
     */
    public static AnimationController create (String type, Properties props)
    {
        if (ROTATION.equals(type)) {
            return new RotationController(props);
            
        } else {
            log.warning("Requested invalid animation controller [type=" +
                type + "].");
            return null;
        }
    }
    
    /**
     * Sets the target of this controller.
     */
    public void setTarget (Node target)
    {
        _target = target;
    }
    
    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // should never happen
        }
    }
    
    /** The part instance being controlled. */
    protected Node _target;
}
