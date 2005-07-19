//
// $Id$

package com.threerings.bang.game.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.samskivert.util.Tuple;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.BonusMarker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.StartMarker;

/**
 * Bang board related utility routines.
 */
public class BoardUtil
{
    /**
     * Saves the supplied board and pieces to the specified target file.
     */
    public static void saveBoard (BangBoard board, Piece[] pieces, File target)
        throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(target)));
        oos.writeObject(board);
        oos.writeInt(pieces.length);
        for (int ii = 0; ii < pieces.length; ii++) {
            writePiece(oos, pieces[ii]);
        }
        oos.flush();
    }

    /**
     * Reads a board (and pieces) from a file created with a previous call
     * to {@link #saveBoard}.
     *
     * @return an {@link Tuple} wherein the left side is the board and the
     * right side is an array of {@link Piece} objects.
     */
    public static Tuple loadBoard (File source)
        throws IOException
    {
        return loadBoard(new BufferedInputStream(new FileInputStream(source)));
    }

    /**
     * Reads a board (and pieces) from a byte array loaded from a file
     * created with a previous call to {@link #saveBoard}.
     *
     * @return an {@link Tuple} wherein the left side is the board and the
     * right side is an array of {@link Piece} objects.
     */
    public static Tuple loadBoard (byte[] source)
        throws IOException
    {
        return loadBoard(new ByteArrayInputStream(source));
    }

    public static void main (String[] args)
    {
        try {
            loadBoard(new File(args[0]));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /** Helper for the two load board methods. */
    protected static Tuple loadBoard (InputStream input)
        throws IOException
    {
        ObjectInputStream oin = new ObjectInputStream(input);
        Tuple tuple = new Tuple();
        try {
            tuple.left = oin.readObject();
        } catch (ClassNotFoundException cnfe) {
            IOException ioe = new IOException("Missing class?");
            ioe.initCause(cnfe);
            throw ioe;
        }
        int pcount = oin.readInt();
        Piece[] pieces = new Piece[pcount];
        for (int ii = 0; ii < pieces.length; ii++) {
            pieces[ii] = readPiece(oin);
        }
        tuple.right = pieces;
        return tuple;
    }

    /** Helper method. */
    protected static void writePiece (ObjectOutputStream oout, Piece piece)
        throws IOException
    {
        if (piece instanceof Prop) {
            oout.writeUTF(((Prop)piece).getType());
        } else if (piece instanceof StartMarker) {
            oout.writeUTF("__start__");
        } else if (piece instanceof BonusMarker) {
            oout.writeUTF("__bonus__");
        } else {
            throw new IOException("Unknown piece type " +
                                  "[type=" + piece.getClass().getName() +
                                  ", piece=" + piece + "].");
        }
        oout.writeInt(piece.pieceId);
        oout.writeShort(piece.x);
        oout.writeShort(piece.y);
        oout.writeShort(piece.orientation);
    }

    /** Helper method. */
    protected static Piece readPiece (ObjectInputStream oin)
        throws IOException
    {
        String type = oin.readUTF();
        Piece piece;
        if (type.equals("__start__")) {
            piece = new StartMarker();
        } else if (type.equals("__bonus__")) {
            piece = new BonusMarker();
        } else {
            piece = Prop.getProp(type);
        }
        piece.pieceId = oin.readInt();
        short x = oin.readShort(), y = oin.readShort();
        piece.orientation = oin.readShort();
        piece.position(x, y);
        return piece;
    }
}
