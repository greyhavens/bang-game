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
import com.threerings.bang.game.data.piece.Piece;

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
        oos.writeObject(pieces);
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

        // temporary translations from an old packaging scheme
        for (int ii = 0; ii < XLATE.length; ii++) {
            oin.addTranslation("com.threerings.bang." + XLATE[ii],
                               "com.threerings.bang.game." + XLATE[ii]);
        }
        oin.addTranslation("[Lcom.threerings.bang.data.piece.Piece;",
                           "[Lcom.threerings.bang.game.data.piece.Piece;");

        // if we're running in Game Gardens, we need to configure the
        // appropriate class loader
        oin.setClassLoader(BoardUtil.class.getClassLoader());

        // now read in the board and pieces
        Tuple tuple = new Tuple();
        try {
            tuple.left = oin.readObject();
            tuple.right = oin.readObject();
        } catch (ClassNotFoundException cnfe) {
            IOException ioe = new IOException("Missing class?");
            ioe.initCause(cnfe);
            throw ioe;
        }
        return tuple;
    }

    protected static final String[] XLATE = {
        "data.BangBoard", "data.piece.BonusMarker", "data.piece.Prop",
        "data.piece.StartMarker" };
}
