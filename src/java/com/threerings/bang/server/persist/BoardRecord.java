//
// $Id$

package com.threerings.bang.server.persist;

import com.samskivert.util.StringUtil;

/**
 * Contains persistent information associated with a particular game
 * board.
 */
public class BoardRecord
{
    /** The unique identifier for this board. */
    public int boardId;

    /** The human readable name of this board. */
    public String name;

    /** The username of the player that created this board, or null if it
     * is a system created board. */
    public String creator;

    /** A comma separated list of scenarios for which this board is
     * usable. */
    public String scenarios;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** The number of player-games that have been played on this board
     * (ie. if four players play one game on the board, this value is
     * incremented by four). */
    public int plays;

    /** The serialized board data. */
    public byte[] data;

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Helper function for {@link #toString}. */
    public String dataToString ()
    {
        return data.length + " bytes";
    }
}
