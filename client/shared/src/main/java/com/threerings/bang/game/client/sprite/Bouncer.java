//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Bounces a sprite (or any spatial) up and down along the Z axis. Used for the
 * cursor.
 *
 * <p>jME3 cutover (Phase 2): was a fork {@code com.jme.scene.Controller}; now a jME3
 * {@link AbstractControl} added to its target via {@code target.addControl(new Bouncer(...))}
 * (BangBoardView relies on this).
 */
public class Bouncer extends AbstractControl
{
    /**
     * Creates a bouncer that bounces using a sine function at the specified
     * number of radians per second, with a range of the specified distance.
     */
    public Bouncer (Spatial target, float speed, float range)
    {
        _target = target;
        _speed = speed;
        _range = range;
    }

    @Override // documentation inherited
    protected void controlUpdate (float time)
    {
        Vector3f trans = spatial.getLocalTranslation();
        float z = trans.z - _dz;
        _angle += _speed * time;
        _dz = FastMath.sin(_angle) * _range;
        spatial.setLocalTranslation(trans.x, trans.y, z + _dz);
    }

    @Override // documentation inherited
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // no per-render work
    }

    protected Spatial _target;
    protected float _angle, _dz;
    protected float _speed, _range;
}
