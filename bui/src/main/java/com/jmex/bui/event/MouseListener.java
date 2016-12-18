//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatches mouse events to listeners on a component.
 */
public interface MouseListener extends ComponentListener
{
    /**
     * Dispatched when a button is pressed within the bounds of the target
     * component.
     */
    public void mousePressed (MouseEvent event);

    /**
     * Dispatched when a button is released after having been pressed
     * within the bounds of the target component.
     */
    public void mouseReleased (MouseEvent event);

    /**
     * Dispatched when the mouse enters the bounds of the target
     * component.
     */
    public void mouseEntered (MouseEvent event);

    /**
     * Dispatched when the mouse exits the bounds of the target component.
     */
    public void mouseExited (MouseEvent event);
}
