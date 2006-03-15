//
// $Id$

package com.threerings.bang.client.util;

import java.util.Properties;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.*;

/**
 * A rotation animation with constant angular velocity.
 */
public class RotationController extends AnimationController
{
    public RotationController (Properties props)
    {
        String axisstr = props.getProperty("axis", "x"),
            rpsstr = props.getProperty("radpersec", "3.14");
        if (axisstr.equalsIgnoreCase("x")) {
            _axis = Vector3f.UNIT_X;
        } else if (axisstr.equalsIgnoreCase("y")) {
            _axis = Vector3f.UNIT_Y;
        } else if (axisstr.equalsIgnoreCase("z")) {
            _axis = Vector3f.UNIT_Z;
        } else {
            float[] axis = StringUtil.parseFloatArray(axisstr);
            if (axis != null && axis.length == 3) {
                _axis = new Vector3f(axis[0], axis[1],
                    axis[2]).normalizeLocal();
                    
            } else {
                log.warning("Invalid rotation axis [axis=" + axisstr + "].");
            }
        }
        try {
            _radpersec = Float.parseFloat(rpsstr);
        } catch (NumberFormatException e) {
            log.warning("Invalid rotation rate [radpersec=" + rpsstr + "].");
        }
    }
    
    // documentation inherited
    public void update (float time)
    {
        // apply separately to all meshes under the target
        _rot.fromAngleNormalAxis(time * _radpersec, _axis);
        applyRotation(_target);
    }
    
    /**
     * Applies the computed rotation to all {@link Geometry} nodes under the
     * parameter.
     */
    protected void applyRotation (Spatial spatial)
    {
        if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                applyRotation(node.getChild(ii));
            }
            
        } else if (spatial instanceof Geometry) {
            ((Geometry)spatial).getModelBound().getCenter(_center);
            Quaternion lrot = spatial.getLocalRotation().multLocal(_rot);
            lrot.mult(_center, _trans);
            _center.subtract(_trans, spatial.getLocalTranslation());
        }
    }
    
    protected Vector3f _axis;
    protected float _radpersec;
    protected Quaternion _rot = new Quaternion();
    protected Vector3f _center = new Vector3f(), _trans = new Vector3f();
}
