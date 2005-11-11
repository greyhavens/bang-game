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
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    /** The piece id of the target. */
    public int targetId;

    /** The new total damage to assign to the target. */
    public int newDamage;

    /**
     * Constructor used when creating a new train effect.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     */
    public TrainEffect (Piece target, int damage)
    {
        setTarget(target, damage);
    }

    /** Constructor used when unserializing. */
    public TrainEffect ()
    {
    }

    /**
     * Configures this train effect with a target and a damage amount. Any
     * previous target will be overridden.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     */
    public void setTarget (Piece target, int damage)
    {
        targetId = target.pieceId;
        newDamage = Math.min(100, target.damage + damage);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target != null) {
            dammap.increment(target.owner, newDamage - target.damage);
        } else {
            log.warning("Train effect missing target [id=" + targetId + "].");
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing train target " + this + ".");
            return;
        }
        ShotEffect.damage(bangobj, obs, -1, target, newDamage, DAMAGED);
    }
}
