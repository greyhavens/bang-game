//
// $Id$

package com.samskivert.bang.util;

import com.samskivert.util.ArrayIntSet;

/**
 * Provides an efficient way of "noting" a set of tiles.
 */
public class PointSet extends ArrayIntSet
{
    public void add (int tx, int ty)
    {
        add(((tx << 16) | (ty & 0xFFFF)));
    }

    public boolean contains (int tx, int ty)
    {
        return contains(((tx << 16) | (ty & 0xFFFF)));
    }

    public int getX (int index)
    {
        return (short)(get(index) >> 16);
    }

    public int getY (int index)
    {
        return (short)(get(index) & 0xFFFF);
    }
}
