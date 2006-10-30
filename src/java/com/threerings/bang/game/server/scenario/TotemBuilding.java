//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.TotemLogic;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;
import com.threerings.bang.game.data.effect.TotemEffect;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.util.PointSet;

import com.threerings.parlor.game.data.GameAI;
import com.threerings.presents.server.InvocationException;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li>Fill this in once the scenario is finalized
 * </ul>
 */
public class TotemBuilding extends Scenario
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
    public void filterPieces (
        BangObject bangobj, ArrayList<Piece> starts, ArrayList<Piece> pieces,
        ArrayList<Piece> updates)
    {
        super.filterPieces(bangobj, starts, pieces, updates);
        
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
            dropBonus(bangobj, TotemEffect.TOTEM_PIECES[
                    RandomUtil.getInt(TotemEffect.TOTEM_PIECES.length)],
                    _totems.getX(ii), _totems.getY(ii));
        }
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // count up the totem pieces that are "in play"
        int totems = 0;
        int crowns = 0;
        int bases = 0;
        for (int ii = 0; ii < pieces.length; ii++) {
            String type = null;
            if (pieces[ii] instanceof TotemBonus) {
                totems++;
                type = ((TotemBonus)pieces[ii]).getConfig().type;
            } else if (pieces[ii] instanceof Unit && 
                     TotemBonus.isHolding((Unit)pieces[ii])) {
                totems++;
                type = ((Unit)pieces[ii]).holding;
            } else if (pieces[ii] instanceof TotemBase) {
                bases++;
                if (!((TotemBase)pieces[ii]).canAddPiece()) {
                    crowns++;
                }
            }
            if (TotemEffect.TOTEM_CROWN_BONUS.equals(type)) {
                crowns++;
            }
        }

        // if there is not at least 1.5 totems in play for every player in the
        // game, try to spawn another one
        int numPlayers = bangobj.getActivePlayerCount();
        if (totems < TOTEM_RATIO * numPlayers) {
            String type = TotemEffect.TOTEM_PIECES[RandomUtil.getInt(
                    TotemEffect.TOTEM_PIECES.length)];
            float progress = (float)bangobj.tick / bangobj.duration;
            // if we're far enough along in the round and there isn't a
            // crown for each base, maybe spawn one
            if (progress > CROWN_SPAWN && crowns < bases && 
                    RandomUtil.getInt(100) > 50) {
                type = TotemEffect.TOTEM_CROWN_BONUS;
            }
            return placeBonus(bangobj, pieces, Bonus.createBonus(
                        BonusConfig.getConfig(type)),
                    _totems);
        } else {
            return super.addBonus(bangobj, pieces);
        }
    }

    @Override // documentation inherited
    public void recordStats (
        BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the number of totem pieces stacked
        for (TotemBonus.Type type : TotemBonus.Type.values()) {
            int totems = bangobj.stats[pidx].getIntStat(type.stat());
            if (totems > 0) {
                user.stats.incrementStat(type.stat(), totems);
            }
        }
    }

    protected static class TotemBaseDelegate extends CounterDelegate
    {
        @Override // documentation inherited
        public void roundWillStart (BangObject bangobj)
            throws InvocationException
        {
            super.roundWillStart(bangobj);
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
        public void tick (BangObject bangobj, short tick)
        {
            boolean alive = false;
            for (TotemBase base : _bases) {
                alive = alive || base.canAddPiece();
            }
            if (!alive) {
                bangobj.setLastTick(tick);
            }
        }

        @Override // documentation inherited
        public void roundDidEnd (BangObject bangobj)
        {
        }

        @Override // documentation inherited
        public void pieceWasKilled (BangObject bangobj, Piece piece)
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
                BangObject bangobj, int owner, int delta, Stat.Type stat)
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
                for (int ii = 0; ii < piecePerTotem.length; ii++) {
                    TotemBase base = _bases.get(ii);
                    for (int jj = 0, nn = base.numPieces(); jj < nn; jj++) {
                        int owner = base.getOwner(jj);
                        piecePerTotem[ii][owner]++;
                        TotemBonus.Type type = base.getType(jj);
                        totemHeight[ii] += type.height();
                        points[owner] += type.value();
                    }
                    for (int pieces : piecePerTotem[ii]) {
                        maxPiece[ii] = Math.max(maxPiece[ii], pieces);
                    }
                }

                for (int ii = 0; ii < piecePerTotem.length; ii++) {
                    for (int jj = 0; jj < piecePerTotem[ii].length; jj++) {
                        int pieces = piecePerTotem[ii][jj];
                        
                        // bonus if they have the most pieces on this totem
                        // and the totem is not empty
                        if (pieces > 0 && pieces == maxPiece[ii]) {
                            totemPoints[jj] += MAX_PIECE_BONUS;
                        }
                        
                        // bonus points for each piece based on the height
                        // of the totem
                        if (pieces > 0) {
                            totemPoints[jj] += totemHeight[ii] * HEIGHT_BONUS;
                        }
                    }
                }

                for (int ii = 0; ii < points.length; ii++) {
                    int diff = points[ii] + totemPoints[ii] - _points[ii];
                    bangobj.grantPoints(ii, 
                            points[ii] + totemPoints[ii] - _points[ii]);
                    _points[ii] = points[ii] + totemPoints[ii];
                    bangobj.stats[ii].setStat(
                        Stat.Type.TOTEM_POINTS, totemPoints[ii]); 
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
        protected static final int HEIGHT_BONUS = 3;
    }
    
    /** Used to track the locations of all the totem generating spots. */
    protected PointSet _totems = new PointSet();    

    /** How long into the round before crowns will spawn. */
    protected static final float CROWN_SPAWN = 0.7f;

    /** Totem to player ratio. */
    protected static final float TOTEM_RATIO = 1.5f;
}
