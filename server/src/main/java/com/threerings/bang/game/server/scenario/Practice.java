//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PieceSet;

import com.threerings.presents.server.InvocationException;

/**
 * Handles the server side operation of practice scenarios.
 */
public class Practice extends Scenario
{
    /**
     * Creates a practice scenario and registers its delegates.
     */
    public Practice ()
    {
        registerDelegate(new RespawnDelegate(1));
    }

    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj, Piece[] starts, PieceSet purchases)
        throws InvocationException
    {
        super.roundWillStart(bangobj, starts, purchases);

        // The user gets 3 units of the specified type
        BangConfig bconfig = (BangConfig)_bangmgr.getConfig();
        Unit[] units = new Unit[NUM_UNITS];
        for (int ii = 0; ii < units.length; ii++) {
            units[ii] = Unit.getUnit(bconfig.getScenario(0));
        }

        for (int ii = 0; ii < bangobj.players.length; ii++) {
            if (!_bangmgr.isAI(ii)) {
                _bangmgr.initAndPrepareUnits(units, ii);
            }
        }

        // The AI gets 3 random units
        UnitConfig[] ucs = UnitConfig.getTownUnits(bangobj.townId, VALID_RANKS);
        int[] weights = new int[ucs.length];
        Arrays.fill(weights, 1);
        units = new Unit[NUM_UNITS];
        for (int ii = 0; ii < units.length; ii++) {
            int idx = RandomUtil.getWeightedIndex(weights);
            units[ii] = Unit.getUnit(ucs[idx].type);
            weights[idx] = 0;
        }
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            if (_bangmgr.isAI(ii)) {
                _bangmgr.initAndPrepareUnits(units, ii);
            }
        }
    }

    @Override // documentation inherited
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
        // give the AI a new unit to replace the fallen
        if (_bangmgr.isAI(piece.owner)) {
            int owner = piece.owner;
            UnitConfig[] ucs = UnitConfig.getTownUnits(bangobj.townId, VALID_RANKS);
            piece = Unit.getUnit(ucs[RandomUtil.getInt(ucs.length)].type);
            _bangmgr.initAndPrepareUnit((Unit)piece, owner);
            piece.lastActed = bangobj.tick;
        }
        super.pieceWasKilled(bangobj, piece, shooter, sidx);
    }

    @Override // documentation inherited
    public boolean addBonus (BangObject bangobj, List<Piece> pieces)
    {
        // no automatic bonuses in practice scenario
        return false;
    }

    @Override // documentation inherited
    public boolean shouldPayEarnings (PlayerObject user)
    {
        return false;
    }

    @Override // documentation inherited
    public short getBaseDuration ()
    {
        // practice scenarios don't have a definite ending time
        return 4000;
    }

    protected static final int NUM_UNITS = 3;

    protected static final EnumSet<UnitConfig.Rank> VALID_RANKS =
        EnumSet.of(UnitConfig.Rank.NORMAL, UnitConfig.Rank.BIGSHOT);
}
