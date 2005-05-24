//
// $Id$

package com.threerings.bang.client.util;

import com.jme.bui.event.BEvent;
import com.jme.bui.event.EventListener;
import com.jme.bui.event.KeyEvent;
import com.jme.input.KeyInput;

/**
 * Listens for the escape key to be pressed and does something.
 */
public abstract class EscapeListener implements EventListener
{
    public void eventDispatched (BEvent event)
    {
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getKeyCode() == KeyInput.KEY_ESCAPE &&
                kev.getType() == KeyEvent.KEY_PRESSED) {
                escapePressed();
            }
        }
    }

    public abstract void escapePressed ();
}
