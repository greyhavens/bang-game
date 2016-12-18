//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.ReboundHandler;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.LoggingRobot;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a proximity shot was fired from one piece to adjacent
 * pieces.
 */
public class SpringShotEffect extends ShotEffect
{
    /**
     * Constructor used when creating a new proximity shot effect.
     */
    public SpringShotEffect (Piece shooter, Piece target, int damage,
                       String[] attackIcons, String[] defendIcons)
    {
        super(shooter, target, damage, attackIcons, defendIcons);
    }

    /** Constructor used when unserializing. */
    public SpringShotEffect ()
    {
    }

    /** The x and y coordinates to which the target was sent. */
    public short x, y;

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Piece piece = bangobj.pieces.get(targetId);
        return new Rectangle [] {
            new Rectangle(piece.x, piece.y, 1, 1), new Rectangle(x, y, 1, 1)
        };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        Piece target = bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing target for rebound effect", "id", targetId);
            return;
        }

        // choose a random distance within the limits; it doesn't matter that
        // it will be biased towards the edges
        int dist = RandomUtil.getInRange(MIN_REBOUND_DISTANCE - 1, MAX_REBOUND_DISTANCE);
        Point pt = null;
        while (pt == null && dist > 0) {
            pt = bangobj.board.getOccupiableSpot(target.x, target.y, dist,
                dist + 3, null);
            dist--;
        }
        if (pt == null) {
            log.warning("Couldn't find occupiable spot for rebound effect", "x", target.x,
                        "y", target.y, "dist", dist);
            x = target.x;
            y = target.y;
        } else {
            x = (short)pt.x;
            y = (short)pt.y;
        }
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        // logging robots are built to stand being dropped from the sky
        return (piece instanceof LoggingRobot) ? 0 : REBOUND_DAMAGE;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new ReboundHandler();
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // move the piece
        Piece target = bangobj.pieces.get(targetId);
        if (target == null) {
            log.warning("Missing target for spring shot effect", "id", targetId);
            return false;
        }
        moveAndReport(bangobj, target, x, y, obs);

        return true;
    }

    /**
     * Damage the target piece and handle any death effects.
     */
    public boolean pieceDropped (BangObject bangobj, Observer obs)
    {
        Piece target = bangobj.pieces.get(targetId);
        Piece shooter = bangobj.pieces.get(shooterId);

        if (target == null) {
            log.warning("Missing target for spring shot effect", "id", targetId);
            return false;
        } else {
            damage(bangobj, obs, shooter.owner, shooter, target, getBaseDamage(target),
                ShotEffect.DAMAGED);
            return true;
        }
    }

    /** The minimum distance away to send sprung pieces. */
    protected static final int MIN_REBOUND_DISTANCE = 5;

    /** The maximum distance away. */
    protected static final int MAX_REBOUND_DISTANCE = 10;

    /** The amount of damage caused on landing. */
    protected static final int REBOUND_DAMAGE = 20;
}
