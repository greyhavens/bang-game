//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Observer;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.ChainingShotHandler;
import com.threerings.bang.game.client.EffectHandler;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Communicates that a chaining shot was fired from one piece to another.
 */
public class ChainingShotEffect extends ShotEffect
{
    public ShotEffect[] chainShot;
    
    /**
     * Constructor used when creating a new chaining shot effect.
     */
    public ChainingShotEffect (Piece shooter, Piece target, int damage,
                       String attackIcon, String defendIcon)
    {
        super(shooter, target, damage, attackIcon, defendIcon);
    }

    /** Constructor used when unserializing. */
    public ChainingShotEffect ()
    {
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieces = super.getAffectedPieces();
        for (ShotEffect chain : chainShot) {
            pieces = concatenate(pieces, chain.getAffectedPieces());
        }
        return pieces;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] pieces = super.getAffectedPieces();
        for (ShotEffect chain : chainShot) {
            pieces = concatenate(pieces, chain.getAffectedPieces());
        }
        return pieces;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        for (ShotEffect chain : chainShot) {
            chain.prepare(bangobj, dammap);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (!super.apply(bangobj, obs)) {
            return false;
        }
        for (ShotEffect chain : chainShot) {
            chain.shooterLastActed = -1;
            if (!chain.apply(bangobj, obs)) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new ChainingShotHandler();
    }
}
