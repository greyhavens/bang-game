//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.util;

/**
 * Represents the location of a component.
 */
public class Point
{
    /** The x position of the entity in question. */
    public int x;

    /** The y position of the entity in question. */
    public int y;

    public Point (int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Point (Point other)
    {
        x = other.x;
        y = other.y;
    }

    public Point ()
    {
    }

    public String toString ()
    {
        return (x >= 0 ? "+" : "") + x + (y >= 0 ? "+" : "") + y;
    }
}
