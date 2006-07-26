//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.TreeBed;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a tree grew or shrunk.
 */
public class TreeBedEffect extends Effect
{
    /** The id of the affected tree bed. */
    public int pieceId;
    
    /** The amount of damage inflicted. */
    public int damage;
    
    /**
     * No-arg constructor for deserialization.
     */
    public TreeBedEffect ()
    {
    }
    
    /**
     * Creates a new tree bed effect.
     *
     * @param bed the affected bed
     * @param damage the amount of damage inflicted
     */
    public TreeBedEffect (TreeBed bed, int damage)
    {
        pieceId = bed.pieceId;
        this.damage = damage;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // no-op
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer observer)
    {
        TreeBed bed = (TreeBed)bangobj.pieces.get(pieceId);
        if (bed == null) {
            log.warning("Missing piece for tree bed effect [pieceId=" +
                pieceId + "].");
            return false;
        }
        bed.damage(damage);
        reportEffect(observer, bed, UPDATED);
        return true;
    }
}
