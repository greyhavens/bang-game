//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.awt.Canvas;

import com.badlogic.gdx.Input.Keys;

import com.jmex.bui.event.InputEvent;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

/**
 * Bridges between the AWT and the BUI input event system when we are
 * being used in an AWT canvas.
 */
public class CanvasRootNode extends BRootNode
    implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener,
               java.awt.event.MouseWheelListener, java.awt.event.KeyListener
{
    public CanvasRootNode (Canvas canvas)
    {
        _canvas = canvas;

        // we want to hear about mouse movement, clicking, and keys
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
    }

    // documentation inherited
    public long getTickStamp ()
    {
        return System.currentTimeMillis();
    }

    // documentation inherited
    public void rootInvalidated (BComponent root)
    {
        // TODO: queue up an event to revalidate this component; for now we'll
        // just emulate the old behavior which was to validate every time
        root.validate();
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (java.awt.event.MouseEvent e) {
        // N/A
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (java.awt.event.MouseEvent e) {
        // N/A
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (java.awt.event.MouseEvent e) {
        // N/A
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (java.awt.event.MouseEvent e)
    {
        updateState(e);

        _ccomponent = getTargetComponent();
        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_PRESSED,
            convertButton(e), _mouseX, _mouseY);
        dispatchEvent(_ccomponent, event);
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (java.awt.event.MouseEvent e)
    {
        updateState(e);

        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_RELEASED,
            convertButton(e), _mouseX, _mouseY);
        dispatchEvent(getTargetComponent(), event);
        _ccomponent = null;
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (java.awt.event.MouseEvent e)
    {
        mouseMoved(e);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (java.awt.event.MouseEvent e)
    {
        boolean mouseMoved = updateState(e);

        // if the mouse has moved, generate a moved or dragged event
        if (mouseMoved) {
            BComponent tcomponent = getTargetComponent();
            int type = (tcomponent != null && tcomponent == _ccomponent) ?
                MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
            MouseEvent event = new MouseEvent(
                this, e.getWhen(), _modifiers, type, _mouseX, _mouseY);
            dispatchEvent(tcomponent, event);
        }
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheelMoved (java.awt.event.MouseWheelEvent e)
    {
        updateState(e);

        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_WHEELED,
            convertButton(e), _mouseX, _mouseY, e.getWheelRotation());
        dispatchEvent(getTargetComponent(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (java.awt.event.KeyEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        KeyEvent event = new KeyEvent(
            this, e.getWhen(), _modifiers, KeyEvent.KEY_PRESSED,
            e.getKeyChar(), convertKeyCode(e));
        dispatchEvent(getTargetComponent(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (java.awt.event.KeyEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        KeyEvent event = new KeyEvent(
            this, e.getWhen(), _modifiers, KeyEvent.KEY_RELEASED,
            e.getKeyChar(), convertKeyCode(e));
        dispatchEvent(getTargetComponent(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyTyped (java.awt.event.KeyEvent e)
    {
        // N/A
    }

    protected boolean updateState (java.awt.event.MouseEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        // determine whether the mouse moved
        int mx = e.getX(), my = _canvas.getHeight() - e.getY();
        if (_mouseX != mx || _mouseY != my) {
            mouseDidMove(mx, my);
            return true;
        }

        return false;
    }

    protected BComponent getTargetComponent ()
    {
        // mouse press and mouse motion events do not necessarily go to
        // the component under the mouse. when the mouse is clicked down
        // on a component (any button), it becomes the "clicked"
        // component, the target for all subsequent click and motion
        // events (which become drag events) until all buttons are
        // released
        if (_ccomponent != null) {
            return _ccomponent;
        }
        // if there's no clicked component, use the hover component
        if (_hcomponent != null) {
            return _hcomponent;
        }
        // if there's no hover component, use the default event target
        return null;
    }

    protected int convertModifiers (int modifiers)
    {
        int nmodifiers = 0;
        if ((modifiers & java.awt.event.InputEvent.BUTTON1_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON3_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON2_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.SHIFT_MASK) != 0) {
            nmodifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
            nmodifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_MASK) != 0) {
            nmodifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.META_MASK) != 0) {
            nmodifiers |= InputEvent.META_DOWN_MASK;
        }
        return nmodifiers;
    }

    protected int convertButton (java.awt.event.MouseEvent e)
    {
        // OpenGL and the AWT disagree about mouse button numbering (AWT
        // is left=1 middle=2 right=3, OpenGL is left=0 middle=2 right=1)
        switch (e.getButton()) {
        case java.awt.event.MouseEvent.BUTTON1: return MouseEvent.BUTTON1;
        case java.awt.event.MouseEvent.BUTTON3: return MouseEvent.BUTTON2;
        case java.awt.event.MouseEvent.BUTTON2: return MouseEvent.BUTTON3;
        case 0: return -1; // this is generated when we wheel
        default:
            Log.log.warning("Requested to map unknown button '" +
                            e.getButton() + "'.");
            return e.getButton();
        }
    }

    protected int convertKeyCode (java.awt.event.KeyEvent e)
    {
        switch (e.getKeyCode()) {
        case java.awt.event.KeyEvent.VK_ESCAPE: return Keys.ESCAPE;
        case java.awt.event.KeyEvent.VK_1: return Keys.NUM_1;
        case java.awt.event.KeyEvent.VK_2: return Keys.NUM_2;
        case java.awt.event.KeyEvent.VK_3: return Keys.NUM_3;
        case java.awt.event.KeyEvent.VK_4: return Keys.NUM_4;
        case java.awt.event.KeyEvent.VK_5: return Keys.NUM_5;
        case java.awt.event.KeyEvent.VK_6: return Keys.NUM_6;
        case java.awt.event.KeyEvent.VK_7: return Keys.NUM_7;
        case java.awt.event.KeyEvent.VK_8: return Keys.NUM_8;
        case java.awt.event.KeyEvent.VK_9: return Keys.NUM_9;
        case java.awt.event.KeyEvent.VK_0: return Keys.NUM_0;
        case java.awt.event.KeyEvent.VK_MINUS: return Keys.MINUS;
        case java.awt.event.KeyEvent.VK_EQUALS: return Keys.EQUALS;
        case java.awt.event.KeyEvent.VK_BACK_SPACE: return Keys.BACK;
        case java.awt.event.KeyEvent.VK_TAB: return Keys.TAB;
        case java.awt.event.KeyEvent.VK_Q: return Keys.Q;
        case java.awt.event.KeyEvent.VK_W: return Keys.W;
        case java.awt.event.KeyEvent.VK_E: return Keys.E;
        case java.awt.event.KeyEvent.VK_R: return Keys.R;
        case java.awt.event.KeyEvent.VK_T: return Keys.T;
        case java.awt.event.KeyEvent.VK_Y: return Keys.Y;
        case java.awt.event.KeyEvent.VK_U: return Keys.U;
        case java.awt.event.KeyEvent.VK_I: return Keys.I;
        case java.awt.event.KeyEvent.VK_O: return Keys.O;
        case java.awt.event.KeyEvent.VK_P: return Keys.P;
        case java.awt.event.KeyEvent.VK_OPEN_BRACKET: return Keys.LEFT_BRACKET;
        case java.awt.event.KeyEvent.VK_CLOSE_BRACKET: return Keys.RIGHT_BRACKET;
        case java.awt.event.KeyEvent.VK_ENTER: return Keys.ENTER;
        case java.awt.event.KeyEvent.VK_CONTROL: return e.getKeyLocation() ==
                java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                Keys.CONTROL_LEFT : Keys.CONTROL_RIGHT;
        case java.awt.event.KeyEvent.VK_A: return Keys.A;
        case java.awt.event.KeyEvent.VK_S: return Keys.S;
        case java.awt.event.KeyEvent.VK_D: return Keys.D;
        case java.awt.event.KeyEvent.VK_F: return Keys.F;
        case java.awt.event.KeyEvent.VK_G: return Keys.G;
        case java.awt.event.KeyEvent.VK_H: return Keys.H;
        case java.awt.event.KeyEvent.VK_J: return Keys.J;
        case java.awt.event.KeyEvent.VK_K: return Keys.K;
        case java.awt.event.KeyEvent.VK_L: return Keys.L;
        case java.awt.event.KeyEvent.VK_SEMICOLON:
            return Keys.SEMICOLON;
        case java.awt.event.KeyEvent.VK_QUOTE: return Keys.APOSTROPHE;
        case java.awt.event.KeyEvent.VK_BACK_QUOTE: return Keys.GRAVE;
        case java.awt.event.KeyEvent.VK_SHIFT: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                Keys.SHIFT_LEFT : Keys.SHIFT_RIGHT;
        case java.awt.event.KeyEvent.VK_BACK_SLASH:
            return Keys.BACKSLASH;
        case java.awt.event.KeyEvent.VK_Z: return Keys.Z;
        case java.awt.event.KeyEvent.VK_X: return Keys.X;
        case java.awt.event.KeyEvent.VK_C: return Keys.C;
        case java.awt.event.KeyEvent.VK_V: return Keys.V;
        case java.awt.event.KeyEvent.VK_B: return Keys.B;
        case java.awt.event.KeyEvent.VK_N: return Keys.N;
        case java.awt.event.KeyEvent.VK_M: return Keys.M;
        case java.awt.event.KeyEvent.VK_COMMA: return Keys.COMMA;
        case java.awt.event.KeyEvent.VK_PERIOD: return Keys.PERIOD;
        case java.awt.event.KeyEvent.VK_SLASH: return Keys.SLASH;
        case java.awt.event.KeyEvent.VK_MULTIPLY: return Keys.STAR;
//        case java.awt.event.KeyEvent.VK_0: return Keys.LMENU;
        case java.awt.event.KeyEvent.VK_SPACE: return Keys.SPACE;
        // case java.awt.event.KeyEvent.VK_CAPS_LOCK: return Keys.CAPITAL;
        case java.awt.event.KeyEvent.VK_F1: return Keys.F1;
        case java.awt.event.KeyEvent.VK_F2: return Keys.F2;
        case java.awt.event.KeyEvent.VK_F3: return Keys.F3;
        case java.awt.event.KeyEvent.VK_F4: return Keys.F4;
        case java.awt.event.KeyEvent.VK_F5: return Keys.F5;
        case java.awt.event.KeyEvent.VK_F6: return Keys.F6;
        case java.awt.event.KeyEvent.VK_F7: return Keys.F7;
        case java.awt.event.KeyEvent.VK_F8: return Keys.F8;
        case java.awt.event.KeyEvent.VK_F9: return Keys.F9;
        case java.awt.event.KeyEvent.VK_F10: return Keys.F10;
        // case java.awt.event.KeyEvent.VK_NUM_LOCK: return Keys.NUMLOCK;
        // case java.awt.event.KeyEvent.VK_SCROLL_LOCK: return Keys.SCROLL;
        case java.awt.event.KeyEvent.VK_NUMPAD7: return Keys.NUMPAD_7;
        case java.awt.event.KeyEvent.VK_NUMPAD8: return Keys.NUMPAD_8;
        case java.awt.event.KeyEvent.VK_NUMPAD9: return Keys.NUMPAD_9;
        case java.awt.event.KeyEvent.VK_SUBTRACT: return Keys.MINUS;
        case java.awt.event.KeyEvent.VK_NUMPAD4: return Keys.NUMPAD_4;
        case java.awt.event.KeyEvent.VK_NUMPAD5: return Keys.NUMPAD_5;
        case java.awt.event.KeyEvent.VK_NUMPAD6: return Keys.NUMPAD_6;
        case java.awt.event.KeyEvent.VK_ADD: return Keys.PLUS;
        case java.awt.event.KeyEvent.VK_NUMPAD1: return Keys.NUMPAD_1;
        case java.awt.event.KeyEvent.VK_NUMPAD2: return Keys.NUMPAD_2;
        case java.awt.event.KeyEvent.VK_NUMPAD3: return Keys.NUMPAD_3;
        case java.awt.event.KeyEvent.VK_NUMPAD0: return Keys.NUMPAD_0;
        case java.awt.event.KeyEvent.VK_DECIMAL: return Keys.PERIOD;
        case java.awt.event.KeyEvent.VK_F11: return Keys.F11;
        case java.awt.event.KeyEvent.VK_F12: return Keys.F12;
        // case java.awt.event.KeyEvent.VK_F13: return Keys.F13;
        // case java.awt.event.KeyEvent.VK_F14: return Keys.F14;
        // case java.awt.event.KeyEvent.VK_F15: return Keys.F15;
        // case java.awt.event.KeyEvent.VK_KANA: return Keys.KANA;
        // case java.awt.event.KeyEvent.VK_CONVERT: return Keys.CONVERT;
        // case java.awt.event.KeyEvent.VK_NONCONVERT:
        //     return Keys.NOCONVERT;
//        case java.awt.event.KeyEvent.VK_0: return Keys.YEN;
        // case java.awt.event.KeyEvent.VK_CIRCUMFLEX:
        //     return Keys.CIRCUMFLEX;
        case java.awt.event.KeyEvent.VK_AT: return Keys.AT;
        case java.awt.event.KeyEvent.VK_COLON: return Keys.COLON;
        // case java.awt.event.KeyEvent.VK_UNDERSCORE:
        //     return Keys.UNDERLINE;
        // case java.awt.event.KeyEvent.VK_KANJI: return Keys.KANJI;
        // case java.awt.event.KeyEvent.VK_STOP: return Keys.STOP;
//        case java.awt.event.KeyEvent.VK_0: return Keys.AX;
        // case java.awt.event.KeyEvent.VK_UNDEFINED:
        //     return Keys.UNLABELED;
        case java.awt.event.KeyEvent.VK_DIVIDE: return Keys.SLASH;
        // case java.awt.event.KeyEvent.VK_PRINTSCREEN: return Keys.SYSRQ;
//        case java.awt.event.KeyEvent.VK_0: return Keys.RMENU;
        case java.awt.event.KeyEvent.VK_PAUSE: return Keys.MEDIA_PLAY_PAUSE;
        case java.awt.event.KeyEvent.VK_HOME: return Keys.HOME;
        case java.awt.event.KeyEvent.VK_UP: return Keys.UP;
//        case java.awt.event.KeyEvent.VK_0: return Keys.PRIOR;
        case java.awt.event.KeyEvent.VK_PAGE_UP: return Keys.PAGE_UP;
        case java.awt.event.KeyEvent.VK_LEFT: return Keys.LEFT;
        case java.awt.event.KeyEvent.VK_RIGHT: return Keys.RIGHT;
        case java.awt.event.KeyEvent.VK_END: return Keys.END;
        case java.awt.event.KeyEvent.VK_DOWN: return Keys.DOWN;
//        case java.awt.event.KeyEvent.VK_0: return Keys.NEXT;
        case java.awt.event.KeyEvent.VK_PAGE_DOWN: return Keys.PAGE_DOWN;
        case java.awt.event.KeyEvent.VK_INSERT: return Keys.INSERT;
        case java.awt.event.KeyEvent.VK_DELETE: return Keys.FORWARD_DEL;
//        case java.awt.event.KeyEvent.VK_0: return Keys.LWIN;
//        case java.awt.event.KeyEvent.VK_0: return Keys.RWIN;
//        case java.awt.event.KeyEvent.VK_0: return Keys.APPS;
//        case java.awt.event.KeyEvent.VK_0: return Keys.POWER;
//        case java.awt.event.KeyEvent.VK_0: return Keys.SLEEP;
        default: return Keys.UNKNOWN;
        }
    }

    protected Canvas _canvas;
}
