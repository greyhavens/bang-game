//
// $Id$

package com.samskivert.bang.data;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;

/**
 * Delivered when a move results in a modification to the board terrain.
 */
public class ModifyBoardEvent extends DEvent
{
    public int x;
    public int y;
    public int tile;

    public ModifyBoardEvent ()
    {
    }

    public ModifyBoardEvent (int targetOid, int x, int y, Terrain tile)
    {
        super(targetOid);
        this.x = x;
        this.y = y;
        this.tile = tile.code;
    }

    // documentation inherited
    public boolean applyToObject (DObject target)
        throws ObjectAccessException
    {
        ((BangObject)target).board.setTile(x, y, Terrain.fromCode(tile));
        return true;
    }
}
