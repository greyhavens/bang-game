//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.BonusSprite;
import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.effect.DuplicateEffect;
import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.effect.RepairEffect;

/**
 * Represents an exciting bonus waiting to be picked up by a player on the
 * board. Bonuses may generate full-blown effects or just influence the
 * piece that picked them up.
 */
public class Bonus extends Piece
{
    /** Indicates the type of bonus. */
    public enum Type { UNKNOWN, REPAIR, DUPLICATE };

    /** Unserialization constructor. */
    public Bonus ()
    {
    }

    /**
     * Creates a particular type of bonus piece.
     */
    public Bonus (Type type)
    {
        _type = type;
    }

    /**
     * Called when a piece has landed on this bonus and is activating it,
     * this should return an object indicating the effect that the bonus
     * has on this piece or the entire board. Those effects will be
     * processed at the end of the tick.
     */
    public Effect affect (Piece other)
    {
        switch (_type) {
        case REPAIR: return new RepairEffect(other.pieceId);
        case DUPLICATE: return new DuplicateEffect(other.pieceId);
        }
        return null;
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BonusSprite("unknown");
    }

    /** The type of bonus we represent. */
    protected transient Type _type = Type.UNKNOWN;
}
