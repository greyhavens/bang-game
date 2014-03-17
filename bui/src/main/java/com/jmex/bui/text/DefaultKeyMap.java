//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.badlogic.gdx.Input.Keys;
import com.jmex.bui.event.InputEvent;

/**
 * Defines a default key mapping for our text editing components.
 */
public class DefaultKeyMap extends BKeyMap
{
    public DefaultKeyMap ()
    {
        addMapping(ANY_MODIFIER, Keys.ENTER, EditCommands.ACTION);
        addMapping(ANY_MODIFIER, Keys.BACK, EditCommands.BACKSPACE);
        addMapping(ANY_MODIFIER, Keys.DEL, EditCommands.DELETE);

        addMapping(ANY_MODIFIER, Keys.LEFT, EditCommands.CURSOR_LEFT);
        addMapping(ANY_MODIFIER, Keys.RIGHT, EditCommands.CURSOR_RIGHT);

        addMapping(ANY_MODIFIER, Keys.HOME, EditCommands.START_OF_LINE);
        addMapping(ANY_MODIFIER, Keys.END, EditCommands.END_OF_LINE);

        addMapping(ANY_MODIFIER, Keys.ESCAPE, EditCommands.RELEASE_FOCUS);

        // some emacs commands because I love them so
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.A, EditCommands.START_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.E, EditCommands.END_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.D, EditCommands.DELETE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.K, EditCommands.CLEAR);
    }
}
