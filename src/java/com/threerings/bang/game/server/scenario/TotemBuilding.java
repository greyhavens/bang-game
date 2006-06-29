//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.TotemLogic;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;

import com.threerings.bang.game.data.effect.TotemEffect;

import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.presents.server.InvocationException;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li> Players all have a totem base on the board.
 * <li> Totem pieces are generated and scattered randomly around the board.
 * <li> Any player unit can pick up a totem piece and carry it back to their
 * totem base, adding the piece to their totem pole.
 * <li> Players can attack an opponents totem, which has 100 health, and
 * destroy the top piece on the pole.  (Which resets the health to 100).
 * <li> Near the end of the scenario, top totem pieces are generated.
 * <li> The round ends after a top piece is added to a pole, or after a
 * fixed time limit.
 * <li> Points are granted for each totem piece on a players pole, with
 * duplicate pieces counted at half value.
 * </ul>
 */
public class TotemBuilding extends Scenario
    implements ScenarioCodes
{
    /**
     * Creates a totem building scenario and registers its delegates.
     */
    public TotemBuilding ()
    {
        registerDelegate(new RespawnDelegate());
        registerDelegate(new TotemBaseDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new TotemLogic();
    }

    @Override // documentation inherited
    public void filterPieces (BangObject bangobj, ArrayList<Piece> starts,
                              ArrayList<Piece> pieces)
    {
        super.filterPieces(bangobj, starts, pieces);
        
        // extract and remove all totem markers
        _totems.clear();
        for (Iterator <Piece> iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = iter.next();
            if (Marker.isMarker(p, Marker.TOTEM)) {
                _totems.add(p.x, p.y);
                iter.remove();
            }
        }
    }
    
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, ArrayList<Piece> starts,
                                PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // start with totem pieces at every totem spot
        for (int ii = 0; ii < _totems.size(); ii++) {
            dropBonus(bangobj, TotemEffect.TOTEM_MIDDLE_BONUS,
                    _totems.getX(ii), _totems.getY(ii));
        }
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // count up the totem pieces that are "in play"
        int totems = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii] instanceof TotemBonus ||
                    (pieces[ii] instanceof Unit && 
                     TotemBonus.isHolding((Unit)pieces[ii]))) {
                totems++;
            }
        }

        // if there is not at least two totems in play for every player in the
        // game, try to spawn another one
        if (totems < bangobj.getActivePlayerCount() * 2) {
            return placeBonus(bangobj, pieces, Bonus.createBonus(
                        BonusConfig.getConfig(TotemEffect.TOTEM_MIDDLE_BONUS)),
                    _totems);
        } else {
            return super.addBonus(bangobj, pieces);
        }
    }

    protected class TotemBaseDelegate extends ScenarioDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
            throws InvocationException
        {
            Piece[] pieces = bangobj.getPieceArray();
            for (int ii = 0; ii < pieces.length; ii++) {
                if (!(pieces[ii] instanceof TotemBase)) {
                    continue;
                }

                TotemBase base = (TotemBase)pieces[ii];
                _bases.add(base);
            }
        }

        @Override // documentation inherited
        public void roundDidEnd (BangObject bangobj)
        {
        }

        @Override // documentation inherited
        public void pieceMoved (BangObject bangobj, Piece piece)
        {
            if (piece instanceof Unit) {
                checkAddToBase(bangobj, (Unit)piece);
            }
        }

        @Override // documentation inherited
        public void pieceWasKilled (BangObject bangobj, Piece piece)
        {
            if (piece instanceof TotemBase) {
                recalculatePoints(bangobj);
            }
        }

        /**
         * Checks if the unit as a totem piece to add to the base.
         */
        protected void checkAddToBase (BangObject bangobj, Unit unit)
        {
            if (_bases == null || _bases.size() == 0) {
                return;
            }

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
            if (TotemBonus.isHolding(unit) && base.canAddPiece()) {
                TotemEffect effect = new TotemEffect();
                effect.init(unit);
                effect.type = unit.holding;
                effect.baseId = base.pieceId;
                effect.dropping = true;
                _bangmgr.deployEffect(unit.owner, effect);
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
            int[][] piecePerTotem = 
                new int[_bases.size()][_points.length];
            int[] maxPiece = new int[piecePerTotem.length];
            int[] totemHeight = new int[piecePerTotem.length];
            int[] numPieces = new int[_points.length];

            for (int ii = 0; ii < piecePerTotem.length; ii++) {
                TotemBase base = _bases.get(ii);
                totemHeight[ii] = base.getTotemHeight();
                for (int jj = 0; jj < totemHeight[ii]; jj++) {
                    int owner = base.getOwner(jj);
                    piecePerTotem[ii][owner]++;
                    numPieces[owner]++;
                }
                for (int pieces : piecePerTotem[ii]) {
                    maxPiece[ii] = Math.max(maxPiece[ii], pieces);
                }
            }

            for (int ii = 0; ii < piecePerTotem.length; ii++) {
                for (int jj = 0; jj < piecePerTotem[ii].length; jj++) {
                    int pieces = piecePerTotem[ii][jj];
                    int pts = pieces * POINTS_PER_TOTEM;
                    points[jj] += pts;
                    
                    // bonus if they have the most pieces on this totem
                    if (pieces == maxPiece[ii]) {
                        points[jj] += MAX_PIECE_BONUS;
                    }
                    
                    // bonus points for each piece based on the height
                    // of the totem
                    if (pieces > 0) {
                        points[jj] += totemHeight[ii] * HEIGHT_BONUS;
                    }
                }
            }

            bangobj.startTransaction();
            try {
                for (int ii = 0; ii < points.length; ii++) {
                    bangobj.grantPoints(ii, points[ii] - _points[ii]);
                    _points[ii] = points[ii];
                    bangobj.stats[ii].setStat(
                            Stat.Type.TOTEM_PIECES, numPieces[ii]);
                    bangobj.stats[ii].setStat(Stat.Type.TOTEM_POINTS, 
                            points[ii] - numPieces[ii] * POINTS_PER_TOTEM); 
                }
            } finally {
                bangobj.commitTransaction();
            }
        }
        
        /** A list of active totem bases. */
        protected ArrayList<TotemBase> _bases = new ArrayList<TotemBase>();

        /** Stores the old points values. */
        protected int[] _points;

        /** Point bonuses. */
        protected static final int MAX_PIECE_BONUS = 50;
        protected static final int HEIGHT_BONUS = 5;
    }
    
    /** Used to track the locations of all the totem generating spots. */
    protected PointSet _totems = new PointSet();    
}
