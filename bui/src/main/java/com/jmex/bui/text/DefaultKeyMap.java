//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jme3.input.KeyInput;
import com.jmex.bui.event.InputEvent;

/**
 * Defines a default key mapping for our text editing components.
 */
public class DefaultKeyMap extends BKeyMap
{
    public DefaultKeyMap ()
    {
        addMapping(ANY_MODIFIER, KeyInput.KEY_RETURN, EditCommand.ACTION);
        addMapping(ANY_MODIFIER, KeyInput.KEY_NUMPADENTER, EditCommand.ACTION);
        addMapping(ANY_MODIFIER, KeyInput.KEY_BACK, EditCommand.BACKSPACE);
        addMapping(ANY_MODIFIER, KeyInput.KEY_DELETE, EditCommand.DELETE);

        addMapping(ANY_MODIFIER, KeyInput.KEY_LEFT, EditCommand.CURSOR_LEFT);
        addMapping(ANY_MODIFIER, KeyInput.KEY_RIGHT, EditCommand.CURSOR_RIGHT);

        addMapping(ANY_MODIFIER, KeyInput.KEY_HOME, EditCommand.START_OF_LINE);
        addMapping(ANY_MODIFIER, KeyInput.KEY_END, EditCommand.END_OF_LINE);

        addMapping(ANY_MODIFIER, KeyInput.KEY_ESCAPE, EditCommand.RELEASE_FOCUS);

        // some emacs commands because I love them so
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_A, EditCommand.START_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_E, EditCommand.END_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_D, EditCommand.DELETE);
        addMapping(InputEvent.CTRL_DOWN_MASK, KeyInput.KEY_K, EditCommand.CLEAR);
    }
}
