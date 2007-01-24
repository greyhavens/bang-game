//
// $Id$

package com.threerings.bang.server.persist;

import java.io.IOException;

import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.util.BoardFile;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains all information about a board for storage in the database.
 */
public class BoardRecord
{
    /** Indicates that this board should not be included in normal match made games. */
    public static final int PRIVATE_BOARD = 1 << 0;

    /** The unique identifier for this board. */
    public int boardId;

    /** The human readable name of this board. */
    public String name;

    /** The username of the player that created this board, or null if it is a system created
     * board. */
    public String creator;

    /** A comma separated list of scenarios for which this board is usable. Use {@link
     * #setScenarios} and {@link #getScenarios}. */
    public String scenarios;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** Bit flags maintained for this board. */
    public int flags;

    /** The MD5 hash of the board data. */
    public byte[] dataHash;

    /** The serialized data. */
    public byte[] data;

    /**
     * Zero argument constructor used when loading from the database.
     */
    public BoardRecord ()
    {
    }

    /**
     * Creates a board record from the data contained in the supplied board
     * file.
     */
    public BoardRecord (BoardFile file)
        throws IOException
    {
        name = file.name;
        creator = file.creator;
        setScenarios(file.scenarios);
        players = file.players;
        setFlag(PRIVATE_BOARD, file.privateBoard);
        data = new BoardData(file.board, file.pieces).toBytes();
        dataHash = BoardData.getDataHash(data);
    }

    /**
     * Returns an array of all scenarios for which this board is
     * applicable.
     */
    public String[] getScenarios ()
    {
        return (scenarios == null) ? new String[0] :
            StringUtil.split(scenarios, ",");
    }

    /**
     * Returns the index of the earliest town in which this board can be used.
     */
    public int getMinimumTownIndex ()
    {
        int minTownIdx = 0;
        for (String scenId : getScenarios()) {
            ScenarioInfo info = ScenarioInfo.getScenarioInfo(scenId);
            if (info != null) {
                minTownIdx = Math.max(
                    minTownIdx, BangUtil.getTownIndex(info.getTownId()));
            }
        }
        return minTownIdx;
    }

    /**
     * Configures this board record with a set of valid scenarios.
     */
    public void setScenarios (String[] scenids)
    {
        scenarios = StringUtil.join(scenids, ",");
    }

    /**
     * Returns true if the supplied flag is set.
     */
    public boolean isFlagSet (int flag)
    {
        return (flags & flag) != 0;
    }

    /**
     * Activates or deactivates the specified flag.
     */
    public void setFlag (int flag, boolean active)
    {
        if (active) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    /**
     * Unserializes our binary data into a {@link BoardData} record.
     */
    public BoardData getBoardData ()
        throws IOException
    {
        return BoardData.fromBytes(data);
    }

    @Override // from BoardData
    public String toString ()
    {
        return "[id=" + boardId + ", name=" + name + ", players=" + players + ", flags=" + flags +
            ", data=" + super.toString() + ", hash=" + StringUtil.hexlate(dataHash) + "]";
    }
}
