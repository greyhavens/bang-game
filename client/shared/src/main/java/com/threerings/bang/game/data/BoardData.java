//
// $Id$

package com.threerings.bang.game.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains the data ({@link BangBoard} and props, markers, etc.) associated
 * with a board stored on the client or server.
 *
 * <p>jME3 cutover (Phase 3): the on-disk/byte form is now pure Narya streaming (this class is
 * already a {@link Streamable}, as are {@link BangBoard} and the {@link Piece} hierarchy). The
 * fork/jME3 {@code Savable} {@code BinaryExporter}/{@code Importer} path is dropped — no jME3
 * dependency on the board read path (see docs/jme3-board-conversion.md §5).
 */
public class BoardData
    implements Streamable
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
        try {
            return (BoardData)new ObjectInputStream(
                new ByteArrayInputStream(data)).readObject();
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe);
        }
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
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(this);
        oout.flush();
        return bout.toByteArray();
    }
}
