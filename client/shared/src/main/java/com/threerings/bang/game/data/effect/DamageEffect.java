//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Generic damage effect.
 */
public class DamageEffect extends Effect
{
    /** The id of the damaged piece. */
    public int pieceId;

    /** The base amount of damage to inflict. */
    public int baseDamage;

    /** The piece's new damage amount. */
    public int ndamage;

    /** The player index originating the damage. */
    public int pidx = -1;

    /** The piece's death effect, if it died. */
    public Effect deathEffect;

    public DamageEffect ()
    {
    }

    public DamageEffect (Piece piece, int damage)
    {
        pieceId = piece.pieceId;
        baseDamage = damage;
    }

    public DamageEffect (Piece piece, int damage, int pidx)
    {
        this(piece, damage);
        this.pidx = pidx;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (deathEffect == null) {
            return new int[] { pieceId };
        }
        return ArrayUtil.append(deathEffect.getAffectedPieces(), pieceId);
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return (deathEffect == null) ? NO_PIECES : deathEffect.getWaitPieces();
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece != null) {
            ndamage = Math.min(100, piece.damage + baseDamage);
            dammap.increment(piece.owner, ndamage - piece.damage);
            if (ndamage == 100 && piece.damage < 100) {
                deathEffect = piece.willDie(bangobj, -1);
                if (deathEffect != null) {
                    deathEffect.prepare(bangobj, dammap);
                }
            }
        } else {
            log.warning("Damage effect missing piece", "id", pieceId);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (deathEffect != null) {
            deathEffect.apply(bangobj, obs);
        }
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing piece for damage effect", "id", pieceId);
            return false;
        }
        return damage(bangobj, obs, pidx, null, piece, ndamage,
            ShotEffect.DAMAGED);
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return baseDamage;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.DamageEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.DamageEffect.class, "baseDamage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.DamageEffect.class, "ndamage", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.DamageEffect.class, "pidx", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.DamageEffect.class, "deathEffect", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.DamageEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.DamageEffect.class, "baseDamage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.DamageEffect.class, "ndamage", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.DamageEffect.class, "pidx", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.DamageEffect.class, "deathEffect", this, out);
    }
}
