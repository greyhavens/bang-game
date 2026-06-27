//
// $Id$

package com.threerings.jme.model;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Runtime jME3 port of the fork's {@code com.threerings.jme.model.Rotator} procedural model
 * controller: spins the spatial it is attached to about a fixed axis at a constant angular velocity
 * (radians/second). The fork declared these in a model's {@code model.properties} (e.g. the
 * frontier-town windmill blades, circling vultures, the firestarter bonus); the jME3 converter
 * preserves the declaration as {@code bang.properties} user data but does not convert it, so
 * {@link Model#applyControllers} recreates the controller on each model instance using this control.
 *
 * <p>Matches the fork's per-frame update: {@code localRotation *= fromAngleNormalAxis(tpf*velocity,
 * axis)}.
 */
public class Rotator extends AbstractControl
{
    /**
     * @param axis the (already-chosen) rotation axis; normalized defensively.
     * @param velocity angular velocity in radians per second.
     */
    public Rotator (Vector3f axis, float velocity)
    {
        _axis = (axis == null) ? Vector3f.UNIT_X.clone() : axis.normalize();
        _velocity = velocity;
    }

    @Override // documentation inherited
    protected void controlUpdate (float tpf)
    {
        if (spatial == null) {
            return;
        }
        _rot.fromAngleNormalAxis(tpf * _velocity, _axis);
        // localRotation *= delta; setLocalRotation flags the transform dirty so the spin shows
        spatial.setLocalRotation(spatial.getLocalRotation().multLocal(_rot));
    }

    @Override // documentation inherited
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // no per-render work
    }

    protected final Vector3f _axis;
    protected final float _velocity;
    protected final Quaternion _rot = new Quaternion();
}
