//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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
        
        // choose the direction such that the fewest number of the causer's
        // units are in the way
        int[] dirs;
        if (xstripe > ystripe) {
            dirs = Y_DIRECTIONS;
            
        } else if (ystripe > xstripe) {
            dirs = X_DIRECTIONS;
            
        } else {
            dirs = DIRECTIONS;
        }
        int dir = dirs[RandomUtil.getInt(dirs.length)];
        
        // determine the approximate point of origin
        int ox, oy;
        if (dir == NORTH || dir == SOUTH) {
            ox = x;
            oy = (dir == NORTH) ? bangobj.board.getHeight() - 1 : 0;
            
        } else {
            ox = (dir == WEST) ? bangobj.board.getWidth() - 1 : 0;
            oy = y;
        }
        
        // scatter the initial locations about the origin
        StreamablePoint[] locs = new StreamablePoint[NUM_BUFFALO];
        int extent = radius + 1;
        for (int i = 0; i < locs.length; i++) {
            StreamablePoint loc;
            do {
                loc = new StreamablePoint(
                    ox + RandomUtil.getInt(+extent, -extent),
                    oy + RandomUtil.getInt(+extent, -extent));
            
            } while (ListUtil.contains(locs, loc));
            locs[i] = loc;
        }
        
        preparePaths(bangobj.board, dir, locs);
        prepareCollisions(bangobj, dammap);
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
     * Initializes the paths for the buffalo.
     */
    protected void preparePaths (BangBoard board, int dir,
        StreamablePoint[] locs)
    {
        // run the search algorithm for each buffalo; if it doesn't find a path
        // across the board, we have to append a backwards path
        paths = new StreamableArrayList[NUM_BUFFALO];
        int maxStep = (int)(MAX_STEP_FACTOR * 
            ((dir == NORTH || dir == SOUTH) ?
                board.getHeight() : board.getWidth()));
        for (int i = 0; i < locs.length; i++) {
            paths[i] = findFurthestPath(board, dir, locs[i],
                new HashSet<Point>(), 0, maxStep);
            if (rankPath(board, dir, paths[i]) < 1f) {
                int oppdir = (dir + 2) % DIRECTIONS.length;
                Point eloc = (Point)paths[i].get(paths[i].size() - 1);
                StreamableArrayList bpath = findFurthestPath(board, oppdir,
                    eloc, new HashSet<Point>(), paths[i].size() - 1,
                    maxStep * 2);
                paths[i].addAll(bpath.subList(1, bpath.size()));
            }
        }
    }

    /**
     * Uses a simple best-first search to find the path that gets the furthest
     * across the board.
     *
     * @param dir the direction of the stampede
     * @param loc the current location of the buffalo
     * @param visited the set of nodes visited so far
     * @param step the current time step
     * @param maxStep the maximum time step
     */
    protected StreamableArrayList findFurthestPath (BangBoard board, int dir,
        Point loc, HashSet<Point> visited, int step, int maxStep)
    {
        // if we've reached the edge or the maximum number of steps,
        // stop searching
        StreamableArrayList path = new StreamableArrayList();
        path.add(loc);
        float rank = rankPath(board, dir, path);
        if (rank == 1f || step == maxStep) {
            return path;
        }
        
        // fwd, left, right, back (swap left and right randomly)
        boolean swap = (RandomUtil.getInt(2) == 1);
        int[] dirs = new int[] { dir,
            (dir + (swap ? 3 : 1)) % DIRECTIONS.length,
            (dir + (swap ? 1 : 3)) % DIRECTIONS.length,
            (dir + 2) % DIRECTIONS.length };

        // find the path leading to the highest ranking point (including the
        // empty path in the search), breaking out if we get across the board
        float highestRank = rank;
        StreamableArrayList highestPath = new StreamableArrayList();
        visited.add(loc);
        int nstep = step + 1;
        for (int i = 0; i < dirs.length; i++) {
            Point nloc = new StreamablePoint(loc.x + DX[dirs[i]],
                loc.y + DY[dirs[i]]);
            if (board.isGroundOccupiable(nloc.x, nloc.y) &&
                !visited.contains(nloc) && !containsBuffalo(nloc, nstep)) {
                StreamableArrayList npath = findFurthestPath(board, dir, nloc,
                    visited, nstep, maxStep);
                float nrank = rankPath(board, dir, npath);
                if (nrank > highestRank) {
                    highestRank = nrank;
                    highestPath = npath;
                    if (highestRank == 1f) {
                        break;
                    }
                }
            }
        }
        visited.remove(loc);
        
        path.addAll(highestPath);
        return path;
    }
    
    /**
     * Ranks the specified path with respect to the stampede direction.
     * Higher ranks are further across the board, with 1 as the ultimate
     * goal (the opposite edge).
     */
    protected float rankPath (BangBoard board, int dir, ArrayList path)
    {
        Point loc = (Point)path.get(path.size() - 1);
        if (dir == NORTH || dir == SOUTH) {
            float base = loc.y / (board.getHeight() - 1f);
            return (dir == SOUTH ? base : 1f - base);
            
        } else {
            float base = loc.x / (board.getWidth() - 1f);
            return (dir == EAST ? base : 1f - base);      
        }
    }
    
    /**
     * Checks whether a buffalo whose path has already been computed occupies
     * the specified location at the given time step.
     */
    protected boolean containsBuffalo (Point loc, int step)
    {
        if (step < 0) {
            return false;
        }
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null && paths[i].size() > step &&
                paths[i].get(step).equals(loc)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Initializes the collisions for the buffalo.
     */
    protected void prepareCollisions (BangObject bangobj, IntIntMap dammap)
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
    
    /** The number of buffalo in the stampede. */
    protected static final int NUM_BUFFALO = 3;
    
    /** Multiplied by the board dimension to determine the maximum number of
     * steps the buffalo can take in traversing the board. */
    protected static final float MAX_STEP_FACTOR = 2f;
}
