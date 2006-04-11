//
// $Id$

package com.threerings.bang.client;

import com.jme.input.KeyInput;
import com.jmex.bui.BComponent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.KeyEvent;

import com.samskivert.util.HashIntMap;

import com.threerings.bang.util.BasicContext;

/**
 * Is responsible for dispatching key presses that are context free (should
 * work regardless of what the user is doing, assuming some UI component that
 * has the focus does not consume the key press first).
 *
 * @see KeyInput
 */
public class GlobalKeyManager
{
    public static interface Command
    {
        /** Called when our registered key has been pressed. */
        public void invoke (int keyCode, int modifiers);
    }

    /**
     * Registers the global key manager with the BUI input system and prepares
     * it for operation.
     */
    public void init (BasicContext ctx)
    {
        ctx.getRootNode().pushDefaultEventTarget(new BComponent() {
            public boolean dispatchEvent (BEvent event) {
                KeyEvent kev = (event instanceof KeyEvent) ?
                    (KeyEvent)event : null;
                if (kev != null && kev.getType() == KeyEvent.KEY_PRESSED) {
                    int keyCode = kev.getKeyCode();
                    Command command = _commap.get(keyCode);
                    if (command != null) {
                        command.invoke(keyCode, kev.getModifiers());
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Registers a global key mapping.
     */
    public void registerCommand (int keyCode, Command command)
    {
        _commap.put(keyCode, command);
    }

    /**
     * Clears a global key mapping.
     */
    public void clearCommand (int keyCode)
    {
        _commap.remove(keyCode);
    }

    protected HashIntMap<Command> _commap = new HashIntMap<Command>();
}
