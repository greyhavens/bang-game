//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;

import java.util.ArrayList;
import java.util.LinkedList;

import com.threerings.presents.server.InvocationException;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.HealHeroEffect;
import com.threerings.bang.game.data.effect.LevelEffect;
import com.threerings.bang.game.data.effect.RemovePieceEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
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
            ArrayList<Point> heals = bangobj.board.getRandomOccupiableSpots(
                    _bangmgr.getTeamSize(ii), _startSpots[ii].x, _startSpots[ii].y, 2, 5);
            for (Point heal : heals) {
                dropBonus(bangobj, HealHeroEffect.HEAL_HERO, heal.x, heal.y);
            }
        }

        _respawns = new RespawnList[bangobj.getPlayerCount()];
        for (int ii = 0; ii < _respawns.length; ii++) {
            _respawns[ii] = new RespawnList();
        }
    }

    @Override // documentation inherited
    public void recordStats (BangObject bangobj, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(bangobj, gameTime, pidx, user);

        // record the hero level
        int level = bangobj.stats[pidx].getIntStat(StatType.HERO_LEVEL);
        if (level > 0) {
            user.stats.incrementStat(StatType.HERO_LEVEL, level);
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        if ((piece instanceof Unit) && ((Unit)piece).originalOwner != -1) {
            Unit unit = (Unit)piece;

            // heroes respawn immediately
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                unit.setRespawnTick(bangobj.tick);
                _respawns[unit.originalOwner].addFirst(unit);
            } else {
                unit.setRespawnTick((short)(bangobj.tick + 2));
                _respawns[unit.originalOwner].offer(unit);
            }
        }

        super.pieceWasKilled(bangobj, piece, shooter, sidx);
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

                Point spot = null;
                if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                    spot = getStartSpot(unit.owner);
                } else {
                    if (hero == null) {
                        hero = _herodel.getHero(bangobj, ii);
                    }
                    spot = new Point(hero.x, hero.y);
                }

                respawnUnit(bangobj, unit, spot);
                validate = true;
            }
        }
        return validate;
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, Piece[] pieces)
    {
        // count up the unclaimed scenario bonuses on the board
        int bonuses = 0;
        for (Piece piece : pieces) {
            if (piece instanceof Bonus && ((Bonus)piece).isScenarioBonus()) {
                bonuses++;
            }
        }

        if (bonuses < bangobj.getPlayerCount() * _bangmgr.getTeamSize(0)) {
            Unit hero = _herodel.getHero(bangobj, _bonuses++ % bangobj.getPlayerCount());
            ArrayList<Point> heals = bangobj.board.getRandomOccupiableSpots(
                    1, hero.x, hero.y, 2, 5);
            for (Point heal : heals) {
                dropBonus(bangobj, HealHeroEffect.HEAL_HERO, heal.x, heal.y);
            }
            return true;
        }

        return super.addBonus(bangobj, pieces);
    }

    /**
     * Respawns a unit.
     */
    protected void respawnUnit (BangObject bangobj, Unit unit, Point spot)
    {
        log.fine("Respawning " + unit + ".");

        // figure out where to put this guy
        Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
        if (bspot == null) {
            bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 10);
        }

        // reassign the unit to its original owner
        unit.setOwner(bangobj, unit.originalOwner);

        // reset the units vital statistics but don't have bigshots reset their active tick
        short oldLastActed = unit.lastActed;
        unit.respawnInit(bangobj);
        if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            unit.lastActed = oldLastActed;
        }

        // if the unit is still in play for some reason, remove it first
        if (bangobj.pieces.containsKey(unit.getKey())) {
            _bangmgr.deployEffect(-1, new RemovePieceEffect(unit));
        }

        // don't respawn units for players that are no longer active
        if (!_bangmgr.isActivePlayer(unit.owner)) {
            return;
        }

        // then position it and add it back at its new location
        unit.position(bspot.x, bspot.y);
        _bangmgr.addPiece(unit, AddPieceEffect.RESPAWNED);
        if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            _bangmgr.deployEffect(-1,
                    LevelEffect.changeLevel(bangobj, unit.owner, _herodel.getLevel(unit.owner)));
        }
    }

    protected class RespawnList extends LinkedList<Unit> {};

    protected RespawnList[] _respawns;
    protected HeroDelegate _herodel;
    protected int _bonuses;
}
