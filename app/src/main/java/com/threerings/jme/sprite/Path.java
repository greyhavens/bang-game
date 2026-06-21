//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme.sprite;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Defines a framework for moving sprites around and notifying interested parties when the sprite
 * has completed its path or if the path has been cancelled.
 *
 * <p>jME3 cutover (Phase 1): the fork's <code>com.jme.scene.Controller</code> base (a Savable
 * per-frame-updated object with active/speed/repeat-type flags) has no jME3 equivalent, so it is
 * re-implemented here as a {@link AbstractControl}. jME3 has no speed/repeat-type on a Control, so
 * those flags are provided by this small base: {@link #controlUpdate} scales the frame delta by
 * {@link #setSpeed} and dispatches to the abstract {@link #update(float)} that subclasses (the
 * sprite/camera path types) already override.
 */
public abstract class Path extends AbstractControl
    implements AnimationController
{
    /**
     * Creates and initializes this path with the sprite it will be manipulating.
     */
    protected Path (Sprite sprite)
    {
        _sprite = sprite;
    }

    /**
     * Advances this path by the (speed-scaled) elapsed time. Subclasses implement the actual
     * sprite motion here, exactly as they did under the fork {@code Controller.update(float)}.
     */
    public abstract void update (float time);

    /**
     * Sets the speed multiplier applied to the elapsed time before {@link #update} is called.
     */
    public void setSpeed (float speed)
    {
        _speed = speed;
    }

    /**
     * Returns the current speed multiplier.
     */
    public float getSpeed ()
    {
        return _speed;
    }

    /**
     * Sets whether this path is active (delegates to the control's enabled flag).
     */
    public void setActive (boolean active)
    {
        setEnabled(active);
    }

    /**
     * Returns whether this path is active.
     */
    public boolean isActive ()
    {
        return isEnabled();
    }

    /**
     * Sets the repeat type ({@link com.threerings.jme.util.JmeUtil#RT_CLAMP RT_CLAMP} /
     * {@code RT_WRAP} / {@code RT_CYCLE}). Most paths are one-shot and ignore this.
     */
    public void setRepeatType (int repeatType)
    {
        _repeatType = repeatType;
    }

    /**
     * Returns the configured repeat type.
     */
    public int getRepeatType ()
    {
        return _repeatType;
    }

    /**
     * Called when this path is removed from its sprite (either due to completion or
     * cancellation).
     */
    public void wasRemoved ()
    {
    }

    @Override
    protected void controlUpdate (float tpf)
    {
        update(tpf * _speed);
    }

    @Override
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // no per-render work
    }

    protected Sprite _sprite;

    /** Speed multiplier on the elapsed-time delta. */
    protected float _speed = 1f;

    /** Repeat type; see {@link com.threerings.jme.util.JmeUtil}. */
    protected int _repeatType = com.threerings.jme.util.JmeUtil.RT_CLAMP;
}
