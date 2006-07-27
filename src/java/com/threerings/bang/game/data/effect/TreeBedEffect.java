//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a tree grew or shrunk.
 */
public class TreeBedEffect extends Effect
{
    /** The id of the affected tree bed. */
    public int bedId;
    
    /** The ids of the pieces growing or shrinking the tree. */
    public int[] pieceIds;
    
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
     * @param pieces the pieces growing or shrinking the tree
     * @param damage the amount of damage inflicted
     */
    public TreeBedEffect (TreeBed bed, Piece[] pieces, int damage)
    {
        bedId = bed.pieceId;
        pieceIds = new int[pieces.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            pieceIds[ii] = pieces[ii].pieceId;
        }
        this.damage = damage;
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { bedId };
    }
    
    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return pieceIds;
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // award points to the growers if the growth is positive
        if (damage > 0) {
            return;
        }
        for (int pieceId : pieceIds) {
            Piece piece = bangobj.pieces.get(pieceId);
            if (piece == null || piece.owner == -1) {
                continue;
            }
            bangobj.stats[piece.owner].incrementStat(Stat.Type.TREE_POINTS,
                ForestGuardiansInfo.POINTS_PER_TREE_GROWTH);
            bangobj.grantPoints(piece.owner,
                ForestGuardiansInfo.POINTS_PER_TREE_GROWTH);
        }
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer observer)
    {
        TreeBed bed = (TreeBed)bangobj.pieces.get(bedId);
        if (bed == null) {
            log.warning("Missing piece for tree bed effect [pieceId=" +
                bedId + "].");
            return false;
        }
        bed.damage(damage);
        reportEffect(observer, bed, UPDATED);
        return true;
    }
}
