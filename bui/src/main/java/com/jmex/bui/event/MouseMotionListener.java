//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatches mouse motion events to listeners on a component.
 */
public interface MouseMotionListener extends ComponentListener
{
    /**
     * Dispatched when the mouse is moved within the bounds of the target
     * component.
     */
    public void mouseMoved (MouseEvent event);

    /**
     * Dispatched when the mouse is moved after a button having been
     * pressed within the bounds of the target component.
     */
    public void mouseDragged (MouseEvent event);
}
