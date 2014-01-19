//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.ArrayList;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * An effect that damages units in the paht of a rockslide.
 */
public class RockslideEffect extends Effect
    implements PieceCodes
{
    /** The index of the player causing the damage or -1. */
    public int causer = -1;

    /** The location on the board the rockslide starts. */
    public short x, y, dx, dy;

    /** The pieces affected. */
    public int[] pieces;

    /** The updated damage values. */
    public int[] newDamage;

    /** Death effects corresponding to each piece (<code>null</code> for pieces
     * that didn't die or didn't produce a death effect). */
    public Effect[] deathEffects;

    public RockslideEffect ()
    {
    }

    public RockslideEffect (int x, int y, int causer)
    {
        this.x = (short)x;
        this.y = (short)y;
        this.causer = causer;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        int[] apieces = pieces;
        for (Effect effect : deathEffects) {
            if (effect != null) {
                apieces = concatenate(apieces, effect.getAffectedPieces());
            }
        }
        return apieces;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        int[] wpieces = NO_PIECES;
        for (Effect effect : deathEffects) {
            if (effect != null) {
                wpieces = concatenate(wpieces, effect.getWaitPieces());
            }
        }
        return wpieces;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        ArrayList<Piece> affected = new ArrayList<Piece>();
        int dir = bangobj.board.getTerrainSlope(x, y);
        if (dir == -1) {
            return;
        }
        Rectangle playarea = bangobj.board.getPlayableArea();
        PointSet slidePts = new PointSet();
        int tx = x, ty = y;
        for (int ii = 0; ii <= SLIDE_DISTANCE; ii++) {
            log.info("Adding tile to rockslide", "x", tx, "y", ty);
            slidePts.add(tx, ty);
            tx += DX[dir];
            ty += DY[dir];
            if (!playarea.contains(tx, ty)) {
                return;
            }
        }
        dx = (short)tx;
        dy = (short)ty;
        for (Piece p : bangobj.pieces) {
            if (p.isTargetable() && slidePts.contains(p.x, p.y) &&
                    !p.isAirborne()) {
                affected.add(p);
            }
        }
        if (affected.size() == 0) {
            return;
        }
        pieces = new int[affected.size()];
        newDamage = new int[pieces.length];
        deathEffects = new Effect[pieces.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = affected.get(ii);
            pieces[ii] = p.pieceId;
            int damage = Math.min(100 - p.damage, SLIDE_DAMAGE);
            newDamage[ii] = p.damage + damage;
            dammap.increment(p.owner, damage);
            if (newDamage[ii] == 100) {
                deathEffects[ii] = p.willDie(bangobj, -1);
                if (deathEffects[ii] != null) {
                    deathEffects[ii].prepare(bangobj, dammap);
                }
            }
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (pieces != null);
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle[] {
            new Rectangle(Math.min(dx, x), Math.min(dy, y), 
                Math.max(1, Math.abs(dx - x)), 
                Math.max(1, Math.abs(dy - y))) };
    }

    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return SLIDE_DAMAGE;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (pidx == -1) {
            return null;
        }
        String names = getPieceNames(bangobj, pidx, pieces);
        return (names == null) ?
            null : MessageBundle.compose("m.effect_rockslide", names);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        boolean success = true;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = bangobj.pieces.get(pieces[ii]);
            if (target == null) {
                log.warning("Missing piece for rockslide effect", "pid", pieces[ii],
                            "effect", this);
                success = false;
                continue;
            }
            if (deathEffects[ii] != null) {
                deathEffects[ii].apply(bangobj, obs);
            }
            damage(bangobj, obs, causer, null, target, newDamage[ii],
                    ShotEffect.DAMAGED);
        }
        return success;
    }

    protected static final int SLIDE_DAMAGE = 30;
    protected static final int SLIDE_DISTANCE = 3;
}
