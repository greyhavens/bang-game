//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Clears all wreckage from the board.
 */
public class ClearWreckageEffect extends GlobalEffect
{    
    /** The effect to apply to the board. */
    public static final String TUMBLEWEED_WIND = "indian_post/tumbleweed_wind";
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        affectBoard(bangobj, TUMBLEWEED_WIND, false, obs);
        return super.apply(bangobj, obs);
    }
    
    @Override // documentation inherited
    public boolean validPiece (Piece piece)
    {
        return (piece instanceof Unit && !piece.isAlive());
    }

    @Override // documentation inherited
    public Effect getEffect (Piece piece)
    {
        return new ClearPieceEffect(piece);
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_tumbleweed_wind";
    }
}
