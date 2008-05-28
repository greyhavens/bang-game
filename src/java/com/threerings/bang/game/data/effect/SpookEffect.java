//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.util.PointSet;
import com.samskivert.util.IntIntMap;

/**
 * A move effect for a cow that also changes its ownership.
 */
public class SpookEffect extends MoveEffect
    implements PieceCodes
{
    /** An effect reported when a cow is branded. */
    public static final String BRANDED = "frontier_town/cow/branded";

    /** An effect reported when a cow is merely spooked. */
    public static final String SPOOKED = "frontier_town/cow/spooked";

    /** The new owner of the cow. */
    public int owner = -1;

    /** The piece id of the spooker (used for animation purposes). */
    public int spookerId;

    /** The direction we're being spooked in. */
    public int direction;

    public SpookEffect ()
    {
    }

    public SpookEffect (int owner, int spookerId, int direction)
    {
        this.owner = owner;
        this.spookerId = spookerId;
        this.direction = direction;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { spookerId };
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle [] {
            new Rectangle(ox, oy, 1, 1),
            new Rectangle(nx-1, ny, 3, 1),
            new Rectangle(nx, ny-1, 1, 3)
        };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);
        nx = ox;
        ny = oy;

        Cow cow = (Cow)bangobj.pieces.get(pieceId);
        if (cow == null) {
            return;
        }

        // otherwise look around for somewhere nicer to stand
        PointSet moves = new PointSet();
        bangobj.board.computeMoves(cow, moves, null);
        int[] coords = moves.toIntArray();
        ArrayUtil.shuffle(coords);

        // move any coords containing train tracks to the end
        int tidx = coords.length;
        for (int ii = 0; ii < tidx; ) {
            if (bangobj.getTracks().containsKey(coords[ii])) {
                int tmp = coords[--tidx];
                coords[tidx] = coords[ii];
                coords[ii] = tmp;
            } else {
                ii++;
            }
        }

        // first look for a coordinate in the direction that we want to move
        // (that's not a track)
        for (int ii = 0; ii < tidx; ii++) {
            int hx = PointSet.decodeX(coords[ii]);
            int hy = PointSet.decodeY(coords[ii]);
            if (whichDirection(hx, hy) == direction) {
                nx = (short)hx;
                ny = (short)hy;
                break;
            }
        }

        // if that failed, go with anything that works
        if (nx == ox && ny == oy && coords.length > 0) {
            nx = (short)PointSet.decodeX(coords[0]);
            ny = (short)PointSet.decodeY(coords[0]);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // update the cow's owner (if appropriate), then let them move
        Cow cow = (Cow)bangobj.pieces.get(pieceId);
        if (cow != null) {
            String effect = SPOOKED;
            if (owner != -1 && cow.owner != owner) {
                cow.setOwner(bangobj, owner);
                effect = BRANDED;
            }
            // report an effect on the cow so that we can play a sound
            reportEffect(obs, cow, effect);
        }
        return super.apply(bangobj,obs);
    }

    protected int whichDirection (int nx, int ny)
    {
        if (nx == ox) {
            return (ny < oy) ? NORTH : SOUTH;
        } else if (ny == oy) {
            return (nx < ox) ? WEST : EAST;
        }
        return -1;
    }
}
