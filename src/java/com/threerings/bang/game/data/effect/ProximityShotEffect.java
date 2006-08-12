//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.ProximityShotHandler;
import com.threerings.bang.game.client.EffectHandler;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Communicates that a proximity shot was fired from one piece to adjacent
 * pieces.
 */
public class ProximityShotEffect extends ShotEffect
{
    public ShotEffect[] proxShot;
    
    /**
     * Constructor used when creating a new proximity shot effect.
     */
    public ProximityShotEffect (Piece shooter, Piece target, int damage,
                       String attackIcon, String defendIcon)
    {
        super(shooter, target, damage, attackIcon, defendIcon);
        type = PROXIMITY;
    }

    /** Constructor used when unserializing. */
    public ProximityShotEffect ()
    {
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = super.getAffectedPieces();
        for (ShotEffect prox : proxShot) {
            pieces = concatenate(pieces, prox.getAffectedPieces());
        }
        return pieces;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] pieces = super.getAffectedPieces();
        for (ShotEffect prox : proxShot) {
            pieces = concatenate(pieces, prox.getAffectedPieces());
        }
        return pieces;
    }

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        Rectangle rect = super.getBounds();
        for (ShotEffect prox : proxShot) {
            rect.add(prox.getBounds());
        }
        rect.grow(1, 1);
        return rect;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        for (ShotEffect prox : proxShot) {
            prox.type = PROXIMITY;
            prox.prepare(bangobj, dammap);
        }
    }

    @Override // documentation inherited
    public void preapply (BangObject bangobj, Observer obs)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);
        for (ShotEffect prox : proxShot) {
            prox.apply(bangobj, obs);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new ProximityShotHandler();
    }
}
