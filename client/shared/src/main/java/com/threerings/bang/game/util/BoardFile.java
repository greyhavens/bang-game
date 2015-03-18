//
// $Id$

package com.threerings.bang.game.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.util.BangUtil;

/**
 * Contains all the data stored for a board when stored on the file system.
 */
public class BoardFile
    implements Savable
{
    /** The human readable name of this board. */
    public String name;

    /** The username of the player that created this board, or null if it
     * is a system created board. */
    public String creator;

    /** The scenarios for which this board is usable. */
    public String[] scenarios;

    /** Private boards are not included in the random selection for normal match made games. */
    public boolean privateBoard;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** The board itself (elevation data, etc.). */
    public BangBoard board;

    /** The props and markers on the board. */
    public List<Piece> pieces;

    /** A hash of our board and pieces data. Not serialized. */
    public byte[] dataHash;

    /**
     * Creates a board data instance from this file.
     */
    public BoardData toData () {
        return new BoardData(board, pieces);
    }

    /**
     * Loads a board file from the supplied binary data.
     */
    public static BoardFile loadFrom (byte[] data)
        throws IOException
    {
        return (BoardFile)BinaryImporter.getInstance().load(data);
    }

    /**
     * Loads a board file from the supplied file target.
     */
    public static BoardFile loadFrom (File target)
        throws IOException
    {
        return (BoardFile)BinaryImporter.getInstance().load(target);
    }

    /**
     * Loads a board file from the supplied input stream target.
     */
    public static BoardFile loadFrom (InputStream target)
        throws IOException
    {
        return (BoardFile)BinaryImporter.getInstance().load(target);
    }

    /**
     * Saves a board file to the supplied file target.
     */
    public static void saveTo (BoardFile file, File target)
        throws IOException
    {
        BinaryExporter.getInstance().save(file, new FileOutputStream(target));
    }

    /**
     * Returns the index of the earliest town in which this board can be used.
     */
    public int getMinimumTownIndex ()
    {
        int minTownIdx = 0;
        for (String scenId : scenarios) {
            ScenarioInfo info = ScenarioInfo.getScenarioInfo(scenId);
            if (info != null) {
                minTownIdx = Math.max(minTownIdx, BangUtil.getTownIndex(info.getTownId()));
            }
        }
        return minTownIdx;
    }

    // from interface Savable
    public Class<?> getClassTag ()
    {
        return BoardFile.class;
    }

    // from interface Savable
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        name = capsule.readString("name", "");
        creator = capsule.readString("creator", "");
        scenarios = capsule.readStringArray("scenarios", DEF_SCENARIOS);
        players = capsule.readInt("players", 2);
        board = (BangBoard)capsule.readSavable("board", null);
        pieces = capsule.readSavableArrayList("pieces", NO_PIECES);
        privateBoard = capsule.readBoolean("private", false);
        dataHash = BoardData.getDataHash(toData().toBytes());
    }

    // from interface Savable
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(name, "name", "");
        capsule.write(creator, "creator", "");
        capsule.write(scenarios, "scenarios", DEF_SCENARIOS);
        capsule.write(players, "players", 2);
        capsule.write(board, "board", null);
        capsule.writeSavableArrayList(pieces, "pieces", NO_PIECES);
        capsule.write(privateBoard, "private", false);
    }

    protected static final String[] DEF_SCENARIOS = {};
    protected static final ArrayList<Piece> NO_PIECES = new ArrayList<Piece>();
}
