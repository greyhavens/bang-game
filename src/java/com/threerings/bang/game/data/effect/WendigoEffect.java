//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import java.awt.Point;
import java.awt.Rectangle;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.StampedeEffect.Collision;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Wendigo;

import com.threerings.bang.game.util.PointList;
import com.threerings.bang.game.util.PointSet;

import com.threerings.io.SimpleStreamableObject;

import static com.threerings.bang.Log.log;

/**
 * Represents the effect of a Wendigo decimating a second of the board.
 */
public class WendigoEffect extends Effect
    implements PieceCodes
{
    /** The speed of the wendigo in tiles per second. */
    public static final float WENDIGO_SPEED = 4f;

    /** The identifier for the type of effect that we produce. */
    public static final String EATEN = "bang";

    /** An array of wendigo movements. */
    public Movement[] moves;

    /** The list of collisions between the wendigo and units. */
    public Collision[] collisions;

    /** Set of safe points where units are not affected. */
    public transient PointSet safePoints;

    public static class Movement extends SimpleStreamableObject
    {
        public int nx, ny;
        public int pieceId;
        public PointList path;
    }

    public static WendigoEffect wendigosAttack (
            BangObject bangobj, ArrayList<Wendigo> wendigos)
    {
        WendigoEffect effect = new WendigoEffect();
        effect.moves = new Movement[wendigos.size()];
        Rectangle playarea = bangobj.board.getPlayableArea();
        for (int ii = 0; ii < effect.moves.length; ii++) {
            Wendigo w = wendigos.get(ii);
            effect.moves[ii] = new Movement();
            effect.moves[ii].pieceId = w.pieceId;
            switch (w.orientation) {
              case NORTH:
                effect.moves[ii].nx = w.x;
                effect.moves[ii].ny = playarea.y - 2;
                break;
              case SOUTH:
                effect.moves[ii].nx = w.x;
                effect.moves[ii].ny = playarea.y + playarea.height;
                break;
              case EAST:
                effect.moves[ii].ny = w.y;
                effect.moves[ii].nx = playarea.x + playarea.width;
                break;
              case WEST:
                effect.moves[ii].ny = w.y;
                effect.moves[ii].nx = playarea.x - 2;
                break;
            }
        }
        return effect;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] pieceIds = new int[collisions.length + moves.length];
        for (int ii = 0; ii < moves.length; ii++) {
            pieceIds[ii] = moves[ii].pieceId;
        }
        for (int ii = 0; ii < collisions.length; ii++) {
            pieceIds[moves.length + ii] = collisions[ii].targetId;
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
    public Rectangle getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        _colMap = new HashIntMap<Collision>();
        for (Movement m : moves) {
            Piece w = bangobj.pieces.get(m.pieceId);
            if (w == null) { 
                continue;
            }

            createPath(bangobj, w, m);

            createCollisions(bangobj, dammap, w);
        }
        collisions = _colMap.values().toArray(new Collision[0]);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        int pathlength = 0;
        for (Movement m : moves) {
            Piece piece = bangobj.pieces.get(m.pieceId);
            if (piece == null) {
                log.warning("Missing target for wendigo effect [id=" + 
                        m.pieceId + "].");
                return false;
            }
            bangobj.board.clearShadow(piece);
            piece.position(m.nx, m.ny);
            bangobj.removePieceDirect(piece);

            // delay the tick by the amount of time it takes for the wendigo
            // to run it's course
            pathlength = Math.max(pathlength, 
                    Math.abs((piece.orientation == EAST ||
                    piece.orientation == WEST) ?
                        piece.x - m.nx : piece.y - m.ny));
        }
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
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        _bounds = (Rectangle)bangobj.board.getPlayableArea().clone();
        return new WendigoHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return 100;
    }
    
    /**
     * Create the path the wendigo takes.
     */
    protected void createPath (BangObject bangobj, Piece w, Movement m)
    {
        m.path = new PointList();
        if (w.x != m.nx) {
            int step = (m.nx > w.x) ? 1 : -1;
            for (int xx = w.x; xx != m.nx + step; xx += step) {
                m.path.add(new Point(xx, m.ny));
            }
        } else {
            int step = (m.ny > w.y) ? 1 : -1;
            for (int yy = w.y; yy != m.ny + step; yy += step) {
                m.path.add(new Point(m.nx, yy));
            }
        }
    }

    /**
     * Create the list of units that the wendigo eats.
     */
    protected void createCollisions (
        BangObject bangobj, IntIntMap dammap, Piece w)
    {
        boolean horiz = (w.orientation == EAST ||
                w.orientation == WEST);
        int idx = (horiz ? w.y : w.x);
        int step = (horiz ? w.x : w.y);
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive() && 
                    (safePoints == null || !safePoints.contains(p.x, p.y)) &&
                    !TalismanEffect.TALISMAN_BONUS.equals(((Unit)p).holding)) {
                int pidx = (horiz ? p.y : p.x);
                if (idx == pidx || idx + 1 == pidx) {
                    Unit unit = (Unit)p.clone();
                    int dist = Math.abs(step - (horiz ? unit.x : unit.y));
                    Collision col = _colMap.get(unit.pieceId);
                    if (col != null && col.step <= dist) {
                        continue;
                    }
                    dammap.increment(unit.owner, 100 - unit.damage);
                    Effect deffect = unit.willDie(bangobj, -1);
                    if (deffect != null) {
                        deffect.prepare(bangobj, dammap);
                    }
                    unit.damage = 100;
                    col = new Collision(
                            dist, unit.pieceId, unit.x, unit.y, deffect);
                    _colMap.put(unit.pieceId, col);
                }
            }
        }
    }
    
    /** Mapping of target piece Ids to collision records. */
    protected transient HashIntMap<Collision> _colMap;

    /** The bounds of the effect. */
    protected transient Rectangle _bounds;
}
