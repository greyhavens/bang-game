//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.ArrayList;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.Input.Keys;

import com.jme.input.InputHandler;
import com.jme.input.KeyInput;
import com.jme.input.KeyInputListener;
import com.jme.input.MouseInput;
import com.jme.input.MouseInputListener;
import com.jme.util.Timer;

import com.jmex.bui.event.InputEvent;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

/**
 * Processes the polled input information available from the underlying
 * input system into input events and dispatches those to the appropriate
 * parties.
 */
public class PolledRootNode extends BRootNode
{
    public PolledRootNode (Timer timer, InputHandler handler)
    {
        _timer = timer;
        _handler = handler;

        // register our interest in key presses and mouse events
        KeyInput.get().addListener(_keyListener);
        MouseInput.get().addListener(_mouseListener);
    }

    // documentation inherited
    public long getTickStamp ()
    {
        return _tickStamp;
    }

    // documentation inherited
    public void rootInvalidated (BComponent root)
    {
        // add the component to the list of invalid roots
        if (!_invalidRoots.contains(root)) {
            _invalidRoots.add(root);
        }
    }

    // documentation inherited
    public void updateWorldData (float timePerFrame)
    {
        super.updateWorldData(timePerFrame);

        // determine our tick stamp in milliseconds
        _tickStamp = _timer.getTime() * 1000 / _timer.getResolution();

        // poll the keyboard and mouse and notify event listeners
        KeyInput.get().update();
        MouseInput.get().update();

        // if we have no focus component, update the normal input handler
        if (_focus == null && _handler != null) {
            _handler.update(timePerFrame);
        }

        // if our OpenGL window lost focus, clear our modifiers
        boolean lostFocus = !Display.isActive();
        if (_modifiers != 0 && lostFocus) {
            _modifiers = 0;
        }

        // effect key repeat
        if (_pressed >= 0 && _nextRepeat < _tickStamp) {
            if (lostFocus || !KeyInput.get().isKeyDown(_pressed)) {
                // stop repeating if our window lost focus or for whatever
                // reason we missed the key up event
                _pressed = -1;
            } else {
                // otherwise generate and dispatch a key repeat event
                _nextRepeat += SUBSEQ_REPEAT_DELAY;
                KeyEvent event = new KeyEvent(
                    PolledRootNode.this, _tickStamp, _modifiers,
                    KeyEvent.KEY_PRESSED, _presschar, _pressed);
                dispatchEvent(_focus, event);
            }
        }

        // validate all invalid roots
        while (_invalidRoots.size() > 0) {
            BComponent root = _invalidRoots.remove(0);
            // make sure the root is still added to the view hierarchy
            if (root.isAdded()) {
                root.validate();
            }
        }
    }

    // documentation inherited
    public float getTooltipTimeout ()
    {
        return (KeyInput.get().isKeyDown(Keys.CONTROL_LEFT) ||
                KeyInput.get().isKeyDown(Keys.CONTROL_RIGHT)) ? 0 : _tipTime;
    }

    /** This listener is notified when a key is pressed or released. */
    protected KeyInputListener _keyListener = new KeyInputListener() {
        public void onKey (char character, int keyCode, boolean pressed) {
            // first update the state of the modifiers
            int modifierMask = -1;
            for (int ii = 0; ii < KEY_MODIFIER_MAP.length; ii += 2) {
                if (KEY_MODIFIER_MAP[ii] == keyCode) {
                    modifierMask = KEY_MODIFIER_MAP[ii+1];
                    break;
                }
            }
            if (modifierMask != -1) {
                if (pressed) {
                    _modifiers |= modifierMask;
                } else {
                    _modifiers &= ~modifierMask;
                }
            }

            // generate a key event and dispatch it
            KeyEvent event = new KeyEvent(
                PolledRootNode.this, _tickStamp, _modifiers,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                character, keyCode);
            dispatchEvent(_focus, event);

            // update our stored list of pressed keys
            if (pressed) {
                _pressed = keyCode;
                _presschar = character;
                _nextRepeat = _tickStamp + INITIAL_REPEAT_DELAY;
            } else {
                if (_pressed == keyCode) {
                    _pressed = -1;
                }
            }
        }
    };

