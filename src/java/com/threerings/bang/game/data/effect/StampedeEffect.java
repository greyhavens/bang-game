//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.util.RandomUtil;
import com.threerings.util.StreamableArrayList;
import com.threerings.util.StreamablePoint;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.StampedeHandler;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

/**
 * Represents the effect of a stampede running over the board.
 */
public class StampedeEffect extends Effect
    implements PieceCodes
{
    /** The id of the player causing the damage or -1. */
    public transient int causer;
    
    /** The location selected. */
    public transient int x, y;
    
    /** The radius of the effect. */
    public transient int radius;
    
    /** The paths to be followed by each buffalo. */
    public StreamableArrayList[] paths;
    
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
        paths = new StreamableArrayList[NUM_BUFFALO];
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
        }
        
        // step through until all the buffalo have left the board
        while (step(bangobj, dammap, locs, dir) > 0);
    }
    
    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer observer)
    {
        
    }
    
    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new StampedeHandler();
    }
    
    /**
     * Executes a single step of the stampede process.
     *
     * @param locs the current locations of the buffalo, with
     * <code>null</code> representing those that have left the board
     * @param dir the direction being travelled by the buffalo
     * @return the number of buffalo remaining on the board
     */
    protected int step (BangObject bangobj, IntIntMap dammap,
        StreamablePoint[] locs, int dir)
    {
        int remaining = 0;
        for (int i = 0; i < locs.length; i++) {
            if (locs[i] == null) {
                continue;
            }
            StreamablePoint loc = new StreamablePoint(locs[i].x + DX[dir],
                locs[i].y + DY[dir]);
            paths[i].add(loc);
            if (loc.x < 0 || loc.y < 0 || loc.x >= bangobj.board.getWidth() ||
                loc.y >= bangobj.board.getHeight()) {
                locs[i] = null;
                
            } else {
                locs[i] = loc;
                remaining++;
            }
        }
        return remaining;
    }
    
    /** The number of buffalo in the stampede. */
    protected static final int NUM_BUFFALO = 3;
}
