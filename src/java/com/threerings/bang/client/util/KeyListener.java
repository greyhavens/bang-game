//
// $Id$

package com.threerings.bang.client.util;

import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.EventListener;
import com.jmex.bui.event.KeyEvent;
import com.jme.input.KeyInput;

/**
 * Reacts to the pressing of keys.
 */
public abstract class KeyListener implements EventListener
{
    public void eventDispatched (BEvent event)
    {
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getType() == KeyEvent.KEY_PRESSED) {
                keyPressed(kev.getKeyCode());
            }
        }
    }

    public abstract void keyPressed (int keyCode);
}
