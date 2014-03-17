//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

/**
 * Defines the various commands handled by our text editing components.
 */
public enum EditCommand
{
    ACTION,
    BACKSPACE,
    DELETE,
    CURSOR_LEFT, CURSOR_RIGHT,
    START_OF_LINE, END_OF_LINE,
    RELEASE_FOCUS,
    CLEAR,

    NONE;
}
