//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Spins a sprite (or any spatial) around the up vector. Used for bonuses.
 *
 * <p>jME3 cutover (Phase 2): was a fork {@code com.jme.scene.Controller}; now a jME3
 * {@link AbstractControl} added to its target via {@code target.addControl(new Spinner(...))}
 * (BangBoardView relies on this). The {@code target} field is retained for source compatibility
 * but the control drives the spatial it is attached to.
 */
public class Spinner extends AbstractControl
{
    /**
     * Creates a spinner that rotates at the specified number of radians
     * per second.
     */
    public Spinner (Spatial target, float speed)
    {
        _target = target;
        _speed = speed;
    }

    /**
     * Sets the spinner's speed.
     */
    public void setSpeed (float speed)
    {
        _speed = speed;
    }

    /**
     * Returns the spinner's speed.
     */
    public float getSpeed (float speed)
    {
        return _speed;
    }

    @Override // documentation inherited
    protected void controlUpdate (float time)
    {
        _angle += _speed * time;
        _rotation.fromAngleAxis(_angle, UP);
        spatial.setLocalRotation(_rotation);
    }

    @Override // documentation inherited
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // no per-render work
    }

    protected Spatial _target;
    protected float _angle, _speed;
    protected Quaternion _rotation = new Quaternion();
}
