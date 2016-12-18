//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.List;

import java.awt.Point;

import com.samskivert.util.RandomUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AddSpawnedBonusEffect;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.effect.LevelEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.HeroBuildingInfo;

/**
 * Handles managing of hero levels, influences and respawn.
 */
public class HeroDelegate extends CounterDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        super.roundWillStart(bangobj);
        _levels = new byte[_bangmgr.getPlayerCount()];
        _xp = new int[_bangmgr.getPlayerCount()];
    }

    @Override // documentation inherited
    public boolean tick (BangObject bangobj, short tick)
    {
        if (tick == 0) {
            for (int ii = 0; ii < bangobj.getPlayerCount(); ii++) {
                LevelEffect effect = LevelEffect.changeLevel(bangobj, ii, (byte)0);
                if (effect != null) {
                    _bangmgr.deployEffect(-1, effect);
                }
            }
        }
        return false;
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        super.pieceWasKilled(bangobj, piece, shooter, sidx);

        // the base experience gained is inversely proportional to the hero level
        if (shooter != -1) {
            _xp[shooter] += 1 + Math.max(0, 3 - getLevel(shooter) / 4);
        }

        // killing a hero will result in bonuses flying out of their ass
        if (piece instanceof Unit && ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            if (shooter != -1 && _levels[piece.owner] > 10) {
                bangobj.stats[shooter].incrementStat(StatType.HERO_KILLING, 1);
            }
            spawnBonusesFromHero(bangobj, piece);
            _xp[piece.owner] = Math.max(0, _xp[piece.owner] - _levels[piece.owner]);
        }

        if (sidx != -1) {
            Piece spiece = bangobj.pieces.get(sidx);
            // if the hero kills another unit then they get 7 additional xp
            if (spiece != null && spiece instanceof Unit &&
                    ((Unit)spiece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                _xp[spiece.owner] += 7;
            }
        }

        updateLevels(bangobj, piece.pieceId);
    }

    @Override // documentation inherited
    public void roundDidEnd (BangObject bangobj)
    {
        if (_xp == null) {
            return;
        }
        for (int ii = 0; ii < _xp.length; ii++) {
            if (_xp[ii] >= LEVEL_XP[LEVEL_XP.length-1]) {
                bangobj.stats[ii].setStat(StatType.TOP_LEVEL, 1);
            }
        }
    }

    /**
     * Returns the hero level for the player.
     */
    public byte getLevel (int pidx)
    {
        return _levels[pidx];
    }

    /**
     * Returns the hero for the player.
     */
    public Unit getHero (BangObject bangobj, int pidx)
    {
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Unit && piece.owner == pidx &&
                    ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                return (Unit)piece;
            }
        }
        return null;
    }

    /**
     * Used by the tutorial to apply level effects immediately.
     */
    public void applyLevels (BangObject bangobj)
    {
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Unit &&
                    ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT &&
                    ((Unit)piece).getInfluence(Unit.InfluenceType.SPECIAL) == null) {
                LevelEffect effect = LevelEffect.changeLevel(
                        bangobj, piece.owner, _levels[piece.owner]);
                if (effect != null) {
                    _bangmgr.deployEffect(-1, effect);
                }
            }
        }
    }

    /**
     * Updates the levels for each player based on their xp.
     */
    protected void updateLevels (BangObject bangobj, int queuePiece)
    {
        for (Counter counter : _counters) {
            for (int ii = LEVEL_XP.length - 1; ii >= 0; ii--) {
                if (_xp[counter.owner] >= LEVEL_XP[ii]) {
                    int diff = ii - _levels[counter.owner];
                    _levels[counter.owner] = (byte)ii;
                    bangobj.stats[counter.owner].setStat(StatType.HERO_LEVEL, ii);
                    bangobj.grantPoints(counter.owner, diff * pointsPerCounter());
                    break;
                }
            }
            if (_levels[counter.owner] != counter.count) {
                if (_levels[counter.owner] > counter.count) {
                    LevelEffect effect = LevelEffect.changeLevel(
                            bangobj, counter.owner, _levels[counter.owner]);
                    if (effect != null) {
                        _bangmgr.deployEffect(-1, effect);
                    }
                }
                _bangmgr.deployEffect(-1, CountEffect.changeCount(
                            counter.pieceId, _levels[counter.owner], queuePiece));
            }
        }
    }

    @Override // documentation inherited from CounterDelegate
    protected int pointsPerCounter ()
    {
        return HeroBuildingInfo.POINTS_PER_LEVEL;
    }

    @Override // documentation inherited
    protected void checkAdjustedCounter (BangObject bangobj, Unit unit)
    {
        // nothing doing
    }

    /**
     * When a hero dies, bonuses fly out from their position, their strength based on the
     * level of the hero.
     */
    protected void spawnBonusesFromHero (BangObject bangobj, Piece piece)
    {
        // figure out how many bonuses to spawn based on the hero level
        int numSpawns = 1 + getLevel(piece.owner) / 3;

        List<Point> drops = bangobj.board.getRandomOccupiableSpots(
            numSpawns, piece.x, piece.y, 1, 3);
        Bonus[] bonuses = selectBonuses(bangobj, getLevel(piece.owner), drops.size());
        for (int ii = 0; ii < bonuses.length; ii++) {
            Bonus bonus = bonuses[ii];
            Point drop = drops.get(ii);
            bonus.assignPieceId(bangobj);
            bonus.position(drop.x, drop.y);
            _bangmgr.deployEffect(-1,
                    new AddSpawnedBonusEffect(bonus, piece.x, piece.y, piece.pieceId, ii == 0));
            if (_parent instanceof HeroBuilding) {
                ((HeroBuilding)_parent).bonusAdded(bangobj.tick, bonus);
            }
        }
    }

    /**
     * Selects a random bonus to be spawned from a dying hero.
     */
    protected Bonus[] selectBonuses (BangObject bangobj, int level, int num)
    {
        BonusConfig[] configs = BonusConfig.getTownBonuses(bangobj.townId);
        int[] weights = new int[configs.length];
        Bonus[] bonuses = new Bonus[num];
        int cardIdx = -1;

        // Use the base weight of the bonuses, but have cutoffs based on the hero level
        for (int ii = 0; ii < configs.length; ii++) {
            BonusConfig config = configs[ii];
            if (config.baseWeight <= 0 || config.minPointDiff > 0) {
                continue;
            }
            if (config.baseWeight < (8 - level)*10) {
                continue;
            }
            int weight = config.baseWeight;
            // We like cards in this situation
            if ("card".equals(config.type)) {
                weight *= 4;
                cardIdx = ii;
            }
            weights[ii] = weight;
        }
        for ( ; num > 0; num--) {
            int idx = RandomUtil.getWeightedIndex(weights);
            bonuses[num-1] = Bonus.createBonus(configs[idx]);
            if (idx != cardIdx) {
                weights[idx] = 0;
            }
        }
        return bonuses;
    }

    protected byte[] _levels;
    protected int[] _xp;

    protected static final int[] LEVEL_XP = {
        0, 5, 12, 20, 30, 42, 56, 72, 90, 110, 135, 160, 180, 230, 300
    };
}