    /** This listener is notified when the mouse is updated. */
    protected MouseInputListener _mouseListener = new MouseInputListener() {
        public void onButton (int button, boolean pressed, int x, int y) {
            // recalculate the hover component whenever the a button is pressed
            updateHoverComponent(x, y);

            // if we had no mouse button down previous to this, whatever's
            // under the mouse becomes the "clicked" component (which might be
            // null)
            if (pressed && (_modifiers & ANY_BUTTON_PRESSED) == 0) {
                setFocus(_ccomponent = _hcomponent);
            }

            // update the state of the modifiers
            if (pressed) {
                _modifiers |= MOUSE_MODIFIER_MAP[button];
            } else {
                _modifiers &= ~MOUSE_MODIFIER_MAP[button];
            }

            // generate a mouse event and dispatch it
            dispatchEvent(new MouseEvent(
                              PolledRootNode.this, _tickStamp, _modifiers,
                              pressed ? MouseEvent.MOUSE_PRESSED :
                              MouseEvent.MOUSE_RELEASED, button, x, y));

            // finally, if no buttons are up after processing, clear out our
            // "clicked" component
            if ((_modifiers & ANY_BUTTON_PRESSED) == 0) {
                _ccomponent = null;
            }
        }

        public void onMove (int newX, int newY) {
            mouseDidMove(newX, newY);
            dispatchEvent(new MouseEvent(
                              PolledRootNode.this, _tickStamp, _modifiers,
                              _ccomponent != null ? MouseEvent.MOUSE_DRAGGED :
                              MouseEvent.MOUSE_MOVED,
                              newX, newY));
        }

        public void onWheel (int wheelDelta, int x, int y) {
            dispatchEvent(new MouseEvent(
                              PolledRootNode.this, _tickStamp, _modifiers,
                              MouseEvent.MOUSE_WHEELED, -1, x, y, wheelDelta));
            updateHoverComponent(x, y);
        }

        protected void dispatchEvent (MouseEvent event) {
            PolledRootNode.this.dispatchEvent(
                _ccomponent != null ? _ccomponent : _hcomponent, event);
        }
    };

    protected long _tickStamp;
    protected Timer _timer;
    protected InputHandler _handler;
    protected ArrayList<BComponent> _invalidRoots = new ArrayList<BComponent>();

    /** This is used for key repeat. */
    protected int _pressed = -1;

    /** This is used for key repeat. */
    protected char _presschar;

    /** This is used for key repeat. */
    protected long _nextRepeat;

    /** Maps key codes to modifier flags. */
    protected static final int[] KEY_MODIFIER_MAP = {
        Keys.SHIFT_LEFT, InputEvent.SHIFT_DOWN_MASK,
        Keys.SHIFT_RIGHT, InputEvent.SHIFT_DOWN_MASK,
        Keys.CONTROL_LEFT, InputEvent.CTRL_DOWN_MASK,
        Keys.CONTROL_RIGHT, InputEvent.CTRL_DOWN_MASK,
        Keys.ALT_LEFT, InputEvent.ALT_DOWN_MASK,
        Keys.ALT_RIGHT, InputEvent.ALT_DOWN_MASK,
        // TODO: no meta key codes in GDX?
        Keys.SOFT_LEFT, InputEvent.META_DOWN_MASK,
        Keys.SOFT_RIGHT, InputEvent.META_DOWN_MASK,
    };

    /** Maps button indices to modifier flags. */
    protected static final int[] MOUSE_MODIFIER_MAP = {
        InputEvent.BUTTON1_DOWN_MASK,
        InputEvent.BUTTON2_DOWN_MASK,
        InputEvent.BUTTON3_DOWN_MASK,
    };

    /** Used to check whether any button remains pressed. */
    protected static final int ANY_BUTTON_PRESSED =
        InputEvent.BUTTON1_DOWN_MASK |
        InputEvent.BUTTON2_DOWN_MASK |
        InputEvent.BUTTON3_DOWN_MASK;

    protected static final long INITIAL_REPEAT_DELAY = 400L;
    protected static final long SUBSEQ_REPEAT_DELAY = 30L;
}
