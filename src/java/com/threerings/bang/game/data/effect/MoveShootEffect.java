//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.client.MoveShootHandler;
import com.threerings.bang.game.client.EffectHandler;

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
    public Rectangle getBounds ()
    {
        Rectangle rect = super.getBounds();
        rect.add(shotEffect.getBounds());
        return rect;
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
        super.apply(bangobj, obs);

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
