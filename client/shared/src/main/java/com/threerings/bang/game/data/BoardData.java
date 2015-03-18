//
// $Id$

package com.threerings.bang.game.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.threerings.io.Streamable;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains the data ({@link BangBoard} and props, markers, etc.) associated
 * with a board stored on the client or server.
 */
public class BoardData
    implements Savable, Streamable
{
    /** The board itself (elevation data, etc.). */
    public BangBoard board;

    /** The props and markers on the board. */
    public List<Piece> pieces;

    /**
     * Loads and decodes the supplied serialized representation.
     */
    public static BoardData fromBytes (byte[] data)
        throws IOException
    {
        return (BoardData)BinaryImporter.getInstance().load(data);
    }

    /**
     * Computes and returns the MD5 hash of the supplied board data.
     */
    public static byte[] getDataHash (byte[] data)
    {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 codec not available");
        }
    }

    /**
     * An empty constructor for unserialization.
     */
    public BoardData ()
    {
    }

    /**
     * Creates a board data record with the supplied info.
     */
    public BoardData (BangBoard board, List<Piece> pieces)
    {
        this.board = board;
        this.pieces = pieces;
    }

    /**
     * Serializes this instance using the JME version tolerant binary format
     * and returns the serialized data.
     */
    public byte[] toBytes ()
        throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(this, bout);
        return bout.toByteArray();
    }

    // from interface Savable
    public Class<?> getClassTag ()
    {
        return BoardData.class;
    }

    // from interface Savable
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        board = (BangBoard)capsule.readSavable("board", null);
        pieces = capsule.readSavableArrayList("pieces", NO_PIECES);
    }

    // from interface Savable
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(board, "board", null);
        capsule.writeSavableArrayList(pieces, "pieces", NO_PIECES);
    }

    protected static final List<Piece> NO_PIECES = new ArrayList<Piece>();
}
