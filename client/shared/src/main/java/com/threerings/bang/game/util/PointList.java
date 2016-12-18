//
// $Id$

package com.threerings.bang.game.util;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Contains a list of points and efficiently serializes them over the network.
 */
public class PointList extends ArrayList<Point>
    implements Streamable
{
    /** Writes this list to the supplied stream. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        int ecount = size();
        out.writeInt(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            Point p = get(ii);
            out.writeInt(p.x);
            out.writeInt(p.y);
        }
    }

    /** Reads our contents from the supplied stream. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        int ecount = in.readInt();
        ensureCapacity(ecount);
        for (int ii = 0; ii < ecount; ii++) {
            Point p = new Point();
            p.x = in.readInt();
            p.y = in.readInt();
            add(p);
        }
    }
}
