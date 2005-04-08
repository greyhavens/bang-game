//
// $Id$

package com.samskivert.bang.data.piece;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import com.threerings.media.util.MathUtil;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.UnitSprite;
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
        return new UnitSprite("artillery");
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return 9;
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 2;
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return FIRE_DISTANCE;
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
            if (pdist <= fdist && pdist < dist && pdist > 1) {
                dist = pdist;
                target = p;
            }
        }

        if (target != null) {
            shots.add(shoot(target));
        }
    }

    @Override // documentation inherited
    protected boolean validTarget (Piece target)
    {
        return super.validTarget(target) && !(target instanceof Chopper);
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 34;
        } else if (target instanceof Chopper) {
            return 0;
        } else if (target instanceof Artillery) {
            return 34;
        } else if (target instanceof Marine) {
            return 17;
        } else {
            return super.computeDamage(target);
        }
    }
}
