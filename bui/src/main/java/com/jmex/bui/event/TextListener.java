//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Used to communicate text modification events for text components.
 */
public interface TextListener extends ComponentListener
{
    /**
     * Dispatched when the text value changes in a text component.
     */
    public void textChanged (TextEvent event);
}
