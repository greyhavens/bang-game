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
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Sends the victim back to its last position.
 */
public class ReboundEffect extends TrapEffect
{
    /** Fired off when the spring is activated. */
    public static final String ACTIVATED_SPRING = "frontier_town/spring";

    /** The x and y coordinates to which the target was sent. */
    public short x, y;

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        return new Rectangle [] {
            new Rectangle(piece.x, piece.y, 1, 1), new Rectangle(x, y, 1, 1)
        };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        Piece target = bangobj.pieces.get(pieceId);
        if (target == null) {
            log.warning("Missing target for rebound effect", "id", pieceId);
            return;
        }

        // choose a random distance within the limits; it doesn't matter that
        // it will be biased towards the edges
        int dist = RandomUtil.getInRange(MIN_REBOUND_DISTANCE - 1,
            MAX_REBOUND_DISTANCE);
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
    public boolean trapPiece (BangObject bangobj, Observer obs, int causer)
    {
        // move the piece
        Piece target = bangobj.pieces.get(pieceId);
        if (target == null) {
            log.warning("Missing target for rebound effect", "id", pieceId);
            return false;
        }
        moveAndReport(bangobj, target, x, y, obs);

        // if on the server proceed immediately
        if (bangobj.getManager().isManager(bangobj)) {
            return super.trapPiece(bangobj, obs, causer);
        }
        _causer = causer;
        return true;
    }

    /**
     * Called by the effect handler to finish applying the effect after the
     * spring animation has completed.
     */
    public void finishTrapPiece (BangObject bangobj, Observer obs)
    {
        super.trapPiece(bangobj, obs, _causer);
    }

    protected transient int _causer;

    /** The minimum distance away to send sprung pieces. */
    protected static final int MIN_REBOUND_DISTANCE = 5;

    /** The maximum distance away. */
    protected static final int MAX_REBOUND_DISTANCE = 10;

    /** The amount of damage caused on landing. */
    protected static final int REBOUND_DAMAGE = 20;
}
