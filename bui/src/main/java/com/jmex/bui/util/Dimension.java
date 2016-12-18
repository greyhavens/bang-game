//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.util;

/**
 * Represents the size of a component.
 */
public class Dimension
{
    /** The width of the entity in question. */
    public int width;

    /** The height of the entity in question. */
    public int height;

    public Dimension (int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public Dimension (Dimension other)
    {
        width = other.width;
        height = other.height;
    }

    public Dimension ()
    {
    }

    public String toString ()
    {
        return width + "x" + height;
    }
}
