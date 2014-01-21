//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jmex.bui.event.InputEvent;
import com.jme.input.KeyInput;

/**
 * Defines a default key mapping for our text editing components.
 */
public class DefaultKeyMap extends BKeyMap
{
    public DefaultKeyMap ()
    {
        addMapping(ANY_MODIFIER, KeyInput.KEY_RETURN, EditCommands.ACTION);
        addMapping(ANY_MODIFIER, KeyInput.KEY_NUMPADENTER, EditCommands.ACTION);
        addMapping(ANY_MODIFIER, KeyInput.KEY_BACK, EditCommands.BACKSPACE);
        addMapping(ANY_MODIFIER, KeyInput.KEY_DELETE, EditCommands.DELETE);

        addMapping(ANY_MODIFIER, KeyInput.KEY_LEFT, EditCommands.CURSOR_LEFT);
        addMapping(ANY_MODIFIER, KeyInput.KEY_RIGHT, EditCommands.CURSOR_RIGHT);

        addMapping(ANY_MODIFIER, KeyInput.KEY_HOME, EditCommands.START_OF_LINE);
        addMapping(ANY_MODIFIER, KeyInput.KEY_END, EditCommands.END_OF_LINE);

        addMapping(
                ANY_MODIFIER, KeyInput.KEY_ESCAPE, EditCommands.RELEASE_FOCUS);

        // some emacs commands because I love them so
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_A,
                   EditCommands.START_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_E,
                   EditCommands.END_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_D,
                   EditCommands.DELETE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_K,
                   EditCommands.CLEAR);
    }
}
