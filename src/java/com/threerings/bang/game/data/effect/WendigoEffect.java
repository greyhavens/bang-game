//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import java.awt.Point;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.StampedeEffect.Collision;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointList;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Represents the effect of a Wendigo decimating a second of the board.
 */
public class WendigoEffect extends Effect
{
    /** The speed of the wendigo in tiles per second. */
    public static final float WENDIGO_SPEED = 4f;

    /** The identifier for the type of effect that we produce. */
    public static final String EATEN = "bang";

    /** The identifier for the wendigo piece. */
    public int pieceId;

    /** The destination coordinates. */
    public int nx, ny;

    /** The path to be followed by the wendigo. */
    public PointList path;

    /** The list of collisions between the wendigo and units. */
    public Collision[] collisions;

    /** Set of safe points where units are not affected. */
    public transient PointSet safePoints;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieceIds = new int[collisions.length + 1];
        pieceIds[0] = pieceId;
        for (int ii = 0; ii < collisions.length; ii++) {
            pieceIds[1 + ii] = collisions[ii].targetId;
            if (collisions[ii].deathEffect != null) {
                pieceIds = concatenate(pieceIds,
                        collisions[ii].deathEffect.getAffectedPieces());
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
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece w = bangobj.pieces.get(pieceId);
        if (w == null) { 
            return;
        }

        createPath(bangobj, w);

        createCollisions(bangobj, dammap, w);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for wendigo effect [id=" + pieceId + 
                    "].");
            return false;
        }

        // delay the tick by the amount of time it takes for the wendigo
        // to run it's course
        int pathlength = Math.abs((piece.orientation == PieceCodes.EAST ||
                piece.orientation == PieceCodes.WEST) ?
                    piece.x - nx : piece.y - ny);
        reportDelay(obs, (long)(pathlength * 1000 / WENDIGO_SPEED));

        // apply the collisions in order
        for (Collision collision : collisions) {
            Piece target = bangobj.pieces.get(collision.targetId);
            if (collision.deathEffect != null) {
                collision.deathEffect.apply(bangobj, obs);
            }
            if (target != null) {
                damage(bangobj, obs, -1, target, 100, EATEN);
            }
        }
        bangobj.board.clearShadow(piece);
        piece.position(nx, ny);
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new WendigoHandler();
    }

    /**
     * Create the path the wendigo takes.
     */
    protected void createPath (BangObject bangobj, Piece w)
    {
        path = new PointList();
        if (w.x != nx) {
            int step = (nx > w.x) ? 1 : -1;
            for (int xx = w.x; xx != nx + step; xx += step) {
                path.add(new Point(xx, ny));
            }
        } else {
            int step = (ny > w.y) ? 1 : -1;
            for (int yy = w.y; yy != ny + step; yy += step) {
                path.add(new Point(nx, yy));
            }
        }
    }

    /**
     * Create the list of units that the wendigo eats.
     */
    protected void createCollisions (
        BangObject bangobj, IntIntMap dammap, Piece w)
    {
        boolean horiz = (w.orientation == PieceCodes.EAST ||
                w.orientation == PieceCodes.WEST);
        int idx = (horiz ? w.y : w.x);
        int step = (horiz ? w.x : w.y);
        ArrayList<Collision> cols = new ArrayList<Collision>();
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive() && 
                    (safePoints == null || !safePoints.contains(p.x, p.y)) &&
                    !TalismanEffect.TALISMAN_BONUS.equals(((Unit)p).holding)) {
                int pidx = (horiz ? p.y : p.x);
                if (idx == pidx || idx + 1 == pidx) {
                    Unit unit = (Unit)p.clone();
                    dammap.increment(unit.owner, 100 - unit.damage);
                    Effect deffect = unit.willDie(bangobj, -1);
                    if (deffect != null) {
                        deffect.prepare(bangobj, dammap);
                    }
                    unit.damage = 100;
                    cols.add(new Collision(
                                Math.abs(step - (horiz ? unit.x : unit.y)),
                                unit.pieceId, unit.x, unit.y, deffect));
                }
            }
        }
        collisions = cols.toArray(new Collision[cols.size()]);
    }
}
