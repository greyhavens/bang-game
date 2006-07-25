//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * The effect of activating a trap.
 */
public class TrapEffect extends BonusEffect
{
    /** The victim of the trap. */
    public int pieceId;
    
    /** The victim's new damage. */
    public int newDamage;
    
    /** If the victim dies, its death effect. */
    public Effect deathEffect;
    
    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = new int[] { pieceId };
        if (deathEffect != null) {
            pieces = concatenate(pieces, deathEffect.getAffectedPieces());
        }
        return pieces;
    }
    
    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return (deathEffect == null) ? NO_PIECES : deathEffect.getWaitPieces();
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // determine the damage for the piece
        Piece target = bangobj.pieces.get(pieceId);
        if (target == null) {
            log.warning("Missing target for trap effect " +
                "[id=" + pieceId + "].");
            return;
        }
        int damage = getDamage(target);
        newDamage = target.damage + damage;
        dammap.increment(target.owner, damage);
        if (newDamage == 100) {
            deathEffect = target.willDie(bangobj, bonusId);
            if (deathEffect != null) {
                deathEffect.prepare(bangobj, dammap);
            }
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // find out who owns the bonus and what kind it is
        int causer = -1;
        Piece bonus = bangobj.pieces.get(bonusId);
        if (bonus != null) {
            causer = bonus.owner;
            _type = ((Bonus)bonus).getConfig().type;
            _type = _type.substring(_type.lastIndexOf('/') + 1);
        }
        
        // remove the bonus
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for trap effect " +
                        "[id=" + pieceId + "].");
            return false;
        }

        if (deathEffect != null) {
            deathEffect.apply(bangobj, obs);
        }
        damage(bangobj, obs, causer, piece, newDamage, "trap".equals(_type) ?
            ShotEffect.DAMAGED : ShotEffect.EXPLODED);
        
        return true;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            return null;
        }
        return MessageBundle.compose("m.effect_" + _type, piece.getName());
    }
    
    /**
     * Returns the amount of damage to apply to the victim.
     */
    protected int getDamage (Piece piece)
    {
        return Math.min(TRAP_DAMAGE, 100-piece.damage);
    }
    
    /** The type of the trap. */
    protected transient String _type;

    /** The amount of damage done by the trap. */
    protected static final int TRAP_DAMAGE = 50;
}
