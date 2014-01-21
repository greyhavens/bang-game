//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatches action events to interested parties.
 */
public interface ActionListener extends ComponentListener
{
    /**
     * Dispatched when a component has generated an "action".
     */
    public void actionPerformed (ActionEvent event);
}
