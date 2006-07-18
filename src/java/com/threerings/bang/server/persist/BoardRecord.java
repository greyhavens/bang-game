//
// $Id$

package com.threerings.bang.server.persist;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.Properties;

import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Extends {@link BoardData} with additional information to be stored in the
 * database and in files handled by the editor.
 */
public class BoardRecord extends BoardData
{
    /** The unique identifier for this board. */
    public int boardId;

    /** The human readable name of this board. */
    public String name;

    /** The username of the player that created this board, or null if it
     * is a system created board. */
    public String creator;

    /** A comma separated list of scenarios for which this board is
     * usable. Use {@link #setScenarios} and {@link #getScenarios}. */
    public String scenarios;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** The number of player-games that have been played on this board
     * (ie. if four players play one game on the board, this value is
     * incremented by four). */
    public int plays;

    /** The MD5 hash of the board data. */
    public byte[] dataHash;

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
     * Configures this board record with a set of valid scenarios.
     */
    public void setScenarios (String[] scenids)
    {
        scenarios = StringUtil.join(scenids, ",");
        sortScenarios();
    }
     
    /**
     * Saves this board record to the specified target file.
     */
    public void save (File target)
        throws IOException
    {
        Properties props = new Properties();
        props.setProperty("name", name);
        props.setProperty("players", String.valueOf(players));
        props.setProperty("scenarios", scenarios);
        props.setProperty("data", StringUtil.hexlate(data));
        BufferedOutputStream out = new BufferedOutputStream(
            new FileOutputStream(target));
        props.store(out, "");
        out.close();
    }

    /**
     * Loads the specified file containing board record data into this
     * instance.
     */
    public void load (File source)
        throws IOException
    {
        load(new FileInputStream(source));
    }

    /**
     * Loads the supplied input stream containing board record data into
     * this instance.
     */
    public void load (InputStream in)
        throws IOException
    {
        Properties props = new Properties();
        BufferedInputStream bin = new BufferedInputStream(in);
        props.load(bin);
        name = props.getProperty("name");
        String pstr = props.getProperty("players", "0");
        try {
            players = Integer.valueOf(pstr);
        } catch (Exception e) {
            log.warning("Invalid players property [prop=" + pstr + "].");
        }
        scenarios = props.getProperty("scenarios");
        sortScenarios();
        data = StringUtil.unhexlate(props.getProperty("data", ""));
        bin.close();
    }

    /** Helper function for {@link #toString}. */
    public String dataHashToString ()
    {
        return StringUtil.hexlate(dataHash);
    }

    @Override // documentation inherited
    protected void writePiece (ObjectOutputStream oout, Piece piece)
        throws IOException
    {
        super.writePiece(oout, piece, _sortedScenIds);
    }

    @Override // documentation inherited
    protected Piece readPiece (ObjectInputStream oin)
        throws IOException
    {
        return super.readPiece(oin, _sortedScenIds);
    }

    /**
     * Create a sorted array of the scenario ids.
     */
    protected void sortScenarios ()
    {  
        _sortedScenIds = getScenarios(); 
        Arrays.sort(_sortedScenIds);
    }

    /** A sorted array of scenario ids. */
    protected transient String[] _sortedScenIds;
}
