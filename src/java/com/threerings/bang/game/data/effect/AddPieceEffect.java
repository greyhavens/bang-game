//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Adds a piece to the board.
 */
public class AddPieceEffect extends Effect
{
    /** The respawned action: floats the piece up from the ground with a
     * glowing particle system. */
    public static final String RESPAWNED = "__respawned__";
    
    /** The dropped action: drops the piece from the sky and activates a dust
     * effect when it hits the ground. */
    public static final String DROPPED = "dropped";
    
    /** The piece to add. */
    public Piece piece;
    
    /** The effect to report on the piece after addition, if any. */
    public String effect;
    
    public AddPieceEffect ()
    {
    }
    
    public AddPieceEffect (Piece piece)
    {
        this.piece = piece;
    }
    
    public AddPieceEffect (Piece piece, String effect)
    {
        this.piece = piece;
        this.effect = effect;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { piece.pieceId };
    }

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        return new Rectangle(piece.x, piece.y, 1, 1);
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        bangobj.board.shadowPiece(piece);
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        addAndReport(bangobj, piece, obs);
        if (effect != null) {
            reportEffect(obs, piece, effect);
        }
        return true;
    }
}
