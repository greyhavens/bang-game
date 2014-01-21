//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.List;

import java.awt.Rectangle;

import com.google.common.collect.Lists;
import com.samskivert.util.IntIntMap;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Wendigo;

import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Represents the effect of a Wendigo decimating the board.
 */
public class WendigoEffect extends Effect
    implements PieceCodes
{
    /** The speed of the wendigo in tiles per second. */
    public static final float WENDIGO_SPEED = 6f;

    /** The identifier for the type of effect that we produce. */
    public static final String EATEN = "bang";

    /** The effect reported for units protected by a safe spot. */
    public static final String SAFE_PROTECT = "indian_post/safe_spot/protect";

    /** The effect reported for units protected by a talisman. */
    public static final String TALISMAN_PROTECT =
        "indian_post/talisman/protect";

    /** An array of wendigo movements. */
    public Movement[] moves;

    /** The list of collisions between the wendigo and units. */
    public Collision[] collisions;

    /** Set of safe spots where units are not affected. */
    public transient PointSet safeSpots;

    public static class Movement extends SimpleStreamableObject
    {
        public int nx, ny;
        public int pieceId;
    }

    public static class Collision extends SimpleStreamableObject
    {
        /** The timestep at which the collision occurred. */
        public int step;

        /** The id of the unit hit. */
        public int targetId;

        /** Whether or not the unit was on a safe spot. */
        public boolean safe;

        /** Whether or not the unit was holding a talisman. */
        public boolean talisman;

        /** The unit's death effect, if it died. */
        public Effect deathEffect;

        public Collision ()
        {
        }

        public Collision (
            int step, int targetId, boolean safe, boolean talisman,
            Effect deathEffect)
        {
            this.step = step;
            this.targetId = targetId;
            this.safe = safe;
            this.talisman = talisman;
            this.deathEffect = deathEffect;
        }

        public boolean isKill ()
        {
            return !(safe || talisman);
        }
    }

    public static WendigoEffect wendigoAttack (BangObject bangobj, List<Wendigo> wendigo)
    {
        WendigoEffect effect = new WendigoEffect();
        effect.moves = new Movement[wendigo.size()];
        Rectangle playarea = bangobj.board.getPlayableArea();
        for (int ii = 0; ii < effect.moves.length; ii++) {
            Wendigo w = wendigo.get(ii);
            effect.moves[ii] = new Movement();
            effect.moves[ii].pieceId = w.pieceId;
            switch (w.orientation) {
              case NORTH:
                effect.moves[ii].nx = w.x;
                effect.moves[ii].ny = playarea.y - 4;
                break;
              case SOUTH:
                effect.moves[ii].nx = w.x;
                effect.moves[ii].ny = playarea.y + playarea.height + 2;
                break;
              case EAST:
                effect.moves[ii].ny = w.y;
                effect.moves[ii].nx = playarea.x + playarea.width + 2;
                break;
              case WEST:
                effect.moves[ii].ny = w.y;
                effect.moves[ii].nx = playarea.x - 4;
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
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle[] { bangobj.board.getPlayableArea() };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece w = bangobj.pieces.get(moves[0].pieceId);
        createCollisions(bangobj, dammap, w);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        int pathlength = 0;
        for (Movement m : moves) {
            Piece piece = bangobj.pieces.get(m.pieceId);
            if (piece == null) {
                log.warning("Missing target for wendigo effect", "id", m.pieceId);
                return false;
            }
            bangobj.board.clearShadow(piece);
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
            if (collision.isKill()) {
                if (collision.deathEffect != null) {
                    collision.deathEffect.apply(bangobj, obs);
                }
                damage(bangobj, obs, -1, null, target, 100, EATEN);
            } else {
                if (collision.safe) {
                    reportEffect(obs, target, SAFE_PROTECT);
                }
                if (collision.talisman) {
                    reportEffect(obs, target, TALISMAN_PROTECT);
                }
            }
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        return new WendigoHandler();
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return 100;
    }

    /**
     * Create the list of units that the wendigo eats.
     */
    protected void createCollisions (
        BangObject bangobj, IntIntMap dammap, Piece w)
    {
        List<Collision> colList = Lists.newArrayList();
        boolean horiz = (w.orientation == EAST ||
                w.orientation == WEST);
        int step = (horiz ? w.x : w.y);
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive()) {
                Unit unit = (Unit)p.clone();
                int dist = Math.abs(step - (horiz ? unit.x : unit.y));
                boolean safe = (safeSpots != null &&
                                safeSpots.contains(unit.x, unit.y));
                boolean talisman =
                    TalismanEffect.TALISMAN_BONUS.equals(unit.holding);
                Effect deffect = null;
                if (!(safe || talisman)) {
                    dammap.increment(unit.owner, 100 - unit.damage);
                    deffect = unit.willDie(bangobj, -1);
                    if (deffect != null) {
                        deffect.prepare(bangobj, dammap);
                    }
                    unit.damage = 100;
                }
                colList.add(new Collision(
                            dist, unit.pieceId, safe, talisman, deffect));
            }
        }
        collisions = colList.toArray(new Collision[colList.size()]);
    }
}
