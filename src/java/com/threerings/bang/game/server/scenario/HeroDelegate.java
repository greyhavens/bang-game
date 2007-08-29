//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.threerings.bang.data.StatType;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.effect.LevelEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.HeroBuildingInfo;
import com.threerings.presents.server.InvocationException;

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
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        super.pieceWasKilled(bangobj, piece, shooter, sidx);

        // any kills will give the player 1 xp
        if (shooter != -1) {
            _xp[shooter] += 1;
        }

        // losing their hero will automatically drop them down one level
        if (piece instanceof Unit && ((Unit)piece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            _xp[piece.owner] = LEVEL_XP[Math.max(0, _levels[piece.owner] - 1)];
        }

        if (sidx != -1) {
            Piece spiece = bangobj.pieces.get(sidx);
            // if the hero kills another unit then they get 9 additional xp
            if (spiece != null && spiece instanceof Unit &&
                    ((Unit)spiece).getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                _xp[spiece.owner] += 9;
            }
        }

        updateLevels(bangobj, piece.pieceId);
    }

    /**
     * Updates the levels for each player based on their xp.
     */
    protected void updateLevels (BangObject bangobj, int queuePiece)
    {
        for (Counter counter : _counters) {
            for (int ii = LEVEL_XP.length - 1; ii > 0; ii--) {
                if (_xp[counter.owner] >= LEVEL_XP[ii]) {
                    _levels[counter.owner] = (byte)ii;
                    bangobj.stats[counter.owner].setStat(StatType.HERO_LEVEL, ii);
                    break;
                }
            }
            if (_levels[counter.owner] != counter.count) {
                _bangmgr.deployEffect(-1, LevelEffect.changeLevel(
                           bangobj, counter.owner, _levels[counter.owner]));
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

    protected byte[] _levels;
    protected int[] _xp;

    protected static final int[] LEVEL_XP = {0, 5, 12, 20, 30, 42, 56, 72, 90, 110};
}
