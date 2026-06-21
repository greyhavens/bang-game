//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.ArrayList;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.renderer.RenderManager;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

import com.jme3.input.KeyInput;

import com.jmex.bui.event.InputEvent;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

/**
 * The concrete jME3 {@link BRootNode}. Replaces the deleted fork {@code PolledRootNode}
 * (LWJGL2/gdx-polled glue): the host attaches this node's {@link RawInputListener} to jME3's
 * {@link InputManager} (via {@link #registerWith}), and BUI input flows from jME3 input events
 * through {@link BRootNode#dispatchEvent}.
 *
 * <p>BUI's component model compares key codes directly against jME3 {@code KeyInput.KEY_*}
 * constants (woven through {@code BComponent}/{@code KeyEvent}/{@code DefaultKeyMap}); the jME3
 * raw key code flows through unchanged (the former gdx {@code Input.Keys} keycode space and its
 * translation table were removed in the Phase-8 cutover). The mouse y-axis is flipped from jME3's
 * bottom-left origin... actually jME3 mouse
 * events already use a bottom-left origin (y up), which matches BUI, so no flip is needed.
 */
public class Jme3RootNode extends BRootNode
    implements RawInputListener
{
    public Jme3RootNode ()
    {
        // Seed the tick stamp immediately. getTickStamp() must return a valid wall-clock value
        // before the first updateRootState frame: BangClient.IdleTracker.start() samples it as its
        // baseline _lastEventStamp, and if it read the field's default 0 the idle delta would be
        // ~System.currentTimeMillis() (decades) on the very first expire() tick, logging a passive
        // (no-input) client out instantly.
        _tickStamp = System.currentTimeMillis();
    }

    /**
     * Attaches this node's raw input listener to the supplied jME3 input manager and records the
     * display height (jME3 mouse events report y with a bottom-left origin, matching BUI).
     */
    public void registerWith (InputManager inputManager)
    {
        _inputManager = inputManager;
        inputManager.addRawInputListener(this);
    }

    /**
     * Records the host's jME3 {@link RenderManager} so {@link BGeomView} can create per-component
     * viewports for embedded 3D geometry. Called by the host ({@code JmeApp}) at init.
     */
    public void setRenderManager (RenderManager renderManager)
    {
        _renderManager = renderManager;
    }

    // documentation inherited
    public long getTickStamp ()
    {
        return _tickStamp;
    }

    @Override // documentation inherited
    public RenderManager getRenderManager ()
    {
        return _renderManager;
    }

    // documentation inherited
    public void rootInvalidated (BComponent root)
    {
        if (!_invalidRoots.contains(root)) {
            _invalidRoots.add(root);
        }
    }

    @Override // documentation inherited
    public void updateRootState (float time)
    {
        // determine our tick stamp in milliseconds
        _tickStamp = System.currentTimeMillis();

        // effect key repeat
        if (_pressed >= 0 && _nextRepeat < _tickStamp) {
            _nextRepeat += SUBSEQ_REPEAT_DELAY;
            KeyEvent event = new KeyEvent(
                this, _tickStamp, _modifiers, KeyEvent.KEY_TYPED, _presschar, _pressed);
            dispatchEvent(_focus, event);
        }

        // validate all invalid roots
        while (_invalidRoots.size() > 0) {
            BComponent root = _invalidRoots.remove(0);
            if (root.isAdded()) {
                root.validate();
            }
        }

        // run the standard tooltip/geom-view logic
        super.updateRootState(time);
    }

    @Override // documentation inherited
    public float getTooltipTimeout ()
    {
        return ((_modifiers & InputEvent.CTRL_DOWN_MASK) != 0) ? 0 : _tipTime;
    }

    // from interface RawInputListener
    public void beginInput () {}
    public void endInput () {}
    public void onJoyAxisEvent (JoyAxisEvent evt) {}
    public void onJoyButtonEvent (JoyButtonEvent evt) {}
    public void onTouchEvent (TouchEvent evt) {}

    // from interface RawInputListener
    public void onKeyEvent (KeyInputEvent kie)
    {
        int code = kie.getKeyCode();
        char ch = kie.getKeyChar();
        if (kie.isRepeating()) {
            return; // we synthesize our own repeats
        }
        if (kie.isPressed()) {
            int modMask = getModMask(code);
            if (modMask != -1) {
                _modifiers |= modMask;
            }
            dispatchEvent(_focus, new KeyEvent(
                this, _tickStamp, _modifiers, KeyEvent.KEY_PRESSED, (char)0, code));
            // a key that yields a character generates a TYPED event too
            if (ch != 0 && ch != '￿' && !Character.isISOControl(ch)) {
                dispatchEvent(_focus, new KeyEvent(
                    this, _tickStamp, _modifiers, KeyEvent.KEY_TYPED, ch, 0));
                _presschar = ch;
            } else {
                _presschar = (char)0;
            }
            _pressed = code;
            _nextRepeat = _tickStamp + INITIAL_REPEAT_DELAY;
            // jME3 cutover: only swallow the key when a BUI component actually has focus (text
            // entry, chat, etc.). With nothing focused the event must fall through to the
            // InputManager mappings so the camera AnalogListener (GodViewHandler: WASD pan / zoom /
            // orbit) still receives it — consuming unconditionally was why WASD panning was dead.
            // Remember which presses we consumed so the matching release is consumed too (below).
            if (_focus != null) {
                _consumedPresses.add(kie.getKeyCode());
                kie.setConsumed();
            }
        } else {
            int modMask = getModMask(code);
            if (modMask != -1) {
                _modifiers &= ~modMask;
            }
            dispatchEvent(_focus, new KeyEvent(
                this, _tickStamp, _modifiers, KeyEvent.KEY_RELEASED, (char)0, code));
            _pressed = -1;
            // consume the release iff we consumed its press, keeping the InputManager's per-key
            // state balanced even when focus changes mid-hold. Gating the release on the *current*
            // focus instead would let a consumed release the camera mapping never sees leave a pan
            // stuck on (key pressed unfocused -> pan starts; focus grabbed; release eaten).
            if (_consumedPresses.remove(kie.getKeyCode())) {
                kie.setConsumed();
            }
        }
    }

    // from interface RawInputListener
    public void onMouseMotionEvent (MouseMotionEvent mme)
    {
        int x = mme.getX(), y = mme.getY();
        _mouseX = x;
        _mouseY = y;

        // jME3 folds wheel scrolling into the mouse-motion event (MouseMotionEvent.getDeltaWheel);
        // a pure scroll arrives with dx==dy==0 and only deltaWheel set. The fork delivered wheel via
        // a dedicated onWheel() that recomputed the hover component for the wheel's coords and then
        // dispatched MOUSE_WHEELED to it; do the same here so the event lands on whatever component
        // the cursor is over (e.g. BangBoardView's swing-camera listener) even when the wheel turns
        // without a preceding mouse move (otherwise the wheel dispatches against a stale/null hover
        // target and GameInputHandler._swingListener never fires -> camera never rotates).
        int wheelDelta = mme.getDeltaWheel();
        if (wheelDelta != 0) {
            updateHoverComponent(x, y);
            // jME3 reports positive deltaWheel for scroll up; BUI expects positive for up too.
            dispatchMouse(new MouseEvent(
                this, _tickStamp, _modifiers, MouseEvent.MOUSE_WHEELED, -1, x, y,
                wheelDelta > 0 ? 1 : -1));
        }

        mouseDidMove(x, y);
        dispatchMouse(new MouseEvent(
            this, _tickStamp, _modifiers,
            _ccomponent != null ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED, x, y));
        mme.setConsumed();
    }

    // from interface RawInputListener
    public void onMouseButtonEvent (MouseButtonEvent mbe)
    {
        int button = mbe.getButtonIndex();
        boolean pressed = mbe.isPressed();
        int x = mbe.getX(), y = mbe.getY();

        // jME3 buttons: 0=left,1=right,2=middle; BUI: 0=left,1=right,2=middle (same indices)
        updateHoverComponent(x, y);
        if (pressed && (_modifiers & ANY_BUTTON_PRESSED) == 0) {
            setFocus(_ccomponent = _hcomponent);
        }
        if (button >= 0 && button < MOUSE_MODIFIER_MAP.length) {
            if (pressed) {
                _modifiers |= MOUSE_MODIFIER_MAP[button];
            } else {
                _modifiers &= ~MOUSE_MODIFIER_MAP[button];
            }
        }
        dispatchMouse(new MouseEvent(
            this, _tickStamp, _modifiers,
            pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED, button, x, y));
        if ((_modifiers & ANY_BUTTON_PRESSED) == 0) {
            _ccomponent = null;
        }
        mbe.setConsumed();
    }

    protected void dispatchMouse (MouseEvent event)
    {
        dispatchEvent(_ccomponent != null ? _ccomponent : _hcomponent, event);
    }

    protected int getModMask (int keyCode)
    {
        for (int ii = 0; ii < KEY_MODIFIER_MAP.length; ii += 2) {
            if (KEY_MODIFIER_MAP[ii] == keyCode) {
                return KEY_MODIFIER_MAP[ii+1];
            }
        }
        return -1;
    }

    protected InputManager _inputManager;
    protected RenderManager _renderManager;
    protected long _tickStamp;
    protected ArrayList<BComponent> _invalidRoots = new ArrayList<BComponent>();

    /** Used for key repeat. */
    protected int _pressed = -1;
    protected char _presschar;
    protected long _nextRepeat;

    /** Raw jME key codes whose KEY_PRESSED we consumed (a BUI component held focus); the matching
     * KEY_RELEASED is consumed too so the InputManager's analog key state stays balanced across a
     * focus change mid-hold (see {@link #onKeyEvent}). */
    protected final java.util.Set<Integer> _consumedPresses = new java.util.HashSet<>();

    /** Maps (jME3 {@code KeyInput}) key codes to modifier flags. */
    protected static final int[] KEY_MODIFIER_MAP = {
        KeyInput.KEY_LSHIFT, InputEvent.SHIFT_DOWN_MASK,
        KeyInput.KEY_RSHIFT, InputEvent.SHIFT_DOWN_MASK,
        KeyInput.KEY_LCONTROL, InputEvent.CTRL_DOWN_MASK,
        KeyInput.KEY_RCONTROL, InputEvent.CTRL_DOWN_MASK,
        KeyInput.KEY_LMENU, InputEvent.ALT_DOWN_MASK,
        KeyInput.KEY_RMENU, InputEvent.ALT_DOWN_MASK,
    };

    protected static final int[] MOUSE_MODIFIER_MAP = {
        InputEvent.BUTTON1_DOWN_MASK,
        InputEvent.BUTTON2_DOWN_MASK,
        InputEvent.BUTTON3_DOWN_MASK,
    };

    protected static final int ANY_BUTTON_PRESSED =
        InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;

    protected static final long INITIAL_REPEAT_DELAY = 300L;
    protected static final long SUBSEQ_REPEAT_DELAY = 50L;
}
