//
// $Id$

package com.threerings.bang.game.util;

import java.awt.Rectangle;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

/**
 * Provides an efficient way of "noting" a set of tiles.
 */
public class PointSet extends ArrayIntSet
{
    public void add (int tx, int ty)
    {
        add(((tx << 16) | (ty & 0xFFFF)));
    }

    public void remove (int tx, int ty)
    {
        remove(((tx << 16) | (ty & 0xFFFF)));
    }

    public boolean containsAdjacent (int tx, int ty)
    {
        return contains(tx + 1, ty) || contains(tx - 1, ty) ||
            contains(tx, ty + 1) || contains(tx, ty - 1);
    }
    
    public boolean contains (int tx, int ty)
    {
        return contains(((tx << 16) | (ty & 0xFFFF)));
    }

    public int getX (int index)
    {
//         return (short)(get(index) >> 16);
        return decodeX(get(index));
    }

    public int getY (int index)
    {
//         return (short)(get(index) & 0xFFFF);
        return decodeY(get(index));
    }

    public static int decodeX (int pair)
    {
        return (short)(pair >> 16);
    }

    public static int decodeY (int pair)
    {
        return (short)(pair & 0xFFFF);
    }

    /**
     * Adds all the points that frame the specified point at a certain
     * distance. Any points that fall outside the supplied bounds are not
     * added.
     */
    public void addFrame (int sx, int sy, int dist, Rectangle bounds)
    {
        int miny = sy - dist, maxy = sy + dist;
        int minx = sx - dist, maxx = sx + dist;
        for (int xx = minx; xx <= maxx; xx++) {
            addIf(bounds, xx, miny);
            addIf(bounds, xx, maxy);
        }
        for (int yy = miny+1; yy < maxy; yy++) {
            addIf(bounds, minx, yy);
            addIf(bounds, maxx, yy);
        }
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("(");
        for (int ii = 0, ll = size(); ii < ll; ii++) {
            if (ii > 0) {
                buf.append(", ");
            }
            StringUtil.coordsToString(buf, getX(ii), getY(ii));
        }
        return buf.append(")").toString();
    }

    protected void addIf (Rectangle bounds, int x, int y)
    {
        if (bounds.contains(x, y)) {
            add(x, y);
        }
    }
}
