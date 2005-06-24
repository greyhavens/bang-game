//
// $Id$

package com.threerings.bang.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.util.Tuple;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.PieceDSet;
import com.threerings.bang.data.generate.CompoundGenerator;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BoardUtil;

import static com.threerings.bang.Log.log;

/**
 * Handles the server side of the "editor" mode of the game.
 */
public class EditorManager extends GameManager
{
    // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BangObject.class;
    }

    // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();
        _bangobj = (BangObject)_gameobj;
    }

    // documentation inherited
    protected void gameWillStart ()
    {
        super.gameWillStart();

        // set up the game object
        ArrayList<Piece> pieces = new ArrayList<Piece>();
        _bangobj.setBoard(createBoard(pieces));
        _bangobj.setPieces(new PieceDSet(pieces.iterator()));

        // initialize our pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            ((Piece)iter.next()).init();
        }
    }

    /**
     * Creates the bang board based on the game config, filling in the
     * supplied pieces array with the starting pieces.
     */
    protected BangBoard createBoard (ArrayList<Piece> pieces)
    {
        // first, try loading it from our game configuration
        EditorConfig gconfig = (EditorConfig)_gameconfig;
//         if (gconfig.board != null && gconfig.board.length > 0) {
//             try {
//                 Tuple tup = BoardUtil.loadBoard(gconfig.board);
//                 BangBoard board = (BangBoard)tup.left;
//                 Piece[] pvec = (Piece[])tup.right;
//                 Collections.addAll(pieces, pvec);
//                 return board;
//             } catch (IOException ioe) {
//                 log.log(Level.WARNING, "Failed to unserialize board.", ioe);
//             }
//         }

        // if that doesn't work, generate a random board
        int size = 16; // gconfig.size;
        BangBoard board = new BangBoard(size, size);
        CompoundGenerator gen = new CompoundGenerator();
        gen.generate(board, pieces);
        return board;
    }

    /** A casted reference to our game object. */
    protected BangObject _bangobj;
}
