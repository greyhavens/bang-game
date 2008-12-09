//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.client.MoveShootHandler;
import com.threerings.bang.game.client.EffectHandler;

import static com.threerings.bang.Log.log;

/**
 * An effect used when a unit moves and shoots during the movement.
 */
public class MoveShootEffect extends MoveEffect
{
    /** The shot effect. */
    public ShotEffect shotEffect;

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return concatenate(super.getAffectedPieces(), 
                shotEffect.getAffectedPieces());
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return shotEffect.getWaitPieces();
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return concatenate(super.getBounds(bangobj), 
                shotEffect.getBounds(bangobj));
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        shotEffect.prepare(bangobj, dammap);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for move effect", "id", pieceId);
            return false;
        }

        piece.lastActed = newLastActed;
        int ox = piece.x, oy = piece.y;
        moveAndReport(bangobj, piece, nx, ny, obs);
        piece.didMove(piece.getDistance(ox, oy));

        // apply the shot effect immediately on the server
        if (bangobj.getManager().isManager(bangobj)) {
            return shotEffect.apply(bangobj, obs);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new MoveShootHandler();
    }
    
    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return shotEffect.getBaseDamage(piece);
    }
}
