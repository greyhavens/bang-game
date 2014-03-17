//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Collections;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.RandomUtil;

import com.threerings.util.MessageBundle;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.StampedeHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.util.PointList;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.*;

/**
 * Represents the effect of a stampede running over the board.
 */
public class StampedeEffect extends Effect
    implements PieceCodes
{
    /** The speed of the bison in tiles per second. */
    public static final float BISON_SPEED = 4f;
        
    /** The amount of damage taken by units hit by bison. */
    public static final int COLLISION_DAMAGE = 50;
    
    /**
     * Represents a bison's collision with a unit.
     */
    public static class Collision extends SimpleStreamableObject
    {
        /** The timestep at which the collision occurred. */
        public int step;

        /** The id of the unit hit. */
        public int targetId;

        /** The coordinates to which the unit was pushed. */
        public short x, y;

        /** The unit's death effect, if it died. */
        public Effect deathEffect;
        
        public Collision ()
        {
        }

        public Collision (
            int step, int targetId, int x, int y, Effect deathEffect)
        {
            this.step = step;
            this.targetId = targetId;
            this.x = (short)x;
            this.y = (short)y;
            this.deathEffect = deathEffect;
        }
    }

    /** The id of the player causing the damage or -1. */
    public transient int causer;

    /** The location selected. */
    public transient int x, y;

    /** The radius of the effect. */
    public transient int radius;

    /** The path to be followed by the bison. */
    public PointList path;

    /** The list of collisions between bison and units. */
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
        for (Collision collision : collisions) {
            if (collision.deathEffect != null) {
                pieceIds = concatenate(pieceIds,
                    collision.deathEffect.getAffectedPieces());
            }
        }
        return pieceIds;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] wpieces = NO_PIECES;
        for (Collision collision : collisions) {
            if (collision.deathEffect != null) {
                wpieces = concatenate(wpieces,
                    collision.deathEffect.getWaitPieces());
            }
        }
        return wpieces;
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        Rectangle rect = null;
        for (Point p : path) {
            if (rect == null) {
                rect = new Rectangle(p);
            } else {
                rect.add(p);
            }
        }
        if (rect != null) {
            rect.width++;
            rect.height++;
        }
        return new Rectangle[] { rect };
    }
    
    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // create the path that the bison will follow
        createPath(bangobj.board);

        // create the list of collisions
        createCollisions(bangobj, dammap);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (collisions.length > 0);
    }
    
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // delay the tick by the amount of time it takes for the bison to run
        // their course
        reportDelay(obs, (long)((path.size()-1) * 1000 / BISON_SPEED));

        // apply the collisions in order
        for (Collision collision : collisions) {
            if (collision.deathEffect != null) {
                collision.deathEffect.apply(bangobj, obs);
            }
            collide(bangobj, obs, causer, -1, collision.targetId,
                COLLISION_DAMAGE, collision.x, collision.y,
                ShotEffect.DAMAGED);
        }

        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new StampedeHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return COLLISION_DAMAGE;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx == -1) {
            return null;
        }
        int[] pieceIds = new int[collisions.length];
        for (int ii = 0; ii < pieceIds.length; ii++) {
            pieceIds[ii] = collisions[ii].targetId;
        }
        String names = getPieceNames(bangobj, pidx, pieceIds);
        return (names == null) ?
            null : MessageBundle.compose("m.effect_stampede", names);
    }
    
    /**
     * Creates a path for the bison.
     */
    protected void createPath (BangBoard board)
    {
        // starting at the target position, grow the path from both ends until
        // we're blocked or we've reached the maximum length
        path = new PointList();
        Point start = new Point(x, y);
        path.add(start);
        int hdir = growPath(board, path, -1);
        if (path.size() == 1) {
            log.warning("Couldn't find anywhere for the bison to go!", "effect", this);
            return;
        }
        Collections.reverse(path);
        PointList rpath = new PointList();
        rpath.add(start);
        if (growPath(board, rpath, (hdir + 2) % 4) != -1) {
            path.addAll(rpath.subList(1, rpath.size()));
        }
    }
    
    /**
     * Extends the path by one tile.
     *
     * @param path the path so far
     * @param dir the preferred direction, or -1 for none
     * @return the direction taken, or -1 for none
     */
    protected int growPath (BangBoard board, PointList path, int dir)
    {
        Point last = path.get(path.size() - 1);
        int[] dirs;
        if (dir != -1) {
            int rot = (RandomUtil.getInt(2) == 0) ? 1 : 3;
            dirs = new int[] { dir, (dir + rot) % 4, (dir + rot + 2) % 4,
                (dir + 2) % 4 };
        } else {
            dirs = DIRECTIONS.clone();
            ArrayUtil.shuffle(dirs);
        }
        PointList bpath = null;
        int bdir = -1, marker = path.size();
        for (int ii = 0; ii < dirs.length; ii++) {
            Point next = new Point(last.x + DX[dirs[ii]],
                last.y + DY[dirs[ii]]);
            if (!board.isGroundOccupiable(next.x, next.y, true) ||
                path.contains(next)) {
                continue;
            }
            path.add(next);
            if (path.size() == STAMPEDE_LENGTH + 1) {
                return dirs[ii];
            }
            growPath(board, path, dirs[ii]);
            if (path.size() == STAMPEDE_LENGTH + 1) {
                return dirs[ii];
            }
            if (bpath == null || (path.size() - marker) > bpath.size()) {
                bpath = new PointList();
                bpath.addAll(path.subList(marker, path.size()));
                bdir = dirs[ii];
            }
            path.subList(marker, path.size()).clear();
        }
        if (bpath != null) {
            path.addAll(bpath);
        }
        return bdir;
    }
    
    /**
     * Creates the collision list for the bison.
     */
    protected void createCollisions (BangObject bangobj, IntIntMap dammap)
    {
        // clone all the non-flying units
        ArrayList<Piece> units = new ArrayList<Piece>();
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Unit && !piece.isAirborne()) {
                units.add((Piece)piece.clone());
            }
        }

        // step through the path, updating units and generating collisions
        ArrayList<Collision> cols = new ArrayList<Collision>();
        Point loc = new Point();
        PointSet newlocs = new PointSet();
        for (int ii = 0, nn = path.size(); ii < nn; ii++) {
            for (Piece unit : units) {
                loc.setLocation(unit.x, unit.y);
                if (containsBison(loc, ii)) {
                    // try to move the unit to a point that wasn't occupied by
                    // a bison in the last step and won't be in the next step
                    ArrayList<Point> nlocs = new ArrayList<Point>();
                    for (int jj = 0; jj < DIRECTIONS.length; jj++) {
                        Point nloc = new Point(loc.x + DX[jj], loc.y + DY[jj]);
                        if (bangobj.board.isOccupiable(nloc.x, nloc.y) &&
                            !containsBison(nloc, ii - 1) &&
                            !containsBison(nloc, ii + 1) &&
                            !newlocs.contains(nloc.x, nloc.y)) {
                            nlocs.add(nloc);
                        }
                    }
                    Point nloc = (nlocs.size() > 0 ? RandomUtil.pickRandom(nlocs) : loc);
                    int damage = Math.min(100, unit.damage + COLLISION_DAMAGE);
                    dammap.increment(unit.owner, damage - unit.damage);
                    Effect deffect = null;
                    if (damage == 100 && unit.damage < 100) {
                        deffect = unit.willDie(bangobj, -1);
                        if (deffect != null) {
                            deffect.prepare(bangobj, dammap);
                        }
                    }
                    unit.damage = damage;
                    cols.add(new Collision(ii, unit.pieceId, nloc.x, nloc.y,
                        deffect));
                    newlocs.add(nloc.x, nloc.y);
                }
            }
        }
        collisions = cols.toArray(new Collision[cols.size()]);
    }

    /**
     * Checks whether the specified location contains a bison at the given
     * step along the paths.
     */
    protected boolean containsBison (Point loc, int step)
    {
        if (step < 0) {
            return false;
        }
        return path.size() > step && path.get(step).equals(loc);
    }

    /** The (maximum) length of the bison stampede in each direction. */
    protected static final int STAMPEDE_LENGTH = 4;
}
