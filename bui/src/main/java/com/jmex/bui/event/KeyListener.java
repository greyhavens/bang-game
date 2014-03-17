//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatches key events to listeners on a component.
 */
public interface KeyListener extends ComponentListener
{
    /**
     * Dispatched when a key is pressed within the bounds of the target component.
     */
    public void keyPressed (KeyEvent event);

    /**
     * Dispatched when a key is typed within the bounds of the target component.
     */
    public void keyTyped (KeyEvent event);

    /**
     * Dispatched when a key is released after having been pressed within the bounds of the target
     * component.
     */
    public void keyReleased (KeyEvent event);
}
