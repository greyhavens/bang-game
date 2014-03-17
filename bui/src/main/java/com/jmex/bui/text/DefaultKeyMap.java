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
        addMapping(ANY_MODIFIER, Keys.ENTER, EditCommand.ACTION);
        addMapping(ANY_MODIFIER, Keys.BACKSPACE, EditCommand.BACKSPACE);
        addMapping(ANY_MODIFIER, Keys.FORWARD_DEL, EditCommand.DELETE);

        addMapping(ANY_MODIFIER, Keys.LEFT, EditCommand.CURSOR_LEFT);
        addMapping(ANY_MODIFIER, Keys.RIGHT, EditCommand.CURSOR_RIGHT);

        addMapping(ANY_MODIFIER, Keys.HOME, EditCommand.START_OF_LINE);
        addMapping(ANY_MODIFIER, Keys.END, EditCommand.END_OF_LINE);

        addMapping(ANY_MODIFIER, Keys.ESCAPE, EditCommand.RELEASE_FOCUS);

        // some emacs commands because I love them so
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.A, EditCommand.START_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.E, EditCommand.END_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.D, EditCommand.DELETE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keys.K, EditCommand.CLEAR);
    }
}
