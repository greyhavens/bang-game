//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.util.List;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TrainSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;

/**
 * A train car or engine.
 */
public class Train extends Piece
{
    /** The engine that leads the train. */
    public static final byte ENGINE = 0;

    /** The caboose that tails the train. */
    public static final byte CABOOSE = 1;

    /** A car carrying cattle. */
    public static final byte CATTLE_CAR = 2;

    /** A car carrying freight. */
    public static final byte FREIGHT_CAR = 3;

    /** A car carrying coal. */
    public static final byte COAL_CAR = 4;

    /** The types of cars to insert between the engine and the caboose. */
    public static final byte[] CAR_TYPES =
        { CATTLE_CAR, FREIGHT_CAR, COAL_CAR };

    /** A special value indicating that a short value is unset. */
    public static final short UNSET = Short.MIN_VALUE;

    /** The type of train piece: engine, caboose, etc. */
    public byte type;

    /** The last occupied position of the piece. */
    public short lastX = UNSET, lastY = UNSET;

    /** The next position that the piece will occupy. */
    public short nextX, nextY;

    /** The connected track group on which this train lies. */
    public int group;

    /** The path being followed by the train, if any. */
    public transient List<Point> path;

    /**
     * Attempts to find a path from the train's next position to the given
     * destination.
     *
     * @return the computed path, or <code>null</code> if a path couldn't
     * be found
     */
    public List<Point> findPath (final BangObject bangobj, Track dest)
    {
        if (nextX == UNSET || dest.intersects(nextX, nextY)) {
            // we're about to disappear, or that's already where we're going
            return null;
        }
        AStarPathUtil.TraversalPred tpred = new AStarPathUtil.TraversalPred() {
            public boolean canTraverse (Object traverser, int x, int y) {
                return !intersects(x, y); // don't go back to previous position
            }
        };
        AStarPathUtil.Stepper stepper = new AStarPathUtil.Stepper() {
            public void considerSteps (int x, int y) {
                Track[] adj = bangobj.getTracks().get(
                    coord(x, y)).getAdjacent(bangobj);
                for (int ii = 0; ii < adj.length; ii++) {
                    considerStep(adj[ii].x, adj[ii].y, 1);
                }
            }
        };
        List<Point> path = AStarPathUtil.getPath(tpred, stepper, this,
            bangobj.getTracks().size(), nextX, nextY, dest.x, dest.y, false);
        if (path != null) {
            path.remove(0); // the first element is redundant
        }
        return path;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !(lapper.isAirborne() || lapper instanceof Track || lapper instanceof Marker ||
            lapper instanceof Bonus || lapper instanceof Prop);
    }

    @Override // documentation inherited
    public Effect[] maybeInteract (BangObject bangobj, Piece other)
    {
        // don't punt bonuses here; we'll do it in the TrainDelegate
        return NO_EFFECTS;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TrainSprite(type);
    }

    @Override // documentation inherited
    public boolean position (int nx, int ny)
    {
        // updates the next position to be occupied by this train, recording
        // the current position as the last position...
        lastX = x;
        lastY = y;

        // ...moving the train into the previously stored next position...
        boolean changed = super.position(nextX, nextY);

        // ...and storing the new next position
        nextX = (short)nx;
        nextY = (short)ny;

        return changed;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return super.toString() + " lx:" + lastX + " ly:" + lastY + " nx:" +
            nextX + " ny:" + nextY + " p:" + path + " t:" + type;
    }
}
