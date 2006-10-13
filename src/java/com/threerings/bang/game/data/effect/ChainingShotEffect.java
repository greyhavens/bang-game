//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

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
    /** An effect reported on the primary target. */
    public static final String PRIMARY_EFFECT =
        "indian_post/storm_caller/attack";

    /** An effect reported on the chained targets. */
    public static final String SECONDARY_EFFECT =
        "indian_post/storm_caller/damage";

    public ShotEffect[] chainShot;

    /**
     * Constructor used when creating a new chaining shot effect.
     */
    public ChainingShotEffect (Piece shooter, Piece target, int damage,
                       String[] attackIcons, String[] defendIcons)
    {
        super(shooter, target, damage, attackIcons, defendIcons);
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
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Rectangle[] results = super.getBounds(bangobj);
        for (ShotEffect chain : chainShot) {
            results = concatenate(results, chain.getBounds(bangobj));
        }
        return results;
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
        // process all levels
        for (int dist = 0; apply(bangobj, obs, dist); dist++);
        return true;
    }

    /**
     * Applies the shot effects at the specified distance.
     *
     * @return true if there are more shot effects, false if this is the
     * last level
     */
    public boolean apply (BangObject bangobj, Observer obs, int dist)
    {
        if (dist == 0) {
            Piece ptarget = bangobj.pieces.get(targetId);
            if (ptarget != null) {
                reportEffect(obs, ptarget, PRIMARY_EFFECT);
            }
            super.apply(bangobj, obs);
            return chainShot.length > 0;

        } else {
            boolean remaining = false;
            int px = xcoords[0], py = ycoords[0];
            for (ShotEffect chain : chainShot) {
                int cdist = Math.abs(px - chain.xcoords[0]) +
                    Math.abs(py - chain.ycoords[0]);
                if (cdist == dist) {
                    Piece starget = bangobj.pieces.get(chain.targetId);
                    if (starget != null) {
                        reportEffect(obs, starget, SECONDARY_EFFECT);
                    }
                    chain.apply(bangobj, obs);

                } else if (cdist > dist) {
                    remaining = true;
                }
            }
            return remaining;
        }
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new ChainingShotHandler();
    }
}
