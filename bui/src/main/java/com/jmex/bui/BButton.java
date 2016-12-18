//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BIcon;

/**
 * Displays a simple button that can be depressed and which generates an action event when pressed
 * and released.
 */
public class BButton extends BLabel
    implements BConstants
{
    /** Indicates that this button is in the down state. */
    public static final int DOWN = BComponent.STATE_COUNT + 0;

    /**
     * Creates a button with the specified textual label.
     */
    public BButton (String text)
    {
        this(text, "");
    }

    /**
     * Creates a button with the specified label and action. The action will be dispatched via an
     * {@link ActionEvent} when the button is clicked.
     */
    public BButton (String text, String action)
    {
        this(text, null, action);
    }

    /**
     * Creates a button with the specified label and action. The action will be dispatched via an
     * {@link ActionEvent} to the specified {@link ActionListener} when the button is clicked.
     */
    public BButton (String text, ActionListener listener, String action)
    {
        super(text);
        _action = action;
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} when the button is clicked.
     */
    public BButton (BIcon icon, String action)
    {
        this(icon, null, action);
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public BButton (BIcon icon, ActionListener listener, String action)
    {
        super(icon);
        _action = action;
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Configures the action to be generated when this button is clicked.
     *
     * @return this button for handy call chaining.
     */
    public BComponent setAction (String action)
    {
        _action = action;
        return this;
    }

    /**
     * Returns the action generated when this button is clicked.
     */
    public String getAction ()
    {
        return _action;
    }

    // documentation inherited
    public int getState ()
    {
        int state = super.getState();
        if (state == DISABLED) {
            return state;
        }

        if (_armed && _pressed) {
            return DOWN;
        } else {
            return state; // most likely HOVER
        }
    }

    // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            int ostate = getState();
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_ENTERED:
                _armed = _pressed;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_EXITED:
                _armed = false;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    _pressed = true;
                    _armed = true;
                } else if (mev.getButton() == 1) {
                    // clicking the right mouse button after arming the button disarms it
                    _armed = false;
                }
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (_armed && _pressed) {
                    // create and dispatch an action event
                    fireAction(mev.getWhen(), mev.getModifiers());
                    _armed = false;
                }
                _pressed = false;
                break;

            default:
                return super.dispatchEvent(event);
            }

            // update our background image if necessary
            int state = getState();
            if (state != ostate) {
                stateDidChange();
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "button";
    }

    // documentation inherited
    protected int getStateCount ()
    {
        return STATE_COUNT;
    }

    // documentation inherited
    protected String getStatePseudoClass (int state)
    {
        if (state >= BComponent.STATE_COUNT) {
            return STATE_PCLASSES[state-BComponent.STATE_COUNT];
        } else {
            return super.getStatePseudoClass(state);
        }
    }

    // documentation inherited
    protected void configureStyle (BStyleSheet style)
    {
        super.configureStyle(style);

        // check to see if our stylesheet provides us with an icon
        if (_label.getIcon() == null) {
            BIcon icon = style.getIcon(this, getStatePseudoClass(DEFAULT));
            if (icon != null) {
                _label.setIcon(icon);
            }
        }
    }

    /**
     * Called when the button is "clicked" which may due to the mouse being pressed and released
     * while over the button or due to keyboard manipulation while the button has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new ActionEvent(this, when, modifiers, _action));
    }

    protected boolean _armed, _pressed;
    protected String _action;

    protected static final int STATE_COUNT = BComponent.STATE_COUNT + 1;
    protected static final String[] STATE_PCLASSES = { "down" };
}
