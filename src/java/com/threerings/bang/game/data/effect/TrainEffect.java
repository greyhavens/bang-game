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

    /** The x and y coordinates to which the target was pushed. */
    public short x, y;
    
    /**
     * Constructor used when creating a new train effect.
     *
     * @param damage the amount by which to increase the target's damage.
     * This will be capped and converted to an absolute value.
     * @param x the x coordinate to which the piece was pushed
     * @param y the y coordinate to which the piece was pushed
     */
    public TrainEffect (Piece target, int damage, int x, int y)
    {
        setTarget(target, damage, x, y);
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
     * @param x the x coordinate to which the piece was pushed
     * @param y the y coordinate to which the piece was pushed
     */
    public void setTarget (Piece target, int damage, int x, int y)
    {
        targetId = target.pieceId;
        newDamage = Math.min(100, target.damage + damage);
        this.x = (short)x;
        this.y = (short)y;   
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
        
        // move the target to its new coordinates
        if (target.x != x || target.y != y) {
            Piece otarget = target, ntarget = (Piece)target.clone();
            ntarget.position(x, y);
            reportUpdate(obs, otarget, ntarget);
            bangobj.updatePieceDirect(ntarget);
            target = ntarget;
        }
        
        // damage the target if it's still alive
        if (target.isAlive()) {
            ShotEffect.damage(bangobj, obs, -1, target, newDamage, DAMAGED);
        }
    }
}
