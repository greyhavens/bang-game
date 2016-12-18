//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * An interface used to inform listeners when a model has changed.
 */
public interface ChangeListener
{
    /**
     * Indicates that the underlying model has changed.
     */
    public void stateChanged (ChangeEvent event);
}
