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
    public static final int COLLISION_DAMAGE = 20;
    
    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    /** The piece id of the target. */
    public int targetId;

    /** The x and y coordinates to which the target was pushed. */
    public short x, y;
    
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
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target != null) {
            dammap.increment(target.owner, COLLISION_DAMAGE);
        } else {
            log.warning("Train effect missing target [id=" + targetId + "].");
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        collide(bangobj, obs, -1, targetId, COLLISION_DAMAGE, x, y, DAMAGED);
    }
    
    /**
     * Handles a collision that moves and damages a unit.
     *
     * @param collider the index of the user causing the collision, or -1
     */
    public static void collide (BangObject bangobj, Observer obs, int collider,
        int targetId, int damage, int x, int y, String effect)
    {
        Piece target = (Piece)bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing collision target [targetId=" + targetId +
                "].");
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
            ShotEffect.damage(bangobj, obs, collider, target,
                Math.min(100, target.damage + damage), effect);
        }
    }
}
