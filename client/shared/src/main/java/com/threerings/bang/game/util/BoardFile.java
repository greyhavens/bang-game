//
// $Id$

package com.threerings.bang.game.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BoardData;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.util.BangUtil;

/**
 * Contains all the data stored for a board when stored on the file system.
 *
 * <p>jME3 cutover (Phase 3): the on-disk {@code .board} format is now pure Narya streaming (header
 * + a {@link BoardData} graph), produced by the one-time tools/board-converter and read here with
 * no fork/jME3 {@code Savable} dependency (see docs/jme3-board-conversion.md §5). The on-disk
 * extension is unchanged ({@code .board}); only the bytes inside changed.
 */
public class BoardFile
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
        return loadFrom(new ByteArrayInputStream(data));
    }

    /**
     * Loads a board file from the supplied file target.
     */
    public static BoardFile loadFrom (File target)
        throws IOException
    {
        try (InputStream in = new BufferedInputStream(new FileInputStream(target))) {
            return loadFrom(in);
        }
    }

    /**
     * Loads a board file from the supplied input stream target. Reads the Narya {@code .nboard}
     * layout: {@code UTF name · UTF creator · int players · boolean private · object scenarios ·
     * object BoardData}. {@code dataHash} is derived on read (not stored).
     */
    public static BoardFile loadFrom (InputStream target)
        throws IOException
    {
        try {
            ObjectInputStream oin = new ObjectInputStream(target);
            BoardFile bf = new BoardFile();
            bf.name = oin.readUTF();
            bf.creator = oin.readUTF();
            bf.players = oin.readInt();
            bf.privateBoard = oin.readBoolean();
            bf.scenarios = (String[])oin.readObject();
            BoardData bd = (BoardData)oin.readObject();
            bf.board = bd.board;
            bf.pieces = bd.pieces;
            bf.dataHash = BoardData.getDataHash(bf.toData().toBytes());
            return bf;
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe);
        }
    }

    /**
     * Saves a board file to the supplied file target (the editor save path), in the Narya
     * {@code .nboard} layout that {@link #loadFrom} reads.
     */
    public static void saveTo (BoardFile file, File target)
        throws IOException
    {
        try (OutputStream out = new FileOutputStream(target)) {
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeUTF(file.name == null ? "" : file.name);
            oout.writeUTF(file.creator == null ? "" : file.creator);
            oout.writeInt(file.players);
            oout.writeBoolean(file.privateBoard);
            oout.writeObject(file.scenarios == null ? DEF_SCENARIOS : file.scenarios);
            oout.writeObject(new BoardData(file.board, file.pieces));
            oout.flush();
        }
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

    protected static final String[] DEF_SCENARIOS = {};
}
