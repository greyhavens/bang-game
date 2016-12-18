//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.List;

import com.threerings.parlor.game.data.GameAI;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.server.ai.AILogic;
import com.threerings.bang.game.server.ai.LandGrabLogic;

/**
 * Implements the server side of the Land Grab gameplay scenario.
 */
public class LandGrab extends Scenario
{
    public LandGrab ()
    {
        registerDelegate(new TrainDelegate());
        registerDelegate(new RespawnDelegate());
        registerDelegate(_homedel = new HomesteadDelegate());
    }

    /**
     * Returns the list of homesteads on the board.
     */
    public List<Homestead> getHomesteads ()
    {
        return _homedel.getHomesteads();
    }

    @Override // from Scenario
    public AILogic createAILogic (GameAI ai)
    {
        return new LandGrabLogic(this);
    }

    @Override // from Scenario
    public void roundDidEnd (BangObject bangobj)
    {
        super.roundDidEnd(bangobj);

        // increment each players' homestead related stats
        int[] steads = new int[bangobj.players.length];
        for (Homestead stead : getHomesteads()) {
            if (stead.owner >= 0) {
                steads[stead.owner]++;
            }
        }
        if (bangobj.lastTick < bangobj.duration / 2) {
            return;
        }
        int loneClaimerIdx = -1;
        for (int ii = 0; ii < steads.length; ii++) {
            // check to see if one player alone holds all claimed claims
            if (steads[ii] > 0) {
                loneClaimerIdx = (loneClaimerIdx == -1) ? ii : -2;
            }
        }
        if (loneClaimerIdx >= 0) {
            bangobj.stats[loneClaimerIdx].incrementStat(StatType.LONE_STEADER, 1);
        }
    }

    @Override // from Scenario
    public void recordStats (
        StatSet[] stats, int gameTime, int pidx, PlayerObject user)
    {
        super.recordStats(stats, gameTime, pidx, user);

        // persist their various homestead related stats
        for (StatType stat : ACCUM_STATS) {
            user.stats.incrementStat(stat, stats[pidx].getIntStat(stat));
        }
    }

    @Override // from Scenario
    protected Point getStartSpot (int pidx)
    {
        Point spot = _homedel.getStartSpot(pidx);
        return (spot == null) ? super.getStartSpot(pidx) : spot;
    }

    /** Handles the behavior of our homesteads. */
    protected HomesteadDelegate _homedel;

    /** Stats we accumulate from the in-game versions to the player's object. */
    protected static final StatType[] ACCUM_STATS = {
        StatType.STEADS_CLAIMED, StatType.STEADS_DESTROYED, StatType.LONE_STEADER
    };
}
