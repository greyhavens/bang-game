//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.StampedeHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.util.PointList;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Represents the effect of a stampede running over the board.
 */
public class StampedeEffect extends Effect
    implements PieceCodes
{
    /** The speed of the buffalo in tiles per second. */
    public static final float BUFFALO_SPEED = 4f;

    /** The amount of damage taken by units hit by buffalo. */
    public static final int COLLISION_DAMAGE = 20;

    /** The identifier for the type of effect that we produce. */
    public static final String DAMAGED = "bang";

    /**
     * Represents a buffalo's collision with a unit.
     */
    public static class Collision extends SimpleStreamableObject
    {
        /** The timestep at which the collision occurred. */
        public int step;

        /** The id of the unit hit. */
        public int targetId;

        /** The coordinates to which the unit was pushed. */
        public short x, y;

        public Collision ()
        {
        }

        public Collision (int step, int targetId, int x, int y)
        {
            this.step = step;
            this.targetId = targetId;
            this.x = (short)x;
            this.y = (short)y;
        }
    }

    /** The id of the player causing the damage or -1. */
    public transient int causer;

    /** The location selected. */
    public transient int x, y;

    /** The radius of the effect. */
    public transient int radius;

    /** The paths to be followed by each buffalo. */
    public PointList[] paths;

    /** The list of collisions between buffalo and units. */
    public Collision[] collisions;

    public StampedeEffect ()
    {
    }

    public StampedeEffect (int causer, int x, int y, int radius)
    {
        this.causer = causer;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieceIds = new int[collisions.length];
        for (int ii = 0; ii < pieceIds.length; ii++) {
            pieceIds[ii] = collisions[ii].targetId;
        }
        return pieceIds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // determine how many of the causer's units are in the x and y stripes
        int xstripe = 0, ystripe = 0;
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            if (piece.owner != causer) {
                continue;
            }
            if (Math.abs(piece.x - x) <= radius*2) {
                ystripe++;
            }
            if (Math.abs(piece.y - y) <= radius*2) {
                xstripe++;
            }
        }

        // choose the order of directions to try such that the directions with
        // the fewest number of causer units in the way are first
        int[] dirs = new int[DIRECTIONS.length];
        if (xstripe > ystripe) {
            shuffleCopy(Y_DIRECTIONS, 0, dirs, 0, Y_DIRECTIONS.length);
            shuffleCopy(X_DIRECTIONS, 0, dirs, Y_DIRECTIONS.length,
                X_DIRECTIONS.length);

        } else if (ystripe > xstripe) {
            shuffleCopy(X_DIRECTIONS, 0, dirs, 0, X_DIRECTIONS.length);
            shuffleCopy(Y_DIRECTIONS, 0, dirs, X_DIRECTIONS.length,
                Y_DIRECTIONS.length);

        } else {
            shuffleCopy(DIRECTIONS, 0, dirs, 0, DIRECTIONS.length);
        }

        // try the directions in sequence and use whichever one gets the
        // buffalo closest to the target
        int minSquareDist = Integer.MAX_VALUE;
        PointList[] bestPaths = null;
        for (int ii = 0; ii < dirs.length; ii++) {
            PointList[] paths = new PointList[NUM_BUFFALO];
            int squareDist = createPaths(bangobj.board, dirs[ii], paths);
            if (squareDist < minSquareDist) {
                minSquareDist = squareDist;
                bestPaths = paths;
                if (minSquareDist <= radius * radius) {
                    break;
                }
            }
        }
        paths = bestPaths;

        // create the list of collisions
        createCollisions(bangobj, dammap);
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        // delay the tick by the amount of time it takes for the buffalo to run
        // their course
        int maxlen = 0;
        for (int i = 0; i < paths.length; i++) {
            maxlen = Math.max(maxlen, paths[i].size());
        }
        reportDelay(obs, (long)((maxlen-1) * 1000 / BUFFALO_SPEED));

        // apply the collisions in order
        for (int ii = 0; ii < collisions.length; ii++) {
            Collision collision = collisions[ii];
            collide(bangobj, obs, causer, collision.targetId, COLLISION_DAMAGE,
                    collision.x, collision.y, DAMAGED);
        }
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new StampedeHandler();
    }

    /**
     * Copies elements from the source array to the destination array in
     * shuffled order.
     */
    protected void shuffleCopy (
        int[] src, int srcidx, int[] dest, int destidx, int length)
    {
        System.arraycopy(src, srcidx, dest, destidx, length);
        ArrayUtil.shuffle(dest, destidx, length);
    }

    /**
     * Creates a set of paths for buffalo traversing the board in the specified
     * direction.
     *
     * @param paths the array of paths to populate
     * @return the minimum square distance to the stampede target reached in
     * the paths
     */
    protected int createPaths (BangBoard board, int dir, PointList[] paths)
    {
        // determine the approximate point of origin
        int ox, oy;
        if (dir == NORTH || dir == SOUTH) {
            ox = x;
            oy = (dir == NORTH) ? board.getHeight() - radius - 1 : radius;

        } else {
            ox = (dir == WEST) ? board.getWidth() - radius - 1 : radius;
            oy = y;
        }

        // scatter the initial locations about the origin
        Point[] locs = new Point[paths.length];
        int[] dirs = new int[paths.length];
        boolean[] reversed = new boolean[paths.length];
        int extent = radius + 1;
        for (int i = 0; i < locs.length; i++) {
            Point loc;
            do {
                loc = new Point(
                    ox + RandomUtil.getInt(+extent, -extent),
                    oy + RandomUtil.getInt(+extent, -extent));

            } while (ListUtil.contains(locs, loc));
            locs[i] = loc;
            paths[i] = new PointList();
            paths[i].add(loc);
            dirs[i] = dir;
        }

        // step through until all buffalo have left the board or we've reached
        // the maximum path length
        int maxStep = (int)(MAX_STEP_FACTOR * ((dir == NORTH || dir == SOUTH) ?
            board.getHeight() : board.getWidth()));
        int minSquareDist = Integer.MAX_VALUE;
        for (int ii = 0; ii < maxStep; ii++) {
            int dist = stepStampede(board, dir, paths, locs, dirs, reversed);
            minSquareDist = Math.min(minSquareDist, dist);
            if (ListUtil.size(locs) == 0) {
                break;
            }
        }
        return minSquareDist;
    }

    /**
     * Executes a single step of the stampede.
     *
     * @param locs the current locations of each buffalo
     * @param dirs the temporary directions of each buffalo
     * @param reversed whether each buffalo is moving in reverse with respect
     * to the stampede direction
     * @param dirs the temporary directions of each buffalo
     * @return the minimum square distance to the stampede target reached on
     * this step
     */
    protected int stepStampede (
        BangBoard board, int dir, PointList[] paths,
        Point[] locs, int[] dirs, boolean[] reversed)
    {
        Point[] nlocs = new Point[paths.length];
        int minSquareDist = Integer.MAX_VALUE;
        for (int i = 0; i < paths.length; i++) {
            if (locs[i] == null) {
                continue;
            }

            // arrange the list of possible directions in order of preference
            int fdir = (dir + (reversed[i] ? 2 : 0)) % DIRECTIONS.length;
            int[] ndirs;
            if (dirs[i] == fdir) {
                boolean swap = (RandomUtil.getInt(2) == 1);
                ndirs = new int[] { fdir, // fwd, left, right, back
                    (fdir + (swap ? 3 : 1)) % DIRECTIONS.length,
                    (fdir + (swap ? 1 : 3)) % DIRECTIONS.length,
                    (fdir + 2) % DIRECTIONS.length };

            } else {
                ndirs = new int[] { fdir, // fwd, same, back, other
                    dirs[i], (fdir + 2) % DIRECTIONS.length,
                    (dirs[i] + 2) % DIRECTIONS.length };
            }

            // see if we can go somewhere; if not, wait
            int ndir = dirs[i];
            nlocs[i] = locs[i];
            for (int j = 0; j < ndirs.length; j++) {
                Point nloc = new Point(
                    locs[i].x + DX[ndirs[j]], locs[i].y + DY[ndirs[j]]);
                if (board.isGroundOccupiable(nloc.x, nloc.y, true) &&
                    !ListUtil.contains(nlocs, nloc)) {
                    ndir = ndirs[j];
                    nlocs[i] = nloc;
                    break;
                }
            }

            // toggle the reverse flag if we're going backwards with respect to
            // the forward or temporary direction
            int rndir = (ndir + 2) % DIRECTIONS.length;
            if (rndir == dirs[i] || rndir == fdir) {
                reversed[i] = !reversed[i];
            }

            // update the direction and min dist, add the new location to the
            // path, and end if we've reached the edge of the board
            dirs[i] = ndir;
            paths[i].add(nlocs[i]);
            minSquareDist = Math.min(minSquareDist,
                getSquareDistanceToTarget(nlocs[i]));
            if (isOnEdge(board, fdir, nlocs[i])) {
                nlocs[i] = null;
            }
        }
        System.arraycopy(nlocs, 0, locs, 0, paths.length);
        return minSquareDist;
    }

    /**
     * Gets the squared distance from the specified point to the stampede
     * target.
     */
    protected int getSquareDistanceToTarget (Point loc)
    {
        int dx = loc.x - x, dy = loc.y - y;
        return dx*dx + dy*dy;
    }

    /**
     * Determines whether the specified point is on the edge of the board with
     * respect to the specified stampede direction.
     */
    protected boolean isOnEdge (BangBoard board, int dir, Point loc)
    {
        if (dir == NORTH) {
            return loc.y == 0;

        } else if (dir == SOUTH) {
            return loc.y == board.getHeight() - 1;

        } else if (dir == WEST) {
            return loc.x == 0;

        } else { // dir == EAST
            return loc.x == board.getWidth() - 1;
        }
    }

    /**
     * Creates the collision list for the buffalo.
     */
    protected void createCollisions (BangObject bangobj, IntIntMap dammap)
    {
        // clone all the non-flying units
        ArrayList<Piece> units = new ArrayList<Piece>();
        for (Iterator it = bangobj.pieces.iterator(); it.hasNext(); ) {
            Piece piece = (Piece)it.next();
            if (piece instanceof Unit && !piece.isFlyer()) {
                units.add((Piece)piece.clone());
            }
        }

        // find the maximum path length
        int maxlen = 0;
        for (int ii = 0; ii < paths.length; ii++) {
            maxlen = Math.max(maxlen, paths[ii].size());
        }

        // step through the paths, updating units and generating collisions
        ArrayList<Collision> cols = new ArrayList<Collision>();
        Point loc = new Point();
        for (int i = 0; i < maxlen; i++) {
            for (Piece unit : units) {
                loc.setLocation(unit.x, unit.y);
                if (containsBuffalo(loc, i)) {
                    // try to move the unit to a point that wasn't occupied by
                    // a buffalo in the last step and won't be in the next step
                    ArrayList<Point> nlocs = new ArrayList<Point>();
                    for (int j = 0; j < DIRECTIONS.length; j++) {
                        Point nloc = new Point(loc.x + DX[j], loc.y + DY[j]);
                        if (bangobj.board.canOccupy(unit, nloc.x, nloc.y) &&
                            !containsBuffalo(nloc, i - 1) &&
                            !containsBuffalo(nloc, i + 1)) {
                            nlocs.add(nloc);
                        }
                    }
                    Point nloc = (nlocs.size() > 0 ?
                        (Point)RandomUtil.pickRandom(nlocs) : loc);
                    cols.add(new Collision(i, unit.pieceId, nloc.x, nloc.y));
                    bangobj.board.updateShadow(unit, null);
                    unit.position(nloc.x, nloc.y);
                    bangobj.board.updateShadow(null, unit);
                    dammap.increment(unit.owner, COLLISION_DAMAGE);
                }
            }
        }
        collisions = cols.toArray(new Collision[cols.size()]);
    }

    /**
     * Checks whether the specified location contains a buffalo at the given
     * step along the paths.
     */
    protected boolean containsBuffalo (Point loc, int step)
    {
        if (step < 0) {
            return false;
        }
        for (int ii = 0; ii < paths.length; ii++) {
            if (paths[ii].size() > step && paths[ii].get(step).equals(loc)) {
                return true;
            }
        }
        return false;
    }

    /** The number of buffalo in the stampede. */
    protected static final int NUM_BUFFALO = 3;

    /** Multiplied by the board dimension to determine the maximum number of
     * steps the buffalo can take in traversing the board. */
    protected static final float MAX_STEP_FACTOR = 2f;
}
