//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.RandomUtil;
import com.threerings.util.StreamableArrayList;
import com.threerings.util.StreamablePoint;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.StampedeHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
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
    public StreamableArrayList[] paths;
    
    /** The list of collisions between buffalo and units. */
    public StreamableArrayList collisions;
    
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
        
        // try the directions in sequence until we find one that lets the
        // buffalo across the board (if there isn't one, use the one that
        // gets them the furthest)
        float maxRank = Float.MIN_VALUE;
        StreamableArrayList[] maxPaths = null;
        for (int i = 0; i < dirs.length; i++) {
            StreamableArrayList[] paths = new StreamableArrayList[NUM_BUFFALO];
            float rank = createPaths(bangobj.board, dirs[i], paths);
            if (rank > maxRank) {
                maxRank = rank;
                maxPaths = paths;
                if (rank >= 1f) {
                    break;
                }
            }
        }
        paths = maxPaths;
        
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
        for (int i = 0, size = collisions.size(); i < size; i++) {
            Collision collision = (Collision)collisions.get(i);
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
    protected void shuffleCopy (int[] src, int srcidx, int[] dest, int destidx,
        int length)
    {
        System.arraycopy(src, srcidx, dest, destidx, length);
        ArrayUtil.shuffle(dest, destidx, length);
    }
    
    /**
     * Creates a set of paths for buffalo traversing the board in the specified
     * direction.
     *
     * @param paths the array of paths to populate
     * @return the maximum path rank for the paths created, indicating how far
     * across the board the paths reach
     */
    protected float createPaths (BangBoard board, int dir,
        StreamableArrayList[] paths)
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
        StreamablePoint[] locs = new StreamablePoint[paths.length];
        int[] dirs = new int[paths.length];
        boolean[] reversed = new boolean[paths.length];
        int extent = radius + 1;
        for (int i = 0; i < locs.length; i++) {
            StreamablePoint loc;
            do {
                loc = new StreamablePoint(
                    ox + RandomUtil.getInt(+extent, -extent),
                    oy + RandomUtil.getInt(+extent, -extent));
            
            } while (ListUtil.contains(locs, loc));
            locs[i] = loc;
            paths[i] = new StreamableArrayList();
            paths[i].add(loc);
            dirs[i] = dir;
        }
        
        // step through until all buffalo have left the board or we've reached
        // the maximum path length
        int maxStep = (int)(MAX_STEP_FACTOR * ((dir == NORTH || dir == SOUTH) ?
            board.getHeight() : board.getWidth()));
        float maxRank = 0f;
        for (int i = 0; i < maxStep; i++) {
            maxRank = Math.max(maxRank, stepStampede(board, dir, paths, locs,
                dirs, reversed));
            if (ListUtil.size(locs) == 0) {
                break;
            }
        }
        return maxRank;
    }
    
    /**
     * Executes a single step of the stampede.
     *
     * @param locs the current locations of each buffalo
     * @param dirs the temporary directions of each buffalo
     * @param reversed whether each buffalo is moving in reverse with respect
     * to the stampede direction
     * @param dirs the temporary directions of each buffalo
     * @return the maximum rank reached on this step
     */
    protected float stepStampede (BangBoard board, int dir,
        StreamableArrayList[] paths, StreamablePoint[] locs, int[] dirs,
        boolean[] reversed)
    {
        StreamablePoint[] nlocs = new StreamablePoint[paths.length];
        float maxRank = Float.MIN_VALUE;
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
                StreamablePoint nloc = new StreamablePoint(
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
            
            // update the direction and max rank, add the new location to the
            // path, and end if we've reached the edge of the board
            dirs[i] = ndir;
            paths[i].add(nlocs[i]);
            maxRank = Math.max(maxRank, getRank(board, dir, nlocs[i]));
            float frank = getRank(board, fdir, nlocs[i]);
            if (frank >= 1f) {
                nlocs[i] = null;
            }
        }
        System.arraycopy(nlocs, 0, locs, 0, paths.length);
        return maxRank;
    }
    
    /**
     * Ranks the specified point with respect to the stampede direction.
     * Higher ranks are further across the board, with 1 as the ultimate
     * goal (the opposite edge).
     */
    protected float getRank (BangBoard board, int dir, Point loc)
    {
        if (dir == NORTH || dir == SOUTH) {
            float base = loc.y / (board.getHeight() - 1f);
            return (dir == SOUTH ? base : 1f - base);
            
        } else {
            float base = loc.x / (board.getWidth() - 1f);
            return (dir == EAST ? base : 1f - base);      
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
        for (int i = 0; i < paths.length; i++) {
            maxlen = Math.max(maxlen, paths[i].size());
        }
        
        // step through the paths, updating units and generating collisions
        collisions = new StreamableArrayList();
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
                    collisions.add(new Collision(i, unit.pieceId, nloc.x,
                        nloc.y));
                    bangobj.board.updateShadow(unit, null);
                    unit.position(nloc.x, nloc.y);
                    bangobj.board.updateShadow(null, unit);
                    dammap.increment(unit.owner, COLLISION_DAMAGE);
                }
            }
        }
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
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].size() > step && paths[i].get(step).equals(loc)) {
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
