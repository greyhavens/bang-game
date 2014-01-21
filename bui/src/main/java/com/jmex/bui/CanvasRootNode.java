//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.awt.Canvas;

import com.jme.input.KeyInput;

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
        case java.awt.event.KeyEvent.VK_ESCAPE: return KeyInput.KEY_ESCAPE;
        case java.awt.event.KeyEvent.VK_1: return KeyInput.KEY_1;
        case java.awt.event.KeyEvent.VK_2: return KeyInput.KEY_2;
        case java.awt.event.KeyEvent.VK_3: return KeyInput.KEY_3;
        case java.awt.event.KeyEvent.VK_4: return KeyInput.KEY_4;
        case java.awt.event.KeyEvent.VK_5: return KeyInput.KEY_5;
        case java.awt.event.KeyEvent.VK_6: return KeyInput.KEY_6;
        case java.awt.event.KeyEvent.VK_7: return KeyInput.KEY_7;
        case java.awt.event.KeyEvent.VK_8: return KeyInput.KEY_8;
        case java.awt.event.KeyEvent.VK_9: return KeyInput.KEY_9;
        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_0;
        case java.awt.event.KeyEvent.VK_MINUS: return KeyInput.KEY_MINUS;
        case java.awt.event.KeyEvent.VK_EQUALS: return e.getKeyLocation() == 
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                KeyInput.KEY_NUMPADEQUALS : KeyInput.KEY_EQUALS;
        case java.awt.event.KeyEvent.VK_BACK_SPACE: return KeyInput.KEY_BACK;
        case java.awt.event.KeyEvent.VK_TAB: return KeyInput.KEY_TAB;
        case java.awt.event.KeyEvent.VK_Q: return KeyInput.KEY_Q;
        case java.awt.event.KeyEvent.VK_W: return KeyInput.KEY_W;
        case java.awt.event.KeyEvent.VK_E: return KeyInput.KEY_E;
        case java.awt.event.KeyEvent.VK_R: return KeyInput.KEY_R;
        case java.awt.event.KeyEvent.VK_T: return KeyInput.KEY_T;
        case java.awt.event.KeyEvent.VK_Y: return KeyInput.KEY_Y;
        case java.awt.event.KeyEvent.VK_U: return KeyInput.KEY_U;
        case java.awt.event.KeyEvent.VK_I: return KeyInput.KEY_I;
        case java.awt.event.KeyEvent.VK_O: return KeyInput.KEY_O;
        case java.awt.event.KeyEvent.VK_P: return KeyInput.KEY_P;
        case java.awt.event.KeyEvent.VK_OPEN_BRACKET:
            return KeyInput.KEY_LBRACKET;
        case java.awt.event.KeyEvent.VK_CLOSE_BRACKET:
            return KeyInput.KEY_RBRACKET;
        case java.awt.event.KeyEvent.VK_ENTER: return e.getKeyLocation() == 
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                KeyInput.KEY_NUMPADENTER : KeyInput.KEY_RETURN;
        case java.awt.event.KeyEvent.VK_CONTROL: return e.getKeyLocation() == 
            java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                KeyInput.KEY_LCONTROL : KeyInput.KEY_RCONTROL;
        case java.awt.event.KeyEvent.VK_A: return KeyInput.KEY_A;
        case java.awt.event.KeyEvent.VK_S: return KeyInput.KEY_S;
        case java.awt.event.KeyEvent.VK_D: return KeyInput.KEY_D;
        case java.awt.event.KeyEvent.VK_F: return KeyInput.KEY_F;
        case java.awt.event.KeyEvent.VK_G: return KeyInput.KEY_G;
        case java.awt.event.KeyEvent.VK_H: return KeyInput.KEY_H;
        case java.awt.event.KeyEvent.VK_J: return KeyInput.KEY_J;
        case java.awt.event.KeyEvent.VK_K: return KeyInput.KEY_K;
        case java.awt.event.KeyEvent.VK_L: return KeyInput.KEY_L;
        case java.awt.event.KeyEvent.VK_SEMICOLON:
            return KeyInput.KEY_SEMICOLON;
        case java.awt.event.KeyEvent.VK_QUOTE: return KeyInput.KEY_APOSTROPHE;
        case java.awt.event.KeyEvent.VK_BACK_QUOTE: return KeyInput.KEY_GRAVE;
        case java.awt.event.KeyEvent.VK_SHIFT: return e.getKeyLocation() == 
            java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                KeyInput.KEY_LSHIFT : KeyInput.KEY_RSHIFT;
        case java.awt.event.KeyEvent.VK_BACK_SLASH:
            return KeyInput.KEY_BACKSLASH;
        case java.awt.event.KeyEvent.VK_Z: return KeyInput.KEY_Z;
        case java.awt.event.KeyEvent.VK_X: return KeyInput.KEY_X;
        case java.awt.event.KeyEvent.VK_C: return KeyInput.KEY_C;
        case java.awt.event.KeyEvent.VK_V: return KeyInput.KEY_V;
        case java.awt.event.KeyEvent.VK_B: return KeyInput.KEY_B;
        case java.awt.event.KeyEvent.VK_N: return KeyInput.KEY_N;
        case java.awt.event.KeyEvent.VK_M: return KeyInput.KEY_M;
        case java.awt.event.KeyEvent.VK_COMMA: return e.getKeyLocation() == 
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                KeyInput.KEY_NUMPADCOMMA : KeyInput.KEY_COMMA;
        case java.awt.event.KeyEvent.VK_PERIOD: return KeyInput.KEY_PERIOD;
        case java.awt.event.KeyEvent.VK_SLASH: return KeyInput.KEY_SLASH;
        case java.awt.event.KeyEvent.VK_MULTIPLY: return KeyInput.KEY_MULTIPLY;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_LMENU;
        case java.awt.event.KeyEvent.VK_SPACE: return KeyInput.KEY_SPACE;
        case java.awt.event.KeyEvent.VK_CAPS_LOCK: return KeyInput.KEY_CAPITAL;
        case java.awt.event.KeyEvent.VK_F1: return KeyInput.KEY_F1;
        case java.awt.event.KeyEvent.VK_F2: return KeyInput.KEY_F2;
        case java.awt.event.KeyEvent.VK_F3: return KeyInput.KEY_F3;
        case java.awt.event.KeyEvent.VK_F4: return KeyInput.KEY_F4;
        case java.awt.event.KeyEvent.VK_F5: return KeyInput.KEY_F5;
        case java.awt.event.KeyEvent.VK_F6: return KeyInput.KEY_F6;
        case java.awt.event.KeyEvent.VK_F7: return KeyInput.KEY_F7;
        case java.awt.event.KeyEvent.VK_F8: return KeyInput.KEY_F8;
        case java.awt.event.KeyEvent.VK_F9: return KeyInput.KEY_F9;
        case java.awt.event.KeyEvent.VK_F10: return KeyInput.KEY_F10;
        case java.awt.event.KeyEvent.VK_NUM_LOCK: return KeyInput.KEY_NUMLOCK;
        case java.awt.event.KeyEvent.VK_SCROLL_LOCK: return KeyInput.KEY_SCROLL;
        case java.awt.event.KeyEvent.VK_NUMPAD7: return KeyInput.KEY_NUMPAD7;
        case java.awt.event.KeyEvent.VK_NUMPAD8: return KeyInput.KEY_NUMPAD8;
        case java.awt.event.KeyEvent.VK_NUMPAD9: return KeyInput.KEY_NUMPAD9;
        case java.awt.event.KeyEvent.VK_SUBTRACT: return KeyInput.KEY_SUBTRACT;
        case java.awt.event.KeyEvent.VK_NUMPAD4: return KeyInput.KEY_NUMPAD4;
        case java.awt.event.KeyEvent.VK_NUMPAD5: return KeyInput.KEY_NUMPAD5;
        case java.awt.event.KeyEvent.VK_NUMPAD6: return KeyInput.KEY_NUMPAD6;
        case java.awt.event.KeyEvent.VK_ADD: return KeyInput.KEY_ADD;
        case java.awt.event.KeyEvent.VK_NUMPAD1: return KeyInput.KEY_NUMPAD1;
        case java.awt.event.KeyEvent.VK_NUMPAD2: return KeyInput.KEY_NUMPAD2;
        case java.awt.event.KeyEvent.VK_NUMPAD3: return KeyInput.KEY_NUMPAD3;
        case java.awt.event.KeyEvent.VK_NUMPAD0: return KeyInput.KEY_NUMPAD0;
        case java.awt.event.KeyEvent.VK_DECIMAL: return KeyInput.KEY_DECIMAL;
        case java.awt.event.KeyEvent.VK_F11: return KeyInput.KEY_F11;
        case java.awt.event.KeyEvent.VK_F12: return KeyInput.KEY_F12;
        case java.awt.event.KeyEvent.VK_F13: return KeyInput.KEY_F13;
        case java.awt.event.KeyEvent.VK_F14: return KeyInput.KEY_F14;
        case java.awt.event.KeyEvent.VK_F15: return KeyInput.KEY_F15;
        case java.awt.event.KeyEvent.VK_KANA: return KeyInput.KEY_KANA;
        case java.awt.event.KeyEvent.VK_CONVERT: return KeyInput.KEY_CONVERT;
        case java.awt.event.KeyEvent.VK_NONCONVERT:
            return KeyInput.KEY_NOCONVERT;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_YEN;
        case java.awt.event.KeyEvent.VK_CIRCUMFLEX:
            return KeyInput.KEY_CIRCUMFLEX;
        case java.awt.event.KeyEvent.VK_AT: return KeyInput.KEY_AT;
        case java.awt.event.KeyEvent.VK_COLON: return KeyInput.KEY_COLON;
        case java.awt.event.KeyEvent.VK_UNDERSCORE:
            return KeyInput.KEY_UNDERLINE;
        case java.awt.event.KeyEvent.VK_KANJI: return KeyInput.KEY_KANJI;
        case java.awt.event.KeyEvent.VK_STOP: return KeyInput.KEY_STOP;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_AX;
        case java.awt.event.KeyEvent.VK_UNDEFINED:
            return KeyInput.KEY_UNLABELED;
        case java.awt.event.KeyEvent.VK_DIVIDE: return KeyInput.KEY_DIVIDE;
        case java.awt.event.KeyEvent.VK_PRINTSCREEN: return KeyInput.KEY_SYSRQ;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_RMENU;
        case java.awt.event.KeyEvent.VK_PAUSE: return KeyInput.KEY_PAUSE;
        case java.awt.event.KeyEvent.VK_HOME: return KeyInput.KEY_HOME;
        case java.awt.event.KeyEvent.VK_UP: return KeyInput.KEY_UP;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_PRIOR;
        case java.awt.event.KeyEvent.VK_PAGE_UP: return KeyInput.KEY_PGUP;
        case java.awt.event.KeyEvent.VK_LEFT: return KeyInput.KEY_LEFT;
        case java.awt.event.KeyEvent.VK_RIGHT: return KeyInput.KEY_RIGHT;
        case java.awt.event.KeyEvent.VK_END: return KeyInput.KEY_END;
        case java.awt.event.KeyEvent.VK_DOWN: return KeyInput.KEY_DOWN;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_NEXT;
        case java.awt.event.KeyEvent.VK_PAGE_DOWN: return KeyInput.KEY_PGDN;
        case java.awt.event.KeyEvent.VK_INSERT: return KeyInput.KEY_INSERT;
        case java.awt.event.KeyEvent.VK_DELETE: return KeyInput.KEY_DELETE;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_LWIN;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_RWIN;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_APPS;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_POWER;
//        case java.awt.event.KeyEvent.VK_0: return KeyInput.KEY_SLEEP;
        default: return KeyInput.KEY_UNLABELED;
        }
    }
    
    protected Canvas _canvas;
}
