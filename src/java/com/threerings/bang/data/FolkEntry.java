//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains simple a player id, used to track friends and foes.
 */
public class FolkEntry
    implements DSet.Entry
{
    /** The id of the player in question. */
    public Integer playerId;

    public FolkEntry (int playerId)
    {
        this.playerId = playerId;
    }

    public FolkEntry ()
    {
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return playerId;
    }
}
