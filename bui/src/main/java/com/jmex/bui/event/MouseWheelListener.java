//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatches mouse wheel events to listeners on a component.
 */
public interface MouseWheelListener extends ComponentListener
{
    /**
     * Dispatched when the mouse wheel is rotated within the bounds of the
     * target component.
     */
    public void mouseWheeled (MouseEvent event);
}
