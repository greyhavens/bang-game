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

package com.threerings.jme.camera;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.FastMath;

/**
 * Sets up camera controls for moving around from a top-down perspective, suitable for strategy
 * games and their ilk. The "ground" is assumed to be the XY plane.
 *
 * <p>jME3 cutover (Phase 1): the fork's polled <code>InputHandler</code>/<code>InputAction</code>/
 * <code>KeyBindingManager</code> action-list model has no jME3 equivalent (§2.10 of the migration
 * map: REBUILD onto <code>InputManager</code> mappings + <code>AnalogListener</code>). This is
 * now a jME3 {@link AnalogListener} carrying the pan/zoom/orbit/tilt camera logic; the named
 * mappings are the analog actions it responds to. Binding the mappings to actual keys
 * ({@link #registerWith}) is wired in Phase 3, when the host flips to a jME3 LWJGL3 context and
 * the input source switches from libGDX to jME3's {@code InputManager}.
 */
public class GodViewHandler
    implements AnalogListener
{
    // analog action names (formerly fork KeyBindingManager bindings)
    public static final String FORWARD = "forward";
    public static final String BACKWARD = "backward";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String ZOOM_IN = "zoomIn";
    public static final String ZOOM_OUT = "zoomOut";
    public static final String TURN_RIGHT = "turnRight";
    public static final String TURN_LEFT = "turnLeft";
    public static final String TILT_FORWARD = "tiltForward";
    public static final String TILT_BACK = "tiltBack";

    /** Pan/zoom speed multiplier (formerly the fork InputAction speed). */
    public static final float SPEED = 0.5f;

    /**
     * Creates the handler.
     *
     * @param camhand The camera to move with this handler.
     */
    public GodViewHandler (CameraHandler camhand)
    {
        _camhand = camhand;
    }

    /**
     * Registers this handler's key mappings and listeners with the supplied jME3 input manager.
     * Phase 3 calls this once the jME3 host owns the input manager.
     */
    public void registerWith (InputManager inputManager)
    {
        inputManager.addMapping(FORWARD, new KeyTrigger(KeyInput.KEY_W),
            new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(BACKWARD, new KeyTrigger(KeyInput.KEY_S),
            new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping(LEFT, new KeyTrigger(KeyInput.KEY_A),
            new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(RIGHT, new KeyTrigger(KeyInput.KEY_D),
            new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(ZOOM_IN, new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(ZOOM_OUT, new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping(TURN_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(TURN_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(TILT_FORWARD, new KeyTrigger(KeyInput.KEY_HOME));
        inputManager.addMapping(TILT_BACK, new KeyTrigger(KeyInput.KEY_END));
        inputManager.addListener(this, FORWARD, BACKWARD, LEFT, RIGHT,
            ZOOM_IN, ZOOM_OUT, TURN_RIGHT, TURN_LEFT, TILT_FORWARD, TILT_BACK);
    }

    /**
     * Returns whether this handler currently processes input. The fork
     * {@code InputHandler} carried this flag; it is preserved here so callers can
     * gate camera input (the actual jME3 input wiring is Phase 3, {@link #registerWith}).
     */
    public boolean isEnabled ()
    {
        return _enabled;
    }

    /**
     * Enables or disables input processing for this handler.
     */
    public void setEnabled (boolean enabled)
    {
        _enabled = enabled;
    }

    // from interface AnalogListener
    public void onAnalog (String name, float value, float tpf)
    {
        if (!_enabled) {
            return;
        }
        // value already incorporates tpf for analog key triggers
        switch (name) {
        case FORWARD:
            _camhand.panCamera(0, SPEED * value);
            break;
        case BACKWARD:
            _camhand.panCamera(0, -SPEED * value);
            break;
        case LEFT:
            _camhand.panCamera(-SPEED * value, 0);
            break;
        case RIGHT:
            _camhand.panCamera(SPEED * value, 0);
            break;
        case ZOOM_IN:
            _camhand.zoomCamera(-SPEED * value);
            break;
        case ZOOM_OUT:
            _camhand.zoomCamera(SPEED * value);
            break;
        case TURN_RIGHT:
            _camhand.orbitCamera(-FastMath.PI / 2 * value);
            break;
        case TURN_LEFT:
            _camhand.orbitCamera(FastMath.PI / 2 * value);
            break;
        case TILT_FORWARD:
            _camhand.tiltCamera(-FastMath.PI / 2 * value);
            break;
        case TILT_BACK:
            _camhand.tiltCamera(FastMath.PI / 2 * value);
            break;
        }
    }

    protected CameraHandler _camhand;

    /** Whether this handler processes input (Phase-3 input wiring honours this flag). */
    protected boolean _enabled = true;
}
