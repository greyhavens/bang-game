//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.samskivert.util.Tuple;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.AddHeroEffect;
import com.threerings.bang.game.data.effect.HealHeroEffect;
import com.threerings.bang.game.data.effect.RemovePieceEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Revolutionary;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PieceSet;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.HeroLogic;

import static com.threerings.bang.Log.log;

/**
 * A gameplay scenario wherein:
 * <ul>
 * <li>You bigshot is your "hero" which you try to level.
 * <li>Units landing on bonus tiles will heal the hero.
 * <li>The hero killing opponent units will gain experience/level
 * <li>Each level gained by the hero will give them some bonus influence.
 * <li>Units respawn next to their hero.
 * <li>The higher level the hero is, the slower units will respawn.
 * </ul>
 */
public class HeroBuilding extends Scenario
    implements PieceCodes
{
    /**
     * Creates a hero building scenario and registers its delegates.
     */
    public HeroBuilding ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(_herodel = new HeroDelegate());
    }

    @Override // documentation inherited
    public AILogic createAILogic (GameAI ai)
    {
        return new HeroLogic(_herodel);
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        for (int ii = 0; ii < _startSpots.length; ii++) {
            List<Point> heals = bangobj.board.getRandomOccupiableSpots(
                _bangmgr.getTeamSize(ii), _startSpots[ii].x, _startSpots[ii].y, 2, 5);
            for (Point heal : heals) {
                Bonus bonus = dropBonus(bangobj, HealHeroEffect.HEAL_HERO, heal.x, heal.y);
                if (bonus != null) {
                    _hbonuses.offer(new TimedBonus(bangobj.tick, bonus));
                }
            }
        }

        _respawns = new RespawnList[bangobj.getPlayerCount()];
        for (int ii = 0; ii < _respawns.length; ii++) {
            _respawns[ii] = new RespawnList();
        }
    }

    @Override // documentation inherited
    public void recordStats (StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // record the hero level
        int level = stats[pidx].getIntStat(StatType.HERO_LEVEL);
        if (level > 0) {
            user.stats.incrementStat(StatType.HERO_LEVEL, level);
        }
        if (stats[pidx].getIntStat(StatType.TOP_LEVEL) > 0) {
            user.stats.incrementStat(StatType.TOP_LEVEL, 1);
        }
        if (stats[pidx].getIntStat(StatType.HERO_KILLING) > 0) {
            user.stats.incrementStat(StatType.HERO_KILLING, 1);
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        if ((piece instanceof Unit) && ((Unit)piece).originalOwner != -1) {
            Unit unit = (Unit)piece;

            // heroes respawn immediately
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                if (unit instanceof Revolutionary) {
                    for (Unit u : _respawns[unit.originalOwner]) {
                        u.setRespawnTick(Short.MIN_VALUE);
                    }
                }
                unit.setRespawnTick((short)(bangobj.tick + 1));
                _respawns[unit.originalOwner].addFirst(unit);
            } else {
                unit.setRespawnTick((short)(bangobj.tick + 2));
                _respawns[unit.originalOwner].offer(unit);
            }
        }

        super.pieceWasKilled(bangobj, piece, shooter, sidx);
    }

    @Override // documentation inherited
    public void pieceWasRemoved (BangObject bangobj, Piece piece)
    {
        if (!(piece instanceof Bonus)) {
            return;
        }
        // if one of our tracked bonuses is removed, we no longer need to track it
        Bonus bonus = (Bonus)piece;
        for (Iterator<TimedBonus> iter = _hbonuses.iterator(); iter.hasNext(); ) {
            TimedBonus tb = iter.next();
            if (tb.right.pieceId == bonus.pieceId) {
                iter.remove();
                break;
            }
        }

    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        super.tick(bangobj, tick);

        boolean validate = false;

        for (int ii = 0; ii < _respawns.length; ii++) {
            Unit hero = null;
            while (_respawns[ii].size() > 0) {
                if (_respawns[ii].peek().getConfig().rank != UnitConfig.Rank.BIGSHOT &&
                        _respawns[ii].peek().getRespawnTick() + _herodel.getLevel(ii) / 2 > tick) {
                    break;
                }

                Unit unit = _respawns[ii].poll();

                if (hero == null) {
                    hero = _herodel.getHero(bangobj, ii);
                }
                Point spot = (hero == null) ? getStartSpot(unit.owner) : new Point(hero.x, hero.y);

                respawnUnit(bangobj, unit, spot);
                validate = true;
            }
        }

        // clear out bonuses that have gone stale
        while (_hbonuses.size() > 0) {
            if (_hbonuses.peek().left.intValue() + BONUS_TIMEOUT < bangobj.tick) {
                _bangmgr.deployEffect(-1, new RemovePieceEffect(_hbonuses.poll().right));
            } else {
                break;
            }
        }
        return validate;
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // count up the unclaimed scenario bonuses on the board
        int bonuses = 0;
        for (Piece piece : pieces) {
            if (piece instanceof Bonus && ((Bonus)piece).isScenarioBonus()) {
                bonuses++;
            }
        }

        if (bonuses < bangobj.getPlayerCount() * _bangmgr.getTeamSize(0)) {
            _bonuses = (_bonuses + 1) % bangobj.getPlayerCount();
            Unit hero = _herodel.getHero(bangobj, _bonuses);
            Point spot = (hero == null) ? getStartSpot(_bonuses) : new Point(hero.x, hero.y);
            List<Point> heals = bangobj.board.getRandomOccupiableSpots(1, spot.x, spot.y, 2, 5);
            for (Point heal : heals) {
                Bonus bonus = dropBonus(bangobj, HealHeroEffect.HEAL_HERO, heal.x, heal.y);
                if (bonus != null) {
                    _hbonuses.offer(new TimedBonus(bangobj.tick, bonus));
                    return true;
                }
            }
        }

        return super.addBonus(bangobj, pieces);
    }

    /**
     * Called by the delegate when a bonus is added which has a limited lifespan.
     */
    public void bonusAdded (short tick, Bonus bonus)
    {
        _hbonuses.offer(new TimedBonus(tick, bonus));
    }

    /**
     * Respawns a unit.
     */
    protected void respawnUnit (BangObject bangobj, Unit unit, Point spot)
    {
        log.debug("Respawning " + unit + ".");

        // figure out where to put this guy
        Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 10);
        }

        // reassign the unit to its original owner
        unit.setOwner(bangobj, unit.originalOwner);

        // reset the units vital statistics but don't have bigshots reset their active tick
        short oldLastActed = (short)(unit.lastActed + 1);
        unit.respawnInit(bangobj);
        // if the unit is still in play for some reason, remove it first
        if (bangobj.pieces.containsKey(unit.getKey())) {
            _bangmgr.deployEffect(-1, new RemovePieceEffect(unit));
        }

        // don't respawn units for players that are no longer active
        if (!_bangmgr.isActivePlayer(unit.owner)) {
            return;
        }

        if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            unit.lastActed = oldLastActed;
        }

        // then position it and add it back at its new location
        unit.position(bspot.x, bspot.y);
        if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            _bangmgr.deployEffect(-1, new AddHeroEffect(unit, _herodel.getLevel(unit.owner)));
        } else {
            _bangmgr.addPiece(unit, AddPieceEffect.RESPAWNED);
        }
    }

    @Override // documentation inherited
    protected float getScaleFactor ()
    {
        return 0f;
    }

    protected class RespawnList extends LinkedList<Unit> {};
    protected class TimedBonus extends Tuple<Integer, Bonus> {
        public TimedBonus (short tick, Bonus bonus) {
            super(new Integer(tick), bonus);
        }
    };

    protected RespawnList[] _respawns;
    protected LinkedList<TimedBonus> _hbonuses = new LinkedList<TimedBonus>();
    protected HeroDelegate _herodel;
    protected int _bonuses;

    protected static final int BONUS_TIMEOUT = 12;
}
