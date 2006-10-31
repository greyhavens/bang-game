//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.CollisionHandler;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a unit was hit by a train.
 */
public class TrainEffect extends Effect
{
    /** The piece id of the target. */
    public int targetId;

    /** The x and y coordinates to which the target was pushed. */
    public short x, y;
    
    /** The target's death effect, if it died. */
    public Effect deathEffect;
    
    /** Constructor used when unserializing. */
    public TrainEffect ()
    {
    }
    
    /**
     * Constructor used when creating a new train effect.
     *
     * @param x the x coordinate to which the piece was pushed
     * @param y the y coordinate to which the piece was pushed
     */
    public TrainEffect (Piece target, int x, int y)
    {
        targetId = target.pieceId;
        this.x = (short)x;
        this.y = (short)y;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (deathEffect == null) {
            return new int[] { targetId };
        }
        return ArrayUtil.append(deathEffect.getAffectedPieces(), targetId);
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return (deathEffect == null) ? NO_PIECES : deathEffect.getWaitPieces();
    }
    
    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = bangobj.pieces.get(targetId);
        if (target != null) {
            int damage = Math.min(100, target.damage + COLLISION_DAMAGE);
            dammap.increment(target.owner, damage - target.damage);
            if (damage == 100 && target.damage < 100) {
                deathEffect = target.willDie(bangobj, -1);
                if (deathEffect != null) {
                    deathEffect.prepare(bangobj, dammap);
                }
            }
        } else {
            log.warning("Train effect missing target [id=" + targetId + "].");
        }
    }
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (deathEffect != null) {
            deathEffect.apply(bangobj, obs);
        }
        Piece target = bangobj.pieces.get(targetId);
        _wasAlive = (target != null) && target.isAlive();
        return collide(bangobj, obs, -1, -1, targetId, COLLISION_DAMAGE,
                       x, y, ShotEffect.DAMAGED);
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new CollisionHandler(COLLISION_DAMAGE);
    }
    
    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return COLLISION_DAMAGE;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(targetId);
        if (piece == null || piece.owner != pidx || !_wasAlive || pidx == -1) {
            return null;
        }
        return MessageBundle.compose("m.effect_train", piece.getName());
    }
    
    /** Whether or not the piece was alive before the train hit it. */
    protected boolean _wasAlive;
    
    /** The amount of damage taken by units hit by the train. */
    protected static final int COLLISION_DAMAGE = 50;
}
