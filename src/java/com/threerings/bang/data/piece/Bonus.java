//
// $Id$

package com.threerings.bang.data.piece;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.bang.client.sprite.BonusSprite;
import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.data.effect.DuplicateEffect;
import com.threerings.bang.data.effect.Effect;

import com.threerings.bang.data.effect.GrantSurpriseEffect;
import com.threerings.bang.data.effect.RepairEffect;
import com.threerings.bang.data.surprise.MissileSurprise;
import com.threerings.bang.data.surprise.RepairSurprise;

/**
 * Represents an exciting bonus waiting to be picked up by a player on the
 * board. Bonuses may generate full-blown effects or just influence the
 * piece that picked them up.
 */
public class Bonus extends Piece
{
    /** Indicates the type of bonus. */
    public enum Type { UNKNOWN, REPAIR, DUPLICATE, MISSILE_SURPRISE,
                       REPAIR_SURPRISE };

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

    public Type getType ()
    {
        return _type;
    }

    /**
     * Called when a piece has landed on this bonus and is activating it,
     * this should return an object indicating the effect that the bonus
     * has on this piece or the entire board. Those effects will be
     * processed at the end of the tick.
     */
    public Effect affect (Piece piece)
    {
        switch (_type) {
        case REPAIR: return new RepairEffect(piece.pieceId);
        case DUPLICATE: return new DuplicateEffect(piece.pieceId);
        case MISSILE_SURPRISE:
            return new GrantSurpriseEffect(piece.owner, new MissileSurprise());
        case REPAIR_SURPRISE:
            return new GrantSurpriseEffect(piece.owner, new RepairSurprise());
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
        String type;
        switch (_type) {
        case REPAIR: type = "repair"; break;
        case MISSILE_SURPRISE: type = "surprise"; break;
        case REPAIR_SURPRISE: type = "surprise"; break;
        case DUPLICATE: type = "unknown"; break;
        default:
        case UNKNOWN: type = "unknown"; break;
        }
        return new BonusSprite(type);
    }

    /**
     * Extends default behavior to serialize our bonus type.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        _type = Type.valueOf(Type.class, in.readUTF());
    }

    /**
     * Extends default behavior to serialize our bonus type.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeUTF(_type.name());
    }

    /** The type of bonus we represent. */
    protected transient Type _type = Type.UNKNOWN;
}
