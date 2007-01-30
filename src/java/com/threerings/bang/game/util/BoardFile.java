//
// $Id$

package com.threerings.bang.game.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

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
    public boolean privateBoard = true;

    /** The number of players for which this board is appropriate. */
    public int players;

    /** The board itself (elevation data, etc.). */
    public BangBoard board;

    /** The props and markers on the board. */
    public Piece[] pieces;

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

    // from interface Savable
    public Class getClassTag ()
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
        pieces = ArrayUtil.copy(capsule.readSavableArray("pieces", null), new Piece[0]);
        privateBoard = capsule.readBoolean("private", false);
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
        capsule.write(pieces, "pieces", null);
        capsule.write(privateBoard, "private", false);
    }

    protected static final String[] DEF_SCENARIOS = {};
}
