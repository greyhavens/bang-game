//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import com.threerings.media.util.MathUtil;

import com.samskivert.bang.client.sprite.ArtillerySprite;
import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.Shot;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PointSet;

import static com.samskivert.bang.Log.log;

/**
 * Handles the state and behavior of the artillery piece.
 */
public class Artillery extends Piece
    implements PlayerPiece
{
    /** A tank can fire at a target up to seven squares away. */
    public static final int FIRE_DISTANCE = 4;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ArtillerySprite();
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return 9;
    }

    @Override // documentation inherited
    public void react (BangObject bangobj, Piece[] pieces, PieceSet updates,
                       ArrayList<Shot> shots)
    {
        Piece target = null;
        int dist = Integer.MAX_VALUE;
        int fdist = FIRE_DISTANCE*FIRE_DISTANCE;

        // locate the closest target in range and shoot 'em!
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (!validTarget(p)) {
                continue;
            }
            int pdist = MathUtil.distanceSq(x, y, p.x, p.y);
            if (pdist <= fdist && pdist < dist) {
                dist = pdist;
                target = p;
            }
        }

        if (target != null) {
            shots.add(shoot(target));
            updates.add(target);
        }
    }

    @Override // documentation inherited
    public void enumerateAttacks (PointSet set)
    {
        int fdist = FIRE_DISTANCE*FIRE_DISTANCE;
        for (int yy = y - FIRE_DISTANCE; yy <= y + FIRE_DISTANCE; yy++) {
            for (int xx = x - FIRE_DISTANCE; xx <= x + FIRE_DISTANCE; xx++) {
                int pdist = MathUtil.distanceSq(x, y, xx, yy);
                if ((xx != x || yy != y) && (pdist <= fdist)) {
                    set.add(xx, yy);
                }
            }
        }
    }

    /**
     * Affects the target piece with damage.
     */
    public Shot shoot (Piece target)
    {
        int damage = Math.min(target.energy, target.maximumEnergy()/10);
        log.info("Doing " + damage + " damage to " + target + ".");
        target.energy -= damage;
        return new Shot(pieceId, target.x, target.y);
    }

    /** Returns true if we can and should fire upon this target. */
    protected boolean validTarget (Piece target)
    {
        return (target != null && target.owner != -1 &&
                target.owner != owner && target.energy > 0);
    }
}
