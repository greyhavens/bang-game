//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Listens for all input events.
 */
public interface EventListener extends ComponentListener
{
    /**
     * Indicates that an event was dispatched on the target component.
     */
    public void eventDispatched (BEvent event);
}
