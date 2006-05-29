//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a unit was hit by a train.
 */
public class TrainEffect extends Effect
{
    /** The amount of damage taken by units hit by the train. */
    public static final int COLLISION_DAMAGE = 50;
    
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

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
        return new int[] { targetId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target != null) {
            int damage = Math.min(100, target.damage + COLLISION_DAMAGE);
            dammap.increment(target.owner, damage - target.damage);
            if (damage == 100 && target.damage < 100) {
                deathEffect = target.willDie(bangobj, -1);
                deathEffect.prepare(bangobj, dammap);
            }
        } else {
            log.warning("Train effect missing target [id=" + targetId + "].");
        }
    }
    
    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        if (deathEffect != null) {
            deathEffect.apply(bangobj, obs);
        }
        collide(bangobj, obs, -1, targetId, COLLISION_DAMAGE, x, y, DAMAGED);
    }
}
