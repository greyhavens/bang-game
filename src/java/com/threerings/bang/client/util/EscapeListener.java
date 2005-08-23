//
// $Id$

package com.threerings.bang.client.util;

import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.EventListener;
import com.jmex.bui.event.KeyEvent;
import com.jme.input.KeyInput;

/**
 * Listens for the escape key to be pressed and does something.
 */
public abstract class EscapeListener extends KeyListener
{
    @Override // documentation inherited
    public void keyPressed (int keyCode)
    {
        if (keyCode == KeyInput.KEY_ESCAPE) {
            escapePressed();
        }
    }

    public abstract void escapePressed ();
}
