//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.StatType;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.effect.TotemEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Handles the building of totem poles on totem bases.
 */
public class TotemBaseDelegate extends CounterDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        super.roundWillStart(bangobj);

        // locate our totem bases
        for (Piece p : bangobj.getPieceArray()) {
            if (p instanceof TotemBase) {
                _bases.add((TotemBase)p);
            }
        }
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        boolean validate = super.tick(bangobj, tick);

        boolean alive = false;
        for (TotemBase base : _bases) {
            alive = alive || base.canAddPiece();
        }
        if (!alive) {
            bangobj.setLastTick(tick);
        }
        _picker = null;
        return validate;
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        if (TotemEffect.PICKED_UP_TOTEM.equals(effect) ||
                TotemEffect.PICKED_UP_CROWN.equals(effect)) {
            _picker = piece;
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        if (piece instanceof TotemBase) {
            TotemBase base = (TotemBase)piece;
            int owner = base.getDestroyedOwner();
            if (owner > -1) {
                adjustCounter(bangobj, owner, -1,
                              base.getDestroyedType().stat());
                recalculatePoints(bangobj);
            }
        }
    }

    protected void adjustCounter (
        BangObject bangobj, int owner, int delta, StatType stat)
    {
        bangobj.stats[owner].incrementStat(stat, delta);
        for (Counter counter: _counters) {
            if (counter.owner == owner) {
                _bangmgr.deployEffect(
                    -1, CountEffect.changeCount(counter.pieceId,
                                                counter.count + delta));
            }
        }
    }

    @Override // documentation inherited
    protected int pointsPerCounter ()
    {
        return 0;
    }

    @Override // documentation inherited
    protected void checkAdjustedCounter (BangObject bangobj, Unit unit)
    {
        if (_bases == null || _bases.size() == 0) {
            return;
        }

        boolean justPickedUp = (_picker != null && _picker.pieceId == unit.pieceId);

        // if this unit landed next to one of the bases, do some stuff
        TotemBase base = null;
        for (TotemBase b : _bases) {
            if (b.getDistance(unit) <= 1) {
                base = b;
                break;
            }
        }
        if (base == null) {
            return;
        }

        // add a totem piece to the base if we can
        if (!justPickedUp && TotemBonus.isHolding(unit) && base.canAddPiece()) {
            TotemEffect effect = new TotemEffect();
            effect.init(unit);
            effect.type = unit.holding;
            effect.baseId = base.pieceId;
            effect.dropping = true;
            _bangmgr.deployEffect(unit.owner, effect);
            adjustCounter(bangobj, unit.owner, 1,
                          TotemBonus.TOTEM_LOOKUP.get(effect.type).stat());
            recalculatePoints(bangobj);
        }
    }

    /**
     * Recalculates the points for all the players.
     */
    protected void recalculatePoints (BangObject bangobj)
    {
        if (_points == null) {
            _points = new int[bangobj.players.length];
        }
        int[] points = new int[_points.length];
        int[] totemPoints = new int[_points.length];
        int[][] piecePerTotem =
            new int[_bases.size()][_points.length];
        int[] maxPiece = new int[piecePerTotem.length];
        int[] totemHeight = new int[piecePerTotem.length];

        bangobj.startTransaction();
        try {
            for (int tt = 0; tt < piecePerTotem.length; tt++) {
                TotemBase base = _bases.get(tt);
                for (int pp = 0, nn = base.numPieces(); pp < nn; pp++) {
                    int owner = base.getOwner(pp);
                    piecePerTotem[tt][owner]++;
                    TotemBonus.Type type = base.getType(pp);
                    totemHeight[tt] += type.height();
                    points[owner] += type.value();
                }
                for (int pieces : piecePerTotem[tt]) {
                    maxPiece[tt] = Math.max(maxPiece[tt], pieces);
                }
            }

            for (int tt = 0; tt < piecePerTotem.length; tt++) {
                for (int pp = 0; pp < piecePerTotem[tt].length; pp++) {
                    int pieces = piecePerTotem[tt][pp];

                    // bonus if they have the most pieces on this totem
                    // and the totem is not empty
                    if (pieces > 0 && pieces == maxPiece[tt]) {
                        totemPoints[pp] += MAX_PIECE_BONUS;
                    }

                    // bonus points for each piece based on the height
                    // of the totem
                    if (pieces > 0) {
                        totemPoints[pp] += totemHeight[tt] * HEIGHT_BONUS;
                    }
                }
            }

            for (int pp = 0; pp < points.length; pp++) {
                int diff = points[pp] + totemPoints[pp] - _points[pp];
                bangobj.grantPoints(pp, diff);
                _points[pp] = points[pp] + totemPoints[pp];
                bangobj.stats[pp].setStat(
                    StatType.TOTEM_POINTS, totemPoints[pp]);
            }
        } finally {
            bangobj.commitTransaction();
        }

    }

    /** A list of active totem bases. */
    protected List<TotemBase> _bases = Lists.newArrayList();

    /** Stores the old points values. */
    protected int[] _points;

    /** The last piece to pick up a totem bonus. */
    protected Piece _picker;

    /** Point bonuses. */
    protected static final int MAX_PIECE_BONUS = 50;
    protected static final int HEIGHT_BONUS = 3;
}
