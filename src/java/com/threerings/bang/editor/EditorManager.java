//
// $Id$

package com.threerings.bang.editor;

import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.PieceDSet;
import com.threerings.bang.game.data.generate.CompoundGenerator;
import com.threerings.bang.game.data.piece.Piece;

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
        BangBoard board = new BangBoard(DEFAULT_BOARD_WIDTH,
            DEFAULT_BOARD_HEIGHT);
        CompoundGenerator gen = new CompoundGenerator();
        gen.generate(board, pieces);
        return board;
    }

    /** A casted reference to our game object. */
    protected BangObject _bangobj;
    
    /** The default board dimensions. */
    protected static final int DEFAULT_BOARD_WIDTH = 32;
    protected static final int DEFAULT_BOARD_HEIGHT = 32;
}
