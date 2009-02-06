//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Allows players to put an impassable barricade on the board that
 * expires after some number of ticks.
 */
public class Barricade extends AddPieceCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "barricade";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.STEADS_CLAIMED_2;
    }
    
    // documentation inherited
    protected Piece createPiece ()
    {
        return new com.threerings.bang.game.data.piece.Barricade();
    }
    
    @Override // documentation inherited
    protected String getAddedEffect ()
    {
        return AddPieceEffect.DROPPED;
    }
}
