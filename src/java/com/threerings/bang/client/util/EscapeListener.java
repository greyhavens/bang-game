//
// $Id$

package com.threerings.bang.client.util;

import com.jme.input.KeyInput;
import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.KeyListener;

/**
 * Listens for the escape key to be pressed and does something.
 */
public abstract class EscapeListener
    implements KeyListener
{
    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent event)
    {
        if (event.getKeyCode() == KeyInput.KEY_ESCAPE) {
            escapePressed();
        }
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent event)
    {
    }

    public abstract void escapePressed ();
}
